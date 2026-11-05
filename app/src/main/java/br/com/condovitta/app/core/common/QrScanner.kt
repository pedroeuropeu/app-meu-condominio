package br.com.condovitta.app.core.common

import android.content.Context
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning

/**
 * Lê um QR com o leitor do Google Play Services (abre a câmera dele; o app não pede permissão de câmera).
 * [onCodigo] recebe o texto lido; [onErro] quando o leitor não está disponível no aparelho.
 */
fun lerQr(context: Context, onCodigo: (String) -> Unit, onErro: (String) -> Unit) {
    val options = GmsBarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_QR_CODE).enableAutoZoom().build()
    GmsBarcodeScanning.getClient(context, options).startScan()
        .addOnSuccessListener { b -> b.rawValue?.takeIf { it.isNotBlank() }?.let(onCodigo) }
        .addOnFailureListener { onErro("Não deu para abrir o leitor de QR. Digite o código na busca.") }
}

/** O QR da autorização de visita traz o código de 6 dígitos (às vezes dentro de um texto ou link). */
fun codigoDoQr(texto: String): String? = Regex("\\b(\\d{6})\\b").find(texto)?.groupValues?.get(1)
