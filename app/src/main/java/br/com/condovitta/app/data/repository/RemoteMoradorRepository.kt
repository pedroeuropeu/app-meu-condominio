package br.com.condovitta.app.data.repository

import br.com.condovitta.app.data.remote.AutorizacaoFormDto
import br.com.condovitta.app.data.remote.AutorizadoFormDto
import br.com.condovitta.app.data.remote.MoradorApi
import br.com.condovitta.app.data.remote.RespostaDto
import br.com.condovitta.app.data.remote.apiCall
import br.com.condovitta.app.data.remote.apiUnit
import br.com.condovitta.app.data.remote.toDomain
import br.com.condovitta.app.domain.AppError
import br.com.condovitta.app.domain.Autorizacao
import br.com.condovitta.app.domain.AutorizadoEncomenda
import br.com.condovitta.app.domain.Aviso
import br.com.condovitta.app.domain.Financeiro
import br.com.condovitta.app.domain.MinhasEncomendas
import br.com.condovitta.app.domain.MinhasVisitas
import br.com.condovitta.app.domain.MoradorRepository
import br.com.condovitta.app.domain.NovaAutorizacao
import br.com.condovitta.app.domain.NovoAutorizado
import br.com.condovitta.app.domain.Outcome
import br.com.condovitta.app.domain.Visita
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RemoteMoradorRepository @Inject constructor(private val api: MoradorApi) : MoradorRepository {

    override suspend fun financeiro(): Outcome<Financeiro> = apiCall { api.financeiro().toDomain() }
    override suspend fun boletoPdf(id: Long): Outcome<ByteArray> = apiCall { api.boletoPdf(id).use { it.bytes() } }
    override suspend fun pixQr(id: Long): Outcome<ByteArray?> = semArquivoNull(apiCall { api.pixQr(id).use { it.bytes() } })

    override suspend fun visitas(): Outcome<MinhasVisitas> = apiCall { api.visitas().toDomain() }

    override suspend fun autorizar(form: NovaAutorizacao): Outcome<Autorizacao> = apiCall {
        api.autorizar(
            AutorizacaoFormDto(
                nome = form.nome.trim(), documento = form.documento?.trim()?.ifBlank { null }, telefone = form.telefone?.trim()?.ifBlank { null },
                tipo = form.tipo.name, placa = form.placa?.uppercase()?.filter { it.isLetterOrDigit() }?.ifBlank { null },
                inicio = form.inicio?.format(DATA_HORA), fim = form.fim?.format(DATA_HORA), recorrente = form.recorrente,
                dias = form.dias.takeIf { form.recorrente }?.map { it.name }, horaInicio = form.horaInicio?.format(HORA),
                horaFim = form.horaFim?.format(HORA), validoAte = form.validoAte?.toString(), obs = form.obs?.trim()?.ifBlank { null }
            )
        ).toDomain()
    }

    override suspend fun revogar(id: Long): Outcome<Unit> = apiUnit { api.revogar(id) }
    override suspend fun reativar(id: Long): Outcome<Autorizacao> = apiCall { api.reativar(id).toDomain() }
    override suspend fun qrVisitante(id: Long): Outcome<ByteArray?> = semArquivoNull(apiCall { api.qrVisitante(id).use { it.bytes() } })
    override suspend fun responder(acessoId: Long, liberar: Boolean, recado: String?): Outcome<Visita> =
        apiCall { api.responder(acessoId, RespostaDto(liberar, recado?.trim()?.ifBlank { null })).toDomain() }

    override suspend fun encomendas(): Outcome<MinhasEncomendas> = apiCall { api.encomendas().toDomain() }
    override suspend fun autorizarRetirada(form: NovoAutorizado): Outcome<AutorizadoEncomenda> = apiCall {
        api.autorizarRetirada(
            AutorizadoFormDto(form.nome.trim(), form.documento?.trim()?.ifBlank { null }, form.relacao?.trim()?.ifBlank { null }, form.escopo.name, form.encomendaId, form.validoAte?.toString())
        ).toDomain()
    }
    override suspend fun removerAutorizado(id: Long): Outcome<Unit> = apiUnit { api.removerAutorizado(id) }

    override suspend fun avisos(): Outcome<List<Aviso>> = apiCall { api.mural().map { it.toDomain() } }
    override suspend fun marcarLido(id: Long): Outcome<Unit> = apiUnit { api.lido(id) }
    override suspend fun ciente(id: Long): Outcome<Unit> = apiUnit { api.ciente(id) }

    private fun semArquivoNull(r: Outcome<ByteArray>): Outcome<ByteArray?> = when (r) {
        is Outcome.Ok -> r
        is Outcome.Err -> if (r.error == AppError.NotFound) Outcome.Ok(null) else r
    }

    private companion object {
        val DATA_HORA: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")
        val HORA: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    }
}
