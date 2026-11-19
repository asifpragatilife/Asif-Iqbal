package com.example.vision

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

data class SmartClassification(
    val category: String, // "ID", "Receipt", "Business Card", "Document"
    val suggestedFolderId: String,
    val suggestedTitle: String,
    val tags: List<String>,
)

object OnDeviceOcrEngine {

    private val recognizer by lazy {
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    }

    /**
     * Runs Google ML Kit Text Recognition 100% on-device offline.
     */
    suspend fun recognizeText(bitmap: Bitmap): String = suspendCancellableCoroutine { cont ->
        val input = InputImage.fromBitmap(bitmap, 0)
        recognizer.process(input)
            .addOnSuccessListener { result ->
                if (cont.isActive) cont.resume(result.text.trim())
            }
            .addOnFailureListener {
                if (cont.isActive) cont.resume("")
            }
    }

    /**
     * Offline heuristic classifier to auto-tag and categorize scans as ID, Receipt, Business Card, or Document.
     */
    fun classify(ocrText: String, width: Int = 1000, height: Int = 1400): SmartClassification {
        val lower = ocrText.lowercase()
        val lines = ocrText.lines().map { it.trim() }.filter { it.isNotEmpty() }

        val receiptScore = listOf("total", "subtotal", "tax", "receipt", "cashier", "qty", "usd", "$", "bdt", "৳", "invoice")
            .count { lower.contains(it) }
        val idScore = listOf("passport", "national id", "nid", "date of birth", "dob", "driver license", "nationality", "expires", "id no")
            .count { lower.contains(it) }
        val cardScore = listOf("@", "tel", "mobile", "ceo", "director", "founder", "www.", "linkedin", "ext.")
            .count { lower.contains(it) } + if (width > height && lines.size in 2..10) 1 else 0

        return when {
            idScore >= 2 -> SmartClassification(
                category = "ID",
                suggestedFolderId = "folder_ids",
                suggestedTitle = lines.firstOrNull()?.take(36) ?: "Identity Document",
                tags = listOf("ID", "Verified", "Vault Ready"),
            )
            receiptScore >= 2 -> SmartClassification(
                category = "Receipt",
                suggestedFolderId = "folder_receipts",
                suggestedTitle = lines.firstOrNull()?.take(36)?.let { "$it Receipt" } ?: "Expense Receipt",
                tags = listOf("Receipt", "Expense", "Tax"),
            )
            cardScore >= 2 -> SmartClassification(
                category = "Business Card",
                suggestedFolderId = "folder_work",
                suggestedTitle = lines.firstOrNull()?.take(36) ?: "Business Contact",
                tags = listOf("Contact", "Card"),
            )
            else -> SmartClassification(
                category = "Document",
                suggestedFolderId = "folder_work",
                suggestedTitle = lines.firstOrNull()?.take(36) ?: "Scanned Document",
                tags = listOf("Scan", "Offline OCR"),
            )
        }
    }
}
