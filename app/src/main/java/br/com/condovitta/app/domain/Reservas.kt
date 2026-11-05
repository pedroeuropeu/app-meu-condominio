package br.com.condovitta.app.domain

import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.YearMonth

/* Reservas de áreas comuns (AreaComumDTO, DisponibilidadeDTO e ReservaDTO do gestao-condominio). */

data class ItemArea(
    val id: Long,
    val nome: String,
    val apelido: String?,
    val capacidade: Int?,
    val taxa: BigDecimal?,
    val recursos: List<String>,
    /** motivo, quando o espaço está em manutenção ou interditado */
    val manutencao: String?
) {
    val disponivel: Boolean get() = manutencao == null
}

data class TurnoArea(val id: Long, val nome: String, val inicio: LocalTime?, val fim: LocalTime?, val taxa: BigDecimal?)

data class AreaComum(
    val id: Long,
    val nome: String,
    val descricao: String?,
    /** nome do ícone Material ("local_fire_department") */
    val icone: String?,
    /** "#EA580C" */
    val cor: String?,
    val taxa: BigDecimal?,
    val maxConvidados: Int?,
    val antecedenciaMinDias: Int,
    val antecedenciaMaxDias: Int,
    val cancelamentoGratisHoras: Int?,
    val exigeTermo: Boolean,
    val exigeAprovacao: Boolean,
    val termoTexto: String?,
    val itens: List<ItemArea>,
    val turnos: List<TurnoArea>
)

/** Situação de um dia / espaço / horário no calendário (os nomes do backend). */
enum class SituacaoDia {
    LIVRE, PARCIAL, LOTADO, PASSADO, FORA, FECHADO, BLOQUEADO, MANUTENCAO, INTERDITADA, OCUPADA, RESERVADO, PENDENTE;

    val reservavel: Boolean get() = this == LIVRE || this == PARCIAL

    companion object {
        fun of(v: String?) = entries.firstOrNull { it.name.equals(v, ignoreCase = true) } ?: FECHADO
    }
}

data class TurnoDia(val turnoId: Long, val situacao: SituacaoDia, val unidade: String?)
data class ItemDia(val itemId: Long, val situacao: SituacaoDia, val motivo: String?, val turnos: List<TurnoDia>)
data class DiaDisponibilidade(val data: LocalDate, val situacao: SituacaoDia, val motivo: String?, val itens: List<ItemDia>) {
    fun item(itemId: Long) = itens.firstOrNull { it.itemId == itemId }

    /** situação do dia para um espaço: livre se algum horário dele está livre */
    fun para(itemId: Long?): SituacaoDia {
        if (!situacao.reservavel || itemId == null) return situacao
        val i = item(itemId) ?: return situacao
        if (i.turnos.isEmpty()) return i.situacao
        val livres = i.turnos.count { it.situacao == SituacaoDia.LIVRE }
        return when {
            livres == 0 -> SituacaoDia.LOTADO
            livres < i.turnos.size -> SituacaoDia.PARCIAL
            else -> SituacaoDia.LIVRE
        }
    }

    fun turnoLivre(itemId: Long, turnoId: Long): Boolean =
        situacao.reservavel && item(itemId)?.turnos?.firstOrNull { it.turnoId == turnoId }?.situacao == SituacaoDia.LIVRE
}

data class Disponibilidade(val areaId: Long, val mes: YearMonth, val hoje: LocalDate, val dias: List<DiaDisponibilidade>) {
    fun dia(d: LocalDate) = dias.firstOrNull { it.data == d }
}

enum class StatusReserva(val label: String) {
    AGUARDANDO_APROVACAO("Aguardando aprovação"), CONFIRMADA("Confirmada"), RECUSADA("Recusada"), CANCELADA("Cancelada");

    companion object {
        fun of(v: String?) = entries.firstOrNull { it.name == v } ?: CONFIRMADA
    }
}

enum class ChaveReserva { NA_PORTARIA, ENTREGUE, DEVOLVIDA }

data class Convidado(val id: Long, val nome: String, val documento: String?, val extra: Boolean, val chegouEm: LocalDateTime?)

data class Reserva(
    val id: Long,
    val protocolo: String?,
    val areaId: Long?,
    val areaNome: String,
    val areaIcone: String?,
    val areaCor: String?,
    val itemNome: String?,
    val turnoNome: String?,
    val data: LocalDate,
    val inicio: LocalTime?,
    val fim: LocalTime?,
    val unidade: String?,
    val morador: String?,
    val convidados: Int,
    val status: StatusReserva,
    val taxa: BigDecimal?,
    val chave: ChaveReserva?,
    val chaveEntregueEm: LocalDateTime?,
    val chaveDevolvidaEm: LocalDateTime?,
    val cancelamentoGratisAte: LocalDateTime?,
    val limiteConvidados: Int?,
    val motivoDecisao: String?,
    val lista: List<Convidado>
) {
    /** "Churrasqueira 3 · Gourmet" (o espaço já diz a área; sem espaço, o nome da área) */
    val local: String get() = itemNome?.takeIf { it.isNotBlank() } ?: areaNome
    val ativa: Boolean get() = status == StatusReserva.CONFIRMADA || status == StatusReserva.AGUARDANDO_APROVACAO
    val chegaram: Int get() = lista.count { it.chegouEm != null }
}

data class Cancelamento(val reserva: Reserva, val taxaMantida: Boolean, val mensagem: String?)

data class ReservasPortaria(val hoje: List<Reserva>, val amanha: List<Reserva>)

enum class ModoAssinatura { DESENHO, DIGITADA }

data class NovaReserva(
    val areaId: Long,
    val itemId: Long,
    val turnoId: Long,
    val data: LocalDate,
    val convidados: Int,
    val lista: List<String> = emptyList(),
    val aceite: Boolean = false,
    val modo: ModoAssinatura? = null,
    val nomeDigitado: String? = null,
    val observacoes: String? = null,
    /** PNG da assinatura desenhada (modo DESENHO) */
    val assinatura: ByteArray? = null
) {
    override fun equals(other: Any?) = other is NovaReserva && areaId == other.areaId && itemId == other.itemId && turnoId == other.turnoId &&
        data == other.data && convidados == other.convidados && lista == other.lista && modo == other.modo
    override fun hashCode() = listOf(areaId, itemId, turnoId, data, convidados).hashCode()
}

/** Reservas: o morador reserva e cancela; a portaria vê as do dia, faz check-in e controla a chave. */
interface ReservasRepository {
    suspend fun areas(): Outcome<List<AreaComum>>
    suspend fun disponibilidade(areaId: Long, mes: YearMonth): Outcome<Disponibilidade>
    suspend fun minhas(): Outcome<List<Reserva>>
    suspend fun criar(form: NovaReserva): Outcome<Reserva>
    suspend fun cancelar(id: Long): Outcome<Cancelamento>
    suspend fun desfazerCancelamento(id: Long): Outcome<Reserva>
    suspend fun atualizarLista(id: Long, nomes: List<String>): Outcome<Reserva>

    suspend fun portaria(): Outcome<ReservasPortaria>
    suspend fun checkin(reservaId: Long, convidadoId: Long): Outcome<Convidado>
    suspend fun incluirConvidado(reservaId: Long, nome: String): Outcome<Convidado>
    suspend fun entregarChave(id: Long): Outcome<Reserva>
    suspend fun desfazerChave(id: Long): Outcome<Reserva>
    suspend fun devolucao(id: Long, ok: Boolean, observacoes: String?): Outcome<Reserva>
}
