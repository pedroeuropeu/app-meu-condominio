package br.com.condovitta.app.ui

import br.com.condovitta.app.domain.Outcome
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Um aviso (snackbar). [onAction] roda se a pessoa tocar na ação; [onDismiss] roda quando o aviso some sem a ação
 * (o tempo acabou, veio outro aviso por cima ou a tela foi fechada). Só um dos dois roda.
 */
class UiMessage(val text: String, val action: String? = null, val onAction: (() -> Unit)? = null, val onDismiss: (() -> Unit)? = null) {
    private val settled = AtomicBoolean(false)

    fun settle(acted: Boolean) {
        if (!settled.compareAndSet(false, true)) return
        if (acted) onAction?.invoke() else onDismiss?.invoke()
    }
}

/**
 * Avisos do app (sem toast: um snackbar por vez, o novo substitui o anterior). Sobrevivem à troca de tela: o que roda
 * depois do aviso (excluir de verdade, desfazer) usa o escopo do app, não o da tela.
 */
@Singleton
class Messenger @Inject constructor() {
    private val _messages = MutableSharedFlow<UiMessage>(extraBufferCapacity = 4, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val messages: SharedFlow<UiMessage> = _messages.asSharedFlow()
    private val scope by lazy { CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate) }

    fun show(text: String, action: String? = null, onAction: (() -> Unit)? = null) {
        _messages.tryEmit(UiMessage(text, action, onAction))
    }

    fun launch(block: suspend CoroutineScope.() -> Unit): Job = scope.launch(block = block)

    /**
     * Exclusão sem "tem certeza?": o item já saiu da tela e o aviso oferece "Desfazer". O servidor só recebe ([commit])
     * quando o aviso some sem "Desfazer"; se falhar, [undo] devolve o item e o erro aparece.
     */
    fun deferred(text: String, undo: () -> Unit, commit: suspend () -> Outcome<Unit>) {
        _messages.tryEmit(UiMessage(text, "Desfazer", onAction = undo, onDismiss = {
            scope.launch {
                val r = commit()
                if (r is Outcome.Err) { undo(); show(r.error.message) }
            }
        }))
    }

    /** Já fez no servidor; "Desfazer" chama [restore] e, dando certo, [onRestored] para a tela recarregar. */
    fun undoable(text: String, restore: suspend () -> Outcome<Unit>, onRestored: () -> Unit = {}) {
        show(text, "Desfazer") {
            scope.launch {
                when (val r = restore()) {
                    is Outcome.Ok -> onRestored()
                    is Outcome.Err -> show(r.error.message)
                }
            }
        }
    }
}
