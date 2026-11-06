package br.com.condovitta.app.data.demo

import br.com.condovitta.app.core.common.TimeProvider
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
import br.com.condovitta.app.domain.Outcome
import br.com.condovitta.app.domain.TipoEvento
import br.com.condovitta.app.domain.Transparencia
import br.com.condovitta.app.domain.TransparenciaRepository
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.temporal.TemporalAdjusters
import javax.inject.Inject
import javax.inject.Singleton

/** Prestação de contas, gás do B-204 e o calendário do condomínio. */
@Singleton
class DemoTransparenciaRepository @Inject constructor(time: TimeProvider) : DemoBase(time), TransparenciaRepository {

    private fun bd(v: String) = BigDecimal(v)
    private fun mesAtual() = YearMonth.from(time.today())

    /* ---------------- prestação de contas ---------------- */

    /** os três últimos meses fechados estão publicados */
    private fun publicados() = (1L..3L).map { mesAtual().minusMonths(it).toString() }

    private fun mes(ym: String): Transparencia {
        val k = publicados().indexOf(ym).coerceAtLeast(0)
        val fator = bd("1.00").subtract(bd("0.04").multiply(BigDecimal(k)))
        val receitas = bd("71840.00").multiply(fator).setScale(2, RoundingMode.HALF_UP)
        val cats = listOf(
            CategoriaValor("Pessoal e encargos", "#2563EB", bd("28400.00")),
            CategoriaValor("Água e energia", "#0891B2", bd("12950.00")),
            CategoriaValor("Manutenção", "#D97706", bd("9870.00").multiply(fator).setScale(2, RoundingMode.HALF_UP)),
            CategoriaValor("Limpeza e jardinagem", "#059669", bd("6200.00")),
            CategoriaValor("Administração", "#7C3AED", bd("4300.00")),
            CategoriaValor("Seguro e taxas", "#64748B", bd("2180.00"))
        )
        val despesas = cats.fold(BigDecimal.ZERO) { a, c -> a + c.valor }
        val ymd = YearMonth.parse(ym)
        val lancamentos = listOf(
            LancamentoPublico(1, ymd.atDay(5), "Folha de pagamento · portaria e zeladoria", "Pessoal e encargos", "Vila do Sol Serviços", bd("21800.00"), false),
            LancamentoPublico(2, ymd.atDay(10), "Conta de energia · áreas comuns", "Água e energia", "Equatorial Goiás", bd("7420.00"), true),
            LancamentoPublico(3, ymd.atDay(12), "Conta de água", "Água e energia", "Saneago", bd("5530.00"), true),
            LancamentoPublico(4, ymd.atDay(15), "Manutenção preventiva dos elevadores", "Manutenção", "Elevadores Atlas", bd("3900.00"), true),
            LancamentoPublico(5, ymd.atDay(18), "Poda e jardinagem", "Limpeza e jardinagem", "Jardinagem Verde Vida", bd("2400.00"), true),
            LancamentoPublico(6, ymd.atDay(20), "Honorários da administradora", "Administração", "Condovitta Gestão", bd("4300.00"), true),
            LancamentoPublico(7, ymd.atDay(22), "Troca de bomba da piscina", "Manutenção", "Aqua Piscinas", bd("2870.00"), false)
        ).sortedByDescending { it.data }
        val serie = (5L downTo 0L).map { mesAtual().minusMonths(it + 1) }.map {
            MesValor(it.toString(), bd("68000.00").add(BigDecimal(it.monthValue * 410L)), bd("61000.00").add(BigDecimal(it.monthValue * 530L)))
        }
        return Transparencia(
            ym, publicados(), receitas, despesas, receitas - despesas, bd("3.2"), bd("-1.8"), bd("48230.55"), bd("126400.00"), bd("180000.00"),
            cats, serie, lancamentos, ymd.plusMonths(1).atDay(8).atTime(17, 30)
        )
    }

    override suspend fun transparencia(mes: String?): Outcome<Transparencia> = op {
        val ym = mes ?: publicados().first()
        if (ym !in publicados()) naoAchou()
        mes(ym)
    }

    override suspend fun balancetePdf(mes: String): Outcome<ByteArray> = op { DemoPdf.boleto("Balancete de $mes") }
    override suspend fun nota(lancamentoId: Long): Outcome<ByteArray> = op { DemoPdf.boleto("Nota fiscal do lancamento $lancamentoId") }

    override suspend fun balancetes(): Outcome<List<BalanceteHistorico>> = op {
        (4L..9L).map { mesAtual().minusMonths(it) }.map {
            BalanceteHistorico(it.monthValue.toLong(), it.toString(), bd("69500.00"), bd("64100.00"), bd("5400.00"), "balancete-$it.pdf", true, null)
        }
    }

    override suspend fun balanceteArquivo(id: Long): Outcome<ByteArray> = op { DemoPdf.boleto("Balancete importado $id") }

    /* ---------------- gás ---------------- */

    private var contestada = false

    override suspend fun meuGas(): Outcome<MeuGas> = op {
        val preco = bd("8.40")
        val consumos = listOf("11.2", "10.8", "12.5", "13.1", "14.6", "15.2", "13.9", "12.4", "11.7", "10.9", "12.8", "15.9").map(::bd)
        val medias = listOf("10.9", "10.5", "11.8", "12.6", "13.4", "14.1", "13.2", "12.0", "11.3", "10.8", "11.6", "12.2").map(::bd)
        val meses = (11 downTo 0).map { mesAtual().minusMonths(it.toLong()) }
        var leitura = bd("1186.420")
        val historico = meses.mapIndexed { i, m ->
            val anterior = leitura
            leitura += consumos[i]
            GasHistorico(1000L + i, m.toString(), leitura, anterior, consumos[i], consumos[i].multiply(preco).setScale(2, RoundingMode.HALF_UP),
                "Carlos Mendes", m.atDay(3).atTime(9, 20), consumos[i] > medias[i].multiply(bd("1.2")), i == 4)
        }.reversed()
        val u = historico.first()
        MeuGas(
            "B-204", "Bloco B", preco, bd("4.90"),
            GasUltima(u.leituraId, u.mes, u.atual, u.anterior, u.consumo, u.valor, u.lidaEm, u.lidoPor, true, false, false, contestada),
            historico[1].consumo, medias.last(), consumos.fold(BigDecimal.ZERO) { a, b -> a + b }.divide(BigDecimal(12), 1, RoundingMode.HALF_UP),
            meses.last().toString(), consumos.maxOrNull(),
            meses.mapIndexed { i, m -> GasConsumoMes(m.toString(), consumos[i], medias[i]) }, historico, true
        )
    }

    override suspend fun contestarGas(mensagem: String?): Outcome<Unit> = op {
        if (contestada) falha("Você já contestou esta leitura. A administração vai responder.")
        contestada = true
    }

    override suspend fun fotoGas(leituraId: Long): Outcome<ByteArray> = op { DemoPdf.boleto("Foto do medidor · leitura $leituraId") }

    /* ---------------- calendário ---------------- */

    private val lembretes = mutableMapOf<Pair<Long, LocalDate>, Int>()

    private fun eventosDoMes(ym: YearMonth): List<EventoCalendario> {
        val hoje = time.today()
        fun ev(id: Long, d: LocalDate, ini: LocalTime?, fim: LocalTime?, t: String, tipo: TipoEvento, local: String?, desc: String?, rec: String? = null, forn: String? = null, blocos: List<String> = emptyList()) =
            EventoCalendario("e$id:$d", id, d, ini, fim, t, tipo, local, blocos, desc, rec, forn)
        val terca = ym.atDay(1).with(TemporalAdjusters.firstInMonth(DayOfWeek.TUESDAY))
        val lista = mutableListOf(
            ev(1, terca, LocalTime.of(8, 0), LocalTime.of(12, 0), "Manutenção preventiva dos elevadores", TipoEvento.MANUTENCAO, "Blocos A e B",
                "Um elevador por vez fica parado durante a manutenção.", "MENSAL", "Elevadores Atlas", listOf("A", "B")),
            ev(2, ym.atDay(15), LocalTime.of(7, 0), LocalTime.of(11, 0), "Limpeza da caixa d'água", TipoEvento.MANUTENCAO, "Torre B",
                "Falta de água no Bloco B das 7h às 11h.", null, "Hidro Limp", listOf("B")),
            ev(3, ym.atDay(ym.lengthOfMonth().coerceAtMost(28)), LocalTime.of(16, 0), LocalTime.of(20, 0), "Festa da criançada", TipoEvento.SOCIAL, "Salão de festas",
                "Pula-pula, pipoca e algodão-doce. Traga as crianças!")
        )
        if (ym == YearMonth.from(hoje)) {
            lista += ev(4, hoje, LocalTime.of(19, 0), LocalTime.of(22, 0), "AGE de novembro", TipoEvento.ASSEMBLEIA, "Online · Salão de festas", "Troca do portão da garagem e assuntos gerais.")
            lista += ev(5, hoje.plusDays(2), LocalTime.of(9, 0), null, "Dedetização das áreas comuns", TipoEvento.OBRA, "Garagem e áreas comuns", "Mantenha os pets dentro de casa.", null, "Dedetiza Já")
            lista += ev(6, hoje.plusDays(6), LocalTime.of(19, 30), LocalTime.of(21, 0), "Reunião do conselho", TipoEvento.REUNIAO, "Sala da administração", null)
        }
        return lista
    }

    override suspend fun calendario(de: LocalDate, ate: LocalDate): Outcome<List<EventoCalendario>> = op {
        if (ate.isBefore(de)) falha("Período inválido.")
        generateSequence(YearMonth.from(de)) { it.plusMonths(1) }.takeWhile { !it.isAfter(YearMonth.from(ate)) }
            .flatMap { eventosDoMes(it) }.filter { !it.data.isBefore(de) && !it.data.isAfter(ate) }
            .sortedWith(compareBy({ it.data }, { it.inicio })).toList()
    }

    override suspend fun lembretes(): Outcome<List<Lembrete>> = op { lembretes.map { (k, v) -> Lembrete(k.first, k.second, v) } }

    override suspend fun lembrete(eventoId: Long, data: LocalDate, minutosAntes: Int?): Outcome<Unit> = op {
        if (data.isBefore(time.today())) falha("Este evento já aconteceu.")
        if (minutosAntes == null || minutosAntes <= 0) lembretes.remove(eventoId to data) else lembretes[eventoId to data] = minutosAntes
    }

    override suspend fun ics(eventoId: Long, data: LocalDate): Outcome<ByteArray> = op {
        val d = data.toString().replace("-", "")
        ("BEGIN:VCALENDAR\r\nVERSION:2.0\r\nPRODID:-//Condovitta//Demo//PT\r\nBEGIN:VEVENT\r\nUID:e$eventoId-$d@condovitta\r\n" +
            "DTSTART;VALUE=DATE:$d\r\nSUMMARY:Evento do condomínio\r\nEND:VEVENT\r\nEND:VCALENDAR\r\n").toByteArray()
    }
}
