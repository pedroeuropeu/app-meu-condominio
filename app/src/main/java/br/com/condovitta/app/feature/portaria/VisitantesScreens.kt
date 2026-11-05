package br.com.condovitta.app.feature.portaria

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.Apartment
import androidx.compose.material.icons.rounded.Badge
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DoorFront
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Pending
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Send
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.condovitta.app.core.common.Fmt
import br.com.condovitta.app.core.designsystem.AppCard
import br.com.condovitta.app.core.designsystem.BackTopBar
import br.com.condovitta.app.core.designsystem.IconBubble
import br.com.condovitta.app.core.designsystem.StatusBadge
import br.com.condovitta.app.core.designsystem.ext
import br.com.condovitta.app.domain.StatusVisita
import br.com.condovitta.app.domain.TipoVisitante
import br.com.condovitta.app.domain.UnidadeResumo
import br.com.condovitta.app.domain.ViaLiberacao
import br.com.condovitta.app.domain.Visita

/* ------------------------------------------------------------------ Visitantes e acesso (aba) */

@Composable
fun VisitantesScreen(padding: PaddingValues, nav: PortariaNav, vm: VisitantesViewModel = hiltViewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    val filtro by vm.filtro.collectAsStateWithLifecycle()
    LifecycleResumeEffect(Unit) {
        vm.refresh()
        onPauseOrDispose { }
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Visitantes e acesso", style = MaterialTheme.typography.titleLarge) },
                actions = {
                    IconButton(onClick = nav.bloqueados) { Icon(Icons.Rounded.Shield, contentDescription = "Bloqueados") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = nav.novaVisita, icon = { Icon(Icons.Rounded.PersonAdd, contentDescription = null) }, text = { Text("Nova visita") },
                containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("fab_nova_visita")
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = Modifier.padding(bottom = padding.calculateBottomPadding())
    ) { p ->
        val l = state.data
        PullToRefreshBox(isRefreshing = state.loading && l != null, onRefresh = vm::refresh, modifier = Modifier.padding(p).fillMaxSize()) {
            LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 96.dp)) {
                item {
                    FilterChips(
                        listOf(
                            FiltroVisitas.ESPERADOS to "Esperados" + (l?.esperados?.size?.let { " · $it" } ?: ""),
                            FiltroVisitas.DENTRO to "No condomínio" + (l?.dentro?.size?.let { " · $it" } ?: ""),
                            FiltroVisitas.HISTORICO to "Histórico"
                        ),
                        filtro, onSelect = { vm.filtro.value = it },
                        modifier = Modifier.horizontalScroll(rememberScrollState()).padding(vertical = 6.dp)
                    )
                }
                when {
                    l == null && state.loading -> item { LoadingBox() }
                    l == null -> item { ErrorBox(state.error ?: "Não deu para carregar.", onRetry = vm::refresh) }
                    else -> {
                        val list = when (filtro) {
                            FiltroVisitas.ESPERADOS -> l.esperados
                            FiltroVisitas.DENTRO -> l.dentro
                            FiltroVisitas.HISTORICO -> l.historico
                        }
                        if (list.isEmpty()) item { EmptyBox(Icons.Rounded.Badge, "Nada por aqui", "Nenhum visitante neste filtro.") }
                        else item {
                            Spacer(Modifier.height(8.dp))
                            CardList(list) { v -> VisitaRow(v) { nav.visitante(v.visitanteId) } }
                        }
                        if (l.bloqueados > 0) item {
                            Spacer(Modifier.height(14.dp))
                            Text(
                                "${l.bloqueados} pessoa(s) na lista de bloqueados · ver",
                                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.clickable(onClick = nav.bloqueados).padding(4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

/* ------------------------------------------------------------------ Detalhe do visitante */

@Composable
fun VisitanteScreen(nav: PortariaNav, vm: VisitanteViewModel = hiltViewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    val busy by vm.busy.collectAsStateWithLifecycle()
    val context = LocalContext.current
    LaunchedEffect(Unit) { vm.refresh() }
    var liberarComo by remember { mutableStateOf(false) }

    Scaffold(
        topBar = { BackTopBar("Visitante", onBack = nav.back) },
        bottomBar = { state.data?.let { v -> VisitanteAcoes(v, busy, onLiberar = { if (v.status == StatusVisita.AGUARDANDO) liberarComo = true else vm.liberar() }, onRecusar = { vm.recusar() }, onSaida = { vm.saida() }, onConsultar = vm::consultar) } },
        containerColor = MaterialTheme.colorScheme.background
    ) { p ->
        val v = state.data
        Column(Modifier.padding(p).fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
            when {
                v == null && state.loading -> LoadingBox()
                v == null -> ErrorBox(state.error ?: "Não deu para carregar.", onRetry = vm::refresh)
                else -> {
                    AppCard {
                        Column(Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                                IconBubble(v.tipo.icon, v.tipo.color, size = 56.dp)
                                Column(Modifier.weight(1f)) {
                                    Text(v.nome, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.ext.ink)
                                    Spacer(Modifier.height(6.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        StatusBadge(v.tipo.label, MaterialTheme.ext.muted, MaterialTheme.colorScheme.surfaceContainer, icon = v.tipo.icon)
                                        VisitaStatusBadge(v.status)
                                    }
                                }
                            }
                            if (v.placa != null) {
                                HorizontalDivider(Modifier.padding(vertical = 12.dp), color = MaterialTheme.ext.cardBorder)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Plate(v.placa)
                                    Spacer(Modifier.weight(1f))
                                    Text(v.veiculo ?: "", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.ext.muted)
                                }
                            }
                            InfoRows(
                                listOf(
                                    "Unidade" to listOfNotNull(v.unidade, v.morador).joinToString(" · "),
                                    "Documento" to v.documento,
                                    "Telefone" to v.telefone,
                                    "Janela" to v.janela,
                                    "Autorização" to listOfNotNull(
                                        if (v.daPortaria) "Cadastrado na portaria" else "Autorizado no app",
                                        v.autorizadoPor?.let { "por $it" }, v.recorrencia
                                    ).joinToString(" · "),
                                    "Entrada" to v.entradaEm?.let { listOfNotNull(Fmt.hora(it), v.via?.label, v.porteiro?.let { p -> "por $p" }).joinToString(" · ") },
                                    "Saída" to v.saidaEm?.let { Fmt.hora(it) },
                                    "Observação" to v.obs
                                )
                            )
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    when {
                        v.status == StatusVisita.AGUARDANDO -> NoteBox(
                            Icons.Rounded.Pending, MaterialTheme.ext.warn,
                            "O morador ainda não respondeu" + (v.consultadoEm?.let { " (perguntamos às ${Fmt.hora(it)})" } ?: "") +
                                ". Você pode chamar a unidade pelo interfone e liberar manualmente."
                        )
                        v.status == StatusVisita.LIBERADO -> NoteBox(
                            Icons.Rounded.Info, MaterialTheme.ext.success,
                            "${v.respondidoPor ?: "O morador"} liberou pelo app" + (v.recado?.let { ": \"$it\"" } ?: ".")
                        )
                        v.status == StatusVisita.RECUSADO && v.recado != null -> NoteBox(Icons.Rounded.Info, MaterialTheme.ext.danger, "Recado do morador: \"${v.recado}\"")
                    }
                    if (v.telefone != null) {
                        Spacer(Modifier.height(10.dp))
                        OutlinedButton(
                            onClick = {
                                val tel = v.telefone.filter { it.isDigit() || it == '+' }
                                runCatching { context.startActivity(android.content.Intent(android.content.Intent.ACTION_DIAL, android.net.Uri.parse("tel:$tel"))) }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Rounded.Call, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.size(8.dp))
                            Text("Ligar para o visitante")
                        }
                    }
                    Spacer(Modifier.height(24.dp))
                }
            }
        }
    }

    if (liberarComo) {
        ModalBottomSheet(onDismissRequest = { liberarComo = false }) {
            Column(Modifier.padding(horizontal = 24.dp).padding(bottom = 24.dp).navigationBarsPadding()) {
                Text("Liberar sem a resposta do morador?", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(6.dp))
                Text("Registre como a entrada foi confirmada.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.ext.muted)
                Spacer(Modifier.height(16.dp))
                Button(onClick = { liberarComo = false; vm.liberar(ViaLiberacao.INTERFONE) }, modifier = Modifier.fillMaxWidth().height(50.dp)) {
                    Icon(Icons.Rounded.Call, contentDescription = null); Spacer(Modifier.size(8.dp)); Text("Morador confirmou pelo interfone")
                }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = { liberarComo = false }, modifier = Modifier.fillMaxWidth().height(50.dp)) { Text("Aguardar a resposta no app") }
            }
        }
    }
}

@Composable
private fun VisitanteAcoes(v: Visita, busy: Boolean, onLiberar: () -> Unit, onRecusar: () -> Unit, onSaida: () -> Unit, onConsultar: () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.surfaceContainerLowest, shadowElevation = 8.dp) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 16.dp, vertical = 12.dp)) {
            when {
                v.status == StatusVisita.DENTRO -> Button(
                    onClick = onSaida, enabled = !busy, modifier = Modifier.fillMaxWidth().height(52.dp).testTag("acao_saida"),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error, contentColor = MaterialTheme.colorScheme.onError)
                ) { BusyOr(busy) { Icon(Icons.AutoMirrored.Rounded.Logout, contentDescription = null); Spacer(Modifier.size(8.dp)); Text("Registrar saída") } }
                v.status.pendente -> {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        if (v.acessoId != null) OutlinedButton(onClick = onRecusar, enabled = !busy, modifier = Modifier.height(52.dp)) { Text("Recusar") }
                        else if (v.unidadeId != null && v.status == StatusVisita.ESPERADO) OutlinedButton(onClick = onConsultar, enabled = !busy, modifier = Modifier.height(52.dp)) {
                            Icon(Icons.Rounded.Send, contentDescription = null, modifier = Modifier.size(18.dp)); Spacer(Modifier.size(6.dp)); Text("Consultar")
                        }
                        Button(onClick = onLiberar, enabled = !busy, modifier = Modifier.weight(1f).height(52.dp).testTag("acao_liberar")) {
                            BusyOr(busy) { Icon(Icons.Rounded.DoorFront, contentDescription = null); Spacer(Modifier.size(8.dp)); Text("Liberar entrada") }
                        }
                    }
                }
                else -> Text(
                    if (v.status == StatusVisita.SAIU) "Visita encerrada às ${Fmt.hora(v.saidaEm)}." else "Entrada recusada.",
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.ext.muted, modifier = Modifier.padding(vertical = 8.dp)
                )
            }
        }
    }
}

@Composable
internal fun BusyOr(busy: Boolean, content: @Composable () -> Unit) {
    if (busy) CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.5.dp, color = MaterialTheme.colorScheme.onPrimary) else Row(verticalAlignment = Alignment.CenterVertically) { content() }
}

/* ------------------------------------------------------------------ Nova visita */

@Composable
fun NovaVisitaScreen(nav: PortariaNav, vm: NovaVisitaViewModel = hiltViewModel()) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    val unidades by vm.unidades.collectAsStateWithLifecycle()
    val done by vm.done.collectAsStateWithLifecycle()
    var picker by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(done) { if (done != null) nav.back() }

    Scaffold(
        topBar = { BackTopBar("Nova visita", onBack = nav.back) },
        bottomBar = {
            Surface(color = MaterialTheme.colorScheme.surfaceContainerLowest, shadowElevation = 8.dp) {
                Column(Modifier.fillMaxWidth().navigationBarsPadding().imePadding().padding(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { vm.salvar(consultar = false) }, enabled = ui.valido && !ui.saving, modifier = Modifier.fillMaxWidth().height(52.dp).testTag("nova_visita_liberar")) {
                        BusyOr(ui.saving) { Icon(Icons.Rounded.DoorFront, contentDescription = null); Spacer(Modifier.size(8.dp)); Text("Liberar entrada agora") }
                    }
                    if (ui.unidade != null) OutlinedButton(onClick = { vm.salvar(consultar = true) }, enabled = ui.valido && !ui.saving, modifier = Modifier.fillMaxWidth().height(48.dp)) {
                        Icon(Icons.Rounded.Send, contentDescription = null, modifier = Modifier.size(18.dp)); Spacer(Modifier.size(8.dp)); Text("Pedir ao morador pelo app")
                    }
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { p ->
        Column(Modifier.padding(p).fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
            Text("Cadastro rápido de quem chegou sem pré-autorização. O morador é avisado na hora.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.ext.muted)
            Spacer(Modifier.height(16.dp))
            FieldLabel("Unidade de destino")
            Surface(
                onClick = { picker = true }, shape = MaterialTheme.shapes.small, color = MaterialTheme.ext.card,
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline), modifier = Modifier.fillMaxWidth().testTag("nova_visita_unidade")
            ) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Apartment, contentDescription = null, tint = MaterialTheme.ext.muted)
                    Spacer(Modifier.size(10.dp))
                    Text(
                        when {
                            ui.unidade != null -> "${ui.unidade!!.rotulo} · ${ui.unidade!!.responsavel ?: "Sem morador"}"
                            ui.condominio -> "Condomínio (áreas comuns / administração)"
                            else -> "Escolher a unidade…"
                        },
                        color = if (ui.unidade != null || ui.condominio) MaterialTheme.ext.ink else MaterialTheme.ext.muted
                    )
                }
            }
            Spacer(Modifier.height(14.dp))
            OutlinedTextField(
                ui.nome, { v -> vm.update { it.copy(nome = v) } }, label = { Text("Nome do visitante") }, singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words), modifier = Modifier.fillMaxWidth().testTag("nova_visita_nome")
            )
            Spacer(Modifier.height(14.dp))
            FieldLabel("Tipo")
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TipoVisitante.entries.forEach { t ->
                    FilterChip(
                        selected = ui.tipo == t, onClick = { vm.update { it.copy(tipo = t) } }, label = { Text(t.label) },
                        leadingIcon = { Icon(t.icon, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = MaterialTheme.colorScheme.primary, selectedLabelColor = MaterialTheme.colorScheme.onPrimary, selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary)
                    )
                }
            }
            Spacer(Modifier.height(14.dp))
            OutlinedTextField(ui.documento, { v -> vm.update { it.copy(documento = v) } }, label = { Text("Documento (opcional)") }, placeholder = { Text("RG ou CPF") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    ui.placa, { v -> vm.update { it.copy(placa = v.uppercase().filter { c -> c.isLetterOrDigit() }.take(7)) } },
                    label = { Text("Placa (opcional)") }, singleLine = true, modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters)
                )
                OutlinedTextField(
                    ui.telefone, { v -> vm.update { it.copy(telefone = v) } }, label = { Text("Telefone") }, singleLine = true,
                    modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                )
            }
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(ui.obs, { v -> vm.update { it.copy(obs = v) } }, label = { Text("Observação (opcional)") }, placeholder = { Text("Ex.: técnico da internet") }, modifier = Modifier.fillMaxWidth())
            if (ui.error != null) {
                Spacer(Modifier.height(12.dp))
                NoteBox(Icons.Rounded.ErrorOutline, MaterialTheme.colorScheme.error, ui.error!!)
            }
            Spacer(Modifier.height(12.dp))
            NoteBox(Icons.Rounded.Info, MaterialTheme.colorScheme.primary, "Liberando agora, o visitante entra como \"No condomínio\" pelos próximos 4 h e fica no histórico do dia.")
            Spacer(Modifier.height(24.dp))
        }
    }

    if (picker) UnidadePicker(
        unidades = unidades, permitirCondominio = true,
        onPick = { u -> picker = false; vm.update { it.copy(unidade = u, condominio = u == null) } },
        onDismiss = { picker = false }
    )
}

@Composable
internal fun FieldLabel(text: String) {
    Text(text, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.ext.muted, modifier = Modifier.padding(bottom = 6.dp))
}

/** Escolher a unidade: busca por "B-204", "b204" ou pelo nome do morador. [onPick] com null = condomínio. */
@Composable
fun UnidadePicker(unidades: List<UnidadeResumo>, permitirCondominio: Boolean, onPick: (UnidadeResumo?) -> Unit, onDismiss: () -> Unit) {
    var q by rememberSaveable { mutableStateOf("") }
    val n = q.uppercase().filter { it.isLetterOrDigit() }
    val filtradas = unidades.filter { u ->
        n.isEmpty() || u.rotulo.uppercase().filter { it.isLetterOrDigit() }.contains(n) || u.responsavel?.uppercase()?.contains(q.trim().uppercase()) == true
    }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.fillMaxHeight(0.85f).padding(horizontal = 16.dp).imePadding()) {
            Text("Unidade de destino", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                q, { q = it }, placeholder = { Text("B-204 ou nome do morador") }, singleLine = true,
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                trailingIcon = { if (q.isNotEmpty()) IconButton(onClick = { q = "" }) { Icon(Icons.Rounded.Close, contentDescription = "Limpar") } },
                modifier = Modifier.fillMaxWidth().testTag("unidade_busca")
            )
            Spacer(Modifier.height(8.dp))
            LazyColumn(Modifier.weight(1f)) {
                if (permitirCondominio && n.isEmpty()) item {
                    PickRow("Condomínio", "Áreas comuns ou administração") { onPick(null) }
                }
                if (unidades.isEmpty()) item { LoadingBox() }
                items(filtradas, key = { it.id }) { u ->
                    PickRow(u.rotulo, listOfNotNull(u.responsavel ?: "Sem morador cadastrado", if (u.moradores > 1) "${u.moradores} moradores" else null).joinToString(" · ")) { onPick(u) }
                }
            }
            TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End).navigationBarsPadding()) { Text("Cancelar") }
        }
    }
}

@Composable
private fun PickRow(title: String, subtitle: String, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 12.dp, horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        IconBubble(Icons.Rounded.Apartment, MaterialTheme.colorScheme.primary, size = 38.dp)
        Spacer(Modifier.size(12.dp))
        Column {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.ext.ink)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.ext.muted)
        }
    }
}

/* ------------------------------------------------------------------ Bloqueados */

@Composable
fun BloqueadosScreen(nav: PortariaNav, vm: BloqueadosViewModel = hiltViewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    Scaffold(topBar = { BackTopBar("Lista de bloqueados", onBack = nav.back) }, containerColor = MaterialTheme.colorScheme.background) { p ->
        Column(Modifier.padding(p).fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
            NoteBox(Icons.Rounded.Shield, MaterialTheme.ext.danger, "Pessoas com entrada negada pelo síndico ou pela administração. Se alguém da lista chegar, avise a administração.")
            Spacer(Modifier.height(14.dp))
            val l = state.data
            when {
                l == null && state.loading -> LoadingBox()
                l == null -> ErrorBox(state.error ?: "Não deu para carregar.", onRetry = vm::refresh)
                l.isEmpty() -> EmptyBox(Icons.Rounded.Block, "Ninguém bloqueado", "A lista está vazia.")
                else -> CardList(l) { b ->
                    Row(Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        IconBubble(Icons.Rounded.Close, MaterialTheme.ext.danger)
                        Column(Modifier.weight(1f)) {
                            Text(b.nome, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.ext.ink)
                            Text(listOfNotNull(b.documento, b.desde?.let { "desde ${Fmt.data(it)}" }).joinToString(" · "), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.ext.muted)
                            if (b.motivo != null) Text(b.motivo, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.ext.ink, modifier = Modifier.padding(top = 4.dp))
                            if (b.por != null) Text("Decisão: ${b.por}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.ext.faint, modifier = Modifier.padding(top = 2.dp))
                        }
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}
