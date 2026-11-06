package br.com.condovitta.app.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.condovitta.app.data.local.SettingsRepository
import br.com.condovitta.app.domain.AppError
import br.com.condovitta.app.domain.AuthRepository
import br.com.condovitta.app.domain.Outcome
import br.com.condovitta.app.domain.SessionEnd
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val remember: Boolean = true,
    /** o servidor pediu o código do app autenticador (2FA) */
    val askTotp: Boolean = false,
    val totp: String = "",
    val loading: Boolean = false,
    /** erro no lugar do formulário (sem toast) */
    val error: String? = null,
    val forgotSent: Boolean = false,
    val forgotLoading: Boolean = false,
    val forgotError: String? = null
) {
    val canSubmit: Boolean get() = email.isNotBlank() && password.isNotBlank() && (!askTotp || totp.length >= 6) && !loading
}

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val auth: AuthRepository,
    private val settings: SettingsRepository
) : ViewModel() {

    private val _state = MutableStateFlow(LoginUiState())
    val state: StateFlow<LoginUiState> = _state.asStateFlow()

    init {
        reiniciar()
    }

    /**
     * Formulário limpo a cada vez que o login aparece (o ViewModel vive com a Activity e sobrevive ao logout). Abre com o
     * último e-mail, menos depois de "Passar o turno": aí fica vazio para o próximo porteiro.
     */
    fun reiniciar() {
        _state.value = LoginUiState()
        viewModelScope.launch {
            if (auth.lastEnd.value == SessionEnd.TURNO_PASSADO) return@launch
            val last = settings.current().lastEmail
            if (last != null) _state.update { if (it.email.isEmpty()) it.copy(email = last) else it }
        }
    }

    fun onEmail(v: String) = _state.update { it.copy(email = v.trim(), error = null, askTotp = false, totp = "") }
    fun onPassword(v: String) = _state.update { it.copy(password = v, error = null) }
    fun onRemember(v: Boolean) = _state.update { it.copy(remember = v) }
    fun onTotp(v: String) = _state.update { it.copy(totp = v.filter(Char::isDigit).take(6), error = null) }

    fun submit() {
        val s = _state.value
        if (!s.canSubmit) return
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            val r = auth.login(s.email, s.password, s.remember, s.totp.takeIf { s.askTotp })
            _state.update {
                when (r) {
                    is Outcome.Ok -> it.copy(loading = false, password = "", totp = "")
                    is Outcome.Err -> when (val e = r.error) {
                        // primeira vez: só pede o código, sem cara de erro
                        is AppError.OtpRequired -> it.copy(loading = false, askTotp = true, error = if (e.invalid || it.askTotp) e.message else null)
                        else -> it.copy(loading = false, error = e.message)
                    }
                }
            }
        }
    }

    fun forgot(email: String) {
        if (email.isBlank()) {
            _state.update { it.copy(forgotError = "Informe o seu e-mail.") }
            return
        }
        _state.update { it.copy(forgotLoading = true, forgotError = null) }
        viewModelScope.launch {
            val r = auth.forgotPassword(email)
            _state.update {
                when (r) {
                    is Outcome.Ok -> it.copy(forgotLoading = false, forgotSent = true)
                    is Outcome.Err -> it.copy(forgotLoading = false, forgotError = r.error.message)
                }
            }
        }
    }

    fun resetForgot() = _state.update { it.copy(forgotSent = false, forgotError = null, forgotLoading = false) }
}
