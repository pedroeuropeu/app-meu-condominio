package br.com.condovitta.app.feature.portaria

import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.IconButton
import androidx.compose.material.icons.rounded.Close
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.shape.RoundedCornerShape
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddAPhoto
import androidx.compose.material.icons.rounded.Apartment
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DoneAll
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material3.Button
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.condovitta.app.core.common.Fmt
import br.com.condovitta.app.core.common.Photos
import br.com.condovitta.app.core.designsystem.AppCard
import br.com.condovitta.app.core.designsystem.BackTopBar
import br.com.condovitta.app.core.designsystem.IconBubble
import br.com.condovitta.app.core.designsystem.SignaturePad
import br.com.condovitta.app.core.designsystem.StatusBadge
import br.com.condovitta.app.core.designsystem.ext
import br.com.condovitta.app.core.designsystem.rememberSignatureState
import br.com.condovitta.app.domain.Encomenda
import br.com.condovitta.app.domain.Entrega
import br.com.condovitta.app.domain.Portaria
import br.com.condovitta.app.domain.TamanhoEncomenda
import br.com.condovitta.app.domain.TipoRetirada
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/* ------------------------------------------------------------------ Encomendas (aba) */

@Composable
fun EncomendasScreen(nav: PortariaNav, vm: EncomendasViewModel = hiltViewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    val filtro by vm.filtro.collectAsStateWithLifecycle()
    var busca by rememberSaveable { mutableStateOf("") }
    LifecycleResumeEffect(Unit) {
        vm.refresh()
        onPauseOrDispose { }
    }
    Scaffold(
        topBar = { BackTopBar("Encomendas", onBack = nav.back) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = nav.novaEncomenda, icon = { Icon(Icons.Rounded.Inventory2, contentDescription = null) }, text = { Text("Registrar") },
                containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("fab_registrar_encomenda")
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { p ->
        val d = state.data
        PullToRefreshBox(isRefreshing = state.loading && d != null, onRefresh = vm::refresh, modifier = Modifier.padding(p).fillMaxSize()) {
            LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 96.dp)) {
                item {
                    // busca rápida: filtra a lista na hora (unidade, morador, transportadora, local, rastreio)
                    OutlinedTextField(
                        value = busca, onValueChange = { busca = it.take(60) },
                        placeholder = { Text("Unidade, morador ou transportadora") },
                        leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                        trailingIcon = { if (busca.isNotEmpty()) IconButton(onClick = { busca = "" }) { Icon(Icons.Rounded.Close, contentDescription = "Limpar busca") } },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.ext.card, unfocusedContainerColor = MaterialTheme.ext.card,
                            unfocusedBorderColor = MaterialTheme.ext.cardBorder
                        ),
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp).testTag("encomendas_busca")
                    )
                }
                item {
                    FilterChips(
                        listOf(
                            FiltroEncomendas.AGUARDANDO to "Aguardando retirada" + (d?.aguardando?.size?.let { " · $it" } ?: ""),
                            FiltroEncomendas.ENTREGUES to "Retiradas hoje" + (d?.entregues?.size?.let { " · $it" } ?: "")
                        ),
                        filtro, onSelect = { vm.filtro.value = it },
                        modifier = Modifier.horizontalScroll(rememberScrollState()).padding(vertical = 6.dp)
                    )
                }
                when {
                    d == null && state.loading -> item { LoadingBox() }
                    d == null -> item { ErrorBox(state.error ?: "Não deu para carregar.", onRetry = vm::refresh) }
                    else -> {
                        val list = Portaria.filtrarEncomendas(if (filtro == FiltroEncomendas.AGUARDANDO) d.aguardando else d.entregues, busca)
                        if (list.isEmpty() && busca.isNotBlank()) item {
                            val outra = Portaria.filtrarEncomendas(if (filtro == FiltroEncomendas.AGUARDANDO) d.entregues else d.aguardando, busca).size
                            EmptyBox(
                                Icons.Rounded.Search, "Nada com \"${busca.trim()}\"",
                                if (outra > 0) "Há $outra em \"${if (filtro == FiltroEncomendas.AGUARDANDO) "Retiradas hoje" else "Aguardando retirada"}\"." else "Confira a unidade ou o nome do morador."
                            )
                        } else if (list.isEmpty()) item {
                            EmptyBox(Icons.Rounded.Inventory2, if (filtro == FiltroEncomendas.AGUARDANDO) "Tudo entregue" else "Nenhuma retirada hoje", "Nenhuma encomenda neste filtro.")
                        } else item {
                            Spacer(Modifier.height(8.dp))
                            CardList(list) { e -> EncomendaRow(e) { nav.encomenda(e.id) } }
                            Spacer(Modifier.height(10.dp))
                            Text(
                                "Hoje: ${d.recebidasHoje} recebida(s) · ${d.entreguesHoje} entregue(s)",
                                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.ext.muted
                            )
                        }
                    }
                }
            }
        }
    }
}

/* ------------------------------------------------------------------ Detalhe da encomenda */

@Composable
fun EncomendaScreen(nav: PortariaNav, vm: EncomendaViewModel = hiltViewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    val foto by vm.foto.collectAsStateWithLifecycle()
    val busy by vm.busy.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { vm.refresh() }
    var entregar by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        topBar = { BackTopBar("Encomenda", onBack = nav.back) },
        bottomBar = {
            val e = state.data
            if (e != null && !e.retirada) Surface(color = MaterialTheme.colorScheme.surfaceContainerLowest, shadowElevation = 8.dp) {
                Row(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 16.dp, vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(onClick = vm::avisar, enabled = !busy, modifier = Modifier.height(52.dp)) {
                        Icon(Icons.Rounded.Notifications, contentDescription = null, modifier = Modifier.size(18.dp)); Spacer(Modifier.size(6.dp)); Text("Avisar")
                    }
                    Button(onClick = { entregar = true }, enabled = !busy, modifier = Modifier.weight(1f).height(52.dp).testTag("acao_entregar")) {
                        Icon(Icons.Rounded.DoneAll, contentDescription = null); Spacer(Modifier.size(8.dp)); Text("Registrar retirada")
                    }
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { p ->
        val e = state.data
        Column(Modifier.padding(p).fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
            when {
                e == null && state.loading -> LoadingBox()
                e == null -> ErrorBox(state.error ?: "Não deu para carregar.", onRetry = vm::refresh)
                else -> {
                    EncomendaCard(e, vm.codigoDemo)
                    Spacer(Modifier.height(18.dp))
                    Text("Foto na chegada", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.ext.ink)
                    Spacer(Modifier.height(8.dp))
                    val bmp = remember(foto) { Photos.decode(foto) }
                    if (bmp != null) Image(
                        bmp.asImageBitmap(), contentDescription = "Foto da encomenda", contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxWidth().height(220.dp).clip(MaterialTheme.shapes.large)
                    ) else PhotoPlaceholder(if (e.temFoto) "Carregando a foto…" else "Sem foto registrada", Icons.Rounded.PhotoCamera)
                    Spacer(Modifier.height(24.dp))
                }
            }
        }
    }

    val e = state.data
    if (entregar && e != null) EntregaSheet(e, vm, onDismiss = { entregar = false })
}

@Composable
private fun EncomendaCard(e: Encomenda, codigoDemo: String?) {
    AppCard {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                IconBubble(if (e.perecivel) Icons.Rounded.WaterDrop else Icons.Rounded.Inventory2, if (e.perecivel) Color(0xFF0891B2) else Color(0xFFD97706), size = 56.dp)
                Column(Modifier.weight(1f)) {
                    Text("${e.unidade} · ${e.destinatario ?: "Morador"}", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.ext.ink)
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (e.retirada) StatusBadge("Retirada", MaterialTheme.ext.success, MaterialTheme.ext.successContainer, icon = Icons.Rounded.Check)
                        else StatusBadge("Aguardando retirada", MaterialTheme.ext.warn, MaterialTheme.ext.warnContainer)
                        if (e.perecivel) StatusBadge("Perecível", MaterialTheme.ext.info, MaterialTheme.ext.infoContainer, icon = Icons.Rounded.WaterDrop)
                    }
                }
            }
            InfoRows(
                listOf(
                    "Transportadora" to e.transportadora,
                    "Tamanho" to e.tamanho?.label,
                    "Rastreio" to e.rastreio,
                    "Guardada em" to e.local,
                    "Recebida" to listOfNotNull(Fmt.quando(e.recebidaEm), e.recebidaPor?.let { "por $it" }).joinToString(" "),
                    if (e.retirada) "Retirada" to Fmt.quando(e.retiradaEm) else "Avisos ao morador" to "${e.avisos}" + (e.ultimoAvisoEm?.let { " · último ${Fmt.quando(it)}" } ?: ""),
                    "Retirada por" to e.retiradaPor?.let { listOfNotNull(it, e.retiradaDocumento, if (e.tipoRetirada == TipoRetirada.TERCEIRO) "terceiro" else null).joinToString(" · ") },
                    "Entregue por" to e.entreguePor,
                    "Código (demo)" to codigoDemo?.takeIf { !e.retirada }
                )
            )
        }
    }
}

@Composable
private fun PhotoPlaceholder(text: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: (() -> Unit)? = null) {
    Surface(
        onClick = onClick ?: {}, enabled = onClick != null, shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.ext.cardBorder), modifier = Modifier.fillMaxWidth().height(140.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.ext.muted, modifier = Modifier.size(32.dp))
            Spacer(Modifier.height(6.dp))
            Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.ext.muted)
        }
    }
}

/** Registrar a retirada: o morador mostra o código do app; outra pessoa informa nome e documento. Sempre com assinatura. */
@Composable
private fun EntregaSheet(e: Encomenda, vm: EncomendaViewModel, onDismiss: () -> Unit) {
    val busy by vm.busy.collectAsStateWithLifecycle()
    val erro by vm.entregaErro.collectAsStateWithLifecycle()
    val entregue by vm.entregue.collectAsStateWithLifecycle()
    val dest by vm.destinatarios.collectAsStateWithLifecycle()
    val inicial = remember { entregue }
    LaunchedEffect(entregue) { if (entregue != inicial) onDismiss() }

    var modo by rememberSaveable { mutableStateOf(TipoRetirada.MORADOR) }
    var codigo by rememberSaveable { mutableStateOf("") }
    var nome by rememberSaveable { mutableStateOf("") }
    var documento by rememberSaveable { mutableStateOf("") }
    val assinatura = rememberSignatureState()
    var faltaAssinar by remember { mutableStateOf(false) }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 20.dp).navigationBarsPadding().imePadding().verticalScroll(rememberScrollState())) {
            Text("Registrar retirada", style = MaterialTheme.typography.titleLarge)
            Text("${e.unidade} · ${e.transportadora} · ${e.local ?: ""}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.ext.muted)
            Spacer(Modifier.height(14.dp))
            FieldLabel("Quem está retirando?")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(modo == TipoRetirada.MORADOR, { modo = TipoRetirada.MORADOR }, label = { Text("${e.destinatario ?: "Morador"} (morador)") }, colors = chipColors())
                FilterChip(modo == TipoRetirada.TERCEIRO, { modo = TipoRetirada.TERCEIRO }, label = { Text("Outra pessoa") }, colors = chipColors())
            }
            Spacer(Modifier.height(12.dp))
            if (modo == TipoRetirada.MORADOR) {
                OutlinedTextField(
                    codigo, { codigo = it.filter(Char::isDigit).take(6) }, label = { Text("Código de retirada") },
                    supportingText = { Text("O morador vê o código no app, em Encomendas." + (vm.codigoDemo?.let { " (Demo: $it)" } ?: "")) },
                    singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    modifier = Modifier.fillMaxWidth().testTag("entrega_codigo")
                )
            } else {
                val autorizados = dest?.autorizados.orEmpty()
                if (autorizados.isNotEmpty()) {
                    Text("Autorizados pelo morador", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.ext.muted)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        autorizados.forEach { a ->
                            FilterChip(nome == a.nome, { nome = a.nome; documento = a.documento.orEmpty() }, label = { Text(listOfNotNull(a.nome, a.relacao).joinToString(" · ")) }, colors = chipColors())
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                }
                OutlinedTextField(nome, { nome = it }, label = { Text("Nome de quem retira") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(documento, { documento = it }, label = { Text("Documento (RG ou CPF)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            }
            Spacer(Modifier.height(14.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                FieldLabel("Assinatura de quem retirou")
                Spacer(Modifier.weight(1f))
                TextButton(onClick = { assinatura.clear() }) { Text("Limpar") }
            }
            SignaturePad(assinatura)
            val msg = erro ?: if (faltaAssinar) "Peça para assinar no quadro antes de confirmar." else null
            if (msg != null) {
                Spacer(Modifier.height(10.dp))
                NoteBox(Icons.Rounded.ErrorOutline, MaterialTheme.colorScheme.error, msg)
            }
            Spacer(Modifier.height(16.dp))
            val pronto = if (modo == TipoRetirada.MORADOR) codigo.length >= 4 else nome.isNotBlank() && documento.isNotBlank()
            val scope = rememberCoroutineScope()
            Button(
                onClick = {
                    scope.launch {
                        val png = withContext(Dispatchers.Default) { assinatura.toPng() }
                        faltaAssinar = png == null
                        if (png != null) vm.entregar(
                            if (modo == TipoRetirada.MORADOR) Entrega(TipoRetirada.MORADOR, codigo = codigo, assinatura = png)
                            else Entrega(TipoRetirada.TERCEIRO, nome = nome, documento = documento, assinatura = png)
                        )
                    }
                },
                enabled = pronto && !busy, modifier = Modifier.fillMaxWidth().height(52.dp).testTag("entrega_confirmar")
            ) { BusyOr(busy) { Icon(Icons.Rounded.DoneAll, contentDescription = null); Spacer(Modifier.size(8.dp)); Text("Confirmar entrega") } }
        }
    }
}

@Composable
private fun chipColors() = FilterChipDefaults.filterChipColors(selectedContainerColor = MaterialTheme.colorScheme.primary, selectedLabelColor = MaterialTheme.colorScheme.onPrimary)

/* ------------------------------------------------------------------ Registrar encomenda */

/**
 * Registrar encomenda em 2 passos, com botões grandes: (1) a unidade no teclado (bloco + número, confere o nome do
 * morador); (2) a foto e a transportadora. Gera o código de retirada e avisa o morador.
 */
@Composable
fun NovaEncomendaScreen(nav: PortariaNav, vm: NovaEncomendaViewModel = hiltViewModel()) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    val unidades by vm.unidades.collectAsStateWithLifecycle()
    val done by vm.done.collectAsStateWithLifecycle()
    var passo by rememberSaveable { mutableStateOf(1) }
    var bloco by rememberSaveable { mutableStateOf<String?>(null) }
    var numero by rememberSaveable { mutableStateOf("") }
    var picker by rememberSaveable { mutableStateOf(false) }
    var mais by rememberSaveable { mutableStateOf(false) }
    var captureUri by rememberSaveable { mutableStateOf<Uri?>(null) }
    var processando by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    LaunchedEffect(done) { if (done) nav.back() }

    val blocos = remember(unidades) { unidades.mapNotNull { it.bloco?.takeIf(String::isNotBlank) }.distinct().sorted() }
    LaunchedEffect(blocos) { if (bloco == null && blocos.size == 1) bloco = blocos.first() }
    val achada = remember(unidades, bloco, numero) { Portaria.acharUnidade(unidades, bloco, numero) }
    LaunchedEffect(achada?.id) { if (achada != null && ui.unidade?.id != achada.id) vm.unidade(achada) }

    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        val uri = captureUri
        if (ok && uri != null) scope.launch {
            processando = true
            val bytes = withContext(Dispatchers.IO) { Photos.compress(context, uri) }
            processando = false
            vm.update { it.copy(foto = bytes ?: it.foto) }
        }
    }
    fun tirarFoto() {
        val uri = Photos.newCaptureUri(context)
        captureUri = uri
        runCatching { camera.launch(uri) }
    }

    Scaffold(
        topBar = { BackTopBar("Registrar encomenda", onBack = { if (passo == 2) passo = 1 else nav.back() }) },
        bottomBar = {
            Surface(color = MaterialTheme.colorScheme.background) {
                val mod = Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 16.dp, vertical = 12.dp).height(58.dp)
                if (passo == 1) {
                    Button(onClick = { passo = 2 }, enabled = ui.unidade != null, modifier = mod.testTag("encomenda_continuar")) {
                        Text("Continuar", style = MaterialTheme.typography.titleMedium)
                    }
                } else {
                    Button(onClick = vm::salvar, enabled = ui.valido && !ui.saving, modifier = mod.testTag("encomenda_salvar")) {
                        BusyOr(ui.saving) { Icon(Icons.Rounded.NotificationsActive, contentDescription = null); Spacer(Modifier.size(8.dp)); Text("Registrar e avisar", style = MaterialTheme.typography.titleMedium) }
                    }
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { p ->
        Column(Modifier.padding(p).fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
            Text(
                if (passo == 1) "Passo 1 de 2 · Para qual unidade?" else "Passo 2 de 2 · Foto e transportadora",
                style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.ext.muted
            )
            Spacer(Modifier.height(12.dp))
            if (passo == 1) {
                if (blocos.size > 1) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        blocos.forEach { b ->
                            Tecla(b, selecionada = bloco == b, modifier = Modifier.weight(1f)) { bloco = b; numero = "" }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                }
                AppCard {
                    Column(Modifier.fillMaxWidth().padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            listOfNotNull(bloco, numero.ifEmpty { "—" }).joinToString("-"),
                            style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold, color = MaterialTheme.ext.ink,
                            modifier = Modifier.testTag("encomenda_unidade_digitada")
                        )
                        val u = ui.unidade
                        Text(
                            when {
                                u != null && achada != null -> listOfNotNull(u.rotulo, ui.destinatarios.joinToString().ifBlank { u.responsavel ?: "Sem morador cadastrado" }).joinToString(" · ")
                                numero.isNotEmpty() -> "Unidade não encontrada"
                                else -> "Digite o número do apartamento"
                            },
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (achada != null) MaterialTheme.colorScheme.primary else MaterialTheme.ext.muted,
                            textAlign = TextAlign.Center
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                listOf(listOf("1", "2", "3"), listOf("4", "5", "6"), listOf("7", "8", "9"), listOf("⌫", "0", "Limpar")).forEach { linha ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        linha.forEach { k ->
                            Tecla(k, modifier = Modifier.weight(1f)) {
                                numero = when (k) { "⌫" -> numero.dropLast(1); "Limpar" -> ""; else -> (numero + k).take(5) }
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
                TextButton(onClick = { picker = true }, modifier = Modifier.align(Alignment.CenterHorizontally)) { Text("Procurar na lista de unidades") }
            } else {
                val bmp = remember(ui.foto) { Photos.decode(ui.foto) }
                if (bmp != null) {
                    Box {
                        Image(bmp.asImageBitmap(), contentDescription = "Foto do pacote", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxWidth().height(220.dp).clip(MaterialTheme.shapes.large))
                        TextButton(onClick = ::tirarFoto, modifier = Modifier.align(Alignment.BottomEnd).padding(8.dp)) {
                            Surface(color = Color.Black.copy(alpha = 0.55f), shape = MaterialTheme.shapes.small) { Text("Refazer", color = Color.White, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)) }
                        }
                    }
                } else PhotoPlaceholder(if (processando) "Preparando a foto…" else "Tirar foto do pacote", Icons.Rounded.AddAPhoto, onClick = ::tirarFoto)

                Spacer(Modifier.height(16.dp))
                FieldLabel("Transportadora")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Portaria.TRANSPORTADORAS.forEach { t ->
                        FilterChip(ui.transportadora == t, { vm.update { it.copy(transportadora = t) } }, label = { Text(t, style = MaterialTheme.typography.bodyLarge) }, colors = chipColors(), modifier = Modifier.height(44.dp))
                    }
                }
                Spacer(Modifier.height(12.dp))
                Surface(shape = MaterialTheme.shapes.medium, color = Color(0xFF0891B2).copy(alpha = 0.10f), modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(horizontal = 14.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.WaterDrop, contentDescription = null, tint = Color(0xFF0891B2))
                        Text("É perecível (vai para a geladeira)", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f).padding(start = 10.dp))
                        Switch(ui.perecivel, vm::perecivel)
                    }
                }
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = { mais = !mais }) { Text(if (mais) "Menos detalhes" else "Mais detalhes (destinatário, tamanho, local, rastreio)") }
                if (mais) {
                    if (ui.destinatarios.size > 1) {
                        FieldLabel("Destinatário")
                        ChipRow(ui.destinatarios, ui.destinatario) { d -> vm.update { it.copy(destinatario = d) } }
                        Spacer(Modifier.height(12.dp))
                    }
                    FieldLabel("Tamanho")
                    ChipRow(TamanhoEncomenda.entries.map { it.label }, ui.tamanho?.label) { l -> vm.update { it.copy(tamanho = TamanhoEncomenda.entries.first { t -> t.label == l }) } }
                    Spacer(Modifier.height(12.dp))
                    FieldLabel("Onde vai guardar")
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Portaria.LOCAIS.forEach { l -> FilterChip(ui.local == l, { vm.update { it.copy(local = l) } }, label = { Text(l) }, colors = chipColors()) }
                    }
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(ui.rastreio, { v -> vm.update { it.copy(rastreio = v) } }, label = { Text("Código de rastreio (opcional)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                }
                if (ui.error != null) {
                    Spacer(Modifier.height(12.dp))
                    NoteBox(Icons.Rounded.ErrorOutline, MaterialTheme.colorScheme.error, ui.error!!)
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }

    if (picker) UnidadePicker(unidades, permitirCondominio = false, onPick = { u ->
        picker = false
        u?.let { bloco = it.bloco; numero = Portaria.numeroDa(it); vm.unidade(it) }
    }, onDismiss = { picker = false })
}

/** Tecla grande do teclado de unidade. */
@Composable
private fun Tecla(texto: String, modifier: Modifier = Modifier, selecionada: Boolean = false, onClick: () -> Unit) {
    Surface(
        onClick = onClick, shape = RoundedCornerShape(16.dp),
        color = if (selecionada) MaterialTheme.colorScheme.primary else MaterialTheme.ext.card,
        border = if (selecionada) null else BorderStroke(1.dp, MaterialTheme.ext.cardBorder),
        modifier = modifier.height(58.dp).testTag("tecla_$texto")
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                texto, style = if (texto.length > 2) MaterialTheme.typography.titleSmall else MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold, color = if (selecionada) MaterialTheme.colorScheme.onPrimary else MaterialTheme.ext.ink
            )
        }
    }
}

@Composable
private fun ChipRow(options: List<String>, selected: String?, onSelect: (String) -> Unit) {
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { o -> FilterChip(selected == o, { onSelect(o) }, label = { Text(o, fontWeight = if (selected == o) FontWeight.SemiBold else null) }, colors = chipColors()) }
    }
}
