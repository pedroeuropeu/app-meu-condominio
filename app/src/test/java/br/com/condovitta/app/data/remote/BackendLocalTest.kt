package br.com.condovitta.app.data.remote

import br.com.condovitta.app.data.repository.RemoteAtendimentoRepository
import br.com.condovitta.app.data.repository.RemoteContextRepository
import br.com.condovitta.app.data.repository.RemoteGovernancaRepository
import br.com.condovitta.app.data.repository.RemoteMoradorRepository
import br.com.condovitta.app.data.repository.RemoteNotificacoesRepository
import br.com.condovitta.app.data.repository.RemotePortariaRepository
import br.com.condovitta.app.data.repository.RemoteReservasRepository
import br.com.condovitta.app.data.repository.RemoteRotinaRepository
import br.com.condovitta.app.data.repository.RemoteTransparenciaRepository
import br.com.condovitta.app.domain.Outcome
import br.com.condovitta.app.domain.Papel
import com.google.common.truth.Truth.assertWithMessage
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import org.junit.Assume.assumeTrue
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.time.LocalDate
import java.time.YearMonth
import java.util.concurrent.TimeUnit

/**
 * Contrato real com o gestao-condominio rodando nesta máquina (`make up`): entra com os usuários de desenvolvimento do
 * README do backend e chama cada leitura dos repositórios do app, com os mesmos DTOs. Pega campo renomeado, tipo errado
 * ou endpoint que não existe antes de chegar no aparelho.
 *
 *   CONDOVITTA_LOCAL=1 ./gradlew :app:testProdDebugUnitTest --tests '*BackendLocalTest*'
 *
 * Sem a variável o teste é pulado (o CI não tem o backend).
 */
class BackendLocalTest {

    private val base = System.getenv("CONDOVITTA_API") ?: "http://localhost:8090/"

    private fun json() = ApiJson.asConverterFactory("application/json".toMediaType())

    private class Sessao(val retrofit: Retrofit, val condominio: CondominioContext)

    private fun entrar(email: String, senha: String): Sessao = runBlocking {
        val publico = Retrofit.Builder().baseUrl(base).addConverterFactory(json()).build().create(PublicApi::class.java)
        val token = publico.login(LoginRequestDto(email, senha)).accessToken
        val condominio = CondominioContext()
        val client = OkHttpClient.Builder().readTimeout(30, TimeUnit.SECONDS).addInterceptor { chain ->
            val b = chain.request().newBuilder().header("Authorization", "Bearer $token")
            condominio.condominioId?.let { b.header("X-Condominio-Id", it.toString()) }
            chain.proceed(b.build())
        }.build()
        val r = Retrofit.Builder().baseUrl(base).client(client).addConverterFactory(json()).build()
        Sessao(r, condominio)
    }

    private val falhas = mutableListOf<String>()

    private suspend fun <T> confere(nome: String, block: suspend () -> Outcome<T>): T? = when (val r = block()) {
        is Outcome.Ok -> r.value
        is Outcome.Err -> { falhas += "$nome: ${r.error::class.simpleName} ${r.error.message}"; null }
    }

    private fun relatorio(quem: String) {
        assertWithMessage("$quem — chamadas que falharam contra o backend local:\n" + falhas.joinToString("\n")).that(falhas).isEmpty()
    }

    @Test fun portaria() = runBlocking {
        assumeTrue("defina CONDOVITTA_LOCAL=1 com o backend no ar", System.getenv("CONDOVITTA_LOCAL") == "1")
        val s = entrar("portaria@condominio.local", "Portaria@123")
        val me = confere("me") { RemoteContextRepository(s.retrofit.create(CondovittaApi::class.java)).me() }!!
        s.condominio.condominioId = me.memberships.first { it.papel == Papel.PORTARIA }.condominioId

        val p = RemotePortariaRepository(s.retrofit.create(PortariaApi::class.java), s.condominio)
        confere("painel") { p.painel() }
        confere("busca") { p.busca("204") }
        confere("unidades") { p.unidades() }
        val acesso = confere("acesso") { p.acesso() }
        confere("bloqueados") { p.bloqueados() }
        val enc = confere("encomendas") { p.encomendas() }
        enc?.aguardando?.firstOrNull()?.let { e -> confere("destinatarios") { p.destinatarios(e.unidadeId) } }
        acesso?.dentro?.firstOrNull { it.temFoto }?.acessoId?.let { a -> confere("verFotoAcesso") { p.verFotoAcesso(a) } }

        val rot = RemoteRotinaRepository(s.retrofit.create(RotinaApi::class.java))
        confere("livro") { rot.livro() }
        confere("veiculos") { rot.veiculos() }
        confere("gas") { rot.gas() }
        confere("reservasPortaria") { RemoteReservasRepository(s.retrofit.create(ReservasApi::class.java)).portaria() }
        confere("notificacoes") { RemoteNotificacoesRepository(s.retrofit.create(NotificacoesApi::class.java)).listar() }
        relatorio("portaria")
    }

    @Test fun morador() = runBlocking {
        assumeTrue("defina CONDOVITTA_LOCAL=1 com o backend no ar", System.getenv("CONDOVITTA_LOCAL") == "1")
        val s = entrar("morador@condominio.local", "Morador@123")
        val me = confere("me") { RemoteContextRepository(s.retrofit.create(CondovittaApi::class.java)).me() }!!
        s.condominio.condominioId = me.memberships.first { it.papel == Papel.MORADOR }.condominioId

        val m = RemoteMoradorRepository(s.retrofit.create(MoradorApi::class.java))
        confere("financeiro") { m.financeiro() }
        confere("visitas") { m.visitas() }
        confere("encomendas") { m.encomendas() }
        confere("avisos") { m.avisos() }

        val r = RemoteReservasRepository(s.retrofit.create(ReservasApi::class.java))
        val areas = confere("areas") { r.areas() }
        areas?.firstOrNull()?.let { a -> confere("disponibilidade") { r.disponibilidade(a.id, YearMonth.now()) } }
        confere("minhasReservas") { r.minhas() }

        val rot = RemoteRotinaRepository(s.retrofit.create(RotinaApi::class.java))
        confere("minhaUnidade") { rot.minhaUnidade() }
        confere("notificacoes") { RemoteNotificacoesRepository(s.retrofit.create(NotificacoesApi::class.java)).listar() }

        val at = RemoteAtendimentoRepository(s.retrofit.create(AtendimentoApi::class.java))
        val ch = confere("chamados") { at.chamados() }
        ch?.itens?.firstOrNull()?.let { c -> confere("chamado") { at.chamado(c.id) } }
        confere("ocorrencias") { at.ocorrencias() }
        confere("unidadesOcorrencia") { at.unidades() }

        val gov = RemoteGovernancaRepository(s.retrofit.create(GovernancaApi::class.java))
        val docs = confere("documentos") { gov.documentos() }
        docs?.pendentes?.firstOrNull()?.let { d -> confere("documento") { gov.documento(d.id) } }
        confere("assembleias") { gov.assembleias() }
        confere("plenarioAtual") { gov.plenarioAtual() }

        val tr = RemoteTransparenciaRepository(s.retrofit.create(TransparenciaApi::class.java))
        confere("transparencia") { tr.transparencia() }
        confere("balancetes") { tr.balancetes() }
        confere("meuGas") { tr.meuGas() }
        confere("calendario") { tr.calendario(LocalDate.now().withDayOfMonth(1), LocalDate.now().plusMonths(2)) }
        confere("lembretes") { tr.lembretes() }
        relatorio("morador")
    }
}
