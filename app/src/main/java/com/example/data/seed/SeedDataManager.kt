package com.example.data.seed

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import com.example.data.local.AppDatabase
import com.example.data.local.DocumentEntity
import com.example.data.local.FolderEntity
import com.example.data.local.PageEntity
import com.example.data.local.QrHistoryEntity
import com.example.data.local.SignatureEntity
import com.example.data.local.VaultActivityEntity
import com.example.data.preferences.AppPreferences
import com.example.security.VaultCryptoManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

object SeedDataManager {

    suspend fun seedIfNeeded(
        context: Context,
        db: AppDatabase,
        preferences: AppPreferences,
        crypto: VaultCryptoManager,
    ) = withContext(Dispatchers.IO) {
        val prefs = preferences.current()
        if (prefs.pinHash.isBlank()) {
            val salt = crypto.generateSaltBase64()
            val hash = crypto.hashPinBase64(VaultCryptoManager.DEFAULT_DEMO_PIN, salt)
            preferences.savePinCredentials(hash, salt)
        }

        if (db.documentDao().countAll() > 0) return@withContext

        val now = System.currentTimeMillis()
        val docsDir = File(context.filesDir, "documents").apply { mkdirs() }
        val sigDir = File(context.filesDir, "signatures").apply { mkdirs() }

        val folders = listOf(
            FolderEntity("folder_work", "Work Contracts", "blue", now - 500_000),
            FolderEntity("folder_receipts", "Receipts", "orange", now - 400_000),
            FolderEntity("folder_ids", "Identity & IDs", "green", now - 300_000),
            FolderEntity("folder_finance", "Finance", "purple", now - 200_000),
        )
        folders.forEach { db.folderDao().upsert(it) }

        // 1. NDA Agreement (PDF, 2 pages)
        val ndaP1 = renderDocumentMockup(
            header = "NON-DISCLOSURE AGREEMENT",
            accentColor = Color.parseColor("#2F7BFF"),
            lines = listOf(
                "Effective Date: November 14, 2026",
                "Parties: AsifAraf Labs & Meridian Security",
                "",
                "1. CONFIDENTIAL INFORMATION",
                "All technical blueprints, encryption keys,",
                "and offline vision pipelines remain strictly",
                "confidential and stored locally on device.",
                "",
                "2. TERM & COMPLIANCE",
                "This agreement remains valid for 36 months.",
                "Signed: ____________________",
            ),
            footer = "Page 1 of 2 • Verified Offline Scan",
        )
        val ndaP2 = renderDocumentMockup(
            header = "SCHEDULE A — SECURITY ANNEX",
            accentColor = Color.parseColor("#2F7BFF"),
            lines = listOf(
                "Zero-Knowledge Storage Mandate:",
                "- AES-256-GCM local encryption",
                "- PBKDF2-HMAC-SHA256 key derivation",
                "- No external analytics or telemetry",
                "",
                "Authorized Signatory: Asif Rahman",
                "Date: 2026-11-14",
            ),
            footer = "Page 2 of 2 • AsifAraf Archive",
        )
        val nda1File = saveJpg(ndaP1, File(docsDir, "seed_nda_1.jpg"))
        val nda2File = saveJpg(ndaP2, File(docsDir, "seed_nda_2.jpg"))
        db.documentDao().upsert(
            DocumentEntity(
                id = "doc_seed_nda",
                title = "Meridian NDA Agreement",
                folderId = "folder_work",
                docType = "PDF",
                category = "Document",
                createdAt = now - 86_400_000L * 2,
                updatedAt = now - 86_400_000L * 1,
                pageCount = 2,
                totalSizeBytes = nda1File.length() + nda2File.length(),
                isLockedInVault = false,
                ocrText = "NON-DISCLOSURE AGREEMENT\nEffective Date: November 14, 2026\nParties: AsifAraf Labs & Meridian Security\nAll technical blueprints and encryption keys remain strictly confidential.",
                tagsCsv = "Contract,NDA,Legal",
                thumbnailPath = nda1File.absolutePath,
            ),
        )
        db.pageDao().upsertAll(
            listOf(
                PageEntity("p_nda_1", "doc_seed_nda", 0, nda1File.absolutePath, false, "Auto", ocrText = "NON-DISCLOSURE AGREEMENT\nEffective Date: November 14, 2026"),
                PageEntity("p_nda_2", "doc_seed_nda", 1, nda2File.absolutePath, false, "Auto", ocrText = "SCHEDULE A — SECURITY ANNEX\nAES-256-GCM local encryption"),
            ),
        )

        // 2. Q4 Expense Sheet (XLSX)
        val sheetBmp = renderSpreadsheetMockup()
        val sheetFile = saveJpg(sheetBmp, File(docsDir, "seed_q4_sheet.jpg"))
        db.documentDao().upsert(
            DocumentEntity(
                id = "doc_seed_sheet",
                title = "Q4 Cloud & Hardware Ledger",
                folderId = "folder_finance",
                docType = "XLSX",
                category = "Document",
                createdAt = now - 86_400_000L * 3,
                updatedAt = now - 3_600_000L * 5,
                pageCount = 1,
                totalSizeBytes = sheetFile.length(),
                isLockedInVault = false,
                ocrText = "Item | Category | Amount\nMacBook Pro M4 | Hardware | \$2499.00\nYubiKey 5C NFC | Security | \$55.00\nDesk Dock | Office | \$189.00\nTotal | USD | \$2743.00",
                tagsCsv = "Budget,Q4,Excel",
                thumbnailPath = sheetFile.absolutePath,
            ),
        )
        db.pageDao().upsert(
            PageEntity("p_sheet_1", "doc_seed_sheet", 0, sheetFile.absolutePath, false, "Magic Color"),
        )

        // 3. Coffee & Supplies Receipt (JPG)
        val receiptBmp = renderReceiptMockup()
        val receiptFile = saveJpg(receiptBmp, File(docsDir, "seed_receipt.jpg"))
        db.documentDao().upsert(
            DocumentEntity(
                id = "doc_seed_receipt",
                title = "North End Coffee & Stationery",
                folderId = "folder_receipts",
                docType = "JPG",
                category = "Receipt",
                createdAt = now - 86_400_000L * 4,
                updatedAt = now - 3_600_000L * 9,
                pageCount = 1,
                totalSizeBytes = receiptFile.length(),
                isLockedInVault = false,
                ocrText = "NORTH END COFFEE ROASTERS\nReceipt #40812\nPour Over Blend   \$6.50\nMoleskine Grid    \$22.00\nSubtotal          \$28.50\nTax (8%)          \$2.28\nTOTAL             \$30.78",
                tagsCsv = "Receipt,Expense,Tax",
                thumbnailPath = receiptFile.absolutePath,
            ),
        )
        db.pageDao().upsert(
            PageEntity("p_receipt_1", "doc_seed_receipt", 0, receiptFile.absolutePath, false, "B&W"),
        )

        // 4. Architecture Proposal (DOCX)
        val proposalBmp = renderDocumentMockup(
            header = "ASIFARAF MOBILE ARCHITECTURE",
            accentColor = Color.parseColor("#34D399"),
            lines = listOf(
                "Prepared for Product Review — v2.4",
                "",
                "Executive Summary:",
                "All document capture, ML Kit text recognition,",
                "perspective unwarping, and PDF flattening run",
                "entirely inside the user's device sandbox.",
                "",
                "Zero telemetry packets are transmitted.",
                "Optional cloud sync remains disabled by default.",
            ),
            footer = "Word Export Ready • .DOCX",
        )
        val proposalFile = saveJpg(proposalBmp, File(docsDir, "seed_proposal.jpg"))
        db.documentDao().upsert(
            DocumentEntity(
                id = "doc_seed_proposal",
                title = "Offline AI Architecture Brief",
                folderId = "folder_work",
                docType = "DOCX",
                category = "Document",
                createdAt = now - 86_400_000L * 5,
                updatedAt = now - 86_400_000L * 2,
                pageCount = 1,
                totalSizeBytes = proposalFile.length(),
                isLockedInVault = false,
                ocrText = "ASIFARAF MOBILE ARCHITECTURE\nAll document capture, ML Kit text recognition, and PDF flattening run entirely inside the user's device sandbox.",
                tagsCsv = "Brief,Word,Architecture",
                thumbnailPath = proposalFile.absolutePath,
            ),
        )
        db.pageDao().upsert(
            PageEntity("p_proposal_1", "doc_seed_proposal", 0, proposalFile.absolutePath, false, "Original"),
        )

        // 5. Vaulted Encrypted Passport ID
        val passportBmp = renderIdCardMockup(
            title = "PASSPORT • BIOMETRIC ID",
            holder = "ASIF ARAF",
            idNumber = "A08492710",
            expiry = "14 NOV 2031",
            accent = Color.parseColor("#22D3EE"),
        )
        val tmpPass = saveJpg(passportBmp, File(context.cacheDir, "tmp_seed_pass.jpg"))
        val encPass = crypto.encryptFileToVault(tmpPass, "doc_vault_passport_0")
        val passLen = tmpPass.length()
        tmpPass.delete()

        db.documentDao().upsert(
            DocumentEntity(
                id = "doc_vault_passport",
                title = "Passport — Asif Araf",
                folderId = "folder_ids",
                docType = "PDF",
                category = "ID",
                createdAt = now - 86_400_000L * 10,
                updatedAt = now - 3_600_000L * 2,
                pageCount = 1,
                totalSizeBytes = passLen,
                isLockedInVault = true,
                ocrText = "PASSPORT\nName: ASIF ARAF\nPassport No: A08492710\nDate of Expiry: 14 NOV 2031",
                tagsCsv = "Passport,ID,Encrypted",
                thumbnailPath = "",
            ),
        )
        db.pageDao().upsert(
            PageEntity("p_vault_pass", "doc_vault_passport", 0, encPass.absolutePath, true, "Original"),
        )

        // 6. Vaulted Encrypted Tax & Recovery Key Sheet
        val taxBmp = renderIdCardMockup(
            title = "RECOVERY KEYS & TAX PIN",
            holder = "OBSIDIAN MASTER BACKUP",
            idNumber = "TIN-992-4810-BD",
            expiry = "CONFIDENTIAL",
            accent = Color.parseColor("#A78BFA"),
        )
        val tmpTax = saveJpg(taxBmp, File(context.cacheDir, "tmp_seed_tax.jpg"))
        val encTax = crypto.encryptFileToVault(tmpTax, "doc_vault_tax_0")
        val taxLen = tmpTax.length()
        tmpTax.delete()

        db.documentDao().upsert(
            DocumentEntity(
                id = "doc_vault_tax",
                title = "Tax Certificate & Recovery Codes",
                folderId = "folder_finance",
                docType = "PDF",
                category = "Document",
                createdAt = now - 86_400_000L * 14,
                updatedAt = now - 86_400_000L * 6,
                pageCount = 1,
                totalSizeBytes = taxLen,
                isLockedInVault = true,
                ocrText = "TIN-992-4810-BD\nConfidential Backup Codes",
                tagsCsv = "Tax,Confidential",
                thumbnailPath = "",
            ),
        )
        db.pageDao().upsert(
            PageEntity("p_vault_tax", "doc_vault_tax", 0, encTax.absolutePath, true, "Original"),
        )

        // Sample Signature
        val sigBmp = renderSampleSignature()
        val sigFile = File(sigDir, "sig_default.png")
        FileOutputStream(sigFile).use { sigBmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
        db.signatureDao().upsert(
            SignatureEntity("sig_default", "Asif Primary Ink", sigFile.absolutePath, now - 86_400_000L),
        )

        // Sample QR History
        db.qrHistoryDao().upsert(
            QrHistoryEntity(
                id = "qr_seed_1",
                rawContent = "WIFI:T:WPA;S:ObsidianStudio_5G;P:ZeroTrust2026;;",
                qrType = "WIFI",
                summary = "Wi-Fi: ObsidianStudio_5G",
                isGenerated = false,
                createdAt = now - 7_200_000L,
            ),
        )
        db.qrHistoryDao().upsert(
            QrHistoryEntity(
                id = "qr_seed_2",
                rawContent = "https://asifaraf.local/verify/doc-409",
                qrType = "URL",
                summary = "Website Link",
                isGenerated = true,
                createdAt = now - 14_400_000L,
            ),
        )

        // Sample Vault Audit Log
        db.vaultActivityDao().insert(
            VaultActivityEntity(
                id = "act_seed_1",
                action = "Vault Initialized",
                detail = "AES-256-GCM Keystore & PBKDF2 PIN configured",
                timestamp = now - 86_400_000L * 2,
                isSuccess = true,
            ),
        )
        db.vaultActivityDao().insert(
            VaultActivityEntity(
                id = "act_seed_2",
                action = "Encrypted File Sealed",
                detail = "Passport — Asif Araf (zero plaintext on disk)",
                timestamp = now - 3_600_000L * 2,
                isSuccess = true,
            ),
        )
    }

    private fun saveJpg(bmp: Bitmap, file: File): File {
        FileOutputStream(file).use { bmp.compress(Bitmap.CompressFormat.JPEG, 92, it) }
        return file
    }

    private fun renderDocumentMockup(
        header: String,
        accentColor: Int,
        lines: List<String>,
        footer: String,
    ): Bitmap {
        val w = 800
        val h = 1100
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        c.drawColor(Color.parseColor("#F8FAFC"))

        val topBar = Paint().apply { color = accentColor }
        c.drawRect(0f, 0f, w.toFloat(), 20f, topBar)

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#0F172A")
            textSize = 34f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        c.drawText(header, 56f, 110f, titlePaint)

        val divider = Paint().apply {
            color = accentColor
            strokeWidth = 4f
        }
        c.drawLine(56f, 134f, w - 56f, 134f, divider)

        val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#334155")
            textSize = 26f
            typeface = Typeface.SANS_SERIF
        }
        var y = 205f
        lines.forEach { line ->
            c.drawText(line, 56f, y, bodyPaint)
            y += 52f
        }

        val footPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#64748B")
            textSize = 20f
        }
        c.drawText(footer, 56f, h - 54f, footPaint)
        return bmp
    }

    private fun renderReceiptMockup(): Bitmap {
        val w = 720
        val h = 1040
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        c.drawColor(Color.parseColor("#FFFDF7"))

        val monoBold = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#111827")
            textSize = 32f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        }
        val mono = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#374151")
            textSize = 25f
            typeface = Typeface.MONOSPACE
        }
        c.drawText("NORTH END COFFEE", 140f, 100f, monoBold)
        c.drawText("--------------------------------", 70f, 150f, mono)
        val items = listOf(
            "Receipt #40812",
            "Date: 2026-11-12  09:41 AM",
            "",
            "Pour Over Blend          $6.50",
            "Moleskine Grid Pad       $22.00",
            "--------------------------------",
            "Subtotal                $28.50",
            "Tax (8.0%)               $2.28",
            "TOTAL                   $30.78",
            "",
            "PAID VIA CONTACTLESS",
            "THANK YOU FOR VISITING",
        )
        var y = 210f
        items.forEach {
            c.drawText(it, 70f, y, mono)
            y += 56f
        }
        return bmp
    }

    private fun renderSpreadsheetMockup(): Bitmap {
        val w = 800
        val h = 1040
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        c.drawColor(Color.WHITE)

        val headerBg = Paint().apply { color = Color.parseColor("#0F1B2D") }
        c.drawRect(40f, 80f, w - 40f, 155f, headerBg)

        val hText = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 26f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        c.drawText("Q4 HARDWARE & SECURITY LEDGER", 64f, 128f, hText)

        val border = Paint().apply {
            color = Color.parseColor("#CBD5E1")
            style = Paint.Style.STROKE
            strokeWidth = 2f
        }
        val cellText = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#1E293B")
            textSize = 24f
        }
        val rows = listOf(
            Triple("Item", "Category", "Amount"),
            Triple("MacBook Pro M4", "Hardware", "$2,499.00"),
            Triple("YubiKey 5C NFC", "Security", "$55.00"),
            Triple("CalDigit TS4 Dock", "Office", "$189.00"),
            Triple("Encrypted SSD 2TB", "Backup", "$165.00"),
            Triple("Total Spend", "USD", "$2,908.00"),
        )
        var top = 160f
        rows.forEachIndexed { idx, (a, b, v) ->
            if (idx == 0 || idx == rows.lastIndex) {
                val fill = Paint().apply { color = Color.parseColor("#EFF6FF") }
                c.drawRect(40f, top, w - 40f, top + 84f, fill)
            }
            c.drawRect(40f, top, w - 40f, top + 84f, border)
            c.drawText(a, 60f, top + 52f, cellText)
            c.drawText(b, 390f, top + 52f, cellText)
            c.drawText(v, 590f, top + 52f, cellText)
            top += 84f
        }
        return bmp
    }

    private fun renderIdCardMockup(
        title: String,
        holder: String,
        idNumber: String,
        expiry: String,
        accent: Int,
    ): Bitmap {
        val w = 860
        val h = 560
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val bg = Paint().apply {
            shader = LinearGradient(
                0f, 0f, w.toFloat(), h.toFloat(),
                Color.parseColor("#0A1220"), Color.parseColor("#192B47"), Shader.TileMode.CLAMP,
            )
        }
        c.drawRoundRect(RectF(0f, 0f, w.toFloat(), h.toFloat()), 32f, 32f, bg)

        val accentPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = accent; textSize = 26f; typeface = Typeface.DEFAULT_BOLD }
        val whitePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; textSize = 36f; typeface = Typeface.DEFAULT_BOLD }
        val mutedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#94A3B8"); textSize = 22f }

        c.drawText(title, 48f, 76f, accentPaint)
        c.drawRoundRect(RectF(48f, 130f, 228f, 340f), 20f, 20f, Paint().apply { color = Color.parseColor("#233B61") })
        c.drawText("HOLDER", 270f, 165f, mutedPaint)
        c.drawText(holder, 270f, 212f, whitePaint)
        c.drawText("DOCUMENT ID", 270f, 275f, mutedPaint)
        c.drawText(idNumber, 270f, 320f, whitePaint)
        c.drawText("VALID UNTIL: $expiry", 48f, 440f, accentPaint)
        c.drawText("P<BGD<<ASIF<<ARAF<<<<<<<<<<<<<<<<<<<<<", 48f, 504f, mutedPaint)
        return bmp
    }

    private fun renderSampleSignature(): Bitmap {
        val bmp = Bitmap.createBitmap(560, 220, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#0F1B2D")
            style = Paint.Style.STROKE
            strokeWidth = 9f
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }
        val path = Path().apply {
            moveTo(45f, 155f)
            cubicTo(90f, 35f, 135f, 35f, 165f, 150f)
            cubicTo(185f, 110f, 230f, 80f, 260f, 140f)
            cubicTo(295f, 165f, 325f, 65f, 375f, 95f)
            cubicTo(420f, 120f, 455f, 145f, 515f, 118f)
        }
        c.drawPath(path, p)
        return bmp
    }
}
