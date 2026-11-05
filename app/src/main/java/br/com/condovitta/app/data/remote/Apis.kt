package br.com.condovitta.app.data.remote

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

/** Rotas públicas (sem token): /public/auth/ do AuthController. */
interface PublicApi {
    @POST("public/auth/login")
    suspend fun login(@Body body: LoginRequestDto): TokensDto

    @POST("public/auth/logout")
    suspend fun logout(@Body body: LogoutRequestDto): Response<Unit>

    @POST("public/auth/forgot-password")
    suspend fun forgotPassword(@Body body: ForgotPasswordRequestDto): Response<Unit>
}

/** Rotas autenticadas (/api/v1/..., com Bearer e X-Condominio-Id). Cada fase acrescenta as suas. */
interface CondovittaApi {
    @GET("api/v1/me")
    suspend fun me(): MeDto
}
