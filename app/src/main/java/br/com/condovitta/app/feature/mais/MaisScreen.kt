package br.com.condovitta.app.feature.mais

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import br.com.condovitta.app.BuildConfig
import br.com.condovitta.app.core.designsystem.AppCard
import br.com.condovitta.app.core.designsystem.Avatar
import br.com.condovitta.app.core.designsystem.IconBubble
import br.com.condovitta.app.core.designsystem.MenuRow
import br.com.condovitta.app.core.designsystem.SectionHeader
import br.com.condovitta.app.core.designsystem.ext
import br.com.condovitta.app.data.local.ThemeMode
import br.com.condovitta.app.domain.Me
import br.com.condovitta.app.domain.Modo
import br.com.condovitta.app.domain.Perfil
import br.com.condovitta.app.ui.navigation.SoonCatalog

/** O que a tela Mais pode fazer com a sessão e as preferências (vem do SessionViewModel). */
data class MaisActions(
    val theme: ThemeMode,
    val onTheme: (ThemeMode) -> Unit,
    val lockEnabled: Boolean,
    val lockAvailable: Boolean,
    val onLock: (Boolean) -> Unit,
    val podeTrocarPerfil: Boolean,
    val onTrocarPerfil: () -> Unit,
    val onSair: () -> Unit,
    val onPassarTurno: () -> Unit
)

private val SERVICOS_PORTARIA = listOf("port/reservas", "port/livro", "port/gas", "port/veiculos", "port/bloqueados")
private val SERVICOS_MORADOR = listOf(
    "app/visitantes", "app/encomendas", "app/avisos", "app/documentos", "app/chamados", "app/gas",
    "app/assembleias", "app/prestacao", "app/ocorrencias", "app/unidade", "app/calendario"
)

@Composable
fun MaisScreen(me: Me, perfil: Perfil, actions: MaisActions, padding: PaddingValues, onSoon: (String) -> Unit) {
    val portaria = perfil.modo == Modo.PORTARIA
    Column(
        Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(bottom = padding.calculateBottomPadding())
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        Text("Mais", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.ext.ink, modifier = Modifier.padding(top = 16.dp, bottom = 6.dp))

        AppCard {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Avatar(me.iniciais, container = MaterialTheme.colorScheme.primaryContainer, content = MaterialTheme.colorScheme.onPrimaryContainer, size = 52.dp)
                Column(Modifier.weight(1f).padding(start = 14.dp)) {
                    Text(me.nome, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.ext.ink)
                    Text(me.email, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.ext.muted)
                    Text(
                        (if (portaria) "Portaria · " else listOfNotNull("Morador", perfil.unidade).joinToString(" · ") + " · ") + perfil.condominioNome,
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        SectionHeader(if (portaria) "Portaria" else "Serviços")
        AppCard {
            val keys = if (portaria) SERVICOS_PORTARIA else SERVICOS_MORADOR
            keys.forEachIndexed { i, key ->
                val info = SoonCatalog[key]
                MenuRow(info.icon, info.color, info.title, info.description, onClick = { onSoon(key) })
                if (i < keys.lastIndex) HorizontalDivider(color = MaterialTheme.ext.cardBorder, modifier = Modifier.padding(start = 72.dp))
            }
        }

        SectionHeader("Aparência")
        AppCard {
            Row(Modifier.padding(start = 14.dp, end = 14.dp, top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                IconBubble(Icons.Rounded.Palette, Color(0xFF7C3AED))
                Text("Tema", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(start = 14.dp))
            }
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(14.dp)) {
                val options = listOf(ThemeMode.SISTEMA to "Sistema", ThemeMode.CLARO to "Claro", ThemeMode.ESCURO to "Escuro")
                options.forEachIndexed { i, (mode, label) ->
                    SegmentedButton(
                        selected = actions.theme == mode,
                        onClick = { actions.onTheme(mode) },
                        shape = SegmentedButtonDefaults.itemShape(i, options.size),
                        colors = SegmentedButtonDefaults.colors(
                            activeContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            activeContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    ) { Text(label) }
                }
            }
        }

        if (!portaria) {
            SectionHeader("Segurança")
            AppCard {
                MenuRow(
                    Icons.Rounded.Fingerprint, Color(0xFF0D9488), "Bloquear com digital ou PIN",
                    if (actions.lockAvailable) "Pede a digital, o rosto ou o PIN do aparelho ao abrir o app."
                    else "Configure um bloqueio de tela no aparelho para usar.",
                    onClick = { if (actions.lockAvailable) actions.onLock(!actions.lockEnabled) },
                    trailing = { Switch(checked = actions.lockEnabled, onCheckedChange = actions.onLock, enabled = actions.lockAvailable) }
                )
            }
        }

        SectionHeader("Conta")
        AppCard {
            if (actions.podeTrocarPerfil) {
                MenuRow(Icons.Rounded.SwapHoriz, MaterialTheme.colorScheme.primary, "Trocar de perfil", "Portaria, morador ou outro condomínio", onClick = actions.onTrocarPerfil)
                HorizontalDivider(color = MaterialTheme.ext.cardBorder, modifier = Modifier.padding(start = 72.dp))
            }
            if (portaria) {
                Column(Modifier.testTag("passar_turno")) {
                    MenuRow(
                        Icons.AutoMirrored.Rounded.Logout, MaterialTheme.ext.warn, "Passar o turno",
                        "Sai da sua conta e deixa a portaria pronta para o próximo porteiro entrar.", onClick = actions.onPassarTurno
                    )
                }
            } else {
                Column(Modifier.testTag("sair")) {
                    MenuRow(Icons.AutoMirrored.Rounded.Logout, MaterialTheme.ext.danger, "Sair", null, onClick = actions.onSair)
                }
            }
        }

        Spacer(Modifier.height(20.dp))
        Text(
            "Condovitta ${BuildConfig.VERSION_NAME}\nTecnologia para viver melhor em condomínio",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.ext.faint, textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(24.dp))
    }
}
