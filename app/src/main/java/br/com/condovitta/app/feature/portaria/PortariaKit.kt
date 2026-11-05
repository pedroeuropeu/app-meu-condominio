package br.com.condovitta.app.feature.portaria

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.Pending
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Handyman
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.SensorOccupied
import androidx.compose.material.icons.rounded.ThumbUp
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.condovitta.app.core.common.Fmt
import br.com.condovitta.app.core.designsystem.AppCard
import br.com.condovitta.app.core.designsystem.IconBubble
import br.com.condovitta.app.core.designsystem.StatusBadge
import br.com.condovitta.app.core.designsystem.ext
import br.com.condovitta.app.domain.Encomenda
import br.com.condovitta.app.domain.Portaria
import br.com.condovitta.app.domain.StatusVisita
import br.com.condovitta.app.domain.TipoVisitante
import br.com.condovitta.app.domain.Visita

/* Peças das telas da portaria (as mesmas do protótipo: visRow, encRow, badge, placa, kpi). */

data class Loadable<T>(val data: T? = null, val loading: Boolean = true, val error: String? = null)

val TipoVisitante.icon: ImageVector
    get() = when (this) {
        TipoVisitante.VISITA -> Icons.Rounded.Person
        TipoVisitante.PRESTADOR -> Icons.Rounded.Handyman
        TipoVisitante.ENTREGA -> Icons.Rounded.Inventory2
        TipoVisitante.APP -> Icons.Rounded.DirectionsCar
    }

val TipoVisitante.color: Color
    get() = when (this) {
        TipoVisitante.VISITA -> Color(0xFF1F4FE0)
        TipoVisitante.PRESTADOR -> Color(0xFFD97706)
        TipoVisitante.ENTREGA -> Color(0xFF0891B2)
        TipoVisitante.APP -> Color(0xFF7C3AED)
    }

@Composable
fun VisitaStatusBadge(status: StatusVisita) {
    val e = MaterialTheme.ext
    when (status) {
        StatusVisita.ESPERADO -> StatusBadge("Esperado", e.info, e.infoContainer, icon = Icons.Rounded.Schedule)
        StatusVisita.AGUARDANDO -> StatusBadge("Aguardando morador", e.warn, e.warnContainer, icon = Icons.Rounded.Pending)
        StatusVisita.LIBERADO -> StatusBadge("Liberado pelo morador", e.success, e.successContainer, icon = Icons.Rounded.ThumbUp)
        StatusVisita.DENTRO -> StatusBadge("No condomínio", e.success, e.successContainer, icon = Icons.Rounded.SensorOccupied)
        StatusVisita.SAIU -> StatusBadge("Já saiu", e.muted, MaterialTheme.colorScheme.surfaceContainer, icon = Icons.AutoMirrored.Rounded.Logout)
        StatusVisita.RECUSADO -> StatusBadge("Recusado", e.danger, e.dangerContainer, icon = Icons.Rounded.Close)
    }
}

/** Uma linha de visitante: tipo, nome, unidade e a situação com o horário. */
@Composable
fun VisitaRow(v: Visita, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        IconBubble(v.tipo.icon, v.tipo.color)
        Column(Modifier.weight(1f)) {
            Text(v.nome, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.ext.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                listOfNotNull(v.unidade, v.tipo.label, v.obs).joinToString(" · "),
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.ext.muted, maxLines = 1, overflow = TextOverflow.Ellipsis
            )
        }
        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            VisitaStatusBadge(v.status)
            Text(
                when (v.status) {
                    StatusVisita.DENTRO -> "entrou ${Fmt.hora(v.entradaEm)}"
                    StatusVisita.SAIU -> "saiu ${Fmt.hora(v.saidaEm)}"
                    else -> v.janela ?: ""
                },
                style = MaterialTheme.typography.labelSmall, color = MaterialTheme.ext.muted
            )
        }
    }
}

/** Uma linha de encomenda: unidade, destinatário, transportadora, local e situação. */
@Composable
fun EncomendaRow(e: Encomenda, onClick: () -> Unit) {
    val atrasada = Portaria.atrasada(e)
    val ext = MaterialTheme.ext
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        IconBubble(
            if (e.perecivel) Icons.Rounded.WaterDrop else Icons.Rounded.Inventory2,
            when {
                e.perecivel -> Color(0xFF0891B2)
                atrasada -> Color(0xFFDC2626)
                else -> Color(0xFFD97706)
            }
        )
        Column(Modifier.weight(1f)) {
            Text("${e.unidade} · ${e.destinatario ?: "Morador"}", style = MaterialTheme.typography.titleSmall, color = ext.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                listOfNotNull(e.transportadora, e.tamanho?.label, e.local).joinToString(" · "),
                style = MaterialTheme.typography.bodySmall, color = ext.muted, maxLines = 1, overflow = TextOverflow.Ellipsis
            )
        }
        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            when {
                e.retirada -> StatusBadge("Retirada", ext.success, ext.successContainer, icon = Icons.Rounded.Check)
                atrasada -> StatusBadge("${e.avisos} avisos", ext.danger, ext.dangerContainer)
                e.perecivel -> StatusBadge("Perecível", ext.info, ext.infoContainer)
                else -> StatusBadge("Aguardando", ext.warn, ext.warnContainer)
            }
            Text(Fmt.quando(if (e.retirada) e.retiradaEm else e.recebidaEm), style = MaterialTheme.typography.labelSmall, color = ext.muted)
        }
    }
}

/** Lista dentro de um cartão, com divisórias. */
@Composable
fun <T> CardList(items: List<T>, row: @Composable (T) -> Unit) {
    AppCard {
        items.forEachIndexed { i, item ->
            row(item)
            if (i < items.lastIndex) HorizontalDivider(color = MaterialTheme.ext.cardBorder, modifier = Modifier.padding(start = 70.dp))
        }
    }
}

/** Placa no estilo Mercosul (faixa azul "BRASIL"). */
@Composable
fun Plate(placa: String, modifier: Modifier = Modifier) {
    Column(
        modifier
            .clip(RoundedCornerShape(6.dp))
            .border(1.5.dp, Color(0xFF1E293B), RoundedCornerShape(6.dp))
            .background(Color.White)
            .width(IntrinsicSize.Max)
            .widthIn(min = 92.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(Modifier.fillMaxWidth().background(Color(0xFF1D4ED8)).padding(vertical = 1.dp), contentAlignment = Alignment.Center) {
            Text("BRASIL", color = Color.White, fontSize = 7.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        }
        Text(
            Portaria.formatarPlaca(placa) ?: placa, color = Color(0xFF0F172A), fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold, fontSize = 15.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
        )
    }
}

/** Número do turno (Esperados, No condomínio, Encomendas). */
@Composable
fun Kpi(icon: ImageVector, color: Color, value: String, label: String, modifier: Modifier = Modifier, onClick: (() -> Unit)? = null) {
    Surface(
        onClick = onClick ?: {}, enabled = onClick != null,
        shape = MaterialTheme.shapes.medium, color = MaterialTheme.ext.card,
        border = BorderStroke(1.dp, MaterialTheme.ext.cardBorder), modifier = modifier
    ) {
        Column(Modifier.padding(12.dp)) {
            IconBubble(icon, color, size = 34.dp)
            Spacer(Modifier.height(8.dp))
            Text(value, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.ext.ink)
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.ext.muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

/** Linhas "chave: valor" do cartão de detalhe. */
@Composable
fun InfoRows(rows: List<Pair<String, String?>>) {
    Column(Modifier.padding(top = 8.dp)) {
        rows.filter { !it.second.isNullOrBlank() }.forEach { (k, v) ->
            Row(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                Text(k, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.ext.muted, modifier = Modifier.width(120.dp))
                Text(v!!, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.ext.ink, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
            }
        }
    }
}

/** Filtros em chips (Esperados / No condomínio / Histórico). */
@Composable
fun <T> FilterChips(options: List<Pair<T, String>>, selected: T, onSelect: (T) -> Unit, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { (value, label) ->
            FilterChip(
                selected = value == selected, onClick = { onSelect(value) }, label = { Text(label) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    }
}

@Composable
fun EmptyBox(icon: ImageVector, title: String, text: String, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth().padding(vertical = 40.dp, horizontal = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        IconBubble(icon, MaterialTheme.ext.muted, size = 60.dp)
        Spacer(Modifier.height(12.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.ext.ink)
        Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.ext.muted, textAlign = TextAlign.Center)
    }
}

@Composable
fun LoadingBox(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(Modifier.size(32.dp)) }
}

@Composable
fun ErrorBox(message: String, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        IconBubble(Icons.Rounded.CloudOff, MaterialTheme.ext.warn, size = 60.dp)
        Spacer(Modifier.height(12.dp))
        Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.ext.muted, textAlign = TextAlign.Center)
        Spacer(Modifier.height(14.dp))
        Button(onClick = onRetry) { Text("Tentar de novo") }
    }
}

/** Faixa de aviso (nota do protótipo). */
@Composable
fun NoteBox(icon: ImageVector, color: Color, text: String, modifier: Modifier = Modifier) {
    Surface(color = color.copy(alpha = 0.12f), shape = MaterialTheme.shapes.medium, modifier = modifier.fillMaxWidth()) {
        Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
            Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}
