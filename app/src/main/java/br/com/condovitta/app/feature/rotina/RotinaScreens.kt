package br.com.condovitta.app.feature.rotina

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Login
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Pets
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.ReportProblem
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.vector.ImageVector
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
import br.com.condovitta.app.core.designsystem.SectionHeader
import br.com.condovitta.app.core.designsystem.StatusBadge
import br.com.condovitta.app.core.designsystem.ext
import br.com.condovitta.app.domain.LeituraGas
import br.com.condovitta.app.domain.SentidoGaragem
import br.com.condovitta.app.domain.TipoRegistro
import br.com.condovitta.app.domain.Veiculo
import br.com.condovitta.app.feature.portaria.BusyOr
import br.com.condovitta.app.feature.portaria.CardList
import br.com.condovitta.app.feature.portaria.EmptyBox
import br.com.condovitta.app.feature.portaria.ErrorBox
import br.com.condovitta.app.feature.portaria.Kpi
import br.com.condovitta.app.feature.portaria.LoadingBox
import br.com.condovitta.app.feature.portaria.NoteBox
import br.com.condovitta.app.feature.portaria.Plate
import br.com.condovitta.app.feature.portaria.VisitaRow
import java.math.BigDecimal
import java.math.RoundingMode

private fun TipoRegistro.icone(): Pair<ImageVector, Color> = when (this) {
    TipoRegistro.OCORRENCIA -> Icons.Rounded.ReportProblem to Color(0xFFDC2626)
    TipoRegistro.RONDA -> Icons.Rounded.Shield to Color(0xFF2563EB)
    TipoRegistro.CHAVE -> Icons.Rounded.Key to Color(0xFF7C3AED)
    TipoRegistro.PRESTADOR -> Icons.Rounded.Build to Color(0xFFD97706)
    TipoRegistro.INFORMATIVO -> Icons.Rounded.Info to Color(0xFF0D9488)
    TipoRegistro.PASSAGEM -> Icons.Rounded.SwapHoriz to Color(0xFF64748B)
}

@Composable
private fun chipColors() = FilterChipDefaults.filterChipColors(selectedContainerColor = MaterialTheme.colorScheme.primary, selectedLabelColor = MaterialTheme.colorScheme.onPrimary)

/* ------------------------------------------------------------------ Livro da portaria */

@Composable
fun LivroScreen(onBack: () -> Unit, vm: LivroViewModel = hiltViewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    var novo by rememberSaveable { mutableStateOf(false) }
    LifecycleResumeEffect(Unit) {
        vm.refresh()
        onPauseOrDispose { }
    }
    Scaffold(
        topBar = { BackTopBar("Livro da portaria", onBack = onBack) },
        floatingActionButton = {
            ExtendedFloatingActionButton(onClick = { novo = true }, icon = { Icon(Icons.Rounded.Add, contentDescription = null) }, text = { Text("Registrar") },
                containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.testTag("livro_registrar"))
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { p ->
        val l = state.data
        PullToRefreshBox(isRefreshing = state.loading && l != null, onRefresh = { vm.refresh() }, modifier = Modifier.padding(p).fillMaxSize()) {
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
                when {
                    l == null && state.loading -> LoadingBox()
                    l == null -> ErrorBox(state.error ?: "Não deu para carregar.", onRetry = { vm.refresh() })
                    else -> {
                        if (l.chaves.isNotEmpty()) {
                            SectionHeader("Chaves emprestadas")
                            CardList(l.chaves) { c ->
                                Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    IconBubble(Icons.Rounded.Key, Color(0xFF7C3AED))
                                    Column(Modifier.weight(1f)) {
                                        Text(c.chave, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.ext.ink)
                                        Text(listOfNotNull(c.com, c.desde?.let { "desde ${Fmt.hora(it)}" }, c.prevista?.let { "volta $it" }).joinToString(" · "),
                                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.ext.muted)
                                    }
                                    TextButton(onClick = { vm.devolverChave(c.id) }) { Text("Devolvida") }
                                }
                            }
                        }
                        SectionHeader("Registros do dia")
                        if (l.registros.isEmpty()) EmptyBox(Icons.AutoMirrored.Rounded.MenuBook, "Livro vazio", "Ocorrências, rondas e prestadores do turno aparecem aqui.")
                        else CardList(l.registros) { r ->
                            val (ic, cor) = r.tipo.icone()
                            Row(Modifier.fillMaxWidth().padding(start = 14.dp, top = 12.dp, bottom = 12.dp, end = 4.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                IconBubble(ic, cor)
                                Column(Modifier.weight(1f)) {
                                    Text(listOfNotNull(Fmt.hora(r.quando), r.tipo.label, r.unidade).joinToString(" · "), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.ext.muted)
                                    Text(r.texto, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.ext.ink)
                                    Text(listOfNotNull(r.autor, if (r.avisouSindico) "síndico avisado" else null).joinToString(" · "), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.ext.faint)
                                }
                                if (!r.travado) IconButton(onClick = { vm.excluir(r.id) }) { Icon(Icons.Rounded.Close, contentDescription = "Apagar registro", tint = MaterialTheme.ext.faint) }
                            }
                        }
                        Spacer(Modifier.height(96.dp))
                    }
                }
            }
        }
    }
    if (novo) NovoRegistroSheet(vm, onDismiss = { novo = false })
}

@Composable
private fun NovoRegistroSheet(vm: LivroViewModel, onDismiss: () -> Unit) {
    val salvando by vm.salvando.collectAsStateWithLifecycle()
    val erro by vm.erro.collectAsStateWithLifecycle()
    val salvo by vm.salvo.collectAsStateWithLifecycle()
    val inicial = remember { salvo }
    LaunchedEffect(salvo) { if (salvo != inicial) onDismiss() }
    var tipo by rememberSaveable { mutableStateOf(TipoRegistro.OCORRENCIA) }
    var texto by rememberSaveable { mutableStateOf("") }
    var avisar by rememberSaveable { mutableStateOf(false) }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 20.dp).navigationBarsPadding().imePadding()) {
            Text("Novo registro", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(10.dp))
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(TipoRegistro.OCORRENCIA, TipoRegistro.RONDA, TipoRegistro.PRESTADOR, TipoRegistro.INFORMATIVO).forEach { t ->
                    FilterChip(tipo == t, { tipo = t }, label = { Text(t.label) }, colors = chipColors())
                }
            }
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(texto, { texto = it.take(2000) }, label = { Text("O que aconteceu?") }, minLines = 3,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences), modifier = Modifier.fillMaxWidth().testTag("livro_texto"))
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 6.dp)) {
                Text("Avisar o síndico", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                Switch(avisar, { avisar = it })
            }
            erro?.let { Spacer(Modifier.height(8.dp)); NoteBox(Icons.Rounded.ErrorOutline, MaterialTheme.colorScheme.error, it) }
            Spacer(Modifier.height(12.dp))
            Button(onClick = { vm.registrar(tipo, texto, avisar) }, enabled = texto.trim().length >= 8 && !salvando, modifier = Modifier.fillMaxWidth().height(50.dp)) {
                BusyOr(salvando) { Text("Registrar no livro") }
            }
        }
    }
}

/* ------------------------------------------------------------------ Veículos e placas */

@Composable
fun VeiculosScreen(onBack: () -> Unit, vm: VeiculosViewModel = hiltViewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    val consulta by vm.consulta.collectAsStateWithLifecycle()
    var placa by rememberSaveable { mutableStateOf("") }
    LifecycleResumeEffect(Unit) {
        vm.refresh()
        onPauseOrDispose { }
    }
    Scaffold(topBar = { BackTopBar("Veículos e placas", onBack = onBack) }, containerColor = MaterialTheme.colorScheme.background) { p ->
        val d = state.data
        PullToRefreshBox(isRefreshing = state.loading && d != null, onRefresh = { vm.refresh() }, modifier = Modifier.padding(p).fillMaxSize()) {
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
                OutlinedTextField(
                    placa, { v -> placa = v.uppercase().filter { it.isLetterOrDigit() }.take(7); vm.consultar(placa) },
                    label = { Text("Placa") }, placeholder = { Text("ABC1D23") }, singleLine = true,
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters), modifier = Modifier.fillMaxWidth().testTag("veiculo_placa")
                )
                consulta?.let { c ->
                    Spacer(Modifier.height(10.dp))
                    when {
                        c.loading -> LoadingBox()
                        c.error != null -> NoteBox(Icons.Rounded.ErrorOutline, MaterialTheme.ext.warn, c.error)
                        c.data != null -> {
                            val r = c.data
                            if (r.moradores.isEmpty() && r.visitantes.isEmpty()) NoteBox(Icons.Rounded.Warning, MaterialTheme.ext.warn, "Placa ${r.placa} não está cadastrada nem autorizada hoje.")
                            r.moradores.forEach { v -> VeiculoCard(v) }
                            if (r.visitantes.isNotEmpty()) CardList(r.visitantes) { v -> VisitaRow(v) {} }
                            Spacer(Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Button(onClick = { vm.registrar(r.placa, SentidoGaragem.ENTRADA) }, modifier = Modifier.weight(1f)) {
                                    Icon(Icons.AutoMirrored.Rounded.Login, contentDescription = null, modifier = Modifier.size(18.dp)); Spacer(Modifier.size(6.dp)); Text("Entrada")
                                }
                                OutlinedButton(onClick = { vm.registrar(r.placa, SentidoGaragem.SAIDA) }, modifier = Modifier.weight(1f)) {
                                    Icon(Icons.AutoMirrored.Rounded.Logout, contentDescription = null, modifier = Modifier.size(18.dp)); Spacer(Modifier.size(6.dp)); Text("Saída")
                                }
                            }
                        }
                    }
                }
                when {
                    d == null && state.loading -> LoadingBox()
                    d == null -> ErrorBox(state.error ?: "Não deu para carregar.", onRetry = { vm.refresh() })
                    else -> {
                        if (d.movimento.isNotEmpty()) {
                            SectionHeader("Movimento de hoje")
                            CardList(d.movimento) { m ->
                                Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Plate(m.placa)
                                    Column(Modifier.weight(1f)) {
                                        Text("${if (m.sentido == SentidoGaragem.ENTRADA) "Entrou" else "Saiu"} ${Fmt.hora(m.quando)}", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.ext.ink)
                                        Text(listOfNotNull(m.deQuem, m.portao).joinToString(" · "), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.ext.muted)
                                    }
                                }
                            }
                        }
                        SectionHeader("Veículos dos moradores")
                        d.moradores.forEach { v -> VeiculoCard(v) }
                        Spacer(Modifier.height(32.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun VeiculoCard(v: Veiculo) {
    AppCard(Modifier.padding(bottom = 10.dp)) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Plate(v.placa)
            Column(Modifier.weight(1f)) {
                Text(listOfNotNull(v.modelo, v.cor).joinToString(" "), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.ext.ink)
                Text(listOfNotNull(v.unidade, v.morador, v.vaga?.let { "vaga $it" }, v.acesso).joinToString(" · "), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.ext.muted)
                if (v.bloqueado && v.obs != null) Text(v.obs, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.ext.danger)
            }
            if (v.bloqueado) StatusBadge("Bloqueado", MaterialTheme.ext.danger, MaterialTheme.ext.dangerContainer)
        }
    }
}

/* ------------------------------------------------------------------ Leitura de gás */

@Composable
fun GasScreen(onBack: () -> Unit, vm: GasViewModel = hiltViewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    var lendo by rememberSaveable { mutableStateOf<Long?>(null) }
    LifecycleResumeEffect(Unit) {
        vm.refresh()
        onPauseOrDispose { }
    }
    Scaffold(topBar = { BackTopBar("Leitura de gás", onBack = onBack) }, containerColor = MaterialTheme.colorScheme.background) { p ->
        val g = state.data
        PullToRefreshBox(isRefreshing = state.loading && g != null, onRefresh = { vm.refresh() }, modifier = Modifier.padding(p).fillMaxSize()) {
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
                when {
                    g == null && state.loading -> LoadingBox()
                    g == null -> ErrorBox(state.error ?: "Não deu para carregar.", onRetry = { vm.refresh() })
                    else -> {
                        Text("Mês ${Fmt.competencia(g.mes)}" + if (g.fechado) " · fechado" else "", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.ext.muted)
                        Spacer(Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Kpi(Icons.Rounded.CheckCircle, Color(0xFF059669), "${g.lidas}/${g.total}", "Lidas", Modifier.weight(1f))
                            Kpi(Icons.Rounded.LocalFireDepartment, Color(0xFFDC2626), "${g.total - g.lidas}", "Pendentes", Modifier.weight(1f))
                            Kpi(Icons.Rounded.Warning, Color(0xFFD97706), "${g.alertas}", "Fora do padrão", Modifier.weight(1f))
                        }
                        if (g.fechado) { Spacer(Modifier.height(10.dp)); NoteBox(Icons.Rounded.Info, MaterialTheme.ext.info, "O mês já foi fechado pela administração: as leituras não podem mais ser alteradas.") }
                        SectionHeader("Unidades", subtitle = "Toque para lançar a leitura do medidor (m³)")
                        CardList(g.unidades) { l -> LeituraRow(l, enabled = !g.fechado) { lendo = l.unidadeId } }
                        Spacer(Modifier.height(32.dp))
                    }
                }
            }
        }
    }
    state.data?.unidades?.firstOrNull { it.unidadeId == lendo }?.let { l -> LeituraSheet(l, onSalvar = { v -> vm.lancar(l.unidadeId, v) { lendo = null } }, onDismiss = { lendo = null }) }
}

private fun m3(v: BigDecimal?) = v?.setScale(3, RoundingMode.HALF_UP)?.toPlainString()?.replace('.', ',') ?: "—"

@Composable
private fun LeituraRow(l: LeituraGas, enabled: Boolean, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(enabled = enabled, onClick = onClick).padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        IconBubble(Icons.Rounded.LocalFireDepartment, if (l.alerta != null) Color(0xFFD97706) else if (l.lida) Color(0xFF059669) else MaterialTheme.ext.muted, size = 40.dp)
        Column(Modifier.weight(1f)) {
            Text("${l.unidade} · ${l.morador ?: "—"}", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.ext.ink)
            Text(if (l.lida) "Anterior ${m3(l.anterior)} → atual ${m3(l.atual)} · consumo ${m3(l.consumo)} m³" else "Anterior ${m3(l.anterior)} m³ · falta ler",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.ext.muted)
            l.alerta?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.ext.warn, fontWeight = FontWeight.Medium) }
        }
        if (l.lida) Icon(Icons.Rounded.CheckCircle, contentDescription = "Lida", tint = MaterialTheme.ext.success)
    }
}

@Composable
private fun LeituraSheet(l: LeituraGas, onSalvar: (String) -> Unit, onDismiss: () -> Unit) {
    var valor by rememberSaveable { mutableStateOf(l.atual?.let(::m3)?.replace(".", "") ?: "") }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 20.dp).navigationBarsPadding().imePadding()) {
            Text("Leitura · ${l.unidade}", style = MaterialTheme.typography.titleLarge)
            Text("Leitura anterior: ${m3(l.anterior)} m³", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.ext.muted)
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(valor, { v -> valor = v.filter { it.isDigit() || it == ',' || it == '.' }.take(12) }, label = { Text("Leitura atual (m³)") }, singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth().testTag("gas_leitura"))
            Spacer(Modifier.height(12.dp))
            Button(onClick = { onSalvar(valor) }, enabled = valor.isNotBlank(), modifier = Modifier.fillMaxWidth().height(50.dp)) { Text("Lançar leitura") }
        }
    }
}

/* ------------------------------------------------------------------ Minha unidade (morador) */

@Composable
fun MinhaUnidadeScreen(onBack: () -> Unit, vm: MinhaUnidadeViewModel = hiltViewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    var novoPet by rememberSaveable { mutableStateOf(false) }
    var novoDom by rememberSaveable { mutableStateOf(false) }
    LifecycleResumeEffect(Unit) {
        vm.refresh()
        onPauseOrDispose { }
    }
    Scaffold(topBar = { BackTopBar("Minha unidade", onBack = onBack) }, containerColor = MaterialTheme.colorScheme.background) { p ->
        val u = state.data
        PullToRefreshBox(isRefreshing = state.loading && u != null, onRefresh = { vm.refresh() }, modifier = Modifier.padding(p).fillMaxSize()) {
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
                when {
                    u == null && state.loading -> LoadingBox()
                    u == null -> ErrorBox(state.error ?: "Não deu para carregar.", onRetry = { vm.refresh() })
                    else -> {
                        AppCard {
                            Column(Modifier.padding(16.dp)) {
                                Text(u.unidade, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.ext.ink)
                                Text(listOfNotNull(u.condominio, u.vagas?.let { "$it vaga(s)" }, u.vagasDescricao).joinToString(" · "), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.ext.muted)
                            }
                        }
                        SectionHeader("Moradores")
                        CardList(u.moradores) { m ->
                            Linha(Icons.Rounded.Person, MaterialTheme.colorScheme.primary, m.nome + if (m.titular) " · titular" else "",
                                listOfNotNull(m.vinculo, m.situacao, m.telefone).joinToString(" · "))
                        }
                        SectionHeader("Pets")
                        if (u.pets.isEmpty()) Text("Nenhum pet cadastrado.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.ext.muted)
                        else CardList(u.pets) { pet ->
                            Linha(Icons.Rounded.Pets, Color(0xFFD97706), pet.nome, listOfNotNull(pet.especie, pet.raca, pet.porte, pet.vacina?.let { "vacina ${Fmt.data(it)}" }).joinToString(" · "),
                                onRemover = if (u.possoGerenciar) ({ vm.removerPet(pet.id) }) else null)
                        }
                        if (u.possoGerenciar) TextButton(onClick = { novoPet = true }) { Icon(Icons.Rounded.Add, contentDescription = null); Text("Cadastrar pet") }
                        SectionHeader("Funcionários domésticos", subtitle = "A portaria libera a entrada deles")
                        if (u.domesticos.isEmpty()) Text("Nenhum cadastrado.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.ext.muted)
                        else CardList(u.domesticos) { d ->
                            Linha(Icons.Rounded.Build, Color(0xFF0891B2), d.nome, listOfNotNull(d.funcao, d.diasHorarios, d.documento).joinToString(" · "),
                                onRemover = if (u.possoGerenciar) ({ vm.removerDomestico(d.id) }) else null)
                        }
                        if (u.possoGerenciar) TextButton(onClick = { novoDom = true }) { Icon(Icons.Rounded.Add, contentDescription = null); Text("Cadastrar funcionário") }
                        Spacer(Modifier.height(10.dp))
                        NoteBox(Icons.Rounded.Info, MaterialTheme.colorScheme.primary, "Para incluir ou tirar moradores, use a web do condomínio (o convite vai por e-mail).")
                        Spacer(Modifier.height(32.dp))
                    }
                }
            }
        }
    }
    if (novoPet) CadastroSheet("Cadastrar pet", listOf("Nome", "Espécie (cachorro, gato…)", "Raça", "Porte")) { v ->
        novoPet = false; if (v != null) vm.adicionarPet(v[0], v[1], v[2], v[3])
    }
    if (novoDom) CadastroSheet("Cadastrar funcionário", listOf("Nome", "Documento", "Função (diarista, babá…)", "Dias e horários")) { v ->
        novoDom = false; if (v != null) vm.adicionarDomestico(v[0], v[1], v[2], v[3])
    }
}

@Composable
private fun Linha(icon: ImageVector, cor: Color, titulo: String, sub: String, onRemover: (() -> Unit)? = null) {
    Row(Modifier.fillMaxWidth().padding(start = 14.dp, top = 12.dp, bottom = 12.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        IconBubble(icon, cor, size = 40.dp)
        Column(Modifier.weight(1f)) {
            Text(titulo, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.ext.ink)
            if (sub.isNotBlank()) Text(sub, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.ext.muted)
        }
        if (onRemover != null) IconButton(onClick = onRemover) { Icon(Icons.Rounded.Close, contentDescription = "Remover $titulo", tint = MaterialTheme.ext.faint) }
    }
}

/** Formulário simples de 4 campos (o primeiro é obrigatório). [onFim] com null = cancelou. */
@Composable
private fun CadastroSheet(titulo: String, campos: List<String>, onFim: (List<String>?) -> Unit) {
    val valores = remember { campos.map { mutableStateOf("") } }
    ModalBottomSheet(onDismissRequest = { onFim(null) }, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 20.dp).navigationBarsPadding().imePadding()) {
            Text(titulo, style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(10.dp))
            campos.forEachIndexed { i, c ->
                OutlinedTextField(valores[i].value, { valores[i].value = it }, label = { Text(c) }, singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences), modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp))
            }
            Spacer(Modifier.height(4.dp))
            Button(onClick = { onFim(valores.map { it.value.trim() }) }, enabled = valores[0].value.isNotBlank(), modifier = Modifier.fillMaxWidth().height(50.dp)) { Text("Salvar") }
        }
    }
}

