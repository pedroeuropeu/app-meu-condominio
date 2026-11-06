package br.com.condovitta.app.data.repository

import br.com.condovitta.app.data.remote.NotificacoesApi
import br.com.condovitta.app.data.remote.apiCall
import br.com.condovitta.app.data.remote.apiUnit
import br.com.condovitta.app.data.remote.toDomain
import br.com.condovitta.app.domain.Notificacoes
import br.com.condovitta.app.domain.NotificacoesRepository
import br.com.condovitta.app.domain.Outcome
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RemoteNotificacoesRepository @Inject constructor(private val api: NotificacoesApi) : NotificacoesRepository {
    override suspend fun listar(apenasNaoLidas: Boolean, limite: Int): Outcome<Notificacoes> = apiCall { api.listar(limite, apenasNaoLidas).toDomain() }
    override suspend fun marcarLida(id: Long): Outcome<Unit> = apiUnit { api.lida(id) }
    override suspend fun todasLidas(): Outcome<Unit> = apiUnit { api.todasLidas() }
}
