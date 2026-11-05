package br.com.condovitta.app.domain

import java.math.BigDecimal
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/* Modelos do app do morador (FinanceiroDTO, VisitanteDTO, EncomendaDTO e ComunicadoDTO do gestao-condominio). */

/* ---------------- boletos ---------------- */

enum class StatusBoleto(val label: String) {
    EM_ABERTO("Em aberto"), VENCIDO("Vencido"), PAGO("Pago"), EM_ACORDO("Em acordo");

    companion object {
        fun of(v: String?) = entries.firstOrNull { it.name.equals(v, ignoreCase = true) } ?: EM_ABERTO
    }
}

data class ItemBoleto(val descricao: String, val valor: BigDecimal, val tipo: String?)

data class Boleto(
    val id: Long,
    /** "2026-11" */
    val competencia: String,
    val vencimento: LocalDate?,
    val valor: BigDecimal,
    val status: StatusBoleto,
    val pagoEm: LocalDate?,
    val valorPago: BigDecimal?,
    val forma: String?,
    val linhaDigitavel: String?,
    /** Pix copia e cola */
    val pix: String?,
    val temPdf: Boolean,
    val pagoComAtraso: Boolean,
    /** valor com multa e juros, quando vencido */
    val corrigido: BigDecimal?,
    val itens: List<ItemBoleto>
) {
    val aPagar: Boolean get() = status == StatusBoleto.EM_ABERTO || status == StatusBoleto.VENCIDO
}

data class Financeiro(val unidade: String?, val condominio: String?, val atual: Boleto?, val boletos: List<Boleto>) {
    val vencidos: List<Boleto> get() = boletos.filter { it.status == StatusBoleto.VENCIDO }
    val pagos: List<Boleto> get() = boletos.filter { it.status == StatusBoleto.PAGO }.sortedByDescending { it.competencia }
}

/* ---------------- visitantes ---------------- */

/** Dias da semana como o backend guarda ("SEG,QUA"). */
enum class DiaSemana(val label: String, val dow: DayOfWeek) {
    DOM("dom", DayOfWeek.SUNDAY), SEG("seg", DayOfWeek.MONDAY), TER("ter", DayOfWeek.TUESDAY), QUA("qua", DayOfWeek.WEDNESDAY),
    QUI("qui", DayOfWeek.THURSDAY), SEX("sex", DayOfWeek.FRIDAY), SAB("sáb", DayOfWeek.SATURDAY);

    companion object {
        fun of(v: String) = entries.firstOrNull { it.name == v.uppercase() }

        /** "seg a sex", "seg, qua e sex", "todos os dias" (como Janela.descricao no backend) */
        fun descrever(dias: List<DiaSemana>): String {
            val d = dias.sortedBy { it.ordinal }
            return when {
                d.size == 7 -> "todos os dias"
                d == listOf(SEG, TER, QUA, QUI, SEX) -> "seg a sex"
                d.isEmpty() -> ""
                d.size == 1 -> d[0].label
                else -> d.dropLast(1).joinToString(", ") { it.label } + " e " + d.last().label
            }
        }
    }
}

enum class SituacaoAutorizacao { LIBERADA_AGORA, AGENDADA, EXPIRADA, REVOGADA }

data class Autorizacao(
    val id: Long,
    val nome: String,
    val documento: String?,
    val telefone: String?,
    val tipo: TipoVisitante,
    val placa: String?,
    val veiculo: String?,
    val inicio: LocalDateTime?,
    val fim: LocalDateTime?,
    val recorrente: Boolean,
    val dias: List<DiaSemana>,
    val horaInicio: LocalTime?,
    val horaFim: LocalTime?,
    val validoAte: LocalDate?,
    val obs: String?,
    /** código que a portaria confere (também vai no QR) */
    val codigo: String?,
    val ativa: Boolean,
    val autorizadoPor: String?
) {
    /** A situação sai das datas, calculada na hora (como faz a web). */
    fun situacao(agora: LocalDateTime): SituacaoAutorizacao {
        if (!ativa) return SituacaoAutorizacao.REVOGADA
        if (recorrente) {
            if (validoAte != null && agora.toLocalDate().isAfter(validoAte)) return SituacaoAutorizacao.EXPIRADA
            val hoje = dias.any { it.dow == agora.dayOfWeek }
            val t = agora.toLocalTime()
            val dentro = hoje && (horaInicio == null || !t.isBefore(horaInicio)) && (horaFim == null || !t.isAfter(horaFim))
            return if (dentro) SituacaoAutorizacao.LIBERADA_AGORA else SituacaoAutorizacao.AGENDADA
        }
        return when {
            fim != null && agora.isAfter(fim) -> SituacaoAutorizacao.EXPIRADA
            inicio != null && agora.isBefore(inicio) -> SituacaoAutorizacao.AGENDADA
            else -> SituacaoAutorizacao.LIBERADA_AGORA
        }
    }
}

data class MinhasVisitas(
    val unidade: String?,
    val autorizacoes: List<Autorizacao>,
    /** entradas e saídas recentes na portaria */
    val historico: List<Visita>,
    /** a portaria pergunta: "fulano está aqui, posso liberar?" */
    val consultas: List<Visita>,
    val entradas30d: Long
)

data class NovaAutorizacao(
    val nome: String,
    val tipo: TipoVisitante,
    val documento: String? = null,
    val telefone: String? = null,
    val placa: String? = null,
    val obs: String? = null,
    val inicio: LocalDateTime? = null,
    val fim: LocalDateTime? = null,
    val recorrente: Boolean = false,
    val dias: List<DiaSemana> = emptyList(),
    val horaInicio: LocalTime? = null,
    val horaFim: LocalTime? = null,
    val validoAte: LocalDate? = null
)

/* ---------------- encomendas ---------------- */

enum class EscopoAutorizado(val label: String) {
    ESTA("Só esta encomenda"), ATE("Até uma data"), SEMPRE("Sempre")
}

data class AutorizadoEncomenda(
    val id: Long,
    val nome: String,
    val documento: String?,
    val relacao: String?,
    val escopo: EscopoAutorizado,
    val encomendaId: Long?,
    val validoAte: LocalDate?,
    /** morador da unidade (não sai da lista) */
    val fixo: Boolean
)

data class MinhasEncomendas(val unidade: String?, val aguardando: List<Encomenda>, val retiradas: List<Encomenda>, val autorizados: List<AutorizadoEncomenda>)

data class NovoAutorizado(val nome: String, val documento: String?, val relacao: String?, val escopo: EscopoAutorizado, val encomendaId: Long? = null, val validoAte: LocalDate? = null)

/* ---------------- avisos ---------------- */

data class Aviso(
    val id: Long,
    val titulo: String,
    val texto: String,
    val categoria: String,
    val autor: String?,
    val publicadoEm: LocalDateTime?,
    val fixado: Boolean,
    val exigeCiencia: Boolean,
    val lido: Boolean,
    val ciente: Boolean,
    val cienteEm: LocalDateTime?
)

object Categorias {
    private val LABELS = mapOf(
        "AVISOS_GERAIS" to "Avisos gerais", "MANUTENCAO" to "Manutenção", "OBRAS" to "Obras", "FINANCEIRO" to "Financeiro",
        "SEGURANCA" to "Segurança", "REGRAS" to "Regras", "SOCIAL" to "Social"
    )

    fun label(v: String?) = LABELS[v] ?: v?.lowercase()?.replaceFirstChar { it.uppercase() } ?: "Aviso"
}
