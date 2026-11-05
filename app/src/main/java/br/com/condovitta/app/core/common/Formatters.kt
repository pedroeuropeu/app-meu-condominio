package br.com.condovitta.app.core.common

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Datas e horas em pt-BR, como na web e no protótipo. */
object Fmt {
    private val PT = Locale.forLanguageTag("pt-BR")
    private val HHMM = DateTimeFormatter.ofPattern("HH:mm", PT)
    private val DDMM = DateTimeFormatter.ofPattern("dd/MM", PT)
    private val DDMMYYYY = DateTimeFormatter.ofPattern("dd/MM/yyyy", PT)

    private val BRL = java.text.NumberFormat.getCurrencyInstance(PT)
    private val MES = DateTimeFormatter.ofPattern("MMMM 'de' yyyy", PT)

    /** "R$ 612,40" */
    fun money(v: java.math.BigDecimal?): String = v?.let { BRL.format(it).replace('\u00A0', ' ') } ?: "—"

    /** "2026-11" → "Novembro de 2026" */
    fun competencia(ym: String): String = runCatching { java.time.YearMonth.parse(ym).format(MES).replaceFirstChar { it.uppercase() } }.getOrDefault(ym)

    fun hora(t: java.time.LocalTime?): String = t?.format(HHMM) ?: "—"

    fun hora(dt: LocalDateTime?): String = dt?.format(HHMM) ?: "—"

    /** "hoje 09:29", "ontem 15:10" ou "30/10 15:10" */
    fun quando(dt: LocalDateTime?, hoje: LocalDate = LocalDate.now()): String {
        if (dt == null) return "—"
        val d = dt.toLocalDate()
        val dia = when (d) {
            hoje -> "hoje"
            hoje.minusDays(1) -> "ontem"
            else -> d.format(DDMM)
        }
        return "$dia ${dt.format(HHMM)}"
    }

    fun data(d: LocalDate?): String = d?.format(DDMMYYYY) ?: "—"

    /** há quantos dias (para encomenda parada) */
    fun dias(dt: LocalDateTime?, hoje: LocalDate = LocalDate.now()): Long =
        dt?.let { java.time.temporal.ChronoUnit.DAYS.between(it.toLocalDate(), hoje) } ?: 0
}
