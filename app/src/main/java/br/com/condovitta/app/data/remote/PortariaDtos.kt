package br.com.condovitta.app.data.remote

import br.com.condovitta.app.domain.AcessoListas
import br.com.condovitta.app.domain.AutorizadoRetirada
import br.com.condovitta.app.domain.Bloqueado
import br.com.condovitta.app.domain.Busca
import br.com.condovitta.app.domain.Destinatarios
import br.com.condovitta.app.domain.Encomenda
import br.com.condovitta.app.domain.EncomendasPortaria
import br.com.condovitta.app.domain.Painel
import br.com.condovitta.app.domain.StatusVisita
import br.com.condovitta.app.domain.TamanhoEncomenda
import br.com.condovitta.app.domain.TipoRetirada
import br.com.condovitta.app.domain.TipoVisitante
import br.com.condovitta.app.domain.UnidadeBusca
import br.com.condovitta.app.domain.UnidadeResumo
import br.com.condovitta.app.domain.VeiculoBusca
import br.com.condovitta.app.domain.ViaLiberacao
import br.com.condovitta.app.domain.Visita
import kotlinx.serialization.Serializable
import java.time.LocalDate
import java.time.LocalDateTime

/* DTOs da portaria, fiéis aos records do backend. Datas chegam em ISO ("2026-11-04T09:29:13.123"). */

@Serializable
data class VisitaDto(
    val visitanteId: Long,
    val acessoId: Long? = null,
    val nome: String = "",
    val documento: String? = null,
    val telefone: String? = null,
    val tipo: String? = null,
    val unidadeId: Long? = null,
    val unidade: String? = null,
    val morador: String? = null,
    val placa: String? = null,
    val veiculoModelo: String? = null,
    val veiculoCor: String? = null,
    val status: String? = null,
    val data: String? = null,
    val inicio: String? = null,
    val fim: String? = null,
    val autorizadoAte: String? = null,
    val entradaEm: String? = null,
    val saidaEm: String? = null,
    val porteiro: String? = null,
    val origem: String? = null,
    val recorrencia: String? = null,
    val obs: String? = null,
    val via: String? = null,
    val consultadoEm: String? = null,
    val respondidoEm: String? = null,
    val respondidoPor: String? = null,
    val recado: String? = null,
    val autorizadoPor: String? = null,
    val temFoto: Boolean = false,
    val avisar: Boolean = false
)

@Serializable
data class ListasDto(val esperados: List<VisitaDto> = emptyList(), val dentro: List<VisitaDto> = emptyList(), val historico: List<VisitaDto> = emptyList(), val bloqueados: Int = 0)

@Serializable
data class BloqueadoDto(val id: Long, val nome: String = "", val documento: String? = null, val motivo: String? = null, val por: String? = null, val desde: String? = null)

@Serializable
data class VisitanteAvulsoDto(
    val nome: String,
    val documento: String? = null,
    val telefone: String? = null,
    val tipo: String,
    val unidadeId: Long? = null,
    val obs: String? = null,
    val veiculoModelo: String? = null,
    val veiculoCor: String? = null,
    val placa: String? = null,
    val data: String? = null,
    val inicio: String,
    val fim: String,
    val recorrente: Boolean = false,
    val consultar: Boolean = false
)

@Serializable
data class EntradaDto(val via: String? = null)

@Serializable
data class UnidadeResumoDto(val id: Long, val rotulo: String = "", val bloco: String? = null, val andar: Int? = null, val numero: Int? = null, val responsavel: String? = null, val moradores: Int = 0)

@Serializable
data class EncomendaDto(
    val id: Long,
    val unidadeId: Long = 0,
    val unidade: String? = null,
    val destinatario: String? = null,
    val transportadora: String? = null,
    val tamanho: String? = null,
    val rastreio: String? = null,
    val local: String? = null,
    val perecivel: Boolean = false,
    val temFoto: Boolean = false,
    val codigo: String? = null,
    val situacao: String? = null,
    val recebidaEm: String? = null,
    val recebidaPor: String? = null,
    val avisos: Int = 0,
    val ultimoAvisoEm: String? = null,
    val retiradaEm: String? = null,
    val retiradaPor: String? = null,
    val retiradaDocumento: String? = null,
    val tipoRetirada: String? = null,
    val entreguePor: String? = null,
    val temAssinatura: Boolean = false
)

@Serializable
data class EncomendasPortariaDto(val aguardando: List<EncomendaDto> = emptyList(), val entregues: List<EncomendaDto> = emptyList(), val recebidasHoje: Long = 0, val entreguesHoje: Long = 0)

@Serializable
data class NovaEncomendaDto(
    val unidadeId: Long,
    val destinatario: String? = null,
    val transportadora: String,
    val tamanho: String? = null,
    val rastreio: String? = null,
    val local: String? = null,
    val perecivel: Boolean = false
)

@Serializable
data class EntregaDto(val modo: String, val codigo: String? = null, val nome: String? = null, val documento: String? = null, val assinatura: String)

@Serializable
data class AutorizadoDto(val id: Long? = null, val nome: String = "", val documento: String? = null, val relacao: String? = null, val fixo: Boolean = false)

@Serializable
data class DestinatariosDto(val unidade: String? = null, val moradores: List<String> = emptyList(), val autorizados: List<AutorizadoDto> = emptyList())

@Serializable
data class PainelDto(
    val turno: String? = null,
    val faixa: String? = null,
    val proximoTurno: String? = null,
    val esperados: List<VisitaDto> = emptyList(),
    val dentro: List<VisitaDto> = emptyList(),
    val encomendasAguardando: Long = 0,
    val encomendasParadas7: Long = 0,
    val pereciveis: List<EncomendaDto> = emptyList()
)

@Serializable
data class UnidadeBuscaDto(val unidadeId: Long, val unidade: String = "", val morador: String? = null, val moradores: Int = 0, val encomendasAguardando: Long = 0)

@Serializable
data class VeiculoDto(
    val id: Long? = null,
    val placa: String = "",
    val modelo: String? = null,
    val cor: String? = null,
    val unidadeId: Long? = null,
    val unidade: String? = null,
    val morador: String? = null,
    val vaga: String? = null,
    val acesso: String? = null,
    val bloqueado: Boolean = false,
    val obs: String? = null
)

@Serializable
data class BuscaDto(
    val bloqueados: List<BloqueadoDto> = emptyList(),
    val visitantes: List<VisitaDto> = emptyList(),
    val unidades: List<UnidadeBuscaDto> = emptyList(),
    val veiculos: List<VeiculoDto> = emptyList()
)

/* ---------------- mapeamento ---------------- */

internal fun dateTime(s: String?): LocalDateTime? = s?.let { runCatching { LocalDateTime.parse(it) }.getOrNull() }
internal fun date(s: String?): LocalDate? = s?.let { runCatching { LocalDate.parse(it.take(10)) }.getOrNull() }

/** "14:00:00" → "14:00" */
internal fun hhmm(s: String?): String? = s?.takeIf { it.length >= 5 }?.take(5)

fun VisitaDto.toDomain() = Visita(
    visitanteId = visitanteId, acessoId = acessoId, nome = nome, documento = documento, telefone = telefone,
    tipo = TipoVisitante.of(tipo), unidadeId = unidadeId, unidade = unidade ?: "Condomínio", morador = morador,
    placa = placa, veiculoModelo = veiculoModelo, veiculoCor = veiculoCor, status = StatusVisita.of(status),
    inicio = hhmm(inicio), fim = hhmm(fim), entradaEm = dateTime(entradaEm), saidaEm = dateTime(saidaEm), porteiro = porteiro,
    daPortaria = origem == "PORTARIA", recorrencia = recorrencia, obs = obs, via = ViaLiberacao.of(via),
    consultadoEm = dateTime(consultadoEm), respondidoEm = dateTime(respondidoEm), respondidoPor = respondidoPor,
    recado = recado, autorizadoPor = autorizadoPor, temFoto = temFoto
)

fun ListasDto.toDomain() = AcessoListas(esperados.map { it.toDomain() }, dentro.map { it.toDomain() }, historico.map { it.toDomain() }, bloqueados)

fun BloqueadoDto.toDomain() = Bloqueado(id, nome, documento, motivo, por, date(desde))

fun UnidadeResumoDto.toDomain() = UnidadeResumo(id, rotulo, bloco, responsavel, moradores)

fun EncomendaDto.toDomain() = Encomenda(
    id = id, unidadeId = unidadeId, unidade = unidade ?: "—", destinatario = destinatario, transportadora = transportadora ?: "—",
    tamanho = TamanhoEncomenda.of(tamanho), rastreio = rastreio, local = local, perecivel = perecivel, temFoto = temFoto,
    retirada = situacao == "RETIRADA", recebidaEm = dateTime(recebidaEm), recebidaPor = recebidaPor, avisos = avisos,
    ultimoAvisoEm = dateTime(ultimoAvisoEm), retiradaEm = dateTime(retiradaEm), retiradaPor = retiradaPor,
    retiradaDocumento = retiradaDocumento, tipoRetirada = TipoRetirada.entries.firstOrNull { it.name == tipoRetirada },
    entreguePor = entreguePor, temAssinatura = temAssinatura, codigo = codigo
)

fun EncomendasPortariaDto.toDomain() = EncomendasPortaria(aguardando.map { it.toDomain() }, entregues.map { it.toDomain() }, recebidasHoje, entreguesHoje)

fun DestinatariosDto.toDomain() = Destinatarios(unidade ?: "—", moradores, autorizados.map { AutorizadoRetirada(it.nome, it.documento, it.relacao) })

fun PainelDto.toDomain() = Painel(
    turno, faixa, proximoTurno, esperados.map { it.toDomain() }, dentro.map { it.toDomain() },
    encomendasAguardando, encomendasParadas7, pereciveis.map { it.toDomain() }
)

fun BuscaDto.toDomain() = Busca(
    bloqueados.map { it.toDomain() }, visitantes.map { it.toDomain() },
    unidades.map { UnidadeBusca(it.unidadeId, it.unidade, it.morador, it.moradores, it.encomendasAguardando) },
    veiculos.map { VeiculoBusca(it.placa, it.modelo, it.cor, it.unidade, it.morador, it.acesso, it.bloqueado, it.obs) }
)
