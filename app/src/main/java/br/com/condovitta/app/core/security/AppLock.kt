package br.com.condovitta.app.core.security

import android.os.Build
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import br.com.condovitta.app.core.common.TimeProvider
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Bloqueio do app: ao abrir com sessão salva, ou ao voltar depois do tempo escolhido em segundo plano,
 * o app esconde tudo até a pessoa confirmar a digital, o rosto ou o PIN do aparelho.
 */
@Singleton
class AppLockManager @Inject constructor(private val time: TimeProvider) {

    private val _locked = MutableStateFlow(false)
    val locked: StateFlow<Boolean> = _locked.asStateFlow()

    private var backgroundedAt: Long? = null

    /** Chamado na abertura do app (processo novo). */
    fun onColdStart(loggedIn: Boolean, lockEnabled: Boolean) {
        _locked.value = loggedIn && lockEnabled
    }

    fun onBackground() {
        backgroundedAt = time.epochMillis()
    }

    fun onForeground(loggedIn: Boolean, lockEnabled: Boolean, timeoutMinutes: Int) {
        val since = backgroundedAt ?: return
        backgroundedAt = null
        if (loggedIn && lockEnabled && time.epochMillis() - since >= timeoutMinutes * 60_000L) _locked.value = true
    }

    fun lockNow() {
        _locked.value = true
    }

    fun unlock() {
        _locked.value = false
    }
}

/** Autenticadores aceitos: biometria forte ou o bloqueio de tela do aparelho (PIN, padrão, senha). */
fun lockAuthenticators(): Int =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) BIOMETRIC_STRONG or DEVICE_CREDENTIAL else BIOMETRIC_WEAK or DEVICE_CREDENTIAL

/** O aparelho tem biometria ou bloqueio de tela configurado? Sem isso o bloqueio do app não pode ser ligado. */
fun canUseDeviceLock(activity: FragmentActivity): Boolean =
    BiometricManager.from(activity).canAuthenticate(lockAuthenticators()) == BiometricManager.BIOMETRIC_SUCCESS

/** Mostra o diálogo do sistema (BiometricPrompt). */
fun showUnlockPrompt(activity: FragmentActivity, onSuccess: () -> Unit, onError: (String) -> Unit) {
    val prompt = BiometricPrompt(activity, ContextCompat.getMainExecutor(activity), object : BiometricPrompt.AuthenticationCallback() {
        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) = onSuccess()
        override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
            if (errorCode != BiometricPrompt.ERROR_USER_CANCELED && errorCode != BiometricPrompt.ERROR_CANCELED) onError(errString.toString())
        }
    })
    val info = BiometricPrompt.PromptInfo.Builder()
        .setTitle("Desbloquear o Condovitta")
        .setSubtitle("Use a digital, o rosto ou o bloqueio de tela")
        .setAllowedAuthenticators(lockAuthenticators())
        .build()
    prompt.authenticate(info)
}
