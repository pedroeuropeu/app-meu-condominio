package br.com.condovitta.app.feature.servicos

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.rounded.Campaign
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Draw
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.MarkEmailRead
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.condovitta.app.core.common.Fmt
import br.com.condovitta.app.core.designsystem.AppCard
import br.com.condovitta.app.core.designsystem.IconBubble
import br.com.condovitta.app.core.designsystem.SectionHeader
import br.com.condovitta.app.core.designsystem.SignaturePad
import br.com.condovitta.app.core.designsystem.StatusBadge
import br.com.condovitta.app.core.designsystem.ext
import br.com.condovitta.app.core.designsystem.rememberSignatureState
import br.com.condovitta.app.domain.FormaAssinatura
import br.com.condovitta.app.feature.morador.abrirPdf
import br.com.condovitta.app.feature.portaria.BusyOr
import br.com.condovitta.app.feature.portaria.CardList
import br.com.condovitta.app.feature.portaria.EmptyBox
import br.com.condovitta.app.feature.portaria.InfoRows
import br.com.condovitta.app.feature.portaria.NoteBox
import java.time.LocalDate

private val Violet = Color(0xFF7C3AED)
private val Slate = Color(0xFF64748B)

/* ------------------------------------------------------------------ lista */

@Composable
fun DocumentosScreen(onBack: () -> Unit, onAbrir: (Long) -> Unit, vm: DocumentosViewModel = hiltViewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    val baixando by vm.baixando.collectAsStateWithLifecycle()
    val context = LocalContext.current
    TelaServico("Documentos", state, onBack, { vm.refresh() }) { d ->
        if (d.pendentes.isNotEmpty()) {
            SectionHeader("Para assinar", subtitle = "${d.pendentes.size} documento(s) esperando você")
            CardList(d.pendentes) { p ->
                val hoje = LocalDate.now()
                LinhaServico(
                    Icons.Rounded.Draw, Violet, p.titulo,
                    listOfNotNull(p.tipo, p.solicitante).joinToString(" · ").ifBlank { null },
                    p.prazo?.let { "Prazo: ${Fmt.data(it)}" + if (!it.isAfter(hoje.plusDays(2))) " · vence logo" else "" },
                    onClick = { onAbrir(p.id) },
                    trailing = {
                        if (p.opcional) StatusBadge("Opcional", MaterialTheme.ext.muted, MaterialTheme.ext.cardBorder)
                        else StatusBadge(if (p.visualizado) "Visto" else "Novo", MaterialTheme.ext.warn, MaterialTheme.ext.warnContainer)
                    }
                )
            }
        } else {
            Spacer(Modifier.height(8.dp))
            NoteBox(Icons.Rounded.CheckCircle, MaterialTheme.ext.success, "Nenhum documento esperando a sua assinatura.")
        }
        if (d.oficiais.isNotEmpty()) {
            SectionHeader("Documentos do condomínio", subtitle = "Convenção, atas e editais")
            CardList(d.oficiais) { o ->
                LinhaServico(
                    if (o.edital) Icons.Rounded.Campaign else Icons.Rounded.Description, Slate, o.titulo, o.sub, onClick = {
                        vm.oficial(o.id) { abrirPdf(context, it, "documento-${o.id}.pdf") }
                    },
                    trailing = { Icon(Icons.Rounded.PictureAsPdf, contentDescription = "Abrir PDF", tint = if (baixando == "o${o.id}") MaterialTheme.ext.faint else MaterialTheme.ext.muted) }
                )
            }
        }
        if (d.assinados.isNotEmpty()) {
            SectionHeader("Assinados por mim")
            CardList(d.assinados) { a ->
                LinhaServico(
                    Icons.Rounded.Verified, MaterialTheme.ext.success, a.titulo,
                    listOfNotNull(a.assinado?.let { "Assinado ${Fmt.quando(it)}" }, a.origem).joinToString(" · "),
                    a.codigo?.let { "Código $it" }, onClick = { onAbrir(a.id) }
                )
            }
        }
        if (d.pendentes.isEmpty() && d.assinados.isEmpty() && d.oficiais.isEmpty())
            EmptyBox(Icons.Rounded.Description, "Nada por aqui", "Documentos para assinar e os documentos do condomínio aparecem aqui.")
        FimDaLista(fab = false)
    }
}

/* ------------------------------------------------------------------ detalhe e assinatura */

@Composable
fun DocumentoScreen(onBack: () -> Unit, vm: DocumentoViewModel = hiltViewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    val baixando by vm.baixando.collectAsStateWithLifecycle()
    val salvando by vm.envio.salvando.collectAsStateWithLifecycle()
    val erro by vm.envio.erro.collectAsStateWithLifecycle()
    val codigoPara by vm.codigoPara.collectAsStateWithLifecycle()
    val resultado by vm.resultado.collectAsStateWithLifecycle()
    val evidencias by vm.evidencias.collectAsStateWithLifecycle()
    val recusou by vm.recusou.collectAsStateWithLifecycle()
    LaunchedEffect(recusou) { if (recusou) onBack() }
    val context = LocalContext.current
    val assinatura = rememberSignatureState()
    var codigo by rememberSaveable { mutableStateOf("") }
    var recusando by rememberSaveable { mutableStateOf(false) }

    TelaServico("Documento", state, onBack, { vm.refresh() }) { d ->
        Spacer(Modifier.height(8.dp))
        AppCard {
            Column(Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    IconBubble(Icons.Rounded.Description, Violet)
                    Text(d.titulo, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.ext.ink, modifier = Modifier.weight(1f))
                }
                InfoRows(
                    listOf(
                        "Tipo" to d.tipo, "Enviado por" to d.solicitante, "Enviado" to d.enviado?.let { Fmt.quando(it) }, "Prazo" to d.prazo?.let { Fmt.data(it) },
                        "Páginas" to d.paginas?.toString(), "Assinatura" to if (d.opcional) "Opcional" else "Obrigatória"
                    )
                )
                d.mensagem?.let { Spacer(Modifier.height(6.dp)); NoteBox(Icons.Rounded.Info, MaterialTheme.colorScheme.primary, it) }
                Spacer(Modifier.height(12.dp))
                OutlinedButton(onClick = { vm.pdf { abrirPdf(context, it, "documento-${d.id}.pdf") } }, enabled = baixando == null, modifier = Modifier.fillMaxWidth().testTag("doc_ver")) {
                    Icon(Icons.Rounded.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text(if (d.finalDisponivel) "  Ver documento assinado" else "  Ler o documento")
                }
            }
        }

        when {
            d.assinei -> {
                Spacer(Modifier.height(12.dp))
                NoteBox(Icons.Rounded.Verified, MaterialTheme.ext.success,
                    "Você assinou este documento." + (resultado?.codigo ?: d.codigo)?.let { " Código de verificação: $it." }.orEmpty() +
                        resultado?.confirmacao?.let { " Confirmação: $it." }.orEmpty())
            }
            d.recusei -> { Spacer(Modifier.height(12.dp)); NoteBox(Icons.Rounded.ErrorOutline, MaterialTheme.ext.danger, "Você recusou este documento.") }
            d.cancelado -> { Spacer(Modifier.height(12.dp)); NoteBox(Icons.Rounded.Info, MaterialTheme.ext.muted, "Este documento foi cancelado e não precisa mais de assinatura.") }
            !d.suaVez -> { Spacer(Modifier.height(12.dp)); NoteBox(Icons.Rounded.Info, MaterialTheme.ext.info, "Ainda não é a sua vez: outra pessoa assina antes de você. Avisaremos quando for.") }
            d.formas == FormaAssinatura.ARQUIVO -> {
                Spacer(Modifier.height(12.dp))
                NoteBox(Icons.Rounded.Info, MaterialTheme.ext.info, "Este documento pede o arquivo assinado (PDF ou foto). Envie pelo site do Condovitta.")
            }
            d.podeAssinarAqui -> {
                SectionHeader("Sua assinatura", subtitle = "Desenhe com o dedo dentro do quadro")
                AppCard {
                    Column(Modifier.padding(12.dp)) {
                        SignaturePad(assinatura, Modifier.fillMaxWidth().testTag("doc_assinatura"))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(d.nome ?: "", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.ext.muted, modifier = Modifier.weight(1f))
                            TextButton(onClick = { assinatura.clear() }) { Text("Limpar") }
                        }
                    }
                }
                if (d.exigeCodigo) {
                    Spacer(Modifier.height(10.dp))
                    if (codigoPara == null) {
                        NoteBox(Icons.Rounded.MarkEmailRead, MaterialTheme.colorScheme.primary, "Para confirmar que é você, vamos enviar um código de 6 dígitos para ${d.emailMascarado ?: "o seu e-mail"}.")
                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(onClick = { vm.pedirCodigo() }, modifier = Modifier.fillMaxWidth().testTag("doc_pedir_codigo")) { Text("Enviar código por e-mail") }
                    } else {
                        OutlinedTextField(
                            codigo, { codigo = it.filter(Char::isDigit).take(6) }, label = { Text("Código enviado para $codigoPara") }, singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                            textStyle = MaterialTheme.typography.titleMedium.copy(fontFamily = FontFamily.Monospace),
                            modifier = Modifier.fillMaxWidth().testTag("doc_codigo")
                        )
                        TextButton(onClick = { vm.pedirCodigo() }) { Text("Reenviar código") }
                    }
                }
                erro?.let { Spacer(Modifier.height(8.dp)); NoteBox(Icons.Rounded.ErrorOutline, MaterialTheme.colorScheme.error, it) }
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = { vm.assinar(assinatura.toPng(), codigo) },
                    enabled = !salvando && (!d.exigeCodigo || codigo.length == 6),
                    modifier = Modifier.fillMaxWidth().height(52.dp).testTag("doc_assinar")
                ) { BusyOr(salvando) { Text("Assinar documento") } }
                Text(
                    "Ao assinar, registramos data, hora, aparelho e IP como evidência, com validade jurídica (MP 2.200-2/2001).",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.ext.muted, modifier = Modifier.padding(top = 8.dp)
                )
                TextButton(onClick = { recusando = true }, modifier = Modifier.fillMaxWidth()) { Text("Recusar assinatura", color = MaterialTheme.colorScheme.error) }
            }
        }

        if (d.assinei || d.recusei) {
            SectionHeader("Evidências")
            val ev = evidencias
            if (ev == null) OutlinedButton(onClick = { vm.carregarEvidencias() }, modifier = Modifier.fillMaxWidth()) { Text("Ver o histórico de evidências") }
            else CardList(ev.eventos) { e ->
                LinhaServico(Icons.Rounded.CheckCircle, if (e.tipo == "RECUSADO") MaterialTheme.ext.danger else MaterialTheme.ext.success,
                    e.titulo ?: e.tipo, listOfNotNull(e.ator, e.detalhe).joinToString(" · ").ifBlank { null }, e.quando?.let { Fmt.quando(it) }, onClick = null)
            }
            ev?.hashFinal?.let { Text("Hash do documento final: $it", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.ext.faint, modifier = Modifier.padding(top = 8.dp)) }
        }
        Spacer(Modifier.height(24.dp))
    }

    if (recusando) RecusarSheet(vm, onDismiss = { recusando = false })
}

@Composable
private fun RecusarSheet(vm: DocumentoViewModel, onDismiss: () -> Unit) {
    val salvando by vm.envio.salvando.collectAsStateWithLifecycle()
    val erro by vm.envio.erro.collectAsStateWithLifecycle()
    var motivo by rememberSaveable { mutableStateOf("") }
    LaunchedEffect(Unit) { vm.envio.erro.value = null }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 20.dp).navigationBarsPadding().imePadding().verticalScroll(rememberScrollState())) {
            Text("Recusar assinatura", style = MaterialTheme.typography.titleLarge)
            Text("Quem enviou o documento recebe o motivo.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.ext.muted)
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(motivo, { motivo = it.take(1000) }, label = { Text("Motivo") }, minLines = 3,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences), modifier = Modifier.fillMaxWidth())
            erro?.let { Spacer(Modifier.height(8.dp)); NoteBox(Icons.Rounded.ErrorOutline, MaterialTheme.colorScheme.error, it) }
            Spacer(Modifier.height(12.dp))
            Button(onClick = { vm.recusar(motivo) }, enabled = motivo.isNotBlank() && !salvando, modifier = Modifier.fillMaxWidth().height(50.dp)) {
                BusyOr(salvando) { Text("Recusar") }
            }
        }
    }
}
