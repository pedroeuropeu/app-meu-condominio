package br.com.condovitta.app.data.repository

import br.com.condovitta.app.core.common.TimeProvider
import br.com.condovitta.app.core.security.SessionStore
import br.com.condovitta.app.core.security.SessionTokens
import br.com.condovitta.app.data.local.SettingsRepository
import br.com.condovitta.app.data.remote.CondominioContext
import br.com.condovitta.app.data.remote.CondovittaApi
import br.com.condovitta.app.data.remote.ForgotPasswordRequestDto
import br.com.condovitta.app.data.remote.LoginRequestDto
import br.com.condovitta.app.data.remote.LogoutRequestDto
import br.com.condovitta.app.data.remote.PublicApi
import br.com.condovitta.app.data.remote.SessionEvents
import br.com.condovitta.app.data.remote.apiCall
import br.com.condovitta.app.data.remote.apiUnit
import br.com.condovitta.app.data.remote.toDomain
import br.com.condovitta.app.di.AppScope
import br.com.condovitta.app.domain.AuthRepository
import br.com.condovitta.app.domain.ContextRepository
import br.com.condovitta.app.domain.Me
import br.com.condovitta.app.domain.Outcome
import br.com.condovitta.app.domain.SessionEnd
import br.com.condovitta.app.domain.SessionState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/** Login pela tela própria do app (POST /public/auth/login → tokens do Keycloak), sessão cifrada no aparelho. */
@Singleton
class RemoteAuthRepository @Inject constructor(
    private val api: PublicApi,
    private val store: SessionStore,
    private val settings: SettingsRepository,
    private val condominio: CondominioContext,
    private val time: TimeProvider,
    events: SessionEvents,
    @AppScope private val scope: CoroutineScope
) : AuthRepository {

    private val _session = MutableStateFlow<SessionState>(SessionState.Unknown)
    override val session: StateFlow<SessionState> = _session.asStateFlow()
    private val _lastEnd = MutableStateFlow<SessionEnd?>(null)
    override val lastEnd: StateFlow<SessionEnd?> = _lastEnd.asStateFlow()

    init {
        events.onExpired { scope.launch { end(SessionEnd.EXPIRADA) } }
    }

    override suspend fun restore() {
        val tokens = store.load()
        _session.value = if (tokens != null && (tokens.canRefresh(time.epochMillis()) || !tokens.accessNeedsRefresh(time.epochMillis())))
            SessionState.LoggedIn(tokens.email) else {
            if (tokens != null) store.clear()
            SessionState.LoggedOut
        }
    }

    override suspend fun login(email: String, password: String, remember: Boolean, totp: String?): Outcome<Unit> {
        val mail = email.trim()
        return when (val r = apiCall { api.login(LoginRequestDto(mail, password, remember, totp?.takeIf { it.isNotBlank() })) }) {
            is Outcome.Err -> r
            is Outcome.Ok -> {
                val now = time.epochMillis()
                val t = r.value
                store.save(
                    SessionTokens(
                        accessToken = t.accessToken,
                        refreshToken = t.refreshToken,
                        accessExpiresAt = now + t.expiresIn * 1000L,
                        refreshExpiresAt = if (t.refreshExpiresIn > 0) now + t.refreshExpiresIn * 1000L else 0,
                        email = mail
                    )
                )
                settings.setLastEmail(mail)
                _lastEnd.value = null
                _session.value = SessionState.LoggedIn(mail)
                Outcome.Ok(Unit)
            }
        }
    }

    override suspend fun forgotPassword(email: String): Outcome<Unit> = apiUnit { api.forgotPassword(ForgotPasswordRequestDto(email.trim())) }

    override suspend fun logout(reason: SessionEnd?) {
        val refresh = store.current()?.refreshToken
        end(reason)
        // encerra no Keycloak sem segurar a pessoa na tela (se falhar, o token vence sozinho)
        scope.launch { apiUnit { api.logout(LogoutRequestDto(refresh)) } }
    }

    private suspend fun end(reason: SessionEnd?) {
        if (_session.value is SessionState.LoggedOut) return
        store.clear()
        condominio.condominioId = null
        _lastEnd.value = reason
        _session.value = SessionState.LoggedOut
    }
}

@Singleton
class RemoteContextRepository @Inject constructor(private val api: CondovittaApi) : ContextRepository {
    override suspend fun me(): Outcome<Me> = apiCall { api.me().toDomain() }
}
