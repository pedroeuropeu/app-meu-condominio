package br.com.condovitta.app.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

enum class ThemeMode { SISTEMA, CLARO, ESCURO }

/** Preferências deste aparelho (não são da conta). */
data class Settings(
    val theme: ThemeMode = ThemeMode.SISTEMA,
    /** bloqueio por digital/rosto/PIN na abertura e ao voltar do segundo plano (só no modo morador) */
    val lockEnabled: Boolean = false,
    val lockTimeoutMinutes: Int = 2,
    /** perfil escolhido em "Como você quer entrar?" ([br.com.condovitta.app.domain.Perfil.chave]) */
    val perfil: String? = null,
    /** último e-mail que entrou (o login já abre preenchido; útil na troca de porteiro) */
    val lastEmail: String? = null,
    /** "email|id": a última notificação já mostrada para esta conta (para não repetir) */
    val notifVista: String? = null,
    /** já pediu a permissão de notificação (Android 13+) */
    val pediuNotificacao: Boolean = false
)

@Singleton
class SettingsRepository @Inject constructor(@Named("settings") private val dataStore: DataStore<Preferences>) {

    val settings: Flow<Settings> = dataStore.data.map { p ->
        Settings(
            theme = p[THEME]?.let { v -> ThemeMode.entries.firstOrNull { it.name == v } } ?: ThemeMode.SISTEMA,
            lockEnabled = p[LOCK] ?: false,
            lockTimeoutMinutes = p[LOCK_TIMEOUT] ?: 2,
            perfil = p[PERFIL],
            lastEmail = p[LAST_EMAIL],
            notifVista = p[NOTIF_VISTA],
            pediuNotificacao = p[PEDIU_NOTIF] ?: false
        )
    }

    suspend fun current(): Settings = settings.first()

    suspend fun setTheme(mode: ThemeMode) = dataStore.edit { it[THEME] = mode.name }
    suspend fun setLockEnabled(enabled: Boolean) = dataStore.edit { it[LOCK] = enabled }
    suspend fun setLockTimeout(minutes: Int) = dataStore.edit { it[LOCK_TIMEOUT] = minutes }
    suspend fun setPerfil(chave: String?) = dataStore.edit { if (chave == null) it.remove(PERFIL) else it[PERFIL] = chave }
    suspend fun setLastEmail(email: String) = dataStore.edit { it[LAST_EMAIL] = email }
    suspend fun setNotifVista(valor: String) = dataStore.edit { it[NOTIF_VISTA] = valor }
    suspend fun setPediuNotificacao() = dataStore.edit { it[PEDIU_NOTIF] = true }

    private companion object {
        val THEME = stringPreferencesKey("theme")
        val LOCK = booleanPreferencesKey("lock_enabled")
        val LOCK_TIMEOUT = intPreferencesKey("lock_timeout")
        val PERFIL = stringPreferencesKey("perfil")
        val LAST_EMAIL = stringPreferencesKey("last_email")
        val NOTIF_VISTA = stringPreferencesKey("notif_vista")
        val PEDIU_NOTIF = booleanPreferencesKey("pediu_notificacao")
    }
}
