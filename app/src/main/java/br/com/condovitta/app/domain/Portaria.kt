package br.com.condovitta.app.domain

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/* Modelos da portaria (AcessoDTO, EncomendaDTO e PortariaDTO do gestao-condominio). */

enum class TipoVisitante(val label: String) {
    VISITA("Visita"), PRESTADOR("Prestador"), ENTREGA("Entrega"), APP("App de transporte");

    companion object {
        fun of(v: String?) = entries.firstOrNull { it.name == v } ?: VISITA
    }
}

/** Situação da visita no dia (o backend manda em minúsculas). */
enum class StatusVisita {
    ESPERADO, AGUARDANDO, LIBERADO, DENTRO, SAIU, RECUSADO;

    /** ainda vai entrar (a portaria libera ou recusa) */
    val pendente: Boolean get() = this == ESPERADO || this == AGUARDANDO || this == LIBERADO

    companion object {
        fun of(v: String?) = entries.firstOrNull { it.name.equals(v, ignoreCase = true) } ?: ESPERADO
    }
}

/** Como a entrada foi liberada. */
enum class ViaLiberacao(val label: String) {
    AUTORIZACAO("Autorizado no app"), APP("Liberado pelo morador no app"), INTERFONE("Liberado pelo interfone");

    companion object {
        fun of(v: String?) = entries.firstOrNull { it.name == v }
    }
}

data class Visita(
    val visitanteId: Long,
    val acessoId: Long?,
    val nome: String,
    val documento: String?,
    val telefone: String?,
    val tipo: TipoVisitante,
    val unidadeId: Long?,
    val unidade: String,
    val morador: String?,
    val placa: String?,
    val veiculoModelo: String?,
    val veiculoCor: String?,
    val status: StatusVisita,
    val inicio: String?,
    val fim: String?,
    val entradaEm: LocalDateTime?,
    val saidaEm: LocalDateTime?,
    val porteiro: String?,
    val daPortaria: Boolean,
    val recorrencia: String?,
    val obs: String?,
    val via: ViaLiberacao?,
    val consultadoEm: LocalDateTime?,
    val respondidoEm: LocalDateTime?,
    val respondidoPor: String?,
    val recado: String?,
    val autorizadoPor: String?,
    /** a portaria tirou a foto de quem entrou */
    val temFoto: Boolean = false
) {
    val veiculo: String? get() = listOfNotNull(veiculoModelo, veiculoCor).joinToString(" ").ifBlank { null }
    val janela: String? get() = if (inicio != null && fim != null) "$inicio – $fim" else null
}

/** As três abas de Visitantes (GET /portaria/acesso). */
data class AcessoListas(val esperados: List<Visita>, val dentro: List<Visita>, val historico: List<Visita>, val bloqueados: Int) {
    fun todas() = esperados + dentro + historico
}

data class Bloqueado(val id: Long, val nome: String, val documento: String?, val motivo: String?, val por: String?, val desde: LocalDate?)

data class UnidadeResumo(val id: Long, val rotulo: String, val bloco: String?, val responsavel: String?, val moradores: Int)

enum class TamanhoEncomenda(val label: String) {
    ENVELOPE("Envelope"), PEQUENO("Pequeno"), MEDIO("Médio"), GRANDE("Grande"), VOLUME_EXTRA("Volume extra");

    companion object {
        fun of(v: String?) = entries.firstOrNull { it.name == v }
    }
}

enum class TipoRetirada { MORADOR, TERCEIRO }

data class Encomenda(
    val id: Long,
    val unidadeId: Long,
    val unidade: String,
    val destinatario: String?,
    val transportadora: String,
    val tamanho: TamanhoEncomenda?,
    val rastreio: String?,
    val local: String?,
    val perecivel: Boolean,
    val temFoto: Boolean,
    val retirada: Boolean,
    val recebidaEm: LocalDateTime?,
    val recebidaPor: String?,
    val avisos: Int,
    val ultimoAvisoEm: LocalDateTime?,
    val retiradaEm: LocalDateTime?,
    val retiradaPor: String?,
    val retiradaDocumento: String?,
    val tipoRetirada: TipoRetirada?,
    val entreguePor: String?,
    val temAssinatura: Boolean,
    /** código de retirada: só vem na tela do morador */
    val codigo: String? = null
)

data class EncomendasPortaria(val aguardando: List<Encomenda>, val entregues: List<Encomenda>, val recebidasHoje: Long, val entreguesHoje: Long)

/** Quem pode retirar: moradores da unidade e terceiros autorizados por eles. */
data class Destinatarios(val unidade: String, val moradores: List<String>, val autorizados: List<AutorizadoRetirada>)

data class AutorizadoRetirada(val nome: String, val documento: String?, val relacao: String?)

/** Painel do turno (GET /portaria/painel). */
data class Painel(
    val turno: String?,
    val faixa: String?,
    val proximoTurno: String?,
    val esperados: List<Visita>,
    val dentro: List<Visita>,
    val encomendasAguardando: Long,
    val encomendasParadas7: Long,
    val pereciveis: List<Encomenda>
)

data class UnidadeBusca(val unidadeId: Long, val unidade: String, val morador: String?, val moradores: Int, val encomendasAguardando: Long)

data class VeiculoBusca(val placa: String, val modelo: String?, val cor: String?, val unidade: String?, val morador: String?, val acesso: String?, val bloqueado: Boolean, val obs: String?)

/** Busca rápida do painel: morador, unidade ou placa (GET /portaria/busca). */
data class Busca(val bloqueados: List<Bloqueado>, val visitantes: List<Visita>, val unidades: List<UnidadeBusca>, val veiculos: List<VeiculoBusca>) {
    val vazia: Boolean get() = bloqueados.isEmpty() && visitantes.isEmpty() && unidades.isEmpty() && veiculos.isEmpty()
}

/** Visita avulsa cadastrada na portaria. [consultar] = pergunta ao morador pelo app antes de liberar. */
data class NovaVisita(
    val nome: String,
    val tipo: TipoVisitante,
    val unidadeId: Long?,
    val documento: String? = null,
    val telefone: String? = null,
    val placa: String? = null,
    val obs: String? = null,
    val inicio: LocalTime,
    val fim: LocalTime,
    val consultar: Boolean = false
)

data class NovaEncomenda(
    val unidadeId: Long,
    val destinatario: String?,
    val transportadora: String,
    val tamanho: TamanhoEncomenda?,
    val local: String?,
    val perecivel: Boolean,
    val rastreio: String? = null
)

/** Retirada: o morador mostra o [codigo]; terceiro informa [nome] e [documento]. [assinatura] é um PNG. */
data class Entrega(val modo: TipoRetirada, val codigo: String? = null, val nome: String? = null, val documento: String? = null, val assinatura: ByteArray) {
    override fun equals(other: Any?) = other is Entrega && modo == other.modo && codigo == other.codigo && nome == other.nome &&
        documento == other.documento && assinatura.contentEquals(other.assinatura)
    override fun hashCode() = listOf(modo, codigo, nome, documento).hashCode() * 31 + assinatura.contentHashCode()
}

object Portaria {
    val TRANSPORTADORAS = listOf("Mercado Livre", "Amazon", "Shopee", "Correios", "Magalu", "Loggi", "Jadlog", "Total Express", "iFood Mercado", "Outra")
    val LOCAIS = listOf("Prateleira A", "Prateleira B", "Prateleira C", "Prateleira D", "Sala de volumes", "Geladeira da portaria")
    const val GELADEIRA = "Geladeira da portaria"

    /** Placa no padrão "ABC-1234" (antiga) ou "ABC1D23" (Mercosul). */
    fun formatarPlaca(p: String?): String? {
        val s = p?.uppercase()?.filter { it.isLetterOrDigit() }?.take(7)?.ifBlank { null } ?: return null
        return if (Regex("^[A-Z]{3}\\d{4}$").matches(s)) "${s.take(3)}-${s.drop(3)}" else s
    }

    /** encomenda parada: muitos avisos sem retirada */
    fun atrasada(e: Encomenda) = !e.retirada && e.avisos >= 3

    /** número da unidade sem o bloco: "B-204" → "204", "Apto 12" → "12" */
    fun numeroDa(u: UnidadeResumo): String = u.rotulo.substringAfterLast('-').filter { it.isLetterOrDigit() }.let { r ->
        if (r.any { it.isDigit() } && r.any { it.isLetter() } && '-' !in u.rotulo) r.filter { it.isDigit() } else r
    }

    /** a unidade do teclado da portaria: bloco (se houver) + número digitado, ignorando zeros à esquerda */
    fun acharUnidade(unidades: List<UnidadeResumo>, bloco: String?, numero: String): UnidadeResumo? {
        val n = numero.trimStart('0').ifEmpty { if (numero.isEmpty()) return null else "0" }
        return unidades.filter { bloco == null || it.bloco.isNullOrBlank() || it.bloco.equals(bloco, ignoreCase = true) }
            .firstOrNull { numeroDa(it).trimStart('0').equals(n, ignoreCase = true) }
    }
}
