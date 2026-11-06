package br.com.condovitta.app.data.demo

import br.com.condovitta.app.core.common.TimeProvider
import br.com.condovitta.app.domain.AssembleiaAoVivo
import br.com.condovitta.app.domain.AssembleiaEncerrada
import br.com.condovitta.app.domain.AssembleiaProxima
import br.com.condovitta.app.domain.ChatPlenario
import br.com.condovitta.app.domain.DocAssinado
import br.com.condovitta.app.domain.DocOficial
import br.com.condovitta.app.domain.DocParaAssinar
import br.com.condovitta.app.domain.DocPendente
import br.com.condovitta.app.domain.EstadoPlenario
import br.com.condovitta.app.domain.EventoEvidencia
import br.com.condovitta.app.domain.Evidencias
import br.com.condovitta.app.domain.FormaAssinatura
import br.com.condovitta.app.domain.GovernancaRepository
import br.com.condovitta.app.domain.ItemPauta
import br.com.condovitta.app.domain.MeusDocumentos
import br.com.condovitta.app.domain.MinhasAssembleias
import br.com.condovitta.app.domain.OpcaoVoto
import br.com.condovitta.app.domain.Outcome
import br.com.condovitta.app.domain.Placar
import br.com.condovitta.app.domain.Poderes
import br.com.condovitta.app.domain.Procuracao
import br.com.condovitta.app.domain.Recibo
import br.com.condovitta.app.domain.Representante
import br.com.condovitta.app.domain.ResultadoAssinatura
import java.time.LocalTime
import javax.inject.Inject
import javax.inject.Singleton

/** Documentos e assembleias do Residencial Vila do Sol 2, vistos pela Ana Souza (B-204). */
@Singleton
class DemoGovernancaRepository @Inject constructor(time: TimeProvider) : DemoBase(time), GovernancaRepository {

    private fun hoje() = time.today()
    private fun dia(d: Long, h: Int, m: Int = 0) = hoje().minusDays(d).atTime(h, m)

    /* ---------------- documentos ---------------- */

    private val docs = mutableListOf(
        DocParaAssinar(501, "Termo do novo regimento das churrasqueiras", "Termo", "Síndica Marina Costa", dia(0, 8), hoje().plusDays(7), 2, false,
            "Pedimos a ciência de todos os moradores sobre as novas regras de uso das churrasqueiras.", "enviado", "enviado", FormaAssinatura.TODAS,
            true, true, "Ana Souza", "an***@email.com", "Residencial Vila do Sol 2", null, false),
        DocParaAssinar(502, "Autorização de obra · troca de piso", "Autorização", "Administração", dia(3, 14), hoje().plusDays(2), 1, true,
            null, "visualizado", "visualizado", FormaAssinatura.TODAS, false, true, "Ana Souza", "an***@email.com", "Residencial Vila do Sol 2", null, false),
        DocParaAssinar(480, "Termo de uso do salão de festas · 12/10", "Termo", "Reserva do salão", dia(30, 10), null, 1, false, null,
            "assinado", "assinado", FormaAssinatura.TODAS, false, false, "Ana Souza", "an***@email.com", "Residencial Vila do Sol 2", "CV-8F3A-21D0", true)
    )
    private val assinadoEm = mutableMapOf(480L to dia(30, 10, 12))
    private val enviouCodigo = mutableSetOf<Long>()

    override suspend fun documentos(): Outcome<MeusDocumentos> = op {
        MeusDocumentos(
            docs.filter { !it.assinei && !it.recusei && !it.cancelado }.map { DocPendente(it.id, it.titulo, it.tipo, it.solicitante, it.enviado, it.prazo, it.paginas, it.opcional, it.meuStatus == "visualizado") },
            docs.filter { it.assinei }.map { DocAssinado(it.id, it.titulo, assinadoEm[it.id], "Assinatura desenhada", it.codigo, if (it.id == 480L) "Reserva" else "Solicitação do síndico", it.finalDisponivel) },
            listOf(
                DocOficial(701, "Ata da AGO de abril", "Assinada em 02/05/2026", false),
                DocOficial(702, "Edital da AGE de novembro", "Publicado em ${hoje().minusDays(12).let { "%02d/%02d/%d".format(it.dayOfMonth, it.monthValue, it.year) }}", true),
                DocOficial(703, "Convenção do condomínio", "Registrada em cartório", false)
            )
        )
    }

    private fun idx(id: Long) = docs.indexOfFirst { it.id == id }.takeIf { it >= 0 } ?: naoAchou()

    override suspend fun documento(id: Long): Outcome<DocParaAssinar> = op {
        val i = idx(id)
        if (docs[i].meuStatus == "enviado") docs[i] = docs[i].copy(meuStatus = "visualizado", status = "visualizado")
        docs[i]
    }

    override suspend fun documentoPdf(id: Long): Outcome<ByteArray> = op { DemoPdf.boleto(docs[idx(id)].titulo) }

    override suspend fun enviarCodigo(id: Long): Outcome<String> = op { enviouCodigo += id; docs[idx(id)].emailMascarado ?: "o seu e-mail" }

    override suspend fun assinar(id: Long, png: ByteArray, codigo: String?): Outcome<ResultadoAssinatura> = op {
        val i = idx(id)
        val d = docs[i]
        if (!d.podeAssinarAqui) falha("Este documento não está aguardando a sua assinatura.")
        if (png.isEmpty()) falha("Desenhe a sua assinatura.")
        if (d.exigeCodigo) {
            if (id !in enviouCodigo) falha("Peça o código por e-mail antes de assinar.")
            if (codigo?.length != 6) falha("Digite o código de 6 dígitos que enviamos por e-mail.")
        }
        val cod = "CV-${id.toString(16).uppercase()}-${(1000..9999).random()}"
        docs[i] = d.copy(status = "assinado", meuStatus = "assinado", codigo = cod, finalDisponivel = true)
        assinadoEm[id] = time.now()
        ResultadoAssinatura(cod, "Assinatura desenhada", time.now(), if (d.exigeCodigo) "Código por e-mail" else "Sessão autenticada", "9f2c…a71b", true)
    }

    override suspend fun recusar(id: Long, motivo: String): Outcome<Unit> = op {
        if (motivo.isBlank()) falha("Conte o motivo da recusa.")
        val i = idx(id)
        docs[i] = docs[i].copy(meuStatus = "recusado", status = "recusado")
    }

    override suspend fun evidencias(id: Long): Outcome<Evidencias> = op {
        val d = docs[idx(id)]
        val eventos = buildList {
            add(EventoEvidencia("CRIADO", "Documento criado", d.solicitante, null, d.enviado))
            add(EventoEvidencia("ENVIADO", "Enviado para assinatura", d.solicitante, "Ana Souza · an***@email.com", d.enviado))
            if (d.meuStatus != "enviado") add(EventoEvidencia("VISUALIZADO", "Visualizado", "Ana Souza", "App Condovitta · Android", d.enviado?.plusHours(1)))
            if (d.assinei) add(EventoEvidencia("ASSINADO", "Assinado", "Ana Souza", "Assinatura desenhada", assinadoEm[id]))
            if (d.assinei) add(EventoEvidencia("CONCLUIDO", "Concluído", null, "Todas as assinaturas recebidas", assinadoEm[id]))
        }
        Evidencias(d.codigo, d.titulo, "3b1e…09cd", if (d.assinei) "9f2c…a71b" else null, d.assinei, eventos)
    }

    override suspend fun oficialPdf(id: Long): Outcome<ByteArray> = op { DemoPdf.boleto("Documento oficial $id") }

    /* ---------------- assembleias ---------------- */

    private var pauta = mutableListOf(
        ItemPauta(1, 1, "Aprovação das contas de outubro", null, "Maioria simples", "votado", "aprovado", Placar(41, 3, 4), null, null),
        ItemPauta(2, 2, "Troca do portão da garagem", "Três orçamentos; o recomendado é da Portões Goiás (R$ 18.400 em 4x).", "Maioria simples", "em_votacao", null,
            Placar(22, 9, 2), time.now().plusMinutes(12), "Precisa de mais votos a favor do que contra"),
        ItemPauta(3, 3, "Assuntos gerais", null, "Informativo", "pendente", null, null, null, null)
    )
    private var presente = false
    private val votos = mutableMapOf<Long, Recibo>()
    private var fila: Int? = null
    private val chat = mutableListOf(
        ChatPlenario(1, "Mesa", null, dia(0, 19, 2), "Boa noite! Assembleia instalada em 1ª chamada.", true),
        ChatPlenario(2, "Roberto Lima", "A-102", dia(0, 19, 10), "O portão novo tem garantia de quantos anos?", false),
        ChatPlenario(3, "Mesa", null, dia(0, 19, 12), "Garantia de 2 anos na automação e 5 na estrutura.", true)
    )
    private var procuracao: Procuracao? = null

    override suspend fun assembleias(): Outcome<MinhasAssembleias> = op {
        MinhasAssembleias(
            AssembleiaAoVivo(900, "AGE de novembro", "AGE", "1ª chamada", dia(0, 19), "Marina Costa (síndica)", 37, pauta.firstOrNull { it.emVotacao }, pauta.toList()),
            listOf(
                AssembleiaProxima(901, "AGO de prestação de contas anual", "AGO", hoje().plusDays(20), LocalTime.of(19, 0), LocalTime.of(19, 30),
                    "Online · Salão de festas", "Salão de festas", dia(2, 10),
                    listOf("Prestação de contas de 2026", "Previsão orçamentária de 2027", "Eleição do síndico e do conselho"),
                    true, procuracao == null, procuracao)
            ),
            listOf(
                AssembleiaEncerrada(880, "AGE de agosto · obras da fachada", "AGE", hoje().minusDays(80), "52 de 96 unidades (54%)",
                    listOf(
                        ItemPauta(11, 1, "Pintura da fachada", null, "Maioria absoluta", "votado", "aprovado", Placar(44, 6, 2), null, null),
                        ItemPauta(12, 2, "Rateio em 10 parcelas", null, "Maioria simples", "votado", "rejeitado", Placar(18, 30, 4), null, null)
                    ), true, "todos")
            )
        )
    }

    override suspend fun edital(id: Long): Outcome<ByteArray> = op { DemoPdf.boleto("Edital de convocação da assembleia $id") }
    override suspend fun ata(id: Long): Outcome<ByteArray> = op { DemoPdf.boleto("Ata da assembleia $id") }

    override suspend fun representantes(id: Long): Outcome<List<Representante>> = op {
        listOf(Representante(11, "Roberto Lima", "A-102"), Representante(12, "Larissa Alves", "B-203"), Representante(13, "Marcos Teixeira", "A-501"))
    }

    override suspend fun enviarProcuracao(id: Long, representanteId: Long, poderes: Poderes, png: ByteArray): Outcome<Procuracao> = op {
        if (procuracao != null) falha("A sua unidade já tem uma procuração para esta assembleia.")
        val r = listOf(11L to ("Roberto Lima" to "A-102"), 12L to ("Larissa Alves" to "B-203"), 13L to ("Marcos Teixeira" to "A-501")).toMap()[representanteId] ?: naoAchou()
        Procuracao(nextId(), r.first, r.second, poderes.label, "valida", time.now()).also { procuracao = it }
    }

    override suspend fun cancelarProcuracao(id: Long, procuracaoId: Long): Outcome<Unit> = op { procuracao = null }

    /* ---------------- plenário ---------------- */

    private fun estado() = EstadoPlenario(
        900, "AGE de novembro", "AGE", "ao_vivo", "1ª chamada", "Marina Costa (síndica)", "Híbrida", pauta.toList(), pauta.firstOrNull { it.emVotacao }?.id,
        if (presente) 49 else 48, 96, 37, "Marina Costa · síndica", chat.toList(), "B-204", presente, presente, fila, emptyList(), votos.toMap()
    )

    override suspend fun plenarioAtual(): Outcome<Long?> = op { 900L }
    override suspend fun estado(id: Long): Outcome<EstadoPlenario> = op { estado() }
    override suspend fun presenca(id: Long): Outcome<EstadoPlenario> = op { presente = true; estado() }

    override suspend fun votar(id: Long, itemId: Long, opcao: OpcaoVoto): Outcome<Recibo> = op {
        if (!presente) falha("Confirme a sua presença antes de votar.")
        val i = pauta.indexOfFirst { it.id == itemId }.takeIf { it >= 0 } ?: naoAchou()
        val item = pauta[i]
        if (!item.emVotacao) falha("A votação deste item não está aberta.")
        val p = item.placar ?: Placar(0, 0, 0)
        fun Placar.mais(o: OpcaoVoto?, d: Int) = when (o) {
            OpcaoVoto.A_FAVOR -> copy(aFavor = aFavor + d)
            OpcaoVoto.CONTRA -> copy(contra = contra + d)
            OpcaoVoto.ABSTENCAO -> copy(abstencao = abstencao + d)
            null -> this
        }
        pauta[i] = item.copy(placar = p.mais(votos[itemId]?.opcao, -1).mais(opcao, 1))
        Recibo(opcao, "VT-900-$itemId-B204", time.now(), 1).also { votos[itemId] = it }
    }

    override suspend fun palavra(id: Long): Outcome<Int?> = op { fila = if (fila == null) 3 else null; fila }

    override suspend fun chat(id: Long, texto: String): Outcome<ChatPlenario> = op {
        if (texto.isBlank()) falha("Escreva a mensagem.")
        ChatPlenario(nextId(), "Ana Souza", "B-204", time.now(), texto.trim(), false).also { chat += it }
    }

    override suspend fun comprovante(id: Long, itemId: Long): Outcome<ByteArray> = op { DemoPdf.boleto("Comprovante de voto ${votos[itemId]?.protocolo ?: ""}") }
}
