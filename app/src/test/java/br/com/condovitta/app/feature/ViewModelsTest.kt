package br.com.condovitta.app.feature

import br.com.condovitta.app.MainDispatcherRule
import br.com.condovitta.app.data.demo.DemoAuthRepository
import br.com.condovitta.app.data.demo.DemoContextRepository
import br.com.condovitta.app.data.local.SettingsRepository
import br.com.condovitta.app.data.remote.CondominioContext
import br.com.condovitta.app.domain.AppError
import br.com.condovitta.app.domain.AuthRepository
import br.com.condovitta.app.domain.Modo
import br.com.condovitta.app.domain.Outcome
import br.com.condovitta.app.domain.SessionEnd
import br.com.condovitta.app.domain.SessionState
import br.com.condovitta.app.feature.auth.LoginViewModel
import br.com.condovitta.app.tempDataStore
import br.com.condovitta.app.ui.ContextoState
import br.com.condovitta.app.ui.SessionViewModel
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ViewModelsTest {

    @get:Rule val main = MainDispatcherRule()

    private val settings = SettingsRepository(tempDataStore("settings-${System.nanoTime()}"))
    private val auth = DemoAuthRepository(settings)
    private val condominio = CondominioContext()

    private fun sessionVm() = SessionViewModel(auth, DemoContextRepository(auth), settings, condominio)

    @Test fun `porteiro loga e cai direto na portaria com o condominio no cabecalho`() = runTest {
        val vm = sessionVm()
        auth.restore()
        auth.login("portaria@condominio.local", "x", remember = true)

        val ready = vm.contexto.first { it is ContextoState.Ready } as ContextoState.Ready
        assertThat(ready.perfil?.modo).isEqualTo(Modo.PORTARIA)
        assertThat(condominio.condominioId).isEqualTo(1L)
    }

    @Test fun `sindico escolhe o modo e a escolha fica lembrada`() = runTest {
        val vm = sessionVm()
        auth.login("sindico@condominio.local", "x", remember = true)

        val ready = vm.contexto.first { it is ContextoState.Ready } as ContextoState.Ready
        assertThat(ready.perfil).isNull()
        assertThat(condominio.condominioId).isNull()

        vm.escolher(ready.disponiveis.first { it.modo == Modo.MORADOR })
        val escolhido = vm.contexto.first { (it as? ContextoState.Ready)?.perfil != null } as ContextoState.Ready
        assertThat(escolhido.perfil?.unidade).isEqualTo("A-102")
        assertThat(settings.settings.first { it.perfil != null }.perfil).isEqualTo("MORADOR:1")

        // abre de novo: entra direto no perfil lembrado
        vm.carregar()
        val reaberto = vm.contexto.first { (it as? ContextoState.Ready)?.perfil != null && it !== escolhido } as ContextoState.Ready
        assertThat(reaberto.perfil?.modo).isEqualTo(Modo.MORADOR)
    }

    @Test fun `passar o turno volta ao login e esquece o perfil`() = runTest {
        val vm = sessionVm()
        auth.login("portaria@condominio.local", "x", remember = true)
        vm.contexto.first { (it as? ContextoState.Ready)?.perfil != null }
        vm.sair(SessionEnd.TURNO_PASSADO)

        auth.session.first { it == SessionState.LoggedOut }
        assertThat(auth.lastEnd.value).isEqualTo(SessionEnd.TURNO_PASSADO)
        assertThat(settings.settings.first { it.perfil == null }.perfil).isNull()
    }

    @Test fun `login pede o codigo do autenticador sem cara de erro na primeira vez`() = runTest {
        val repo = mockk<AuthRepository>(relaxed = true)
        every { repo.session } returns MutableStateFlow(SessionState.LoggedOut)
        coEvery { repo.login(any(), any(), any(), null) } returns Outcome.Err(AppError.OtpRequired())
        coEvery { repo.login(any(), any(), any(), "123456") } returns Outcome.Err(AppError.OtpRequired("Código ou senha incorretos.", invalid = true))
        val vm = LoginViewModel(repo, settings)

        vm.onEmail("sindico@condominio.local")
        vm.onPassword("senha")
        vm.submit()
        advanceUntilIdle()
        assertThat(vm.state.value.askTotp).isTrue()
        assertThat(vm.state.value.error).isNull()
        assertThat(vm.state.value.canSubmit).isFalse()

        vm.onTotp("12a3456789")
        assertThat(vm.state.value.totp).isEqualTo("123456")
        vm.submit()
        advanceUntilIdle()
        assertThat(vm.state.value.error).isEqualTo("Código ou senha incorretos.")
    }

    @Test fun `login abre com o ultimo email`() = runTest {
        settings.setLastEmail("carlos@condominio.local")
        val vm = LoginViewModel(auth, settings)
        assertThat(vm.state.first { it.email.isNotEmpty() }.email).isEqualTo("carlos@condominio.local")
    }
}
