package br.com.condovitta.app.data.remote

import br.com.condovitta.app.domain.AreaComum
import br.com.condovitta.app.domain.Cancelamento
import br.com.condovitta.app.domain.ChaveReserva
import br.com.condovitta.app.domain.Convidado
import br.com.condovitta.app.domain.DiaDisponibilidade
import br.com.condovitta.app.domain.Disponibilidade
import br.com.condovitta.app.domain.ItemArea
import br.com.condovitta.app.domain.ItemDia
import br.com.condovitta.app.domain.Reserva
import br.com.condovitta.app.domain.ReservasPortaria
import br.com.condovitta.app.domain.SituacaoDia
import br.com.condovitta.app.domain.StatusReserva
import br.com.condovitta.app.domain.TurnoArea
import br.com.condovitta.app.domain.TurnoDia
import kotlinx.serialization.Serializable
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth

/* ---------------- áreas comuns ---------------- */

@Serializable
data class ManutencaoDto(val bloqueioId: Long? = null, val de: String? = null, val ate: String? = null, val motivo: String? = null, val hoje: Boolean = false, val interditada: Boolean = false)

@Serializable
data class ItemAreaDto(
    val id: Long,
    val nome: String = "",
    val apelido: String? = null,
    val capacidade: Int? = null,
    @Serializable(with = BigDecimalSerializer::class) val taxa: BigDecimal? = null,
    @Serializable(with = BigDecimalSerializer::class) val taxaEfetiva: BigDecimal? = null,
    val recursos: List<String>? = null,
    val ordem: Int? = null,
    val manutencao: ManutencaoDto? = null
)

@Serializable
data class TurnoAreaDto(
    val id: Long,
    val nome: String = "",
    val inicio: String? = null,
    val fim: String? = null,
    val ordem: Int? = null,
    @Serializable(with = BigDecimalSerializer::class) val taxa: BigDecimal? = null,
    @Serializable(with = BigDecimalSerializer::class) val taxaEfetiva: BigDecimal? = null
)

@Serializable
data class AreaComumDto(
    val id: Long,
    val nome: String = "",
    val descricao: String? = null,
    val icone: String? = null,
    val cor: String? = null,
    @Serializable(with = BigDecimalSerializer::class) val taxa: BigDecimal? = null,
    val maxConvidados: Int? = null,
    val antecedenciaMinDias: Int? = null,
    val antecedenciaMaxDias: Int? = null,
    val cancelamentoGratisHoras: Int? = null,
    val porTurno: Boolean = true,
    val exigeTermo: Boolean = false,
    val exigeAprovacao: Boolean = false,
    val termoTexto: String? = null,
    val ordem: Int? = null,
    val itens: List<ItemAreaDto> = emptyList(),
    val turnos: List<TurnoAreaDto> = emptyList()
)

fun AreaComumDto.toDomain() = AreaComum(
    id, nome, descricao, icone, cor, taxa, maxConvidados, antecedenciaMinDias ?: 0, antecedenciaMaxDias ?: 90, cancelamentoGratisHoras,
    exigeTermo, exigeAprovacao, termoTexto,
    itens.sortedBy { it.ordem ?: 0 }.map { i ->
        ItemArea(i.id, i.nome, i.apelido, i.capacidade, i.taxaEfetiva ?: i.taxa, i.recursos.orEmpty(),
            i.manutencao?.takeIf { it.hoje || it.interditada }?.let { it.motivo ?: if (it.interditada) "Interditado" else "Em manutenção" })
    },
    turnos.sortedBy { it.ordem ?: 0 }.map { t -> TurnoArea(t.id, t.nome, time(t.inicio), time(t.fim), t.taxaEfetiva ?: t.taxa) }
)

@Serializable
data class TurnoDiaDto(val turnoId: Long, val status: String? = null, val reservaId: Long? = null, val unidadeId: Long? = null, val unidade: String? = null, val motivo: String? = null)

@Serializable
data class ItemDiaDto(val itemId: Long, val status: String? = null, val motivo: String? = null, val turnos: List<TurnoDiaDto> = emptyList())

@Serializable
data class DiaDto(val data: String, val status: String? = null, val motivo: String? = null, val itens: List<ItemDiaDto> = emptyList())

@Serializable
data class DisponibilidadeDto(val areaId: Long, val ano: Int, val mes: Int, val hoje: String? = null, val dias: List<DiaDto> = emptyList())

fun DisponibilidadeDto.toDomain() = Disponibilidade(
    areaId, YearMonth.of(ano, mes), date(hoje) ?: LocalDate.now(),
    dias.mapNotNull { d ->
        val data = date(d.data) ?: return@mapNotNull null
        DiaDisponibilidade(data, SituacaoDia.of(d.status), d.motivo, d.itens.map { i ->
            ItemDia(i.itemId, SituacaoDia.of(i.status), i.motivo, i.turnos.map { TurnoDia(it.turnoId, SituacaoDia.of(it.status), it.unidade) })
        })
    }
)

/* ---------------- reservas ---------------- */

@Serializable
data class ConvidadoDto(val id: Long, val nome: String = "", val documento: String? = null, val extra: Boolean = false, val chegouEm: String? = null)

@Serializable
data class ReservaDto(
    val id: Long,
    val protocolo: String? = null,
    val areaId: Long? = null,
    val areaNome: String? = null,
    val areaIcone: String? = null,
    val areaCor: String? = null,
    val itemNome: String? = null,
    val itemApelido: String? = null,
    val turnoNome: String? = null,
    val data: String,
    val inicio: String? = null,
    val fim: String? = null,
    val unidade: String? = null,
    val morador: String? = null,
    val convidados: Int? = null,
    val status: String? = null,
    @Serializable(with = BigDecimalSerializer::class) val taxa: BigDecimal? = null,
    val chave: String? = null,
    val chaveEntregueEm: String? = null,
    val chaveDevolvidaEm: String? = null,
    val cancelamentoGratisAte: String? = null,
    val limiteConvidados: Int? = null,
    val motivoDecisao: String? = null,
    val lista: List<ConvidadoDto>? = null
)

fun ConvidadoDto.toDomain() = Convidado(id, nome, documento, extra, dateTime(chegouEm))

fun ReservaDto.toDomain() = Reserva(
    id = id, protocolo = protocolo, areaId = areaId, areaNome = areaNome ?: "Área comum", areaIcone = areaIcone, areaCor = areaCor,
    itemNome = itemApelido?.takeIf { it.isNotBlank() } ?: itemNome, turnoNome = turnoNome, data = date(data) ?: LocalDate.now(),
    inicio = time(inicio), fim = time(fim), unidade = unidade, morador = morador, convidados = convidados ?: 0,
    status = StatusReserva.of(status), taxa = taxa, chave = ChaveReserva.entries.firstOrNull { it.name == chave },
    chaveEntregueEm = dateTime(chaveEntregueEm), chaveDevolvidaEm = dateTime(chaveDevolvidaEm),
    cancelamentoGratisAte = dateTime(cancelamentoGratisAte), limiteConvidados = limiteConvidados, motivoDecisao = motivoDecisao,
    lista = lista.orEmpty().map { it.toDomain() }
)

@Serializable
data class CancelamentoDto(val reserva: ReservaDto, val taxaMantida: Boolean = false, val mensagem: String? = null)

fun CancelamentoDto.toDomain() = Cancelamento(reserva.toDomain(), taxaMantida, mensagem)

@Serializable
data class ReservasPortariaDto(val hoje: String? = null, val amanha: String? = null, val reservasHoje: List<ReservaDto> = emptyList(), val reservasAmanha: List<ReservaDto> = emptyList())

fun ReservasPortariaDto.toDomain() = ReservasPortaria(reservasHoje.map { it.toDomain() }, reservasAmanha.map { it.toDomain() })

@Serializable
data class NovoConvidadoDto(val nome: String, val documento: String? = null)

@Serializable
data class NovaReservaDto(
    val areaId: Long,
    val itemId: Long,
    val turnoId: Long,
    val data: String,
    val convidados: Int,
    val lista: List<NovoConvidadoDto>? = null,
    val aceite: Boolean = false,
    val modoAssinatura: String? = null,
    val nomeDigitado: String? = null,
    val observacoes: String? = null
)

@Serializable
data class ListaDto(val lista: List<NovoConvidadoDto>)

@Serializable
data class IncluirConvidadoDto(val nome: String)

@Serializable
data class DevolucaoDto(val ok: Boolean, val observacoes: String? = null)

/** ReservaController e AreaComumController. */
interface ReservasApi {
    @GET("api/v1/areas-comuns")
    suspend fun areas(): List<AreaComumDto>

    @GET("api/v1/areas-comuns/{id}/disponibilidade")
    suspend fun disponibilidade(@Path("id") areaId: Long, @Query("ano") ano: Int, @Query("mes") mes: Int): DisponibilidadeDto

    @GET("api/v1/reservas/minhas")
    suspend fun minhas(): List<ReservaDto>

    /** com assinatura desenhada: "dados" (JSON) + "arquivo" (PNG) */
    @Multipart
    @POST("api/v1/reservas")
    suspend fun criarComArquivo(@Part("dados") dados: RequestBody, @Part arquivo: MultipartBody.Part): ReservaDto

    @POST("api/v1/reservas")
    suspend fun criar(@Body body: NovaReservaDto): ReservaDto

    @POST("api/v1/reservas/{id}/cancelar")
    suspend fun cancelar(@Path("id") id: Long): CancelamentoDto

    @POST("api/v1/reservas/{id}/desfazer-cancelamento")
    suspend fun desfazerCancelamento(@Path("id") id: Long): ReservaDto

    @PUT("api/v1/reservas/{id}/convidados")
    suspend fun atualizarLista(@Path("id") id: Long, @Body body: ListaDto): ReservaDto

    @GET("api/v1/reservas/portaria")
    suspend fun portaria(): ReservasPortariaDto

    @POST("api/v1/reservas/{id}/convidados/{convidadoId}/checkin")
    suspend fun checkin(@Path("id") id: Long, @Path("convidadoId") convidadoId: Long): ConvidadoDto

    @POST("api/v1/reservas/{id}/convidados")
    suspend fun incluirConvidado(@Path("id") id: Long, @Body body: IncluirConvidadoDto): ConvidadoDto

    @POST("api/v1/reservas/{id}/chave")
    suspend fun entregarChave(@Path("id") id: Long): ReservaDto

    @DELETE("api/v1/reservas/{id}/chave")
    suspend fun desfazerChave(@Path("id") id: Long): ReservaDto

    @POST("api/v1/reservas/{id}/devolucao")
    suspend fun devolucao(@Path("id") id: Long, @Body body: DevolucaoDto): ReservaDto
}

