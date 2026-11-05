package br.com.condovitta.app.domain

import kotlinx.coroutines.flow.StateFlow

/** Entrar, sair e recuperar a senha. */
interface AuthRepository {
    val session: StateFlow<SessionState>

    /** Lê a sessão salva no aparelho (abertura do app). */
    suspend fun restore()

    suspend fun login(email: String, password: String, remember: Boolean, totp: String? = null): Outcome<Unit>

    suspend fun forgotPassword(email: String): Outcome<Unit>

    /** Sai da conta. [reason] aparece no login (sessão expirada, turno passado). */
    suspend fun logout(reason: SessionEnd? = null)

    /** Por que a última sessão terminou (null = saiu por conta própria ou nunca entrou). */
    val lastEnd: StateFlow<SessionEnd?>
}

/** Quem está logado e em quais condomínios. */
interface ContextRepository {
    suspend fun me(): Outcome<Me>
}
