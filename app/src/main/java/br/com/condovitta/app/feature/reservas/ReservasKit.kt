package br.com.condovitta.app.feature.reservas

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Celebration
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Pool
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.SportsSoccer
import androidx.compose.material.icons.rounded.SportsTennis
import androidx.compose.material.icons.rounded.Toys
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import br.com.condovitta.app.core.common.Fmt
import br.com.condovitta.app.core.designsystem.IconBubble
import br.com.condovitta.app.core.designsystem.StatusBadge
import br.com.condovitta.app.core.designsystem.ext
import br.com.condovitta.app.domain.Disponibilidade
import br.com.condovitta.app.domain.Reserva
import br.com.condovitta.app.domain.SituacaoDia
import br.com.condovitta.app.domain.StatusReserva
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

private val PT = Locale.forLanguageTag("pt-BR")

/** Ícone Material pelo nome que a web usa ("local_fire_department"). */
fun areaIcon(nome: String?): ImageVector = when (nome) {
    "local_fire_department", "outdoor_grill" -> Icons.Rounded.LocalFireDepartment
    "celebration", "party_mode" -> Icons.Rounded.Celebration
    "sports_soccer", "sports_basketball", "sports_volleyball", "sell" -> Icons.Rounded.SportsSoccer
    "sports_tennis" -> Icons.Rounded.SportsTennis
    "pool" -> Icons.Rounded.Pool
    "fitness_center" -> Icons.Rounded.FitnessCenter
    "restaurant", "kitchen", "dinner_dining" -> Icons.Rounded.Restaurant
    "toys", "child_care" -> Icons.Rounded.Toys
    else -> Icons.Rounded.Event
}

/** "#EA580C" → Color (cai no violeta da marca se vier vazio ou inválido). */
fun areaColor(hex: String?): Color = runCatching { Color(android.graphics.Color.parseColor(hex)) }.getOrDefault(Color(0xFF7C3AED))

/** "sáb., 14 de novembro" */
fun dataLonga(d: LocalDate): String = d.format(DateTimeFormatter.ofPattern("EEE, d 'de' MMMM", PT))

fun horario(r: Reserva): String {
    // horários com nome de hora ("18h – 19h") já dizem tudo
    if (r.turnoNome != null && Regex("\\d{1,2}h").containsMatchIn(r.turnoNome)) return r.turnoNome
    return listOfNotNull(r.turnoNome, if (r.inicio != null && r.fim != null) "${Fmt.hora(r.inicio)} – ${Fmt.hora(r.fim)}" else null).joinToString(" · ")
}

@Composable
fun StatusReservaBadge(s: StatusReserva) {
    val e = MaterialTheme.ext
    when (s) {
        StatusReserva.CONFIRMADA -> StatusBadge("Confirmada", e.success, e.successContainer, icon = Icons.Rounded.Check)
        StatusReserva.AGUARDANDO_APROVACAO -> StatusBadge("Aguardando aprovação", e.warn, e.warnContainer, icon = Icons.Rounded.Schedule)
        StatusReserva.RECUSADA -> StatusBadge("Recusada", e.danger, e.dangerContainer, icon = Icons.Rounded.Close)
        StatusReserva.CANCELADA -> StatusBadge("Cancelada", e.muted, MaterialTheme.colorScheme.surfaceContainer)
    }
}

@Composable
fun ReservaRow(r: Reserva, onClick: (() -> Unit)? = null, trailing: (@Composable () -> Unit)? = null) {
    val convidados = trailing == null
    Row(
        Modifier.fillMaxWidth().then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        IconBubble(areaIcon(r.areaIcone), areaColor(r.areaCor))
        Column(Modifier.weight(1f)) {
            Text(r.local, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.ext.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(dataLonga(r.data).replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.ext.muted)
            Text(horario(r) + if (convidados) " · ${r.convidados} convidados" else "", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.ext.muted)
        }
        if (trailing != null) trailing() else StatusReservaBadge(r.status)
    }
}

/** Cor do dia no calendário: verde livre, âmbar quase cheio, vermelho sem vaga, cinza fora do período. */
@Composable
private fun corDia(s: SituacaoDia): Pair<Color, Color> {
    val e = MaterialTheme.ext
    return when (s) {
        SituacaoDia.LIVRE -> e.successContainer to e.success
        SituacaoDia.PARCIAL -> e.warnContainer to e.warn
        SituacaoDia.LOTADO, SituacaoDia.OCUPADA, SituacaoDia.RESERVADO -> e.dangerContainer to e.danger
        else -> Color.Transparent to e.faint
    }
}

/**
 * Calendário do mês (segunda a domingo) com a disponibilidade de um espaço. Só os dias livres ou quase cheios são
 * tocáveis.
 */
@Composable
fun CalendarioMes(
    mes: YearMonth,
    disp: Disponibilidade?,
    itemId: Long?,
    selecionado: LocalDate?,
    podeVoltar: Boolean,
    onMes: (YearMonth) -> Unit,
    onDia: (LocalDate) -> Unit
) {
    Column(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { onMes(mes.minusMonths(1)) }, enabled = podeVoltar) { Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, contentDescription = "Mês anterior") }
            Text(
                mes.month.getDisplayName(TextStyle.FULL_STANDALONE, PT).replaceFirstChar { it.uppercase() } + " ${mes.year}",
                style = MaterialTheme.typography.titleMedium, color = MaterialTheme.ext.ink, textAlign = TextAlign.Center, modifier = Modifier.weight(1f)
            )
            IconButton(onClick = { onMes(mes.plusMonths(1)) }) { Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = "Próximo mês") }
        }
        Row(Modifier.fillMaxWidth()) {
            DayOfWeek.entries.forEach { d ->
                Text(d.getDisplayName(TextStyle.SHORT, PT).take(3).replace(".", ""), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.ext.muted,
                    textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
            }
        }
        Spacer(Modifier.height(4.dp))
        val vazios = mes.atDay(1).dayOfWeek.value - 1
        val celulas = List(vazios) { null } + (1..mes.lengthOfMonth()).map { mes.atDay(it) }
        celulas.chunked(7).forEach { semana ->
            Row(Modifier.fillMaxWidth()) {
                semana.forEach { d ->
                    Box(Modifier.weight(1f).aspectRatio(1f).padding(3.dp), contentAlignment = Alignment.Center) {
                        if (d != null) {
                            val s = disp?.dia(d)?.para(itemId) ?: SituacaoDia.FECHADO
                            val (fundo, texto) = corDia(s)
                            val sel = d == selecionado
                            Box(
                                Modifier.fillMaxWidth().aspectRatio(1f).clip(CircleShape)
                                    .background(if (sel) MaterialTheme.colorScheme.primary else fundo)
                                    .then(if (sel) Modifier else Modifier.border(0.dp, Color.Transparent, CircleShape))
                                    .clickable(enabled = s.reservavel) { onDia(d) }
                                    .testTag("dia_${d.dayOfMonth}"),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    d.dayOfMonth.toString(), style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (s.reservavel || sel) FontWeight.SemiBold else null,
                                    color = if (sel) MaterialTheme.colorScheme.onPrimary else texto
                                )
                            }
                        }
                    }
                }
                repeat(7 - semana.size) { Box(Modifier.weight(1f)) }
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.padding(horizontal = 6.dp)) {
            Legenda(MaterialTheme.ext.success, "Livre")
            Legenda(MaterialTheme.ext.warn, "Quase cheio")
            Legenda(MaterialTheme.ext.danger, "Sem vaga")
        }
    }
}

@Composable
private fun Legenda(cor: Color, texto: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(10.dp).clip(RoundedCornerShape(3.dp)).background(cor))
        Spacer(Modifier.size(5.dp))
        Text(texto, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.ext.muted)
    }
}
