package br.com.condovitta.app.domain

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/* Documentos para assinar e assembleias/plenário — DocumentoAssinaturaDTO e AssembleiaDTO do gestao-condominio. */

/* ---------------- documentos ---------------- */

data class DocPendente(
    val id: Long,
    val titulo: String,
    val tipo: String?,
    val solicitante: String?,
    val enviado: LocalDateTime?,
    val prazo: LocalDate?,
    val paginas: Int?,
    val opcional: Boolean,
    val visualizado: Boolean
)

data class DocAssinado(
    val id: Long,
    val titulo: String,
    val assinado: LocalDateTime?,
    val metodo: String?,
    val codigo: String?,
    val origem: String?,
    val finalDisponivel: Boolean
)

/** Ata ou edital publicado (qualquer morador baixa). */
data class DocOficial(val id: Long, val titulo: String, val sub: String?, val edital: Boolean)

data class MeusDocumentos(val pendentes: List<DocPendente>, val assinados: List<DocAssinado>, val oficiais: List<DocOficial>)

enum class FormaAssinatura { TODAS, TELA, ARQUIVO;

    companion object {
        fun of(v: String?) = entries.firstOrNull { it.name == v } ?: TODAS
    }
}

data class DocParaAssinar(
    val id: Long,
    val titulo: String,
    val tipo: String?,
    val solicitante: String?,
    val enviado: LocalDateTime?,
    val prazo: LocalDate?,
    val paginas: Int?,
    val opcional: Boolean,
    val mensagem: String?,
    /** enviado, visualizado, pendente, assinado, recusado, cancelado */
    val status: String,
    /** aguardando, enviado, visualizado, assinado, recusado */
    val meuStatus: String,
    val formas: FormaAssinatura,
    val exigeCodigo: Boolean,
    val suaVez: Boolean,
    val nome: String?,
    val emailMascarado: String?,
    val condominio: String?,
    val codigo: String?,
    val finalDisponivel: Boolean
) {
    val assinei: Boolean get() = meuStatus == "assinado"
    val recusei: Boolean get() = meuStatus == "recusado"
    val cancelado: Boolean get() = status == "cancelado" || status == "recusado"
    /** o app assina desenhando na tela; documento que só aceita arquivo vai pela web */
    val podeAssinarAqui: Boolean get() = !assinei && !recusei && !cancelado && suaVez && formas != FormaAssinatura.ARQUIVO
}

data class ResultadoAssinatura(val codigo: String?, val metodo: String?, val assinadoEm: LocalDateTime?, val confirmacao: String?, val hash: String?, val concluido: Boolean)

data class EventoEvidencia(val tipo: String, val titulo: String?, val ator: String?, val detalhe: String?, val quando: LocalDateTime?)

data class Evidencias(val codigo: String?, val titulo: String?, val hashOriginal: String?, val hashFinal: String?, val concluido: Boolean, val eventos: List<EventoEvidencia>)

/* ---------------- assembleias ---------------- */

data class Placar(val aFavor: Int, val contra: Int, val abstencao: Int) {
    val total: Int get() = aFavor + contra + abstencao
}

data class ItemPauta(
    val id: Long,
    val ordem: Int?,
    val titulo: String,
    val descricao: String?,
    val quorumLabel: String?,
    /** pendente, em_votacao, votado */
    val status: String,
    /** aprovado, rejeitado, sessao_permanente */
    val resultado: String?,
    val placar: Placar?,
    val votacaoEncerraEm: LocalDateTime?,
    val precisaHint: String?
) {
    val emVotacao: Boolean get() = status == "em_votacao"
    val votado: Boolean get() = status == "votado"
    val resultadoLabel: String? get() = when (resultado) {
        "aprovado" -> "Aprovado"
        "rejeitado" -> "Rejeitado"
        "sessao_permanente" -> "Sessão permanente"
        else -> null
    }
}

data class AssembleiaAoVivo(
    val id: Long, val titulo: String, val tipo: String?, val chamada: String?, val abertaEm: LocalDateTime?,
    val presidente: String?, val naSala: Int, val emVotacao: ItemPauta?, val pauta: List<ItemPauta>
)

data class Procuracao(
    val id: Long, val outorgado: String?, val outorgadoUnidade: String?, val poderesLabel: String?,
    /** valida, cancelada */
    val status: String, val enviadaEm: LocalDateTime?
) {
    val valida: Boolean get() = status == "valida"
}

data class AssembleiaProxima(
    val id: Long, val titulo: String, val tipo: String?, val data: LocalDate?, val hora: LocalTime?, val segunda: LocalTime?,
    val modalidade: String?, val local: String?, val editalEm: LocalDateTime?, val pauta: List<String>,
    val aceitaProcuracao: Boolean, val podeEnviarProcuracao: Boolean, val minhaProcuracao: Procuracao?
)

data class AssembleiaEncerrada(
    val id: Long, val titulo: String, val tipo: String?, val data: LocalDate?, val presenca: String?, val itens: List<ItemPauta>,
    val ataDisponivel: Boolean,
    /** todos, alguns, nenhum, sem_unidade */
    val participacao: String?
)

data class MinhasAssembleias(val aoVivo: AssembleiaAoVivo?, val proximas: List<AssembleiaProxima>, val encerradas: List<AssembleiaEncerrada>)

data class Representante(val usuarioId: Long, val nome: String, val unidade: String?)

enum class Poderes(val label: String) { TODOS("Votar e assinar a presença"), PRESENCA("Só a presença") }

/* ---------------- plenário (assembleia ao vivo) ---------------- */

enum class OpcaoVoto(val label: String) {
    A_FAVOR("A favor"), CONTRA("Contra"), ABSTENCAO("Abstenção");

    companion object {
        fun of(v: String?) = entries.firstOrNull { it.name == v }
    }
}

data class Recibo(val opcao: OpcaoVoto?, val protocolo: String?, val hora: LocalDateTime?, val unidades: Int)

data class ChatPlenario(val id: Long, val nome: String?, val unidade: String?, val hora: LocalDateTime?, val texto: String, val mesa: Boolean)

data class EstadoPlenario(
    val id: Long,
    val titulo: String,
    val tipo: String?,
    /** rascunho, agendada, ao_vivo, encerrada, cancelada */
    val status: String,
    val chamada: String?,
    val presidente: String?,
    val modalidade: String?,
    val pauta: List<ItemPauta>,
    val itemAtualId: Long?,
    val presentes: Int,
    val unidadesTotal: Int,
    val naSala: Int,
    val falando: String?,
    val chat: List<ChatPlenario>,
    val unidade: String?,
    val presente: Boolean,
    val podeVotar: Boolean,
    val filaPosicao: Int?,
    val representadas: List<String>,
    val votos: Map<Long, Recibo>
) {
    val aoVivo: Boolean get() = status == "ao_vivo"
    val itemAtual: ItemPauta? get() = pauta.firstOrNull { it.emVotacao } ?: pauta.firstOrNull { it.id == itemAtualId }
}

interface GovernancaRepository {
    suspend fun documentos(): Outcome<MeusDocumentos>
    suspend fun documento(id: Long): Outcome<DocParaAssinar>
    suspend fun documentoPdf(id: Long): Outcome<ByteArray>
    /** manda o código por e-mail; devolve o e-mail mascarado */
    suspend fun enviarCodigo(id: Long): Outcome<String>
    suspend fun assinar(id: Long, png: ByteArray, codigo: String?): Outcome<ResultadoAssinatura>
    suspend fun recusar(id: Long, motivo: String): Outcome<Unit>
    suspend fun evidencias(id: Long): Outcome<Evidencias>
    suspend fun oficialPdf(id: Long): Outcome<ByteArray>

    suspend fun assembleias(): Outcome<MinhasAssembleias>
    suspend fun edital(id: Long): Outcome<ByteArray>
    suspend fun ata(id: Long): Outcome<ByteArray>
    suspend fun representantes(id: Long): Outcome<List<Representante>>
    suspend fun enviarProcuracao(id: Long, representanteId: Long, poderes: Poderes, png: ByteArray): Outcome<Procuracao>
    suspend fun cancelarProcuracao(id: Long, procuracaoId: Long): Outcome<Unit>

    suspend fun plenarioAtual(): Outcome<Long?>
    suspend fun estado(id: Long): Outcome<EstadoPlenario>
    suspend fun presenca(id: Long): Outcome<EstadoPlenario>
    suspend fun votar(id: Long, itemId: Long, opcao: OpcaoVoto): Outcome<Recibo>
    /** entra ou sai da fila; devolve a posição (null = saiu) */
    suspend fun palavra(id: Long): Outcome<Int?>
    suspend fun chat(id: Long, texto: String): Outcome<ChatPlenario>
    suspend fun comprovante(id: Long, itemId: Long): Outcome<ByteArray>
}
