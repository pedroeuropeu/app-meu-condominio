package br.com.condovitta.app.feature.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.MarkEmailRead
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.condovitta.app.BuildConfig
import br.com.condovitta.app.core.designsystem.BrandLogo
import br.com.condovitta.app.core.designsystem.StatusBadge
import br.com.condovitta.app.core.designsystem.ext
import br.com.condovitta.app.domain.SessionEnd

/** Entrar: e-mail ou CPF e senha (com o código do autenticador quando a conta tem 2FA). */
@Composable
fun LoginScreen(dark: Boolean, lastEnd: SessionEnd?, vm: LoginViewModel = hiltViewModel()) {
    val s by vm.state.collectAsStateWithLifecycle()
    val focus = LocalFocusManager.current
    var showPass by rememberSaveable { mutableStateOf(false) }
    var forgotOpen by rememberSaveable { mutableStateOf(false) }

    Column(
        Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            BrandLogo(dark, Modifier.height(44.dp))
            Spacer(Modifier.weight(1f))
            if (BuildConfig.ENV_LABEL.isNotEmpty()) StatusBadge(BuildConfig.ENV_LABEL, MaterialTheme.ext.warn, MaterialTheme.ext.warnContainer)
        }
        Spacer(Modifier.height(36.dp))
        Text("Bem-vindo de volta", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.ext.ink)
        Spacer(Modifier.height(6.dp))
        Text("Entre com seu e-mail ou CPF e a senha para acessar o seu condomínio.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.ext.muted)

        when (lastEnd) {
            SessionEnd.TURNO_PASSADO -> Notice(Icons.Rounded.Info, MaterialTheme.ext.info, "Turno passado. Quem assume a portaria entra com o próprio e-mail e senha.")
            SessionEnd.EXPIRADA -> Notice(Icons.Rounded.Info, MaterialTheme.ext.warn, "Sua sessão terminou. Entre de novo para continuar.")
            null -> Unit
        }
        if (BuildConfig.DEMO) {
            Notice(
                Icons.Rounded.Info, MaterialTheme.ext.accent,
                "Demonstração: qualquer senha entra. Com \"portaria\" no e-mail você entra como porteiro; com \"sindico\", escolhe o modo."
            )
        }

        Spacer(Modifier.height(22.dp))
        OutlinedTextField(
            value = s.email, onValueChange = vm::onEmail,
            label = { Text("E-mail ou CPF") }, singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
            modifier = Modifier.fillMaxWidth().testTag("login_email"),
            enabled = !s.loading
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = s.password, onValueChange = vm::onPassword,
            label = { Text("Senha") }, singleLine = true,
            visualTransformation = if (showPass) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = { showPass = !showPass }) {
                    Icon(if (showPass) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility, contentDescription = if (showPass) "Esconder a senha" else "Mostrar a senha")
                }
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = if (s.askTotp) ImeAction.Next else ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { focus.clearFocus(); vm.submit() }),
            modifier = Modifier.fillMaxWidth().testTag("login_password"),
            enabled = !s.loading
        )
        if (s.askTotp) {
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = s.totp, onValueChange = vm::onTotp,
                label = { Text("Código do app autenticador") }, singleLine = true,
                supportingText = { Text("Os 6 números que aparecem no seu app (Google Authenticator, Authy…).") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { focus.clearFocus(); vm.submit() }),
                modifier = Modifier.fillMaxWidth(),
                enabled = !s.loading
            )
        }
        Row(Modifier.fillMaxWidth().padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = s.remember, onCheckedChange = vm::onRemember)
            Text("Lembrar de mim", style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.weight(1f))
            TextButton(onClick = { vm.resetForgot(); forgotOpen = true }) { Text("Esqueci a senha") }
        }
        if (s.error != null) {
            Notice(Icons.Rounded.ErrorOutline, MaterialTheme.colorScheme.error, s.error!!)
        }
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = { focus.clearFocus(); vm.submit() },
            enabled = s.canSubmit,
            modifier = Modifier.fillMaxWidth().height(54.dp).testTag("login_submit"),
            shape = MaterialTheme.shapes.medium
        ) {
            if (s.loading) CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.5.dp, color = MaterialTheme.colorScheme.onPrimary)
            else Text("Entrar", style = MaterialTheme.typography.titleMedium)
        }
        Spacer(Modifier.weight(1f))
        Spacer(Modifier.height(28.dp))
        Text(
            "O acesso é criado pela administração do condomínio. Não recebeu o convite? Fale com a portaria ou o síndico.",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.ext.muted, textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }

    if (forgotOpen) ForgotSheet(initialEmail = s.email, state = s, onSend = vm::forgot, onClose = { forgotOpen = false })
}

@Composable
private fun Notice(icon: ImageVector, color: Color, text: String) {
    Surface(color = color.copy(alpha = 0.10f), shape = MaterialTheme.shapes.medium, modifier = Modifier.fillMaxWidth().padding(top = 14.dp)) {
        Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
            Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
private fun ForgotSheet(initialEmail: String, state: LoginUiState, onSend: (String) -> Unit, onClose: () -> Unit) {
    var email by rememberSaveable { mutableStateOf(initialEmail) }
    ModalBottomSheet(onDismissRequest = onClose, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.padding(horizontal = 24.dp).padding(bottom = 24.dp).navigationBarsPadding().imePadding()) {
            if (state.forgotSent) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.MarkEmailRead, contentDescription = null, tint = MaterialTheme.ext.success)
                    Spacer(Modifier.width(10.dp))
                    Text("Confira o seu e-mail", style = MaterialTheme.typography.titleLarge)
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    "Se houver uma conta com $email, mandamos o link para criar uma nova senha. Ele vale por pouco tempo.",
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.ext.muted
                )
                Spacer(Modifier.height(20.dp))
                Button(onClick = onClose, modifier = Modifier.fillMaxWidth().height(50.dp)) { Text("Entendi") }
            } else {
                Text("Esqueci a senha", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(6.dp))
                Text("Mandamos um link para você criar uma nova senha.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.ext.muted)
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value = email, onValueChange = { email = it.trim() }, label = { Text("E-mail") }, singleLine = true,
                    isError = state.forgotError != null,
                    supportingText = state.forgotError?.let { { Text(it) } },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = { onSend(email) }),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(16.dp))
                Button(onClick = { onSend(email) }, enabled = !state.forgotLoading, modifier = Modifier.fillMaxWidth().height(50.dp)) {
                    if (state.forgotLoading) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                    else Text("Enviar link", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}
