package br.com.condovitta.app.feature

import androidx.lifecycle.SavedStateHandle
import br.com.condovitta.app.FixedTime
import br.com.condovitta.app.MainDispatcherRule
import br.com.condovitta.app.data.demo.DemoMoradorRepository
import br.com.condovitta.app.data.remote.ApiJson
import br.com.condovitta.app.data.remote.MoradorApi
import br.com.condovitta.app.data.repository.RemoteMoradorRepository
import br.com.condovitta.app.domain.Autorizacao
import br.com.condovitta.app.domain.DiaSemana
import br.com.condovitta.app.domain.EscopoAutorizado
import br.com.condovitta.app.domain.NovaAutorizacao
import br.com.condovitta.app.domain.NovoAutorizado
import br.com.condovitta.app.domain.Outcome
import br.com.condovitta.app.domain.SituacaoAutorizacao
import br.com.condovitta.app.domain.StatusBoleto
import br.com.condovitta.app.domain.StatusVisita
import br.com.condovitta.app.domain.TipoVisitante
import br.com.condovitta.app.feature.morador.AutorizacaoViewModel
import br.com.condovitta.app.feature.morador.AvisosViewModel
import br.com.condovitta.app.feature.morador.NovaAutorizacaoViewModel
import br.com.condovitta.app.feature.morador.QuandoVisita
import br.com.condovitta.app.feature.morador.convite
import br.com.condovitta.app.feature.morador.janela
import br.com.condovitta.app.feature.morador.prazo
import br.com.condovitta.app.core.common.Fmt
import br.com.condovitta.app.ui.Messenger
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalTime

@OptIn(ExperimentalCoroutinesApi::class)
class MoradorTest {

    @get:Rule val main = MainDispatcherRule()

    private lateinit var server: MockWebServer
    private val time = FixedTime() // quarta, 04/11/2026 10:00

    @Before fun setUp() { server = MockWebServer().apply { start() } }
    @After fun tearDown() = server.shutdown()

    private fun remote() = RemoteMoradorRepository(
        Retrofit.Builder().baseUrl(server.url("/")).client(OkHttpClient())
            .addConverterFactory(ApiJson.asConverterFactory("application/json".toMediaType())).build().create(MoradorApi::class.java)
    )

    /* ---------------- contrato com o backend ---------------- */

    @Test fun `boletos do morador mantem o valor exato`() = runTest {
        server.enqueue(MockResponse().setBody("""{"unidadeId":8,"unidade":"B-204","condominio":"Vila do Sol 2",
            "atual":{"id":900,"competencia":"2026-11-01","vencimento":"2026-11-10","valor":612.40,"status":"em_aberto","linhaDigitavel":"03399.6","pix":"000201","temPdf":true,
              "itens":[{"descricao":"Taxa condominial","valor":480.00,"tipo":"TAXA"},{"descricao":"Fundo","valor":132.40,"tipo":"FUNDO"}]},
            "boletos":[{"id":899,"competencia":"2026-10","vencimento":"2026-10-10","valor":598.10,"status":"pago","pagoEm":"2026-10-08","valorPago":598.10,"forma":"Pix"},
                       {"id":898,"competencia":"2026-09","vencimento":"2026-09-10","valor":581.70,"status":"vencido","corrigido":605.33}],
            "historico":[],"acordos":[]}"""))
        val f = (remote().financeiro() as Outcome.Ok).value
        assertThat(f.atual!!.valor).isEqualTo(BigDecimal("612.40"))
        assertThat(f.atual!!.competencia).isEqualTo("2026-11")
        assertThat(f.atual!!.itens.map { it.valor }).containsExactly(BigDecimal("480.00"), BigDecimal("132.40")).inOrder()
        assertThat(f.pagos.single().status).isEqualTo(StatusBoleto.PAGO)
        assertThat(f.vencidos.single().corrigido).isEqualTo(BigDecimal("605.33"))
        assertThat(server.takeRequest().path).isEqualTo("/api/v1/financeiro/morador")
    }

    @Test fun `autorizacao recorrente manda dias e horarios`() = runTest {
        server.enqueue(MockResponse().setBody("""{"id":1,"nome":"Juliana","tipo":"PRESTADOR","recorrente":true,"dias":["SEG","QUA"],"horaInicio":"10:00:00","horaFim":"17:00:00","codigo":"418273","situacao":"ATIVA"}"""))
        val a = (remote().autorizar(
            NovaAutorizacao("Juliana", TipoVisitante.PRESTADOR, recorrente = true, dias = listOf(DiaSemana.SEG, DiaSemana.QUA),
                horaInicio = LocalTime.of(10, 0), horaFim = LocalTime.of(17, 0), validoAte = LocalDate.of(2027, 5, 1))
        ) as Outcome.Ok).value
        val body = server.takeRequest().body.readUtf8()
        assertThat(body).contains("\"dias\":[\"SEG\",\"QUA\"]")
        assertThat(body).contains("\"horaInicio\":\"10:00\"")
        assertThat(body).contains("\"validoAte\":\"2027-05-01\"")
        assertThat(body).contains("\"recorrente\":true")
        assertThat(a.dias).containsExactly(DiaSemana.SEG, DiaSemana.QUA)
        assertThat(a.codigo).isEqualTo("418273")
    }

    @Test fun `autorizacao de um dia manda inicio e fim`() = runTest {
        server.enqueue(MockResponse().setBody("""{"id":2,"nome":"Marcos","tipo":"VISITA","inicio":"2026-11-06T19:00:00","fim":"2026-11-06T23:00:00","situacao":"ATIVA"}"""))
        remote().autorizar(NovaAutorizacao("Marcos", TipoVisitante.VISITA, inicio = LocalDate.of(2026, 11, 6).atTime(19, 0), fim = LocalDate.of(2026, 11, 6).atTime(23, 0)))
        val body = server.takeRequest().body.readUtf8()
        assertThat(body).contains("\"inicio\":\"2026-11-06T19:00:00\"")
        assertThat(body).doesNotContain("\"dias\"")
    }

    @Test fun `responder a portaria`() = runTest {
        server.enqueue(MockResponse().setBody("""{"visitanteId":7,"acessoId":507,"nome":"Bruno","status":"liberado","via":"APP"}"""))
        val v = (remote().responder(507, liberar = true, recado = " pode subir ") as Outcome.Ok).value
        assertThat(v.status).isEqualTo(StatusVisita.LIBERADO)
        val req = server.takeRequest()
        assertThat(req.path).isEqualTo("/api/v1/visitantes/consultas/507/responder")
        assertThat(req.body.readUtf8()).isEqualTo("""{"liberar":true,"recado":"pode subir"}""")
    }

    @Test fun `encomendas trazem o codigo e os autorizados`() = runTest {
        server.enqueue(MockResponse().setBody("""{"unidade":"B-204","aguardando":[{"id":1,"unidadeId":8,"unidade":"B-204","transportadora":"Amazon","codigo":"4821","situacao":"AGUARDANDO"}],
            "retiradas":[],"autorizados":[{"id":3,"nome":"Pedro","escopo":"SEMPRE","fixo":true},{"id":4,"nome":"Marta","escopo":"ATE","validoAte":"2026-11-30","fixo":false}]}"""))
        val m = (remote().encomendas() as Outcome.Ok).value
        assertThat(m.aguardando.single().codigo).isEqualTo("4821")
        assertThat(m.autorizados.map { it.escopo }).containsExactly(EscopoAutorizado.SEMPRE, EscopoAutorizado.ATE).inOrder()
        assertThat(m.autorizados[1].validoAte).isEqualTo(LocalDate.of(2026, 11, 30))
    }

    @Test fun `mural traduz a categoria`() = runTest {
        server.enqueue(MockResponse().setBody("""[{"id":1,"titulo":"Água","texto":"…","categoria":"MANUTENCAO","publicadoEm":"2026-11-01T08:00:00","fixado":true,"exigeCiencia":true,"lido":false,"ciente":false,"anexos":[]}]"""))
        val a = (remote().avisos() as Outcome.Ok).value.single()
        assertThat(a.categoria).isEqualTo("Manutenção")
        assertThat(a.exigeCiencia).isTrue()
    }

    /* ---------------- regras ---------------- */

    private fun aut(recorrente: Boolean = false, dias: List<DiaSemana> = emptyList()) = Autorizacao(
        1, "Juliana Martins", null, null, TipoVisitante.PRESTADOR, null, null,
        if (recorrente) null else time.now().minusHours(1), if (recorrente) null else time.now().plusHours(3),
        recorrente, dias, LocalTime.of(10, 0), LocalTime.of(17, 0), null, null, "418273", true, "Ana"
    )

    @Test fun `situacao da autorizacao sai das datas`() {
        val agora = time.now() // quarta 10:00
        assertThat(aut().situacao(agora)).isEqualTo(SituacaoAutorizacao.LIBERADA_AGORA)
        assertThat(aut().situacao(agora.plusHours(5))).isEqualTo(SituacaoAutorizacao.EXPIRADA)
        assertThat(aut().situacao(agora.minusHours(2))).isEqualTo(SituacaoAutorizacao.AGENDADA)
        assertThat(aut(true, listOf(DiaSemana.QUA)).situacao(agora)).isEqualTo(SituacaoAutorizacao.LIBERADA_AGORA)
        assertThat(aut(true, listOf(DiaSemana.SEX)).situacao(agora)).isEqualTo(SituacaoAutorizacao.AGENDADA)
        assertThat(aut().copy(ativa = false).situacao(agora)).isEqualTo(SituacaoAutorizacao.REVOGADA)
    }

    @Test fun `janela e convite em portugues`() {
        assertThat(DiaSemana.descrever(listOf(DiaSemana.SEX, DiaSemana.SEG, DiaSemana.QUA))).isEqualTo("seg, qua e sex")
        assertThat(DiaSemana.descrever(listOf(DiaSemana.SEG, DiaSemana.TER, DiaSemana.QUA, DiaSemana.QUI, DiaSemana.SEX))).isEqualTo("seg a sex")
        assertThat(janela(aut(true, listOf(DiaSemana.SEG, DiaSemana.QUA)))).isEqualTo("seg e qua · 10:00 – 17:00")
        val c = convite(aut(true, listOf(DiaSemana.SEG)), "Residencial Vila do Sol 2", "B-204")
        assertThat(c).contains("Olá, Juliana Martins!")
        assertThat(c).contains("Residencial Vila do Sol 2 (unidade B-204)")
        assertThat(c).contains("418273")
    }

    @Test fun `dinheiro e prazo do boleto`() {
        assertThat(Fmt.money(BigDecimal("612.40"))).isEqualTo("R$ 612,40")
        assertThat(Fmt.competencia("2026-11")).isEqualTo("Novembro de 2026")
        val f = DemoMoradorRepository(time)
        val b = kotlinx.coroutines.runBlocking { (f.financeiro() as Outcome.Ok).value.atual!! }
        assertThat(prazo(b, time.today())).isEqualTo("Vence em 6 dias")
        assertThat(prazo(b, LocalDate.of(2026, 11, 10))).isEqualTo("Vence hoje")
        assertThat(prazo(b, LocalDate.of(2026, 11, 12))).isEqualTo("Venceu há 2 dia(s)")
    }

    /* ---------------- demo + ViewModels ---------------- */

    @Test fun `responder a consulta tira da lista e vai para o historico`() = runTest {
        val repo = DemoMoradorRepository(time)
        val c = (repo.visitas() as Outcome.Ok).value.consultas.single()
        repo.responder(c.acessoId!!, liberar = true, recado = "Pode subir")
        val v = (repo.visitas() as Outcome.Ok).value
        assertThat(v.consultas).isEmpty()
        assertThat(v.historico.first().status).isEqualTo(StatusVisita.LIBERADO)
        assertThat(v.historico.first().recado).isEqualTo("Pode subir")
    }

    @Test fun `nova autorizacao recorrente exige dias`() = runTest {
        val vm = NovaAutorizacaoViewModel(DemoMoradorRepository(time), Messenger(), time)
        vm.quando(QuandoVisita.RECORRENTE)
        assertThat(vm.ui.value.horaInicio).isEqualTo(LocalTime.of(8, 0))
        vm.update { it.copy(nome = "Amanda Lopes", horaInicio = LocalTime.of(7, 0), horaFim = LocalTime.of(19, 0)) }
        assertThat(vm.ui.value.valido).isFalse()
        vm.update { it.copy(dias = setOf(DiaSemana.SEG, DiaSemana.SEX)) }
        assertThat(vm.ui.value.valido).isTrue()
        vm.salvar()
        val a = vm.criada.first { it != null }!!
        assertThat(a.recorrente).isTrue()
        assertThat(a.dias).containsExactly(DiaSemana.SEG, DiaSemana.SEX).inOrder()
        assertThat(a.codigo).hasLength(6)
    }

    @Test fun `revogar some da lista e desfazer devolve`() = runTest {
        val repo = DemoMoradorRepository(time)
        val vm = AutorizacaoViewModel(repo, Messenger(), time, SavedStateHandle(mapOf("id" to 301L)))
        vm.refresh()
        advanceUntilIdle()
        assertThat(vm.state.value.data?.nome).isEqualTo("Juliana Martins")
        vm.revogar()
        advanceUntilIdle()
        assertThat(vm.revogada.value).isTrue()
        assertThat((repo.visitas() as Outcome.Ok).value.autorizacoes.map { it.id }).doesNotContain(301L)
        repo.reativar(301)
        assertThat((repo.visitas() as Outcome.Ok).value.autorizacoes.map { it.id }).contains(301L)
    }

    @Test fun `abrir aviso marca lido e ciente registra`() = runTest {
        val repo = DemoMoradorRepository(time)
        val vm = AvisosViewModel(repo, Messenger())
        vm.refresh()
        advanceUntilIdle()
        val a = vm.state.value.data!!.first()
        assertThat(a.fixado).isTrue()
        vm.abrir(a)
        advanceUntilIdle()
        assertThat(vm.state.value.data!!.first().lido).isTrue()
        vm.ciente(vm.aberto.value!!)
        advanceUntilIdle()
        assertThat(vm.aberto.value!!.ciente).isTrue()
    }

    @Test fun `autorizado fixo nao sai e ate exige data`() = runTest {
        val repo = DemoMoradorRepository(time)
        assertThat(repo.removerAutorizado(1)).isInstanceOf(Outcome.Err::class.java)
        assertThat(repo.autorizarRetirada(NovoAutorizado("Marta", null, null, EscopoAutorizado.ATE))).isInstanceOf(Outcome.Err::class.java)
        val ok = (repo.autorizarRetirada(NovoAutorizado("Marta", "RG 1", "Sogra", EscopoAutorizado.ATE, validoAte = time.today().plusDays(10))) as Outcome.Ok).value
        assertThat(ok.fixo).isFalse()
    }
}
