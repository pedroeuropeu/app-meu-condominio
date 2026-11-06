package br.com.condovitta.app.feature.notificacoes

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Badge
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.Campaign
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.DoneAll
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material.icons.rounded.Gavel
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.NotificationsOff
import androidx.compose.material.icons.rounded.ReportProblem
import androidx.compose.material.icons.rounded.SupportAgent
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import br.com.condovitta.app.core.common.Fmt
import br.com.condovitta.app.core.designsystem.BackTopBar
import br.com.condovitta.app.core.designsystem.IconBubble
import br.com.condovitta.app.core.designsystem.ext
import br.com.condovitta.app.core.notify.CentralNotificacoes
import br.com.condovitta.app.core.notify.Exibidor
import br.com.condovitta.app.domain.CategoriaNotificacao
import br.com.condovitta.app.domain.Destino
import br.com.condovitta.app.domain.Notificacao
import br.com.condovitta.app.domain.Notificacoes
import br.com.condovitta.app.domain.NotificacoesRepository
import br.com.condovitta.app.domain.Outcome
import br.com.condovitta.app.domain.Rotas
import br.com.condovitta.app.feature.portaria.CardList
import br.com.condovitta.app.feature.portaria.EmptyBox
import br.com.condovitta.app.feature.portaria.ErrorBox
import br.com.condovitta.app.feature.portaria.Loadable
import br.com.condovitta.app.feature.portaria.LoadingBox
import br.com.condovitta.app.feature.portaria.NoteBox
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NotificacoesViewModel @Inject constructor(private val repo: NotificacoesRepository, private val central: CentralNotificacoes) : ViewModel() {
    private val _state = MutableStateFlow(Loadable<Notificacoes>())
    val state: StateFlow<Loadable<Notificacoes>> = _state.asStateFlow()

    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            _state.value = when (val r = repo.listar()) {
                is Outcome.Ok -> Loadable(r.value, loading = false)
                is Outcome.Err -> Loadable(_state.value.data, false, r.error.message)
            }
        }
    }

    /** abrir marca como lida (como na web) */
    fun abrir(n: Notificacao) {
        if (n.lida) return
        _state.update { s -> s.copy(data = s.data?.let { d -> d.copy(naoLidas = (d.naoLidas - 1).coerceAtLeast(0), itens = d.itens.map { if (it.id == n.id) it.copy(lida = true) else it }) }) }
        central.lidaLocal()
        viewModelScope.launch { repo.marcarLida(n.id) }
    }

    fun todasLidas() {
        _state.update { s -> s.copy(data = s.data?.let { d -> d.copy(naoLidas = 0, itens = d.itens.map { it.copy(lida = true) }) }) }
        central.zerar()
        viewModelScope.launch { repo.todasLidas() }
    }
}

fun CategoriaNotificacao.icone(): Pair<ImageVector, Color> = when (this) {
    CategoriaNotificacao.ENCOMENDAS -> Icons.Rounded.Inventory2 to Color(0xFFD97706)
    CategoriaNotificacao.VISITANTES -> Icons.Rounded.Badge to Color(0xFF1F4FE0)
    CategoriaNotificacao.RESERVAS -> Icons.Rounded.Event to Color(0xFF7C3AED)
    CategoriaNotificacao.ASSEMBLEIAS -> Icons.Rounded.Gavel to Color(0xFFDB2777)
    CategoriaNotificacao.COMUNICADOS -> Icons.Rounded.Campaign to Color(0xFF0D9488)
    CategoriaNotificacao.FINANCEIRO -> Icons.Rounded.AccountBalanceWallet to Color(0xFF059669)
    CategoriaNotificacao.CHAMADOS -> Icons.Rounded.SupportAgent to Color(0xFF0891B2)
    CategoriaNotificacao.OCORRENCIAS -> Icons.Rounded.ReportProblem to Color(0xFFDC2626)
    CategoriaNotificacao.DOCUMENTOS -> Icons.Rounded.Description to Color(0xFF64748B)
    CategoriaNotificacao.MANUTENCOES -> Icons.Rounded.Build to Color(0xFF0891B2)
    CategoriaNotificacao.OUTRAS -> Icons.Rounded.Notifications to Color(0xFF64748B)
}

/** A central do sino: abrir marca como lida e leva para a tela do assunto. */
@Composable
fun NotificacoesScreen(onBack: () -> Unit, onAbrir: (Destino) -> Unit, vm: NotificacoesViewModel = hiltViewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    LifecycleResumeEffect(Unit) {
        vm.refresh()
        onPauseOrDispose { }
    }
    Scaffold(
        topBar = {
            BackTopBar("Notificações", onBack = onBack, actions = {
                if ((state.data?.naoLidas ?: 0) > 0) IconButton(onClick = vm::todasLidas) { Icon(Icons.Rounded.DoneAll, contentDescription = "Marcar todas como lidas") }
            })
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { p ->
        val d = state.data
        PullToRefreshBox(isRefreshing = state.loading && d != null, onRefresh = vm::refresh, modifier = Modifier.padding(p).fillMaxSize()) {
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
                if (!Exibidor.podeMostrar(context)) {
                    NoteBox(Icons.Rounded.NotificationsOff, MaterialTheme.ext.warn, "As notificações do app estão desligadas no aparelho. Ligue em Configurações › Apps › Condovitta para receber os avisos na barra.")
                    Spacer(Modifier.height(10.dp))
                }
                when {
                    d == null && state.loading -> LoadingBox()
                    d == null -> ErrorBox(state.error ?: "Não deu para carregar.", onRetry = vm::refresh)
                    d.itens.isEmpty() -> EmptyBox(Icons.Rounded.Notifications, "Nenhuma notificação", "Encomendas, visitas, boletos e avisos aparecem aqui.")
                    else -> CardList(d.itens) { n ->
                        val (ic, cor) = n.categoria.icone()
                        Row(
                            Modifier.fillMaxWidth().clickable { vm.abrir(n); onAbrir(Rotas.destino(n.rota, n.categoria)) }.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            IconBubble(ic, cor)
                            Column(Modifier.weight(1f)) {
                                Text(n.titulo, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.ext.ink, fontWeight = if (n.lida) FontWeight.Medium else FontWeight.Bold)
                                n.texto?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.ext.muted) }
                                Text(Fmt.quando(n.criadoEm), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.ext.faint)
                            }
                            if (!n.lida) Box(Modifier.size(10.dp).background(MaterialTheme.colorScheme.primary, CircleShape))
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
                Text("Os avisos chegam em até 15 minutos com o app fechado e na hora com ele aberto.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.ext.faint)
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}
