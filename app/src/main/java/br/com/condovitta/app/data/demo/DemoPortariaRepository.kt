package br.com.condovitta.app.data.demo

import br.com.condovitta.app.core.common.TimeProvider
import br.com.condovitta.app.domain.AcessoListas
import br.com.condovitta.app.domain.AppError
import br.com.condovitta.app.domain.AutorizadoRetirada
import br.com.condovitta.app.domain.Bloqueado
import br.com.condovitta.app.domain.Busca
import br.com.condovitta.app.domain.Destinatarios
import br.com.condovitta.app.domain.Encomenda
import br.com.condovitta.app.domain.EncomendasPortaria
import br.com.condovitta.app.domain.Entrega
import br.com.condovitta.app.domain.NovaEncomenda
import br.com.condovitta.app.domain.NovaVisita
import br.com.condovitta.app.domain.Outcome
import br.com.condovitta.app.domain.Painel
import br.com.condovitta.app.domain.PortariaRepository
import br.com.condovitta.app.domain.StatusVisita
import br.com.condovitta.app.domain.TamanhoEncomenda
import br.com.condovitta.app.domain.TipoRetirada
import br.com.condovitta.app.domain.TipoVisitante
import br.com.condovitta.app.domain.UnidadeBusca
import br.com.condovitta.app.domain.UnidadeResumo
import br.com.condovitta.app.domain.VeiculoBusca
import br.com.condovitta.app.domain.ViaLiberacao
import br.com.condovitta.app.domain.Visita
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.text.Normalizer
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

/**
 * Portaria de exemplo do Residencial Vila do Sol 2 (os mesmos visitantes, encomendas e bloqueados do prototype/data.js),
 * com as datas ancoradas no "hoje" do aparelho. Tudo em memória: as ações mudam o estado até o app fechar.
 */
@Singleton
class DemoPortariaRepository @Inject constructor(private val time: TimeProvider) : PortariaRepository, DemoCodigo {

    private val lock = Mutex()
    private var seq = 1000L
    private val hoje: LocalDate get() = time.today()

    val unidades: List<UnidadeResumo> = RESPONSAVEIS.entries.mapIndexed { i, (rotulo, nome) ->
        UnidadeResumo(i + 1L, rotulo, rotulo.take(1), nome, 1 + (i % 3))
    }
    private fun unidade(rotulo: String) = unidades.first { it.rotulo == rotulo }
    private fun unidadeId(rotulo: String) = unidade(rotulo).id

    private val visitas = mutableListOf<Visita>()
    private val encomendas = mutableListOf<Encomenda>()
    private val codigos = mutableMapOf<Long, String>()
    /** como estava antes da última ação, para o "Desfazer" */
    private val anteriores = mutableMapOf<Long, Visita>()
    private val bloqueados = listOf(
        Bloqueado(1, "Gilberto Ramos", "CPF ***.218.554-**", "Tentou entrar usando o nome de outro morador.", "Roberto Lima (síndico)", LocalDate.of(2026, 9, 14)),
        Bloqueado(2, "Kleber Santana", "vendedor ambulante", "Venda de porta em porta, proibida pelo regimento.", "Administração", LocalDate.of(2026, 6, 2))
    )
    private val veiculos = listOf(
        VeiculoBusca("SBA2C04", "Compass", "Branco", "B-204", "Ana Souza", "TAG 004821", false, null),
        VeiculoBusca("RXL1A02", "Corolla Cross", "Preto", "A-102", "Roberto Lima", "Leitura de placa", false, null),
        VeiculoBusca("QPT6B52", "Kicks", "Vermelho", "B-204", "Ana Souza", "TAG 004822", false, null),
        VeiculoBusca("RVX3C12", "HB20", "Prata", "C-303", "Larissa Alves", "TAG 003310", true, "Condomínio em atraso há 3 meses")
    )

    init {
        seed()
    }

    private fun at(h: Int, m: Int, dia: LocalDate = hoje) = dia.atTime(h, m)

    private fun visita(
        id: Long, nome: String, tipo: TipoVisitante, unidade: String?, inicio: String, fim: String, status: StatusVisita,
        doc: String? = null, tel: String? = null, obs: String? = null, placa: String? = null, carro: String? = null,
        via: ViaLiberacao? = ViaLiberacao.AUTORIZACAO, daPortaria: Boolean = false, recorrencia: String? = null,
        entrada: LocalDateTime? = null, saida: LocalDateTime? = null
    ) = Visita(
        visitanteId = id, acessoId = if (status == StatusVisita.ESPERADO) null else id + 500, nome = nome, documento = doc, telefone = tel,
        tipo = tipo, unidadeId = unidade?.let { unidadeId(it) }, unidade = unidade ?: "Condomínio",
        morador = unidade?.let { RESPONSAVEIS[it] } ?: "Administração", placa = placa,
        veiculoModelo = carro?.substringBeforeLast(' '), veiculoCor = carro?.substringAfterLast(' '), status = status,
        inicio = inicio, fim = fim, entradaEm = entrada, saidaEm = saida, porteiro = if (entrada != null) "Carlos Mendes" else null,
        daPortaria = daPortaria, recorrencia = recorrencia, obs = obs, via = if (status == StatusVisita.ESPERADO) null else via,
        consultadoEm = if (status == StatusVisita.AGUARDANDO) at(9, 38) else null, respondidoEm = null, respondidoPor = null,
        recado = null, autorizadoPor = if (daPortaria) "Carlos Mendes" else unidade?.let { RESPONSAVEIS[it] }
    )

    private fun seed() {
        visitas += listOf(
            visita(1, "Lucas Prado", TipoVisitante.VISITA, "C-303", "14:00", "18:00", StatusVisita.ESPERADO, "RG 5.482.119 SSP/GO", "(62) 99654-2210", placa = "RTA4B21", carro = "Onix Prata"),
            visita(2, "Juliana Martins", TipoVisitante.PRESTADOR, "B-204", "10:00", "17:00", StatusVisita.ESPERADO, "CPF ***.551.208-**", "(62) 99410-8832", obs = "Diarista", recorrencia = "seg, qua e sex"),
            visita(3, "Marcelo Dias", TipoVisitante.PRESTADOR, "D-402", "10:00", "12:00", StatusVisita.ESPERADO, "CPF ***.903.447-**", "(62) 3201-4400", obs = "Vivo Fibra · instalação", placa = "QPF2E81", carro = "Fiorino Branca"),
            visita(7, "Rafael Torres", TipoVisitante.VISITA, "D-201", "09:38", "12:00", StatusVisita.AGUARDANDO, "CPF ***.320.118-**", "(62) 99877-1203", daPortaria = true, via = null),
            visita(4, "Marcelo Freitas", TipoVisitante.VISITA, "A-501", "18:00", "23:59", StatusVisita.ESPERADO, "RG 3.118.702 SSP/DF", "(61) 98122-7710", obs = "Convidado da festa no salão", placa = "SBC7H44", carro = "HB20 Preto"),
            visita(8, "Rogério Campos", TipoVisitante.PRESTADOR, null, "08:00", "12:00", StatusVisita.DENTRO, "CPF ***.118.620-**", "(62) 3241-0099", obs = "Elevadores Atlas · preventiva Bloco A", placa = "RKO5D33", carro = "Strada Branca", daPortaria = true, via = ViaLiberacao.INTERFONE, entrada = at(8, 5)),
            visita(9, "Patrícia Nunes", TipoVisitante.VISITA, "B-102", "07:30", "09:30", StatusVisita.DENTRO, "CPF ***.774.301-**", "(62) 98450-6612", placa = "PQA1B23", carro = "Kwid Vermelho", entrada = at(7, 41)),
            visita(11, "Amanda Lopes", TipoVisitante.PRESTADOR, "D-305", "07:00", "19:00", StatusVisita.DENTRO, "RG 6.210.554 SSP/GO", "(62) 99812-3390", obs = "Babá", recorrencia = "seg a sex", entrada = at(6, 58)),
            visita(13, "Thiago Barbosa", TipoVisitante.VISITA, "C-501", "06:30", "08:30", StatusVisita.SAIU, "CPF ***.630.552-**", "(62) 99333-1020", entrada = at(6, 41), saida = at(7, 55))
        )
        fun enc(id: Long, rotulo: String, transp: String, tam: TamanhoEncomenda, recebida: LocalDateTime, local: String, codigo: String, avisos: Int, perecivel: Boolean = false, retirada: LocalDateTime? = null, por: String? = null) =
            Encomenda(
                id, unidadeId(rotulo), rotulo, RESPONSAVEIS[rotulo], transp, tam, null, local, perecivel, false, retirada != null, recebida,
                "Carlos Mendes", avisos, recebida, retirada, por, null, if (retirada != null) TipoRetirada.MORADOR else null,
                if (retirada != null) "Carlos Mendes" else null, retirada != null
            ).also { codigos[id] = codigo }
        encomendas += listOf(
            enc(1, "B-204", "Mercado Livre", TamanhoEncomenda.MEDIO, at(9, 29), "Prateleira B", "4821", 1),
            enc(2, "B-204", "Amazon", TamanhoEncomenda.ENVELOPE, at(15, 10, hoje.minusDays(5)), "Prateleira B", "1937", 2),
            enc(6, "A-205", "Amazon", TamanhoEncomenda.PEQUENO, at(8, 15), "Prateleira A", "7700", 1),
            enc(12, "D-601", "iFood Mercado", TamanhoEncomenda.MEDIO, at(9, 5), "Geladeira da portaria", "2214", 1, perecivel = true),
            enc(3, "C-305", "Correios", TamanhoEncomenda.GRANDE, at(11, 2, hoje.minusDays(12)), "Sala de volumes", "5540", 4),
            enc(9, "B-503", "Loggi", TamanhoEncomenda.PEQUENO, at(9, 12, hoje.minusDays(1)), "Prateleira B", "8831", 1),
            enc(15, "A-302", "Amazon", TamanhoEncomenda.MEDIO, at(7, 58), "Prateleira A", "3390", 1, retirada = at(8, 40), por = "Vinícius Ramos"),
            enc(16, "C-501", "Mercado Livre", TamanhoEncomenda.PEQUENO, at(15, 20, hoje.minusDays(1)), "Prateleira C", "9021", 1, retirada = at(7, 10), por = "Thiago Barros")
        )
    }

    private suspend fun <T> op(block: () -> T): Outcome<T> {
        delay(250)
        return lock.withLock {
            try {
                Outcome.Ok(block())
            } catch (e: DemoError) {
                Outcome.Err(e.error)
            }
        }
    }

    private class DemoError(val error: AppError) : RuntimeException()

    private fun falha(msg: String): Nothing = throw DemoError(AppError.Validation(msg))

    private fun listas() = AcessoListas(
        esperados = visitas.filter { it.status.pendente }.sortedBy { it.inicio },
        dentro = visitas.filter { it.status == StatusVisita.DENTRO }.sortedByDescending { it.entradaEm },
        historico = visitas.filter { it.status == StatusVisita.SAIU || it.status == StatusVisita.RECUSADO }.sortedByDescending { it.saidaEm ?: it.entradaEm },
        bloqueados = bloqueados.size
    )

    private fun aguardando() = encomendas.filter { !it.retirada }.sortedByDescending { it.recebidaEm }

    override suspend fun painel(): Outcome<Painel> = op {
        val l = listas()
        val agora = time.now().toLocalTime()
        val (turno, faixa, proximo) = when {
            agora.isBefore(LocalTime.of(6, 0)) -> Triple("Noite", "22h – 06h", "Manhã")
            agora.isBefore(LocalTime.of(14, 0)) -> Triple("Manhã", "06h – 14h", "Tarde")
            agora.isBefore(LocalTime.of(22, 0)) -> Triple("Tarde", "14h – 22h", "Noite")
            else -> Triple("Noite", "22h – 06h", "Manhã")
        }
        val ag = aguardando()
        Painel(turno, faixa, proximo, l.esperados, l.dentro, ag.size.toLong(),
            ag.count { it.recebidaEm?.toLocalDate()?.isBefore(hoje.minusDays(6)) == true }.toLong(), ag.filter { it.perecivel })
    }

    override suspend fun busca(q: String): Outcome<Busca> = op {
        val n = norm(q)
        if (n.length < 2) return@op Busca(emptyList(), emptyList(), emptyList(), emptyList())
        Busca(
            bloqueados = bloqueados.filter { norm(it.nome).contains(n) },
            visitantes = visitas.filter { norm(it.nome).contains(n) || norm(it.unidade).contains(n) || norm(it.placa).contains(n) }.take(5),
            unidades = unidades.filter { norm(it.rotulo).contains(n) || norm(it.responsavel).contains(n) }.take(4).map { u ->
                UnidadeBusca(u.id, u.rotulo, u.responsavel, u.moradores, encomendas.count { !it.retirada && it.unidadeId == u.id }.toLong())
            },
            veiculos = veiculos.filter { norm(it.placa).contains(n) || norm(it.unidade).contains(n) }.take(4)
        )
    }

    override suspend fun unidades(): Outcome<List<UnidadeResumo>> = op { unidades }
    override suspend fun acesso(): Outcome<AcessoListas> = op { listas() }
    override suspend fun bloqueados(): Outcome<List<Bloqueado>> = op { bloqueados }

    override suspend fun novaVisita(form: NovaVisita): Outcome<Visita> = op {
        if (form.nome.isBlank()) falha("Informe o nome do visitante.")
        if (!form.fim.isAfter(form.inicio)) falha("A hora de fim precisa ser depois da hora de início.")
        bloqueados.firstOrNull { norm(it.nome) == norm(form.nome) }?.let { falha("${it.nome} está bloqueado(a): ${it.motivo} Fale com o síndico antes de liberar.") }
        val u = form.unidadeId?.let { id -> unidades.first { it.id == id } }
        val id = ++seq
        val v = Visita(
            id, if (form.consultar) id + 500 else null, form.nome.trim(), form.documento?.ifBlank { null }, form.telefone?.ifBlank { null }, form.tipo,
            u?.id, u?.rotulo ?: "Condomínio", u?.responsavel ?: "Administração", form.placa?.uppercase()?.ifBlank { null }, null, null,
            if (form.consultar && u != null) StatusVisita.AGUARDANDO else StatusVisita.ESPERADO,
            form.inicio.toString().take(5), form.fim.toString().take(5), null, null, null, true, null, form.obs?.ifBlank { null }, null,
            if (form.consultar) time.now() else null, null, null, null, "Carlos Mendes"
        )
        visitas += v
        v
    }

    override suspend fun excluirVisita(visitanteId: Long): Outcome<Unit> = op {
        val v = visitas.firstOrNull { it.visitanteId == visitanteId } ?: throw DemoError(AppError.NotFound)
        if (v.status == StatusVisita.DENTRO || v.status == StatusVisita.SAIU) throw DemoError(AppError.Validation("Este visitante já entrou; registre a saída em vez de excluir."))
        visitas.remove(v)
        Unit
    }

    private fun troca(pred: (Visita) -> Boolean, f: (Visita) -> Visita): Visita {
        val i = visitas.indexOfFirst(pred)
        if (i < 0) throw DemoError(AppError.NotFound)
        val antes = visitas[i]
        val depois = f(antes)
        visitas[i] = depois
        depois.acessoId?.let { anteriores[it] = antes }
        return depois
    }

    override suspend fun entrada(visitanteId: Long, via: ViaLiberacao?): Outcome<Visita> = op {
        troca({ it.visitanteId == visitanteId }) { v ->
            if (v.status == StatusVisita.DENTRO) falha("${v.nome} já está no condomínio.")
            v.copy(
                acessoId = v.acessoId ?: (v.visitanteId + 500), status = StatusVisita.DENTRO, entradaEm = time.now(), porteiro = "Carlos Mendes",
                via = via ?: v.via ?: if (v.status == StatusVisita.LIBERADO) ViaLiberacao.APP else ViaLiberacao.AUTORIZACAO,
                respondidoEm = if (via == ViaLiberacao.INTERFONE) time.now() else v.respondidoEm
            )
        }
    }

    override suspend fun consultar(visitanteId: Long): Outcome<Visita> = op {
        troca({ it.visitanteId == visitanteId }) { v ->
            if (v.unidadeId == null) falha("Visita para o condomínio não tem morador para consultar.")
            v.copy(acessoId = v.acessoId ?: (v.visitanteId + 500), status = StatusVisita.AGUARDANDO, consultadoEm = time.now())
        }
    }

    override suspend fun recusar(acessoId: Long): Outcome<Visita> = op {
        troca({ it.acessoId == acessoId }) { it.copy(status = StatusVisita.RECUSADO, saidaEm = time.now()) }
    }

    override suspend fun saida(acessoId: Long): Outcome<Visita> = op {
        troca({ it.acessoId == acessoId }) { v ->
            if (v.status != StatusVisita.DENTRO) falha("${v.nome} não está no condomínio.")
            v.copy(status = StatusVisita.SAIU, saidaEm = time.now())
        }
    }

    override suspend fun desfazer(acessoId: Long): Outcome<Unit> = op {
        val antes = anteriores.remove(acessoId) ?: falha("Não há o que desfazer.")
        val i = visitas.indexOfFirst { it.visitanteId == antes.visitanteId }
        if (i >= 0) visitas[i] = antes
        Unit
    }

    override suspend fun encomendas(): Outcome<EncomendasPortaria> = op {
        val entregues = encomendas.filter { it.retirada && it.retiradaEm?.toLocalDate() == hoje }.sortedByDescending { it.retiradaEm }
        EncomendasPortaria(aguardando(), entregues, encomendas.count { it.recebidaEm?.toLocalDate() == hoje }.toLong(), entregues.size.toLong())
    }

    override suspend fun destinatarios(unidadeId: Long): Outcome<Destinatarios> = op {
        val u = unidades.first { it.id == unidadeId }
        val extras = if (u.rotulo == "B-204") listOf("Pedro Souza") else emptyList()
        Destinatarios(u.rotulo, listOfNotNull(u.responsavel) + extras,
            if (u.rotulo == "D-305") listOf(AutorizadoRetirada("Amanda Lopes", "RG 6.210.554 SSP/GO", "Babá")) else emptyList())
    }

    override suspend fun registrarEncomenda(form: NovaEncomenda, foto: ByteArray?): Outcome<Encomenda> = op {
        val u = unidades.firstOrNull { it.id == form.unidadeId } ?: falha("Escolha a unidade do destinatário.")
        if (form.transportadora.isBlank()) falha("Escolha a transportadora.")
        val id = ++seq
        val e = Encomenda(
            id, u.id, u.rotulo, form.destinatario ?: u.responsavel, form.transportadora, form.tamanho, form.rastreio, form.local, form.perecivel,
            foto != null, false, time.now(), "Carlos Mendes", 1, time.now(), null, null, null, null, null, false
        )
        codigos[id] = Random.nextInt(1000, 9999).toString()
        fotos[id] = foto
        encomendas += e
        e
    }

    private val fotos = mutableMapOf<Long, ByteArray?>()

    /** o código de retirada (no app real só o morador vê) */
    override fun codigo(id: Long): String? = codigos[id]

    override suspend fun excluirEncomenda(id: Long): Outcome<Unit> = op {
        if (!encomendas.removeAll { it.id == id }) throw DemoError(AppError.NotFound)
        Unit
    }

    private fun trocaEnc(id: Long, f: (Encomenda) -> Encomenda): Encomenda {
        val i = encomendas.indexOfFirst { it.id == id }
        if (i < 0) throw DemoError(AppError.NotFound)
        return f(encomendas[i]).also { encomendas[i] = it }
    }

    override suspend fun avisar(id: Long): Outcome<Encomenda> = op { trocaEnc(id) { it.copy(avisos = it.avisos + 1, ultimoAvisoEm = time.now()) } }

    override suspend fun entregar(id: Long, entrega: Entrega): Outcome<Encomenda> = op {
        trocaEnc(id) { e ->
            if (e.retirada) falha("Esta encomenda já foi entregue.")
            val quem = when (entrega.modo) {
                TipoRetirada.MORADOR -> {
                    if (codigos[id] != entrega.codigo?.filter { it.isDigit() }) falha("Código de retirada não confere. Peça para o morador conferir no app.")
                    e.destinatario ?: "Morador"
                }
                TipoRetirada.TERCEIRO -> {
                    if (entrega.nome.isNullOrBlank() || entrega.documento.isNullOrBlank()) falha("Informe o nome e o documento de quem está retirando.")
                    entrega.nome.trim()
                }
            }
            if (entrega.assinatura.isEmpty()) falha("Peça para assinar no quadro antes de confirmar.")
            e.copy(retirada = true, retiradaEm = time.now(), retiradaPor = quem, retiradaDocumento = entrega.documento, tipoRetirada = entrega.modo, entreguePor = "Carlos Mendes", temAssinatura = true)
        }
    }

    override suspend fun desfazerEntrega(id: Long): Outcome<Encomenda> = op {
        trocaEnc(id) { it.copy(retirada = false, retiradaEm = null, retiradaPor = null, retiradaDocumento = null, tipoRetirada = null, entreguePor = null, temAssinatura = false) }
    }

    override suspend fun fotoEncomenda(id: Long): Outcome<ByteArray?> = op { fotos[id] }

    private val fotosAcesso = mutableMapOf<Long, ByteArray>()

    override suspend fun fotoAcesso(acessoId: Long, foto: ByteArray): Outcome<Visita> = op {
        fotosAcesso[acessoId] = foto
        troca({ it.acessoId == acessoId }) { it.copy(temFoto = true) }
    }

    override suspend fun verFotoAcesso(acessoId: Long): Outcome<ByteArray?> = op { fotosAcesso[acessoId] }

    companion object {
        val RESPONSAVEIS = linkedMapOf(
            "A-102" to "Roberto Lima", "A-205" to "Felipe Araújo", "A-302" to "Vinícius Ramos", "A-403" to "Tatiane Gomes", "A-501" to "Marcos Teixeira", "A-604" to "Helena Castro",
            "B-102" to "Patrícia Nunes", "B-204" to "Ana Souza", "B-301" to "Rafael Rocha", "B-402" to "Daniela Pinto", "B-503" to "Caio Monteiro",
            "C-106" to "Juliana Dias", "C-202" to "Otávio Freitas", "C-303" to "Larissa Alves", "C-305" to "Bruno Campos", "C-404" to "Sabrina Cardoso", "C-501" to "Thiago Barros",
            "D-104" to "Carla Mendonça", "D-201" to "Rafael Torres", "D-305" to "Mariana Lopes", "D-402" to "Eduarda Silva", "D-601" to "Nicolas Moura"
        )

        fun norm(s: String?): String = Normalizer.normalize(s.orEmpty(), Normalizer.Form.NFD).replace(Regex("\\p{M}"), "").lowercase().filter { it.isLetterOrDigit() || it == ' ' }.trim()
    }
}

/** Só no demo: mostra ao porteiro o código que o morador teria no app (para dar para testar a entrega). */
interface DemoCodigo {
    fun codigo(id: Long): String?
}
