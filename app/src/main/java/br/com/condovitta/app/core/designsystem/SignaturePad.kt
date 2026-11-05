package br.com.condovitta.app.core.designsystem

import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Paint
import android.graphics.Path as AndroidPath
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import java.io.ByteArrayOutputStream

/** Traços da assinatura (em pixels do quadro) e a exportação para PNG. */
@Stable
class SignatureState {
    val strokes = mutableStateListOf<List<Offset>>()
    internal val current = mutableStateListOf<Offset>()
    internal val size = mutableStateOf(Size.Zero)

    val isEmpty: Boolean get() = strokes.isEmpty() && current.isEmpty()

    fun clear() {
        strokes.clear()
        current.clear()
    }

    /** PNG com fundo branco e traço escuro, reduzido para no máximo [maxWidth] px (o backend limita o tamanho). */
    fun toPng(maxWidth: Int = 600): ByteArray? {
        val s = size.value
        if (isEmpty || s.width <= 0f || s.height <= 0f) return null
        val scale = minOf(1f, maxWidth / s.width)
        val w = (s.width * scale).toInt().coerceAtLeast(1)
        val h = (s.height * scale).toInt().coerceAtLeast(1)
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = AndroidCanvas(bmp)
        canvas.drawColor(android.graphics.Color.WHITE)
        val paint = Paint().apply {
            color = 0xFF0A2C55.toInt(); strokeWidth = 4f * scale.coerceAtLeast(0.6f); style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND; isAntiAlias = true
        }
        strokes.forEach { pts ->
            if (pts.size == 1) canvas.drawPoint(pts[0].x * scale, pts[0].y * scale, paint)
            else canvas.drawPath(AndroidPath().apply {
                moveTo(pts[0].x * scale, pts[0].y * scale)
                pts.drop(1).forEach { lineTo(it.x * scale, it.y * scale) }
            }, paint)
        }
        return ByteArrayOutputStream().use { out -> bmp.compress(Bitmap.CompressFormat.PNG, 100, out); bmp.recycle(); out.toByteArray() }
    }
}

@Composable
fun rememberSignatureState() = remember { SignatureState() }

/** Quadro para assinar com o dedo (fundo branco nos dois temas, como papel). */
@Composable
fun SignaturePad(state: SignatureState, modifier: Modifier = Modifier, height: Dp = 160.dp) {
    Box(
        modifier
            .fillMaxWidth()
            .height(height)
            .clip(MaterialTheme.shapes.medium)
            .background(Color.White)
            .border(1.dp, MaterialTheme.colorScheme.outline, MaterialTheme.shapes.medium)
            .testTag("assinatura")
    ) {
        if (state.isEmpty) Text("Assine aqui com o dedo", color = Color(0xFF93A1B6), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.align(Alignment.Center))
        Canvas(
            Modifier
                .matchParentSize()
                .onSizeChanged { state.size.value = Size(it.width.toFloat(), it.height.toFloat()) }
                .pointerInput(state) {
                    detectDragGestures(
                        onDragStart = { state.current.clear(); state.current.add(it) },
                        onDrag = { change, _ -> change.consume(); state.current.add(change.position) },
                        onDragEnd = { state.strokes.add(state.current.toList()); state.current.clear() },
                        onDragCancel = { state.strokes.add(state.current.toList()); state.current.clear() }
                    )
                }
        ) {
            val stroke = Stroke(width = 4.dp.toPx() / 1.5f, cap = StrokeCap.Round, join = StrokeJoin.Round)
            (state.strokes + listOf(state.current.toList())).forEach { pts ->
                if (pts.size > 1) drawPath(Path().apply { moveTo(pts[0].x, pts[0].y); pts.drop(1).forEach { lineTo(it.x, it.y) } }, Color(0xFF0A2C55), style = stroke)
            }
        }
        Box(Modifier.align(Alignment.BottomStart).padding(start = 16.dp, end = 16.dp, bottom = 28.dp).fillMaxWidth().height(1.dp).background(Color(0xFFCEDBE3)))
    }
}
