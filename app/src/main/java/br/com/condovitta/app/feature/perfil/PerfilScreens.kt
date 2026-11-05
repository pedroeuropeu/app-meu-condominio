package br.com.condovitta.app.feature.perfil

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.Apartment
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.NoAccounts
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import br.com.condovitta.app.core.designsystem.Brand
import br.com.condovitta.app.core.designsystem.Emblem
import br.com.condovitta.app.core.designsystem.IconBubble
import br.com.condovitta.app.core.designsystem.ext
import br.com.condovitta.app.domain.AppError
import br.com.condovitta.app.domain.Me
import br.com.condovitta.app.domain.Modo
import br.com.condovitta.app.domain.Perfil

/** "Como você quer entrar?": aparece quando a pessoa tem mais de um perfil (ex.: síndico que mora e opera a portaria). */
@Composable
fun EscolhaPerfilScreen(me: Me, disponiveis: List<Perfil>, onEscolher: (Perfil) -> Unit, onSair: () -> Unit) {
    val varios = disponiveis.map { it.condominioId }.distinct().size > 1
    Column(
        Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 24.dp)
    ) {
        Emblem(44.dp)
        Spacer(Modifier.height(22.dp))
        Text("Olá, ${me.primeiroNome}. Como você quer entrar?", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.ext.ink)
        Spacer(Modifier.height(6.dp))
        Text("Você tem mais de um papel no condomínio. Dá para trocar depois em Mais.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.ext.muted)
        Spacer(Modifier.height(20.dp))
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            disponiveis.forEach { p ->
                val portaria = p.modo == Modo.PORTARIA
                PerfilCard(
                    icon = if (portaria) Icons.Rounded.Shield else Icons.Rounded.Apartment,
                    color = if (portaria) Brand.Amber else Brand.Blue,
                    title = if (portaria) "Modo Portaria" else "App do morador",
                    description = buildString {
                        append(if (portaria) "Rápido para a guarita: encomendas, visitantes e liberação de entrada." else "Completo: reservas, boletos, encomendas, avisos e mais.")
                        if (varios || !portaria) append("\n").append(p.condominioNome).append(p.unidade?.let { " · $it" } ?: "")
                    },
                    tag = "perfil_${p.modo.name.lowercase()}_${p.condominioId}",
                    onClick = { onEscolher(p) }
                )
            }
        }
        Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = MaterialTheme.shapes.medium, modifier = Modifier.fillMaxWidth().padding(top = 20.dp)) {
            Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(Icons.Rounded.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                Text(
                    "No aparelho da guarita, use o Modo Portaria: ele não pede digital e tem \"Passar o turno\" para o próximo porteiro.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
        Spacer(Modifier.weight(1f))
        TextButton(onClick = onSair, modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 20.dp)) {
            Icon(Icons.AutoMirrored.Rounded.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.size(6.dp))
            Text("Sair")
        }
    }
}

@Composable
private fun PerfilCard(icon: ImageVector, color: Color, title: String, description: String, tag: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.ext.card,
        border = BorderStroke(1.dp, MaterialTheme.ext.cardBorder),
        modifier = Modifier.fillMaxWidth().testTag(tag)
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            IconBubble(icon, color, size = 50.dp)
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.ext.ink)
                Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.ext.muted)
            }
            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.ext.faint)
        }
    }
}

/** Carregando o contexto da pessoa (/me) depois do login. */
@Composable
fun CarregandoScreen() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Emblem(64.dp)
            Spacer(Modifier.height(24.dp))
            CircularProgressIndicator(Modifier.size(28.dp), strokeWidth = 3.dp)
        }
    }
}

/** Não deu para carregar o contexto (sem internet, servidor fora). */
@Composable
fun ContextoErroScreen(error: AppError, onRetry: () -> Unit, onSair: () -> Unit) {
    CenteredMessage(
        icon = if (error is AppError.Network) Icons.Rounded.CloudOff else Icons.Rounded.Info,
        color = MaterialTheme.ext.warn,
        title = if (error is AppError.Network) "Sem conexão" else "Não deu para abrir",
        text = error.message,
        primary = "Tentar de novo" to onRetry,
        secondary = "Sair" to onSair
    )
}

/** Logou, mas não tem papel que o app atenda (ex.: só funcionário, ou ainda não vinculado a uma unidade). */
@Composable
fun SemAcessoScreen(me: Me, onRetry: () -> Unit, onSair: () -> Unit) {
    CenteredMessage(
        icon = Icons.Rounded.NoAccounts,
        color = MaterialTheme.ext.muted,
        title = "Ainda sem acesso pelo app",
        text = "${me.primeiroNome}, sua conta ainda não está ligada a uma unidade nem à portaria de um condomínio. " +
            "Peça à administração do condomínio para vincular o seu e-mail (${me.email}).",
        primary = "Verificar de novo" to onRetry,
        secondary = "Sair" to onSair
    )
}

@Composable
private fun CenteredMessage(icon: ImageVector, color: Color, title: String, text: String, primary: Pair<String, () -> Unit>, secondary: Pair<String, () -> Unit>) {
    Column(
        Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        IconBubble(icon, color, size = 76.dp)
        Spacer(Modifier.height(18.dp))
        Text(title, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.ext.ink, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.ext.muted, textAlign = TextAlign.Center)
        Spacer(Modifier.height(24.dp))
        Button(onClick = primary.second, modifier = Modifier.fillMaxWidth().height(50.dp)) { Text(primary.first) }
        Spacer(Modifier.height(10.dp))
        OutlinedButton(onClick = secondary.second, modifier = Modifier.fillMaxWidth().height(50.dp)) {
            Icon(Icons.AutoMirrored.Rounded.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.size(6.dp))
            Text(secondary.first)
        }
    }
}
