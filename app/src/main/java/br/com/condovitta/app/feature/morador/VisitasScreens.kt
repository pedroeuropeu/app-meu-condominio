package br.com.condovitta.app.feature.morador

import android.app.TimePickerDialog
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Badge
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.condovitta.app.core.common.Fmt
import br.com.condovitta.app.core.common.Photos
import br.com.condovitta.app.core.designsystem.AppCard
import br.com.condovitta.app.core.designsystem.BackTopBar
import br.com.condovitta.app.core.designsystem.IconBubble
import br.com.condovitta.app.core.designsystem.SectionHeader
import br.com.condovitta.app.core.designsystem.StatusBadge
import br.com.condovitta.app.core.designsystem.ext
import br.com.condovitta.app.domain.Autorizacao
import br.com.condovitta.app.domain.DiaSemana
import br.com.condovitta.app.domain.SituacaoAutorizacao
import br.com.condovitta.app.domain.TipoVisitante
import br.com.condovitta.app.feature.portaria.BusyOr
import br.com.condovitta.app.feature.portaria.CardList
import br.com.condovitta.app.feature.portaria.EmptyBox
import br.com.condovitta.app.feature.portaria.ErrorBox
import br.com.condovitta.app.feature.portaria.FieldLabel
import br.com.condovitta.app.feature.portaria.InfoRows
import br.com.condovitta.app.feature.portaria.LoadingBox
import br.com.condovitta.app.feature.portaria.NoteBox
import br.com.condovitta.app.feature.portaria.VisitaRow
import br.com.condovitta.app.feature.portaria.color
import br.com.condovitta.app.feature.portaria.icon
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset

/** Quando a autorização vale, em palavras ("Hoje, 14:00 – 18:00", "seg, qua e sex · 10:00 – 17:00"). */
fun janela(a: Autorizacao): String = if (a.recorrente) {
    listOfNotNull(DiaSemana.descrever(a.dias), if (a.horaInicio != null && a.horaFim != null) "${Fmt.hora(a.horaInicio)} – ${Fmt.hora(a.horaFim)}" else null,
        a.validoAte?.let { "até ${Fmt.data(it)}" }).joinToString(" · ")
} else {
    val i = a.inicio
    val f = a.fim
    when {
        i == null -> "Sem horário"
        f != null && i.toLocalDate() == f.toLocalDate() -> "${dia(i.toLocalDate())}, ${Fmt.hora(i)} – ${Fmt.hora(f)}"
        else -> "${dia(i.toLocalDate())} ${Fmt.hora(i)} até ${f?.let { "${dia(it.toLocalDate())} ${Fmt.hora(it)}" } ?: "—"}"
    }
}

private fun dia(d: LocalDate, hoje: LocalDate = LocalDate.now()) = when (d) {
    hoje -> "Hoje"
    hoje.plusDays(1) -> "Amanhã"
    else -> Fmt.data(d)
}

@Composable
private fun SituacaoBadge(s: SituacaoAutorizacao) {
    val e = MaterialTheme.ext
    when (s) {
        SituacaoAutorizacao.LIBERADA_AGORA -> StatusBadge("Liberado agora", e.success, e.successContainer)
        SituacaoAutorizacao.AGENDADA -> StatusBadge("Agendado", e.info, e.infoContainer, icon = Icons.Rounded.Schedule)
        SituacaoAutorizacao.EXPIRADA -> StatusBadge("Expirado", e.muted, MaterialTheme.colorScheme.surfaceContainer)
        SituacaoAutorizacao.REVOGADA -> StatusBadge("Revogado", e.danger, e.dangerContainer)
    }
}

/** Texto para mandar ao visitante pelo WhatsApp etc. */
fun convite(a: Autorizacao, condominio: String?, unidade: String?): String = buildString {
    append("Olá, ${a.nome}! Você está autorizado(a) a entrar")
    condominio?.let { append(" no $it") }
    unidade?.let { append(" (unidade $it)") }
    append(".\n").append(janela(a)).append('.')
    a.codigo?.let { append("\nMostre este código na portaria: ").append(it) }
}

/* ------------------------------------------------------------------ Meus visitantes */

@Composable
fun VisitasScreen(nav: MoradorNav, vm: VisitasViewModel = hiltViewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    LifecycleResumeEffect(Unit) {
        vm.refresh()
        onPauseOrDispose { }
    }
    Scaffold(
        topBar = { BackTopBar("Meus visitantes", onBack = nav.back) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = nav.novaAutorizacao, icon = { Icon(Icons.Rounded.PersonAdd, contentDescription = null) }, text = { Text("Autorizar") },
                containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.testTag("fab_autorizar")
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { p ->
        val v = state.data
        PullToRefreshBox(isRefreshing = state.loading && v != null, onRefresh = { vm.refresh() }, modifier = Modifier.padding(p).fillMaxSize()) {
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
                when {
                    v == null && state.loading -> LoadingBox()
                    v == null -> ErrorBox(state.error ?: "Não deu para carregar.", onRetry = { vm.refresh() })
                    else -> {
                        v.consultas.forEach { c ->
                            Spacer(Modifier.height(10.dp))
                            ConsultaCard(c) { liberar, recado -> vm.responder(c, liberar, recado) }
                        }
                        SectionHeader("Autorizações ativas", subtitle = "A portaria libera sem precisar te chamar")
                        val agora = vm.agora()
                        val ativas = v.autorizacoes.filter { it.ativa && it.situacao(agora) != SituacaoAutorizacao.EXPIRADA }
                        if (ativas.isEmpty()) EmptyBox(Icons.Rounded.Badge, "Nenhuma autorização", "Autorize visitas e prestadores para entrarem sem interfone.")
                        else CardList(ativas) { a ->
                            Row(
                                Modifier.fillMaxWidth().clickable { nav.autorizacao(a.id) }.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                IconBubble(a.tipo.icon, a.tipo.color)
                                Column(Modifier.weight(1f)) {
                                    Text(a.nome, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.ext.ink)
                                    Text(listOfNotNull(a.tipo.label, a.obs).joinToString(" · "), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.ext.muted)
                                    Text(janela(a), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.ext.muted)
                                }
                                SituacaoBadge(a.situacao(agora))
                            }
                        }
                        if (v.historico.isNotEmpty()) {
                            SectionHeader("Na portaria hoje", subtitle = "${v.entradas30d} entrada(s) nos últimos 30 dias")
                            CardList(v.historico) { h -> VisitaRow(h) {} }
                        }
                        Spacer(Modifier.height(96.dp))
                    }
                }
            }
        }
    }
}

/* ------------------------------------------------------------------ Autorizar visita */

@Composable
fun NovaAutorizacaoScreen(nav: MoradorNav, condominio: String?, unidade: String?, vm: NovaAutorizacaoViewModel = hiltViewModel()) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    val criada by vm.criada.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var escolherData by remember { mutableStateOf(false) }
    var escolherValidade by remember { mutableStateOf(false) }

    fun hora(atual: LocalTime, set: (LocalTime) -> Unit) =
        TimePickerDialog(context, { _, h, m -> set(LocalTime.of(h, m)) }, atual.hour, atual.minute, true).show()

    val a = criada
    if (a != null) {
        // pronto: mostra o código e oferece compartilhar com o visitante
        Scaffold(topBar = { BackTopBar("Autorização criada", onBack = nav.back) }, containerColor = MaterialTheme.colorScheme.background) { p ->
            Column(Modifier.padding(p).fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                IconBubble(a.tipo.icon, MaterialTheme.ext.success, size = 76.dp)
                Spacer(Modifier.height(16.dp))
                Text("${a.nome} pode entrar", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.ext.ink, textAlign = TextAlign.Center)
                Text(janela(a), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.ext.muted, textAlign = TextAlign.Center)
                Spacer(Modifier.height(20.dp))
                a.codigo?.let { CodigoBox(it, "CÓDIGO PARA A PORTARIA") }
                Spacer(Modifier.height(20.dp))
                Button(onClick = { compartilhar(context, convite(a, condominio, unidade), "Enviar para ${a.nome}") }, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                    Icon(Icons.Rounded.Share, contentDescription = null); Spacer(Modifier.size(8.dp)); Text("Compartilhar com ${a.nome.substringBefore(' ')}")
                }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = nav.back, modifier = Modifier.fillMaxWidth().height(48.dp)) { Text("Concluir") }
            }
        }
        return
    }

    Scaffold(
        topBar = { BackTopBar("Autorizar visita", onBack = nav.back) },
        bottomBar = {
            Surface(color = MaterialTheme.colorScheme.surfaceContainerLowest, shadowElevation = 8.dp) {
                Button(
                    onClick = vm::salvar, enabled = ui.valido && !ui.saving,
                    modifier = Modifier.fillMaxWidth().navigationBarsPadding().imePadding().padding(horizontal = 16.dp, vertical = 12.dp).height(52.dp).testTag("autorizar_salvar")
                ) { BusyOr(ui.saving) { Icon(Icons.Rounded.PersonAdd, contentDescription = null); Spacer(Modifier.size(8.dp)); Text("Autorizar e avisar a portaria") } }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { p ->
        Column(Modifier.padding(p).fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
            Text("Quem você autorizar entra sem a portaria precisar te chamar.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.ext.muted)
            Spacer(Modifier.height(14.dp))
            OutlinedTextField(
                ui.nome, { v -> vm.update { it.copy(nome = v) } }, label = { Text("Nome de quem vem") }, singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words), modifier = Modifier.fillMaxWidth().testTag("autorizar_nome")
            )
            Spacer(Modifier.height(12.dp))
            FieldLabel("Quem é")
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TipoVisitante.entries.forEach { t ->
                    FilterChip(ui.tipo == t, { vm.update { it.copy(tipo = t) } }, label = { Text(t.label) },
                        leadingIcon = { Icon(t.icon, contentDescription = null, modifier = Modifier.size(18.dp)) }, colors = chips())
                }
            }
            Spacer(Modifier.height(14.dp))
            FieldLabel("Quando")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(QuandoVisita.AGORA to "Hoje", QuandoVisita.DATA to "Outro dia", QuandoVisita.RECORRENTE to "Toda semana").forEach { (q, l) ->
                    FilterChip(ui.quando == q, { vm.quando(q) }, label = { Text(l) }, colors = chips())
                }
            }
            Spacer(Modifier.height(10.dp))
            when (ui.quando) {
                QuandoVisita.DATA -> OutlinedButton(onClick = { escolherData = true }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Rounded.CalendarMonth, contentDescription = null, modifier = Modifier.size(18.dp)); Spacer(Modifier.size(8.dp))
                    Text(ui.data?.let { Fmt.data(it) } ?: "Escolher o dia")
                }
                QuandoVisita.RECORRENTE -> {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        DiaSemana.entries.forEach { d ->
                            FilterChip(d in ui.dias, { vm.update { it.copy(dias = if (d in it.dias) it.dias - d else it.dias + d) } }, label = { Text(d.label) }, colors = chips())
                        }
                    }
                    OutlinedButton(onClick = { escolherValidade = true }, modifier = Modifier.fillMaxWidth()) {
                        Text(ui.validoAte?.let { "Vale até ${Fmt.data(it)}" } ?: "Sem data para acabar (toque para definir)")
                    }
                }
                QuandoVisita.AGORA -> Unit
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = { hora(ui.horaInicio) { h -> vm.update { it.copy(horaInicio = h) } } }, modifier = Modifier.weight(1f)) { Text("Das ${Fmt.hora(ui.horaInicio)}") }
                OutlinedButton(onClick = { hora(ui.horaFim) { h -> vm.update { it.copy(horaFim = h) } } }, modifier = Modifier.weight(1f)) { Text("Até ${Fmt.hora(ui.horaFim)}") }
            }
            if (!ui.horaFim.isAfter(ui.horaInicio)) Text("O fim precisa ser depois do início.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)

            Spacer(Modifier.height(14.dp))
            OutlinedTextField(ui.documento, { v -> vm.update { it.copy(documento = v) } }, label = { Text("Documento (opcional)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(ui.placa, { v -> vm.update { it.copy(placa = v.uppercase().filter { c -> c.isLetterOrDigit() }.take(7)) } }, label = { Text("Placa") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters), modifier = Modifier.weight(1f))
                OutlinedTextField(ui.telefone, { v -> vm.update { it.copy(telefone = v) } }, label = { Text("Telefone") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), modifier = Modifier.weight(1f))
            }
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(ui.obs, { v -> vm.update { it.copy(obs = v) } }, label = { Text("Observação (opcional)") }, placeholder = { Text("Ex.: diarista, técnico da internet") }, modifier = Modifier.fillMaxWidth())
            ui.error?.let { Spacer(Modifier.height(12.dp)); NoteBox(Icons.Rounded.ErrorOutline, MaterialTheme.colorScheme.error, it) }
            Spacer(Modifier.height(24.dp))
        }
    }

    if (escolherData || escolherValidade) DataDialog(
        inicial = if (escolherData) ui.data else ui.validoAte, minimo = vm.hoje,
        onOk = { d -> if (escolherData) vm.update { it.copy(data = d) } else vm.update { it.copy(validoAte = d) }; escolherData = false; escolherValidade = false },
        onDismiss = { escolherData = false; escolherValidade = false }
    )
}

@Composable
private fun chips() = FilterChipDefaults.filterChipColors(
    selectedContainerColor = MaterialTheme.colorScheme.primary, selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
    selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary
)

/** Calendário do Material (sem datas passadas). */
@Composable
fun DataDialog(inicial: LocalDate?, minimo: LocalDate, onOk: (LocalDate) -> Unit, onDismiss: () -> Unit) {
    val minMillis = minimo.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()
    val state = rememberDatePickerState(
        initialSelectedDateMillis = (inicial ?: minimo).atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli(),
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long) = utcTimeMillis >= minMillis
        }
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { state.selectedDateMillis?.let { onOk(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()) } }) { Text("OK") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    ) { DatePicker(state) }
}

/* ------------------------------------------------------------------ Detalhe da autorização */

@Composable
fun AutorizacaoScreen(nav: MoradorNav, condominio: String?, unidade: String?, vm: AutorizacaoViewModel = hiltViewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    val qr by vm.qr.collectAsStateWithLifecycle()
    val revogada by vm.revogada.collectAsStateWithLifecycle()
    val context = LocalContext.current
    LaunchedEffect(Unit) { vm.refresh() }
    LaunchedEffect(revogada) { if (revogada) nav.back() }

    Scaffold(topBar = { BackTopBar("Autorização", onBack = nav.back) }, containerColor = MaterialTheme.colorScheme.background) { p ->
        val a = state.data
        Column(Modifier.padding(p).fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
            when {
                a == null && state.loading -> LoadingBox()
                a == null -> ErrorBox(state.error ?: "Não deu para carregar.", onRetry = vm::refresh)
                else -> {
                    AppCard {
                        Column(Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                                IconBubble(a.tipo.icon, a.tipo.color, size = 56.dp)
                                Column(Modifier.weight(1f)) {
                                    Text(a.nome, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.ext.ink)
                                    Spacer(Modifier.height(4.dp))
                                    SituacaoBadge(a.situacao(vm.agora()))
                                }
                            }
                            InfoRows(
                                listOf(
                                    "Quando" to janela(a), "Tipo" to a.tipo.label, "Documento" to a.documento, "Telefone" to a.telefone,
                                    "Veículo" to listOfNotNull(a.placa, a.veiculo).joinToString(" · ").ifBlank { null }, "Observação" to a.obs,
                                    "Autorizado por" to a.autorizadoPor
                                )
                            )
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                        val bmp = remember(qr) { Photos.decode(qr) }
                        if (bmp != null) Image(bmp.asImageBitmap(), contentDescription = "QR da autorização", modifier = Modifier.size(200.dp).clip(MaterialTheme.shapes.medium).background(Color.White))
                        a.codigo?.let { Spacer(Modifier.height(10.dp)); CodigoBox(it, "CÓDIGO PARA A PORTARIA") }
                    }
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = { compartilhar(context, convite(a, condominio, unidade), "Enviar para ${a.nome}") }, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                        Icon(Icons.Rounded.Share, contentDescription = null); Spacer(Modifier.size(8.dp)); Text("Compartilhar")
                    }
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(onClick = vm::revogar, modifier = Modifier.fillMaxWidth().height(48.dp).testTag("autorizacao_revogar")) {
                        Icon(Icons.Rounded.Block, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.ext.danger); Spacer(Modifier.size(8.dp))
                        Text("Revogar autorização", color = MaterialTheme.ext.danger)
                    }
                    Spacer(Modifier.height(12.dp))
                    NoteBox(Icons.Rounded.Info, MaterialTheme.colorScheme.primary, "A portaria confere o nome e o código. Revogando, a entrada deixa de valer na hora.")
                    Spacer(Modifier.height(24.dp))
                }
            }
        }
    }
}
