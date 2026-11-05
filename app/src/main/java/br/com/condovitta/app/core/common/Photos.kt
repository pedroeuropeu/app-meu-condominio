package br.com.condovitta.app.core.common

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.ByteArrayOutputStream
import java.io.File

/** Fotos tiradas pela câmera do aparelho (encomenda na chegada, livro da portaria). */
object Photos {

    /** Arquivo temporário (no cache, fora do backup) e o Uri que a câmera do sistema grava. */
    fun newCaptureUri(context: Context): Uri {
        val dir = File(context.cacheDir, "fotos").apply { mkdirs() }
        dir.listFiles()?.filter { System.currentTimeMillis() - it.lastModified() > 3_600_000 }?.forEach { it.delete() }
        val file = File(dir, "foto-${System.currentTimeMillis()}.jpg")
        return FileProvider.getUriForFile(context, "${context.packageName}.fotos", file)
    }

    /** Lê a foto, endireita pela orientação do EXIF e reduz para no máximo [maxSide] px em JPEG (upload leve). */
    fun compress(context: Context, uri: Uri, maxSide: Int = 1600, quality: Int = 80): ByteArray? = runCatching {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0) return null
        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= maxSide) sample *= 2
        val decoded = context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample }) } ?: return null
        val rotation = context.contentResolver.openInputStream(uri)?.use {
            when (ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                else -> 0f
            }
        } ?: 0f
        val scale = minOf(1f, maxSide.toFloat() / maxOf(decoded.width, decoded.height))
        val m = Matrix().apply { postRotate(rotation); postScale(scale, scale) }
        val out = Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, m, true)
        ByteArrayOutputStream().use { bos ->
            out.compress(Bitmap.CompressFormat.JPEG, quality, bos)
            if (out != decoded) out.recycle()
            decoded.recycle()
            bos.toByteArray()
        }
    }.getOrNull()

    fun decode(bytes: ByteArray?): Bitmap? = bytes?.let { runCatching { BitmapFactory.decodeByteArray(it, 0, it.size) }.getOrNull() }
}
