package br.com.condovitta.app.feature.morador

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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Campaign
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.TaskAlt
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.condovitta.app.core.common.Fmt
import br.com.condovitta.app.core.designsystem.BackTopBar
import br.com.condovitta.app.core.designsystem.IconBubble
import br.com.condovitta.app.core.designsystem.SectionHeader
import br.com.condovitta.app.core.designsystem.StatusBadge
import br.com.condovitta.app.core.designsystem.ext
import br.com.condovitta.app.domain.Aviso
import br.com.condovitta.app.domain.EscopoAutorizado
import br.com.condovitta.app.feature.portaria.BusyOr
import br.com.condovitta.app.feature.portaria.CardList
import br.com.condovitta.app.feature.portaria.EmptyBox
import br.com.condovitta.app.feature.portaria.ErrorBox
import br.com.condovitta.app.feature.portaria.FieldLabel
import br.com.condovitta.app.feature.portaria.LoadingBox
import br.com.condovitta.app.feature.portaria.NoteBox

/* ------------------------------------------------------------------ Encomendas (morador) */

@Composable
fun EncomendasMoradorScreen(nav: MoradorNav, vm: EncomendasMoradorViewModel = hiltViewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    var autorizar by rememberSaveable { mutableStateOf(false) }
    LifecycleResumeEffect(Unit) {
        vm.refresh()
        onPauseOrDispose { }
    }
    Scaffold(topBar = { BackTopBar("Encomendas", onBack = nav.back) }, containerColor = MaterialTheme.colorScheme.background) { p ->
        val d = state.data
        PullToRefreshBox(isRefreshing = state.loading && d != null, onRefresh = { vm.refresh() }, modifier = Modifier.padding(p).fillMaxSize()) {
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
                when {
                    d == null && state.loading -> LoadingBox()
                    d == null -> ErrorBox(state.error ?: "Não deu para carregar.", onRetry = { vm.refresh() })
                    else -> {
                        if (d.aguardando.isEmpty()) EmptyBox(Icons.Rounded.Inventory2, "Nada para retirar", "Quando chegar uma encomenda, ela aparece aqui com o código de retirada.")
                        else {
                            SectionHeader("Aguardando retirada", subtitle = "Mostre o código na portaria")
                            CardList(d.aguardando) { e ->
                                Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    IconBubble(Icons.Rounded.Inventory2, Color(0xFFD97706))
                                    Column(Modifier.weight(1f)) {
                                        Text("${e.transportadora} · ${e.tamanho?.label ?: ""}", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.ext.ink)
                                        Text("Chegou ${Fmt.quando(e.recebidaEm)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.ext.muted)
                                        Text("Guardada em ${e.local ?: "portaria"}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.ext.muted)
                                    }
                                    e.codigo?.let { CodigoBox(it, "CÓDIGO") }
                                }
                            }
                        }
                        SectionHeader("Quem pode retirar", subtitle = "Além de você, a portaria entrega para estas pessoas")
                        CardList(d.autorizados) { a ->
                            Row(Modifier.fillMaxWidth().padding(start = 14.dp, top = 10.dp, bottom = 10.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                IconBubble(Icons.Rounded.Person, MaterialTheme.colorScheme.primary, size = 38.dp)
                                Column(Modifier.weight(1f)) {
                                    Text(a.nome, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.ext.ink)
                                    Text(
                                        listOfNotNull(a.relacao, when (a.escopo) {
                                            EscopoAutorizado.SEMPRE -> "sempre"
                                            EscopoAutorizado.ATE -> a.validoAte?.let { "até ${Fmt.data(it)}" }
                                            EscopoAutorizado.ESTA -> "só uma encomenda"
                                        }).joinToString(" · "),
                                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.ext.muted
                                    )
                                }
                                if (!a.fixo) IconButton(onClick = { vm.remover(a.id) }) { Icon(Icons.Rounded.Close, contentDescription = "Remover ${a.nome}", tint = MaterialTheme.ext.muted) }
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                        OutlinedButton(onClick = { autorizar = true }, modifier = Modifier.fillMaxWidth().height(48.dp)) {
                            Icon(Icons.Rounded.PersonAdd, contentDescription = null, modifier = Modifier.size(18.dp)); Spacer(Modifier.size(8.dp)); Text("Autorizar outra pessoa a retirar")
                        }
                        if (d.retiradas.isNotEmpty()) {
                            SectionHeader("Retiradas")
                            CardList(d.retiradas) { e ->
                                Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    IconBubble(Icons.Rounded.CheckCircle, MaterialTheme.ext.success)
                                    Column(Modifier.weight(1f)) {
                                        Text("${e.transportadora} · ${e.tamanho?.label ?: ""}", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.ext.ink)
                                        Text("Retirada ${Fmt.quando(e.retiradaEm)} por ${e.retiradaPor ?: "—"}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.ext.muted)
                                    }
                                }
                            }
                        }
                        Spacer(Modifier.height(32.dp))
                    }
                }
            }
        }
    }
    if (autorizar) AutorizarRetiradaSheet(vm, onDismiss = { autorizar = false })
}

@Composable
private fun AutorizarRetiradaSheet(vm: EncomendasMoradorViewModel, onDismiss: () -> Unit) {
    val salvando by vm.salvando.collectAsStateWithLifecycle()
    val erro by vm.erroAutorizado.collectAsStateWithLifecycle()
    val ok by vm.autorizadoOk.collectAsStateWithLifecycle()
    val inicial = remember { ok }
    LaunchedEffect(ok) { if (ok != inicial) onDismiss() }
    var nome by rememberSaveable { mutableStateOf("") }
    var documento by rememberSaveable { mutableStateOf("") }
    var relacao by rememberSaveable { mutableStateOf("") }
    var escopo by rememberSaveable { mutableStateOf(EscopoAutorizado.SEMPRE) }
    var ate by rememberSaveable { mutableStateOf<java.time.LocalDate?>(null) }
    var escolherData by remember { mutableStateOf(false) }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 20.dp).navigationBarsPadding().imePadding().verticalScroll(rememberScrollState())) {
            Text("Autorizar retirada", style = MaterialTheme.typography.titleLarge)
            Text("A portaria confere o nome e o documento de quem vier buscar.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.ext.muted)
            Spacer(Modifier.height(14.dp))
            OutlinedTextField(nome, { nome = it }, label = { Text("Nome") }, singleLine = true, keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words), modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(documento, { documento = it }, label = { Text("Documento (RG ou CPF)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(relacao, { relacao = it }, label = { Text("Relação (opcional)") }, placeholder = { Text("Ex.: sogra, babá") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(12.dp))
            FieldLabel("Por quanto tempo")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(EscopoAutorizado.SEMPRE to "Sempre", EscopoAutorizado.ATE to "Até uma data").forEach { (e, l) ->
                    FilterChip(escopo == e, { escopo = e; if (e == EscopoAutorizado.ATE && ate == null) escolherData = true }, label = { Text(l) },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = MaterialTheme.colorScheme.primary, selectedLabelColor = MaterialTheme.colorScheme.onPrimary))
                }
            }
            if (escopo == EscopoAutorizado.ATE) OutlinedButton(onClick = { escolherData = true }, modifier = Modifier.fillMaxWidth()) { Text(ate?.let { "Até ${Fmt.data(it)}" } ?: "Escolher a data") }
            erro?.let { Spacer(Modifier.height(10.dp)); NoteBox(Icons.Rounded.ErrorOutline, MaterialTheme.colorScheme.error, it) }
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = { vm.autorizar(nome, documento, relacao, escopo, ate.takeIf { escopo == EscopoAutorizado.ATE }) },
                enabled = nome.trim().length >= 3 && !salvando && (escopo != EscopoAutorizado.ATE || ate != null),
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) { BusyOr(salvando) { Text("Autorizar") } }
        }
    }
    if (escolherData) DataDialog(ate, java.time.LocalDate.now(), onOk = { ate = it; escolherData = false }, onDismiss = { escolherData = false })
}

/* ------------------------------------------------------------------ Avisos */

@Composable
fun AvisosScreen(nav: MoradorNav, vm: AvisosViewModel = hiltViewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    val aberto by vm.aberto.collectAsStateWithLifecycle()
    LifecycleResumeEffect(Unit) {
        vm.refresh()
        onPauseOrDispose { }
    }
    Scaffold(topBar = { BackTopBar("Avisos", onBack = nav.back) }, containerColor = MaterialTheme.colorScheme.background) { p ->
        val l = state.data
        PullToRefreshBox(isRefreshing = state.loading && l != null, onRefresh = { vm.refresh() }, modifier = Modifier.padding(p).fillMaxSize()) {
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
                when {
                    l == null && state.loading -> LoadingBox()
                    l == null -> ErrorBox(state.error ?: "Não deu para carregar.", onRetry = { vm.refresh() })
                    l.isEmpty() -> EmptyBox(Icons.Rounded.Campaign, "Mural vazio", "Os comunicados do condomínio aparecem aqui.")
                    else -> {
                        Spacer(Modifier.height(8.dp))
                        CardList(l) { a -> AvisoRow(a) { vm.abrir(a) } }
                        Spacer(Modifier.height(32.dp))
                    }
                }
            }
        }
    }
    aberto?.let { a -> AvisoSheet(a, onCiente = { vm.ciente(a) }, onDismiss = vm::fechar) }
}

@Composable
private fun AvisoRow(a: Aviso, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        IconBubble(if (a.fixado) Icons.Rounded.PushPin else Icons.Rounded.Campaign, if (a.fixado) MaterialTheme.ext.danger else Color(0xFF0D9488))
        Column(Modifier.weight(1f)) {
            Text(a.titulo, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.ext.ink, fontWeight = if (a.lido) FontWeight.Medium else FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(listOfNotNull(a.categoria, Fmt.quando(a.publicadoEm), a.autor).joinToString(" · "), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.ext.muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (a.exigeCiencia && !a.ciente) Text("Pede confirmação de ciência", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.ext.warn)
        }
        if (!a.lido) Box(Modifier.size(10.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary).testTag("nao_lido"))
    }
}

@Composable
private fun AvisoSheet(a: Aviso, onCiente: () -> Unit, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.padding(horizontal = 22.dp).padding(bottom = 24.dp).navigationBarsPadding().verticalScroll(rememberScrollState())) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                StatusBadge(a.categoria, MaterialTheme.ext.info, MaterialTheme.ext.infoContainer, icon = Icons.Rounded.Campaign)
                if (a.fixado) StatusBadge("Fixado", MaterialTheme.ext.danger, MaterialTheme.ext.dangerContainer, icon = Icons.Rounded.PushPin)
            }
            Spacer(Modifier.height(12.dp))
            Text(a.titulo, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.ext.ink)
            Text(listOfNotNull(a.autor, Fmt.quando(a.publicadoEm)).joinToString(" · "), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.ext.muted)
            Spacer(Modifier.height(14.dp))
            Text(a.texto, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
            if (a.exigeCiencia) {
                Spacer(Modifier.height(20.dp))
                if (a.ciente) NoteBox(Icons.Rounded.TaskAlt, MaterialTheme.ext.success, "Você confirmou ciência" + (a.cienteEm?.let { " em ${Fmt.quando(it)}" } ?: "") + ".")
                else Button(onClick = onCiente, modifier = Modifier.fillMaxWidth().height(52.dp).testTag("aviso_ciente")) {
                    Icon(Icons.Rounded.TaskAlt, contentDescription = null); Spacer(Modifier.size(8.dp)); Text("Estou ciente")
                }
            }
        }
    }
}
