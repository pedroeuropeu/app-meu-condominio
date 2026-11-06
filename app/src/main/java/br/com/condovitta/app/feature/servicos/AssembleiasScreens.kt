package br.com.condovitta.app.feature.servicos

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.HowToVote
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.RecordVoiceOver
import androidx.compose.material.icons.rounded.ThumbDown
import androidx.compose.material.icons.rounded.ThumbUp
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.condovitta.app.core.common.Fmt
import br.com.condovitta.app.core.designsystem.AppCard
import br.com.condovitta.app.core.designsystem.Hero
import br.com.condovitta.app.core.designsystem.SectionHeader
import br.com.condovitta.app.core.designsystem.SignaturePad
import br.com.condovitta.app.core.designsystem.StatusBadge
import br.com.condovitta.app.core.designsystem.ext
import br.com.condovitta.app.core.designsystem.rememberSignatureState
import br.com.condovitta.app.domain.AssembleiaEncerrada
import br.com.condovitta.app.domain.AssembleiaProxima
import br.com.condovitta.app.domain.ItemPauta
import br.com.condovitta.app.domain.OpcaoVoto
import br.com.condovitta.app.domain.Placar
import br.com.condovitta.app.domain.Poderes
import br.com.condovitta.app.feature.morador.abrirPdf
import br.com.condovitta.app.feature.portaria.BusyOr
import br.com.condovitta.app.feature.portaria.EmptyBox
import br.com.condovitta.app.feature.portaria.NoteBox

private val Pink = Color(0xFFDB2777)

@Composable
private fun ResultadoBadge(i: ItemPauta) {
    val e = MaterialTheme.ext
    when {
        i.resultado == "aprovado" -> StatusBadge("Aprovado", e.success, e.successContainer)
        i.resultado == "rejeitado" -> StatusBadge("Rejeitado", e.danger, e.dangerContainer)
        i.resultadoLabel != null -> StatusBadge(i.resultadoLabel!!, e.muted, e.cardBorder)
        i.emVotacao -> StatusBadge("Em votação", e.warn, e.warnContainer)
        i.votado -> StatusBadge("Votado", e.muted, e.cardBorder)
        else -> StatusBadge("Na pauta", e.muted, e.cardBorder)
    }
}

/** Barra com a fatia a favor / contra / abstenção. */
@Composable
fun PlacarBarra(p: Placar) {
    val total = p.total.coerceAtLeast(1)
    Column(Modifier.fillMaxWidth().padding(top = 6.dp)) {
        Row(Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)).background(MaterialTheme.ext.cardBorder)) {
            if (p.aFavor > 0) Box(Modifier.weight(p.aFavor.toFloat()).fillMaxHeight().background(MaterialTheme.ext.success))
            if (p.contra > 0) Box(Modifier.weight(p.contra.toFloat()).fillMaxHeight().background(MaterialTheme.ext.danger))
            if (p.abstencao > 0) Box(Modifier.weight(p.abstencao.toFloat()).fillMaxHeight().background(MaterialTheme.ext.faint))
            if (p.total == 0) Spacer(Modifier.weight(1f))
        }
        Text(
            "${p.aFavor} a favor (${p.aFavor * 100 / total}%) · ${p.contra} contra · ${p.abstencao} abstenção",
            style = MaterialTheme.typography.labelSmall, color = MaterialTheme.ext.muted, modifier = Modifier.padding(top = 4.dp)
        )
    }
}

/* ------------------------------------------------------------------ lista */

@Composable
fun AssembleiasScreen(onBack: () -> Unit, onPlenario: (Long) -> Unit, vm: AssembleiasViewModel = hiltViewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    val baixando by vm.baixando.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var procurando by rememberSaveable { mutableStateOf<Long?>(null) }
    TelaServico("Assembleias", state, onBack, { vm.refresh() }) { a ->
        a.aoVivo?.let { v ->
            Spacer(Modifier.height(8.dp))
            Hero {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    StatusBadge("AO VIVO", Color.White, Color(0xFFDC2626))
                    Text("  ${v.chamada ?: ""}", color = Color.White.copy(alpha = 0.85f), style = MaterialTheme.typography.labelMedium)
                }
                Text(v.titulo, style = MaterialTheme.typography.headlineSmall, color = Color.White, modifier = Modifier.padding(top = 6.dp))
                Text(listOfNotNull(v.presidente?.let { "Presidente: $it" }, "${v.naSala} na sala").joinToString(" · "), color = Color.White.copy(alpha = 0.85f), style = MaterialTheme.typography.bodySmall)
                v.emVotacao?.let { Text("Em votação: ${it.titulo}", color = Color.White, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 8.dp)) }
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = { onPlenario(v.id) }, colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier.fillMaxWidth().height(50.dp).testTag("assembleia_entrar")
                ) { Icon(Icons.Rounded.HowToVote, contentDescription = null); Text("  Entrar no plenário") }
            }
        }
        if (a.proximas.isNotEmpty()) {
            SectionHeader("Próximas")
            a.proximas.forEach { p ->
                ProximaCard(p, baixando != null, onEdital = { vm.edital(p.id) { abrirPdf(context, it, "edital-${p.id}.pdf") } },
                    onProcuracao = { procurando = p.id }, onCancelar = { pr -> vm.cancelarProcuracao(p.id, pr) })
            }
        }
        if (a.encerradas.isNotEmpty()) {
            SectionHeader("Encerradas")
            a.encerradas.forEach { e -> EncerradaCard(e, baixando != null) { vm.ata(e.id) { abrirPdf(context, it, "ata-${e.id}.pdf") } } }
        }
        if (a.aoVivo == null && a.proximas.isEmpty() && a.encerradas.isEmpty())
            EmptyBox(Icons.Rounded.Groups, "Nenhuma assembleia", "Quando o condomínio convocar uma assembleia, o edital e a votação aparecem aqui.")
        FimDaLista(fab = false)
    }
    procurando?.let { id -> ProcuracaoSheet(vm, id, onDismiss = { procurando = null }) }
}

@Composable
private fun ProximaCard(p: AssembleiaProxima, baixando: Boolean, onEdital: () -> Unit, onProcuracao: () -> Unit, onCancelar: (Long) -> Unit) {
    AppCard(Modifier.padding(bottom = 10.dp)) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(p.titulo, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.ext.ink, modifier = Modifier.weight(1f))
                p.tipo?.let { StatusBadge(it, Pink, Pink.copy(alpha = 0.12f)) }
            }
            Text(
                listOfNotNull(
                    p.data?.let { Fmt.data(it) }, p.hora?.let { "${Fmt.hora(it)} (1ª chamada)" }, p.segunda?.let { "${Fmt.hora(it)} (2ª)" }
                ).joinToString(" · "),
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 4.dp)
            )
            p.modalidade?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.ext.muted) }
            if (p.pauta.isNotEmpty()) {
                Text("Pauta", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.ext.muted, modifier = Modifier.padding(top = 10.dp))
                p.pauta.forEachIndexed { i, t -> Text("${i + 1}. $t", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.ext.ink) }
            }
            p.minhaProcuracao?.takeIf { it.valida }?.let { pr ->
                Spacer(Modifier.height(10.dp))
                NoteBox(Icons.Rounded.CheckCircle, MaterialTheme.ext.success,
                    "Procuração para ${listOfNotNull(pr.outorgado, pr.outorgadoUnidade).joinToString(" · ")}" + (pr.poderesLabel?.let { " ($it)" } ?: ""))
                TextButton(onClick = { onCancelar(pr.id) }) { Text("Cancelar procuração", color = MaterialTheme.colorScheme.error) }
            }
            Row(Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = onEdital, enabled = !baixando, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Rounded.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp)); Text("  Edital")
                }
                if (p.aceitaProcuracao && p.podeEnviarProcuracao && p.minhaProcuracao?.valida != true)
                    OutlinedButton(onClick = onProcuracao, modifier = Modifier.weight(1f).testTag("assembleia_procuracao")) { Text("Procuração") }
            }
        }
    }
}

@Composable
private fun EncerradaCard(e: AssembleiaEncerrada, baixando: Boolean, onAta: () -> Unit) {
    AppCard(Modifier.padding(bottom = 10.dp)) {
        Column(Modifier.padding(16.dp)) {
            Text(e.titulo, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.ext.ink)
            Text(listOfNotNull(e.data?.let { Fmt.data(it) }, e.presenca?.let { "Presença: $it" }).joinToString(" · "), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.ext.muted)
            when (e.participacao) {
                "todos" -> Text("Sua unidade votou em todos os itens", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.ext.success)
                "alguns" -> Text("Sua unidade votou em parte dos itens", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.ext.warn)
                "nenhum" -> Text("Sua unidade não votou", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.ext.muted)
            }
            e.itens.forEach { i ->
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("${i.ordem ?: ""}. ${i.titulo}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.ext.ink, modifier = Modifier.weight(1f))
                    ResultadoBadge(i)
                }
                i.placar?.let { PlacarBarra(it) }
            }
            if (e.ataDisponivel) {
                Spacer(Modifier.height(10.dp))
                OutlinedButton(onClick = onAta, enabled = !baixando, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Rounded.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp)); Text("  Ata assinada")
                }
            }
        }
    }
}

@Composable
private fun ProcuracaoSheet(vm: AssembleiasViewModel, id: Long, onDismiss: () -> Unit) {
    val reps by vm.representantes.collectAsStateWithLifecycle()
    val salvando by vm.envio.salvando.collectAsStateWithLifecycle()
    val erro by vm.envio.erro.collectAsStateWithLifecycle()
    val ok by vm.envio.ok.collectAsStateWithLifecycle()
    val inicial = remember { ok }
    LaunchedEffect(ok) { if (ok != inicial) onDismiss() }
    LaunchedEffect(id) { vm.carregarRepresentantes(id) }
    var escolhido by rememberSaveable { mutableStateOf<Long?>(null) }
    var poderes by rememberSaveable { mutableStateOf(Poderes.TODOS) }
    val assinatura = rememberSignatureState()
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 20.dp).navigationBarsPadding().verticalScroll(rememberScrollState())) {
            Text("Procuração", style = MaterialTheme.typography.titleLarge)
            Text("Quem vai representar a sua unidade nesta assembleia? Cada pessoa representa no máximo 2 unidades.",
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.ext.muted)
            Spacer(Modifier.height(12.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                reps.forEach { r -> FilterChip(escolhido == r.usuarioId, { escolhido = r.usuarioId }, label = { Text(listOfNotNull(r.nome, r.unidade).joinToString(" · ")) }, colors = chipColors()) }
            }
            Text("Poderes", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.ext.muted, modifier = Modifier.padding(top = 10.dp, bottom = 4.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Poderes.entries.forEach { pd -> FilterChip(poderes == pd, { poderes = pd }, label = { Text(pd.label) }, colors = chipColors()) }
            }
            Text("Assine para confirmar", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.ext.muted, modifier = Modifier.padding(top = 10.dp, bottom = 4.dp))
            SignaturePad(assinatura, Modifier.fillMaxWidth(), height = 140.dp)
            TextButton(onClick = { assinatura.clear() }, modifier = Modifier.align(Alignment.End)) { Text("Limpar") }
            erro?.let { NoteBox(Icons.Rounded.ErrorOutline, MaterialTheme.colorScheme.error, it) }
            Spacer(Modifier.height(10.dp))
            Button(onClick = { vm.procuracao(id, escolhido, poderes, assinatura.toPng()) }, enabled = escolhido != null && !salvando, modifier = Modifier.fillMaxWidth().height(50.dp)) {
                BusyOr(salvando) { Text("Enviar procuração") }
            }
        }
    }
}

/* ------------------------------------------------------------------ plenário */

@Composable
fun PlenarioScreen(onBack: () -> Unit, vm: PlenarioViewModel = hiltViewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    val votando by vm.votando.collectAsStateWithLifecycle()
    val baixando by vm.baixando.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var msg by rememberSaveable { mutableStateOf("") }
    LifecycleResumeEffect(Unit) {
        vm.acompanhar(true)
        onPauseOrDispose { vm.acompanhar(false) }
    }
    TelaServico(
        "Plenário", state, onBack, { vm.refresh() },
        bottomBar = {
            val e = state.data
            if (e != null && e.aoVivo && e.presente) {
                Surface(tonalElevation = 3.dp) {
                    Row(Modifier.navigationBarsPadding().imePadding().padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(msg, { msg = it.take(500) }, placeholder = { Text("Mensagem para o chat") }, singleLine = true,
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences), modifier = Modifier.weight(1f))
                        IconButton(onClick = { vm.chat(msg) { msg = "" } }, enabled = msg.isNotBlank()) {
                            Icon(Icons.AutoMirrored.Rounded.Send, contentDescription = "Enviar mensagem", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }
    ) { e ->
        Spacer(Modifier.height(8.dp))
        AppCard {
            Column(Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(e.titulo, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.ext.ink, modifier = Modifier.weight(1f))
                    if (e.aoVivo) StatusBadge("AO VIVO", Color.White, Color(0xFFDC2626)) else StatusBadge(if (e.status == "encerrada") "Encerrada" else "Aguardando", MaterialTheme.ext.muted, MaterialTheme.ext.cardBorder)
                }
                Text(
                    listOfNotNull(e.chamada, e.modalidade, "${e.presentes} de ${e.unidadesTotal} unidades presentes").joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.ext.muted
                )
                e.falando?.let { Text("Com a palavra: $it", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 6.dp)) }
            }
        }
        if (e.aoVivo && !e.presente) {
            Spacer(Modifier.height(12.dp))
            NoteBox(Icons.Rounded.Info, MaterialTheme.colorScheme.primary, "Confirme a sua presença para votar e participar do chat. Ela entra na lista de presença da ata.")
            Spacer(Modifier.height(8.dp))
            Button(onClick = { vm.presenca() }, modifier = Modifier.fillMaxWidth().height(54.dp).testTag("plenario_presenca")) { Text("Confirmar minha presença") }
        }
        if (e.representadas.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            NoteBox(Icons.Rounded.Groups, MaterialTheme.ext.info, "Você também vota por procuração: ${e.representadas.joinToString()}.")
        }
        val atual = e.itemAtual
        if (atual != null && atual.emVotacao) {
            SectionHeader("Em votação agora")
            AppCard {
                Column(Modifier.padding(16.dp)) {
                    Text("${atual.ordem ?: ""}. ${atual.titulo}", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.ext.ink)
                    atual.descricao?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.ext.muted, modifier = Modifier.padding(top = 4.dp)) }
                    Text(listOfNotNull(atual.quorumLabel, atual.precisaHint, atual.votacaoEncerraEm?.let { "encerra às ${Fmt.hora(it)}" }).joinToString(" · "),
                        style = MaterialTheme.typography.labelMedium, color = MaterialTheme.ext.faint, modifier = Modifier.padding(top = 6.dp))
                    val meu = e.votos[atual.id]
                    Spacer(Modifier.height(12.dp))
                    if (e.podeVotar) {
                        VotoBotoes(meu?.opcao, votando == atual.id) { vm.votar(atual.id, it) }
                        meu?.let { r ->
                            Text("Seu voto: ${r.opcao?.label ?: "—"} · protocolo ${r.protocolo ?: "—"}. Você pode trocar enquanto a votação estiver aberta.",
                                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.ext.muted, modifier = Modifier.padding(top = 8.dp))
                            TextButton(onClick = { vm.comprovante(atual.id) { abrirPdf(context, it, "comprovante-${atual.id}.pdf") } }, enabled = baixando == null) { Text("Baixar comprovante") }
                        }
                    } else if (e.presente) {
                        NoteBox(Icons.Rounded.Info, MaterialTheme.ext.muted, "Sua participação nesta assembleia é sem direito a voto.")
                    }
                    atual.placar?.let { PlacarBarra(it) }
                }
            }
        }
        if (e.aoVivo && e.presente) {
            Spacer(Modifier.height(10.dp))
            OutlinedButton(onClick = { vm.palavra() }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Rounded.RecordVoiceOver, contentDescription = null, modifier = Modifier.size(18.dp))
                Text(e.filaPosicao?.let { "  Na fila para falar (posição $it) · sair" } ?: "  Pedir a palavra")
            }
        }
        SectionHeader("Pauta")
        AppCard {
            e.pauta.forEachIndexed { idx, i ->
                Column(Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("${i.ordem ?: idx + 1}. ${i.titulo}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.ext.ink, modifier = Modifier.weight(1f))
                        ResultadoBadge(i)
                    }
                    e.votos[i.id]?.opcao?.let { Text("Seu voto: ${it.label}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary) }
                    if (i.votado) i.placar?.let { PlacarBarra(it) }
                }
            }
        }
        if (e.chat.isNotEmpty()) {
            SectionHeader("Chat")
            AppCard {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    e.chat.takeLast(30).forEach { c ->
                        Column {
                            Text(listOfNotNull(c.nome, c.unidade, c.hora?.let { Fmt.hora(it) }).joinToString(" · "), style = MaterialTheme.typography.labelSmall,
                                color = if (c.mesa) MaterialTheme.colorScheme.primary else MaterialTheme.ext.faint)
                            Text(c.texto, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.ext.ink, fontWeight = if (c.mesa) FontWeight.Medium else FontWeight.Normal)
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

/** Três botões grandes: A favor, Contra, Abstenção (o escolhido fica cheio). */
@Composable
private fun VotoBotoes(escolhido: OpcaoVoto?, ocupado: Boolean, onVoto: (OpcaoVoto) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(
            Triple(OpcaoVoto.A_FAVOR, Icons.Rounded.ThumbUp, MaterialTheme.ext.success),
            Triple(OpcaoVoto.CONTRA, Icons.Rounded.ThumbDown, MaterialTheme.ext.danger),
            Triple(OpcaoVoto.ABSTENCAO, Icons.Rounded.Block, MaterialTheme.ext.muted)
        ).forEach { (o, ic, cor) ->
            val sel = escolhido == o
            Button(
                onClick = { onVoto(o) }, enabled = !ocupado,
                colors = if (sel) ButtonDefaults.buttonColors(containerColor = cor, contentColor = Color.White)
                else ButtonDefaults.buttonColors(containerColor = cor.copy(alpha = 0.12f), contentColor = cor),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 12.dp, horizontal = 4.dp),
                modifier = Modifier.weight(1f).height(64.dp).testTag("voto_${o.name}")
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(ic, contentDescription = null, modifier = Modifier.size(20.dp))
                    Text(o.label, style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}
