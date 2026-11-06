package br.com.condovitta.app.data.remote

import br.com.condovitta.app.domain.Anexo
import br.com.condovitta.app.domain.ArquivoEnvio
import br.com.condovitta.app.domain.CategoriaChamado
import br.com.condovitta.app.domain.Chamado
import br.com.condovitta.app.domain.ChamadoDetalhe
import br.com.condovitta.app.domain.MensagemChamado
import br.com.condovitta.app.domain.MotivoOcorrencia
import br.com.condovitta.app.domain.OcorrenciaContra
import br.com.condovitta.app.domain.OcorrenciaMinha
import br.com.condovitta.app.domain.PainelOcorrencias
import br.com.condovitta.app.domain.PrioridadeChamado
import br.com.condovitta.app.domain.ResumoChamados
import br.com.condovitta.app.domain.StatusChamado
import br.com.condovitta.app.domain.StatusOcorrencia
import br.com.condovitta.app.domain.UnidadeOpcao
import kotlinx.serialization.Serializable
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import java.math.BigDecimal

/* ---------------- multipart ---------------- */

/** Parte JSON ("dados") de um envio multipart. */
internal fun <T> jsonPart(serializer: kotlinx.serialization.KSerializer<T>, value: T): RequestBody =
    ApiJson.encodeToString(serializer, value).toRequestBody("application/json".toMediaType())

internal fun textPart(v: String): RequestBody = v.toRequestBody("text/plain".toMediaType())

internal fun List<ArquivoEnvio>.parts(campo: String): List<MultipartBody.Part> =
    map { MultipartBody.Part.createFormData(campo, it.nome, it.bytes.toRequestBody(it.mime.toMediaType())) }

/* ---------------- chamados ---------------- */

@Serializable
data class AnexoDto(val id: Long, val nome: String = "arquivo", val tamanho: Long = 0, val tipo: String? = null)

@Serializable
data class ChamadoResumoDto(
    val id: Long, val titulo: String = "", val categoria: String? = null, val prioridade: String? = null, val status: String? = null,
    val local: String? = null, val unidadeId: Long? = null, val unidade: String? = null, val solicitante: String? = null, val canal: String? = null,
    val responsavel: String? = null, val abertoEm: String? = null, val prazoEm: String? = null, val primeiraRespostaEm: String? = null,
    val resolvidoEm: String? = null, val pausadoEm: String? = null, val ultimaEm: String? = null, val avaliacao: Int? = null,
    val ultimaAutor: String? = null, val ultimaDaAdministracao: Boolean = false, val ultimaTexto: String? = null, val anexos: Int = 0
)

@Serializable
data class ResumoChamadosDto(val abertos: Long = 0, val aguardando: Long = 0, val resolvidosNoAno: Long = 0, val primeiraRespostaMinutos: Long? = null)

@Serializable
data class MensagemChamadoDto(
    val id: Long, val autor: String? = null, val papel: String? = null, val daAdministracao: Boolean = false, val minha: Boolean = false,
    val interna: Boolean = false, val sistema: Boolean = false, val texto: String? = null, val em: String? = null, val anexos: List<AnexoDto> = emptyList()
)

@Serializable
data class ChamadoDetalheDto(
    val chamado: ChamadoResumoDto, val descricao: String? = null, val aguardandoMotivo: String? = null, val autorizaEntrada: Boolean = false,
    val avaliacaoComentario: String? = null, val moradorUnidade: String? = null, val mensagens: List<MensagemChamadoDto> = emptyList(),
    val anexos: List<AnexoDto> = emptyList()
)

@Serializable
data class NovoChamadoDto(
    val titulo: String, val categoria: String, val prioridade: String, val local: String? = null, val descricao: String? = null, val autorizaEntrada: Boolean = false
)

@Serializable
data class AvaliacaoDto(val nota: Int, val comentario: String? = null)

fun AnexoDto.toDomain() = Anexo(id, nome, tamanho, tipo)

fun ChamadoResumoDto.toDomain() = Chamado(
    id, titulo, CategoriaChamado.of(categoria), PrioridadeChamado.of(prioridade), StatusChamado.of(status), local, unidade, responsavel,
    dateTime(abertoEm), dateTime(prazoEm), dateTime(resolvidoEm), dateTime(ultimaEm), ultimaTexto, ultimaDaAdministracao, avaliacao, anexos
)

fun ResumoChamadosDto.toDomain() = ResumoChamados(abertos, aguardando, resolvidosNoAno, primeiraRespostaMinutos)

fun ChamadoDetalheDto.toDomain() = ChamadoDetalhe(
    chamado.toDomain(), descricao, aguardandoMotivo, autorizaEntrada, avaliacaoComentario,
    mensagens.filterNot { it.interna }.map { MensagemChamado(it.id, it.autor, it.daAdministracao, it.minha, it.sistema, it.texto, dateTime(it.em), it.anexos.map { a -> a.toDomain() }) },
    anexos.map { it.toDomain() }
)

/* ---------------- ocorrências ---------------- */

@Serializable
data class OcorrenciaMinhaDto(
    val id: Long, val protocolo: String? = null, val motivo: String? = null, val unidadeId: Long? = null, val unidade: String? = null,
    val local: String? = null, val status: String? = null, val descricao: String = "", val testemunhas: String? = null, val ocorridaEm: String? = null,
    val registradaEm: String? = null, val abertaEm: String? = null, val prazoDefesa: String? = null, val defesaRecebida: Boolean = false,
    val decisaoTexto: String? = null, val decididaEm: String? = null, val provas: List<AnexoDto> = emptyList()
)

@Serializable
data class OcorrenciaContraDto(
    val id: Long, val protocolo: String? = null, val motivo: String? = null, val local: String? = null, val status: String? = null,
    val descricao: String = "", val ocorridaEm: String? = null, val prazoDefesa: String? = null, val defesa: String? = null,
    val defesaAutor: String? = null, val defesaEm: String? = null, val decisaoTexto: String? = null, val decididaEm: String? = null,
    @Serializable(with = BigDecimalSerializer::class) val valorMulta: BigDecimal? = null, val extrajudicialId: Long? = null,
    val provasDefesa: List<AnexoDto> = emptyList()
)

@Serializable
data class PainelOcorrenciasDto(val minhas: List<OcorrenciaMinhaDto> = emptyList(), val contra: List<OcorrenciaContraDto> = emptyList())

@Serializable
data class UnidadeOpcaoDto(val id: Long, val rotulo: String = "", val bloco: String? = null)

@Serializable
data class NovaOcorrenciaDto(
    val geral: Boolean, val unidade: String? = null, val motivo: String, val ocorridaEm: String, val local: String? = null,
    val descricao: String, val testemunhas: String? = null, val declaracao: Boolean = true
)

fun OcorrenciaMinhaDto.toDomain() = OcorrenciaMinha(
    id, protocolo, MotivoOcorrencia.of(motivo), unidade, local, StatusOcorrencia.of(status), descricao, testemunhas, dateTime(ocorridaEm),
    dateTime(registradaEm ?: abertaEm), date(prazoDefesa), defesaRecebida, decisaoTexto, dateTime(decididaEm), provas.map { it.toDomain() }
)

fun OcorrenciaContraDto.toDomain() = OcorrenciaContra(
    id, protocolo, MotivoOcorrencia.of(motivo), local, StatusOcorrencia.of(status), descricao, dateTime(ocorridaEm), date(prazoDefesa),
    defesa, defesaAutor, dateTime(defesaEm), decisaoTexto, dateTime(decididaEm), valorMulta, provasDefesa.map { it.toDomain() }
)

fun PainelOcorrenciasDto.toDomain() = PainelOcorrencias(minhas.map { it.toDomain() }, contra.map { it.toDomain() })
fun UnidadeOpcaoDto.toDomain() = UnidadeOpcao(id, rotulo, bloco)

/** Chamados e ocorrências do morador. */
interface AtendimentoApi {
    @GET("api/v1/chamados")
    suspend fun chamados(): List<ChamadoResumoDto>

    @GET("api/v1/chamados/resumo")
    suspend fun resumo(): ResumoChamadosDto

    @Multipart
    @POST("api/v1/chamados")
    suspend fun abrir(@Part("dados") dados: RequestBody, @Part arquivos: List<MultipartBody.Part>): ChamadoDetalheDto

    @GET("api/v1/chamados/{id}")
    suspend fun chamado(@Path("id") id: Long): ChamadoDetalheDto

    @Multipart
    @POST("api/v1/chamados/{id}/mensagens")
    suspend fun responder(@Path("id") id: Long, @Part("texto") texto: RequestBody?, @Part arquivos: List<MultipartBody.Part>): ChamadoDetalheDto

    @POST("api/v1/chamados/{id}/avaliacao")
    suspend fun avaliar(@Path("id") id: Long, @Body body: AvaliacaoDto): Response<Unit>

    @POST("api/v1/chamados/{id}/reabrir")
    suspend fun reabrir(@Path("id") id: Long): ChamadoDetalheDto

    @POST("api/v1/chamados/{id}/cancelar")
    suspend fun cancelar(@Path("id") id: Long): Response<Unit>

    @GET("api/v1/chamados/{id}/anexos/{anexoId}")
    suspend fun anexoChamado(@Path("id") id: Long, @Path("anexoId") anexoId: Long): ResponseBody

    @GET("api/v1/ocorrencias")
    suspend fun ocorrencias(): PainelOcorrenciasDto

    @GET("api/v1/ocorrencias/unidades")
    suspend fun unidades(): List<UnidadeOpcaoDto>

    @Multipart
    @POST("api/v1/ocorrencias")
    suspend fun registrar(@Part("dados") dados: RequestBody, @Part provas: List<MultipartBody.Part>): PainelOcorrenciasDto

    @Multipart
    @POST("api/v1/ocorrencias/{id}/defesa")
    suspend fun defesa(@Path("id") id: Long, @Part("texto") texto: RequestBody, @Part provas: List<MultipartBody.Part>): PainelOcorrenciasDto

    @GET("api/v1/ocorrencias/{id}/anexos/{anexoId}")
    suspend fun anexoOcorrencia(@Path("id") id: Long, @Path("anexoId") anexoId: Long): ResponseBody
}
