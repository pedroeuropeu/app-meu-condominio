package br.com.condovitta.app.feature.portaria

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import br.com.condovitta.app.BuildConfig
import br.com.condovitta.app.core.designsystem.AppCard
import br.com.condovitta.app.core.designsystem.BackTopBar
import br.com.condovitta.app.core.designsystem.SectionHeader
import br.com.condovitta.app.core.designsystem.ext
import br.com.condovitta.app.domain.Me
import br.com.condovitta.app.domain.Perfil
import br.com.condovitta.app.feature.mais.MaisActions

/**
 * Menu da portaria: o resto da rotina (reservas, veículos, gás, bloqueados, notificações) e a conta (trocar de perfil,
 * passar o turno). Sem tema nem biometria: o aparelho da guarita é compartilhado e a tela é só de trabalho.
 */
@Composable
fun PortariaMenuScreen(me: Me, perfil: Perfil, mais: MaisActions, nav: PortariaNav) {
    Scaffold(topBar = { BackTopBar("Menu", onBack = nav.back) }, containerColor = MaterialTheme.colorScheme.background) { p ->
        Column(Modifier.padding(p).fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
            Text(
                "${me.nome} · ${perfil.condominioNome}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.ext.muted,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            SectionHeader("Rotina")
            AppCard {
                ItemMenu(Icons.Rounded.Event, "Reservas de hoje", "Convidados, check-in e chaves") { nav.soon("port/reservas") }
                Divisor()
                ItemMenu(Icons.Rounded.DirectionsCar, "Veículos e placas", "Consulta de placa e garagem") { nav.soon("port/veiculos") }
                Divisor()
                ItemMenu(Icons.Rounded.LocalFireDepartment, "Leitura de gás", "Medidor de cada unidade") { nav.soon("port/gas") }
                Divisor()
                ItemMenu(Icons.Rounded.Block, "Bloqueados", "Quem não pode entrar") { nav.bloqueados() }
                Divisor()
                ItemMenu(Icons.Rounded.Notifications, "Notificações", "Respostas dos moradores e avisos") { nav.notificacoes() }
            }
            SectionHeader("Conta")
            AppCard {
                if (mais.podeTrocarPerfil) {
                    ItemMenu(Icons.Rounded.SwapHoriz, "Trocar de perfil", "Ver o app do morador", onClick = mais.onTrocarPerfil)
                    Divisor()
                }
                Box(Modifier.testTag("passar_turno")) {
                    ItemMenu(
                        Icons.AutoMirrored.Rounded.Logout, "Passar o turno", "Sai da sua conta e deixa a portaria pronta para o próximo porteiro",
                        cor = MaterialTheme.ext.danger, onClick = mais.onPassarTurno
                    )
                }
            }
            Spacer(Modifier.height(20.dp))
            Text(
                "Condovitta ${BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.ext.faint,
                textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun Divisor() = HorizontalDivider(color = MaterialTheme.ext.cardBorder, modifier = Modifier.padding(start = 78.dp))

@Composable
private fun ItemMenu(icon: ImageVector, titulo: String, sub: String?, cor: Color = MaterialTheme.colorScheme.primary, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 72.dp).clickable(onClick = onClick).padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(Modifier.size(50.dp).clip(CircleShape).background(cor.copy(alpha = 0.12f)), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = cor, modifier = Modifier.size(26.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(titulo, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.ext.ink)
            if (sub != null) Text(sub, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.ext.muted)
        }
        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.ext.faint)
    }
}
