package br.com.condovitta.app.core.common

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

/** Relógio injetável (os testes fixam a data de "hoje"). */
interface TimeProvider {
    fun today(): LocalDate
    fun now(): LocalDateTime
    fun currentMonth(): YearMonth = YearMonth.from(today())
    fun epochMillis(): Long
}

@Singleton
class SystemTimeProvider @Inject constructor() : TimeProvider {
    private val zone: ZoneId get() = ZoneId.systemDefault()
    override fun today(): LocalDate = LocalDate.now(zone)
    override fun now(): LocalDateTime = LocalDateTime.now(zone)
    override fun epochMillis(): Long = System.currentTimeMillis()
}
