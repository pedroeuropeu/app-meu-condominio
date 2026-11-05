package br.com.condovitta.app.data.remote

import br.com.condovitta.app.domain.Autorizacao
import br.com.condovitta.app.domain.AutorizadoEncomenda
import br.com.condovitta.app.domain.Aviso
import br.com.condovitta.app.domain.Boleto
import br.com.condovitta.app.domain.Categorias
import br.com.condovitta.app.domain.DiaSemana
import br.com.condovitta.app.domain.EscopoAutorizado
import br.com.condovitta.app.domain.Financeiro
import br.com.condovitta.app.domain.ItemBoleto
import br.com.condovitta.app.domain.MinhasEncomendas
import br.com.condovitta.app.domain.MinhasVisitas
import br.com.condovitta.app.domain.StatusBoleto
import br.com.condovitta.app.domain.TipoVisitante
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonUnquotedLiteral
import kotlinx.serialization.json.jsonPrimitive
import java.math.BigDecimal
import java.time.LocalTime

/** BigDecimal do Java ↔ número do JSON, sem passar por Double (dinheiro não pode arredondar). */
object BigDecimalSerializer : KSerializer<BigDecimal> {
    override val descriptor = PrimitiveSerialDescriptor("BigDecimal", PrimitiveKind.STRING)
    override fun deserialize(decoder: Decoder): BigDecimal =
        if (decoder is JsonDecoder) BigDecimal(decoder.decodeJsonElement().jsonPrimitive.content) else BigDecimal(decoder.decodeString())

    @OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)
    override fun serialize(encoder: Encoder, value: BigDecimal) {
        if (encoder is JsonEncoder) encoder.encodeJsonElement(JsonUnquotedLiteral(value.toPlainString())) else encoder.encodeString(value.toPlainString())
    }
}

/* ---------------- boletos ---------------- */

@Serializable
data class ItemComposicaoDto(val descricao: String = "", @Serializable(with = BigDecimalSerializer::class) val valor: BigDecimal = BigDecimal.ZERO, val tipo: String? = null)

@Serializable
data class BoletoMoradorDto(
    val id: Long,
    val competencia: String = "",
    val vencimento: String? = null,
    @Serializable(with = BigDecimalSerializer::class) val valor: BigDecimal = BigDecimal.ZERO,
    val status: String? = null,
    val pagoEm: String? = null,
    @Serializable(with = BigDecimalSerializer::class) val valorPago: BigDecimal? = null,
    val forma: String? = null,
    val linhaDigitavel: String? = null,
    val pix: String? = null,
    val temPdf: Boolean = false,
    val pagoComAtraso: Boolean = false,
    @Serializable(with = BigDecimalSerializer::class) val corrigido: BigDecimal? = null,
    val itens: List<ItemComposicaoDto> = emptyList()
)

@Serializable
data class FinanceiroMoradorDto(
    val unidadeId: Long? = null,
    val unidade: String? = null,
    val condominio: String? = null,
    val atual: BoletoMoradorDto? = null,
    val boletos: List<BoletoMoradorDto> = emptyList()
)

fun BoletoMoradorDto.toDomain() = Boleto(
    id, competencia.take(7), date(vencimento), valor, StatusBoleto.of(status), date(pagoEm), valorPago, forma, linhaDigitavel,
    pix?.takeIf { it.isNotBlank() }, temPdf, pagoComAtraso, corrigido, itens.map { ItemBoleto(it.descricao, it.valor, it.tipo) }
)

fun FinanceiroMoradorDto.toDomain() = Financeiro(unidade, condominio, atual?.toDomain(), boletos.map { it.toDomain() })

/* ---------------- visitantes ---------------- */

@Serializable
data class AutorizacaoDto(
    val id: Long,
    val nome: String = "",
    val documento: String? = null,
    val telefone: String? = null,
    val tipo: String? = null,
    val unidadeId: Long? = null,
    val unidade: String? = null,
    val veiculoModelo: String? = null,
    val veiculoCor: String? = null,
    val placa: String? = null,
    val inicio: String? = null,
    val fim: String? = null,
    val recorrente: Boolean = false,
    val dias: List<String>? = null,
    val horaInicio: String? = null,
    val horaFim: String? = null,
    val validoAte: String? = null,
    val obs: String? = null,
    val avisar: Boolean = true,
    val codigo: String? = null,
    val origem: String? = null,
    val situacao: String? = null,
    val autorizadoPor: String? = null,
    val criadoEm: String? = null
)

@Serializable
data class AutorizacaoFormDto(
    val nome: String,
    val documento: String? = null,
    val telefone: String? = null,
    val tipo: String,
    val veiculoModelo: String? = null,
    val veiculoCor: String? = null,
    val placa: String? = null,
    val inicio: String? = null,
    val fim: String? = null,
    val recorrente: Boolean = false,
    val dias: List<String>? = null,
    val horaInicio: String? = null,
    val horaFim: String? = null,
    val validoAte: String? = null,
    val obs: String? = null,
    val avisar: Boolean = true
)

@Serializable
data class MinhasVisitasDto(
    val unidade: String? = null,
    val autorizacoes: List<AutorizacaoDto> = emptyList(),
    val historico: List<VisitaDto> = emptyList(),
    val consultas: List<VisitaDto> = emptyList(),
    val entradas30d: Long = 0
)

@Serializable
data class RespostaDto(val liberar: Boolean, val recado: String? = null)

internal fun time(s: String?): LocalTime? = s?.let { runCatching { LocalTime.parse(it) }.getOrNull() }

fun AutorizacaoDto.toDomain() = Autorizacao(
    id = id, nome = nome, documento = documento, telefone = telefone, tipo = TipoVisitante.of(tipo), placa = placa,
    veiculo = listOfNotNull(veiculoModelo, veiculoCor).joinToString(" ").ifBlank { null },
    inicio = dateTime(inicio), fim = dateTime(fim), recorrente = recorrente,
    dias = dias.orEmpty().flatMap { it.split(',') }.mapNotNull { DiaSemana.of(it.trim()) },
    horaInicio = time(horaInicio), horaFim = time(horaFim), validoAte = date(validoAte), obs = obs, codigo = codigo,
    ativa = situacao != "REVOGADA", autorizadoPor = autorizadoPor
)

fun MinhasVisitasDto.toDomain() = MinhasVisitas(unidade, autorizacoes.map { it.toDomain() }, historico.map { it.toDomain() }, consultas.map { it.toDomain() }, entradas30d)

/* ---------------- encomendas ---------------- */

@Serializable
data class AutorizadoEncomendaDto(
    val id: Long,
    val nome: String = "",
    val documento: String? = null,
    val relacao: String? = null,
    val escopo: String? = null,
    val encomendaId: Long? = null,
    val validoAte: String? = null,
    val fixo: Boolean = false
)

@Serializable
data class AutorizadoFormDto(val nome: String, val documento: String? = null, val relacao: String? = null, val escopo: String, val encomendaId: Long? = null, val validoAte: String? = null)

@Serializable
data class MinhasEncomendasDto(
    val unidade: String? = null,
    val aguardando: List<EncomendaDto> = emptyList(),
    val retiradas: List<EncomendaDto> = emptyList(),
    val autorizados: List<AutorizadoEncomendaDto> = emptyList()
)

fun AutorizadoEncomendaDto.toDomain() = AutorizadoEncomenda(
    id, nome, documento, relacao, EscopoAutorizado.entries.firstOrNull { it.name == escopo } ?: EscopoAutorizado.SEMPRE, encomendaId, date(validoAte), fixo
)

fun MinhasEncomendasDto.toDomain() = MinhasEncomendas(unidade, aguardando.map { it.toDomain() }, retiradas.map { it.toDomain() }, autorizados.map { it.toDomain() })

/* ---------------- avisos ---------------- */

@Serializable
data class MuralDto(
    val id: Long,
    val titulo: String = "",
    val texto: String = "",
    val categoria: String? = null,
    val autor: String? = null,
    val publicadoEm: String? = null,
    val fixado: Boolean = false,
    val exigeCiencia: Boolean = false,
    val lido: Boolean = false,
    val ciente: Boolean = false,
    val cienteEm: String? = null
)

fun MuralDto.toDomain() = Aviso(id, titulo, texto, Categorias.label(categoria), autor, dateTime(publicadoEm), fixado, exigeCiencia, lido, ciente, dateTime(cienteEm))

