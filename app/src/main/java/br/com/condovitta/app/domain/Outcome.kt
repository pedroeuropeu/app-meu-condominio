package br.com.condovitta.app.domain

/** Resultado de uma operação de dados: sucesso ou erro já traduzido para a pessoa. */
sealed interface Outcome<out T> {
    data class Ok<T>(val value: T) : Outcome<T>
    data class Err(val error: AppError) : Outcome<Nothing>
}

/** Erro já em linguagem de gente. A mensagem do servidor já vem em português e é usada quando existe. */
sealed class AppError(open val message: String) {
    /** Sem internet ou servidor fora do ar. */
    data object Network : AppError("Sem conexão. Confira a internet e tente de novo.")
    data object Unauthorized : AppError("Sua sessão terminou. Entre de novo.")
    data class InvalidCredentials(override val message: String = "E-mail ou senha incorretos.") : AppError(message)

    /** O login pede o código do app autenticador (2FA) — ou o código digitado está errado. */
    data class OtpRequired(override val message: String = "Digite o código do seu app autenticador.", val invalid: Boolean = false) : AppError(message)
    data class Blocked(override val message: String) : AppError(message)
    data class TooManyAttempts(val retryAfterSeconds: Long?) : AppError(
        if (retryAfterSeconds != null) "Muitas tentativas. Tente de novo em ${(retryAfterSeconds + 59) / 60} min." else "Muitas tentativas. Aguarde um pouco."
    )

    /** Sem papel no condomínio escolhido (403 SEM_ACESSO_CONDOMINIO). */
    data class NoAccess(override val message: String = "Você não tem acesso a este condomínio.") : AppError(message)

    /** Condomínio com acesso restrito pelo faturamento (403 FATURAMENTO_BLOQUEADO). */
    data class BillingBlocked(override val message: String) : AppError(message)
    data class Server(override val message: String = "O servidor não respondeu como esperado. Tente de novo.") : AppError(message)
    data class Validation(override val message: String) : AppError(message)
    data object NotFound : AppError("Não encontramos esse registro.")
}

inline fun <T, R> Outcome<T>.map(f: (T) -> R): Outcome<R> = when (this) {
    is Outcome.Ok -> Outcome.Ok(f(value))
    is Outcome.Err -> this
}

fun <T> Outcome<T>.getOrNull(): T? = (this as? Outcome.Ok)?.value

fun <T> Outcome<T>.errorOrNull(): AppError? = (this as? Outcome.Err)?.error
