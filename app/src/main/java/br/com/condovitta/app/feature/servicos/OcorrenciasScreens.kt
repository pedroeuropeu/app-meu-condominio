package br.com.condovitta.app.feature.servicos

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Gavel
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.ReportProblem
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.condovitta.app.core.common.Fmt
import br.com.condovitta.app.core.designsystem.AppCard
import br.com.condovitta.app.core.designsystem.BackTopBar
import br.com.condovitta.app.core.designsystem.SectionHeader
import br.com.condovitta.app.core.designsystem.StatusBadge
import br.com.condovitta.app.core.designsystem.ext
import br.com.condovitta.app.domain.MotivoOcorrencia
import br.com.condovitta.app.domain.NovaOcorrencia
import br.com.condovitta.app.domain.OcorrenciaContra
import br.com.condovitta.app.domain.OcorrenciaMinha
import br.com.condovitta.app.domain.StatusOcorrencia
import br.com.condovitta.app.feature.portaria.BusyOr
import br.com.condovitta.app.feature.portaria.EmptyBox
import br.com.condovitta.app.feature.portaria.FilterChips
import br.com.condovitta.app.feature.portaria.InfoRows
import br.com.condovitta.app.feature.portaria.NoteBox
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneOffset

private val Amber = Color(0xFFD97706)

@Composable
fun StatusOcorrenciaBadge(s: StatusOcorrencia) {
    val e = MaterialTheme.ext
    when (s) {
        StatusOcorrencia.EM_ANALISE -> StatusBadge(s.label, e.info, e.infoContainer)
        StatusOcorrencia.ADVERTENCIA -> StatusBadge(s.label, e.warn, e.warnContainer)
        StatusOcorrencia.MULTA -> StatusBadge(s.label, e.danger, e.dangerContainer)
        StatusOcorrencia.ARQUIVADA -> StatusBadge(s.label, e.muted, e.cardBorder)
    }
}

private enum class AbaOcorrencia { CONTRA, MINHAS }

/* ------------------------------------------------------------------ painel */

@Composable
fun OcorrenciasScreen(onBack: () -> Unit, onNova: () -> Unit, vm: OcorrenciasViewModel = hiltViewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    val baixando by vm.baixando.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var aba by rememberSaveable { mutableStateOf<AbaOcorrencia?>(null) }
    var defender by rememberSaveable { mutableStateOf<Long?>(null) }
    TelaServico(
        "Ocorrências", state, onBack, { vm.refresh() },
        fab = {
            ExtendedFloatingActionButton(onClick = onNova, icon = { Icon(Icons.Rounded.Add, contentDescription = null) }, text = { Text("Registrar") },
                containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.testTag("ocorrencia_nova"))
        }
    ) { p ->
        val hoje = vm.hoje()
        val pendentes = p.contra.count { it.podeDefender(hoje) }
        // abre em "contra a minha unidade" quando há defesa a fazer
        val atual = aba ?: if (pendentes > 0 || p.minhas.isEmpty()) AbaOcorrencia.CONTRA else AbaOcorrencia.MINHAS
        Spacer(Modifier.height(8.dp))
        FilterChips(
            listOf(AbaOcorrencia.CONTRA to "Minha unidade (${p.contra.size})", AbaOcorrencia.MINHAS to "Registradas por mim (${p.minhas.size})"),
            atual, { aba = it }
        )
        Spacer(Modifier.height(8.dp))
        when (atual) {
            AbaOcorrencia.CONTRA -> {
                if (pendentes > 0) NoteBox(Icons.Rounded.Gavel, MaterialTheme.ext.warn, "Há $pendentes ocorrência(s) contra a sua unidade esperando a sua defesa.")
                if (p.contra.isEmpty()) EmptyBox(Icons.Rounded.ReportProblem, "Nada contra a sua unidade", "Se alguém registrar uma ocorrência envolvendo a sua unidade, você poderá se defender por aqui.")
                p.contra.forEach { o -> ContraCard(o, o.podeDefender(hoje), baixando != null, onDefender = { defender = o.id }) { a -> vm.abrirProva(o.id, a) { abrirAnexo(context, it, a) } } }
            }
            AbaOcorrencia.MINHAS -> {
                if (p.minhas.isEmpty()) EmptyBox(Icons.Rounded.ReportProblem, "Nenhuma ocorrência registrada", "Barulho, vaga ocupada, dano ao patrimônio? Registre e acompanhe a decisão por aqui.")
                p.minhas.forEach { o -> MinhaCard(o, baixando != null) { a -> vm.abrirProva(o.id, a) { abrirAnexo(context, it, a) } } }
            }
        }
        FimDaLista()
    }
    defender?.let { id -> DefesaSheet(vm, id, onDismiss = { defender = null }) }
}

@Composable
private fun ContraCard(o: OcorrenciaContra, podeDefender: Boolean, baixando: Boolean, onDefender: () -> Unit, onProva: (br.com.condovitta.app.domain.Anexo) -> Unit) {
    AppCard(Modifier.padding(bottom = 10.dp)) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(o.motivo.label, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.ext.ink, modifier = Modifier.weight(1f))
                StatusOcorrenciaBadge(o.status)
            }
            Text(o.descricao, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.ext.ink, modifier = Modifier.padding(top = 6.dp))
            InfoRows(
                listOf(
                    "Protocolo" to o.protocolo, "Quando" to o.ocorridaEm?.let { Fmt.quando(it) }, "Onde" to o.local,
                    "Prazo da defesa" to o.prazoDefesa?.takeIf { o.defesa == null && o.status == StatusOcorrencia.EM_ANALISE }?.let { Fmt.data(it) },
                    "Multa" to o.valorMulta?.let { Fmt.money(it) }
                )
            )
            o.defesa?.let {
                Spacer(Modifier.height(6.dp))
                NoteBox(Icons.Rounded.Info, MaterialTheme.colorScheme.primary, "Sua defesa${o.defesaEm?.let { d -> " (" + Fmt.quando(d) + ")" } ?: ""}: $it")
                if (o.provasDefesa.isNotEmpty()) FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) { o.provasDefesa.forEach { a -> AnexoChip(a, baixando) { onProva(a) } } }
            }
            o.decisaoTexto?.let {
                Spacer(Modifier.height(6.dp))
                NoteBox(Icons.Rounded.Gavel, if (o.status == StatusOcorrencia.MULTA) MaterialTheme.ext.danger else MaterialTheme.ext.muted, "Decisão: $it")
            }
            if (podeDefender) {
                Spacer(Modifier.height(10.dp))
                Button(onClick = onDefender, modifier = Modifier.fillMaxWidth().testTag("ocorrencia_defender")) { Text("Enviar minha defesa") }
            }
        }
    }
}

@Composable
private fun MinhaCard(o: OcorrenciaMinha, baixando: Boolean, onProva: (br.com.condovitta.app.domain.Anexo) -> Unit) {
    AppCard(Modifier.padding(bottom = 10.dp)) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(o.motivo.label, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.ext.ink, modifier = Modifier.weight(1f))
                StatusOcorrenciaBadge(o.status)
            }
            Text(o.descricao, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.ext.ink, modifier = Modifier.padding(top = 6.dp))
            InfoRows(
                listOf(
                    "Protocolo" to o.protocolo, "Unidade" to (o.unidade ?: "Condomínio (geral)"), "Quando" to o.ocorridaEm?.let { Fmt.quando(it) },
                    "Onde" to o.local, "Testemunhas" to o.testemunhas,
                    "Defesa" to if (o.status == StatusOcorrencia.EM_ANALISE && o.unidade != null) (if (o.defesaRecebida) "Recebida" else "Prazo até ${Fmt.data(o.prazoDefesa)}") else null
                )
            )
            if (o.provas.isNotEmpty()) FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) { o.provas.forEach { a -> AnexoChip(a, baixando) { onProva(a) } } }
            o.decisaoTexto?.let {
                Spacer(Modifier.height(6.dp))
                NoteBox(Icons.Rounded.Gavel, MaterialTheme.ext.muted, "Decisão: $it")
            }
        }
    }
}

@Composable
private fun DefesaSheet(vm: OcorrenciasViewModel, id: Long, onDismiss: () -> Unit) {
    val salvando by vm.envio.salvando.collectAsStateWithLifecycle()
    val erro by vm.envio.erro.collectAsStateWithLifecycle()
    val ok by vm.envio.ok.collectAsStateWithLifecycle()
    val fotos by vm.fotos.collectAsStateWithLifecycle()
    val inicial = remember { ok }
    LaunchedEffect(ok) { if (ok != inicial) onDismiss() }
    LaunchedEffect(Unit) { vm.envio.erro.value = null }
    var texto by rememberSaveable { mutableStateOf("") }
    val escolher = rememberSeletorFotos(5 - fotos.size) { vm.adicionar(it) }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 20.dp).navigationBarsPadding().imePadding()) {
            Text("Sua defesa", style = MaterialTheme.typography.titleLarge)
            Text("Conte a sua versão. Só dá para enviar uma defesa por unidade, dentro do prazo.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.ext.muted)
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(texto, { texto = it.take(4000) }, label = { Text("O que aconteceu, na sua versão") }, minLines = 4,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences), modifier = Modifier.fillMaxWidth().testTag("defesa_texto"))
            Spacer(Modifier.height(8.dp))
            AnexosEnvio(fotos, escolher, { vm.remover(it) }, rotulo = "Anexar provas")
            erro?.let { Spacer(Modifier.height(8.dp)); NoteBox(Icons.Rounded.ErrorOutline, MaterialTheme.colorScheme.error, it) }
            Spacer(Modifier.height(12.dp))
            Button(onClick = { vm.defesa(id, texto) }, enabled = texto.isNotBlank() && !salvando, modifier = Modifier.fillMaxWidth().height(50.dp)) {
                BusyOr(salvando) { Text("Enviar defesa") }
            }
        }
    }
}

/* ------------------------------------------------------------------ registrar */

@Composable
fun NovaOcorrenciaScreen(onBack: () -> Unit, vm: NovaOcorrenciaViewModel = hiltViewModel()) {
    val unidades by vm.unidades.collectAsStateWithLifecycle()
    val salvando by vm.envio.salvando.collectAsStateWithLifecycle()
    val erro by vm.envio.erro.collectAsStateWithLifecycle()
    val ok by vm.envio.ok.collectAsStateWithLifecycle()
    val fotos by vm.fotos.collectAsStateWithLifecycle()
    LaunchedEffect(ok) { if (ok > 0) onBack() }
    val agora = remember { vm.agora() }
    var geral by rememberSaveable { mutableStateOf(false) }
    var unidade by rememberSaveable { mutableStateOf<String?>(null) }
    var busca by rememberSaveable { mutableStateOf("") }
    var motivo by rememberSaveable { mutableStateOf(MotivoOcorrencia.BARULHO_EXCESSIVO) }
    var data by rememberSaveable { mutableStateOf(agora.toLocalDate()) }
    var hora by rememberSaveable { mutableStateOf(agora.toLocalTime().withSecond(0).withNano(0)) }
    var local by rememberSaveable { mutableStateOf("") }
    var descricao by rememberSaveable { mutableStateOf("") }
    var testemunhas by rememberSaveable { mutableStateOf("") }
    var declaro by rememberSaveable { mutableStateOf(false) }
    var escolhendoData by remember { mutableStateOf(false) }
    var escolhendoHora by remember { mutableStateOf(false) }
    val escolher = rememberSeletorFotos(5 - fotos.size) { vm.adicionar(it) }

    Scaffold(topBar = { BackTopBar("Registrar ocorrência", onBack = onBack) }, containerColor = MaterialTheme.colorScheme.background) { p ->
        Column(Modifier.padding(p).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).imePadding()) {
            NoteBox(Icons.Rounded.Info, Amber, "Quem mora na unidade apontada recebe o relato sem o seu nome e tem 5 dias para se defender. A administração decide.")
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 10.dp)) {
                Column(Modifier.weight(1f)) {
                    Text("Problema geral do condomínio", style = MaterialTheme.typography.bodyLarge)
                    Text("Sem apontar uma unidade.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.ext.muted)
                }
                Switch(geral, { geral = it })
            }
            if (!geral) {
                SectionHeader("Unidade envolvida")
                OutlinedTextField(busca, { busca = it.uppercase().take(10) }, label = { Text("Buscar unidade (ex.: B-304)") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("ocorrencia_unidade"))
                Spacer(Modifier.height(6.dp))
                val filtradas = unidades.filter { busca.isBlank() || it.rotulo.replace("-", "").contains(busca.replace("-", ""), ignoreCase = true) }
                    .filterNot { it.rotulo == unidade }.take(12)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    unidade?.let { u -> FilterChip(true, { unidade = null }, label = { Text(u) }, colors = chipColors()) }
                    filtradas.forEach { u -> FilterChip(false, { unidade = u.rotulo; busca = "" }, label = { Text(u.rotulo) }, colors = chipColors()) }
                }
            }
            SectionHeader("Motivo")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MotivoOcorrencia.entries.forEach { m -> FilterChip(motivo == m, { motivo = m }, label = { Text(m.label) }, colors = chipColors()) }
            }
            SectionHeader("Quando e onde")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AssistChip(onClick = { escolhendoData = true }, label = { Text(if (data == agora.toLocalDate()) "Hoje" else Fmt.data(data)) },
                    leadingIcon = { Icon(Icons.Rounded.CalendarMonth, contentDescription = null) })
                AssistChip(onClick = { escolhendoHora = true }, label = { Text(Fmt.hora(hora)) }, leadingIcon = { Icon(Icons.Rounded.Schedule, contentDescription = null) })
            }
            OutlinedTextField(local, { local = it.take(250) }, label = { Text("Local (opcional)") }, placeholder = { Text("Ex.: Garagem, piscina, apartamento de cima") },
                singleLine = true, keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences), modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(descricao, { descricao = it.take(4000) }, label = { Text("O que aconteceu?") }, minLines = 4,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences), modifier = Modifier.fillMaxWidth().testTag("ocorrencia_descricao"))
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(testemunhas, { testemunhas = it.take(500) }, label = { Text("Testemunhas (opcional)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(10.dp))
            AnexosEnvio(fotos, escolher, { vm.remover(it) }, rotulo = "Anexar provas")
            Row(
                Modifier.fillMaxWidth().padding(top = 10.dp).toggleable(declaro, role = Role.Checkbox, onValueChange = { declaro = it }),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(declaro, onCheckedChange = null)
                Text("Declaro que as informações são verdadeiras.", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(start = 8.dp))
            }
            erro?.let { Spacer(Modifier.height(8.dp)); NoteBox(Icons.Rounded.ErrorOutline, MaterialTheme.colorScheme.error, it) }
            Spacer(Modifier.height(14.dp))
            Button(
                onClick = { vm.registrar(NovaOcorrencia(geral, unidade, motivo, data.atTime(hora), local, descricao, testemunhas), declaro) },
                enabled = descricao.isNotBlank() && declaro && (geral || unidade != null) && !salvando,
                modifier = Modifier.fillMaxWidth().height(52.dp).testTag("ocorrencia_enviar")
            ) { BusyOr(salvando) { Text("Registrar ocorrência") } }
            Spacer(Modifier.height(24.dp))
        }
    }

    if (escolhendoData) {
        val hojeMs = agora.toLocalDate().atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()
        val st = rememberDatePickerState(
            initialSelectedDateMillis = data.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli(),
            selectableDates = object : SelectableDates { override fun isSelectableDate(utcTimeMillis: Long) = utcTimeMillis <= hojeMs }
        )
        DatePickerDialog(
            onDismissRequest = { escolhendoData = false },
            confirmButton = {
                TextButton(onClick = {
                    st.selectedDateMillis?.let { data = Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate() }
                    escolhendoData = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { escolhendoData = false }) { Text("Voltar") } }
        ) { DatePicker(st) }
    }
    if (escolhendoHora) {
        val st = rememberTimePickerState(hora.hour, hora.minute, is24Hour = true)
        AlertDialog(
            onDismissRequest = { escolhendoHora = false },
            confirmButton = { TextButton(onClick = { hora = LocalTime.of(st.hour, st.minute); escolhendoHora = false }) { Text("OK") } },
            dismissButton = { TextButton(onClick = { escolhendoHora = false }) { Text("Voltar") } },
            text = { TimePicker(st) }
        )
    }
}
