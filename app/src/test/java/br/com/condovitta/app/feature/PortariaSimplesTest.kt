package br.com.condovitta.app.feature

import androidx.lifecycle.SavedStateHandle
import br.com.condovitta.app.FixedTime
import br.com.condovitta.app.MainDispatcherRule
import br.com.condovitta.app.data.demo.DemoPortariaRepository
import br.com.condovitta.app.data.remote.ApiJson
import br.com.condovitta.app.data.remote.CondominioContext
import br.com.condovitta.app.data.remote.PortariaApi
import br.com.condovitta.app.data.repository.RemotePortariaRepository
import br.com.condovitta.app.domain.Outcome
import br.com.condovitta.app.domain.Portaria
import br.com.condovitta.app.domain.StatusVisita
import br.com.condovitta.app.domain.UnidadeResumo
import br.com.condovitta.app.feature.portaria.FiltroVisitas
import br.com.condovitta.app.feature.portaria.VisitantesViewModel
import br.com.condovitta.app.feature.portaria.entrarComFoto
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

/** Portaria simplificada: teclado de unidade, liberar de um toque e a foto de quem entrou. */
@OptIn(ExperimentalCoroutinesApi::class)
class PortariaSimplesTest {

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

    private val unidades = listOf(
        UnidadeResumo(1, "A-102", "A", "Roberto Lima", 2),
        UnidadeResumo(2, "B-204", "B", "Ana Souza", 2),
        UnidadeResumo(3, "B-024", "B", "Pedro Alves", 1),
        UnidadeResumo(4, "Casa 7", null, "Marta", 1)
    )

    @Test fun `teclado acha a unidade pelo bloco e numero`() {
        assertThat(Portaria.acharUnidade(unidades, "B", "204")?.id).isEqualTo(2)
        assertThat(Portaria.acharUnidade(unidades, "B", "24")?.id).isEqualTo(3)
        assertThat(Portaria.acharUnidade(unidades, "A", "204")).isNull()
        assertThat(Portaria.acharUnidade(unidades, null, "102")?.id).isEqualTo(1)
        assertThat(Portaria.acharUnidade(unidades, "B", "")).isNull()
        assertThat(Portaria.numeroDa(unidades[1])).isEqualTo("204")
    }

    @Test fun `foto do acesso vai em multipart e volta com temFoto`() = runTest {
        server.enqueue(MockResponse().setBody("""{"visitanteId":7,"acessoId":507,"nome":"Rafael Torres","status":"dentro","temFoto":true}"""))
        val v = (remote().fotoAcesso(507, byteArrayOf(9, 8, 7)) as Outcome.Ok).value
        assertThat(v.temFoto).isTrue()
        val req = server.takeRequest()
        assertThat(req.path).isEqualTo("/api/v1/portaria/acessos/507/foto")
        assertThat(req.getHeader("Content-Type")).startsWith("multipart/form-data")
        assertThat(req.body.readUtf8()).contains("name=\"foto\"; filename=\"visitante.jpg\"")
    }

    @Test fun `sem foto o backend responde 404 e a tela recebe nulo`() = runTest {
        server.enqueue(MockResponse().setResponseCode(404).setBody("""{"code":"NAO_ENCONTRADO","message":"Esta entrada não tem foto."}"""))
        assertThat((remote().verFotoAcesso(507) as Outcome.Ok).value).isNull()
    }

    @Test fun `liberar com foto entra e guarda a foto`() = runTest {
        val repo = DemoPortariaRepository(time)
        val esperado = (repo.acesso() as Outcome.Ok).value.esperados.first { it.status == StatusVisita.ESPERADO }
        val (r, fotoFalhou) = repo.entrarComFoto(esperado.visitanteId, null, byteArrayOf(1, 2))
        val v = (r as Outcome.Ok).value
        assertThat(fotoFalhou).isFalse()
        assertThat(v.status).isEqualTo(StatusVisita.DENTRO)
        assertThat(v.temFoto).isTrue()
        assertThat((repo.verFotoAcesso(v.acessoId!!) as Outcome.Ok).value).isEqualTo(byteArrayOf(1, 2))
    }

    @Test fun `lista libera de um toque e abre em No condominio pelo botao do Inicio`() = runTest {
        val repo = DemoPortariaRepository(time)
        val dentro = VisitantesViewModel(repo, Messenger(), SavedStateHandle(mapOf("dentro" to true)))
        assertThat(dentro.filtro.value).isEqualTo(FiltroVisitas.DENTRO)

        val vm = VisitantesViewModel(repo, Messenger(), SavedStateHandle())
        assertThat(vm.filtro.value).isEqualTo(FiltroVisitas.ESPERADOS)
        vm.refresh()
        advanceUntilIdle()
        val alvo = vm.state.value.data!!.esperados.first { it.status == StatusVisita.ESPERADO }
        vm.liberar(alvo, foto = null)
        advanceUntilIdle()
        assertThat(vm.state.value.data!!.dentro.map { it.visitanteId }).contains(alvo.visitanteId)
        assertThat(vm.ocupado.value).isNull()

        val agora = vm.state.value.data!!.dentro.first { it.visitanteId == alvo.visitanteId }
        vm.saida(agora)
        advanceUntilIdle()
        assertThat(vm.state.value.data!!.historico.map { it.visitanteId }).contains(alvo.visitanteId)
    }
}
