package br.com.condovitta.app.data.repository

import br.com.condovitta.app.data.remote.AtendimentoApi
import br.com.condovitta.app.data.remote.AvaliacaoDto
import br.com.condovitta.app.data.remote.ContestacaoDto
import br.com.condovitta.app.data.remote.GovernancaApi
import br.com.condovitta.app.data.remote.LembreteDto
import br.com.condovitta.app.data.remote.MotivoDto
import br.com.condovitta.app.data.remote.NovaOcorrenciaDto
import br.com.condovitta.app.data.remote.NovoChamadoDto
import br.com.condovitta.app.data.remote.ProcuracaoFormDto
import br.com.condovitta.app.data.remote.TextoDto
import br.com.condovitta.app.data.remote.TransparenciaApi
import br.com.condovitta.app.data.remote.VotoDto
import br.com.condovitta.app.data.remote.apiCall
import br.com.condovitta.app.data.remote.apiUnit
import br.com.condovitta.app.data.remote.jsonPart
import br.com.condovitta.app.data.remote.parts
import br.com.condovitta.app.data.remote.textPart
import br.com.condovitta.app.data.remote.toDomain
import br.com.condovitta.app.domain.ArquivoEnvio
import br.com.condovitta.app.domain.AtendimentoRepository
import br.com.condovitta.app.domain.BalanceteHistorico
import br.com.condovitta.app.domain.ChamadoDetalhe
import br.com.condovitta.app.domain.ChatPlenario
import br.com.condovitta.app.domain.DocParaAssinar
import br.com.condovitta.app.domain.EstadoPlenario
import br.com.condovitta.app.domain.EventoCalendario
import br.com.condovitta.app.domain.Evidencias
import br.com.condovitta.app.domain.GovernancaRepository
import br.com.condovitta.app.domain.Lembrete
import br.com.condovitta.app.domain.MeuGas
import br.com.condovitta.app.domain.MeusChamados
import br.com.condovitta.app.domain.MeusDocumentos
import br.com.condovitta.app.domain.MinhasAssembleias
import br.com.condovitta.app.domain.NovaOcorrencia
import br.com.condovitta.app.domain.NovoChamado
import br.com.condovitta.app.domain.OpcaoVoto
import br.com.condovitta.app.domain.Outcome
import br.com.condovitta.app.domain.PainelOcorrencias
import br.com.condovitta.app.domain.Poderes
import br.com.condovitta.app.domain.Procuracao
import br.com.condovitta.app.domain.Recibo
import br.com.condovitta.app.domain.Representante
import br.com.condovitta.app.domain.ResultadoAssinatura
import br.com.condovitta.app.domain.Transparencia
import br.com.condovitta.app.domain.TransparenciaRepository
import br.com.condovitta.app.domain.UnidadeOpcao
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

/** PNG como data URL (assinatura desenhada). */
internal fun dataUrlPng(png: ByteArray): String = "data:image/png;base64," + java.util.Base64.getEncoder().encodeToString(png)

private fun String?.limpo(): String? = this?.trim()?.ifBlank { null }

@Singleton
class RemoteAtendimentoRepository @Inject constructor(private val api: AtendimentoApi) : AtendimentoRepository {
    override suspend fun chamados(): Outcome<MeusChamados> = apiCall {
        coroutineScope {
            val itens = async { api.chamados() }
            val resumo = async { api.resumo() }
            MeusChamados(itens.await().map { it.toDomain() }, resumo.await().toDomain())
        }
    }

    override suspend fun chamado(id: Long): Outcome<ChamadoDetalhe> = apiCall { api.chamado(id).toDomain() }

    override suspend fun abrirChamado(form: NovoChamado, arquivos: List<ArquivoEnvio>): Outcome<ChamadoDetalhe> = apiCall {
        val dto = NovoChamadoDto(form.titulo.trim(), form.categoria.name, form.prioridade.name, form.local.limpo(), form.descricao.limpo(), form.autorizaEntrada)
        api.abrir(jsonPart(NovoChamadoDto.serializer(), dto), arquivos.parts("arquivos")).toDomain()
    }

    override suspend fun responder(id: Long, texto: String?, arquivos: List<ArquivoEnvio>): Outcome<ChamadoDetalhe> =
        apiCall { api.responder(id, texto.limpo()?.let(::textPart), arquivos.parts("arquivos")).toDomain() }

    override suspend fun avaliar(id: Long, nota: Int, comentario: String?): Outcome<Unit> = apiUnit { api.avaliar(id, AvaliacaoDto(nota, comentario.limpo())) }
    override suspend fun reabrir(id: Long): Outcome<ChamadoDetalhe> = apiCall { api.reabrir(id).toDomain() }
    override suspend fun cancelarChamado(id: Long): Outcome<Unit> = apiUnit { api.cancelar(id) }
    override suspend fun anexoChamado(id: Long, anexoId: Long): Outcome<ByteArray> = apiCall { api.anexoChamado(id, anexoId).use { it.bytes() } }

    override suspend fun ocorrencias(): Outcome<PainelOcorrencias> = apiCall { api.ocorrencias().toDomain() }
    override suspend fun unidades(): Outcome<List<UnidadeOpcao>> = apiCall { api.unidades().map { it.toDomain() } }

    override suspend fun registrarOcorrencia(form: NovaOcorrencia, provas: List<ArquivoEnvio>): Outcome<PainelOcorrencias> = apiCall {
        val dto = NovaOcorrenciaDto(
            geral = form.geral, unidade = if (form.geral) null else form.unidade, motivo = form.motivo.name,
            ocorridaEm = form.ocorridaEm.withSecond(0).withNano(0).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
            local = form.local.limpo(), descricao = form.descricao.trim(), testemunhas = form.testemunhas.limpo(), declaracao = true
        )
        api.registrar(jsonPart(NovaOcorrenciaDto.serializer(), dto), provas.parts("provas")).toDomain()
    }

    override suspend fun defesa(id: Long, texto: String, provas: List<ArquivoEnvio>): Outcome<PainelOcorrencias> =
        apiCall { api.defesa(id, textPart(texto.trim()), provas.parts("provas")).toDomain() }

    override suspend fun anexoOcorrencia(id: Long, anexoId: Long): Outcome<ByteArray> = apiCall { api.anexoOcorrencia(id, anexoId).use { it.bytes() } }
}

@Singleton
class RemoteGovernancaRepository @Inject constructor(private val api: GovernancaApi) : GovernancaRepository {
    override suspend fun documentos(): Outcome<MeusDocumentos> = apiCall { api.documentos().toDomain() }
    override suspend fun documento(id: Long): Outcome<DocParaAssinar> = apiCall { api.documento(id).toDomain() }
    override suspend fun documentoPdf(id: Long): Outcome<ByteArray> = apiCall { api.documentoPdf(id).use { it.bytes() } }
    override suspend fun enviarCodigo(id: Long): Outcome<String> = apiCall { api.enviarCodigo(id).email ?: "o seu e-mail" }
    override suspend fun assinar(id: Long, png: ByteArray, codigo: String?): Outcome<ResultadoAssinatura> =
        apiCall { api.assinar(id, textPart("DESENHO"), textPart(dataUrlPng(png)), codigo.limpo()?.let(::textPart)).toDomain() }
    override suspend fun recusar(id: Long, motivo: String): Outcome<Unit> = apiUnit { api.recusar(id, MotivoDto(motivo.trim())) }
    override suspend fun evidencias(id: Long): Outcome<Evidencias> = apiCall { api.evidencias(id).toDomain() }
    override suspend fun oficialPdf(id: Long): Outcome<ByteArray> = apiCall { api.oficialPdf(id).use { it.bytes() } }

    override suspend fun assembleias(): Outcome<MinhasAssembleias> = apiCall { api.assembleias().toDomain() }
    override suspend fun edital(id: Long): Outcome<ByteArray> = apiCall { api.edital(id).use { it.bytes() } }
    override suspend fun ata(id: Long): Outcome<ByteArray> = apiCall { api.ata(id).use { it.bytes() } }
    override suspend fun representantes(id: Long): Outcome<List<Representante>> = apiCall { api.representantes(id).map { Representante(it.usuarioId, it.nome, it.unidade) } }
    override suspend fun enviarProcuracao(id: Long, representanteId: Long, poderes: Poderes, png: ByteArray): Outcome<Procuracao> =
        apiCall { api.procuracao(id, ProcuracaoFormDto(representanteId, poderes.name, dataUrlPng(png))).toDomain() }
    override suspend fun cancelarProcuracao(id: Long, procuracaoId: Long): Outcome<Unit> = apiUnit { api.cancelarProcuracao(id, procuracaoId) }

    override suspend fun plenarioAtual(): Outcome<Long?> = apiCall { api.plenarioAtual().id }
    override suspend fun estado(id: Long): Outcome<EstadoPlenario> = apiCall { api.estado(id).toDomain() }
    override suspend fun presenca(id: Long): Outcome<EstadoPlenario> = apiCall { api.presenca(id).toDomain() }
    override suspend fun votar(id: Long, itemId: Long, opcao: OpcaoVoto): Outcome<Recibo> = apiCall { api.votar(id, itemId, VotoDto(opcao.name)).toDomain() }
    override suspend fun palavra(id: Long): Outcome<Int?> = apiCall { api.palavra(id).let { if (it.naFila) it.posicao ?: 1 else null } }
    override suspend fun chat(id: Long, texto: String): Outcome<ChatPlenario> = apiCall { api.chat(id, TextoDto(texto.trim())).toDomain() }
    override suspend fun comprovante(id: Long, itemId: Long): Outcome<ByteArray> = apiCall { api.comprovante(id, itemId).use { it.bytes() } }
}

@Singleton
class RemoteTransparenciaRepository @Inject constructor(private val api: TransparenciaApi) : TransparenciaRepository {
    override suspend fun transparencia(mes: String?): Outcome<Transparencia> = apiCall { api.transparencia(mes).toDomain() }
    override suspend fun balancetePdf(mes: String): Outcome<ByteArray> = apiCall { api.balancetePdf(mes).use { it.bytes() } }
    override suspend fun nota(lancamentoId: Long): Outcome<ByteArray> = apiCall { api.nota(lancamentoId).use { it.bytes() } }
    override suspend fun balancetes(): Outcome<List<BalanceteHistorico>> = apiCall { api.balancetes().map { it.toDomain() } }
    override suspend fun balanceteArquivo(id: Long): Outcome<ByteArray> = apiCall { api.balanceteArquivo(id).use { it.bytes() } }

    override suspend fun meuGas(): Outcome<MeuGas> = apiCall { api.meuGas().toDomain() }
    override suspend fun contestarGas(mensagem: String?): Outcome<Unit> = apiUnit { api.contestar(ContestacaoDto(mensagem.limpo())) }
    override suspend fun fotoGas(leituraId: Long): Outcome<ByteArray> = apiCall { api.fotoGas(leituraId).use { it.bytes() } }

    override suspend fun calendario(de: LocalDate, ate: LocalDate): Outcome<List<EventoCalendario>> =
        apiCall { api.calendario(de.toString(), ate.toString()).mapNotNull { it.toDomain() }.sortedWith(compareBy({ it.data }, { it.inicio })) }
    override suspend fun lembretes(): Outcome<List<Lembrete>> = apiCall { api.lembretes().mapNotNull { it.toDomain() } }
    override suspend fun lembrete(eventoId: Long, data: LocalDate, minutosAntes: Int?): Outcome<Unit> =
        apiUnit { api.lembrete(LembreteDto(eventoId, data.toString(), minutosAntes?.takeIf { it > 0 })) }
    override suspend fun ics(eventoId: Long, data: LocalDate): Outcome<ByteArray> = apiCall { api.ics(eventoId, data.toString()).use { it.bytes() } }
}
