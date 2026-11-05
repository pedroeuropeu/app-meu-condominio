package br.com.condovitta.app.data.remote

import br.com.condovitta.app.core.common.TimeProvider
import br.com.condovitta.app.core.security.SessionStore
import br.com.condovitta.app.core.security.SessionTokens
import br.com.condovitta.app.domain.AppError
import br.com.condovitta.app.domain.Outcome
import kotlinx.serialization.json.Json
import okhttp3.Authenticator
import okhttp3.FormBody
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import retrofit2.HttpException
import timber.log.Timber
import java.io.IOException
import java.util.concurrent.CopyOnWriteArrayList

val ApiJson = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
    explicitNulls = false
    coerceInputValues = true
    isLenient = true
}

/** Avisos de sessão encerrada (refresh recusado, sessão revogada pelo servidor): o app volta para o login. */
class SessionEvents {
    private val listeners = CopyOnWriteArrayList<() -> Unit>()
    fun onExpired(listener: () -> Unit) { listeners += listener }
    fun expired() = listeners.forEach { it() }
}

/**
 * Condomínio em uso: vai em toda chamada /api pelo cabeçalho X-Condominio-Id (o TenantFilter do backend valida contra os
 * papéis da pessoa). Sem condomínio escolhido o cabeçalho não vai, e o backend usa o único que a pessoa tiver.
 */
class CondominioContext {
    @Volatile
    var condominioId: Long? = null
}

/**
 * Renova o access token no Keycloak com o refresh token (mesmo fluxo da web: grant_type=refresh_token, client público
 * condominio-client). Sincronizado: várias requisições com 401 ao mesmo tempo disparam uma única renovação.
 */
class TokenRefresher(
    private val client: OkHttpClient,
    private val tokenUrl: String,
    private val clientId: String,
    private val store: SessionStore,
    private val time: TimeProvider
) {
    @Synchronized
    fun refresh(staleAccessToken: String?): SessionTokens? {
        val current = store.current() ?: return null
        // outra thread já renovou enquanto esta esperava
        if (staleAccessToken != null && current.accessToken != staleAccessToken && !current.accessNeedsRefresh(time.epochMillis())) return current
        if (!current.canRefresh(time.epochMillis())) return null
        val form = FormBody.Builder()
            .add("grant_type", "refresh_token")
            .add("client_id", clientId)
            .add("refresh_token", current.refreshToken!!)
            .build()
        return try {
            client.newCall(Request.Builder().url(tokenUrl).post(form).build()).execute().use { resp ->
                if (!resp.isSuccessful) {
                    Timber.i("Renovação do token recusada (%d)", resp.code)
                    return null
                }
                val body = ApiJson.decodeFromString(KeycloakTokenResponse.serializer(), resp.body!!.string())
                val now = time.epochMillis()
                val tokens = current.copy(
                    accessToken = body.accessToken,
                    refreshToken = body.refreshToken ?: current.refreshToken,
                    accessExpiresAt = now + body.expiresIn * 1000,
                    refreshExpiresAt = if (body.refreshExpiresIn > 0) now + body.refreshExpiresIn * 1000 else 0
                )
                store.saveBlocking(tokens)
                tokens
            }
        } catch (e: IOException) {
            Timber.w(e, "Falha de rede ao renovar o token")
            null
        }
    }
}

/**
 * Identifica o app nas chamadas ("Condovitta/0.1.0 (Android 16; Pixel 8)"). O backend usa o User-Agent na auditoria e no
 * aviso de novo acesso.
 */
class UserAgentInterceptor(private val userAgent: String) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response = chain.proceed(
        chain.request().newBuilder().header("User-Agent", userAgent).header("Accept-Language", "pt-BR,pt;q=0.9").build()
    )

    companion object {
        fun value(version: String, release: String, model: String): String =
            "Condovitta/$version (Android $release; ${model.filter { it.code in 0x20..0x7e }.trim().ifEmpty { "Android" }})"
    }
}

/** Coloca o Bearer e o condomínio em uso em toda chamada, renovando o token antes de vencer. */
class AuthInterceptor(
    private val store: SessionStore,
    private val refresher: TokenRefresher,
    private val time: TimeProvider,
    private val condominio: CondominioContext
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val builder = chain.request().newBuilder().header("Accept", "application/json")
        condominio.condominioId?.let { builder.header(CONDOMINIO_HEADER, it.toString()) }
        var tokens = store.current()
        if (tokens != null) {
            if (tokens.accessNeedsRefresh(time.epochMillis())) tokens = refresher.refresh(tokens.accessToken) ?: tokens
            builder.header("Authorization", "Bearer ${tokens.accessToken}")
        }
        return chain.proceed(builder.build())
    }

    companion object {
        const val CONDOMINIO_HEADER = "X-Condominio-Id"
    }
}

/** 401: tenta renovar uma vez; se não der, encerra a sessão (o app volta para o login). */
class TokenAuthenticator(private val store: SessionStore, private val refresher: TokenRefresher, private val events: SessionEvents) : Authenticator {
    override fun authenticate(route: Route?, response: Response): Request? {
        if (response.priorResponse != null) return giveUp()
        val sent = response.request.header("Authorization")?.removePrefix("Bearer ")
        // sessão encerrada pelo servidor (bloqueio, sessão revogada): renovar não adianta
        val body = runCatching { response.peekBody(2048).string() }.getOrDefault("")
        if (body.contains("SESSION_ENDED") || body.contains("ACCOUNT_BLOCKED")) return giveUp()
        val tokens = refresher.refresh(sent) ?: return giveUp()
        return response.request.newBuilder().header("Authorization", "Bearer ${tokens.accessToken}").build()
    }

    private fun giveUp(): Request? {
        if (store.current() != null) events.expired()
        return null
    }
}

/** Executa uma chamada e traduz falhas para mensagens que a pessoa entende. */
suspend fun <T> apiCall(block: suspend () -> T): Outcome<T> = try {
    Outcome.Ok(block())
} catch (e: HttpException) {
    Outcome.Err(httpError(e.code(), e.response()?.errorBody()?.string()))
} catch (e: IOException) {
    Outcome.Err(AppError.Network)
} catch (e: kotlinx.serialization.SerializationException) {
    Timber.e(e, "Resposta inesperada da API")
    Outcome.Err(AppError.Server())
} catch (e: kotlin.coroutines.cancellation.CancellationException) {
    throw e
} catch (e: Exception) {
    Timber.e(e, "Erro inesperado")
    Outcome.Err(AppError.Server())
}

/** Respostas com corpo vazio (202/204). */
suspend fun apiUnit(block: suspend () -> retrofit2.Response<Unit>): Outcome<Unit> = when (val r = apiCall(block)) {
    is Outcome.Err -> r
    is Outcome.Ok -> if (r.value.isSuccessful) Outcome.Ok(Unit) else Outcome.Err(httpError(r.value.code(), r.value.errorBody()?.string()))
}

/** Traduz o corpo de erro do backend (AuthError ou ApiError {code, message, requestId}) para [AppError]. */
fun httpError(status: Int, body: String?): AppError {
    val err = body?.let { runCatching { ApiJson.decodeFromString(ApiErrorDto.serializer(), it) }.getOrNull() }
    val message = err?.message?.takeIf { it.isNotBlank() }
    return when (err?.code) {
        "INVALID_CREDENTIALS" -> AppError.InvalidCredentials(message ?: "E-mail ou senha incorretos.")
        "OTP_REQUIRED" -> AppError.OtpRequired(message ?: "Digite o código do seu app autenticador.")
        "OTP_INVALID" -> AppError.OtpRequired(message ?: "Código ou senha incorretos.", invalid = true)
        "ACCOUNT_BLOCKED", "ACCOUNT_SETUP" -> AppError.Blocked(message ?: "Seu acesso está bloqueado. Fale com a administração do condomínio.")
        "SEM_ACESSO_CONDOMINIO" -> AppError.NoAccess(message ?: "Você não tem acesso a este condomínio.")
        "FATURAMENTO_BLOQUEADO" -> AppError.BillingBlocked(message ?: "Acesso restrito pelo faturamento do condomínio.")
        "TOO_MANY_ATTEMPTS" -> AppError.TooManyAttempts(err.retryAfterSeconds)
        else -> when {
            status == 429 -> AppError.TooManyAttempts(err?.retryAfterSeconds)
            status == 401 -> AppError.Unauthorized
            status == 404 -> AppError.NotFound
            status in 400..499 -> AppError.Validation(message ?: "Confira os dados e tente de novo.")
            status == 503 -> AppError.Server(message ?: "O serviço está indisponível agora. Tente em instantes.")
            else -> AppError.Server(message ?: "O servidor não respondeu como esperado. Tente de novo.")
        }
    }
}
