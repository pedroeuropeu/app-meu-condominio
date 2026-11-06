package br.com.condovitta.app.feature.portaria

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import br.com.condovitta.app.core.common.Photos
import br.com.condovitta.app.core.designsystem.ext
import br.com.condovitta.app.domain.Outcome
import br.com.condovitta.app.domain.PortariaRepository
import br.com.condovitta.app.domain.ViaLiberacao
import br.com.condovitta.app.domain.Visita
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Libera a entrada e, se o porteiro tirou, manda a foto de quem entrou. A entrada vale mesmo se a foto não subir
 * (a rede da guarita cai): [fotoFalhou] avisa a tela.
 */
data class EntradaComFoto(val resultado: Outcome<Visita>, val fotoFalhou: Boolean)

suspend fun PortariaRepository.entrarComFoto(visitanteId: Long, via: ViaLiberacao?, foto: ByteArray?): EntradaComFoto {
    val r = entrada(visitanteId, via)
    if (r !is Outcome.Ok || foto == null) return EntradaComFoto(r, false)
    val acesso = r.value.acessoId ?: return EntradaComFoto(r, true)
    return when (val f = fotoAcesso(acesso, foto)) {
        is Outcome.Ok -> EntradaComFoto(f, false)
        is Outcome.Err -> EntradaComFoto(r, true)
    }
}

/** Painel de liberar: "Tirar foto e liberar" (padrão) ou "Liberar sem foto". */
@Composable
fun LiberarSheet(v: Visita, onLiberar: (foto: ByteArray?) -> Unit, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var captureUri by rememberSaveable { mutableStateOf<Uri?>(null) }
    var foto by remember { mutableStateOf<ByteArray?>(null) }
    var preparando by remember { mutableStateOf(false) }
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        val uri = captureUri
        if (ok && uri != null) scope.launch {
            preparando = true
            foto = withContext(Dispatchers.IO) { Photos.compress(context, uri, maxSide = 1200) }
            preparando = false
        }
    }
    fun tirarFoto() {
        val uri = Photos.newCaptureUri(context)
        captureUri = uri
        runCatching { camera.launch(uri) }
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 20.dp).navigationBarsPadding()) {
            Text("Liberar entrada", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.ext.ink)
            Text(listOfNotNull(v.nome, v.unidade, v.tipo.label).joinToString(" · "), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.ext.muted)
            Spacer(Modifier.height(16.dp))
            val bmp = remember(foto) { Photos.decode(foto) }
            if (bmp != null) {
                Image(
                    bmp.asImageBitmap(), contentDescription = "Foto de ${v.nome}", contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxWidth().height(240.dp).clip(MaterialTheme.shapes.large)
                )
                Spacer(Modifier.height(12.dp))
                Button(onClick = { onLiberar(foto) }, modifier = Modifier.fillMaxWidth().height(58.dp).testTag("liberar_confirmar")) {
                    Icon(Icons.Rounded.Check, contentDescription = null)
                    Spacer(Modifier.size(8.dp))
                    Text("Liberar com esta foto", style = MaterialTheme.typography.titleMedium)
                }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = ::tirarFoto, modifier = Modifier.fillMaxWidth().height(52.dp)) { Text("Tirar outra foto") }
            } else {
                Button(onClick = ::tirarFoto, enabled = !preparando, modifier = Modifier.fillMaxWidth().height(58.dp).testTag("liberar_foto")) {
                    Icon(Icons.Rounded.CameraAlt, contentDescription = null)
                    Spacer(Modifier.size(8.dp))
                    Text(if (preparando) "Preparando a foto…" else "Tirar foto e liberar", style = MaterialTheme.typography.titleMedium)
                }
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(onClick = { onLiberar(null) }, modifier = Modifier.fillMaxWidth().height(52.dp).testTag("liberar_sem_foto")) { Text("Liberar sem foto") }
                }
            }
        }
    }
}
