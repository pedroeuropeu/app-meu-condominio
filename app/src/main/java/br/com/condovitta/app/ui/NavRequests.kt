package br.com.condovitta.app.ui

import br.com.condovitta.app.domain.CategoriaNotificacao
import br.com.condovitta.app.domain.Destino
import br.com.condovitta.app.domain.Rotas
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/** Tela que uma notificação tocada pediu para abrir; o modo em uso consome e navega. */
@Singleton
class NavRequests @Inject constructor() {
    private val _pendente = MutableStateFlow<Destino?>(null)
    val pendente: StateFlow<Destino?> = _pendente.asStateFlow()

    fun abrir(rota: String?, categoria: String?) {
        _pendente.value = Rotas.destino(rota, CategoriaNotificacao.of(categoria))
    }

    fun consumir(): Destino? = _pendente.value.also { _pendente.value = null }
}
