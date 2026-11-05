package br.com.condovitta.app.data.demo

import br.com.condovitta.app.data.local.SettingsRepository
import br.com.condovitta.app.domain.AppError
import br.com.condovitta.app.domain.AuthRepository
import br.com.condovitta.app.domain.ContextRepository
import br.com.condovitta.app.domain.Me
import br.com.condovitta.app.domain.Membership
import br.com.condovitta.app.domain.Outcome
import br.com.condovitta.app.domain.Papel
import br.com.condovitta.app.domain.SessionEnd
import br.com.condovitta.app.domain.SessionState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Dados de exemplo do Residencial Vila do Sol 2 (os mesmos do prototype/data.js). Qualquer senha entra; o e-mail decide
 * quem você é: com "portaria" → Carlos Mendes (porteiro); com "sindico" → síndico que também mora (escolhe o modo);
 * qualquer outro → Ana Souza, moradora do B-204.
 */
object DemoData {
    const val CONDOMINIO_ID = 1L
    const val CONDOMINIO_NOME = "Residencial Vila do Sol 2"

    fun me(email: String): Me {
        val e = email.lowercase()
        return when {
            "portaria" in e -> Me(2, "Carlos Mendes", email, false, listOf(vinculo(20, Papel.PORTARIA)))
            "sindico" in e -> Me(3, "Roberto Lima", email, false, listOf(vinculo(30, Papel.SINDICO, 102, "A-102"), vinculo(31, Papel.MORADOR, 102, "A-102")))
            else -> Me(1, "Ana Souza", email, false, listOf(vinculo(10, Papel.MORADOR, 204, "B-204")))
        }
    }

    private fun vinculo(id: Long, papel: Papel, unidadeId: Long? = null, unidade: String? = null) =
        Membership(id, CONDOMINIO_ID, CONDOMINIO_NOME, unidadeId, unidade, papel)
}

@Singleton
class DemoAuthRepository @Inject constructor(private val settings: SettingsRepository) : AuthRepository {
    private val _session = MutableStateFlow<SessionState>(SessionState.Unknown)
    override val session: StateFlow<SessionState> = _session.asStateFlow()
    private val _lastEnd = MutableStateFlow<SessionEnd?>(null)
    override val lastEnd: StateFlow<SessionEnd?> = _lastEnd.asStateFlow()

    /** o demo não guarda sessão: sempre abre no login */
    override suspend fun restore() {
        if (_session.value is SessionState.Unknown) _session.value = SessionState.LoggedOut
    }

    override suspend fun login(email: String, password: String, remember: Boolean, totp: String?): Outcome<Unit> {
        delay(500)
        val mail = email.trim()
        if (!mail.contains('@') && mail.filter { it.isDigit() }.length != 11) return Outcome.Err(AppError.Validation("Este e-mail não parece válido."))
        if (password.isBlank()) return Outcome.Err(AppError.Validation("Informe o e-mail ou CPF e a senha."))
        settings.setLastEmail(mail)
        _lastEnd.value = null
        _session.value = SessionState.LoggedIn(mail)
        return Outcome.Ok(Unit)
    }

    override suspend fun forgotPassword(email: String): Outcome<Unit> {
        delay(400)
        return Outcome.Ok(Unit)
    }

    override suspend fun logout(reason: SessionEnd?) {
        _lastEnd.value = reason
        _session.value = SessionState.LoggedOut
    }
}

@Singleton
class DemoContextRepository @Inject constructor(private val auth: AuthRepository) : ContextRepository {
    override suspend fun me(): Outcome<Me> {
        delay(300)
        val email = (auth.session.value as? SessionState.LoggedIn)?.email ?: return Outcome.Err(AppError.Unauthorized)
        return Outcome.Ok(DemoData.me(email))
    }
}
