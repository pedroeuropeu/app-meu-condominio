package br.com.condovitta.app.core.security

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import timber.log.Timber
import br.com.condovitta.app.core.common.TimeProvider
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

/** Tokens da sessão. Os instantes de expiração são em milissegundos desde 1970. */
@Serializable
data class SessionTokens(
    val accessToken: String,
    val refreshToken: String?,
    val accessExpiresAt: Long,
    val refreshExpiresAt: Long,
    val email: String
) {
    /** Renova 30 s antes de vencer, como o keycloak-js da web (updateToken(30)). */
    fun accessNeedsRefresh(nowMillis: Long): Boolean = nowMillis >= accessExpiresAt - 30_000

    fun canRefresh(nowMillis: Long): Boolean = refreshToken != null && (refreshExpiresAt == 0L || nowMillis < refreshExpiresAt)
}

/**
 * Guarda a sessão cifrada (Keystore) no DataStore e mantém uma cópia em memória para os interceptors do OkHttp,
 * que rodam fora do main thread e precisam ler o token sem suspender.
 */
@Singleton
class SessionStore @Inject constructor(
    @Named("session") private val dataStore: DataStore<Preferences>,
    private val cipher: TokenCipher,
    private val time: TimeProvider
) {
    private val json = Json { ignoreUnknownKeys = true }

    @Volatile
    private var cached: SessionTokens? = null

    @Volatile
    private var loaded = false

    fun current(): SessionTokens? {
        if (!loaded) runBlocking { load() }
        return cached
    }

    suspend fun load(): SessionTokens? {
        if (loaded) return cached
        cached = runCatching {
            dataStore.data.first()[KEY]?.let { b64 ->
                val plain = cipher.decrypt(java.util.Base64.getDecoder().decode(b64))
                json.decodeFromString(SessionTokens.serializer(), plain.decodeToString())
            }
        }.onFailure { Timber.w("Sessão salva ilegível; descartada") }.getOrNull()
        loaded = true
        return cached
    }

    suspend fun save(tokens: SessionTokens) {
        cached = tokens
        loaded = true
        val enc = cipher.encrypt(json.encodeToString(SessionTokens.serializer(), tokens).encodeToByteArray())
        dataStore.edit { it[KEY] = java.util.Base64.getEncoder().encodeToString(enc) }
    }

    /** Versão bloqueante para o renovador de token (thread do OkHttp). */
    fun saveBlocking(tokens: SessionTokens) = runBlocking { save(tokens) }

    suspend fun clear() {
        cached = null
        loaded = true
        dataStore.edit { it.remove(KEY) }
    }

    fun isAccessValid(): Boolean = current()?.let { !it.accessNeedsRefresh(time.epochMillis()) } ?: false

    private companion object {
        val KEY = stringPreferencesKey("session")
    }
}
