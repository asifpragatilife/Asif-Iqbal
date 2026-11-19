package com.example.vision

import android.graphics.Bitmap
import android.graphics.Color
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

data class ParsedQrResult(
    val rawContent: String,
    val qrType: String, // URL, WIFI, CONTACT, EMAIL, TEXT
    val title: String,
    val primaryActionLabel: String,
    val secondaryDetail: String,
)

object QrEngine {

    private val scanner by lazy { BarcodeScanning.getClient() }

    suspend fun scanBitmap(bitmap: Bitmap): List<ParsedQrResult> = suspendCancellableCoroutine { cont ->
        val input = InputImage.fromBitmap(bitmap, 0)
        scanner.process(input)
            .addOnSuccessListener { barcodes ->
                val parsed = barcodes.mapNotNull { barcode ->
                    val raw = barcode.rawValue ?: return@mapNotNull null
                    classifyPayload(raw, barcode.valueType)
                }
                if (cont.isActive) cont.resume(parsed)
            }
            .addOnFailureListener {
                if (cont.isActive) cont.resume(emptyList())
            }
    }

    fun classifyPayload(raw: String, mlKitType: Int = Barcode.TYPE_UNKNOWN): ParsedQrResult {
        val trimmed = raw.trim()
        return when {
            mlKitType == Barcode.TYPE_URL || trimmed.startsWith("http://", true) || trimmed.startsWith("https://", true) ->
                ParsedQrResult(
                    rawContent = trimmed,
                    qrType = "URL",
                    title = "Website Link",
                    primaryActionLabel = "Open Link",
                    secondaryDetail = trimmed,
                )
            mlKitType == Barcode.TYPE_WIFI || trimmed.startsWith("WIFI:", true) -> {
                val ssid = Regex("S:([^;]+)").find(trimmed)?.groupValues?.getOrNull(1) ?: "Network"
                val pass = Regex("P:([^;]+)").find(trimmed)?.groupValues?.getOrNull(1) ?: ""
                ParsedQrResult(
                    rawContent = trimmed,
                    qrType = "WIFI",
                    title = "Wi-Fi: $ssid",
                    primaryActionLabel = "Copy Password",
                    secondaryDetail = if (pass.isNotBlank()) "Password: $pass" else "Open Network",
                )
            }
            mlKitType == Barcode.TYPE_EMAIL || trimmed.startsWith("mailto:", true) ||
                Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$").matches(trimmed) ->
                ParsedQrResult(
                    rawContent = trimmed,
                    qrType = "EMAIL",
                    title = "Email Address",
                    primaryActionLabel = "Compose Email",
                    secondaryDetail = trimmed.removePrefix("mailto:"),
                )
            mlKitType == Barcode.TYPE_CONTACT_INFO || trimmed.startsWith("BEGIN:VCARD", true) -> {
                val fn = Regex("FN:([^\r\n]+)").find(trimmed)?.groupValues?.getOrNull(1) ?: "Contact Card"
                ParsedQrResult(
                    rawContent = trimmed,
                    qrType = "CONTACT",
                    title = fn,
                    primaryActionLabel = "Copy vCard",
                    secondaryDetail = trimmed.lines().take(3).joinToString(" • "),
                )
            }
            else -> ParsedQrResult(
                rawContent = trimmed,
                qrType = "TEXT",
                title = "Plain Text",
                primaryActionLabel = "Copy Text",
                secondaryDetail = trimmed,
            )
        }
    }

    fun generateQrBitmap(content: String, sizePx: Int = 640): Bitmap? {
        if (content.isBlank()) return null
        return runCatching {
            val hints = mapOf(
                EncodeHintType.MARGIN to 2,
                EncodeHintType.CHARACTER_SET to "UTF-8",
            )
            val bitMatrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, sizePx, sizePx, hints)
            val bmp = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
            for (x in 0 until sizePx) {
                for (y in 0 until sizePx) {
                    bmp.setPixel(x, y, if (bitMatrix[x, y]) Color.parseColor("#0A1220") else Color.WHITE)
                }
            }
            bmp
        }.getOrNull()
    }
}
