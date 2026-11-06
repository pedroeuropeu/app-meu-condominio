package br.com.condovitta.app.domain

import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

/* Chamados e ocorrências do morador — ChamadoDTO e OcorrenciaDTO do gestao-condominio. */

/** Arquivo anexado (foto, PDF...) a um chamado, mensagem ou ocorrência. */
data class Anexo(val id: Long, val nome: String, val tamanho: Long, val tipo: String?) {
    val imagem: Boolean get() = tipo?.startsWith("image/") == true
}

/** Arquivo que o app vai enviar (foto da galeria já comprimida). */
class ArquivoEnvio(val nome: String, val mime: String, val bytes: ByteArray)

/* ---------------- chamados ---------------- */

enum class CategoriaChamado(val label: String, val doMorador: Boolean = false) {
    MANUTENCAO("Manutenção", true), LIMPEZA("Limpeza", true), SEGURANCA("Segurança", true), ADMINISTRATIVO("Administrativo", true),
    FINANCEIRO("Financeiro", true), HIDRAULICA("Hidráulica"), ELETRICA("Elétrica"), ELEVADOR("Elevador"), PORTAO_ACESSO("Portão e acesso"),
    INTERFONE("Interfone"), AREAS_COMUNS("Áreas comuns"), JARDINAGEM("Jardinagem");

    companion object {
        fun of(v: String?) = entries.firstOrNull { it.name == v } ?: MANUTENCAO
        val doMorador = entries.filter { it.doMorador }
    }
}

enum class PrioridadeChamado(val label: String) {
    BAIXA("Baixa"), MEDIA("Média"), ALTA("Alta"), URGENTE("Urgente");

    companion object {
        fun of(v: String?) = entries.firstOrNull { it.name == v } ?: MEDIA
    }
}

enum class StatusChamado(val label: String, val aberto: Boolean) {
    ABERTO("Aberto", true), EM_ANDAMENTO("Em andamento", true), AGUARDANDO("Aguardando você", true),
    RESOLVIDO("Resolvido", false), CONCLUIDO("Concluído", false), CANCELADO("Cancelado", false);

    companion object {
        fun of(v: String?) = entries.firstOrNull { it.name == v } ?: ABERTO
    }
}

data class Chamado(
    val id: Long,
    val titulo: String,
    val categoria: CategoriaChamado,
    val prioridade: PrioridadeChamado,
    val status: StatusChamado,
    val local: String?,
    val unidade: String?,
    val responsavel: String?,
    val abertoEm: LocalDateTime?,
    val prazoEm: LocalDateTime?,
    val resolvidoEm: LocalDateTime?,
    val ultimaEm: LocalDateTime?,
    val ultimaTexto: String?,
    val ultimaDaAdministracao: Boolean,
    val avaliacao: Int?,
    val anexos: Int
)

data class ResumoChamados(val abertos: Long, val aguardando: Long, val resolvidosNoAno: Long, val primeiraRespostaMinutos: Long?)

data class MeusChamados(val itens: List<Chamado>, val resumo: ResumoChamados)

data class MensagemChamado(
    val id: Long,
    val autor: String?,
    val daAdministracao: Boolean,
    val minha: Boolean,
    val sistema: Boolean,
    val texto: String?,
    val em: LocalDateTime?,
    val anexos: List<Anexo>
)

data class ChamadoDetalhe(
    val chamado: Chamado,
    val descricao: String?,
    val aguardandoMotivo: String?,
    val autorizaEntrada: Boolean,
    val avaliacaoComentario: String?,
    val mensagens: List<MensagemChamado>,
    val anexos: List<Anexo>
) {
    val podeResponder: Boolean get() = chamado.status.aberto
    val podeAvaliar: Boolean get() = chamado.status == StatusChamado.RESOLVIDO && chamado.avaliacao == null
    val podeReabrir: Boolean get() = chamado.status == StatusChamado.RESOLVIDO || chamado.status == StatusChamado.CONCLUIDO
    val podeCancelar: Boolean get() = chamado.status.aberto
}

data class NovoChamado(
    val titulo: String,
    val categoria: CategoriaChamado,
    val prioridade: PrioridadeChamado,
    val local: String?,
    val descricao: String?,
    val autorizaEntrada: Boolean
)

/* ---------------- ocorrências ---------------- */

enum class MotivoOcorrencia(val label: String) {
    BARULHO_EXCESSIVO("Barulho excessivo"), DESCUMPRIMENTO_REGRAS("Descumprimento de regras"), USO_INDEVIDO_AREA_COMUM("Uso indevido de área comum"),
    DANOS_PATRIMONIAIS("Danos ao patrimônio"), ESTACIONAMENTO_IRREGULAR("Estacionamento irregular"), VISITANTE_NAO_AUTORIZADO("Visitante não autorizado"),
    FALTA_MANUTENCAO("Falta de manutenção"), INFRACAO_CONTRATUAL("Infração contratual"), DESCUMPRIMENTO_HORARIO("Descumprimento de horário"), OUTROS("Outros");

    companion object {
        fun of(v: String?) = entries.firstOrNull { it.name == v } ?: OUTROS
    }
}

enum class StatusOcorrencia(val label: String) {
    EM_ANALISE("Em análise"), ADVERTENCIA("Advertência"), MULTA("Multa"), ARQUIVADA("Arquivada");

    companion object {
        fun of(v: String?) = entries.firstOrNull { it.name == v } ?: EM_ANALISE
    }
}

/** Ocorrência que eu registrei. */
data class OcorrenciaMinha(
    val id: Long,
    val protocolo: String?,
    val motivo: MotivoOcorrencia,
    val unidade: String?,
    val local: String?,
    val status: StatusOcorrencia,
    val descricao: String,
    val testemunhas: String?,
    val ocorridaEm: LocalDateTime?,
    val registradaEm: LocalDateTime?,
    val prazoDefesa: LocalDate?,
    val defesaRecebida: Boolean,
    val decisaoTexto: String?,
    val decididaEm: LocalDateTime?,
    val provas: List<Anexo>
)

/** Ocorrência contra a minha unidade (não mostra quem registrou). */
data class OcorrenciaContra(
    val id: Long,
    val protocolo: String?,
    val motivo: MotivoOcorrencia,
    val local: String?,
    val status: StatusOcorrencia,
    val descricao: String,
    val ocorridaEm: LocalDateTime?,
    val prazoDefesa: LocalDate?,
    val defesa: String?,
    val defesaAutor: String?,
    val defesaEm: LocalDateTime?,
    val decisaoTexto: String?,
    val decididaEm: LocalDateTime?,
    val valorMulta: BigDecimal?,
    val provasDefesa: List<Anexo>
) {
    fun podeDefender(hoje: LocalDate): Boolean = status == StatusOcorrencia.EM_ANALISE && defesa == null && (prazoDefesa == null || !hoje.isAfter(prazoDefesa))
}

data class PainelOcorrencias(val minhas: List<OcorrenciaMinha>, val contra: List<OcorrenciaContra>)

data class UnidadeOpcao(val id: Long, val rotulo: String, val bloco: String?)

data class NovaOcorrencia(
    /** problema do condomínio, sem unidade apontada */
    val geral: Boolean,
    val unidade: String?,
    val motivo: MotivoOcorrencia,
    val ocorridaEm: LocalDateTime,
    val local: String?,
    val descricao: String,
    val testemunhas: String?
)

interface AtendimentoRepository {
    suspend fun chamados(): Outcome<MeusChamados>
    suspend fun chamado(id: Long): Outcome<ChamadoDetalhe>
    suspend fun abrirChamado(form: NovoChamado, arquivos: List<ArquivoEnvio>): Outcome<ChamadoDetalhe>
    suspend fun responder(id: Long, texto: String?, arquivos: List<ArquivoEnvio>): Outcome<ChamadoDetalhe>
    suspend fun avaliar(id: Long, nota: Int, comentario: String?): Outcome<Unit>
    suspend fun reabrir(id: Long): Outcome<ChamadoDetalhe>
    suspend fun cancelarChamado(id: Long): Outcome<Unit>
    suspend fun anexoChamado(id: Long, anexoId: Long): Outcome<ByteArray>

    suspend fun ocorrencias(): Outcome<PainelOcorrencias>
    suspend fun unidades(): Outcome<List<UnidadeOpcao>>
    suspend fun registrarOcorrencia(form: NovaOcorrencia, provas: List<ArquivoEnvio>): Outcome<PainelOcorrencias>
    suspend fun defesa(id: Long, texto: String, provas: List<ArquivoEnvio>): Outcome<PainelOcorrencias>
    suspend fun anexoOcorrencia(id: Long, anexoId: Long): Outcome<ByteArray>
}
