package br.com.condovitta.app.data.remote

import br.com.condovitta.app.domain.CategoriaNotificacao
import br.com.condovitta.app.domain.Notificacao
import br.com.condovitta.app.domain.Notificacoes
import kotlinx.serialization.Serializable
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

@Serializable
data class NotificacaoDto(
    val id: Long,
    val categoria: String? = null,
    val titulo: String = "",
    val texto: String? = null,
    val rota: String? = null,
    val lida: Boolean = false,
    val criadoEm: String? = null
)

@Serializable
data class NotificacoesDto(val naoLidas: Long = 0, val itens: List<NotificacaoDto> = emptyList())

fun NotificacoesDto.toDomain() = Notificacoes(naoLidas, itens.map {
    Notificacao(it.id, CategoriaNotificacao.of(it.categoria), it.titulo, it.texto, it.rota, it.lida, dateTime(it.criadoEm))
})

/** NotificacaoController */
interface NotificacoesApi {
    @GET("api/v1/notificacoes")
    suspend fun listar(@Query("limite") limite: Int, @Query("apenasNaoLidas") apenasNaoLidas: Boolean): NotificacoesDto

    @POST("api/v1/notificacoes/{id}/lida")
    suspend fun lida(@Path("id") id: Long): Response<Unit>

    @POST("api/v1/notificacoes/lidas")
    suspend fun todasLidas(): Response<Unit>
}
