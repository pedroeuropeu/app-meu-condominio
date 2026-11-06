package br.com.condovitta.app.feature.servicos

import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.InsertDriveFile
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarOutline
import androidx.compose.material3.AssistChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import br.com.condovitta.app.core.common.Photos
import br.com.condovitta.app.core.designsystem.BackTopBar
import br.com.condovitta.app.core.designsystem.ext
import br.com.condovitta.app.domain.Anexo
import br.com.condovitta.app.domain.ArquivoEnvio
import br.com.condovitta.app.domain.Outcome
import br.com.condovitta.app.feature.morador.abrirArquivo
import br.com.condovitta.app.feature.portaria.ErrorBox
import br.com.condovitta.app.feature.portaria.Loadable
import br.com.condovitta.app.feature.portaria.LoadingBox
import br.com.condovitta.app.ui.Messenger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.math.BigDecimal
import java.math.RoundingMode

/* Peças comuns das telas de serviços do morador (chamados, ocorrências, documentos, assembleias, contas, gás e calendário). */

/** Carrega mantendo o dado anterior na tela enquanto atualiza. */
internal suspend fun <T> MutableStateFlow<Loadable<T>>.carregar(block: suspend () -> Outcome<T>) {
    update { it.copy(loading = true, error = null) }
    value = when (val r = block()) {
        is Outcome.Ok -> Loadable(r.value, loading = false)
        is Outcome.Err -> Loadable(value.data, false, r.error.message)
    }
}

/** Baixa um arquivo e entrega os bytes para a tela abrir; erro vira aviso. */
internal suspend fun baixar(messenger: Messenger, baixando: MutableStateFlow<String?>, chave: String, block: suspend () -> Outcome<ByteArray>, abrir: (ByteArray) -> Unit) {
    if (baixando.value != null) return
    baixando.value = chave
    when (val r = block()) {
        is Outcome.Ok -> abrir(r.value)
        is Outcome.Err -> messenger.show(r.error.message)
    }
    baixando.value = null
}

fun mimeDe(nome: String, tipo: String? = null): String = tipo?.takeIf { it.isNotBlank() } ?: when (nome.substringAfterLast('.', "").lowercase()) {
    "pdf" -> "application/pdf"
    "png" -> "image/png"
    "jpg", "jpeg" -> "image/jpeg"
    "ics" -> "text/calendar"
    "mp4" -> "video/mp4"
    else -> "application/octet-stream"
}

fun abrirAnexo(context: Context, bytes: ByteArray, a: Anexo) = abrirArquivo(context, bytes, a.nome, mimeDe(a.nome, a.tipo))

/** Tipo do arquivo pelos primeiros bytes (nota fiscal e foto chegam sem nome). */
fun mimeDosBytes(b: ByteArray): Pair<String, String> = when {
    b.size > 4 && b[0] == '%'.code.toByte() && b[1] == 'P'.code.toByte() && b[2] == 'D'.code.toByte() && b[3] == 'F'.code.toByte() -> "application/pdf" to "pdf"
    b.size > 4 && b[0] == 0x89.toByte() && b[1] == 'P'.code.toByte() && b[2] == 'N'.code.toByte() -> "image/png" to "png"
    b.size > 3 && b[0] == 0xFF.toByte() && b[1] == 0xD8.toByte() -> "image/jpeg" to "jpg"
    else -> "application/octet-stream" to "bin"
}

/** Abre um arquivo baixado cujo tipo só se sabe pelo conteúdo. */
fun abrirBaixado(context: Context, bytes: ByteArray, base: String, titulo: String): Boolean {
    val (mime, ext) = mimeDosBytes(bytes)
    return abrirArquivo(context, bytes, "$base.$ext", mime, titulo)
}

/** "+3,2%" / "−1,8%" */
fun pct(v: BigDecimal?): String? = v?.let { (if (it.signum() > 0) "+" else if (it.signum() < 0) "−" else "") + it.abs().setScale(1, RoundingMode.HALF_UP).toPlainString().replace('.', ',') + "%" }

/** "12,5 m³" */
fun m3(v: BigDecimal?): String = v?.let { it.setScale(1, RoundingMode.HALF_UP).toPlainString().replace('.', ',') + " m³" } ?: "—"

fun tamanho(bytes: Long): String = when {
    bytes >= 1_048_576 -> "%.1f MB".format(bytes / 1_048_576.0).replace('.', ',')
    bytes >= 1024 -> "${bytes / 1024} KB"
    else -> "$bytes B"
}

@Composable
internal fun chipColors() = FilterChipDefaults.filterChipColors(selectedContainerColor = MaterialTheme.colorScheme.primary, selectedLabelColor = MaterialTheme.colorScheme.onPrimary)

/** Escolher fotos da galeria (sem permissão), já reduzidas para envio. */
@Composable
fun rememberSeletorFotos(max: Int = 5, onFotos: (List<ArquivoEnvio>) -> Unit): () -> Unit {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(max.coerceAtLeast(2))) { uris ->
        if (uris.isEmpty()) return@rememberLauncherForActivityResult
        scope.launch {
            val fotos = withContext(Dispatchers.IO) {
                uris.take(max).mapIndexedNotNull { i, u -> Photos.compress(context, u)?.let { ArquivoEnvio("foto-${System.currentTimeMillis()}-$i.jpg", "image/jpeg", it) } }
            }
            onFotos(fotos)
        }
    }
    return { launcher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
}

/** Fotos escolhidas para enviar (com o x para tirar) e o botão de adicionar. */
@Composable
fun AnexosEnvio(fotos: List<ArquivoEnvio>, onAdicionar: () -> Unit, onRemover: (ArquivoEnvio) -> Unit, max: Int = 5, rotulo: String = "Adicionar fotos") {
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        fotos.forEachIndexed { i, f ->
            InputChip(
                selected = false, onClick = { onRemover(f) }, label = { Text("Foto ${i + 1} · ${tamanho(f.bytes.size.toLong())}") },
                leadingIcon = { Icon(Icons.Rounded.Image, contentDescription = null, modifier = Modifier.size(18.dp)) },
                trailingIcon = { Icon(Icons.Rounded.Close, contentDescription = "Tirar foto ${i + 1}", modifier = Modifier.size(18.dp)) }
            )
        }
        if (fotos.size < max) AssistChip(onClick = onAdicionar, label = { Text(rotulo) }, leadingIcon = { Icon(Icons.Rounded.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(18.dp)) })
    }
}

/** Anexo recebido: toque para abrir. */
@Composable
fun AnexoChip(a: Anexo, baixando: Boolean, onClick: () -> Unit) {
    AssistChip(
        onClick = onClick, enabled = !baixando,
        label = { Text(a.nome, maxLines = 1) },
        leadingIcon = {
            Icon(
                when { a.imagem -> Icons.Rounded.Image; a.tipo == "application/pdf" -> Icons.Rounded.PictureAsPdf; else -> Icons.AutoMirrored.Rounded.InsertDriveFile },
                contentDescription = null, modifier = Modifier.size(18.dp)
            )
        }
    )
}

@Composable
fun Estrelas(nota: Int, onNota: ((Int) -> Unit)?, tamanho: Int = 36) {
    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        (1..5).forEach { n ->
            val cheia = n <= nota
            val ic = if (cheia) Icons.Rounded.Star else Icons.Rounded.StarOutline
            val tint = if (cheia) Color(0xFFF59E0B) else MaterialTheme.ext.faint
            if (onNota != null) IconButton(onClick = { onNota(n) }) { Icon(ic, contentDescription = "$n estrela(s)", tint = tint, modifier = Modifier.size(tamanho.dp)) }
            else Icon(ic, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
        }
    }
}

/**
 * Tela padrão dos serviços: barra com voltar, puxar para atualizar e os estados carregando/erro. Atualiza ao voltar para a tela.
 */
@Composable
fun <T> TelaServico(
    titulo: String,
    state: Loadable<T>,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    fab: @Composable () -> Unit = {},
    actions: @Composable () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    scroll: Boolean = true,
    content: @Composable ColumnScope.(T) -> Unit
) {
    LifecycleResumeEffect(Unit) {
        onRefresh()
        onPauseOrDispose { }
    }
    Scaffold(
        topBar = { BackTopBar(titulo, onBack = onBack, actions = actions) },
        floatingActionButton = fab,
        bottomBar = bottomBar,
        containerColor = MaterialTheme.colorScheme.background
    ) { p ->
        val d = state.data
        PullToRefreshBox(isRefreshing = state.loading && d != null, onRefresh = onRefresh, modifier = Modifier.padding(p).fillMaxSize()) {
            val mod = if (scroll) Modifier.fillMaxSize().verticalScroll(rememberScrollState()) else Modifier.fillMaxSize()
            Column(mod.padding(horizontal = 16.dp)) {
                when {
                    d == null && state.loading -> LoadingBox()
                    d == null -> ErrorBox(state.error ?: "Não deu para carregar.", onRetry = onRefresh)
                    else -> content(d)
                }
            }
        }
    }
}

/** Espaço no fim da lista para o botão flutuante não cobrir o último item. */
@Composable
fun FimDaLista(fab: Boolean = true) = Spacer(Modifier.height(if (fab) 96.dp else 24.dp))

/** Linha clicável dentro de um cartão (título, linha de apoio e um selo à direita). */
@Composable
fun LinhaServico(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    cor: Color,
    titulo: String,
    sub: String?,
    detalhe: String? = null,
    onClick: (() -> Unit)?,
    trailing: @Composable (() -> Unit)? = null
) {
    val mod = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
    Row(mod.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        br.com.condovitta.app.core.designsystem.IconBubble(icon, cor)
        Column(Modifier.weight(1f)) {
            Text(titulo, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.ext.ink)
            sub?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.ext.muted, maxLines = 2) }
            detalhe?.let { Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.ext.faint) }
        }
        trailing?.let { Box { it() } }
    }
}
