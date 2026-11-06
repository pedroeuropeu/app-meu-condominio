package br.com.condovitta.app.feature

import android.content.Context
import br.com.condovitta.app.FixedTime
import br.com.condovitta.app.core.notify.CentralNotificacoes
import br.com.condovitta.app.data.demo.DemoRotinaRepository
import br.com.condovitta.app.data.local.SettingsRepository
import br.com.condovitta.app.data.remote.ApiJson
import br.com.condovitta.app.data.remote.NotificacoesApi
import br.com.condovitta.app.data.remote.RotinaApi
import br.com.condovitta.app.data.repository.RemoteNotificacoesRepository
import br.com.condovitta.app.data.repository.RemoteRotinaRepository
import br.com.condovitta.app.domain.AuthRepository
import br.com.condovitta.app.domain.CategoriaNotificacao
import br.com.condovitta.app.domain.Destino
import br.com.condovitta.app.domain.NovoRegistro
import br.com.condovitta.app.domain.Notificacao
import br.com.condovitta.app.domain.Outcome
import br.com.condovitta.app.domain.Rotas
import br.com.condovitta.app.domain.SentidoGaragem
import br.com.condovitta.app.domain.SessionState
import br.com.condovitta.app.domain.TipoRegistro
import br.com.condovitta.app.tempDataStore
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.math.BigDecimal

class Fase4Test {

    private lateinit var server: MockWebServer
    private val time = FixedTime()

    @Before fun setUp() { server = MockWebServer().apply { start() } }
    @After fun tearDown() = server.shutdown()

    private fun <T> api(c: Class<T>): T = Retrofit.Builder().baseUrl(server.url("/")).client(OkHttpClient())
        .addConverterFactory(ApiJson.asConverterFactory("application/json".toMediaType())).build().create(c)

    private fun lista(vararg ids: Long, lidas: Set<Long> = emptySet()) = """{"naoLidas":${ids.count { it !in lidas }},"itens":[${
        ids.joinToString(",") { """{"id":$it,"categoria":"ENCOMENDAS","titulo":"Encomenda $it","texto":"t","rota":"/app/morador/encomendas","lida":${it in lidas},"criadoEm":"2026-11-04T09:00:00"}""" }
    }]}"""

    private fun central(): Pair<CentralNotificacoes, MutableList<Notificacao>> {
        val auth = mockk<AuthRepository>(relaxed = true)
        every { auth.session } returns MutableStateFlow(SessionState.LoggedIn("ana.souza@email.com"))
        val c = CentralNotificacoes(mockk<Context>(relaxed = true), RemoteNotificacoesRepository(api(NotificacoesApi::class.java)), SettingsRepository(tempDataStore("n${System.nanoTime()}")), auth)
        val mostradas = mutableListOf<Notificacao>()
        c.exibir = { mostradas += it }
        return c to mostradas
    }

    @Test fun `primeira consulta so marca o ponto de partida e as seguintes mostram as novas`() = runTest {
        val (central, mostradas) = central()
        server.enqueue(MockResponse().setBody(lista(12, 11, 10)))
        central.sincronizar()
        assertThat(mostradas).isEmpty()
        assertThat(central.naoLidas.value).isEqualTo(3)
        assertThat(server.takeRequest().path).isEqualTo("/api/v1/notificacoes?limite=30&apenasNaoLidas=false")

        server.enqueue(MockResponse().setBody(lista(14, 13, 12, 11, 10, lidas = setOf(13))))
        val r = central.sincronizar()
        assertThat((r as Outcome.Ok).value).isEqualTo(1)
        assertThat(mostradas.map { it.id }).containsExactly(14L)

        server.enqueue(MockResponse().setBody(lista(14, 13, 12, 11, 10)))
        central.sincronizar()
        assertThat(mostradas).hasSize(1) // não repete
    }

    @Test fun `rotas da web viram telas do app`() {
        assertThat(Rotas.destino("/app/morador/encomendas")).isEqualTo(Destino.ENCOMENDAS)
        assertThat(Rotas.destino("/app/morador/financeiro")).isEqualTo(Destino.BOLETOS)
        assertThat(Rotas.destino("/app/portaria/acesso")).isEqualTo(Destino.PORTARIA_ACESSO)
        assertThat(Rotas.destino("/app/portaria/gas")).isEqualTo(Destino.PORTARIA_GAS)
        assertThat(Rotas.destino(null, CategoriaNotificacao.RESERVAS)).isEqualTo(Destino.RESERVAS)
        assertThat(Rotas.destino("/app/morador/assembleias", CategoriaNotificacao.ASSEMBLEIAS)).isEqualTo(Destino.NOTIFICACOES)
    }

    @Test fun `livro e leitura de gas no formato do backend`() = runTest {
        val repo = RemoteRotinaRepository(api(RotinaApi::class.java))
        server.enqueue(MockResponse().setBody("""{"id":1,"tipo":"OCORRENCIA","texto":"Portão aberto","travado":false}"""))
        repo.registrar(NovoRegistro(TipoRegistro.OCORRENCIA, " Portão aberto ", avisarSindico = true))
        assertThat(server.takeRequest().body.readUtf8()).isEqualTo("""{"tipo":"OCORRENCIA","texto":"Portão aberto","avisarSindico":true}""")

        server.enqueue(MockResponse().setBody("""{"unidadeId":8,"unidade":"B-204","anterior":120.000,"atual":125.420,"consumo":5.420}"""))
        val l = (repo.lancarGas("2026-11", 8, BigDecimal("125.420")) as Outcome.Ok).value
        val req = server.takeRequest()
        assertThat(req.path).isEqualTo("/api/v1/portaria/gas/2026-11/leituras/8")
        assertThat(req.body.readUtf8()).isEqualTo("""{"leitura":125.420}""")
        assertThat(l.consumo).isEqualTo(BigDecimal("5.420"))

        server.enqueue(MockResponse().setBody("""{"id":3,"placa":"SBA2C04","sentido":"SAIDA","deQuem":"B-204 · morador"}"""))
        val m = (repo.movimento("sba-2c04", SentidoGaragem.SAIDA, "Garagem") as Outcome.Ok).value
        assertThat(server.takeRequest().body.readUtf8()).contains("\"placa\":\"SBA2C04\"")
        assertThat(m.sentido).isEqualTo(SentidoGaragem.SAIDA)
    }

    @Test fun `demo do gas avisa leitura menor que a anterior`() = runTest {
        val repo = DemoRotinaRepository(time)
        val g = (repo.gas() as Outcome.Ok).value
        val pendente = g.unidades.first { !it.lida }
        val l = (repo.lancarGas(g.mes, pendente.unidadeId, pendente.anterior!!.subtract(BigDecimal.ONE)) as Outcome.Ok).value
        assertThat(l.alerta).contains("menor que a anterior")
        assertThat((repo.gas() as Outcome.Ok).value.lidas).isEqualTo(g.lidas + 1)
    }

    @Test fun `demo do livro exige frase e nao apaga registro de outro turno`() = runTest {
        val repo = DemoRotinaRepository(time)
        assertThat(repo.registrar(NovoRegistro(TipoRegistro.RONDA, "ok"))).isInstanceOf(Outcome.Err::class.java)
        val travado = (repo.livro() as Outcome.Ok).value.registros.first { it.travado }
        assertThat(repo.excluirRegistro(travado.id)).isInstanceOf(Outcome.Err::class.java)
    }
}
