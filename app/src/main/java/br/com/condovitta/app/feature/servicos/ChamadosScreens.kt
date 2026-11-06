package br.com.condovitta.app.feature.servicos

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.HourglassTop
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.SupportAgent
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.condovitta.app.core.common.Fmt
import br.com.condovitta.app.core.designsystem.AppCard
import br.com.condovitta.app.core.designsystem.BackTopBar
import br.com.condovitta.app.core.designsystem.SectionHeader
import br.com.condovitta.app.core.designsystem.StatusBadge
import br.com.condovitta.app.core.designsystem.ext
import br.com.condovitta.app.domain.CategoriaChamado
import br.com.condovitta.app.domain.Chamado
import br.com.condovitta.app.domain.MensagemChamado
import br.com.condovitta.app.domain.NovoChamado
import br.com.condovitta.app.domain.PrioridadeChamado
import br.com.condovitta.app.domain.StatusChamado
import br.com.condovitta.app.feature.portaria.BusyOr
import br.com.condovitta.app.feature.portaria.CardList
import br.com.condovitta.app.feature.portaria.EmptyBox
import br.com.condovitta.app.feature.portaria.InfoRows
import br.com.condovitta.app.feature.portaria.Kpi
import br.com.condovitta.app.feature.portaria.NoteBox

private val Cyan = Color(0xFF0891B2)

@Composable
fun StatusChamadoBadge(s: StatusChamado) {
    val e = MaterialTheme.ext
    when (s) {
        StatusChamado.ABERTO -> StatusBadge(s.label, e.info, e.infoContainer)
        StatusChamado.EM_ANDAMENTO -> StatusBadge(s.label, MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.primaryContainer)
        StatusChamado.AGUARDANDO -> StatusBadge(s.label, e.warn, e.warnContainer)
        StatusChamado.RESOLVIDO, StatusChamado.CONCLUIDO -> StatusBadge(s.label, e.success, e.successContainer)
        StatusChamado.CANCELADO -> StatusBadge(s.label, e.muted, e.cardBorder)
    }
}

/* ------------------------------------------------------------------ lista */

@Composable
fun ChamadosScreen(onBack: () -> Unit, onAbrir: (Long) -> Unit, onNovo: () -> Unit, vm: ChamadosViewModel = hiltViewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    TelaServico(
        "Chamados", state, onBack, { vm.refresh() },
        fab = {
            ExtendedFloatingActionButton(onClick = onNovo, icon = { Icon(Icons.Rounded.Add, contentDescription = null) }, text = { Text("Abrir chamado") },
                containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.testTag("chamado_novo"))
        }
    ) { m ->
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Kpi(Icons.Rounded.SupportAgent, Cyan, m.resumo.abertos.toString(), "Em aberto", Modifier.weight(1f))
            Kpi(Icons.Rounded.HourglassTop, MaterialTheme.ext.warn, m.resumo.aguardando.toString(), "Esperam você", Modifier.weight(1f))
            Kpi(Icons.Rounded.CheckCircle, MaterialTheme.ext.success, m.resumo.resolvidosNoAno.toString(), "Resolvidos", Modifier.weight(1f))
        }
        m.resumo.primeiraRespostaMinutos?.takeIf { it > 0 }?.let {
            Spacer(Modifier.height(10.dp))
            NoteBox(Icons.Rounded.Timer, Cyan, "A administração costuma dar a primeira resposta em ${tempo(it)}.")
        }
        val (abertos, fechados) = m.itens.partition { it.status.aberto }
        if (m.itens.isEmpty()) EmptyBox(Icons.Rounded.SupportAgent, "Nenhum chamado ainda", "Algo quebrou, sujou ou precisa de atenção? Abra um chamado e acompanhe por aqui.")
        if (abertos.isNotEmpty()) {
            SectionHeader("Em aberto")
            CardList(abertos) { ChamadoRow(it) { onAbrir(it.id) } }
        }
        if (fechados.isNotEmpty()) {
            SectionHeader("Fechados")
            CardList(fechados) { ChamadoRow(it) { onAbrir(it.id) } }
        }
        FimDaLista()
    }
}

private fun tempo(min: Long): String = when {
    min < 60 -> "$min min"
    min < 60 * 24 -> "${min / 60} h"
    else -> "${min / (60 * 24)} dia(s)"
}

@Composable
private fun ChamadoRow(c: Chamado, onClick: () -> Unit) {
    LinhaServico(
        Icons.Rounded.SupportAgent, if (c.status.aberto) Cyan else MaterialTheme.ext.muted, c.titulo,
        c.ultimaTexto?.let { (if (c.ultimaDaAdministracao) "Administração: " else "Você: ") + it } ?: c.categoria.label,
        listOfNotNull("#${c.id}", c.categoria.label, Fmt.quando(c.ultimaEm ?: c.abertoEm)).joinToString(" · "),
        onClick = onClick,
        trailing = { StatusChamadoBadge(c.status) }
    )
}

/* ------------------------------------------------------------------ abrir chamado */

@Composable
fun NovoChamadoScreen(onBack: () -> Unit, onCriado: (Long) -> Unit, vm: NovoChamadoViewModel = hiltViewModel()) {
    val salvando by vm.envio.salvando.collectAsStateWithLifecycle()
    val erro by vm.envio.erro.collectAsStateWithLifecycle()
    val fotos by vm.fotos.collectAsStateWithLifecycle()
    val criado by vm.criado.collectAsStateWithLifecycle()
    LaunchedEffect(criado) { criado?.let(onCriado) }
    var titulo by rememberSaveable { mutableStateOf("") }
    var categoria by rememberSaveable { mutableStateOf(CategoriaChamado.MANUTENCAO) }
    var prioridade by rememberSaveable { mutableStateOf(PrioridadeChamado.MEDIA) }
    var local by rememberSaveable { mutableStateOf("") }
    var descricao by rememberSaveable { mutableStateOf("") }
    var autoriza by rememberSaveable { mutableStateOf(false) }
    val escolher = rememberSeletorFotos(5 - fotos.size) { vm.adicionar(it) }

    Scaffold(topBar = { BackTopBar("Abrir chamado", onBack = onBack) }, containerColor = MaterialTheme.colorScheme.background) { p ->
        Column(Modifier.padding(p).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).imePadding()) {
            NoteBox(Icons.Rounded.Info, Cyan, "Conte o que aconteceu. A administração responde por aqui e você recebe uma notificação.")
            Spacer(Modifier.height(14.dp))
            OutlinedTextField(titulo, { titulo = it.take(250) }, label = { Text("Título") }, placeholder = { Text("Ex.: Vazamento no teto do banheiro") }, singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences), modifier = Modifier.fillMaxWidth().testTag("chamado_titulo"))
            Text("Assunto", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.ext.muted, modifier = Modifier.padding(top = 14.dp, bottom = 4.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CategoriaChamado.doMorador.forEach { c -> FilterChip(categoria == c, { categoria = c }, label = { Text(c.label) }, colors = chipColors()) }
            }
            Text("Urgência", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.ext.muted, modifier = Modifier.padding(top = 10.dp, bottom = 4.dp))
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PrioridadeChamado.entries.forEach { pr -> FilterChip(prioridade == pr, { prioridade = pr }, label = { Text(pr.label) }, colors = chipColors()) }
            }
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(local, { local = it.take(250) }, label = { Text("Onde? (opcional)") }, placeholder = { Text("Ex.: Banheiro social, garagem G2") }, singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences), modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(descricao, { descricao = it.take(4000) }, label = { Text("Descrição") }, minLines = 4,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences), modifier = Modifier.fillMaxWidth().testTag("chamado_descricao"))
            Spacer(Modifier.height(10.dp))
            AnexosEnvio(fotos, escolher, { vm.remover(it) })
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
                Column(Modifier.weight(1f)) {
                    Text("Autorizo a entrada na unidade", style = MaterialTheme.typography.bodyLarge)
                    Text("Para o zelador ou o técnico verem o problema.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.ext.muted)
                }
                Switch(autoriza, { autoriza = it })
            }
            erro?.let { Spacer(Modifier.height(8.dp)); NoteBox(Icons.Rounded.ErrorOutline, MaterialTheme.colorScheme.error, it) }
            Spacer(Modifier.height(14.dp))
            Button(
                onClick = { vm.abrir(NovoChamado(titulo, categoria, prioridade, local, descricao, autoriza)) },
                enabled = titulo.isNotBlank() && !salvando, modifier = Modifier.fillMaxWidth().height(52.dp).testTag("chamado_enviar")
            ) { BusyOr(salvando) { Text("Abrir chamado") } }
            Spacer(Modifier.height(24.dp))
        }
    }
}

/* ------------------------------------------------------------------ detalhe e conversa */

@Composable
fun ChamadoScreen(onBack: () -> Unit, vm: ChamadoViewModel = hiltViewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    val fotos by vm.fotos.collectAsStateWithLifecycle()
    val salvando by vm.envio.salvando.collectAsStateWithLifecycle()
    val erro by vm.envio.erro.collectAsStateWithLifecycle()
    val baixando by vm.baixando.collectAsStateWithLifecycle()
    val fechou by vm.fechou.collectAsStateWithLifecycle()
    LaunchedEffect(fechou) { if (fechou) onBack() }
    val context = LocalContext.current
    var texto by rememberSaveable { mutableStateOf("") }
    val escolher = rememberSeletorFotos(5 - fotos.size) { vm.adicionar(it) }

    TelaServico(
        "Chamado #${vm.id}", state, onBack, { vm.refresh() },
        bottomBar = {
            val d = state.data
            if (d != null && d.podeResponder) {
                Surface(tonalElevation = 3.dp) {
                    Column(Modifier.navigationBarsPadding().imePadding().padding(horizontal = 12.dp, vertical = 8.dp)) {
                        if (fotos.isNotEmpty()) AnexosEnvio(fotos, escolher, { vm.remover(it) })
                        erro?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = escolher, enabled = fotos.size < 5) { Icon(Icons.Rounded.AddPhotoAlternate, contentDescription = "Anexar foto") }
                            OutlinedTextField(texto, { texto = it.take(4000) }, placeholder = { Text("Escreva uma mensagem") }, maxLines = 4,
                                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences), modifier = Modifier.weight(1f).testTag("chamado_resposta"))
                            IconButton(onClick = { vm.responder(texto) { texto = "" } }, enabled = (texto.isNotBlank() || fotos.isNotEmpty()) && !salvando) {
                                if (salvando) androidx.compose.material3.CircularProgressIndicator(Modifier.padding(4.dp).height(20.dp).widthIn(max = 20.dp), strokeWidth = 2.dp)
                                else Icon(Icons.AutoMirrored.Rounded.Send, contentDescription = "Enviar", tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }
        }
    ) { d ->
        val c = d.chamado
        Spacer(Modifier.height(8.dp))
        AppCard {
            Column(Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(c.titulo, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.ext.ink, modifier = Modifier.weight(1f))
                    StatusChamadoBadge(c.status)
                }
                InfoRows(
                    listOf(
                        "Assunto" to c.categoria.label, "Urgência" to c.prioridade.label, "Onde" to c.local, "Aberto" to Fmt.quando(c.abertoEm),
                        "Responsável" to c.responsavel, "Previsão" to c.prazoEm?.takeIf { c.status.aberto }?.let { Fmt.quando(it) },
                        "Entrada na unidade" to if (d.autorizaEntrada) "Autorizada" else null
                    )
                )
                if (d.anexos.isNotEmpty()) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        d.anexos.forEach { a -> AnexoChip(a, baixando != null) { vm.abrirAnexo(a) { abrirAnexo(context, it, a) } } }
                    }
                }
            }
        }
        d.aguardandoMotivo?.takeIf { c.status == StatusChamado.AGUARDANDO }?.let {
            Spacer(Modifier.height(10.dp))
            NoteBox(Icons.Rounded.HourglassTop, MaterialTheme.ext.warn, "A administração está esperando você: $it")
        }
        if (d.podeAvaliar) AvaliarCard(vm)
        c.avaliacao?.let { n ->
            Spacer(Modifier.height(10.dp))
            AppCard {
                Column(Modifier.padding(16.dp)) {
                    Text("Sua avaliação", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.ext.muted)
                    Estrelas(n, null)
                    d.avaliacaoComentario?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
                }
            }
        }
        SectionHeader("Conversa")
        d.mensagens.forEach { m -> Bolha(m, baixando != null) { a -> vm.abrirAnexo(a) { abrirAnexo(context, it, a) } } }
        Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (d.podeReabrir) OutlinedButton(onClick = { vm.reabrir() }, modifier = Modifier.weight(1f)) { Text("Reabrir chamado") }
            if (d.podeCancelar) TextButton(onClick = { vm.cancelar() }, modifier = Modifier.weight(1f).testTag("chamado_cancelar")) {
                Text("Cancelar chamado", color = MaterialTheme.colorScheme.error)
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun AvaliarCard(vm: ChamadoViewModel) {
    var nota by rememberSaveable { mutableIntStateOf(0) }
    var comentario by rememberSaveable { mutableStateOf("") }
    Spacer(Modifier.height(10.dp))
    AppCard {
        Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("O problema foi resolvido?", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.ext.ink)
            Text("Avalie o atendimento para fechar o chamado. Se não resolveu, reabra.", style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.ext.muted, textAlign = TextAlign.Center)
            Estrelas(nota, { nota = it })
            if (nota > 0) {
                OutlinedTextField(comentario, { comentario = it.take(500) }, label = { Text("Comentário (opcional)") }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                Button(onClick = { vm.avaliar(nota, comentario) }, modifier = Modifier.fillMaxWidth()) { Text("Enviar avaliação") }
            }
        }
    }
}

@Composable
private fun Bolha(m: MensagemChamado, baixando: Boolean, onAnexo: (br.com.condovitta.app.domain.Anexo) -> Unit) {
    if (m.sistema) {
        Text(
            listOfNotNull(m.texto, Fmt.quando(m.em)).joinToString(" · "), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.ext.faint,
            textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)
        )
        return
    }
    val minha = m.minha
    Box(Modifier.fillMaxWidth().padding(vertical = 4.dp), contentAlignment = if (minha) Alignment.CenterEnd else Alignment.CenterStart) {
        Surface(
            color = if (minha) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.ext.card,
            shape = MaterialTheme.shapes.large,
            border = if (minha) null else androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.ext.cardBorder),
            modifier = Modifier.widthIn(max = 300.dp)
        ) {
            Column(Modifier.padding(12.dp)) {
                if (!minha) Text(m.autor ?: "Administração", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                m.texto?.takeIf { it.isNotBlank() }?.let {
                    Text(it, style = MaterialTheme.typography.bodyMedium, color = if (minha) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.ext.ink)
                }
                if (m.anexos.isNotEmpty()) FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) { m.anexos.forEach { a -> AnexoChip(a, baixando) { onAnexo(a) } } }
                Text(Fmt.quando(m.em), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.ext.faint, modifier = Modifier.align(Alignment.End))
            }
        }
    }
    HorizontalDivider(color = Color.Transparent)
}
