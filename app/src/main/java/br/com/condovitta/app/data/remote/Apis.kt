package br.com.condovitta.app.data.remote

import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

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

/** Portaria: PortariaController, PortariaAcessoController e EncomendaController. */
interface PortariaApi {
    @GET("api/v1/portaria/painel")
    suspend fun painel(): PainelDto

    @GET("api/v1/portaria/busca")
    suspend fun busca(@Query("q") q: String): BuscaDto

    @GET("api/v1/portaria/unidades")
    suspend fun unidades(): List<UnidadeResumoDto>

    @GET("api/v1/portaria/acesso")
    suspend fun acesso(): ListasDto

    @GET("api/v1/portaria/bloqueados")
    suspend fun bloqueados(): List<BloqueadoDto>

    @POST("api/v1/portaria/visitantes")
    suspend fun novaVisita(@Body body: VisitanteAvulsoDto): VisitaDto

    @DELETE("api/v1/portaria/visitantes/{id}")
    suspend fun excluirVisita(@Path("id") id: Long): Response<Unit>

    @POST("api/v1/portaria/visitantes/{id}/entrada")
    suspend fun entrada(@Path("id") visitanteId: Long, @Body body: EntradaDto): VisitaDto

    @POST("api/v1/portaria/visitantes/{id}/consultar")
    suspend fun consultar(@Path("id") visitanteId: Long): VisitaDto

    @POST("api/v1/portaria/acessos/{id}/recusar")
    suspend fun recusar(@Path("id") acessoId: Long): VisitaDto

    @POST("api/v1/portaria/acessos/{id}/saida")
    suspend fun saida(@Path("id") acessoId: Long): VisitaDto

    @POST("api/v1/portaria/acessos/{id}/desfazer")
    suspend fun desfazer(@Path("id") acessoId: Long): Response<Unit>

    @GET("api/v1/portaria/encomendas")
    suspend fun encomendas(): EncomendasPortariaDto

    @GET("api/v1/portaria/encomendas/destinatarios/{unidadeId}")
    suspend fun destinatarios(@Path("unidadeId") unidadeId: Long): DestinatariosDto

    /** multipart: "dados" (JSON) e "foto" (opcional) */
    @Multipart
    @POST("api/v1/portaria/encomendas")
    suspend fun registrarEncomenda(@Part("dados") dados: RequestBody, @Part foto: MultipartBody.Part?): EncomendaDto

    @DELETE("api/v1/portaria/encomendas/{id}")
    suspend fun excluirEncomenda(@Path("id") id: Long): Response<Unit>

    @POST("api/v1/portaria/encomendas/{id}/avisar")
    suspend fun avisar(@Path("id") id: Long): EncomendaDto

    @POST("api/v1/portaria/encomendas/{id}/entregar")
    suspend fun entregar(@Path("id") id: Long, @Body body: EntregaDto): EncomendaDto

    @POST("api/v1/portaria/encomendas/{id}/desfazer-entrega")
    suspend fun desfazerEntrega(@Path("id") id: Long): EncomendaDto

    @GET("api/v1/encomendas/{id}/foto")
    suspend fun fotoEncomenda(@Path("id") id: Long): ResponseBody
}

/** App do morador: BoletoController, VisitanteController, EncomendaController e ComunicadoController. */
interface MoradorApi {
    @GET("api/v1/financeiro/morador")
    suspend fun financeiro(): FinanceiroMoradorDto

    @GET("api/v1/financeiro/morador/boletos/{id}/pdf")
    suspend fun boletoPdf(@Path("id") id: Long): ResponseBody

    @GET("api/v1/financeiro/morador/boletos/{id}/pix.png")
    suspend fun pixQr(@Path("id") id: Long): ResponseBody

    @GET("api/v1/visitantes/minhas")
    suspend fun visitas(): MinhasVisitasDto

    @POST("api/v1/visitantes")
    suspend fun autorizar(@Body body: AutorizacaoFormDto): AutorizacaoDto

    @POST("api/v1/visitantes/{id}/revogar")
    suspend fun revogar(@Path("id") id: Long): Response<Unit>

    @POST("api/v1/visitantes/{id}/reativar")
    suspend fun reativar(@Path("id") id: Long): AutorizacaoDto

    @GET("api/v1/visitantes/{id}/qr")
    suspend fun qrVisitante(@Path("id") id: Long): ResponseBody

    @POST("api/v1/visitantes/consultas/{acessoId}/responder")
    suspend fun responder(@Path("acessoId") acessoId: Long, @Body body: RespostaDto): VisitaDto

    @GET("api/v1/encomendas/minhas")
    suspend fun encomendas(): MinhasEncomendasDto

    @POST("api/v1/encomendas/autorizados")
    suspend fun autorizarRetirada(@Body body: AutorizadoFormDto): AutorizadoEncomendaDto

    @DELETE("api/v1/encomendas/autorizados/{id}")
    suspend fun removerAutorizado(@Path("id") id: Long): Response<Unit>

    @GET("api/v1/comunicados/mural")
    suspend fun mural(): List<MuralDto>

    @POST("api/v1/comunicados/{id}/lido")
    suspend fun lido(@Path("id") id: Long): Response<Unit>

    @POST("api/v1/comunicados/{id}/ciente")
    suspend fun ciente(@Path("id") id: Long): Response<Unit>
}
