package br.com.condovitta.app.data.remote

import br.com.condovitta.app.domain.ChaveEmprestada
import br.com.condovitta.app.domain.ConsultaPlaca
import br.com.condovitta.app.domain.Domestico
import br.com.condovitta.app.domain.GasMes
import br.com.condovitta.app.domain.LeituraGas
import br.com.condovitta.app.domain.Livro
import br.com.condovitta.app.domain.MinhaUnidade
import br.com.condovitta.app.domain.MoradorUnidade
import br.com.condovitta.app.domain.MovimentoGaragem
import br.com.condovitta.app.domain.Pet
import br.com.condovitta.app.domain.RegistroLivro
import br.com.condovitta.app.domain.SentidoGaragem
import br.com.condovitta.app.domain.TipoRegistro
import br.com.condovitta.app.domain.Veiculo
import br.com.condovitta.app.domain.VeiculosPortaria
import kotlinx.serialization.Serializable
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query
import java.math.BigDecimal

/* ---------------- livro ---------------- */

@Serializable
data class RegistroDto(
    val id: Long, val quando: String? = null, val tipo: String? = null, val texto: String = "", val unidadeId: Long? = null,
    val unidade: String? = null, val avisouSindico: Boolean = false, val temFoto: Boolean = false, val autor: String? = null, val travado: Boolean = false
)

@Serializable
data class ChaveDto(val id: Long, val chave: String = "", val com: String? = null, val desde: String? = null, val prevista: String? = null, val emprestadaPor: String? = null)

@Serializable
data class LivroDto(val registros: List<RegistroDto> = emptyList(), val chaves: List<ChaveDto> = emptyList())

@Serializable
data class RegistroFormDto(val tipo: String, val texto: String, val unidadeId: Long? = null, val avisarSindico: Boolean = false)

fun RegistroDto.toDomain() = RegistroLivro(id, dateTime(quando), TipoRegistro.of(tipo), texto, unidade, avisouSindico, autor, travado)
fun LivroDto.toDomain() = Livro(registros.map { it.toDomain() }, chaves.map { ChaveEmprestada(it.id, it.chave, it.com, dateTime(it.desde), it.prevista) })

/* ---------------- veículos ---------------- */

@Serializable
data class MovimentoDto(
    val id: Long, val quando: String? = null, val placa: String = "", val sentido: String? = null, val portao: String? = null,
    val meio: String? = null, val motivo: String? = null, val deQuem: String? = null, val porteiro: String? = null
)

@Serializable
data class VeiculosPortariaDto(val moradores: List<VeiculoDto> = emptyList(), val visitantesHoje: List<VisitaDto> = emptyList(), val movimento: List<MovimentoDto> = emptyList())

@Serializable
data class ConsultaPlacaDto(val placa: String = "", val moradores: List<VeiculoDto> = emptyList(), val visitantes: List<VisitaDto> = emptyList())

@Serializable
data class MovimentoFormDto(val placa: String, val sentido: String, val portao: String? = null, val motivo: String? = null)

fun VeiculoDto.toVeiculo() = Veiculo(id, placa, modelo, cor, unidade, morador, vaga, acesso, bloqueado, obs)
fun MovimentoDto.toDomain() = MovimentoGaragem(id, dateTime(quando), placa, if (sentido == "SAIDA") SentidoGaragem.SAIDA else SentidoGaragem.ENTRADA, portao, deQuem, porteiro)

/* ---------------- gás ---------------- */

@Serializable
data class LinhaGasDto(
    val unidadeId: Long, val unidade: String = "", val bloco: String? = null, val andar: Int? = null, val morador: String? = null, val leituraId: Long? = null,
    @Serializable(with = BigDecimalSerializer::class) val anterior: BigDecimal? = null,
    @Serializable(with = BigDecimalSerializer::class) val atual: BigDecimal? = null,
    @Serializable(with = BigDecimalSerializer::class) val consumo: BigDecimal? = null,
    @Serializable(with = BigDecimalSerializer::class) val media: BigDecimal? = null,
    val alerta: String? = null, val estimada: Boolean = false, val temFoto: Boolean = false, val lidaEm: String? = null, val lidoPor: String? = null
)

@Serializable
data class GasPortariaDto(val mes: String = "", val fechado: Boolean = false, val total: Int = 0, val lidas: Int = 0, val alertas: Int = 0, val unidades: List<LinhaGasDto> = emptyList())

@Serializable
data class LeituraFormDto(@Serializable(with = BigDecimalSerializer::class) val leitura: BigDecimal)

fun LinhaGasDto.toDomain() = LeituraGas(unidadeId, unidade, bloco, morador, anterior, atual, consumo, alerta, dateTime(lidaEm))
fun GasPortariaDto.toDomain() = GasMes(mes, fechado, total, lidas, alertas, unidades.map { it.toDomain() })

/* ---------------- minha unidade ---------------- */

@Serializable
data class UnidadeResumoMoradorDto(val id: Long? = null, val label: String = "", val vagas: Int? = null)

@Serializable
data class MoradorItemDto(
    val id: Long, val nome: String = "", val vinculo: String? = null, val vinculoRotulo: String? = null, val email: String? = null,
    val telefone: String? = null, val titular: Boolean = false, val situacao: String? = null
)

@Serializable
data class PetDto(val id: Long, val nome: String = "", val especie: String? = null, val raca: String? = null, val porte: String? = null, val vacina: String? = null)

@Serializable
data class DomesticoDto(val id: Long, val nome: String = "", val documento: String? = null, val funcao: String? = null, val diasHorarios: String? = null)

@Serializable
data class UnidadeDetalheDto(
    val resumo: UnidadeResumoMoradorDto = UnidadeResumoMoradorDto(), val vagasDescricao: String? = null,
    val moradores: List<MoradorItemDto> = emptyList(), val pets: List<PetDto> = emptyList(), val domesticos: List<DomesticoDto> = emptyList()
)

@Serializable
data class MinhaUnidadeDto(val condominio: String? = null, val unidade: UnidadeDetalheDto = UnidadeDetalheDto(), val possoGerenciar: Boolean = false)

@Serializable
data class PetPedidoDto(val nome: String, val especie: String? = null, val raca: String? = null, val porte: String? = null)

@Serializable
data class DomesticoPedidoDto(val nome: String, val documento: String? = null, val funcao: String? = null, val diasHorarios: String? = null)

fun PetDto.toDomain() = Pet(id, nome, especie, raca, porte, date(vacina))
fun DomesticoDto.toDomain() = Domestico(id, nome, documento, funcao, diasHorarios)
fun MinhaUnidadeDto.toDomain() = MinhaUnidade(
    condominio, unidade.resumo.label, unidade.resumo.vagas, unidade.vagasDescricao,
    unidade.moradores.map { MoradorUnidade(it.id, it.nome, it.vinculoRotulo ?: it.vinculo, it.email, it.telefone, it.titular, it.situacao) },
    unidade.pets.map { it.toDomain() }, unidade.domesticos.map { it.toDomain() }, possoGerenciar
)

/** Livro da portaria, veículos, gás e minha unidade. */
interface RotinaApi {
    @GET("api/v1/portaria/livro")
    suspend fun livro(): LivroDto

    @POST("api/v1/portaria/livro")
    suspend fun registrar(@Body body: RegistroFormDto): RegistroDto

    @DELETE("api/v1/portaria/livro/{id}")
    suspend fun excluirRegistro(@Path("id") id: Long): Response<Unit>

    @POST("api/v1/portaria/chaves/{id}/devolver")
    suspend fun devolverChave(@Path("id") id: Long): ChaveDto

    @GET("api/v1/portaria/veiculos")
    suspend fun veiculos(): VeiculosPortariaDto

    @GET("api/v1/portaria/veiculos/placa/{placa}")
    suspend fun placa(@Path("placa") placa: String): ConsultaPlacaDto

    @POST("api/v1/portaria/veiculos/movimentos")
    suspend fun movimento(@Body body: MovimentoFormDto): MovimentoDto

    @GET("api/v1/portaria/gas")
    suspend fun gas(@Query("mes") mes: String?): GasPortariaDto

    @PUT("api/v1/portaria/gas/{mes}/leituras/{unidadeId}")
    suspend fun lancarGas(@Path("mes") mes: String, @Path("unidadeId") unidadeId: Long, @Body body: LeituraFormDto): LinhaGasDto

    @GET("api/v1/minha-unidade")
    suspend fun minhaUnidade(): MinhaUnidadeDto

    @POST("api/v1/minha-unidade/pets")
    suspend fun adicionarPet(@Body body: PetPedidoDto): PetDto

    @DELETE("api/v1/minha-unidade/pets/{id}")
    suspend fun removerPet(@Path("id") id: Long): Response<Unit>

    @POST("api/v1/minha-unidade/domesticos")
    suspend fun adicionarDomestico(@Body body: DomesticoPedidoDto): DomesticoDto

    @DELETE("api/v1/minha-unidade/domesticos/{id}")
    suspend fun removerDomestico(@Path("id") id: Long): Response<Unit>
}

