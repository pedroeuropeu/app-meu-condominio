package br.com.condovitta.app.data.demo

import br.com.condovitta.app.core.common.TimeProvider
import br.com.condovitta.app.domain.AppError
import br.com.condovitta.app.domain.AreaComum
import br.com.condovitta.app.domain.Cancelamento
import br.com.condovitta.app.domain.ChaveReserva
import br.com.condovitta.app.domain.Convidado
import br.com.condovitta.app.domain.DiaDisponibilidade
import br.com.condovitta.app.domain.Disponibilidade
import br.com.condovitta.app.domain.ItemArea
import br.com.condovitta.app.domain.ItemDia
import br.com.condovitta.app.domain.ModoAssinatura
import br.com.condovitta.app.domain.NovaReserva
import br.com.condovitta.app.domain.Outcome
import br.com.condovitta.app.domain.Reserva
import br.com.condovitta.app.domain.ReservasPortaria
import br.com.condovitta.app.domain.ReservasRepository
import br.com.condovitta.app.domain.SituacaoDia
import br.com.condovitta.app.domain.StatusReserva
import br.com.condovitta.app.domain.TurnoArea
import br.com.condovitta.app.domain.TurnoDia
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.math.BigDecimal
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.temporal.TemporalAdjusters
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Áreas comuns do Residencial Vila do Sol 2 (as do prototype/data.js): churrasqueiras, salão, quadra e espaço gourmet. A
 * ocupação do calendário é fixa por dia (o mesmo padrão a cada abertura) e as reservas feitas no app passam a ocupar o horário.
 */
@Singleton
class DemoReservasRepository @Inject constructor(private val time: TimeProvider) : ReservasRepository {

    private val lock = Mutex()
    private var seq = 7000L
    private val hoje: LocalDate get() = time.today()

    private val almocoNoite = { base: Long, taxa: String? ->
        listOf(
            TurnoArea(base + 1, "Almoço", LocalTime.of(10, 0), LocalTime.of(16, 0), taxa?.let(::BigDecimal)),
            TurnoArea(base + 2, "Noite", LocalTime.of(17, 0), LocalTime.of(23, 0), taxa?.let(::BigDecimal))
        )
    }

    private val termo = "Declaro que li e aceito o regimento de uso das áreas comuns. Comprometo-me a devolver o espaço limpo e " +
        "organizado até o fim do horário, a respeitar o limite de convidados e o silêncio após as 22h, e a responder por danos " +
        "causados por mim ou pelos meus convidados. A taxa entra no próximo boleto da unidade."

    val areas: List<AreaComum> = listOf(
        AreaComum(
            1, "Churrasqueiras", "4 churrasqueiras ao redor da piscina", "local_fire_department", "#EA580C", BigDecimal("80"), 30, 1, 60, 48, true, false, termo,
            listOf(
                ItemArea(11, "Churrasqueira 1", "Deck", 20, BigDecimal("80"), listOf("Coberta", "Pia", "Freezer"), null),
                ItemArea(12, "Churrasqueira 2", "Jardim", 15, BigDecimal("60"), listOf("Ao ar livre", "Pia"), null),
                ItemArea(13, "Churrasqueira 3", "Gourmet", 30, BigDecimal("120"), listOf("Coberta", "Forno de pizza", "TV"), null),
                ItemArea(14, "Churrasqueira 4", "Playground", 12, BigDecimal("60"), listOf("Ao lado do parquinho"), "Troca da grelha até o fim do mês")
            ),
            almocoNoite(100, null)
        ),
        AreaComum(
            2, "Salão de festas", "Climatizado, até 80 pessoas", "celebration", "#7C3AED", BigDecimal("250"), 80, 7, 120, 72, true, true, termo,
            listOf(ItemArea(21, "Salão de festas", null, 80, BigDecimal("250"), listOf("Ar-condicionado", "Cozinha", "Som"), null)),
            almocoNoite(200, null)
        ),
        AreaComum(
            3, "Quadra", "Reservas de 1 h, das 7h às 22h", "sports_soccer", "#059669", BigDecimal.ZERO, 12, 0, 14, 2, false, false, null,
            listOf(ItemArea(31, "Quadra poliesportiva", null, 12, BigDecimal.ZERO, listOf("Iluminação", "Rede", "Traves"), null)),
            listOf(7, 8, 18, 19, 20, 21).mapIndexed { i, h -> TurnoArea(300L + i, "%02dh – %02dh".format(h, h + 1), LocalTime.of(h, 0), LocalTime.of(h + 1, 0), null) }
        ),
        AreaComum(
            4, "Espaço gourmet", "Cozinha equipada, mesa para 16", "restaurant", "#D97706", BigDecimal("150"), 16, 2, 60, 48, true, false, termo,
            listOf(ItemArea(41, "Espaço gourmet", null, 16, BigDecimal("150"), listOf("Cooktop", "Forno", "Adega"), null)),
            almocoNoite(400, null)
        )
    )

    private val reservas = mutableListOf<Reserva>()

    init {
        val sab = hoje.with(TemporalAdjusters.next(DayOfWeek.SATURDAY))
        reservas += nova(13, 101, sab.plusWeeks(1), 18, StatusReserva.CONFIRMADA, "B-204", "Ana Souza")
        reservas += nova(21, 202, sab.plusWeeks(5), 60, StatusReserva.AGUARDANDO_APROVACAO, "B-204", "Ana Souza")
        // o que a portaria vê hoje e amanhã
        reservas += nova(21, 202, hoje, 40, StatusReserva.CONFIRMADA, "A-501", "Marcos Teixeira",
            listOf("Juliana Teixeira", "Carlos Freitas", "Marcelo Freitas", "Paula Rocha", "Renata Lima"), ChaveReserva.ENTREGUE)
        reservas += nova(11, 102, hoje, 12, StatusReserva.CONFIRMADA, "C-202", "Otávio Freitas", listOf("Bruna Freitas", "Diego Alves", "Sônia Freitas"))
        reservas += nova(31, 302, hoje, 8, StatusReserva.CONFIRMADA, "D-104", "Carla Mendonça", listOf("Equipe do vôlei"))
        reservas += nova(41, 401, hoje.plusDays(1), 10, StatusReserva.CONFIRMADA, "B-102", "Patrícia Nunes")
    }

    private fun area(itemId: Long) = areas.first { a -> a.itens.any { it.id == itemId } }

    private fun nova(
        itemId: Long, turnoId: Long, data: LocalDate, convidados: Int, status: StatusReserva, unidade: String, morador: String,
        nomes: List<String> = emptyList(), chave: ChaveReserva? = ChaveReserva.NA_PORTARIA
    ): Reserva {
        val a = area(itemId)
        val i = a.itens.first { it.id == itemId }
        val t = a.turnos.first { it.id == turnoId }
        val id = ++seq
        return Reserva(
            id, "R-%05d".format(id), a.id, a.nome, a.icone, a.cor, i.apelido?.let { "${i.nome} · $it" } ?: i.nome, t.nome, data, t.inicio, t.fim,
            unidade, morador, convidados, status, t.taxa ?: i.taxa, chave, if (chave == ChaveReserva.ENTREGUE) hoje.atTime(14, 10) else null, null,
            a.cancelamentoGratisHoras?.let { data.atTime(t.inicio ?: LocalTime.NOON).minusHours(it.toLong()) }, a.maxConvidados?.let { m -> minOf(m, i.capacidade ?: m) },
            null, nomes.mapIndexed { n, nome -> Convidado(id * 100 + n, nome, null, false, null) }
        )
    }

    private class DemoError(val error: AppError) : RuntimeException()

    private fun falha(msg: String): Nothing = throw DemoError(AppError.Validation(msg))

    private suspend fun <T> op(block: () -> T): Outcome<T> {
        delay(250)
        return lock.withLock { try { Outcome.Ok(block()) } catch (e: DemoError) { Outcome.Err(e.error) } }
    }

    override suspend fun areas(): Outcome<List<AreaComum>> = op { areas }

    /** ocupação fixa por dia: alguns horários já reservados por outras unidades (parece um condomínio de verdade) */
    private fun ocupadoFixo(d: LocalDate, itemId: Long, turnoId: Long) = ((d.dayOfYear * 7 + itemId * 3 + turnoId) % 5L) == 0L

    override suspend fun disponibilidade(areaId: Long, mes: YearMonth): Outcome<Disponibilidade> = op {
        val a = areas.first { it.id == areaId }
        val dias = (1..mes.lengthOfMonth()).map { dia ->
            val d = mes.atDay(dia)
            val situacao = when {
                d.isBefore(hoje) -> SituacaoDia.PASSADO
                d.isBefore(hoje.plusDays(a.antecedenciaMinDias.toLong())) || d.isAfter(hoje.plusDays(a.antecedenciaMaxDias.toLong())) -> SituacaoDia.FORA
                else -> null
            }
            val itens = a.itens.map { i ->
                val turnos = a.turnos.map { t ->
                    val minha = reservas.firstOrNull { it.ativa && it.data == d && it.areaId == a.id && it.itemNome?.startsWith(i.nome) == true && it.turnoNome == t.nome }
                    TurnoDia(t.id, when {
                        situacao != null -> situacao
                        i.manutencao != null -> SituacaoDia.MANUTENCAO
                        minha != null -> SituacaoDia.RESERVADO
                        ocupadoFixo(d, i.id, t.id) -> SituacaoDia.OCUPADA
                        else -> SituacaoDia.LIVRE
                    }, minha?.unidade)
                }
                ItemDia(i.id, if (i.manutencao != null) SituacaoDia.MANUTENCAO else situacao ?: SituacaoDia.LIVRE, i.manutencao, turnos)
            }
            val livres = itens.flatMap { it.turnos }.count { it.situacao == SituacaoDia.LIVRE }
            val total = itens.flatMap { it.turnos }.size
            DiaDisponibilidade(d, situacao ?: when {
                livres == 0 -> SituacaoDia.LOTADO
                livres < total -> SituacaoDia.PARCIAL
                else -> SituacaoDia.LIVRE
            }, null, itens)
        }
        Disponibilidade(areaId, mes, hoje, dias)
    }

    override suspend fun minhas(): Outcome<List<Reserva>> = op { reservas.filter { it.unidade == "B-204" }.sortedBy { it.data } }

    override suspend fun criar(form: NovaReserva): Outcome<Reserva> = op {
        val a = areas.first { it.id == form.areaId }
        val i = a.itens.first { it.id == form.itemId }
        if (i.manutencao != null) falha("Este espaço está em manutenção nesta data.")
        val limite = minOf(a.maxConvidados ?: Int.MAX_VALUE, i.capacidade ?: Int.MAX_VALUE)
        if (form.convidados > limite) falha("O limite deste espaço é de $limite convidados.")
        if (a.exigeTermo) {
            if (!form.aceite) falha("Para continuar, leia e aceite o termo de uso.")
            when (form.modo) {
                ModoAssinatura.DESENHO -> if (form.assinatura == null) falha("Desenhe sua assinatura no quadro (ou escolha “Digitar”).")
                ModoAssinatura.DIGITADA -> if (form.nomeDigitado.isNullOrBlank()) falha("Digite seu nome completo para assinar.")
                null -> falha("Escolha como prefere assinar o termo.")
            }
        }
        if (ocupadoFixo(form.data, form.itemId, form.turnoId)) falha("Este horário acabou de ser reservado por outra unidade. Escolha outro.")
        val turno = a.turnos.first { it.id == form.turnoId }
        if (reservas.any { it.ativa && it.data == form.data && it.itemNome?.startsWith(i.nome) == true && it.turnoNome == turno.nome })
            falha("Este horário já está reservado.")
        nova(form.itemId, form.turnoId, form.data, form.convidados, if (a.exigeAprovacao) StatusReserva.AGUARDANDO_APROVACAO else StatusReserva.CONFIRMADA,
            "B-204", "Ana Souza", form.lista.filter { it.isNotBlank() }).also { reservas += it }
    }

    private fun troca(id: Long, f: (Reserva) -> Reserva): Reserva {
        val i = reservas.indexOfFirst { it.id == id }
        if (i < 0) throw DemoError(AppError.NotFound)
        return f(reservas[i]).also { reservas[i] = it }
    }

    override suspend fun cancelar(id: Long): Outcome<Cancelamento> = op {
        val r = troca(id) { if (!it.ativa) falha("Esta reserva já foi cancelada.") else it.copy(status = StatusReserva.CANCELADA) }
        val mantida = r.cancelamentoGratisAte?.let { time.now().isAfter(it) } ?: false
        Cancelamento(r, mantida, if (mantida) "Cancelada fora do prazo: a taxa continua no boleto." else "Reserva cancelada sem custo.")
    }

    override suspend fun desfazerCancelamento(id: Long): Outcome<Reserva> = op { troca(id) { it.copy(status = StatusReserva.CONFIRMADA) } }

    override suspend fun atualizarLista(id: Long, nomes: List<String>): Outcome<Reserva> = op {
        troca(id) { r -> r.copy(lista = nomes.filter { it.isNotBlank() }.mapIndexed { n, nome -> Convidado(id * 100 + n, nome.trim(), null, false, null) }) }
    }

    override suspend fun portaria(): Outcome<ReservasPortaria> = op {
        ReservasPortaria(
            reservas.filter { it.ativa && it.data == hoje }.sortedBy { it.inicio },
            reservas.filter { it.ativa && it.data == hoje.plusDays(1) }.sortedBy { it.inicio }
        )
    }

    override suspend fun checkin(reservaId: Long, convidadoId: Long): Outcome<Convidado> = op {
        var feito: Convidado? = null
        troca(reservaId) { r ->
            r.copy(lista = r.lista.map { c ->
                if (c.id == convidadoId) c.copy(chegouEm = if (c.chegouEm == null) time.now() else null).also { feito = it } else c
            })
        }
        feito ?: throw DemoError(AppError.NotFound)
    }

    override suspend fun incluirConvidado(reservaId: Long, nome: String): Outcome<Convidado> = op {
        if (nome.isBlank()) falha("Digite o nome do convidado.")
        val c = Convidado(++seq, nome.trim(), null, true, time.now())
        troca(reservaId) { it.copy(lista = it.lista + c) }
        c
    }

    override suspend fun entregarChave(id: Long): Outcome<Reserva> = op { troca(id) { it.copy(chave = ChaveReserva.ENTREGUE, chaveEntregueEm = time.now()) } }
    override suspend fun desfazerChave(id: Long): Outcome<Reserva> = op { troca(id) { it.copy(chave = ChaveReserva.NA_PORTARIA, chaveEntregueEm = null, chaveDevolvidaEm = null) } }
    override suspend fun devolucao(id: Long, ok: Boolean, observacoes: String?): Outcome<Reserva> =
        op { troca(id) { it.copy(chave = ChaveReserva.DEVOLVIDA, chaveDevolvidaEm = time.now()) } }
}
