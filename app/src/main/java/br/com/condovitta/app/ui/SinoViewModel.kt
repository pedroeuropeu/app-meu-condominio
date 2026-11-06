package br.com.condovitta.app.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import br.com.condovitta.app.core.notify.CentralNotificacoes
import br.com.condovitta.app.core.notify.NotificacoesWorker
import br.com.condovitta.app.data.local.SettingsRepository
import br.com.condovitta.app.domain.Destino
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

/** O sino do app inteiro: contador, consulta periódica e a tela que uma notificação tocada pediu. */
@HiltViewModel
class SinoViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val central: CentralNotificacoes,
    private val nav: NavRequests,
    private val settings: SettingsRepository
) : ViewModel() {
    val naoLidas: StateFlow<Long> = central.naoLidas
    val destino: StateFlow<Destino?> = nav.pendente

    suspend fun sincronizar() = central.sincronizar()
    fun consumirDestino() = nav.consumir()

    /** entrou num modo: agenda a consulta em segundo plano (a cada 15 min) */
    fun ativar() = NotificacoesWorker.agendar(context)

    /** saiu da conta: para a consulta e zera o sino */
    fun desativar() {
        NotificacoesWorker.cancelar(context)
        central.zerar()
    }

    suspend fun jaPediuPermissao() = settings.current().pediuNotificacao
    suspend fun marcarPediuPermissao() = settings.setPediuNotificacao()
}
