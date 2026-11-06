package br.com.condovitta.app.data.remote

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
import br.com.condovitta.app.domain.ItemPauta
import br.com.condovitta.app.domain.MeusDocumentos
import br.com.condovitta.app.domain.MinhasAssembleias
import br.com.condovitta.app.domain.OpcaoVoto
import br.com.condovitta.app.domain.Placar
import br.com.condovitta.app.domain.Procuracao
import br.com.condovitta.app.domain.Recibo
import br.com.condovitta.app.domain.Representante
import br.com.condovitta.app.domain.ResultadoAssinatura
import kotlinx.serialization.Serializable
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path

/* ---------------- documentos ---------------- */

@Serializable
data class DocPendenteDto(
    val id: Long, val titulo: String = "", val tipo: String? = null, val solicitante: String? = null, val enviado: String? = null,
    val prazo: String? = null, val paginas: Int? = null, val opcional: Boolean = false, val status: String? = null
)

@Serializable
data class DocAssinadoDto(
    val id: Long, val titulo: String = "", val assinado: String? = null, val metodo: String? = null, val codigo: String? = null,
    val origem: String? = null, val finalDisponivel: Boolean = false
)

@Serializable
data class DocOficialDto(val id: Long, val titulo: String = "", val sub: String? = null, val icone: String? = null)

@Serializable
data class MeusDocumentosDto(val pendentes: List<DocPendenteDto> = emptyList(), val assinados: List<DocAssinadoDto> = emptyList(), val oficiais: List<DocOficialDto> = emptyList())

@Serializable
data class ParaAssinarDto(
    val id: Long, val titulo: String = "", val tipo: String? = null, val solicitante: String? = null, val enviado: String? = null,
    val prazo: String? = null, val paginas: Int? = null, val opcional: Boolean = false, val mensagem: String? = null, val status: String? = null,
    val meuStatus: String? = null, val formas: String? = null, val exigeCodigo: Boolean = false, val suaVez: Boolean = true, val nome: String? = null,
    val emailMascarado: String? = null, val condominio: String? = null, val codigo: String? = null, val finalDisponivel: Boolean = false
)

@Serializable
data class CodigoEnviadoDto(val email: String? = null, val validadeMinutos: Int = 10)

@Serializable
data class ResultadoAssinaturaDto(
    val codigo: String? = null, val metodo: String? = null, val assinadoEm: String? = null, val ip: String? = null, val cidade: String? = null,
    val dispositivo: String? = null, val confirmacao: String? = null, val hash: String? = null, val concluido: Boolean = false
)

@Serializable
data class MotivoDto(val motivo: String)

@Serializable
data class EventoEvidenciaDto(
    val tipo: String = "", val titulo: String? = null, val ator: String? = null, val detalhe: String? = null, val ip: String? = null,
    val cidade: String? = null, val dispositivo: String? = null, val quando: String? = null
)

@Serializable
data class EvidenciasDto(
    val codigo: String? = null, val titulo: String? = null, val hashOriginal: String? = null, val hashFinal: String? = null,
    val concluido: Boolean = false, val eventos: List<EventoEvidenciaDto> = emptyList()
)

fun MeusDocumentosDto.toDomain() = MeusDocumentos(
    pendentes.map { DocPendente(it.id, it.titulo, it.tipo, it.solicitante, dateTime(it.enviado), date(it.prazo), it.paginas, it.opcional, it.status == "visualizado") },
    assinados.map { DocAssinado(it.id, it.titulo, dateTime(it.assinado), it.metodo, it.codigo, it.origem, it.finalDisponivel) },
    oficiais.map { DocOficial(it.id, it.titulo, it.sub, it.icone == "pi-megaphone") }
)

fun ParaAssinarDto.toDomain() = DocParaAssinar(
    id, titulo, tipo, solicitante, dateTime(enviado), date(prazo), paginas, opcional, mensagem, status ?: "enviado", meuStatus ?: "enviado",
    FormaAssinatura.of(formas), exigeCodigo, suaVez, nome, emailMascarado, condominio, codigo, finalDisponivel
)

fun ResultadoAssinaturaDto.toDomain() = ResultadoAssinatura(codigo, metodo, dateTime(assinadoEm), confirmacao, hash, concluido)
fun EvidenciasDto.toDomain() = Evidencias(codigo, titulo, hashOriginal, hashFinal, concluido, eventos.map { EventoEvidencia(it.tipo, it.titulo, it.ator, it.detalhe, dateTime(it.quando)) })

/* ---------------- assembleias ---------------- */

@Serializable
data class PlacarDto(val aFavor: Int = 0, val contra: Int = 0, val abstencao: Int = 0)

@Serializable
data class ItemPautaDto(
    val id: Long, val ordem: Int? = null, val titulo: String = "", val descricao: String? = null, val quorum: String? = null,
    val quorumLabel: String? = null, val status: String? = null, val votacaoAbertaEm: String? = null, val votacaoEncerraEm: String? = null,
    val resultado: String? = null, val placar: PlacarDto? = null, val precisa: Int? = null, val precisaHint: String? = null
)

@Serializable
data class AoVivoDto(
    val id: Long, val titulo: String = "", val tipo: String? = null, val chamada: String? = null, val abertaEm: String? = null,
    val presidente: String? = null, val naSala: Int = 0, val emVotacao: ItemPautaDto? = null, val pauta: List<ItemPautaDto> = emptyList()
)

@Serializable
data class ProcuracaoDto(
    val id: Long, val outorgante: String? = null, val outorganteUnidade: String? = null, val outorgado: String? = null,
    val outorgadoUnidade: String? = null, val outorgadoEmail: String? = null, val poderes: String? = null, val poderesLabel: String? = null,
    val status: String? = null, val enviadaEm: String? = null, val solicitacaoId: Long? = null
)

@Serializable
data class ProximaDto(
    val id: Long, val titulo: String = "", val tipo: String? = null, val data: String? = null, val hora: String? = null, val segunda: String? = null,
    val modalidade: String? = null, val local: String? = null, val editalEm: String? = null, val pauta: List<String> = emptyList(),
    val aceitaProcuracao: Boolean = false, val podeEnviarProcuracao: Boolean = false, val minhaProcuracao: ProcuracaoDto? = null
)

@Serializable
data class EncerradaDto(
    val id: Long, val titulo: String = "", val tipo: String? = null, val data: String? = null, val presenca: String? = null,
    val itens: List<ItemPautaDto> = emptyList(), val ataDisponivel: Boolean = false, val participacao: String? = null
)

@Serializable
data class AssembleiasMoradorDto(val aoVivo: AoVivoDto? = null, val proximas: List<ProximaDto> = emptyList(), val encerradas: List<EncerradaDto> = emptyList())

@Serializable
data class RepresentanteDto(val usuarioId: Long, val nome: String = "", val unidade: String? = null)

@Serializable
data class ProcuracaoFormDto(val representanteUsuarioId: Long, val poderes: String, val assinatura: String)

/* ---------------- plenário ---------------- */

@Serializable
data class PlenarioAtualDto(val id: Long? = null, val status: String? = null)

@Serializable
data class QuorumSalaDto(val presentes: Int = 0, val naSala: Int = 0, val porProcuracao: Int = 0, val unidadesTotal: Int = 0, val online: Int = 0)

@Serializable
data class FalandoDto(val nome: String? = null, val unidade: String? = null, val papel: String? = null, val desde: String? = null)

@Serializable
data class ChatDto(val id: Long, val nome: String? = null, val unidade: String? = null, val hora: String? = null, val texto: String = "", val mesa: Boolean = false)

@Serializable
data class SalaDto(
    val id: Long, val titulo: String = "", val tipo: String? = null, val status: String? = null, val chamada: String? = null,
    val presidente: String? = null, val secretario: String? = null, val modalidade: String? = null, val abertaEm: String? = null,
    val encerradaEm: String? = null, val pauta: List<ItemPautaDto> = emptyList(), val itemAtualId: Long? = null, val votados: Int = 0,
    val totalVotacao: Int = 0, val quorum: QuorumSalaDto? = null, val falando: FalandoDto? = null, val chat: List<ChatDto> = emptyList()
)

@Serializable
data class ReciboDto(val opcao: String? = null, val protocolo: String? = null, val hash: String? = null, val hora: String? = null, val unidades: Int = 1)

@Serializable
data class VoceDto(
    val usuarioId: Long? = null, val nome: String? = null, val unidade: String? = null, val presente: Boolean = false, val presidente: Boolean = false,
    val podeVotar: Boolean = false, val filaPosicao: Int? = null, val representadas: List<String> = emptyList(), val votos: Map<String, ReciboDto> = emptyMap()
)

@Serializable
data class EstadoDto(val sala: SalaDto, val voce: VoceDto = VoceDto(), val agora: String? = null)

@Serializable
data class VotoDto(val opcao: String)

@Serializable
data class PalavraDto(val naFila: Boolean = false, val posicao: Int? = null)

@Serializable
data class TextoDto(val texto: String)

fun ItemPautaDto.toDomain() = ItemPauta(
    id, ordem, titulo, descricao, quorumLabel, status ?: "pendente", resultado, placar?.let { Placar(it.aFavor, it.contra, it.abstencao) },
    dateTime(votacaoEncerraEm), precisaHint
)

fun ProcuracaoDto.toDomain() = Procuracao(id, outorgado, outorgadoUnidade, poderesLabel, status ?: "valida", dateTime(enviadaEm))

fun AssembleiasMoradorDto.toDomain() = MinhasAssembleias(
    aoVivo?.let { AssembleiaAoVivo(it.id, it.titulo, it.tipo, it.chamada, dateTime(it.abertaEm), it.presidente, it.naSala, it.emVotacao?.toDomain(), it.pauta.map { p -> p.toDomain() }) },
    proximas.map {
        AssembleiaProxima(it.id, it.titulo, it.tipo, date(it.data), time(it.hora), time(it.segunda), it.modalidade, it.local, dateTime(it.editalEm), it.pauta,
            it.aceitaProcuracao, it.podeEnviarProcuracao, it.minhaProcuracao?.toDomain())
    },
    encerradas.map { AssembleiaEncerrada(it.id, it.titulo, it.tipo, date(it.data), it.presenca, it.itens.map { i -> i.toDomain() }, it.ataDisponivel, it.participacao) }
)

fun ReciboDto.toDomain() = Recibo(OpcaoVoto.of(opcao), protocolo, dateTime(hora), unidades)
fun ChatDto.toDomain() = ChatPlenario(id, nome, unidade, dateTime(hora), texto, mesa)

fun EstadoDto.toDomain() = EstadoPlenario(
    sala.id, sala.titulo, sala.tipo, sala.status ?: "agendada", sala.chamada, sala.presidente, sala.modalidade, sala.pauta.map { it.toDomain() },
    sala.itemAtualId, sala.quorum?.presentes ?: 0, sala.quorum?.unidadesTotal ?: 0, sala.quorum?.naSala ?: 0,
    sala.falando?.nome?.let { n -> listOfNotNull(n, sala.falando.unidade).joinToString(" · ") }, sala.chat.map { it.toDomain() },
    voce.unidade, voce.presente, voce.podeVotar, voce.filaPosicao, voce.representadas,
    voce.votos.mapNotNull { (k, v) -> k.toLongOrNull()?.let { it to v.toDomain() } }.toMap()
)

/** Documentos para assinar, assembleias e o plenário virtual. */
interface GovernancaApi {
    @GET("api/v1/documentos")
    suspend fun documentos(): MeusDocumentosDto

    @GET("api/v1/documentos/{id}")
    suspend fun documento(@Path("id") id: Long): ParaAssinarDto

    @GET("api/v1/documentos/{id}/pdf")
    suspend fun documentoPdf(@Path("id") id: Long): ResponseBody

    @POST("api/v1/documentos/{id}/codigo")
    suspend fun enviarCodigo(@Path("id") id: Long): CodigoEnviadoDto

    @Multipart
    @POST("api/v1/documentos/{id}/assinar")
    suspend fun assinar(@Path("id") id: Long, @Part("metodo") metodo: RequestBody, @Part("imagem") imagem: RequestBody, @Part("codigo") codigo: RequestBody?): ResultadoAssinaturaDto

    @POST("api/v1/documentos/{id}/recusar")
    suspend fun recusar(@Path("id") id: Long, @Body body: MotivoDto): Response<Unit>

    @GET("api/v1/documentos/{id}/evidencias")
    suspend fun evidencias(@Path("id") id: Long): EvidenciasDto

    @GET("api/v1/documentos/oficiais/{id}/pdf")
    suspend fun oficialPdf(@Path("id") id: Long): ResponseBody

    @GET("api/v1/assembleias")
    suspend fun assembleias(): AssembleiasMoradorDto

    @GET("api/v1/assembleias/{id}/edital")
    suspend fun edital(@Path("id") id: Long): ResponseBody

    @GET("api/v1/assembleias/{id}/ata/pdf")
    suspend fun ata(@Path("id") id: Long): ResponseBody

    @GET("api/v1/assembleias/{id}/representantes")
    suspend fun representantes(@Path("id") id: Long): List<RepresentanteDto>

    @POST("api/v1/assembleias/{id}/procuracoes")
    suspend fun procuracao(@Path("id") id: Long, @Body body: ProcuracaoFormDto): ProcuracaoDto

    @DELETE("api/v1/assembleias/{id}/procuracoes/{procuracaoId}")
    suspend fun cancelarProcuracao(@Path("id") id: Long, @Path("procuracaoId") procuracaoId: Long): Response<Unit>

    @GET("api/v1/plenario/atual")
    suspend fun plenarioAtual(): PlenarioAtualDto

    @GET("api/v1/plenario/{id}/estado")
    suspend fun estado(@Path("id") id: Long): EstadoDto

    @POST("api/v1/plenario/{id}/presenca")
    suspend fun presenca(@Path("id") id: Long): EstadoDto

    @POST("api/v1/plenario/{id}/itens/{itemId}/voto")
    suspend fun votar(@Path("id") id: Long, @Path("itemId") itemId: Long, @Body body: VotoDto): ReciboDto

    @POST("api/v1/plenario/{id}/palavra")
    suspend fun palavra(@Path("id") id: Long): PalavraDto

    @POST("api/v1/plenario/{id}/chat")
    suspend fun chat(@Path("id") id: Long, @Body body: TextoDto): ChatDto

    @GET("api/v1/plenario/{id}/itens/{itemId}/comprovante")
    suspend fun comprovante(@Path("id") id: Long, @Path("itemId") itemId: Long): ResponseBody
}
