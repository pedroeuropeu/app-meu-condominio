package br.com.condovitta.app.data.remote

import br.com.condovitta.app.FakeCipher
import br.com.condovitta.app.FixedTime
import br.com.condovitta.app.core.security.SessionStore
import br.com.condovitta.app.core.security.SessionTokens
import br.com.condovitta.app.data.local.SettingsRepository
import br.com.condovitta.app.data.repository.RemoteAuthRepository
import br.com.condovitta.app.data.repository.RemoteContextRepository
import br.com.condovitta.app.domain.AppError
import br.com.condovitta.app.domain.Outcome
import br.com.condovitta.app.domain.Papel
import br.com.condovitta.app.domain.SessionEnd
import br.com.condovitta.app.domain.SessionState
import br.com.condovitta.app.tempDataStore
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.io.IOException
import java.util.concurrent.atomic.AtomicInteger

class NetworkTest {

    private lateinit var server: MockWebServer
    private val time = FixedTime()
    private lateinit var store: SessionStore
    private val condominio = CondominioContext()

    @Before fun setUp() {
        server = MockWebServer().apply { start() }
        store = SessionStore(tempDataStore("session"), FakeCipher(), time)
    }

    @After fun tearDown() = server.shutdown()

    private fun tokens(access: String = "old", expiresIn: Long = 3_600_000) =
        SessionTokens(access, "refresh-1", time.epochMillis() + expiresIn, time.epochMillis() + 86_400_000, "ana.souza@email.com")

    private fun client(events: SessionEvents = SessionEvents()): OkHttpClient {
        val refresher = TokenRefresher(OkHttpClient(), server.url("/token").toString(), "condominio-client", store, time)
        return OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(store, refresher, time, condominio))
            .authenticator(TokenAuthenticator(store, refresher, events))
            .build()
    }

    private fun <T> retrofit(api: Class<T>, http: OkHttpClient = OkHttpClient()): T = Retrofit.Builder().baseUrl(server.url("/")).client(http)
        .addConverterFactory(ApiJson.asConverterFactory("application/json".toMediaType())).build().create(api)

    @Test fun `coloca o Bearer e o condominio em uso`() {
        runBlocking { store.save(tokens()) }
        condominio.condominioId = 7
        server.enqueue(MockResponse().setBody("{}"))
        client().newCall(Request.Builder().url(server.url("/api/v1/portaria/painel")).build()).execute().close()
        val req = server.takeRequest()
        assertThat(req.getHeader("Authorization")).isEqualTo("Bearer old")
        assertThat(req.getHeader("X-Condominio-Id")).isEqualTo("7")
    }

    @Test fun `sem condominio escolhido o cabecalho nao vai`() {
        runBlocking { store.save(tokens()) }
        server.enqueue(MockResponse().setBody("{}"))
        client().newCall(Request.Builder().url(server.url("/api/v1/me")).build()).execute().close()
        assertThat(server.takeRequest().getHeader("X-Condominio-Id")).isNull()
    }

    @Test fun `identifica o app no User-Agent`() {
        server.enqueue(MockResponse().setBody("{}"))
        val ua = UserAgentInterceptor.value("0.1.0", "16", "Pixel 8™")
        OkHttpClient.Builder().addInterceptor(UserAgentInterceptor(ua)).build().newCall(Request.Builder().url(server.url("/api/v1/me")).build()).execute().close()
        assertThat(server.takeRequest().getHeader("User-Agent")).isEqualTo("Condovitta/0.1.0 (Android 16; Pixel 8)")
    }

    @Test fun `401 renova o token no Keycloak e repete a chamada`() {
        runBlocking { store.save(tokens()) }
        server.enqueue(MockResponse().setResponseCode(401))
        server.enqueue(MockResponse().setBody("""{"access_token":"new","refresh_token":"refresh-2","expires_in":300,"refresh_expires_in":1800}"""))
        server.enqueue(MockResponse().setBody("""{"ok":true}"""))

        val resp = client().newCall(Request.Builder().url(server.url("/api/v1/me")).build()).execute()
        assertThat(resp.code).isEqualTo(200)
        resp.close()

        assertThat(server.takeRequest().getHeader("Authorization")).isEqualTo("Bearer old")
        val form = server.takeRequest().body.readUtf8()
        assertThat(form).contains("grant_type=refresh_token")
        assertThat(form).contains("client_id=condominio-client")
        assertThat(form).contains("refresh_token=refresh-1")
        assertThat(server.takeRequest().getHeader("Authorization")).isEqualTo("Bearer new")
        assertThat(store.current()?.refreshToken).isEqualTo("refresh-2")
    }

    @Test fun `token perto de vencer e renovado antes da chamada`() {
        runBlocking { store.save(tokens(expiresIn = 10_000)) }
        server.enqueue(MockResponse().setBody("""{"access_token":"fresh","expires_in":300}"""))
        server.enqueue(MockResponse().setBody("{}"))
        client().newCall(Request.Builder().url(server.url("/api/v1/me")).build()).execute().close()
        assertThat(server.takeRequest().path).isEqualTo("/token")
        assertThat(server.takeRequest().getHeader("Authorization")).isEqualTo("Bearer fresh")
    }

    @Test fun `refresh recusado encerra a sessao`() {
        runBlocking { store.save(tokens()) }
        val expired = AtomicInteger()
        val events = SessionEvents().apply { onExpired { expired.incrementAndGet() } }
        server.enqueue(MockResponse().setResponseCode(401))
        server.enqueue(MockResponse().setResponseCode(400).setBody("""{"error":"invalid_grant"}"""))

        val resp = client(events).newCall(Request.Builder().url(server.url("/api/v1/me")).build()).execute()
        assertThat(resp.code).isEqualTo(401)
        resp.close()
        assertThat(expired.get()).isEqualTo(1)
    }

    @Test fun `conta bloqueada nao tenta renovar`() {
        runBlocking { store.save(tokens()) }
        val expired = AtomicInteger()
        val events = SessionEvents().apply { onExpired { expired.incrementAndGet() } }
        server.enqueue(MockResponse().setResponseCode(401).setBody("""{"code":"ACCOUNT_BLOCKED","message":"Bloqueado"}"""))

        client(events).newCall(Request.Builder().url(server.url("/api/v1/me")).build()).execute().close()
        assertThat(server.requestCount).isEqualTo(1)
        assertThat(expired.get()).isEqualTo(1)
    }

    @Test fun `erros do backend viram mensagens para a pessoa`() {
        assertThat(httpError(401, """{"code":"INVALID_CREDENTIALS","message":"E-mail ou senha incorretos."}""")).isInstanceOf(AppError.InvalidCredentials::class.java)
        val otp = httpError(401, """{"code":"OTP_REQUIRED","message":"Digite o código do seu app autenticador."}""") as AppError.OtpRequired
        assertThat(otp.invalid).isFalse()
        assertThat((httpError(401, """{"code":"OTP_INVALID","message":"Código ou senha incorretos."}""") as AppError.OtpRequired).invalid).isTrue()
        val tooMany = httpError(429, """{"code":"TOO_MANY_ATTEMPTS","message":"x","retryAfterSeconds":120}""") as AppError.TooManyAttempts
        assertThat(tooMany.message).isEqualTo("Muitas tentativas. Tente de novo em 2 min.")
        assertThat(httpError(403, """{"code":"ACCOUNT_SETUP","message":"Falta concluir o seu cadastro."}""").message).isEqualTo("Falta concluir o seu cadastro.")
        assertThat(httpError(403, """{"code":"SEM_ACESSO_CONDOMINIO","message":"Você não tem acesso a este condomínio.","requestId":"r1"}""")).isInstanceOf(AppError.NoAccess::class.java)
        assertThat(httpError(403, """{"code":"FATURAMENTO_BLOQUEADO","message":"Fatura em aberto.","situacao":"GATE_PAGAR"}""").message).isEqualTo("Fatura em aberto.")
        assertThat(httpError(400, """{"code":"VALIDACAO","message":"Unidade obrigatória","requestId":"r2"}""").message).isEqualTo("Unidade obrigatória")
        assertThat(httpError(500, "<html>")).isInstanceOf(AppError.Server::class.java)
        assertThat(httpError(404, null)).isEqualTo(AppError.NotFound)
    }

    @Test fun `sem internet vira erro de rede`() = runTest {
        val r = apiCall<Unit> { throw IOException("offline") }
        assertThat((r as Outcome.Err).error).isEqualTo(AppError.Network)
    }

    @Test fun `login salva a sessao cifrada e o email`() = runTest {
        val settings = SettingsRepository(tempDataStore("settings"))
        val repo = RemoteAuthRepository(retrofit(PublicApi::class.java), store, settings, condominio, time, SessionEvents(), this)

        server.enqueue(MockResponse().setBody("""{"accessToken":"a1","refreshToken":"r1","idToken":"i1","expiresIn":300,"refreshExpiresIn":1800}"""))
        val r = repo.login(" ana.souza@email.com ", "senha", remember = true)

        assertThat(r).isInstanceOf(Outcome.Ok::class.java)
        val req = server.takeRequest()
        assertThat(req.path).isEqualTo("/public/auth/login")
        val body = req.body.readUtf8()
        assertThat(body).contains("\"email\":\"ana.souza@email.com\"")
        assertThat(body).contains("\"remember\":true")
        assertThat(body).doesNotContain("totp")
        assertThat(store.current()?.accessToken).isEqualTo("a1")
        assertThat(store.current()?.accessExpiresAt).isEqualTo(time.epochMillis() + 300_000)
        assertThat(repo.session.value).isEqualTo(SessionState.LoggedIn("ana.souza@email.com"))
        assertThat(settings.settings.first().lastEmail).isEqualTo("ana.souza@email.com")
    }

    @Test fun `login com 2FA manda o codigo`() = runTest {
        val repo = RemoteAuthRepository(retrofit(PublicApi::class.java), store, SettingsRepository(tempDataStore("s3")), condominio, time, SessionEvents(), this)
        server.enqueue(MockResponse().setResponseCode(401).setBody("""{"code":"OTP_REQUIRED","message":"Digite o código do seu app autenticador."}"""))
        val r = repo.login("sindico@condominio.local", "senha", remember = false)
        assertThat((r as Outcome.Err).error).isInstanceOf(AppError.OtpRequired::class.java)

        server.takeRequest()
        server.enqueue(MockResponse().setBody("""{"accessToken":"a1","expiresIn":300}"""))
        repo.login("sindico@condominio.local", "senha", remember = false, totp = "123456")
        assertThat(server.takeRequest().body.readUtf8()).contains("\"totp\":\"123456\"")
    }

    @Test fun `login com senha errada nao cria sessao`() = runTest {
        val repo = RemoteAuthRepository(retrofit(PublicApi::class.java), store, SettingsRepository(tempDataStore("s2")), condominio, time, SessionEvents(), this)
        server.enqueue(MockResponse().setResponseCode(401).setBody("""{"code":"INVALID_CREDENTIALS","message":"E-mail ou senha incorretos."}"""))

        val r = repo.login("ana.souza@email.com", "errada", remember = true)
        assertThat((r as Outcome.Err).error).isInstanceOf(AppError.InvalidCredentials::class.java)
        assertThat(store.current()).isNull()
    }

    @Test fun `passar o turno limpa a sessao e o condominio`() = runTest {
        val repo = RemoteAuthRepository(retrofit(PublicApi::class.java), store, SettingsRepository(tempDataStore("s4")), condominio, time, SessionEvents(), this)
        store.save(tokens())
        repo.restore()
        condominio.condominioId = 1
        server.enqueue(MockResponse().setResponseCode(204))

        repo.logout(SessionEnd.TURNO_PASSADO)
        testScheduler.advanceUntilIdle()

        assertThat(repo.session.value).isEqualTo(SessionState.LoggedOut)
        assertThat(repo.lastEnd.value).isEqualTo(SessionEnd.TURNO_PASSADO)
        assertThat(store.current()).isNull()
        assertThat(condominio.condominioId).isNull()
        val logout = server.takeRequest()
        assertThat(logout.path).isEqualTo("/public/auth/logout")
        assertThat(logout.body.readUtf8()).contains("\"refreshToken\":\"refresh-1\"")
    }

    @Test fun `me traz os papeis por condominio`() = runTest {
        server.enqueue(
            MockResponse().setBody(
                """{"id":5,"nome":"Roberto Lima","email":"sindico@condominio.local","admin":false,
                "memberships":[{"id":1,"condominioId":3,"condominioNome":"Vila do Sol 2","unidadeId":102,"unidade":"A-102","papel":"SINDICO","cargo":null,"permissoes":["X"]},
                               {"id":2,"condominioId":3,"condominioNome":"Vila do Sol 2","papel":"NOVO_PAPEL"}],
                "impersonation":null,"acesso":null}"""
            )
        )
        val me = (RemoteContextRepository(retrofit(CondovittaApi::class.java)).me() as Outcome.Ok).value
        assertThat(me.nome).isEqualTo("Roberto Lima")
        assertThat(me.memberships.map { it.papel }).containsExactly(Papel.SINDICO, Papel.DESCONHECIDO).inOrder()
        assertThat(me.memberships.first().unidade).isEqualTo("A-102")
    }
}
