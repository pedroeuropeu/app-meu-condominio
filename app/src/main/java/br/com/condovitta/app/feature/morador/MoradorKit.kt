package br.com.condovitta.app.feature.morador

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DoorFront
import androidx.compose.material.icons.rounded.QrCode2
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import br.com.condovitta.app.core.common.Fmt
import br.com.condovitta.app.core.common.Photos
import br.com.condovitta.app.core.designsystem.Brand
import br.com.condovitta.app.core.designsystem.IconBubble
import br.com.condovitta.app.core.designsystem.StatusBadge
import br.com.condovitta.app.core.designsystem.ext
import br.com.condovitta.app.domain.Boleto
import br.com.condovitta.app.domain.StatusBoleto
import br.com.condovitta.app.domain.Visita
import br.com.condovitta.app.feature.portaria.color
import br.com.condovitta.app.feature.portaria.icon
import java.io.File
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/* ------------------------------------------------------------------ ações do sistema */

/** Abre um PDF (boleto) no leitor do aparelho; o arquivo fica no cache e não vai para backup. */
fun abrirPdf(context: Context, bytes: ByteArray, nome: String): Boolean = abrirArquivo(context, bytes, nome, "application/pdf")

/** Abre um arquivo baixado (PDF, foto, convite .ics...) no app do aparelho que souber mostrar. Fica no cache, fora do backup. */
fun abrirArquivo(context: Context, bytes: ByteArray, nome: String, mime: String, titulo: String = "Abrir com"): Boolean = runCatching {
    val dir = File(context.cacheDir, "pdfs").apply { mkdirs() }
    val file = File(dir, nome.replace(Regex("[^A-Za-z0-9._-]"), "_")).apply { writeBytes(bytes) }
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fotos", file)
    context.startActivity(
        Intent.createChooser(Intent(Intent.ACTION_VIEW).setDataAndType(uri, mime).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION), titulo)
    )
}.isSuccess

fun compartilhar(context: Context, texto: String, titulo: String = "Compartilhar") {
    runCatching { context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, texto), titulo)) }
}

fun copiar(context: Context, rotulo: String, texto: String) {
    (context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager)?.setPrimaryClip(ClipData.newPlainText(rotulo, texto))
}

/* ------------------------------------------------------------------ boleto */

/** "Vence em 6 dias", "Vence hoje", "Venceu há 3 dias" */
fun prazo(b: Boleto, hoje: LocalDate = LocalDate.now()): String {
    val v = b.vencimento ?: return ""
    val d = ChronoUnit.DAYS.between(hoje, v)
    return when {
        d > 1 -> "Vence em $d dias"
        d == 1L -> "Vence amanhã"
        d == 0L -> "Vence hoje"
        else -> "Venceu há ${-d} dia(s)"
    }
}

/** Cartão azul do boleto em aberto, com Pix e código de barras (igual ao Início e Boletos do protótipo). */
@Composable
fun BoletoCard(b: Boleto, onPix: () -> Unit, onCopiarLinha: () -> Unit, modifier: Modifier = Modifier, onClick: (() -> Unit)? = null) {
    val vencido = b.status == StatusBoleto.VENCIDO
    Surface(onClick = onClick ?: {}, enabled = onClick != null, shape = MaterialTheme.shapes.large, color = Color.Transparent, modifier = modifier.fillMaxWidth()) {
        Column(
            Modifier
                .background(Brush.linearGradient(listOf(Brand.Navy, if (vencido) Color(0xFF9F1239) else Color(0xFF0A7FA0))))
                .padding(18.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Boleto · ${Fmt.competencia(b.competencia)}", color = Color.White.copy(alpha = 0.8f), style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
                Surface(color = Color.White.copy(alpha = 0.18f), shape = MaterialTheme.shapes.extraSmall) {
                    Text(prazo(b), color = Color.White, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(Fmt.money(b.corrigido ?: b.valor), color = Color.White, style = MaterialTheme.typography.displaySmall)
            Text(
                b.vencimento?.let { "Vencimento ${Fmt.data(it)}" + if (b.corrigido != null) " · com multa e juros" else "" } ?: "",
                color = Color.White.copy(alpha = 0.75f), style = MaterialTheme.typography.bodySmall
            )
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (b.pix != null) Button(
                    onClick = onPix, colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Brand.Navy),
                    modifier = Modifier.weight(1f).testTag("boleto_pix")
                ) { Icon(Icons.Rounded.QrCode2, contentDescription = null, modifier = Modifier.size(18.dp)); Spacer(Modifier.size(6.dp)); Text("Pagar com Pix") }
                if (b.linhaDigitavel != null) Button(
                    onClick = onCopiarLinha, colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.18f), contentColor = Color.White),
                    modifier = if (b.pix == null) Modifier.weight(1f) else Modifier
                ) { Icon(Icons.Rounded.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp)); Spacer(Modifier.size(6.dp)); Text("Código") }
            }
        }
    }
}

/** Pagar com Pix: QR (quando o backend gera) e o copia e cola. */
@Composable
fun PixSheet(b: Boleto, qr: ByteArray?, onCopiar: () -> Unit, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(horizontal = 24.dp).padding(bottom = 24.dp).navigationBarsPadding(), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Pagar com Pix", style = MaterialTheme.typography.titleLarge)
            Text("${Fmt.competencia(b.competencia)} · ${Fmt.money(b.corrigido ?: b.valor)}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.ext.muted)
            Spacer(Modifier.height(16.dp))
            val bmp = remember(qr) { Photos.decode(qr) }
            if (bmp != null) Image(bmp.asImageBitmap(), contentDescription = "QR do Pix", modifier = Modifier.size(220.dp).clip(MaterialTheme.shapes.medium).background(Color.White))
            else Box(Modifier.size(160.dp).clip(MaterialTheme.shapes.medium).background(MaterialTheme.colorScheme.surfaceContainer), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.QrCode2, contentDescription = null, tint = MaterialTheme.ext.muted, modifier = Modifier.size(64.dp))
            }
            Spacer(Modifier.height(12.dp))
            Text("Abra o app do seu banco, escolha Pix copia e cola e cole o código. A baixa é automática.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.ext.muted, textAlign = TextAlign.Center)
            Spacer(Modifier.height(16.dp))
            Button(onClick = onCopiar, modifier = Modifier.fillMaxWidth().height(52.dp).testTag("pix_copiar")) {
                Icon(Icons.Rounded.ContentCopy, contentDescription = null); Spacer(Modifier.size(8.dp)); Text("Copiar Pix copia e cola")
            }
        }
    }
}

/* ------------------------------------------------------------------ consulta da portaria */

/** "Fulano está na portaria": liberar ou recusar na hora (com recado opcional). */
@Composable
fun ConsultaCard(c: Visita, onResponder: (liberar: Boolean, recado: String?) -> Unit) {
    var recado by rememberSaveable(c.acessoId) { mutableStateOf("") }
    var enviando by remember(c.acessoId) { mutableStateOf(false) }
    LaunchedEffect(c.acessoId) { enviando = false }
    Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.ext.warnContainer, modifier = Modifier.fillMaxWidth().testTag("consulta_${c.acessoId}")) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                IconBubble(c.tipo.icon, c.tipo.color)
                Column(Modifier.weight(1f)) {
                    Text("Portaria pergunta", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.ext.warn, fontWeight = FontWeight.Bold)
                    Text("${c.nome} está na portaria", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.ext.ink)
                    Text(
                        listOfNotNull(c.tipo.label, c.obs, c.consultadoEm?.let { "desde ${Fmt.hora(it)}" }).joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.ext.muted
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                recado, { recado = it.take(300) }, placeholder = { Text("Recado para a portaria (opcional)") }, singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences), modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = { enviando = true; onResponder(false, recado) }, enabled = !enviando, modifier = Modifier.height(48.dp)) {
                    Icon(Icons.Rounded.Close, contentDescription = null, modifier = Modifier.size(18.dp)); Spacer(Modifier.size(6.dp)); Text("Recusar")
                }
                Button(onClick = { enviando = true; onResponder(true, recado) }, enabled = !enviando, modifier = Modifier.weight(1f).height(48.dp).testTag("consulta_liberar")) {
                    Icon(Icons.Rounded.DoorFront, contentDescription = null, modifier = Modifier.size(18.dp)); Spacer(Modifier.size(6.dp)); Text("Liberar entrada")
                }
            }
        }
    }
}

/** Código grande (retirada de encomenda, autorização de visita). */
@Composable
fun CodigoBox(codigo: String, legenda: String, modifier: Modifier = Modifier) {
    Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.primaryContainer, modifier = modifier) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(legenda, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
            Text(codigo, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 22.sp, letterSpacing = 3.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
        }
    }
}

@Composable
fun BoletoStatusBadge(b: Boleto) {
    val e = MaterialTheme.ext
    when (b.status) {
        StatusBoleto.PAGO -> StatusBadge(if (b.pagoComAtraso) "Pago com atraso" else "Pago", e.success, e.successContainer, icon = Icons.Rounded.Check)
        StatusBoleto.VENCIDO -> StatusBadge("Vencido", e.danger, e.dangerContainer)
        StatusBoleto.EM_ACORDO -> StatusBadge("Em acordo", e.info, e.infoContainer)
        StatusBoleto.EM_ABERTO -> StatusBadge("Em aberto", e.warn, e.warnContainer)
    }
}
