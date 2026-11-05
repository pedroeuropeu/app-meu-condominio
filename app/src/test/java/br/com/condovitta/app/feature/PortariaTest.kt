package br.com.condovitta.app.feature

import androidx.lifecycle.SavedStateHandle
import br.com.condovitta.app.FixedTime
import br.com.condovitta.app.MainDispatcherRule
import br.com.condovitta.app.data.demo.DemoPortariaRepository
import br.com.condovitta.app.data.remote.ApiJson
import br.com.condovitta.app.data.remote.CondominioContext
import br.com.condovitta.app.data.remote.PortariaApi
import br.com.condovitta.app.data.repository.RemotePortariaRepository
import br.com.condovitta.app.domain.AppError
import br.com.condovitta.app.domain.Entrega
import br.com.condovitta.app.domain.NovaEncomenda
import br.com.condovitta.app.domain.NovaVisita
import br.com.condovitta.app.domain.Outcome
import br.com.condovitta.app.domain.Portaria
import br.com.condovitta.app.domain.StatusVisita
import br.com.condovitta.app.domain.TamanhoEncomenda
import br.com.condovitta.app.domain.TipoRetirada
import br.com.condovitta.app.domain.TipoVisitante
import br.com.condovitta.app.domain.ViaLiberacao
import br.com.condovitta.app.feature.portaria.NovaVisitaViewModel
import br.com.condovitta.app.feature.portaria.VisitanteViewModel
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
import java.time.LocalDateTime
import java.time.LocalTime

@OptIn(ExperimentalCoroutinesApi::class)
class PortariaTest {

    @get:Rule val main = MainDispatcherRule()

    private lateinit var server: MockWebServer
    private val time = FixedTime()

    @Before fun setUp() { server = MockWebServer().apply { start() } }
    @After fun tearDown() = server.shutdown()

    private fun remote(): RemotePortariaRepository {
        val api = Retrofit.Builder().baseUrl(server.url("/")).client(OkHttpClient())
            .addConverterFactory(ApiJson.asConverterFactory("application/json".toMediaType())).build().create(PortariaApi::class.java)
        return RemotePortariaRepository(api, CondominioContext())
    }

    /* ---------------- contrato com o backend ---------------- */

    private val visitaJson = """{"visitanteId":7,"acessoId":507,"nome":"Rafael Torres","documento":"CPF ***.320.118-**","tipo":"VISITA",
        "unidadeId":19,"unidade":"D-201","morador":"Rafael Torres","placa":null,"status":"aguardando","data":"2026-11-04",
        "inicio":"09:38:00","fim":"12:00:00","entradaEm":null,"origem":"PORTARIA","via":null,"consultadoEm":"2026-11-04T09:38:12.345",
        "autorizadoPor":"Carlos Mendes","avisar":true,"campoNovo":"ignorado"}"""

    @Test fun `listas de acesso viram visitas com status e horarios`() = runTest {
        server.enqueue(MockResponse().setBody("""{"esperados":[$visitaJson],"dentro":[],"historico":[],"bloqueados":2}"""))
        val l = (remote().acesso() as Outcome.Ok).value
        val v = l.esperados.single()
        assertThat(v.status).isEqualTo(StatusVisita.AGUARDANDO)
        assertThat(v.janela).isEqualTo("09:38 – 12:00")
        assertThat(v.daPortaria).isTrue()
        assertThat(v.consultadoEm).isEqualTo(LocalDateTime.of(2026, 11, 4, 9, 38, 12, 345_000_000))
        assertThat(l.bloqueados).isEqualTo(2)
        assertThat(server.takeRequest().path).isEqualTo("/api/v1/portaria/acesso")
    }

    @Test fun `nova visita manda o formulario do backend`() = runTest {
        server.enqueue(MockResponse().setBody(visitaJson))
        remote().novaVisita(NovaVisita("  Lucas Andrade ", TipoVisitante.PRESTADOR, 19, placa = "rta-4b21", inicio = LocalTime.of(9, 5), fim = LocalTime.of(13, 5), consultar = true))
        val req = server.takeRequest()
        assertThat(req.path).isEqualTo("/api/v1/portaria/visitantes")
        val body = req.body.readUtf8()
        assertThat(body).contains("\"nome\":\"Lucas Andrade\"")
        assertThat(body).contains("\"tipo\":\"PRESTADOR\"")
        assertThat(body).contains("\"placa\":\"RTA4B21\"")
        assertThat(body).contains("\"inicio\":\"09:05\"")
        assertThat(body).contains("\"consultar\":true")
    }

    @Test fun `liberar pelo interfone manda a via`() = runTest {
        server.enqueue(MockResponse().setBody(visitaJson))
        remote().entrada(7, ViaLiberacao.INTERFONE)
        val req = server.takeRequest()
        assertThat(req.path).isEqualTo("/api/v1/portaria/visitantes/7/entrada")
        assertThat(req.body.readUtf8()).isEqualTo("""{"via":"INTERFONE"}""")
    }

    @Test fun `registrar encomenda vai em multipart com dados e foto`() = runTest {
        server.enqueue(MockResponse().setBody("""{"id":40,"unidadeId":8,"unidade":"B-204","transportadora":"Amazon","situacao":"AGUARDANDO","temFoto":true}"""))
        val e = (remote().registrarEncomenda(NovaEncomenda(8, "Ana Souza", "Amazon", TamanhoEncomenda.MEDIO, "Prateleira B", false), byteArrayOf(1, 2, 3)) as Outcome.Ok).value
        assertThat(e.temFoto).isTrue()
        val req = server.takeRequest()
        assertThat(req.getHeader("Content-Type")).startsWith("multipart/form-data")
        val body = req.body.readUtf8()
        assertThat(body).contains("name=\"dados\"")
        assertThat(body).contains("\"tamanho\":\"MEDIO\"")
        assertThat(body).contains("name=\"foto\"; filename=\"encomenda.jpg\"")
        assertThat(body).contains("Content-Type: image/jpeg")
    }

    @Test fun `entrega manda a assinatura como data URL PNG`() = runTest {
        server.enqueue(MockResponse().setBody("""{"id":40,"unidadeId":8,"unidade":"B-204","situacao":"RETIRADA","retiradaPor":"Ana Souza"}"""))
        val e = (remote().entregar(40, Entrega(TipoRetirada.MORADOR, codigo = "48-21", assinatura = byteArrayOf(9, 9))) as Outcome.Ok).value
        assertThat(e.retirada).isTrue()
        val body = server.takeRequest().body.readUtf8()
        assertThat(body).contains("\"modo\":\"MORADOR\"")
        assertThat(body).contains("\"codigo\":\"4821\"")
        assertThat(body).contains("\"assinatura\":\"data:image/png;base64,CQk=\"")
    }

    @Test fun `codigo errado vem como mensagem do backend`() = runTest {
        server.enqueue(MockResponse().setResponseCode(400).setBody("""{"code":"VALIDACAO","message":"Código de retirada não confere. Peça para o morador conferir no app.","requestId":"x"}"""))
        val r = remote().entregar(40, Entrega(TipoRetirada.MORADOR, codigo = "0000", assinatura = byteArrayOf(1)))
        assertThat((r as Outcome.Err).error.message).startsWith("Código de retirada não confere")
    }

    @Test fun `encomenda sem foto nao e erro`() = runTest {
        server.enqueue(MockResponse().setResponseCode(404).setBody("""{"code":"NAO_ENCONTRADO","message":"Arquivo não encontrado."}"""))
        assertThat(remote().fotoEncomenda(40)).isEqualTo(Outcome.Ok(null))
    }

    @Test fun `unidades ficam em cache por condominio`() = runTest {
        server.enqueue(MockResponse().setBody("""[{"id":8,"rotulo":"B-204","bloco":"B","responsavel":"Ana Souza","moradores":2}]"""))
        val repo = remote()
        repo.unidades()
        val u = (repo.unidades() as Outcome.Ok).value
        assertThat(u.single().rotulo).isEqualTo("B-204")
        assertThat(server.requestCount).isEqualTo(1)
    }

    /* ---------------- regras (demo, as mesmas do backend) ---------------- */

    @Test fun `demo barra visitante bloqueado`() = runTest {
        val repo = DemoPortariaRepository(time)
        val r = repo.novaVisita(NovaVisita("Gilberto Ramos", TipoVisitante.VISITA, 1, inicio = LocalTime.of(10, 0), fim = LocalTime.of(12, 0)))
        assertThat((r as Outcome.Err).error.message).contains("bloqueado")
    }

    @Test fun `demo entrada, saida e desfazer`() = runTest {
        val repo = DemoPortariaRepository(time)
        val dentro = (repo.entrada(1) as Outcome.Ok).value
        assertThat(dentro.status).isEqualTo(StatusVisita.DENTRO)
        val saiu = (repo.saida(dentro.acessoId!!) as Outcome.Ok).value
        assertThat(saiu.status).isEqualTo(StatusVisita.SAIU)
        repo.desfazer(saiu.acessoId!!)
        val l = (repo.acesso() as Outcome.Ok).value
        assertThat(l.dentro.map { it.nome }).contains("Lucas Prado")
    }

    @Test fun `demo entrega confere o codigo e exige assinatura`() = runTest {
        val repo = DemoPortariaRepository(time)
        val errado = repo.entregar(1, Entrega(TipoRetirada.MORADOR, codigo = "0000", assinatura = byteArrayOf(1)))
        assertThat((errado as Outcome.Err).error).isInstanceOf(AppError.Validation::class.java)
        val semAssinatura = repo.entregar(1, Entrega(TipoRetirada.MORADOR, codigo = repo.codigo(1), assinatura = byteArrayOf()))
        assertThat((semAssinatura as Outcome.Err).error.message).contains("assinar")
        val ok = (repo.entregar(1, Entrega(TipoRetirada.MORADOR, codigo = repo.codigo(1), assinatura = byteArrayOf(1))) as Outcome.Ok).value
        assertThat(ok.retirada).isTrue()
        assertThat(ok.retiradaPor).isEqualTo("Ana Souza")
        val terceiro = repo.entregar(2, Entrega(TipoRetirada.TERCEIRO, nome = "Pedro", documento = null, assinatura = byteArrayOf(1)))
        assertThat((terceiro as Outcome.Err).error.message).contains("documento")
    }

    @Test fun `demo painel conta perecivel e parada`() = runTest {
        val p = (DemoPortariaRepository(time).painel() as Outcome.Ok).value
        assertThat(p.pereciveis.map { it.unidade }).containsExactly("D-601")
        assertThat(p.encomendasParadas7).isEqualTo(1)
        assertThat(p.dentro).hasSize(3)
    }

    @Test fun `placa formatada e encomenda atrasada`() {
        assertThat(Portaria.formatarPlaca("abc1234")).isEqualTo("ABC-1234")
        assertThat(Portaria.formatarPlaca("rta4b21")).isEqualTo("RTA4B21")
        assertThat(Portaria.formatarPlaca(" ")).isNull()
    }

    /* ---------------- ViewModels ---------------- */

    @Test fun `liberar no detalhe atualiza e oferece desfazer`() = runTest {
        val repo = DemoPortariaRepository(time)
        val messenger = Messenger()
        val vm = VisitanteViewModel(repo, messenger, SavedStateHandle(mapOf("visitanteId" to 2L)))
        vm.refresh()
        advanceUntilIdle()
        assertThat(vm.state.value.data?.nome).isEqualTo("Juliana Martins")
        vm.liberar()
        advanceUntilIdle()
        assertThat(vm.state.value.data?.status).isEqualTo(StatusVisita.DENTRO)
    }

    @Test fun `nova visita libera pelo interfone e fecha`() = runTest {
        val repo = DemoPortariaRepository(time)
        val vm = NovaVisitaViewModel(repo, Messenger(), time)
        advanceUntilIdle()
        vm.update { it.copy(unidade = repo.unidades.first { u -> u.rotulo == "C-303" }, nome = "Lucas Andrade") }
        vm.salvar(consultar = false)
        val id = vm.done.first { it != null }!!
        val v = (repo.acesso() as Outcome.Ok).value.dentro.first { it.visitanteId == id }
        assertThat(v.via).isEqualTo(ViaLiberacao.INTERFONE)
        assertThat(v.daPortaria).isTrue()
    }

    @Test fun `nova visita pedindo ao morador fica aguardando`() = runTest {
        val repo = DemoPortariaRepository(time)
        val vm = NovaVisitaViewModel(repo, Messenger(), time)
        advanceUntilIdle()
        vm.update { it.copy(unidade = repo.unidades.first(), nome = "Técnico Vivo") }
        vm.salvar(consultar = true)
        val id = vm.done.first { it != null }!!
        val v = (repo.acesso() as Outcome.Ok).value.esperados.first { it.visitanteId == id }
        assertThat(v.status).isEqualTo(StatusVisita.AGUARDANDO)
    }
}
