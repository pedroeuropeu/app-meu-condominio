package br.com.condovitta.app

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.fragment.app.FragmentActivity
import br.com.condovitta.app.core.security.LockCoordinator
import br.com.condovitta.app.core.security.canUseDeviceLock
import br.com.condovitta.app.core.security.showUnlockPrompt
import br.com.condovitta.app.ui.CondovittaRoot
import br.com.condovitta.app.ui.Messenger
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/** Atividade única. FragmentActivity porque o BiometricPrompt precisa dela. */
@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    @Inject lateinit var lockCoordinator: LockCoordinator
    @Inject lateinit var messenger: Messenger

    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // contra tapjacking: ignora toques quando outra janela cobre o app
        window.decorView.filterTouchesWhenObscured = true
        // no release, sem captura de tela nem miniatura nos recentes (dados de moradores, códigos de retirada)
        if (!BuildConfig.DEBUG) window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)

        lockCoordinator.reportDeviceLock(canUseDeviceLock(this))
        splash.setKeepOnScreenCondition { !lockCoordinator.ready.value }

        setContent {
            CondovittaRoot(
                messenger = messenger,
                locked = lockCoordinator.locked,
                deviceLockAvailable = { canUseDeviceLock(this) },
                onUnlock = { showUnlockPrompt(this, onSuccess = { lockCoordinator.unlock() }, onError = { messenger.show(it) }) }
            )
        }
    }
}
