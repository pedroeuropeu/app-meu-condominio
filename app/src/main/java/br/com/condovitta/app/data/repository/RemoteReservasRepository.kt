package br.com.condovitta.app.data.repository

import br.com.condovitta.app.data.remote.ApiJson
import br.com.condovitta.app.data.remote.DevolucaoDto
import br.com.condovitta.app.data.remote.IncluirConvidadoDto
import br.com.condovitta.app.data.remote.ListaDto
import br.com.condovitta.app.data.remote.NovaReservaDto
import br.com.condovitta.app.data.remote.NovoConvidadoDto
import br.com.condovitta.app.data.remote.ReservasApi
import br.com.condovitta.app.data.remote.apiCall
import br.com.condovitta.app.data.remote.toDomain
import br.com.condovitta.app.domain.AreaComum
import br.com.condovitta.app.domain.Cancelamento
import br.com.condovitta.app.domain.Convidado
import br.com.condovitta.app.domain.Disponibilidade
import br.com.condovitta.app.domain.ModoAssinatura
import br.com.condovitta.app.domain.NovaReserva
import br.com.condovitta.app.domain.Outcome
import br.com.condovitta.app.domain.Reserva
import br.com.condovitta.app.domain.ReservasPortaria
import br.com.condovitta.app.domain.ReservasRepository
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.time.YearMonth
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RemoteReservasRepository @Inject constructor(private val api: ReservasApi) : ReservasRepository {

    override suspend fun areas(): Outcome<List<AreaComum>> = apiCall { api.areas().map { it.toDomain() } }
    override suspend fun disponibilidade(areaId: Long, mes: YearMonth): Outcome<Disponibilidade> =
        apiCall { api.disponibilidade(areaId, mes.year, mes.monthValue).toDomain() }
    override suspend fun minhas(): Outcome<List<Reserva>> = apiCall { api.minhas().map { it.toDomain() } }

    override suspend fun criar(form: NovaReserva): Outcome<Reserva> = apiCall {
        val dto = NovaReservaDto(
            areaId = form.areaId, itemId = form.itemId, turnoId = form.turnoId, data = form.data.toString(), convidados = form.convidados,
            lista = form.lista.map { it.trim() }.filter { it.isNotEmpty() }.map { NovoConvidadoDto(it) }.ifEmpty { null },
            aceite = form.aceite, modoAssinatura = form.modo?.name, nomeDigitado = form.nomeDigitado?.trim()?.ifBlank { null },
            observacoes = form.observacoes?.trim()?.ifBlank { null }
        )
        val png = form.assinatura
        if (form.modo == ModoAssinatura.DESENHO && png != null) {
            val dados = ApiJson.encodeToString(NovaReservaDto.serializer(), dto).toRequestBody("application/json".toMediaType())
            api.criarComArquivo(dados, MultipartBody.Part.createFormData("arquivo", "assinatura.png", png.toRequestBody("image/png".toMediaType()))).toDomain()
        } else api.criar(dto).toDomain()
    }

    override suspend fun cancelar(id: Long): Outcome<Cancelamento> = apiCall { api.cancelar(id).toDomain() }
    override suspend fun desfazerCancelamento(id: Long): Outcome<Reserva> = apiCall { api.desfazerCancelamento(id).toDomain() }
    override suspend fun atualizarLista(id: Long, nomes: List<String>): Outcome<Reserva> =
        apiCall { api.atualizarLista(id, ListaDto(nomes.map { it.trim() }.filter { it.isNotEmpty() }.map { NovoConvidadoDto(it) })).toDomain() }

    override suspend fun portaria(): Outcome<ReservasPortaria> = apiCall { api.portaria().toDomain() }
    override suspend fun checkin(reservaId: Long, convidadoId: Long): Outcome<Convidado> = apiCall { api.checkin(reservaId, convidadoId).toDomain() }
    override suspend fun incluirConvidado(reservaId: Long, nome: String): Outcome<Convidado> = apiCall { api.incluirConvidado(reservaId, IncluirConvidadoDto(nome.trim())).toDomain() }
    override suspend fun entregarChave(id: Long): Outcome<Reserva> = apiCall { api.entregarChave(id).toDomain() }
    override suspend fun desfazerChave(id: Long): Outcome<Reserva> = apiCall { api.desfazerChave(id).toDomain() }
    override suspend fun devolucao(id: Long, ok: Boolean, observacoes: String?): Outcome<Reserva> =
        apiCall { api.devolucao(id, DevolucaoDto(ok, observacoes?.trim()?.ifBlank { null })).toDomain() }
}
