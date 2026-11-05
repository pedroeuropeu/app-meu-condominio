package br.com.condovitta.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.condovitta.app.data.local.Settings
import br.com.condovitta.app.data.local.SettingsRepository
import br.com.condovitta.app.data.local.ThemeMode
import br.com.condovitta.app.data.remote.CondominioContext
import br.com.condovitta.app.domain.AppError
import br.com.condovitta.app.domain.AuthRepository
import br.com.condovitta.app.domain.ContextRepository
import br.com.condovitta.app.domain.Me
import br.com.condovitta.app.domain.Outcome
import br.com.condovitta.app.domain.Perfil
import br.com.condovitta.app.domain.Perfis
import br.com.condovitta.app.domain.SessionEnd
import br.com.condovitta.app.domain.SessionState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Onde a pessoa está depois de logar: carregando o /me, erro, ou pronta (com ou sem perfil escolhido). */
sealed interface ContextoState {
    data object Loading : ContextoState
    data class Error(val error: AppError) : ContextoState
    data class Ready(val me: Me, val disponiveis: List<Perfil>, val perfil: Perfil?) : ContextoState
}

/**
 * Sessão e contexto do app inteiro: quem está logado (/api/v1/me), quais perfis tem e qual está em uso. O condomínio do
 * perfil escolhido passa a ir no X-Condominio-Id de todas as chamadas.
 */
@HiltViewModel
class SessionViewModel @Inject constructor(
    private val auth: AuthRepository,
    private val contextRepo: ContextRepository,
    private val settings: SettingsRepository,
    private val condominio: CondominioContext
) : ViewModel() {

    val session: StateFlow<SessionState> = auth.session
    val lastEnd: StateFlow<SessionEnd?> = auth.lastEnd
    val prefs: StateFlow<Settings> = settings.settings.stateIn(viewModelScope, SharingStarted.Eagerly, Settings())

    private val _contexto = MutableStateFlow<ContextoState>(ContextoState.Loading)
    val contexto: StateFlow<ContextoState> = _contexto.asStateFlow()

    init {
        viewModelScope.launch {
            auth.session.collect { s ->
                when (s) {
                    is SessionState.LoggedIn -> carregar()
                    else -> _contexto.value = ContextoState.Loading
                }
            }
        }
    }

    fun carregar() {
        viewModelScope.launch {
            _contexto.value = ContextoState.Loading
            // o /me vale para todos os condomínios: vai sem cabeçalho
            condominio.condominioId = null
            when (val r = contextRepo.me()) {
                is Outcome.Err -> _contexto.value = ContextoState.Error(r.error)
                is Outcome.Ok -> {
                    val disponiveis = Perfis.disponiveis(r.value)
                    val perfil = Perfis.escolher(disponiveis, settings.current().perfil)
                    aplicar(perfil)
                    _contexto.value = ContextoState.Ready(r.value, disponiveis, perfil)
                }
            }
        }
    }

    fun escolher(perfil: Perfil) {
        val atual = _contexto.value as? ContextoState.Ready ?: return
        viewModelScope.launch {
            settings.setPerfil(perfil.chave)
            aplicar(perfil)
            _contexto.value = atual.copy(perfil = perfil)
        }
    }

    /** "Trocar de perfil" no Mais: volta para a escolha. */
    fun trocarPerfil() {
        val atual = _contexto.value as? ContextoState.Ready ?: return
        viewModelScope.launch {
            settings.setPerfil(null)
            condominio.condominioId = null
            _contexto.value = atual.copy(perfil = null)
        }
    }

    fun sair(reason: SessionEnd? = null) {
        viewModelScope.launch {
            // passar o turno esquece o perfil (o próximo porteiro escolhe o dele); sair também
            settings.setPerfil(null)
            auth.logout(reason)
        }
    }

    fun setTheme(mode: ThemeMode) = viewModelScope.launch { settings.setTheme(mode) }
    fun setLock(enabled: Boolean) = viewModelScope.launch { settings.setLockEnabled(enabled) }

    private fun aplicar(perfil: Perfil?) {
        condominio.condominioId = perfil?.condominioId
    }
}
