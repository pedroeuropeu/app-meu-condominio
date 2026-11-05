package br.com.condovitta.app.feature.reservas

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.condovitta.app.core.common.Fmt
import br.com.condovitta.app.core.designsystem.AppCard
import br.com.condovitta.app.core.designsystem.BackTopBar
import br.com.condovitta.app.core.designsystem.SectionHeader
import br.com.condovitta.app.core.designsystem.StatusBadge
import br.com.condovitta.app.core.designsystem.ext
import br.com.condovitta.app.domain.ChaveReserva
import br.com.condovitta.app.domain.Reserva
import br.com.condovitta.app.feature.portaria.EmptyBox
import br.com.condovitta.app.feature.portaria.ErrorBox
import br.com.condovitta.app.feature.portaria.InfoRows
import br.com.condovitta.app.feature.portaria.LoadingBox
import br.com.condovitta.app.feature.portaria.NoteBox

/** Reservas de hoje na portaria: lista de convidados (check-in) e a chave da área. */
@Composable
fun PortariaReservasScreen(onBack: () -> Unit, vm: PortariaReservasViewModel = hiltViewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    var checkin by rememberSaveable { mutableStateOf<Long?>(null) }
    var devolucao by rememberSaveable { mutableStateOf<Long?>(null) }
    LifecycleResumeEffect(Unit) {
        vm.refresh()
        onPauseOrDispose { }
    }
    Scaffold(topBar = { BackTopBar("Reservas de hoje", onBack = onBack) }, containerColor = MaterialTheme.colorScheme.background) { p ->
        val d = state.data
        PullToRefreshBox(isRefreshing = state.loading && d != null, onRefresh = vm::refresh, modifier = Modifier.padding(p).fillMaxSize()) {
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
                when {
                    d == null && state.loading -> LoadingBox()
                    d == null -> ErrorBox(state.error ?: "Não deu para carregar.", onRetry = vm::refresh)
                    else -> {
                        NoteBox(Icons.Rounded.Event, MaterialTheme.colorScheme.primary, "Áreas reservadas para hoje. Faça o check-in dos convidados e controle a chave na entrega.")
                        if (d.hoje.isEmpty()) EmptyBox(Icons.Rounded.Event, "Nenhuma reserva hoje", "As reservas confirmadas aparecem aqui no dia.")
                        d.hoje.forEach { r ->
                            Spacer(Modifier.height(12.dp))
                            ReservaPortariaCard(r, onCheckin = { checkin = r.id }, onChave = {
                                when (r.chave) {
                                    ChaveReserva.ENTREGUE -> devolucao = r.id
                                    ChaveReserva.DEVOLVIDA -> Unit
                                    else -> vm.entregarChave(r)
                                }
                            })
                        }
                        if (d.amanha.isNotEmpty()) {
                            SectionHeader("Amanhã")
                            AppCard { d.amanha.forEach { r -> ReservaRow(r) {} } }
                        }
                        Spacer(Modifier.height(32.dp))
                    }
                }
            }
        }
    }
    state.data?.hoje?.firstOrNull { it.id == checkin }?.let { r -> CheckinSheet(r, vm, onDismiss = { checkin = null }) }
    state.data?.hoje?.firstOrNull { it.id == devolucao }?.let { r -> DevolucaoSheet(r, onConfirmar = { ok, obs -> vm.devolucao(r, ok, obs); devolucao = null }, onDismiss = { devolucao = null }) }
}

@Composable
private fun ReservaPortariaCard(r: Reserva, onCheckin: () -> Unit, onChave: () -> Unit) {
    AppCard {
        ReservaRow(r, trailing = {
            StatusBadge("${r.convidados} convidados", MaterialTheme.ext.info, MaterialTheme.ext.infoContainer, icon = Icons.Rounded.Group)
        })
        Column(Modifier.padding(horizontal = 16.dp).padding(bottom = 14.dp)) {
            InfoRows(
                listOf(
                    "Unidade" to listOfNotNull(r.unidade, r.morador).joinToString(" · "),
                    "Lista" to if (r.lista.isEmpty()) "Sem lista (convidados livres)" else "${r.chegaram} de ${r.lista.size} chegaram",
                    "Chave" to when (r.chave) {
                        ChaveReserva.ENTREGUE -> "Entregue ${Fmt.hora(r.chaveEntregueEm)}"
                        ChaveReserva.DEVOLVIDA -> "Devolvida ${Fmt.hora(r.chaveDevolvidaEm)}"
                        else -> "Na portaria"
                    }
                )
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FilledTonalButton(onClick = onCheckin, modifier = Modifier.weight(1f).testTag("checkin_${r.id}")) {
                    Icon(Icons.Rounded.Group, contentDescription = null, modifier = Modifier.size(18.dp)); Spacer(Modifier.size(6.dp)); Text("Check-in")
                }
                OutlinedButton(onClick = onChave, enabled = r.chave != ChaveReserva.DEVOLVIDA, modifier = Modifier.testTag("chave_${r.id}")) {
                    Icon(Icons.Rounded.Key, contentDescription = null, modifier = Modifier.size(18.dp)); Spacer(Modifier.size(6.dp))
                    Text(when (r.chave) { ChaveReserva.ENTREGUE -> "Devolução"; ChaveReserva.DEVOLVIDA -> "Devolvida"; else -> "Entregar chave" })
                }
            }
        }
    }
}

@Composable
private fun CheckinSheet(r: Reserva, vm: PortariaReservasViewModel, onDismiss: () -> Unit) {
    var nome by rememberSaveable { mutableStateOf("") }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 20.dp).navigationBarsPadding().imePadding().verticalScroll(rememberScrollState())) {
            Text("Check-in · ${r.local}", style = MaterialTheme.typography.titleLarge)
            Text("${r.unidade ?: ""} · ${r.chegaram} de ${r.lista.size} chegaram", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.ext.muted)
            Spacer(Modifier.height(10.dp))
            if (r.lista.isEmpty()) Text("O morador não enviou lista. Registre quem chegar como convidado extra.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.ext.muted)
            r.lista.forEach { c ->
                Row(Modifier.fillMaxWidth().clickable { vm.checkin(r, c.id) }.padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(if (c.chegouEm != null) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked, contentDescription = null,
                        tint = if (c.chegouEm != null) MaterialTheme.ext.success else MaterialTheme.ext.faint)
                    Spacer(Modifier.size(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(c.nome, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.ext.ink)
                        Text(listOfNotNull(if (c.extra) "Extra" else null, c.chegouEm?.let { "chegou ${Fmt.hora(it)}" }).joinToString(" · ").ifEmpty { "Toque quando chegar" },
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.ext.muted)
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(nome, { nome = it }, label = { Text("Convidado fora da lista") }, singleLine = true, modifier = Modifier.weight(1f))
                Button(onClick = { vm.incluir(r, nome); nome = "" }, enabled = nome.isNotBlank()) { Icon(Icons.Rounded.PersonAdd, contentDescription = "Incluir") }
            }
        }
    }
}

@Composable
private fun DevolucaoSheet(r: Reserva, onConfirmar: (Boolean, String?) -> Unit, onDismiss: () -> Unit) {
    var ok by rememberSaveable { mutableStateOf(true) }
    var obs by rememberSaveable { mutableStateOf("") }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 20.dp).navigationBarsPadding().imePadding()) {
            Text("Devolução da chave", style = MaterialTheme.typography.titleLarge)
            Text("${r.local} · ${r.unidade ?: ""}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.ext.muted)
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Área em ordem (vistoria ok)", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                Switch(ok, { ok = it })
            }
            if (!ok) OutlinedTextField(obs, { obs = it }, label = { Text("O que encontrou?") }, minLines = 2, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(14.dp))
            Button(onClick = { onConfirmar(ok, obs) }, enabled = ok || obs.isNotBlank(), modifier = Modifier.fillMaxWidth().height(50.dp)) { Text("Registrar devolução") }
        }
    }
}
