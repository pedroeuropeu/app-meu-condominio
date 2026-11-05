package br.com.condovitta.app.data.remote

import br.com.condovitta.app.domain.Me
import br.com.condovitta.app.domain.Membership
import br.com.condovitta.app.domain.Papel
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/* DTOs fiéis ao backend gestao-condominio (records Java → JSON camelCase). */

/** AuthDtos.LoginRequest */
@Serializable
data class LoginRequestDto(val email: String, val password: String, val remember: Boolean = true, val totp: String? = null)

/** AuthDtos.Tokens */
@Serializable
data class TokensDto(
    val accessToken: String,
    val refreshToken: String? = null,
    val idToken: String? = null,
    val expiresIn: Int = 300,
    val refreshExpiresIn: Int = 0
)

@Serializable
data class LogoutRequestDto(val refreshToken: String?)

@Serializable
data class ForgotPasswordRequestDto(val email: String)

/** AuthDtos.AuthError e ApiError {code, message, requestId} */
@Serializable
data class ApiErrorDto(val code: String? = null, val message: String? = null, val requestId: String? = null, val retryAfterSeconds: Long? = null)

/** Resposta do token endpoint do Keycloak (renovação). */
@Serializable
data class KeycloakTokenResponse(
    @SerialName("access_token") val accessToken: String,
    @SerialName("refresh_token") val refreshToken: String? = null,
    @SerialName("expires_in") val expiresIn: Long = 300,
    @SerialName("refresh_expires_in") val refreshExpiresIn: Long = 0
)

/** MembershipDTO */
@Serializable
data class MembershipDto(
    val id: Long = 0,
    val condominioId: Long,
    val condominioNome: String? = null,
    val unidadeId: Long? = null,
    val unidade: String? = null,
    val papel: String? = null,
    val cargo: String? = null,
    val permissoes: List<String>? = null
)

/** MeController.MeDTO */
@Serializable
data class MeDto(
    val id: Long = 0,
    val nome: String? = null,
    val email: String = "",
    val admin: Boolean = false,
    val memberships: List<MembershipDto> = emptyList()
)

fun MeDto.toDomain() = Me(
    id = id,
    nome = nome?.trim().orEmpty().ifBlank { email.substringBefore('@') },
    email = email,
    admin = admin,
    memberships = memberships.map {
        Membership(it.id, it.condominioId, it.condominioNome ?: "Condomínio", it.unidadeId, it.unidade, Papel.of(it.papel))
    }
)
