package br.com.condovitta.app

import android.app.Application
import br.com.condovitta.app.core.security.LockCoordinator
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber
import javax.inject.Inject

@HiltAndroidApp
class CondovittaApp : Application() {

    @Inject lateinit var lockCoordinator: LockCoordinator

    override fun onCreate() {
        super.onCreate()
        // logs só no debug: o release não registra nada (e o R8 remove as chamadas de Log)
        if (BuildConfig.DEBUG) Timber.plant(Timber.DebugTree())
        lockCoordinator.start()
    }
}
