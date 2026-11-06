package br.com.condovitta.app.core.notify

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import br.com.condovitta.app.MainActivity
import br.com.condovitta.app.R
import br.com.condovitta.app.data.local.SettingsRepository
import br.com.condovitta.app.data.remote.CondominioContext
import br.com.condovitta.app.domain.AuthRepository
import br.com.condovitta.app.domain.CategoriaNotificacao
import br.com.condovitta.app.domain.Notificacao
import br.com.condovitta.app.domain.NotificacoesRepository
import br.com.condovitta.app.domain.Outcome
import br.com.condovitta.app.domain.SessionState
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Central das notificações: o contador do sino e a exibição das novas na barra do Android. Sem FCM por enquanto: o app
 * consulta o backend a cada 30 s quando está aberto e a cada 15 min em segundo plano (WorkManager).
 */
@Singleton
class CentralNotificacoes @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repo: NotificacoesRepository,
    private val settings: SettingsRepository,
    private val auth: AuthRepository
) {
    /** como mostrar uma nova (os testes trocam; no app vai para a barra do Android) */
    internal var exibir: (Notificacao) -> Unit = { Exibidor.mostrar(context, it) }

    private val _naoLidas = MutableStateFlow(0L)
    val naoLidas: StateFlow<Long> = _naoLidas.asStateFlow()
    private val mutex = Mutex()

    /** Consulta o sino; as não lidas mais novas que a última já mostrada viram notificação do Android. */
    suspend fun sincronizar(): Outcome<Int> = mutex.withLock {
        val email = (auth.session.value as? SessionState.LoggedIn)?.email ?: return Outcome.Ok(0)
        when (val r = repo.listar(apenasNaoLidas = false, limite = 30)) {
            is Outcome.Err -> r
            is Outcome.Ok -> {
                _naoLidas.value = r.value.naoLidas
                val vista = settings.current().notifVista?.split('|')?.takeIf { it.size == 2 && it[0] == email }?.get(1)?.toLongOrNull()
                val maior = r.value.itens.maxOfOrNull { it.id } ?: 0L
                // primeira vez nesta conta: só marca o ponto de partida (não despeja o histórico na barra)
                val novas = if (vista == null) emptyList() else r.value.itens.filter { !it.lida && it.id > vista }
                novas.sortedBy { it.id }.forEach { exibir(it) }
                if (vista == null || maior > vista) settings.setNotifVista("$email|${maxOf(maior, vista ?: 0L)}")
                Outcome.Ok(novas.size)
            }
        }
    }

    fun lidaLocal(n: Long = 1) { _naoLidas.value = (_naoLidas.value - n).coerceAtLeast(0) }
    fun zerar() { _naoLidas.value = 0 }
}

/** Canais e exibição das notificações (um canal por assunto, para a pessoa silenciar o que quiser). */
object Exibidor {
    const val EXTRA_ROTA = "rota"
    const val EXTRA_CATEGORIA = "categoria"

    private data class Canal(val id: String, val nome: String, val importancia: Int)

    private fun canal(c: CategoriaNotificacao) = when (c) {
        CategoriaNotificacao.VISITANTES -> Canal("visitantes", "Visitantes e portaria", NotificationManager.IMPORTANCE_HIGH)
        CategoriaNotificacao.ENCOMENDAS -> Canal("encomendas", "Encomendas", NotificationManager.IMPORTANCE_HIGH)
        CategoriaNotificacao.FINANCEIRO -> Canal("financeiro", "Boletos e pagamentos", NotificationManager.IMPORTANCE_DEFAULT)
        CategoriaNotificacao.RESERVAS -> Canal("reservas", "Reservas", NotificationManager.IMPORTANCE_DEFAULT)
        CategoriaNotificacao.COMUNICADOS, CategoriaNotificacao.ASSEMBLEIAS -> Canal("avisos", "Avisos e assembleias", NotificationManager.IMPORTANCE_DEFAULT)
        else -> Canal("geral", "Outras notificações", NotificationManager.IMPORTANCE_DEFAULT)
    }

    fun criarCanais(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = context.getSystemService(NotificationManager::class.java) ?: return
        CategoriaNotificacao.entries.map(::canal).distinctBy { it.id }.forEach { nm.createNotificationChannel(NotificationChannel(it.id, it.nome, it.importancia)) }
    }

    fun podeMostrar(context: Context) = Build.VERSION.SDK_INT < 33 ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    fun mostrar(context: Context, n: Notificacao) {
        if (!podeMostrar(context)) return
        criarCanais(context)
        val intent = Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            .putExtra(EXTRA_ROTA, n.rota).putExtra(EXTRA_CATEGORIA, n.categoria.name)
        val pi = PendingIntent.getActivity(context, n.id.toInt(), intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val notification = NotificationCompat.Builder(context, canal(n.categoria).id)
            .setSmallIcon(R.drawable.ic_launcher_monochrome)
            .setColor(0xFF0A62B8.toInt())
            .setContentTitle(n.titulo)
            .setContentText(n.texto)
            .setStyle(NotificationCompat.BigTextStyle().bigText(n.texto))
            .setAutoCancel(true)
            .setContentIntent(pi)
            .setCategory(if (n.categoria == CategoriaNotificacao.VISITANTES) NotificationCompat.CATEGORY_MESSAGE else NotificationCompat.CATEGORY_EVENT)
            .build()
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return
        try {
            NotificationManagerCompat.from(context).notify(n.id.toInt(), notification)
        } catch (_: SecurityException) {
            // a pessoa tirou a permissão entre a checagem e o aviso: só não mostra
        }
    }
}

/** Consulta em segundo plano (mínimo do Android: a cada 15 min, só com internet). */
class NotificacoesWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface Deps {
        fun central(): CentralNotificacoes
        fun auth(): AuthRepository
        fun settings(): SettingsRepository
        fun condominio(): CondominioContext
    }

    override suspend fun doWork(): Result {
        val deps = EntryPointAccessors.fromApplication(applicationContext, Deps::class.java)
        val auth = deps.auth()
        auth.restore()
        if (auth.session.first { it !is SessionState.Unknown } !is SessionState.LoggedIn) return Result.success()
        // processo novo em segundo plano: o condomínio do perfil escolhido vai no cabeçalho
        if (deps.condominio().condominioId == null) {
            deps.settings().current().perfil?.substringAfter(':')?.toLongOrNull()?.let { deps.condominio().condominioId = it }
        }
        return if (deps.central().sincronizar() is Outcome.Ok) Result.success() else Result.retry()
    }

    companion object {
        private const val NOME = "notificacoes"

        fun agendar(context: Context) {
            val req = PeriodicWorkRequestBuilder<NotificacoesWorker>(15, TimeUnit.MINUTES)
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(NOME, ExistingPeriodicWorkPolicy.KEEP, req)
        }

        fun cancelar(context: Context) = WorkManager.getInstance(context).cancelUniqueWork(NOME)
    }
}
