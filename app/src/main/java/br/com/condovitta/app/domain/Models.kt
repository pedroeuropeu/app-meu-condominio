package br.com.condovitta.app.domain

/** Papel da pessoa num condomínio (PapelEnum do backend). */
enum class Papel {
    MORADOR, PORTARIA, FUNCIONARIO, CONSELHO, SINDICO, ADMINISTRACAO, DESCONHECIDO;

    companion object {
        fun of(value: String?): Papel = entries.firstOrNull { it.name == value } ?: DESCONHECIDO
    }
}

/** Papel, unidade e condomínio de um vínculo (MembershipDTO do /api/v1/me). */
data class Membership(
    val id: Long,
    val condominioId: Long,
    val condominioNome: String,
    val unidadeId: Long?,
    val unidade: String?,
    val papel: Papel
)

/** A pessoa logada (GET /api/v1/me). */
data class Me(
    val id: Long,
    val nome: String,
    val email: String,
    val admin: Boolean,
    val memberships: List<Membership>
) {
    val primeiroNome: String get() = nome.trim().substringBefore(' ').ifBlank { email.substringBefore('@') }
    val iniciais: String
        get() {
            val partes = nome.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
            return when {
                partes.size >= 2 -> "${partes.first().first()}${partes.last().first()}"
                partes.size == 1 -> partes.first().take(2)
                else -> email.take(2)
            }.uppercase()
        }
}

/** Os dois jeitos de usar o app: o modo rápido da guarita e o app completo do morador. */
enum class Modo { PORTARIA, MORADOR }

/** Um jeito de entrar: modo + condomínio (e a unidade, no modo morador). */
data class Perfil(
    val modo: Modo,
    val condominioId: Long,
    val condominioNome: String,
    val unidadeId: Long? = null,
    val unidade: String? = null
) {
    val chave: String get() = "${modo.name}:$condominioId"
}

/** Estado da sessão no aparelho. */
sealed interface SessionState {
    data object Unknown : SessionState
    data object LoggedOut : SessionState
    data class LoggedIn(val email: String) : SessionState
}

/** Por que a sessão terminou (o login explica). */
enum class SessionEnd { EXPIRADA, TURNO_PASSADO }
