package br.com.condovitta.app.feature

import androidx.lifecycle.SavedStateHandle
import br.com.condovitta.app.FixedTime
import br.com.condovitta.app.MainDispatcherRule
import br.com.condovitta.app.core.common.codigoDoQr
import br.com.condovitta.app.data.demo.DemoReservasRepository
import br.com.condovitta.app.data.remote.ApiJson
import br.com.condovitta.app.data.remote.ReservasApi
import br.com.condovitta.app.data.repository.RemoteReservasRepository
import br.com.condovitta.app.domain.ChaveReserva
import br.com.condovitta.app.domain.ModoAssinatura
import br.com.condovitta.app.domain.NovaReserva
import br.com.condovitta.app.domain.Outcome
import br.com.condovitta.app.domain.SituacaoDia
import br.com.condovitta.app.domain.StatusReserva
import br.com.condovitta.app.feature.reservas.NovaReservaViewModel
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
import java.time.YearMonth

@OptIn(ExperimentalCoroutinesApi::class)
class ReservasTest {

    @get:Rule val main = MainDispatcherRule()

    private lateinit var server: MockWebServer
    private val time = FixedTime() // quarta, 04/11/2026

    @Before fun setUp() { server = MockWebServer().apply { start() } }
    @After fun tearDown() = server.shutdown()

    private fun remote() = RemoteReservasRepository(
        Retrofit.Builder().baseUrl(server.url("/")).client(OkHttpClient())
            .addConverterFactory(ApiJson.asConverterFactory("application/json".toMediaType())).build().create(ReservasApi::class.java)
    )

    /* ---------------- contrato ---------------- */

    @Test fun `areas trazem espacos, horarios e manutencao`() = runTest {
        server.enqueue(MockResponse().setBody("""[{"id":1,"nome":"Churrasqueiras","icone":"local_fire_department","cor":"#EA580C","taxa":80.00,"maxConvidados":30,
            "antecedenciaMinDias":1,"antecedenciaMaxDias":60,"porTurno":true,"exigeTermo":true,"exigeAprovacao":false,"termoTexto":"Termo",
            "itens":[{"id":11,"nome":"Churrasqueira 1","capacidade":20,"taxa":80,"taxaEfetiva":80,"recursos":["Pia"],"ordem":1},
                     {"id":14,"nome":"Churrasqueira 4","capacidade":12,"ordem":4,"manutencao":{"motivo":"Grelha","hoje":true,"interditada":false}}],
            "turnos":[{"id":2,"nome":"Noite","inicio":"17:00:00","fim":"23:00:00","ordem":2},{"id":1,"nome":"Almoço","inicio":"10:00:00","fim":"16:00:00","ordem":1}]}]"""))
        val a = (remote().areas() as Outcome.Ok).value.single()
        assertThat(a.taxa).isEqualTo(BigDecimal("80.00"))
        assertThat(a.turnos.map { it.nome }).containsExactly("Almoço", "Noite").inOrder()
        assertThat(a.itens.last().manutencao).isEqualTo("Grelha")
        assertThat(a.itens.first().disponivel).isTrue()
    }

    @Test fun `disponibilidade por espaco e horario`() = runTest {
        server.enqueue(MockResponse().setBody("""{"areaId":1,"ano":2026,"mes":11,"hoje":"2026-11-04","dias":[
            {"data":"2026-11-07","status":"PARCIAL","itens":[{"itemId":11,"status":"LIVRE","turnos":[{"turnoId":1,"status":"OCUPADA","unidade":"C-202"},{"turnoId":2,"status":"LIVRE"}]}]},
            {"data":"2026-11-03","status":"PASSADO","itens":[]}]}"""))
        val d = (remote().disponibilidade(1, YearMonth.of(2026, 11)) as Outcome.Ok).value
        assertThat(server.takeRequest().path).isEqualTo("/api/v1/areas-comuns/1/disponibilidade?ano=2026&mes=11")
        val sab = d.dia(LocalDate.of(2026, 11, 7))!!
        assertThat(sab.para(11)).isEqualTo(SituacaoDia.PARCIAL)
        assertThat(sab.turnoLivre(11, 1)).isFalse()
        assertThat(sab.turnoLivre(11, 2)).isTrue()
        assertThat(d.dia(LocalDate.of(2026, 11, 3))!!.situacao.reservavel).isFalse()
    }

    @Test fun `reserva com assinatura desenhada vai em multipart`() = runTest {
        server.enqueue(MockResponse().setBody("""{"id":9,"data":"2026-11-14","areaNome":"Churrasqueiras","status":"CONFIRMADA","convidados":18}"""))
        remote().criar(NovaReserva(1, 13, 101, LocalDate.of(2026, 11, 14), 18, listOf("Pedro", " ", "Marta"), aceite = true, modo = ModoAssinatura.DESENHO, assinatura = byteArrayOf(1, 2)))
        val req = server.takeRequest()
        assertThat(req.getHeader("Content-Type")).startsWith("multipart/form-data")
        val body = req.body.readUtf8()
        assertThat(body).contains("\"modoAssinatura\":\"DESENHO\"")
        assertThat(body).contains("\"lista\":[{\"nome\":\"Pedro\"},{\"nome\":\"Marta\"}]")
        assertThat(body).contains("name=\"arquivo\"; filename=\"assinatura.png\"")
    }

    @Test fun `reserva com nome digitado vai em JSON`() = runTest {
        server.enqueue(MockResponse().setBody("""{"id":9,"data":"2026-11-14","status":"AGUARDANDO_APROVACAO"}"""))
        val r = (remote().criar(NovaReserva(2, 21, 202, LocalDate.of(2026, 12, 12), 60, aceite = true, modo = ModoAssinatura.DIGITADA, nomeDigitado = " Ana Souza ")) as Outcome.Ok).value
        assertThat(r.status).isEqualTo(StatusReserva.AGUARDANDO_APROVACAO)
        val req = server.takeRequest()
        assertThat(req.getHeader("Content-Type")).startsWith("application/json")
        assertThat(req.body.readUtf8()).contains("\"nomeDigitado\":\"Ana Souza\"")
    }

    @Test fun `codigo do QR`() {
        assertThat(codigoDoQr("418273")).isEqualTo("418273")
        assertThat(codigoDoQr("https://condovitta/v/418273?x=1")).isEqualTo("418273")
        assertThat(codigoDoQr("sem código")).isNull()
    }

    /* ---------------- demo + assistente ---------------- */

    @Test fun `demo marca passado, fora do prazo e manutencao`() = runTest {
        val repo = DemoReservasRepository(time)
        val d = (repo.disponibilidade(1, YearMonth.of(2026, 11)) as Outcome.Ok).value
        assertThat(d.dia(LocalDate.of(2026, 11, 3))!!.situacao).isEqualTo(SituacaoDia.PASSADO)
        assertThat(d.dia(LocalDate.of(2026, 11, 4))!!.situacao).isEqualTo(SituacaoDia.FORA) // antecedência mínima de 1 dia
        assertThat(d.dia(LocalDate.of(2026, 11, 20))!!.item(14)!!.situacao).isEqualTo(SituacaoDia.MANUTENCAO)
    }

    @Test fun `assistente percorre os 4 passos e confirma`() = runTest {
        val repo = DemoReservasRepository(time)
        val vm = NovaReservaViewModel(repo, time, SavedStateHandle(mapOf("areaId" to 4L)))
        advanceUntilIdle()
        val ui0 = vm.ui.value
        assertThat(ui0.itemId).isEqualTo(41L) // um espaço só: já vem escolhido
        vm.avancar()
        val disp = vm.ui.first { it.disp != null }.disp!!
        val (dia, turno) = disp.dias.firstNotNullOf { d -> d.item(41)?.turnos?.firstOrNull { it.situacao == SituacaoDia.LIVRE }?.let { d.data to it.turnoId } }
        vm.dia(dia)
        vm.avancar()
        vm.update { it.copy(turnoId = turno, convidados = 20) }
        assertThat(vm.ui.value.podeAvancar()).isFalse() // limite de 16
        vm.update { it.copy(convidados = 12, nomes = "Pedro\nMarta") }
        vm.avancar()
        assertThat(vm.ui.value.passo).isEqualTo(3)
        assertThat(vm.ui.value.podeAvancar()).isFalse() // falta aceitar o termo
        vm.update { it.copy(aceite = true, modo = ModoAssinatura.DIGITADA, nomeDigitado = "Ana Souza") }
        vm.confirmar(null)
        val r = vm.ui.first { it.criada != null }.criada!!
        assertThat(r.status).isEqualTo(StatusReserva.CONFIRMADA)
        assertThat(r.lista.map { it.nome }).containsExactly("Pedro", "Marta")
    }

    @Test fun `desenho sem assinatura nao confirma`() = runTest {
        val vm = NovaReservaViewModel(DemoReservasRepository(time), time, SavedStateHandle(mapOf("areaId" to 4L)))
        advanceUntilIdle()
        vm.update { it.copy(passo = 3, data = LocalDate.of(2026, 11, 20), turnoId = 401, aceite = true, modo = ModoAssinatura.DESENHO) }
        vm.confirmar(null)
        assertThat(vm.ui.value.erro).contains("Desenhe")
        assertThat(vm.ui.value.criada).isNull()
    }

    @Test fun `portaria faz check-in e controla a chave`() = runTest {
        val repo = DemoReservasRepository(time)
        val hoje = (repo.portaria() as Outcome.Ok).value.hoje
        assertThat(hoje).hasSize(3)
        val festa = hoje.first { it.unidade == "A-501" }
        assertThat(festa.chave).isEqualTo(ChaveReserva.ENTREGUE)
        val c = (repo.checkin(festa.id, festa.lista.first().id) as Outcome.Ok).value
        assertThat(c.chegouEm).isNotNull()
        val extra = (repo.incluirConvidado(festa.id, "Tio Zé") as Outcome.Ok).value
        assertThat(extra.extra).isTrue()
        val devolvida = (repo.devolucao(festa.id, true, null) as Outcome.Ok).value
        assertThat(devolvida.chave).isEqualTo(ChaveReserva.DEVOLVIDA)
    }

    @Test fun `cancelar e desfazer`() = runTest {
        val repo = DemoReservasRepository(time)
        val minha = (repo.minhas() as Outcome.Ok).value.first()
        val c = (repo.cancelar(minha.id) as Outcome.Ok).value
        assertThat(c.reserva.status).isEqualTo(StatusReserva.CANCELADA)
        assertThat(c.taxaMantida).isFalse()
        assertThat((repo.desfazerCancelamento(minha.id) as Outcome.Ok).value.status).isEqualTo(StatusReserva.CONFIRMADA)
    }
}
