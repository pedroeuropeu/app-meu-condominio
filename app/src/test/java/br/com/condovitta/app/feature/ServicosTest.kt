package br.com.condovitta.app.feature

import androidx.lifecycle.SavedStateHandle
import br.com.condovitta.app.FixedTime
import br.com.condovitta.app.MainDispatcherRule
import br.com.condovitta.app.data.demo.DemoAtendimentoRepository
import br.com.condovitta.app.data.demo.DemoGovernancaRepository
import br.com.condovitta.app.data.demo.DemoTransparenciaRepository
import br.com.condovitta.app.data.remote.ApiJson
import br.com.condovitta.app.data.remote.AtendimentoApi
import br.com.condovitta.app.data.remote.GovernancaApi
import br.com.condovitta.app.data.remote.TransparenciaApi
import br.com.condovitta.app.data.repository.RemoteAtendimentoRepository
import br.com.condovitta.app.data.repository.RemoteGovernancaRepository
import br.com.condovitta.app.data.repository.RemoteTransparenciaRepository
import br.com.condovitta.app.domain.ArquivoEnvio
import br.com.condovitta.app.domain.CategoriaChamado
import br.com.condovitta.app.domain.MotivoOcorrencia
import br.com.condovitta.app.domain.NovaOcorrencia
import br.com.condovitta.app.domain.NovoChamado
import br.com.condovitta.app.domain.OpcaoVoto
import br.com.condovitta.app.domain.Outcome
import br.com.condovitta.app.domain.PrioridadeChamado
import br.com.condovitta.app.domain.StatusChamado
import br.com.condovitta.app.domain.StatusOcorrencia
import br.com.condovitta.app.domain.TipoEvento
import br.com.condovitta.app.feature.servicos.CalendarioViewModel
import br.com.condovitta.app.feature.servicos.ChamadoViewModel
import br.com.condovitta.app.feature.servicos.DocumentoViewModel
import br.com.condovitta.app.feature.servicos.NovaOcorrenciaViewModel
import br.com.condovitta.app.feature.servicos.OcorrenciasViewModel
import br.com.condovitta.app.feature.servicos.PlenarioViewModel
import br.com.condovitta.app.feature.servicos.PrestacaoViewModel
import br.com.condovitta.app.feature.servicos.mimeDosBytes
import br.com.condovitta.app.feature.servicos.pct
import br.com.condovitta.app.ui.Messenger
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
class ServicosTest {

    @get:Rule val main = MainDispatcherRule()

    private lateinit var server: MockWebServer
    private val time = FixedTime()

    @Before fun setUp() { server = MockWebServer().apply { start() } }
    @After fun tearDown() = server.shutdown()

    private fun <T> api(c: Class<T>): T = Retrofit.Builder().baseUrl(server.url("/")).client(OkHttpClient())
        .addConverterFactory(ApiJson.asConverterFactory("application/json".toMediaType())).build().create(c)

    private fun json(body: String) = MockResponse().setHeader("Content-Type", "application/json").setBody(body)

    /* ------------------------------------------------------------------ API */

    @Test fun `chamados juntam a lista e o resumo`() = runTest {
        server.dispatcher = object : okhttp3.mockwebserver.Dispatcher() {
            override fun dispatch(request: okhttp3.mockwebserver.RecordedRequest) = when (request.path) {
                "/api/v1/chamados/resumo" -> json("""{"abertos":2,"aguardando":1,"resolvidosNoAno":7,"primeiraRespostaMinutos":95}""")
                else -> json("""[{"id":5,"titulo":"Vazamento","categoria":"HIDRAULICA","prioridade":"ALTA","status":"AGUARDANDO","abertoEm":"2026-11-03T09:15:00","ultimaDaAdministracao":true,"anexos":1,"canalNovo":"x"}]""")
            }
        }
        val r = RemoteAtendimentoRepository(api(AtendimentoApi::class.java)).chamados() as Outcome.Ok
        assertThat(r.value.resumo.aguardando).isEqualTo(1)
        val c = r.value.itens.single()
        assertThat(c.categoria).isEqualTo(CategoriaChamado.HIDRAULICA)
        assertThat(c.status).isEqualTo(StatusChamado.AGUARDANDO)
        assertThat(c.abertoEm?.hour).isEqualTo(9)
    }

    @Test fun `abrir chamado vai em multipart com dados em JSON e as fotos`() = runTest {
        server.enqueue(json("""{"chamado":{"id":9,"titulo":"Lâmpada","status":"ABERTO"},"mensagens":[{"id":1,"texto":"oi","minha":true},{"id":2,"texto":"nota","interna":true}]}"""))
        val repo = RemoteAtendimentoRepository(api(AtendimentoApi::class.java))
        val r = repo.abrirChamado(
            NovoChamado(" Lâmpada ", CategoriaChamado.MANUTENCAO, PrioridadeChamado.BAIXA, " ", "Corredor", true),
            listOf(ArquivoEnvio("foto.jpg", "image/jpeg", byteArrayOf(1, 2, 3)))
        ) as Outcome.Ok
        assertThat(r.value.mensagens).hasSize(1) // nota interna nunca aparece
        val req = server.takeRequest()
        assertThat(req.path).isEqualTo("/api/v1/chamados")
        assertThat(req.getHeader("Content-Type")).startsWith("multipart/form-data")
        val body = req.body.readUtf8()
        assertThat(body).contains("name=\"dados\"")
        assertThat(body).contains(""""titulo":"Lâmpada","categoria":"MANUTENCAO","prioridade":"BAIXA"""")
        assertThat(body).doesNotContain("\"local\"")
        assertThat(body).contains("name=\"arquivos\"; filename=\"foto.jpg\"")
    }

    @Test fun `ocorrencia geral nao manda unidade e sempre declara`() = runTest {
        server.enqueue(json("""{"minhas":[{"id":1,"motivo":"BARULHO_EXCESSIVO","status":"MULTA","descricao":"x","prazoDefesa":"2026-11-09"}],"contra":[{"id":2,"motivo":"NOVO_MOTIVO","valorMulta":150.5}]}"""))
        val r = RemoteAtendimentoRepository(api(AtendimentoApi::class.java)).registrarOcorrencia(
            NovaOcorrencia(true, "B-304", MotivoOcorrencia.FALTA_MANUTENCAO, time.now().withSecond(33), null, "Portão quebrado há dias", null), emptyList()
        ) as Outcome.Ok
        assertThat(r.value.minhas.single().status).isEqualTo(StatusOcorrencia.MULTA)
        assertThat(r.value.contra.single().motivo).isEqualTo(MotivoOcorrencia.OUTROS)
        assertThat(r.value.contra.single().valorMulta).isEqualTo(BigDecimal("150.5"))
        val body = server.takeRequest().body.readUtf8()
        assertThat(body).contains(""""geral":true""")
        assertThat(body).doesNotContain("B-304")
        assertThat(body).contains(""""ocorridaEm":"2026-11-04T10:00:00"""")
        assertThat(body).contains(""""declaracao":true""")
    }

    @Test fun `assinar documento manda o desenho como data URL e o codigo`() = runTest {
        server.enqueue(json("""{"codigo":"CV-1","metodo":"Assinatura desenhada","assinadoEm":"2026-11-04T10:00:00","concluido":true}"""))
        val r = RemoteGovernancaRepository(api(GovernancaApi::class.java)).assinar(7, byteArrayOf(1, 2), "123456") as Outcome.Ok
        assertThat(r.value.codigo).isEqualTo("CV-1")
        val req = server.takeRequest()
        assertThat(req.path).isEqualTo("/api/v1/documentos/7/assinar")
        val body = req.body.readUtf8()
        assertThat(body).contains("name=\"metodo\"")
        assertThat(body).contains("DESENHO")
        assertThat(body).contains("data:image/png;base64,AQI=")
        assertThat(body).contains("123456")
    }

    @Test fun `estado do plenario traz votos por item e o chat`() = runTest {
        server.enqueue(json("""{"sala":{"id":3,"titulo":"AGE","status":"ao_vivo","pauta":[{"id":10,"titulo":"Portão","status":"em_votacao","placar":{"aFavor":2,"contra":1,"abstencao":0,"pesoFavor":0.5}}],
            "quorum":{"presentes":40,"unidadesTotal":96,"fracao":0.41},"falando":{"nome":"Marina","unidade":"A-101"},
            "chat":[{"id":1,"nome":"Mesa","hora":"2026-11-04T19:02:00","texto":"Boa noite","mesa":true}]},
            "voce":{"presente":true,"podeVotar":true,"votos":{"10":{"opcao":"CONTRA","protocolo":"P1","hora":"2026-11-04T19:10:00","unidades":2}}},"agora":"2026-11-04T19:11:00"}"""))
        val e = (RemoteGovernancaRepository(api(GovernancaApi::class.java)).estado(3) as Outcome.Ok).value
        assertThat(e.aoVivo).isTrue()
        assertThat(e.itemAtual?.id).isEqualTo(10)
        assertThat(e.votos[10L]?.opcao).isEqualTo(OpcaoVoto.CONTRA)
        assertThat(e.votos[10L]?.unidades).isEqualTo(2)
        assertThat(e.falando).isEqualTo("Marina · A-101")
        assertThat(e.chat.single().hora?.minute).isEqualTo(2)
    }

    @Test fun `transparencia gas e calendario leem os formatos do backend`() = runTest {
        server.enqueue(json("""{"ym":"2026-10","publicados":[{"ym":"2026-09"},{"ym":"2026-10"}],"receitas":71840.0,"despesas":63900,"saldoMes":7940,"variacaoReceitas":3.2,
            "porCategoria":[{"chave":"p","nome":"Pessoal","cor":"#2563EB","valor":28400}],"lancamentos":[{"id":1,"data":"2026-10-05","descricao":"Folha","valor":21800,"temNota":true}]}"""))
        server.enqueue(json("""{"unidade":"B-204","ultima":{"leituraId":4,"mes":"2026-11","consumo":15.9,"valor":133.56,"temFoto":true},"meses":[{"mes":"2026-11","consumo":15.9}],"acimaDoPadrao":true}"""))
        server.enqueue(json("""[{"chave":"e1:2026-11-10","eventoId":1,"data":"2026-11-10","inicio":"08:00:00","fim":"12:00","titulo":"Elevadores","tipo":"MANUTENCAO","recorrencia":"NAO"},
            {"chave":"p2:2026-11-05","planoId":2,"data":"2026-11-05","titulo":"Bombas","tipo":"OUTRO"}]"""))
        val repo = RemoteTransparenciaRepository(api(TransparenciaApi::class.java))
        val t = (repo.transparencia(null) as Outcome.Ok).value
        assertThat(t.publicados).containsExactly("2026-10", "2026-09").inOrder()
        assertThat(t.lancamentos.single().data).isEqualTo(LocalDate.of(2026, 10, 5))
        assertThat(pct(t.variacaoReceitas)).isEqualTo("+3,2%")
        val g = (repo.meuGas() as Outcome.Ok).value
        assertThat(g.ultima?.temFoto).isTrue()
        assertThat(g.acimaDoPadrao).isTrue()
        val ev = (repo.calendario(LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30)) as Outcome.Ok).value
        assertThat(ev.map { it.titulo }).containsExactly("Bombas", "Elevadores").inOrder()
        assertThat(ev[1].inicio).isEqualTo(LocalTime.of(8, 0))
        assertThat(ev[1].recorrencia).isNull()
        assertThat(ev[0].tipo).isEqualTo(TipoEvento.SISTEMA)
        assertThat(server.takeRequest().path).isEqualTo("/api/v1/financeiro/morador/transparencia")
        server.takeRequest()
        assertThat(server.takeRequest().path).isEqualTo("/api/v1/calendario?de=2026-11-01&ate=2026-11-30")
    }

    @Test fun `tipo do arquivo baixado sai pelos primeiros bytes`() {
        assertThat(mimeDosBytes("%PDF-1.4".toByteArray()).first).isEqualTo("application/pdf")
        assertThat(mimeDosBytes(byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0, 0)).second).isEqualTo("jpg")
    }

    /* ------------------------------------------------------------------ ViewModels com os dados de exemplo */

    @Test fun `responder chamado aparece na conversa e avaliar fecha o resolvido`() = runTest {
        val repo = DemoAtendimentoRepository(time)
        val vm = ChamadoViewModel(SavedStateHandle(mapOf("id" to 101L)), repo, Messenger())
        vm.refresh(); advanceUntilIdle()
        var limpou = false
        vm.responder("Pode subir às 14h, estarei em casa.") { limpou = true }
        advanceUntilIdle()
        assertThat(limpou).isTrue()
        assertThat(vm.state.value.data!!.mensagens.last().texto).isEqualTo("Pode subir às 14h, estarei em casa.")

        val resolvido = ChamadoViewModel(SavedStateHandle(mapOf("id" to 98L)), repo, Messenger())
        resolvido.refresh(); advanceUntilIdle()
        assertThat(resolvido.state.value.data!!.podeAvaliar).isTrue()
        resolvido.avaliar(5, "Rápido!"); advanceUntilIdle()
        assertThat(resolvido.state.value.data!!.chamado.status).isEqualTo(StatusChamado.CONCLUIDO)
        assertThat(resolvido.state.value.data!!.podeReabrir).isTrue()
    }

    @Test fun `defesa so dentro do prazo e uma vez`() = runTest {
        val vm = OcorrenciasViewModel(DemoAtendimentoRepository(time), Messenger(), time)
        vm.refresh(); advanceUntilIdle()
        val aberta = vm.state.value.data!!.contra.first { it.podeDefender(time.today()) }
        vm.defesa(aberta.id, "curta"); advanceUntilIdle()
        assertThat(vm.envio.erro.value).isNotNull()
        vm.defesa(aberta.id, "O carro era de uma visita, que já foi orientada."); advanceUntilIdle()
        assertThat(vm.envio.ok.value).isEqualTo(1)
        assertThat(vm.state.value.data!!.contra.first { it.id == aberta.id }.podeDefender(time.today())).isFalse()
    }

    @Test fun `nova ocorrencia exige unidade declaracao e nao aceita a propria unidade`() = runTest {
        val vm = NovaOcorrenciaViewModel(DemoAtendimentoRepository(time), Messenger(), time)
        advanceUntilIdle()
        assertThat(vm.unidades.value).isNotEmpty()
        val base = NovaOcorrencia(false, null, MotivoOcorrencia.BARULHO_EXCESSIVO, time.now().minusHours(2), null, "Som alto depois das 23h", null)
        vm.registrar(base, declaracao = true); advanceUntilIdle()
        assertThat(vm.envio.erro.value).contains("unidade")
        vm.registrar(base.copy(unidade = "B-304"), declaracao = false); advanceUntilIdle()
        assertThat(vm.envio.erro.value).contains("verdadeiras")
        vm.registrar(base.copy(unidade = "B-204"), declaracao = true); advanceUntilIdle()
        assertThat(vm.envio.erro.value).contains("própria unidade")
        vm.registrar(base.copy(unidade = "B-304"), declaracao = true); advanceUntilIdle()
        assertThat(vm.envio.ok.value).isEqualTo(1)
    }

    @Test fun `documento com codigo so assina depois de pedir o codigo`() = runTest {
        val vm = DocumentoViewModel(SavedStateHandle(mapOf("id" to 501L)), DemoGovernancaRepository(time), Messenger())
        vm.refresh(); advanceUntilIdle()
        assertThat(vm.state.value.data!!.exigeCodigo).isTrue()
        vm.assinar(null, ""); advanceUntilIdle()
        assertThat(vm.envio.erro.value).contains("Desenhe")
        vm.assinar(byteArrayOf(1), "123456"); advanceUntilIdle()
        assertThat(vm.envio.erro.value).contains("código")
        vm.pedirCodigo(); advanceUntilIdle()
        assertThat(vm.codigoPara.value).isNotNull()
        vm.assinar(byteArrayOf(1), "123456"); advanceUntilIdle()
        assertThat(vm.resultado.value?.codigo).isNotNull()
        assertThat(vm.state.value.data!!.assinei).isTrue()
    }

    @Test fun `plenario vota depois da presenca e troca o voto`() = runTest {
        val vm = PlenarioViewModel(SavedStateHandle(mapOf("id" to 900L)), DemoGovernancaRepository(time), Messenger())
        vm.refresh(); advanceUntilIdle()
        val item = vm.state.value.data!!.itemAtual!!
        vm.votar(item.id, OpcaoVoto.A_FAVOR); advanceUntilIdle()
        assertThat(vm.state.value.data!!.votos).isEmpty() // sem presença não vota
        vm.presenca(); advanceUntilIdle()
        vm.votar(item.id, OpcaoVoto.A_FAVOR); advanceUntilIdle()
        val antes = vm.state.value.data!!.itemAtual!!.placar!!
        vm.votar(item.id, OpcaoVoto.CONTRA); advanceUntilIdle()
        val depois = vm.state.value.data!!
        assertThat(depois.votos[item.id]?.opcao).isEqualTo(OpcaoVoto.CONTRA)
        assertThat(depois.itemAtual!!.placar!!.aFavor).isEqualTo(antes.aFavor - 1)
        assertThat(depois.itemAtual!!.placar!!.contra).isEqualTo(antes.contra + 1)
    }

    @Test fun `prestacao troca de mes e calendario guarda o lembrete`() = runTest {
        val pv = PrestacaoViewModel(DemoTransparenciaRepository(time), Messenger())
        pv.refresh(); advanceUntilIdle()
        val t = pv.state.value.data!!
        assertThat(t.ym).isEqualTo("2026-10")
        pv.escolher(t.publicados.last()); advanceUntilIdle()
        assertThat(pv.state.value.data!!.ym).isEqualTo("2026-08")
        assertThat(pv.historico.value).isNotEmpty()

        val cv = CalendarioViewModel(DemoTransparenciaRepository(time), Messenger(), time)
        cv.refresh(); advanceUntilIdle()
        val hoje = cv.state.value.data!!.doDia(time.today())
        assertThat(hoje.map { it.tipo }).contains(TipoEvento.ASSEMBLEIA)
        val e = hoje.first()
        cv.lembrar(e, 60); advanceUntilIdle()
        assertThat(cv.state.value.data!!.lembrete(e)?.minutosAntes).isEqualTo(60)
        cv.lembrar(e, 0); advanceUntilIdle()
        assertThat(cv.state.value.data!!.lembrete(e)).isNull()
    }
}
