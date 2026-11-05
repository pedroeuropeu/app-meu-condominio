package br.com.condovitta.app.data.repository

import br.com.condovitta.app.data.remote.ApiJson
import br.com.condovitta.app.data.remote.CondominioContext
import br.com.condovitta.app.data.remote.EntradaDto
import br.com.condovitta.app.data.remote.EntregaDto
import br.com.condovitta.app.data.remote.NovaEncomendaDto
import br.com.condovitta.app.data.remote.PortariaApi
import br.com.condovitta.app.data.remote.VisitanteAvulsoDto
import br.com.condovitta.app.data.remote.apiCall
import br.com.condovitta.app.data.remote.apiUnit
import br.com.condovitta.app.data.remote.toDomain
import br.com.condovitta.app.domain.AcessoListas
import br.com.condovitta.app.domain.Bloqueado
import br.com.condovitta.app.domain.Busca
import br.com.condovitta.app.domain.Destinatarios
import br.com.condovitta.app.domain.Encomenda
import br.com.condovitta.app.domain.EncomendasPortaria
import br.com.condovitta.app.domain.Entrega
import br.com.condovitta.app.domain.NovaEncomenda
import br.com.condovitta.app.domain.NovaVisita
import br.com.condovitta.app.domain.Outcome
import br.com.condovitta.app.domain.Painel
import br.com.condovitta.app.domain.PortariaRepository
import br.com.condovitta.app.domain.UnidadeResumo
import br.com.condovitta.app.domain.ViaLiberacao
import br.com.condovitta.app.domain.Visita
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.time.format.DateTimeFormatter
import java.util.Base64
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RemotePortariaRepository @Inject constructor(
    private val api: PortariaApi,
    private val condominio: CondominioContext
) : PortariaRepository {

    /** a lista de unidades muda pouco: guarda por condomínio enquanto o app está aberto */
    private var unidadesCache: Pair<Long?, List<UnidadeResumo>>? = null

    override suspend fun painel(): Outcome<Painel> = apiCall { api.painel().toDomain() }
    override suspend fun busca(q: String): Outcome<Busca> = apiCall { api.busca(q.trim()).toDomain() }

    override suspend fun unidades(): Outcome<List<UnidadeResumo>> {
        val condo = condominio.condominioId
        unidadesCache?.takeIf { it.first == condo }?.let { return Outcome.Ok(it.second) }
        val r = apiCall { api.unidades().map { it.toDomain() } }
        if (r is Outcome.Ok) unidadesCache = condo to r.value
        return r
    }

    override suspend fun acesso(): Outcome<AcessoListas> = apiCall { api.acesso().toDomain() }
    override suspend fun bloqueados(): Outcome<List<Bloqueado>> = apiCall { api.bloqueados().map { it.toDomain() } }

    override suspend fun novaVisita(form: NovaVisita): Outcome<Visita> = apiCall {
        api.novaVisita(
            VisitanteAvulsoDto(
                nome = form.nome.trim(), documento = form.documento?.trim()?.ifBlank { null }, telefone = form.telefone?.trim()?.ifBlank { null },
                tipo = form.tipo.name, unidadeId = form.unidadeId, obs = form.obs?.trim()?.ifBlank { null },
                placa = form.placa?.uppercase()?.filter { it.isLetterOrDigit() }?.ifBlank { null },
                inicio = form.inicio.format(HORA), fim = form.fim.format(HORA), consultar = form.consultar
            )
        ).toDomain()
    }

    override suspend fun excluirVisita(visitanteId: Long): Outcome<Unit> = apiUnit { api.excluirVisita(visitanteId) }
    override suspend fun entrada(visitanteId: Long, via: ViaLiberacao?): Outcome<Visita> = apiCall { api.entrada(visitanteId, EntradaDto(via?.name)).toDomain() }
    override suspend fun consultar(visitanteId: Long): Outcome<Visita> = apiCall { api.consultar(visitanteId).toDomain() }
    override suspend fun recusar(acessoId: Long): Outcome<Visita> = apiCall { api.recusar(acessoId).toDomain() }
    override suspend fun saida(acessoId: Long): Outcome<Visita> = apiCall { api.saida(acessoId).toDomain() }
    override suspend fun desfazer(acessoId: Long): Outcome<Unit> = apiUnit { api.desfazer(acessoId) }

    override suspend fun encomendas(): Outcome<EncomendasPortaria> = apiCall { api.encomendas().toDomain() }
    override suspend fun destinatarios(unidadeId: Long): Outcome<Destinatarios> = apiCall { api.destinatarios(unidadeId).toDomain() }

    override suspend fun registrarEncomenda(form: NovaEncomenda, foto: ByteArray?): Outcome<Encomenda> = apiCall {
        val dados = NovaEncomendaDto(
            unidadeId = form.unidadeId, destinatario = form.destinatario, transportadora = form.transportadora,
            tamanho = form.tamanho?.name, rastreio = form.rastreio?.trim()?.ifBlank { null }, local = form.local, perecivel = form.perecivel
        )
        val json = ApiJson.encodeToString(NovaEncomendaDto.serializer(), dados).toRequestBody("application/json".toMediaType())
        val part = foto?.let { MultipartBody.Part.createFormData("foto", "encomenda.jpg", it.toRequestBody("image/jpeg".toMediaType())) }
        api.registrarEncomenda(json, part).toDomain()
    }

    override suspend fun excluirEncomenda(id: Long): Outcome<Unit> = apiUnit { api.excluirEncomenda(id) }
    override suspend fun avisar(id: Long): Outcome<Encomenda> = apiCall { api.avisar(id).toDomain() }

    override suspend fun entregar(id: Long, entrega: Entrega): Outcome<Encomenda> = apiCall {
        api.entregar(
            id,
            EntregaDto(
                modo = entrega.modo.name, codigo = entrega.codigo?.filter { it.isDigit() }, nome = entrega.nome?.trim(),
                documento = entrega.documento?.trim(), assinatura = PNG_DATA_URL + Base64.getEncoder().encodeToString(entrega.assinatura)
            )
        ).toDomain()
    }

    override suspend fun desfazerEntrega(id: Long): Outcome<Encomenda> = apiCall { api.desfazerEntrega(id).toDomain() }

    override suspend fun fotoEncomenda(id: Long): Outcome<ByteArray?> = when (val r = apiCall { api.fotoEncomenda(id).use { it.bytes() } }) {
        is Outcome.Ok -> r
        // sem foto o backend responde 404: não é erro para a tela
        is Outcome.Err -> if (r.error == br.com.condovitta.app.domain.AppError.NotFound) Outcome.Ok(null) else r
    }

    private companion object {
        val HORA: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
        const val PNG_DATA_URL = "data:image/png;base64,"
    }
}

