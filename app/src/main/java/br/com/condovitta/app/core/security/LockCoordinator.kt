package br.com.condovitta.app.core.security

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import br.com.condovitta.app.data.local.SettingsRepository
import br.com.condovitta.app.di.AppScope
import br.com.condovitta.app.domain.AuthRepository
import br.com.condovitta.app.domain.Modo
import br.com.condovitta.app.domain.SessionState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Liga o bloqueio do app ao ciclo de vida do processo: decide se abre bloqueado e bloqueia de novo quando o app volta do
 * segundo plano depois do tempo configurado. Na Portaria nunca bloqueia: o aparelho da guarita é compartilhado e a troca
 * de pessoa é pela "Passar o turno".
 */
@Singleton
class LockCoordinator @Inject constructor(
    private val lock: AppLockManager,
    private val auth: AuthRepository,
    private val settings: SettingsRepository,
    @AppScope private val scope: CoroutineScope
) : DefaultLifecycleObserver {

    private val deviceLock = MutableStateFlow<Boolean?>(null)
    private val _ready = MutableStateFlow(false)

    /** A abertura (splash) fica na tela até a sessão e o bloqueio estarem resolvidos, para não piscar telas. */
    val ready: StateFlow<Boolean> = _ready.asStateFlow()
    val locked: StateFlow<Boolean> get() = lock.locked
    val deviceLockAvailable: Boolean get() = deviceLock.value == true

    fun start() {
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
        scope.launch {
            auth.restore()
            val session = auth.session.first { it !is SessionState.Unknown }
            val available = deviceLock.filterNotNull().first()
            lock.onColdStart(session is SessionState.LoggedIn, lockApplies() && available)
            _ready.value = true
        }
        // saiu da conta (inclusive quando o servidor encerra a sessão): o bloqueio não fica por cima do login
        scope.launch { auth.session.collect { if (it is SessionState.LoggedOut) lock.unlock() } }
    }

    fun reportDeviceLock(available: Boolean) {
        deviceLock.value = available
    }

    fun unlock() = lock.unlock()

    override fun onStop(owner: LifecycleOwner) = lock.onBackground()

    override fun onStart(owner: LifecycleOwner) {
        scope.launch {
            lock.onForeground(auth.session.value is SessionState.LoggedIn, lockApplies() && deviceLockAvailable, settings.current().lockTimeoutMinutes)
        }
    }

    private suspend fun lockApplies(): Boolean {
        val s = settings.current()
        return s.lockEnabled && s.perfil?.startsWith(Modo.PORTARIA.name) != true
    }
}
