package br.com.condovitta.app.feature.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Construction
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import br.com.condovitta.app.core.designsystem.Avatar
import br.com.condovitta.app.core.designsystem.Hero
import br.com.condovitta.app.core.designsystem.IconBubble
import br.com.condovitta.app.core.designsystem.ext

/** "Bom dia" até 11h59, "Boa tarde" até 17h59, depois "Boa noite". */
fun saudacao(hora: Int): String = when (hora) {
    in 5..11 -> "Bom dia"
    in 12..17 -> "Boa tarde"
    else -> "Boa noite"
}

/** Topo das telas iniciais: avatar, saudação, condomínio e o sino. */
@Composable
fun GreetingHero(initials: String, kicker: String, name: String, subtitle: String, onBell: () -> Unit, naoLidas: Long = 0, extra: @Composable () -> Unit = {}) {
    Hero {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Avatar(initials)
            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(kicker, style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.75f))
                Text("Olá, $name", style = MaterialTheme.typography.headlineSmall, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.75f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            BadgedBox(badge = { if (naoLidas > 0) Badge { Text(if (naoLidas > 9) "9+" else "$naoLidas") } }) {
                IconButton(onClick = onBell, modifier = Modifier.clip(CircleShape).background(Color.White.copy(alpha = 0.16f))) {
                    Icon(Icons.Rounded.Notifications, contentDescription = if (naoLidas > 0) "Notificações, $naoLidas não lidas" else "Notificações", tint = Color.White)
                }
            }
        }
        extra()
    }
}

data class QuickAction(val label: String, val icon: ImageVector, val color: Color, val onClick: () -> Unit)

/** Grade de ações de um toque (4 por linha). */
@Composable
fun QuickActions(actions: List<QuickAction>) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        actions.chunked(4).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { a ->
                    Surface(
                        onClick = a.onClick,
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.ext.card,
                        border = BorderStroke(1.dp, MaterialTheme.ext.cardBorder),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(Modifier.padding(vertical = 12.dp, horizontal = 4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            IconBubble(a.icon, a.color, size = 44.dp)
                            Spacer(Modifier.height(8.dp))
                            Text(a.label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.ext.ink, textAlign = TextAlign.Center, maxLines = 2, minLines = 2)
                        }
                    }
                }
                repeat(4 - row.size) { Box(Modifier.weight(1f)) }
            }
        }
    }
}

/** Aviso de que esta parte chega numa fase seguinte (honesto, sem números falsos). */
@Composable
fun PhaseNote(text: String) {
    Surface(color = MaterialTheme.ext.infoContainer, shape = MaterialTheme.shapes.medium, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(Icons.Rounded.Construction, contentDescription = null, tint = MaterialTheme.ext.info, modifier = Modifier.size(20.dp))
            Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}
