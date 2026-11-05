package br.com.condovitta.app.feature.reservas

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.selection.toggleable
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.EventAvailable
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.condovitta.app.core.common.Fmt
import br.com.condovitta.app.core.designsystem.AppCard
import br.com.condovitta.app.core.designsystem.BackTopBar
import br.com.condovitta.app.core.designsystem.IconBubble
import br.com.condovitta.app.core.designsystem.SectionHeader
import br.com.condovitta.app.core.designsystem.SignaturePad
import br.com.condovitta.app.core.designsystem.ext
import br.com.condovitta.app.core.designsystem.rememberSignatureState
import br.com.condovitta.app.domain.AreaComum
import br.com.condovitta.app.domain.ModoAssinatura
import br.com.condovitta.app.domain.Reserva
import br.com.condovitta.app.domain.StatusReserva
import br.com.condovitta.app.feature.portaria.BusyOr
import br.com.condovitta.app.feature.portaria.CardList
import br.com.condovitta.app.feature.portaria.EmptyBox
import br.com.condovitta.app.feature.portaria.ErrorBox
import br.com.condovitta.app.feature.portaria.FieldLabel
import br.com.condovitta.app.feature.portaria.InfoRows
import br.com.condovitta.app.feature.portaria.LoadingBox
import br.com.condovitta.app.feature.portaria.NoteBox
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.math.BigDecimal

private fun taxaTexto(v: BigDecimal?) = if (v == null || v.signum() == 0) "Sem taxa" else Fmt.money(v)

/* ------------------------------------------------------------------ aba Reservas (morador) */

@Composable
fun ReservasScreen(padding: PaddingValues, onReservar: (Long) -> Unit, vm: ReservasViewModel = hiltViewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    var aberta by rememberSaveable { mutableStateOf<Long?>(null) }
    LifecycleResumeEffect(Unit) {
        vm.refresh()
        onPauseOrDispose { }
    }
    Scaffold(
        topBar = { TopAppBar(title = { Text("Reservas", style = MaterialTheme.typography.titleLarge) }, colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)) },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = Modifier.padding(bottom = padding.calculateBottomPadding())
    ) { p ->
        val d = state.data
        PullToRefreshBox(isRefreshing = state.loading && d != null, onRefresh = vm::refresh, modifier = Modifier.padding(p).fillMaxSize()) {
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
                when {
                    d == null && state.loading -> LoadingBox()
                    d == null -> ErrorBox(state.error ?: "Não deu para carregar.", onRetry = vm::refresh)
                    else -> {
                        val proximas = d.minhas.filter { it.ativa }
                        SectionHeader("Minhas reservas")
                        if (proximas.isEmpty()) EmptyBox(Icons.Rounded.EventAvailable, "Nenhuma reserva", "Escolha uma área abaixo para reservar.")
                        else CardList(proximas) { r -> ReservaRow(r, onClick = { aberta = r.id }) }
                        SectionHeader("Áreas comuns", subtitle = "Toque para reservar")
                        d.areas.forEach { a -> AreaCard(a) { onReservar(a.id) }; Spacer(Modifier.height(10.dp)) }
                        val antigas = d.minhas.filter { !it.ativa }
                        if (antigas.isNotEmpty()) {
                            SectionHeader("Canceladas e recusadas")
                            CardList(antigas) { r -> ReservaRow(r, onClick = { aberta = r.id }) }
                        }
                        Spacer(Modifier.height(32.dp))
                    }
                }
            }
        }
    }
    state.data?.minhas?.firstOrNull { it.id == aberta }?.let { r -> ReservaSheet(r, onCancelar = { aberta = null; vm.cancelar(r) }, onDismiss = { aberta = null }) }
}

@Composable
private fun AreaCard(a: AreaComum, onClick: () -> Unit) {
    Surface(onClick = onClick, shape = MaterialTheme.shapes.large, color = MaterialTheme.ext.card, border = BorderStroke(1.dp, MaterialTheme.ext.cardBorder),
        modifier = Modifier.fillMaxWidth().testTag("area_${a.id}")) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            IconBubble(areaIcon(a.icone), areaColor(a.cor), size = 50.dp)
            Column(Modifier.weight(1f)) {
                Text(a.nome, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.ext.ink)
                a.descricao?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.ext.muted) }
                Text(
                    listOfNotNull(if (a.itens.size > 1) "${a.itens.size} espaços" else null, taxaTexto(a.taxa).let { if (a.taxa?.signum() == 1) "a partir de $it" else it },
                        if (a.exigeAprovacao) "precisa de aprovação" else null).joinToString(" · "),
                    style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun ReservaSheet(r: Reserva, onCancelar: () -> Unit, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(horizontal = 22.dp).padding(bottom = 24.dp).navigationBarsPadding()) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                IconBubble(areaIcon(r.areaIcone), areaColor(r.areaCor), size = 50.dp)
                Column(Modifier.weight(1f)) {
                    Text(r.local, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.ext.ink)
                    StatusReservaBadge(r.status)
                }
            }
            InfoRows(
                listOf(
                    "Data" to dataLonga(r.data).replaceFirstChar { it.uppercase() }, "Horário" to horario(r), "Convidados" to "${r.convidados}",
                    "Taxa" to taxaTexto(r.taxa), "Protocolo" to r.protocolo,
                    "Cancelar sem custo até" to r.cancelamentoGratisAte?.let { Fmt.quando(it) }, "Motivo" to r.motivoDecisao
                )
            )
            if (r.lista.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text("Lista de convidados (${r.lista.size})", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.ext.muted)
                Text(r.lista.joinToString(", ") { it.nome }, style = MaterialTheme.typography.bodyMedium)
            }
            if (r.ativa) {
                Spacer(Modifier.height(18.dp))
                OutlinedButton(onClick = onCancelar, modifier = Modifier.fillMaxWidth().height(48.dp).testTag("reserva_cancelar")) {
                    Text("Cancelar reserva", color = MaterialTheme.ext.danger)
                }
            }
        }
    }
}

/* ------------------------------------------------------------------ assistente de reserva */

@Composable
fun NovaReservaScreen(onBack: () -> Unit, onConcluir: () -> Unit, vm: NovaReservaViewModel = hiltViewModel()) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    val assinatura = rememberSignatureState()
    val scope = rememberCoroutineScope()
    val voltar = { if (!vm.voltar()) onBack() }
    BackHandler(enabled = ui.criada == null) { voltar() }

    ui.criada?.let { r -> Concluida(r, onConcluir); return }
    val a = ui.area
    val titulos = listOf("Escolha o espaço", "Escolha o dia", "Horário e convidados", if (a?.exigeTermo == true) "Termo de uso" else "Confirmar")

    Scaffold(
        topBar = { BackTopBar(a?.nome ?: "Reservar", onBack = voltar) },
        bottomBar = {
            Surface(color = MaterialTheme.colorScheme.surfaceContainerLowest, shadowElevation = 8.dp) {
                Row(Modifier.fillMaxWidth().navigationBarsPadding().imePadding().padding(horizontal = 16.dp, vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (ui.passo > 0) OutlinedButton(onClick = { vm.voltar() }, modifier = Modifier.height(52.dp)) { Text("Voltar") }
                    Button(
                        onClick = {
                            if (ui.passo < 3) vm.avancar()
                            else scope.launch { vm.confirmar(withContext(Dispatchers.Default) { assinatura.toPng() }) }
                        },
                        enabled = ui.podeAvancar() && !ui.salvando, modifier = Modifier.weight(1f).height(52.dp).testTag("reserva_avancar")
                    ) {
                        BusyOr(ui.salvando) {
                            if (ui.passo == 3) { Icon(Icons.Rounded.Check, contentDescription = null); Spacer(Modifier.size(8.dp)) }
                            Text(when (ui.passo) { 3 -> if (a?.exigeAprovacao == true) "Pedir reserva" else "Confirmar reserva"; else -> "Continuar" })
                        }
                    }
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { p ->
        Column(Modifier.padding(p).fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
            if (a == null) {
                if (ui.erro != null) ErrorBox(ui.erro!!, onRetry = onBack) else LoadingBox()
                return@Column
            }
            LinearProgressIndicator(progress = { (ui.passo + 1) / 4f }, modifier = Modifier.fillMaxWidth().padding(top = 4.dp))
            Text("Passo ${ui.passo + 1} de 4", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.ext.muted, modifier = Modifier.padding(top = 8.dp))
            Text(titulos[ui.passo], style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.ext.ink, modifier = Modifier.padding(bottom = 12.dp))
            when (ui.passo) {
                0 -> PassoEspaco(ui, vm)
                1 -> PassoData(ui, vm)
                2 -> PassoHorario(ui, vm)
                else -> PassoTermo(ui, vm, assinatura)
            }
            ui.erro?.let { Spacer(Modifier.height(12.dp)); NoteBox(Icons.Rounded.ErrorOutline, MaterialTheme.colorScheme.error, it) }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun PassoEspaco(ui: AssistenteUi, vm: NovaReservaViewModel) {
    val a = ui.area!!
    a.descricao?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.ext.muted); Spacer(Modifier.height(10.dp)) }
    a.itens.forEach { i ->
        val sel = ui.itemId == i.id
        Surface(
            onClick = { vm.item(i.id) }, enabled = i.disponivel, shape = MaterialTheme.shapes.large,
            color = if (sel) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.ext.card,
            border = BorderStroke(if (sel) 2.dp else 1.dp, if (sel) MaterialTheme.colorScheme.primary else MaterialTheme.ext.cardBorder),
            modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp).testTag("item_${i.id}")
        ) {
            Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                IconBubble(if (i.disponivel) areaIcon(a.icone) else Icons.Rounded.Build, if (i.disponivel) areaColor(a.cor) else MaterialTheme.ext.muted)
                Column(Modifier.weight(1f)) {
                    Text(listOfNotNull(i.nome, i.apelido).joinToString(" · "), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.ext.ink)
                    Text(listOfNotNull(i.capacidade?.let { "até $it pessoas" }, taxaTexto(i.taxa)).joinToString(" · "), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.ext.muted)
                    if (i.recursos.isNotEmpty()) Text(i.recursos.joinToString(" · "), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.ext.muted)
                    i.manutencao?.let { Text("Em manutenção: $it", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.ext.warn) }
                }
                if (sel) Icon(Icons.Rounded.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
private fun PassoData(ui: AssistenteUi, vm: NovaReservaViewModel) {
    AppCard {
        Box(Modifier.padding(10.dp)) {
            CalendarioMes(
                mes = ui.mes, disp = ui.disp?.takeIf { it.mes == ui.mes }, itemId = ui.itemId, selecionado = ui.data,
                podeVoltar = ui.mes.isAfter(java.time.YearMonth.from(vm.hoje)), onMes = vm::carregarMes, onDia = vm::dia
            )
            if (ui.carregandoDisp) CircularProgressIndicator(Modifier.size(22.dp).align(Alignment.TopEnd), strokeWidth = 2.dp)
        }
    }
    Spacer(Modifier.height(10.dp))
    val a = ui.area!!
    Text(
        "Reservas de ${a.antecedenciaMinDias} a ${a.antecedenciaMaxDias} dias de antecedência." + (a.cancelamentoGratisHoras?.let { " Cancelamento sem custo até $it h antes." } ?: ""),
        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.ext.muted
    )
    ui.data?.let { Spacer(Modifier.height(8.dp)); Text("Dia escolhido: ${dataLonga(it)}", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary) }
}

@Composable
private fun PassoHorario(ui: AssistenteUi, vm: NovaReservaViewModel) {
    val a = ui.area!!
    val dia = ui.disp?.dia(ui.data!!)
    FieldLabel("Horário · ${dataLonga(ui.data!!)}")
    FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        a.turnos.forEach { t ->
            val livre = dia?.turnoLivre(ui.itemId!!, t.id) == true
            val sel = ui.turnoId == t.id
            Surface(
                onClick = { vm.update { it.copy(turnoId = t.id) } }, enabled = livre, shape = MaterialTheme.shapes.medium,
                color = if (sel) MaterialTheme.colorScheme.primary else MaterialTheme.ext.card,
                border = BorderStroke(1.dp, if (sel) MaterialTheme.colorScheme.primary else MaterialTheme.ext.cardBorder),
                modifier = Modifier.testTag("turno_${t.id}")
            ) {
                Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                    val cor = if (sel) MaterialTheme.colorScheme.onPrimary else if (livre) MaterialTheme.ext.ink else MaterialTheme.ext.faint
                    Text(t.nome, style = MaterialTheme.typography.titleSmall, color = cor)
                    if (t.inicio != null && t.fim != null) Text("${Fmt.hora(t.inicio)} – ${Fmt.hora(t.fim)}", style = MaterialTheme.typography.bodySmall, color = cor)
                    if (!livre) Text("Ocupado", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.ext.danger)
                }
            }
        }
    }
    Spacer(Modifier.height(18.dp))
    FieldLabel("Convidados (até ${ui.limite})")
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = { vm.update { it.copy(convidados = (it.convidados - 1).coerceAtLeast(1)) } }) { Icon(Icons.Rounded.Remove, contentDescription = "Menos") }
        Text("${ui.convidados}", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.ext.ink, textAlign = TextAlign.Center, modifier = Modifier.size(width = 56.dp, height = 36.dp))
        IconButton(onClick = { vm.update { it.copy(convidados = (it.convidados + 1).coerceAtMost(it.limite)) } }) { Icon(Icons.Rounded.Add, contentDescription = "Mais") }
        Spacer(Modifier.size(8.dp))
        Icon(Icons.Rounded.Group, contentDescription = null, tint = MaterialTheme.ext.muted)
    }
    Spacer(Modifier.height(10.dp))
    OutlinedTextField(
        ui.nomes, { v -> vm.update { it.copy(nomes = v) } }, label = { Text("Lista de convidados (opcional)") },
        placeholder = { Text("Um nome por linha") }, minLines = 3, modifier = Modifier.fillMaxWidth().heightIn(min = 100.dp)
    )
    Spacer(Modifier.height(8.dp))
    NoteBox(Icons.Rounded.Group, MaterialTheme.colorScheme.primary, "A lista vai para a portaria no dia, para o check-in na entrada. Dá para mudar depois.")
    Spacer(Modifier.height(10.dp))
    OutlinedTextField(ui.observacoes, { v -> vm.update { it.copy(observacoes = v) } }, label = { Text("Observação (opcional)") }, modifier = Modifier.fillMaxWidth())
}

@Composable
private fun PassoTermo(ui: AssistenteUi, vm: NovaReservaViewModel, assinatura: br.com.condovitta.app.core.designsystem.SignatureState) {
    val a = ui.area!!
    AppCard {
        Column(Modifier.padding(16.dp)) {
            InfoRows(
                listOf(
                    "Espaço" to listOfNotNull(ui.item?.nome, ui.item?.apelido).joinToString(" · "),
                    "Data" to ui.data?.let { dataLonga(it).replaceFirstChar { c -> c.uppercase() } },
                    "Horário" to ui.turno?.let { t -> listOfNotNull(t.nome, if (t.inicio != null && t.fim != null) "${Fmt.hora(t.inicio)} – ${Fmt.hora(t.fim)}" else null).distinct().joinToString(" · ") },
                    "Convidados" to "${ui.convidados}",
                    "Taxa" to (taxaTexto(ui.taxa) + if (ui.taxa?.signum() == 1) " (no próximo boleto)" else "")
                )
            )
        }
    }
    if (a.exigeAprovacao) { Spacer(Modifier.height(10.dp)); NoteBox(Icons.Rounded.Schedule, MaterialTheme.ext.warn, "Esta área precisa da aprovação do síndico. Você recebe a resposta no app.") }
    if (!a.exigeTermo) return
    Spacer(Modifier.height(14.dp))
    FieldLabel("Termo de uso")
    Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surfaceContainer, modifier = Modifier.fillMaxWidth()) {
        Text(a.termoTexto ?: "Regimento de uso da área.", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(14.dp))
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
            .toggleable(value = ui.aceite, role = Role.Checkbox, onValueChange = { v -> vm.update { it.copy(aceite = v) } })
            .testTag("termo_aceite")
    ) {
        Checkbox(ui.aceite, onCheckedChange = null)
        Text("Li e concordo com o termo de uso.", style = MaterialTheme.typography.bodyMedium)
    }
    FieldLabel("Assinatura")
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(ModoAssinatura.DESENHO to "Desenhar", ModoAssinatura.DIGITADA to "Digitar o nome").forEach { (m, l) ->
            FilterChip(ui.modo == m, { vm.update { it.copy(modo = m) } }, label = { Text(l) },
                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = MaterialTheme.colorScheme.primary, selectedLabelColor = MaterialTheme.colorScheme.onPrimary))
        }
    }
    Spacer(Modifier.height(8.dp))
    if (ui.modo == ModoAssinatura.DESENHO) {
        SignaturePad(assinatura)
        TextButton(onClick = { assinatura.clear() }) { Text("Limpar assinatura") }
    } else OutlinedTextField(ui.nomeDigitado, { v -> vm.update { it.copy(nomeDigitado = v) } }, label = { Text("Seu nome completo") }, singleLine = true, modifier = Modifier.fillMaxWidth())
}

@Composable
private fun Concluida(r: Reserva, onConcluir: () -> Unit) {
    val pendente = r.status == StatusReserva.AGUARDANDO_APROVACAO
    Column(Modifier.fillMaxSize().padding(24.dp).navigationBarsPadding(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        IconBubble(if (pendente) Icons.Rounded.Schedule else Icons.Rounded.EventAvailable, if (pendente) MaterialTheme.ext.warn else MaterialTheme.ext.success, size = 84.dp)
        Spacer(Modifier.height(18.dp))
        Text(if (pendente) "Pedido enviado" else "Reserva confirmada", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.ext.ink)
        Spacer(Modifier.height(6.dp))
        Text("${r.local}\n${dataLonga(r.data).replaceFirstChar { it.uppercase() }} · ${horario(r)}", style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.ext.muted, textAlign = TextAlign.Center)
        Spacer(Modifier.height(10.dp))
        if (pendente) Text("O síndico vai analisar. Você recebe a resposta no app.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.ext.muted, textAlign = TextAlign.Center)
        r.protocolo?.let { Text("Protocolo $it", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.ext.faint, fontWeight = FontWeight.SemiBold) }
        Spacer(Modifier.height(24.dp))
        Button(onClick = onConcluir, modifier = Modifier.fillMaxWidth().height(52.dp)) { Text("Ver minhas reservas") }
        Spacer(Modifier.height(10.dp))
        NoteBox(Icons.Rounded.Info, MaterialTheme.colorScheme.primary, "A portaria recebe a lista de convidados no dia da reserva.")
    }
}
