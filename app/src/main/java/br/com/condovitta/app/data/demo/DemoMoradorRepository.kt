package br.com.condovitta.app.data.demo

import br.com.condovitta.app.core.common.TimeProvider
import br.com.condovitta.app.domain.AppError
import br.com.condovitta.app.domain.Autorizacao
import br.com.condovitta.app.domain.AutorizadoEncomenda
import br.com.condovitta.app.domain.Aviso
import br.com.condovitta.app.domain.Boleto
import br.com.condovitta.app.domain.DiaSemana
import br.com.condovitta.app.domain.Encomenda
import br.com.condovitta.app.domain.EscopoAutorizado
import br.com.condovitta.app.domain.Financeiro
import br.com.condovitta.app.domain.ItemBoleto
import br.com.condovitta.app.domain.MinhasEncomendas
import br.com.condovitta.app.domain.MinhasVisitas
import br.com.condovitta.app.domain.MoradorRepository
import br.com.condovitta.app.domain.NovaAutorizacao
import br.com.condovitta.app.domain.NovoAutorizado
import br.com.condovitta.app.domain.Outcome
import br.com.condovitta.app.domain.StatusBoleto
import br.com.condovitta.app.domain.StatusVisita
import br.com.condovitta.app.domain.TamanhoEncomenda
import br.com.condovitta.app.domain.TipoRetirada
import br.com.condovitta.app.domain.TipoVisitante
import br.com.condovitta.app.domain.ViaLiberacao
import br.com.condovitta.app.domain.Visita
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

/**
 * A unidade B-204 da Ana Souza no Residencial Vila do Sol 2 (dados do prototype/data.js), com as datas ancoradas no "hoje"
 * do aparelho. Tudo em memória.
 */
@Singleton
class DemoMoradorRepository @Inject constructor(private val time: TimeProvider) : MoradorRepository {

    private val lock = Mutex()
    private var seq = 5000L
    private val hoje: LocalDate get() = time.today()

    private val boletos = mutableListOf<Boleto>()
    private val autorizacoes = mutableListOf<Autorizacao>()
    private val historico = mutableListOf<Visita>()
    private val consultas = mutableListOf<Visita>()
    private val aguardando = mutableListOf<Encomenda>()
    private val retiradas = mutableListOf<Encomenda>()
    private val autorizados = mutableListOf<AutorizadoEncomenda>()
    private val avisos = mutableListOf<Aviso>()

    init {
        seed()
    }

    private fun bd(v: String) = BigDecimal(v)

    private fun seed() {
        val mes = YearMonth.from(hoje)
        val itens = listOf(
            ItemBoleto("Taxa condominial", bd("480.00"), "TAXA"), ItemBoleto("Fundo de reserva (10%)", bd("48.00"), "FUNDO"),
            ItemBoleto("Consumo de gás (mês anterior)", bd("54.40"), "GAS"), ItemBoleto("Reserva churrasqueira 3", bd("30.00"), "RESERVA")
        )
        boletos += Boleto(
            900, mes.toString(), mes.atDay(10).let { if (it.isBefore(hoje)) mes.plusMonths(1).atDay(10) else it }, bd("612.40"), StatusBoleto.EM_ABERTO,
            null, null, null, "03399.63290 64000.000008 00125.201028 4 10110000061240",
            "00020126580014br.gov.bcb.pix0136a1b2c3d4-vila-do-sol-2-condominio5204000053039865802BR5925RESIDENCIAL VILA DO SOL 26007GOIANIA62070503***6304A1B2",
            true, false, null, itens
        )
        listOf("598.10" to 8, "581.70" to 10, "574.30" to 9).forEachIndexed { i, (v, dia) ->
            val m = mes.minusMonths(i + 1L)
            boletos += Boleto(899L - i, m.toString(), m.atDay(10), bd(v), StatusBoleto.PAGO, m.atDay(dia), bd(v), if (i == 2) "Boleto" else "Pix",
                null, null, true, dia > 10, null, listOf(ItemBoleto("Taxa condominial", bd(v), "TAXA")))
        }

        autorizacoes += Autorizacao(
            301, "Juliana Martins", "CPF ***.551.208-**", "(62) 99410-8832", TipoVisitante.PRESTADOR, null, null, null, null, true,
            listOf(DiaSemana.SEG, DiaSemana.QUA, DiaSemana.SEX), LocalTime.of(10, 0), LocalTime.of(17, 0), hoje.plusMonths(6), "Diarista", "418273", true, "Ana Souza"
        )
        autorizacoes += Autorizacao(
            302, "Marcos Lima", null, "(62) 98877-1100", TipoVisitante.VISITA, "QPT6B52", "Kicks Vermelho", hoje.plusDays(2).atTime(19, 0), hoje.plusDays(2).atTime(23, 0),
            false, emptyList(), null, null, null, "Jantar", "552190", true, "Ana Souza"
        )

        fun visita(id: Long, nome: String, tipo: TipoVisitante, status: StatusVisita, h: Int, m: Int, saida: Pair<Int, Int>? = null, obs: String? = null) = Visita(
            id, id + 500, nome, null, null, tipo, 8, "B-204", "Ana Souza", null, null, null, status, null, null,
            if (status == StatusVisita.AGUARDANDO) null else hoje.atTime(h, m), saida?.let { hoje.atTime(it.first, it.second) }, "Carlos Mendes",
            status == StatusVisita.AGUARDANDO, null, obs, if (status == StatusVisita.AGUARDANDO) null else ViaLiberacao.AUTORIZACAO,
            if (status == StatusVisita.AGUARDANDO) time.now().minusMinutes(2) else null, null, null, null, null
        )
        consultas += visita(410, "Bruno Teixeira", TipoVisitante.ENTREGA, StatusVisita.AGUARDANDO, 0, 0, obs = "Entregador · Magalu, entrega de geladeira")
        historico += visita(411, "Juliana Martins", TipoVisitante.PRESTADOR, StatusVisita.SAIU, 10, 2, 16 to 55, "Diarista")

        fun enc(id: Long, transp: String, tam: TamanhoEncomenda, dias: Long, h: Int, mi: Int, local: String, codigo: String, avisos: Int) = Encomenda(
            id, 8, "B-204", "Ana Souza", transp, tam, null, local, false, false, false, hoje.minusDays(dias).atTime(h, mi), "Carlos Mendes", avisos,
            hoje.minusDays(dias).atTime(h, mi), null, null, null, null, null, false, codigo
        )
        aguardando += enc(1, "Mercado Livre", TamanhoEncomenda.MEDIO, 0, 9, 29, "Prateleira B", "4821", 1)
        aguardando += enc(2, "Amazon", TamanhoEncomenda.ENVELOPE, 5, 15, 10, "Prateleira B", "1937", 2)
        retiradas += enc(20, "Shopee", TamanhoEncomenda.PEQUENO, 3, 11, 40, "Prateleira B", "7731", 1)
            .copy(retirada = true, retiradaEm = hoje.minusDays(3).atTime(19, 12), retiradaPor = "Pedro Souza", tipoRetirada = TipoRetirada.MORADOR, entreguePor = "Antônio Ferreira", temAssinatura = true)

        autorizados += AutorizadoEncomenda(1, "Pedro Souza", null, "Morador", EscopoAutorizado.SEMPRE, null, null, true)
        autorizados += AutorizadoEncomenda(2, "Marta Souza", "RG 2.114.009 SSP/GO", "Sogra", EscopoAutorizado.ATE, null, hoje.plusDays(20), false)

        avisos += Aviso(1, "Interrupção de água — sexta", "Na sexta faremos a limpeza semestral das caixas d’água. O fornecimento será interrompido das 9h às 13h em todos os blocos. Deixe água reservada.",
            "Manutenção", "Roberto Lima (síndico)", hoje.minusDays(3).atTime(8, 0), true, true, false, false, null)
        avisos += Aviso(2, "Novo regimento das churrasqueiras", "A partir do dia 15 as reservas das churrasqueiras passam a ser por turno (almoço ou noite) e exigem assinatura do termo no próprio app.",
            "Regras", "Administração", hoje.minusDays(6).atTime(14, 0), false, false, false, false, null)
        avisos += Aviso(3, "Prestação de contas do mês passado", "O balancete já está disponível, com todas as notas fiscais anexadas.",
            "Financeiro", "Administração", hoje.minusDays(7).atTime(10, 0), false, false, true, false, null)
        avisos += Aviso(5, "Festa das crianças", "Pula-pula, pipoca e algodão-doce no salão. Confirme a presença das crianças pelo app.",
            "Social", "Comissão de eventos", hoje.minusDays(17).atTime(9, 0), false, false, true, false, null)
    }

    private class DemoError(val error: AppError) : RuntimeException()

    private fun falha(msg: String): Nothing = throw DemoError(AppError.Validation(msg))

    private suspend fun <T> op(block: () -> T): Outcome<T> {
        delay(250)
        return lock.withLock {
            try { Outcome.Ok(block()) } catch (e: DemoError) { Outcome.Err(e.error) }
        }
    }

    override suspend fun financeiro(): Outcome<Financeiro> = op {
        val ordem = boletos.sortedByDescending { it.competencia }
        Financeiro("B-204", "Residencial Vila do Sol 2", ordem.lastOrNull { it.status == StatusBoleto.EM_ABERTO }, ordem)
    }

    /** PDF mínimo válido (uma página com o texto do boleto), só para o demo abrir no leitor do aparelho. */
    override suspend fun boletoPdf(id: Long): Outcome<ByteArray> = op {
        val b = boletos.firstOrNull { it.id == id } ?: throw DemoError(AppError.NotFound)
        DemoPdf.boleto("Boleto ${b.competencia} - R$ ${b.valor} - ${b.linhaDigitavel ?: ""}")
    }

    override suspend fun pixQr(id: Long): Outcome<ByteArray?> = op { null }

    override suspend fun visitas(): Outcome<MinhasVisitas> = op {
        MinhasVisitas("B-204", autorizacoes.filter { it.ativa }.toList(), historico.toList(), consultas.toList(), 14)
    }

    override suspend fun autorizar(form: NovaAutorizacao): Outcome<Autorizacao> = op {
        if (form.nome.isBlank()) falha("Informe o nome de quem vai entrar.")
        if (form.recorrente && form.dias.isEmpty()) falha("Escolha os dias da semana.")
        if (!form.recorrente && form.inicio != null && form.fim != null && !form.fim.isAfter(form.inicio)) falha("O fim precisa ser depois do início.")
        val a = Autorizacao(
            ++seq, form.nome.trim(), form.documento?.ifBlank { null }, form.telefone?.ifBlank { null }, form.tipo, form.placa?.uppercase()?.ifBlank { null }, null,
            form.inicio, form.fim, form.recorrente, form.dias, form.horaInicio, form.horaFim, form.validoAte, form.obs?.ifBlank { null },
            Random.nextInt(100000, 999999).toString(), true, "Ana Souza"
        )
        autorizacoes += a
        a
    }

    override suspend fun revogar(id: Long): Outcome<Unit> = op {
        val i = autorizacoes.indexOfFirst { it.id == id }
        if (i < 0) throw DemoError(AppError.NotFound)
        autorizacoes[i] = autorizacoes[i].copy(ativa = false)
    }

    override suspend fun reativar(id: Long): Outcome<Autorizacao> = op {
        val i = autorizacoes.indexOfFirst { it.id == id }
        if (i < 0) throw DemoError(AppError.NotFound)
        autorizacoes[i].copy(ativa = true).also { autorizacoes[i] = it }
    }

    override suspend fun qrVisitante(id: Long): Outcome<ByteArray?> = op { null }

    override suspend fun responder(acessoId: Long, liberar: Boolean, recado: String?): Outcome<Visita> = op {
        val c = consultas.firstOrNull { it.acessoId == acessoId } ?: falha("A portaria já resolveu esta visita.")
        consultas.remove(c)
        val r = c.copy(
            status = if (liberar) StatusVisita.LIBERADO else StatusVisita.RECUSADO, respondidoEm = time.now(), respondidoPor = "Ana Souza",
            recado = recado?.ifBlank { null }, via = if (liberar) ViaLiberacao.APP else null
        )
        historico.add(0, r)
        r
    }

    override suspend fun encomendas(): Outcome<MinhasEncomendas> = op {
        MinhasEncomendas("B-204", aguardando.toList(), retiradas.toList(), autorizados.toList())
    }

    override suspend fun autorizarRetirada(form: NovoAutorizado): Outcome<AutorizadoEncomenda> = op {
        if (form.nome.isBlank()) falha("Informe o nome de quem vai retirar.")
        if (form.escopo == EscopoAutorizado.ATE && form.validoAte == null) falha("Informe até quando vale.")
        AutorizadoEncomenda(++seq, form.nome.trim(), form.documento?.ifBlank { null }, form.relacao?.ifBlank { null }, form.escopo, form.encomendaId, form.validoAte, false)
            .also { autorizados += it }
    }

    override suspend fun removerAutorizado(id: Long): Outcome<Unit> = op {
        val a = autorizados.firstOrNull { it.id == id } ?: throw DemoError(AppError.NotFound)
        if (a.fixo) falha("Os moradores da unidade sempre podem retirar.")
        autorizados.remove(a)
        Unit
    }

    override suspend fun avisos(): Outcome<List<Aviso>> = op { avisos.sortedWith(compareByDescending<Aviso> { it.fixado }.thenByDescending { it.publicadoEm }) }

    override suspend fun marcarLido(id: Long): Outcome<Unit> = op {
        val i = avisos.indexOfFirst { it.id == id }
        if (i >= 0) avisos[i] = avisos[i].copy(lido = true)
    }

    override suspend fun ciente(id: Long): Outcome<Unit> = op {
        val i = avisos.indexOfFirst { it.id == id }
        if (i < 0) throw DemoError(AppError.NotFound)
        avisos[i] = avisos[i].copy(lido = true, ciente = true, cienteEm = time.now())
    }
}

/** Gera um PDF de uma página com uma linha de texto (o suficiente para o demo abrir no leitor). */
internal object DemoPdf {
    fun boleto(texto: String): ByteArray {
        val t = texto.replace("(", "[").replace(")", "]").filter { it.code in 0x20..0x7e }
        val stream = "BT /F1 12 Tf 40 780 Td (Condovitta Demo) Tj 0 -24 Td ($t) Tj ET"
        val objs = listOf(
            "<< /Type /Catalog /Pages 2 0 R >>",
            "<< /Type /Pages /Kids [3 0 R] /Count 1 >>",
            "<< /Type /Page /Parent 2 0 R /MediaBox [0 0 595 842] /Contents 4 0 R /Resources << /Font << /F1 5 0 R >> >> >>",
            "<< /Length ${stream.length} >>\nstream\n$stream\nendstream",
            "<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>"
        )
        val sb = StringBuilder("%PDF-1.4\n")
        val offsets = objs.mapIndexed { i, o -> sb.length.also { sb.append("${i + 1} 0 obj\n$o\nendobj\n") } }
        val xref = sb.length
        sb.append("xref\n0 ${objs.size + 1}\n0000000000 65535 f \n")
        offsets.forEach { sb.append(String.format("%010d 00000 n \n", it)) }
        sb.append("trailer\n<< /Size ${objs.size + 1} /Root 1 0 R >>\nstartxref\n$xref\n%%EOF\n")
        return sb.toString().toByteArray(Charsets.ISO_8859_1)
    }
}
