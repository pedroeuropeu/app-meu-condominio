package br.com.condovitta.app.domain

import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/* Prestação de contas, consumo de gás e calendário do morador — PrestacaoService, GasDTO e CalendarioDTO do gestao-condominio. */

/* ---------------- prestação de contas ---------------- */

data class CategoriaValor(val nome: String, val cor: String?, val valor: BigDecimal)
data class MesValor(val ym: String, val receitas: BigDecimal, val despesas: BigDecimal)
data class LancamentoPublico(val id: Long, val data: LocalDate?, val descricao: String, val categoria: String?, val fornecedor: String?, val valor: BigDecimal, val temNota: Boolean)

data class Transparencia(
    /** null = nada publicado ainda */
    val ym: String?,
    val publicados: List<String>,
    val receitas: BigDecimal,
    val despesas: BigDecimal,
    val saldoMes: BigDecimal,
    /** em %, já com 1 casa (null = sem mês anterior) */
    val variacaoReceitas: BigDecimal?,
    val variacaoDespesas: BigDecimal?,
    val contaCorrente: BigDecimal?,
    val fundo: BigDecimal?,
    val metaFundo: BigDecimal?,
    val porCategoria: List<CategoriaValor>,
    val serie: List<MesValor>,
    val lancamentos: List<LancamentoPublico>,
    val publicadoEm: LocalDateTime?
)

data class BalanceteHistorico(
    val id: Long, val competencia: String, val receitas: BigDecimal?, val despesas: BigDecimal?, val saldo: BigDecimal?,
    val arquivoNome: String?, val temArquivo: Boolean, val observacao: String?
)

/* ---------------- gás ---------------- */

data class GasUltima(
    val leituraId: Long?, val mes: String, val atual: BigDecimal?, val anterior: BigDecimal?, val consumo: BigDecimal?, val valor: BigDecimal?,
    val lidaEm: LocalDateTime?, val lidoPor: String?, val temFoto: Boolean, val estimada: Boolean, val fechado: Boolean, val contestada: Boolean
)

data class GasConsumoMes(val mes: String, val consumo: BigDecimal?, val mediaBloco: BigDecimal?)

data class GasHistorico(
    val leituraId: Long?, val mes: String, val atual: BigDecimal?, val anterior: BigDecimal?, val consumo: BigDecimal?, val valor: BigDecimal?,
    val lidoPor: String?, val lidaEm: LocalDateTime?, val acimaDaMedia: Boolean, val estimada: Boolean
)

data class MeuGas(
    val unidade: String?,
    val bloco: String?,
    val preco: BigDecimal?,
    val taxa: BigDecimal?,
    val ultima: GasUltima?,
    val consumoAnterior: BigDecimal?,
    val mediaBloco: BigDecimal?,
    val media12: BigDecimal?,
    val picoMes: String?,
    val picoConsumo: BigDecimal?,
    val meses: List<GasConsumoMes>,
    val leituras: List<GasHistorico>,
    val acimaDoPadrao: Boolean
)

/* ---------------- calendário ---------------- */

enum class TipoEvento(val label: String) {
    ASSEMBLEIA("Assembleia"), MANUTENCAO("Manutenção"), SOCIAL("Social"), REUNIAO("Reunião"), OBRA("Obra"), SISTEMA("Aviso");

    companion object {
        fun of(v: String?) = entries.firstOrNull { it.name == v } ?: SISTEMA
    }
}

data class EventoCalendario(
    val chave: String,
    val eventoId: Long?,
    val data: LocalDate,
    val inicio: LocalTime?,
    val fim: LocalTime?,
    val titulo: String,
    val tipo: TipoEvento,
    val local: String?,
    val blocos: List<String>,
    val descricao: String?,
    val recorrencia: String?,
    val fornecedor: String?
)

data class Lembrete(val eventoId: Long, val data: LocalDate, val minutosAntes: Int)

interface TransparenciaRepository {
    suspend fun transparencia(mes: String? = null): Outcome<Transparencia>
    suspend fun balancetePdf(mes: String): Outcome<ByteArray>
    suspend fun nota(lancamentoId: Long): Outcome<ByteArray>
    suspend fun balancetes(): Outcome<List<BalanceteHistorico>>
    suspend fun balanceteArquivo(id: Long): Outcome<ByteArray>

    suspend fun meuGas(): Outcome<MeuGas>
    suspend fun contestarGas(mensagem: String?): Outcome<Unit>
    suspend fun fotoGas(leituraId: Long): Outcome<ByteArray>

    suspend fun calendario(de: LocalDate, ate: LocalDate): Outcome<List<EventoCalendario>>
    suspend fun lembretes(): Outcome<List<Lembrete>>
    /** minutos null ou 0 remove o lembrete */
    suspend fun lembrete(eventoId: Long, data: LocalDate, minutosAntes: Int?): Outcome<Unit>
    suspend fun ics(eventoId: Long, data: LocalDate): Outcome<ByteArray>
}
