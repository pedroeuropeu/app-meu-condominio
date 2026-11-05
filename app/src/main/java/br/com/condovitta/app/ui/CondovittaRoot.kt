package br.com.condovitta.app.ui

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.condovitta.app.core.designsystem.CondovittaTheme
import br.com.condovitta.app.core.designsystem.Emblem
import br.com.condovitta.app.core.designsystem.ext
import br.com.condovitta.app.data.local.ThemeMode
import br.com.condovitta.app.domain.Modo
import br.com.condovitta.app.domain.SessionEnd
import br.com.condovitta.app.domain.SessionState
import br.com.condovitta.app.feature.auth.LoginScreen
import br.com.condovitta.app.feature.mais.MaisActions
import br.com.condovitta.app.feature.morador.MoradorApp
import br.com.condovitta.app.feature.perfil.CarregandoScreen
import br.com.condovitta.app.feature.perfil.ContextoErroScreen
import br.com.condovitta.app.feature.perfil.EscolhaPerfilScreen
import br.com.condovitta.app.feature.perfil.SemAcessoScreen
import br.com.condovitta.app.feature.portaria.PortariaApp
import kotlinx.coroutines.flow.StateFlow

/**
 * Raiz: decide o que aparece pela sessão e pelo contexto — login, carregando, "Como você quer entrar?", sem acesso, ou um
 * dos dois modos (Portaria em âmbar, Morador nas cores da marca). Os avisos (snackbar) ficam por cima de tudo.
 */
@Composable
fun CondovittaRoot(
    messenger: Messenger,
    locked: StateFlow<Boolean>,
    deviceLockAvailable: () -> Boolean,
    onUnlock: () -> Unit,
    vm: SessionViewModel = hiltViewModel()
) {
    val session by vm.session.collectAsStateWithLifecycle()
    val contexto by vm.contexto.collectAsStateWithLifecycle()
    val prefs by vm.prefs.collectAsStateWithLifecycle()
    val lastEnd by vm.lastEnd.collectAsStateWithLifecycle()
    val isLocked by locked.collectAsStateWithLifecycle()

    val dark = when (prefs.theme) {
        ThemeMode.SISTEMA -> isSystemInDarkTheme()
        ThemeMode.CLARO -> false
        ThemeMode.ESCURO -> true
    }
    val ready = contexto as? ContextoState.Ready
    val portaria = session is SessionState.LoggedIn && ready?.perfil?.modo == Modo.PORTARIA

    CondovittaTheme(dark = dark, portaria = portaria) {
        // ícones da barra de status seguem o tema do app (não o do aparelho); o topo em degradê troca para branco
        val view = LocalView.current
        DisposableEffect(dark) {
            (view.context as? Activity)?.window?.let { WindowCompat.getInsetsController(it, view).isAppearanceLightStatusBars = !dark }
            onDispose { }
        }
        val snackbar = remember { SnackbarHostState() }
        LaunchedEffect(messenger) {
            messenger.messages.collect { m ->
                snackbar.currentSnackbarData?.dismiss()
                val r = snackbar.showSnackbar(m.text, m.action, withDismissAction = false, duration = if (m.action != null) SnackbarDuration.Long else SnackbarDuration.Short)
                m.settle(r == SnackbarResult.ActionPerformed)
            }
        }

        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Box(Modifier.fillMaxSize()) {
                val screen = when {
                    session is SessionState.Unknown -> "unknown"
                    session is SessionState.LoggedOut -> "login"
                    contexto is ContextoState.Loading -> "loading"
                    contexto is ContextoState.Error -> "error"
                    ready != null && ready.perfil == null && ready.disponiveis.isEmpty() -> "sem-acesso"
                    ready != null && ready.perfil == null -> "escolha"
                    else -> "app:${ready?.perfil?.chave}"
                }
                AnimatedContent(targetState = screen, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "root") { s ->
                    when {
                        s == "unknown" -> Box(Modifier.fillMaxSize())
                        s == "login" -> LoginScreen(dark = dark, lastEnd = lastEnd)
                        s == "loading" -> CarregandoScreen()
                        s == "error" -> (contexto as? ContextoState.Error)?.let { ContextoErroScreen(it.error, onRetry = vm::carregar, onSair = { vm.sair() }) }
                        s == "sem-acesso" -> ready?.let { SemAcessoScreen(it.me, onRetry = vm::carregar, onSair = { vm.sair() }) }
                        s == "escolha" -> ready?.let { EscolhaPerfilScreen(it.me, it.disponiveis, onEscolher = vm::escolher, onSair = { vm.sair() }) }
                        else -> {
                            val r = ready ?: return@AnimatedContent
                            val perfil = r.perfil ?: return@AnimatedContent
                            val actions = MaisActions(
                                theme = prefs.theme, onTheme = { vm.setTheme(it) },
                                lockEnabled = prefs.lockEnabled, lockAvailable = deviceLockAvailable(), onLock = { vm.setLock(it) },
                                podeTrocarPerfil = r.disponiveis.size > 1, onTrocarPerfil = vm::trocarPerfil,
                                onSair = { vm.sair() },
                                onPassarTurno = { vm.sair(SessionEnd.TURNO_PASSADO) }
                            )
                            if (perfil.modo == Modo.PORTARIA) PortariaApp(r.me, perfil, actions) else MoradorApp(r.me, perfil, actions, messenger)
                        }
                    }
                }

                if (isLocked && session is SessionState.LoggedIn) LockOverlay(onUnlock)

                SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 80.dp)) { data ->
                    Snackbar(data, actionColor = MaterialTheme.ext.accent)
                }
            }
        }
    }
}

/** Tela de bloqueio (digital/rosto/PIN). Abre o pedido sozinho e deixa um botão para tentar de novo. */
@Composable
private fun LockOverlay(onUnlock: () -> Unit) {
    LaunchedEffect(Unit) { onUnlock() }
    BackHandler { /* bloqueado: não volta para o app por baixo */ }
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxSize().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center) {
            Emblem(64.dp)
            Spacer(Modifier.height(24.dp))
            Text("Condovitta bloqueado", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.ext.ink)
            Spacer(Modifier.height(6.dp))
            Text("Use a digital, o rosto ou o PIN do aparelho.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.ext.muted)
            Spacer(Modifier.height(24.dp))
            Button(onClick = onUnlock) {
                Icon(Icons.Rounded.Lock, contentDescription = null)
                Spacer(Modifier.padding(4.dp))
                Text("Desbloquear")
            }
        }
    }
}
