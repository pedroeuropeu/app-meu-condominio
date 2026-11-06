package br.com.condovitta.app.domain

import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

/* Rotina da portaria (livro, veículos e gás) e a minha unidade do morador — PortariaDTO, VeiculoDTO, GasDTO e
   MoradorDTOs do gestao-condominio. */

enum class TipoRegistro(val label: String) {
    OCORRENCIA("Ocorrência"), RONDA("Ronda"), CHAVE("Chave"), PRESTADOR("Prestador"), INFORMATIVO("Informativo"), PASSAGEM("Passagem de turno");

    companion object {
        fun of(v: String?) = entries.firstOrNull { it.name == v } ?: INFORMATIVO
    }
}

data class RegistroLivro(
    val id: Long,
    val quando: LocalDateTime?,
    val tipo: TipoRegistro,
    val texto: String,
    val unidade: String?,
    val avisouSindico: Boolean,
    val autor: String?,
    /** registro antigo ou de outro turno: não dá mais para apagar */
    val travado: Boolean
)

data class ChaveEmprestada(val id: Long, val chave: String, val com: String?, val desde: LocalDateTime?, val prevista: String?)

data class Livro(val registros: List<RegistroLivro>, val chaves: List<ChaveEmprestada>)

data class NovoRegistro(val tipo: TipoRegistro, val texto: String, val unidadeId: Long? = null, val avisarSindico: Boolean = false)

enum class SentidoGaragem { ENTRADA, SAIDA }

data class Veiculo(
    val id: Long?,
    val placa: String,
    val modelo: String?,
    val cor: String?,
    val unidade: String?,
    val morador: String?,
    val vaga: String?,
    val acesso: String?,
    val bloqueado: Boolean,
    val obs: String?
)

data class MovimentoGaragem(val id: Long, val quando: LocalDateTime?, val placa: String, val sentido: SentidoGaragem, val portao: String?, val deQuem: String?, val porteiro: String?)

data class VeiculosPortaria(val moradores: List<Veiculo>, val movimento: List<MovimentoGaragem>)

data class ConsultaPlaca(val placa: String, val moradores: List<Veiculo>, val visitantes: List<Visita>)

data class LeituraGas(
    val unidadeId: Long,
    val unidade: String,
    val bloco: String?,
    val morador: String?,
    val anterior: BigDecimal?,
    val atual: BigDecimal?,
    val consumo: BigDecimal?,
    /** "acima da média", "leitura menor que a anterior"... */
    val alerta: String?,
    val lidaEm: LocalDateTime?
) {
    val lida: Boolean get() = atual != null
}

data class GasMes(val mes: String, val fechado: Boolean, val total: Int, val lidas: Int, val alertas: Int, val unidades: List<LeituraGas>)

/* ---------------- minha unidade ---------------- */

data class MoradorUnidade(val id: Long, val nome: String, val vinculo: String?, val email: String?, val telefone: String?, val titular: Boolean, val situacao: String?)
data class Pet(val id: Long, val nome: String, val especie: String?, val raca: String?, val porte: String?, val vacina: LocalDate?)
data class Domestico(val id: Long, val nome: String, val documento: String?, val funcao: String?, val diasHorarios: String?)

data class MinhaUnidade(
    val condominio: String?,
    val unidade: String,
    val vagas: Int?,
    val vagasDescricao: String?,
    val moradores: List<MoradorUnidade>,
    val pets: List<Pet>,
    val domesticos: List<Domestico>,
    /** o titular cadastra e remove; os demais só veem */
    val possoGerenciar: Boolean
)

interface RotinaRepository {
    suspend fun livro(): Outcome<Livro>
    suspend fun registrar(form: NovoRegistro): Outcome<RegistroLivro>
    suspend fun excluirRegistro(id: Long): Outcome<Unit>
    suspend fun devolverChave(id: Long): Outcome<Unit>

    suspend fun veiculos(): Outcome<VeiculosPortaria>
    suspend fun consultarPlaca(placa: String): Outcome<ConsultaPlaca>
    suspend fun movimento(placa: String, sentido: SentidoGaragem, portao: String?): Outcome<MovimentoGaragem>

    suspend fun gas(mes: String? = null): Outcome<GasMes>
    suspend fun lancarGas(mes: String, unidadeId: Long, leitura: BigDecimal): Outcome<LeituraGas>

    suspend fun minhaUnidade(): Outcome<MinhaUnidade>
    suspend fun adicionarPet(nome: String, especie: String?, raca: String?, porte: String?): Outcome<Pet>
    suspend fun removerPet(id: Long): Outcome<Unit>
    suspend fun adicionarDomestico(nome: String, documento: String?, funcao: String?, diasHorarios: String?): Outcome<Domestico>
    suspend fun removerDomestico(id: Long): Outcome<Unit>
}
