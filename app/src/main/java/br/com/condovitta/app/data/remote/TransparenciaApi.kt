package br.com.condovitta.app.data.remote

import br.com.condovitta.app.domain.BalanceteHistorico
import br.com.condovitta.app.domain.CategoriaValor
import br.com.condovitta.app.domain.EventoCalendario
import br.com.condovitta.app.domain.GasConsumoMes
import br.com.condovitta.app.domain.GasHistorico
import br.com.condovitta.app.domain.GasUltima
import br.com.condovitta.app.domain.LancamentoPublico
import br.com.condovitta.app.domain.Lembrete
import br.com.condovitta.app.domain.MesValor
import br.com.condovitta.app.domain.MeuGas
import br.com.condovitta.app.domain.TipoEvento
import br.com.condovitta.app.domain.Transparencia
import kotlinx.serialization.Serializable
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query
import java.math.BigDecimal

private typealias Money = @Serializable(with = BigDecimalSerializer::class) BigDecimal

/* ---------------- prestação de contas ---------------- */

@Serializable
data class MesPublicadoDto(val ym: String = "", val status: String? = null, val publicadoEm: String? = null, val vistos: Int = 0)

@Serializable
data class CategoriaValorDto(val chave: String? = null, val nome: String = "", val cor: String? = null, val valor: Money? = null)

@Serializable
data class MesValorDto(val ym: String = "", val receitas: Money? = null, val despesas: Money? = null)

@Serializable
data class LancamentoPublicoDto(
    val id: Long, val data: String? = null, val descricao: String = "", val categoria: String? = null, val fornecedor: String? = null,
    val valor: Money? = null, val temNota: Boolean = false
)

@Serializable
data class TransparenciaDto(
    val ym: String? = null, val publicados: List<MesPublicadoDto> = emptyList(), val receitas: Money? = null, val despesas: Money? = null,
    val saldoMes: Money? = null, val variacaoReceitas: Money? = null, val variacaoDespesas: Money? = null, val contaCorrente: Money? = null,
    val fundo: Money? = null, val metaFundo: Money? = null, val porCategoria: List<CategoriaValorDto> = emptyList(),
    val serie: List<MesValorDto> = emptyList(), val lancamentos: List<LancamentoPublicoDto> = emptyList(), val publicadoEm: String? = null
)

@Serializable
data class BalanceteDto(
    val id: Long, val competencia: String = "", val receitas: Money? = null, val despesas: Money? = null, val saldo: Money? = null,
    val origem: String? = null, val arquivoNome: String? = null, val temArquivo: Boolean = false, val publicado: Boolean = true,
    val observacao: String? = null, val importadoEm: String? = null
)

private val ZERO = BigDecimal.ZERO

fun TransparenciaDto.toDomain() = Transparencia(
    ym, publicados.map { it.ym }.filter { it.isNotBlank() }.sortedDescending(), receitas ?: ZERO, despesas ?: ZERO, saldoMes ?: ZERO,
    variacaoReceitas, variacaoDespesas, contaCorrente, fundo, metaFundo,
    porCategoria.map { CategoriaValor(it.nome, it.cor, it.valor ?: ZERO) },
    serie.map { MesValor(it.ym, it.receitas ?: ZERO, it.despesas ?: ZERO) },
    lancamentos.map { LancamentoPublico(it.id, date(it.data), it.descricao, it.categoria, it.fornecedor, it.valor ?: ZERO, it.temNota) },
    dateTime(publicadoEm)
)

fun BalanceteDto.toDomain() = BalanceteHistorico(id, competencia, receitas, despesas, saldo, arquivoNome, temArquivo, observacao)

/* ---------------- gás ---------------- */

@Serializable
data class GasUltimaDto(
    val leituraId: Long? = null, val mes: String = "", val atual: Money? = null, val anterior: Money? = null, val consumo: Money? = null,
    val valor: Money? = null, val lidaEm: String? = null, val lidoPor: String? = null, val temFoto: Boolean = false, val estimada: Boolean = false,
    val fechado: Boolean = false, val contestada: Boolean = false
)

@Serializable
data class GasMesDto(val mes: String = "", val consumo: Money? = null, val mediaBloco: Money? = null)

@Serializable
data class GasHistoricoDto(
    val leituraId: Long? = null, val mes: String = "", val atual: Money? = null, val anterior: Money? = null, val consumo: Money? = null,
    val valor: Money? = null, val lidoPor: String? = null, val lidaEm: String? = null, val acimaDaMedia: Boolean = false, val estimada: Boolean = false
)

@Serializable
data class MeuGasDto(
    val unidade: String? = null, val bloco: String? = null, val preco: Money? = null, val taxa: Money? = null, val ultima: GasUltimaDto? = null,
    val consumoAnterior: Money? = null, val mediaBloco: Money? = null, val media12: Money? = null, val picoMes: String? = null,
    val picoConsumo: Money? = null, val meses: List<GasMesDto> = emptyList(), val leituras: List<GasHistoricoDto> = emptyList(),
    val acimaDoPadrao: Boolean = false
)

@Serializable
data class ContestacaoDto(val mensagem: String? = null)

fun MeuGasDto.toDomain() = MeuGas(
    unidade, bloco, preco, taxa,
    ultima?.let { GasUltima(it.leituraId, it.mes, it.atual, it.anterior, it.consumo, it.valor, dateTime(it.lidaEm), it.lidoPor, it.temFoto, it.estimada, it.fechado, it.contestada) },
    consumoAnterior, mediaBloco, media12, picoMes, picoConsumo,
    meses.map { GasConsumoMes(it.mes, it.consumo, it.mediaBloco) },
    leituras.map { GasHistorico(it.leituraId, it.mes, it.atual, it.anterior, it.consumo, it.valor, it.lidoPor, dateTime(it.lidaEm), it.acimaDaMedia, it.estimada) },
    acimaDoPadrao
)

/* ---------------- calendário ---------------- */

@Serializable
data class EventoCalendarioDto(
    val chave: String = "", val eventoId: Long? = null, val planoId: Long? = null, val data: String = "", val inicio: String? = null,
    val fim: String? = null, val titulo: String = "", val tipo: String? = null, val local: String? = null, val blocos: List<String> = emptyList(),
    val descricao: String? = null, val recorrencia: String? = null, val fornecedor: String? = null, val doPlano: Boolean = false
)

@Serializable
data class LembreteDto(val eventoId: Long, val data: String, val minutosAntes: Int? = null)

fun EventoCalendarioDto.toDomain(): EventoCalendario? = date(data)?.let { d ->
    EventoCalendario(
        chave.ifBlank { "${eventoId ?: planoId}:$data" }, eventoId, d, time(inicio), time(fim), titulo, TipoEvento.of(tipo), local, blocos,
        descricao, recorrencia?.takeIf { it != "NAO" }, fornecedor
    )
}

fun LembreteDto.toDomain(): Lembrete? = date(data)?.let { Lembrete(eventoId, it, minutosAntes ?: 0) }

/** Prestação de contas, gás do morador e calendário do condomínio. */
interface TransparenciaApi {
    @GET("api/v1/financeiro/morador/transparencia")
    suspend fun transparencia(@Query("mes") mes: String?): TransparenciaDto

    @GET("api/v1/financeiro/morador/transparencia/{mes}/pdf")
    suspend fun balancetePdf(@Path("mes") mes: String): ResponseBody

    @GET("api/v1/financeiro/morador/transparencia/lancamentos/{id}/nota")
    suspend fun nota(@Path("id") id: Long): ResponseBody

    @GET("api/v1/financeiro/balancetes/publicos")
    suspend fun balancetes(): List<BalanceteDto>

    @GET("api/v1/financeiro/balancetes/{id}/arquivo")
    suspend fun balanceteArquivo(@Path("id") id: Long): ResponseBody

    @GET("api/v1/gas/minha")
    suspend fun meuGas(): MeuGasDto

    @POST("api/v1/gas/minha/contestar")
    suspend fun contestar(@Body body: ContestacaoDto): Response<Unit>

    @GET("api/v1/gas/leituras/{id}/foto")
    suspend fun fotoGas(@Path("id") id: Long): ResponseBody

    @GET("api/v1/calendario")
    suspend fun calendario(@Query("de") de: String, @Query("ate") ate: String): List<EventoCalendarioDto>

    @GET("api/v1/calendario/lembretes")
    suspend fun lembretes(): List<LembreteDto>

    @PUT("api/v1/calendario/lembretes")
    suspend fun lembrete(@Body body: LembreteDto): Response<Unit>

    @GET("api/v1/calendario/eventos/{id}/ics")
    suspend fun ics(@Path("id") id: Long, @Query("data") data: String): ResponseBody
}
