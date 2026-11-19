package com.example.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.example.data.local.AppDatabase
import com.example.data.local.DocumentEntity
import com.example.data.local.FolderEntity
import com.example.data.local.PageEntity
import com.example.data.local.QrHistoryEntity
import com.example.data.local.SignatureEntity
import com.example.data.local.VaultActivityEntity
import com.example.data.preferences.AppPreferences
import com.example.data.preferences.ScanQuality
import com.example.pdf.DocumentExportEngine
import com.example.pdf.ExportFormat
import com.example.pdf.NormalizedRect
import com.example.security.VaultCryptoManager
import com.example.vision.OnDeviceOcrEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

class DocumentRepository(
    private val context: Context,
    private val db: AppDatabase,
    val preferences: AppPreferences,
    val crypto: VaultCryptoManager,
) {
    val foldersFlow: Flow<List<FolderEntity>> = db.folderDao().observeAll()
    val libraryDocumentsFlow: Flow<List<DocumentEntity>> = db.documentDao().observeLibraryDocuments()
    val vaultDocumentsFlow: Flow<List<DocumentEntity>> = db.documentDao().observeVaultDocuments()
    val allDocumentsFlow: Flow<List<DocumentEntity>> = db.documentDao().observeAllDocuments()
    val signaturesFlow: Flow<List<SignatureEntity>> = db.signatureDao().observeAll()
    val qrHistoryFlow: Flow<List<QrHistoryEntity>> = db.qrHistoryDao().observeAll()
    val vaultActivityFlow: Flow<List<VaultActivityEntity>> = db.vaultActivityDao().observeAll()

    val docsDir: File by lazy { File(context.filesDir, "documents").apply { mkdirs() } }
    val sigDir: File by lazy { File(context.filesDir, "signatures").apply { mkdirs() } }

    fun observeDocument(docId: String): Flow<DocumentEntity?> = db.documentDao().observeById(docId)
    fun observePages(docId: String): Flow<List<PageEntity>> = db.pageDao().observePagesForDocument(docId)

    suspend fun getPagesOnce(docId: String): List<PageEntity> = db.pageDao().getPagesOnce(docId)
    suspend fun getDocumentOnce(docId: String): DocumentEntity? = db.documentDao().getByIdOnce(docId)

    suspend fun createFolder(name: String, colorKey: String): FolderEntity = withContext(Dispatchers.IO) {
        val folder = FolderEntity(
            id = "folder_${UUID.randomUUID().toString().take(8)}",
            name = name.trim(),
            colorKey = colorKey,
        )
        db.folderDao().upsert(folder)
        folder
    }

    suspend fun saveScannedDocument(
        title: String,
        folderId: String,
        docType: String,
        category: String,
        tags: List<String>,
        pageBitmaps: List<Bitmap>,
        pageFilters: List<String>,
        lockToVault: Boolean,
        quality: ScanQuality,
    ): DocumentEntity = withContext(Dispatchers.IO) {
        val docId = "doc_${UUID.randomUUID().toString().take(10)}"
        val now = System.currentTimeMillis()
        val pages = mutableListOf<PageEntity>()
        val ocrBuilder = StringBuilder()
        var totalSize = 0L
        var thumbPath = ""

        pageBitmaps.forEachIndexed { idx, bmp ->
            val pageId = "page_${UUID.randomUUID().toString().take(8)}"
            val ocrText = OnDeviceOcrEngine.recognizeText(bmp)
            if (ocrText.isNotBlank()) {
                if (ocrBuilder.isNotEmpty()) ocrBuilder.append("\n\n")
                ocrBuilder.append(ocrText)
            }
            val tempPlain = saveBitmapToFile(bmp, File(docsDir, "${docId}_$idx.jpg"), quality.jpegQuality)
            totalSize += tempPlain.length()

            if (idx == 0 && !lockToVault) {
                thumbPath = tempPlain.absolutePath
            }

            val finalPath = if (lockToVault) {
                val enc = crypto.encryptFileToVault(tempPlain, "${docId}_$idx")
                tempPlain.delete() // Zero plaintext persisted when locked
                enc.absolutePath
            } else {
                tempPlain.absolutePath
            }

            pages.add(
                PageEntity(
                    id = pageId,
                    documentId = docId,
                    pageIndex = idx,
                    imagePath = finalPath,
                    isEncrypted = lockToVault,
                    filterMode = pageFilters.getOrElse(idx) { "Original" },
                    ocrText = ocrText,
                ),
            )
        }

        val doc = DocumentEntity(
            id = docId,
            title = title.trim().ifBlank { "Scan_${now % 10000}" },
            folderId = folderId,
            docType = docType,
            category = category,
            createdAt = now,
            updatedAt = now,
            pageCount = pages.size,
            totalSizeBytes = totalSize,
            isLockedInVault = lockToVault,
            ocrText = ocrBuilder.toString(),
            tagsCsv = tags.joinToString(","),
            thumbnailPath = thumbPath,
        )

        db.documentDao().upsert(doc)
        db.pageDao().upsertAll(pages)
        if (lockToVault) {
            logVaultActivity("Encrypted Scan Added", "${doc.title} (${pages.size}p AES-256-GCM)", true)
        }
        doc
    }

    suspend fun loadPageBitmap(page: PageEntity): Bitmap? = withContext(Dispatchers.IO) {
        val file = File(page.imagePath)
        if (!file.exists()) return@withContext null
        return@withContext if (page.isEncrypted) {
            val bytes = crypto.decryptVaultFileToBytes(file) ?: return@withContext null
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        } else {
            BitmapFactory.decodeFile(file.absolutePath)
        }
    }

    suspend fun lockDocumentToVault(docId: String) = withContext(Dispatchers.IO) {
        val doc = db.documentDao().getByIdOnce(docId) ?: return@withContext
        if (doc.isLockedInVault) return@withContext
        val pages = db.pageDao().getPagesOnce(docId)
        val updatedPages = pages.mapIndexed { idx, p ->
            val src = File(p.imagePath)
            if (src.exists() && !p.isEncrypted) {
                val enc = crypto.encryptFileToVault(src, "${docId}_$idx")
                src.delete()
                p.copy(imagePath = enc.absolutePath, isEncrypted = true)
            } else p
        }
        db.pageDao().upsertAll(updatedPages)
        db.documentDao().update(
            doc.copy(
                isLockedInVault = true,
                thumbnailPath = "",
                updatedAt = System.currentTimeMillis(),
            ),
        )
        logVaultActivity("Moved to Vault", "${doc.title} encrypted with AES-256-GCM", true)
    }

    suspend fun unlockDocumentFromVault(docId: String) = withContext(Dispatchers.IO) {
        val doc = db.documentDao().getByIdOnce(docId) ?: return@withContext
        if (!doc.isLockedInVault) return@withContext
        val pages = db.pageDao().getPagesOnce(docId)
        var firstThumb = ""
        val updatedPages = pages.mapIndexed { idx, p ->
            val enc = File(p.imagePath)
            if (enc.exists() && p.isEncrypted) {
                val bytes = crypto.decryptVaultFileToBytes(enc)
                if (bytes != null) {
                    val out = File(docsDir, "${docId}_$idx.jpg")
                    out.writeBytes(bytes)
                    enc.delete()
                    if (idx == 0) firstThumb = out.absolutePath
                    p.copy(imagePath = out.absolutePath, isEncrypted = false)
                } else p
            } else p
        }
        db.pageDao().upsertAll(updatedPages)
        db.documentDao().update(
            doc.copy(
                isLockedInVault = false,
                thumbnailPath = firstThumb,
                updatedAt = System.currentTimeMillis(),
            ),
        )
        logVaultActivity("Removed from Vault", "${doc.title} moved back to Library", true)
    }

    suspend fun renameDocument(docId: String, newTitle: String) = withContext(Dispatchers.IO) {
        val doc = db.documentDao().getByIdOnce(docId) ?: return@withContext
        db.documentDao().update(doc.copy(title = newTitle.trim(), updatedAt = System.currentTimeMillis()))
    }

    suspend fun updateDocumentTags(docId: String, tags: List<String>) = withContext(Dispatchers.IO) {
        val doc = db.documentDao().getByIdOnce(docId) ?: return@withContext
        db.documentDao().update(doc.copy(tagsCsv = tags.joinToString(","), updatedAt = System.currentTimeMillis()))
    }

    suspend fun moveDocumentsToFolder(docIds: List<String>, folderId: String) = withContext(Dispatchers.IO) {
        docIds.forEach { id ->
            db.documentDao().getByIdOnce(id)?.let { doc ->
                db.documentDao().update(doc.copy(folderId = folderId, updatedAt = System.currentTimeMillis()))
            }
        }
    }

    suspend fun deleteDocuments(docIds: List<String>) = withContext(Dispatchers.IO) {
        docIds.forEach { id ->
            val pages = db.pageDao().getPagesOnce(id)
            pages.forEach { File(it.imagePath).delete() }
            db.pageDao().deleteByDocument(id)
            db.documentDao().deleteById(id)
        }
    }

    suspend fun addPageToDocument(docId: String, bitmap: Bitmap) = withContext(Dispatchers.IO) {
        val doc = db.documentDao().getByIdOnce(docId) ?: return@withContext
        val pages = db.pageDao().getPagesOnce(docId)
        val idx = pages.size
        val temp = saveBitmapToFile(bitmap, File(docsDir, "${docId}_${System.currentTimeMillis()}.jpg"), 90)
        val finalPath = if (doc.isLockedInVault) {
            val enc = crypto.encryptFileToVault(temp, "${docId}_${System.currentTimeMillis()}")
            temp.delete()
            enc.absolutePath
        } else temp.absolutePath

        val ocr = OnDeviceOcrEngine.recognizeText(bitmap)
        val newPage = PageEntity(
            id = "page_${UUID.randomUUID().toString().take(8)}",
            documentId = docId,
            pageIndex = idx,
            imagePath = finalPath,
            isEncrypted = doc.isLockedInVault,
            ocrText = ocr,
        )
        db.pageDao().upsert(newPage)
        val combinedOcr = listOf(doc.ocrText, ocr).filter { it.isNotBlank() }.joinToString("\n\n")
        db.documentDao().update(
            doc.copy(
                pageCount = pages.size + 1,
                totalSizeBytes = doc.totalSizeBytes + File(finalPath).length(),
                ocrText = combinedOcr,
                updatedAt = System.currentTimeMillis(),
            ),
        )
    }

    suspend fun deletePage(docId: String, pageId: String) = withContext(Dispatchers.IO) {
        val doc = db.documentDao().getByIdOnce(docId) ?: return@withContext
        val pages = db.pageDao().getPagesOnce(docId)
        if (pages.size <= 1) return@withContext
        val target = pages.firstOrNull { it.id == pageId } ?: return@withContext
        File(target.imagePath).delete()
        db.pageDao().deleteById(pageId)
        val remaining = pages.filter { it.id != pageId }.mapIndexed { i, p -> p.copy(pageIndex = i) }
        db.pageDao().upsertAll(remaining)
        db.documentDao().update(
            doc.copy(
                pageCount = remaining.size,
                thumbnailPath = if (doc.isLockedInVault) "" else remaining.first().imagePath,
                updatedAt = System.currentTimeMillis(),
            ),
        )
    }

    suspend fun swapPageOrder(docId: String, indexA: Int, indexB: Int) = withContext(Dispatchers.IO) {
        val pages = db.pageDao().getPagesOnce(docId).toMutableList()
        if (indexA !in pages.indices || indexB !in pages.indices) return@withContext
        val item = pages.removeAt(indexA)
        pages.add(indexB, item)
        val reindexed = pages.mapIndexed { idx, p -> p.copy(pageIndex = idx) }
        db.pageDao().upsertAll(reindexed)
        val doc = db.documentDao().getByIdOnce(docId) ?: return@withContext
        if (!doc.isLockedInVault && reindexed.isNotEmpty()) {
            db.documentDao().update(
                doc.copy(thumbnailPath = reindexed.first().imagePath, updatedAt = System.currentTimeMillis()),
            )
        }
    }

    suspend fun runOcrForDocument(docId: String): String = withContext(Dispatchers.IO) {
        val doc = db.documentDao().getByIdOnce(docId) ?: return@withContext ""
        val pages = db.pageDao().getPagesOnce(docId)
        val sb = StringBuilder()
        val updated = pages.map { page ->
            val bmp = loadPageBitmap(page)
            val txt = if (bmp != null) OnDeviceOcrEngine.recognizeText(bmp) else page.ocrText
            if (txt.isNotBlank()) {
                if (sb.isNotEmpty()) sb.append("\n\n")
                sb.append(txt)
            }
            page.copy(ocrText = txt)
        }
        db.pageDao().upsertAll(updated)
        val finalTxt = sb.toString()
        db.documentDao().update(doc.copy(ocrText = finalTxt, updatedAt = System.currentTimeMillis()))
        finalTxt
    }

    suspend fun saveSignature(name: String, bitmap: Bitmap): SignatureEntity = withContext(Dispatchers.IO) {
        val id = "sig_${UUID.randomUUID().toString().take(8)}"
        val file = File(sigDir, "$id.png")
        FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        val sig = SignatureEntity(id = id, name = name.ifBlank { "Signature" }, imagePath = file.absolutePath)
        db.signatureDao().upsert(sig)
        sig
    }

    suspend fun deleteSignature(sig: SignatureEntity) = withContext(Dispatchers.IO) {
        File(sig.imagePath).delete()
        db.signatureDao().deleteById(sig.id)
    }

    suspend fun applySignatureToDocument(
        docId: String,
        pageIndex: Int,
        signatureBitmap: Bitmap,
        centerXNorm: Float,
        centerYNorm: Float,
        widthNorm: Float,
    ): File? = withContext(Dispatchers.IO) {
        val doc = db.documentDao().getByIdOnce(docId) ?: return@withContext null
        val pages = db.pageDao().getPagesOnce(docId)
        val bitmaps = pages.mapIndexedNotNull { idx, page ->
            val bmp = loadPageBitmap(page) ?: return@mapIndexedNotNull null
            if (idx == pageIndex) {
                val flattened = DocumentExportEngine.flattenSignatureOntoBitmap(
                    pageBitmap = bmp,
                    signatureBitmap = signatureBitmap,
                    centerXNorm = centerXNorm,
                    centerYNorm = centerYNorm,
                    widthNorm = widthNorm,
                )
                // Also persist flattened page back onto document
                updatePageBitmap(page, flattened)
                flattened
            } else bmp
        }
        if (bitmaps.isEmpty()) return@withContext null
        DocumentExportEngine.buildPdfFromBitmaps(context, bitmaps, "${doc.title}_signed")
    }

    suspend fun applyPermanentRedaction(
        docId: String,
        pageIndex: Int,
        boxes: List<NormalizedRect>,
        saveAsNewCopy: Boolean = true,
    ): Pair<DocumentEntity?, File?> = withContext(Dispatchers.IO) {
        val doc = db.documentDao().getByIdOnce(docId) ?: return@withContext null to null
        val pages = db.pageDao().getPagesOnce(docId)
        val bitmaps = pages.mapIndexedNotNull { idx, page ->
            val bmp = loadPageBitmap(page) ?: return@mapIndexedNotNull null
            if (idx == pageIndex) DocumentExportEngine.burnRedactionsOntoBitmap(bmp, boxes) else bmp
        }
        if (bitmaps.isEmpty()) return@withContext null to null
        val pdfFile = DocumentExportEngine.buildPdfFromBitmaps(context, bitmaps, "${doc.title}_redacted")
        val savedDoc = if (saveAsNewCopy) {
            saveScannedDocument(
                title = "${doc.title} (Redacted)",
                folderId = doc.folderId,
                docType = "PDF",
                category = doc.category,
                tags = listOf("Redacted", "Sanitized"),
                pageBitmaps = bitmaps,
                pageFilters = bitmaps.map { "Original" },
                lockToVault = doc.isLockedInVault,
                quality = ScanQuality.HIGH,
            )
        } else {
            val targetPage = pages.getOrNull(pageIndex)
            if (targetPage != null) updatePageBitmap(targetPage, bitmaps[pageIndex])
            doc
        }
        savedDoc to pdfFile
    }

    private suspend fun updatePageBitmap(page: PageEntity, newBitmap: Bitmap) {
        if (page.isEncrypted) {
            val tmp = saveBitmapToFile(newBitmap, File(context.cacheDir, "tmp_enc_${page.id}.jpg"), 92)
            val enc = crypto.encryptFileToVault(tmp, page.id)
            tmp.delete()
            db.pageDao().upsert(page.copy(imagePath = enc.absolutePath))
        } else {
            saveBitmapToFile(newBitmap, File(page.imagePath), 92)
        }
    }

    suspend fun mergeDocuments(docIds: List<String>, newTitle: String): DocumentEntity? = withContext(Dispatchers.IO) {
        val docs = docIds.mapNotNull { db.documentDao().getByIdOnce(it) }
        if (docs.size < 2) return@withContext null
        val bitmaps = mutableListOf<Bitmap>()
        docs.forEach { d ->
            db.pageDao().getPagesOnce(d.id).forEach { page ->
                loadPageBitmap(page)?.let { bitmaps.add(it) }
            }
        }
        if (bitmaps.isEmpty()) return@withContext null
        saveScannedDocument(
            title = newTitle.ifBlank { "Merged_${docs.first().title}" },
            folderId = docs.first().folderId,
            docType = "PDF",
            category = "Document",
            tags = listOf("Merged", "PDF"),
            pageBitmaps = bitmaps,
            pageFilters = bitmaps.map { "Original" },
            lockToVault = false,
            quality = ScanQuality.HIGH,
        )
    }

    suspend fun splitDocument(docId: String, splitAfterPageIndex: Int): Pair<DocumentEntity?, DocumentEntity?> =
        withContext(Dispatchers.IO) {
            val doc = db.documentDao().getByIdOnce(docId) ?: return@withContext null to null
            val pages = db.pageDao().getPagesOnce(docId)
            if (pages.size < 2) return@withContext null to null
            val splitIdx = splitAfterPageIndex.coerceIn(0, pages.size - 2)
            val partA = pages.subList(0, splitIdx + 1).mapNotNull { loadPageBitmap(it) }
            val partB = pages.subList(splitIdx + 1, pages.size).mapNotNull { loadPageBitmap(it) }
            val docA = saveScannedDocument(
                title = "${doc.title} (Part 1)",
                folderId = doc.folderId,
                docType = "PDF",
                category = doc.category,
                tags = listOf("Split"),
                pageBitmaps = partA,
                pageFilters = partA.map { "Original" },
                lockToVault = false,
                quality = ScanQuality.HIGH,
            )
            val docB = saveScannedDocument(
                title = "${doc.title} (Part 2)",
                folderId = doc.folderId,
                docType = "PDF",
                category = doc.category,
                tags = listOf("Split"),
                pageBitmaps = partB,
                pageFilters = partB.map { "Original" },
                lockToVault = false,
                quality = ScanQuality.HIGH,
            )
            docA to docB
        }

    suspend fun importPdfUri(uri: Uri, title: String): DocumentEntity? = withContext(Dispatchers.IO) {
        val bitmaps = DocumentExportEngine.renderPdfUriToBitmaps(context, uri)
        if (bitmaps.isEmpty()) return@withContext null
        saveScannedDocument(
            title = title,
            folderId = "folder_work",
            docType = "PDF",
            category = "Document",
            tags = listOf("Imported PDF"),
            pageBitmaps = bitmaps,
            pageFilters = bitmaps.map { "Original" },
            lockToVault = false,
            quality = ScanQuality.HIGH,
        )
    }

    suspend fun exportDocument(docId: String, format: ExportFormat, jpegQuality: Int = 90): File? =
        withContext(Dispatchers.IO) {
            val doc = db.documentDao().getByIdOnce(docId) ?: return@withContext null
            val pages = db.pageDao().getPagesOnce(docId)
            val bitmaps = pages.mapNotNull { loadPageBitmap(it) }
            when (format) {
                ExportFormat.PDF -> {
                    if (bitmaps.isEmpty()) return@withContext null
                    DocumentExportEngine.buildPdfFromBitmaps(context, bitmaps, doc.title, jpegQuality)
                }
                ExportFormat.JPG -> {
                    val first = bitmaps.firstOrNull() ?: return@withContext null
                    DocumentExportEngine.exportJpg(context, first, doc.title, jpegQuality)
                }
                ExportFormat.TXT -> DocumentExportEngine.exportTxt(context, doc.title, doc.ocrText, doc.title)
                ExportFormat.DOCX -> DocumentExportEngine.exportDocx(context, doc.title, doc.ocrText, doc.title)
                ExportFormat.XLSX -> DocumentExportEngine.exportXlsx(context, doc.title, doc.ocrText, doc.title)
            }
        }

    suspend fun saveQrRecord(rawContent: String, qrType: String, summary: String, isGenerated: Boolean) =
        withContext(Dispatchers.IO) {
            db.qrHistoryDao().upsert(
                QrHistoryEntity(
                    id = "qr_${UUID.randomUUID().toString().take(8)}",
                    rawContent = rawContent,
                    qrType = qrType,
                    summary = summary,
                    isGenerated = isGenerated,
                ),
            )
        }

    suspend fun deleteQrRecord(id: String) = withContext(Dispatchers.IO) {
        db.qrHistoryDao().deleteById(id)
    }

    suspend fun logVaultActivity(action: String, detail: String, isSuccess: Boolean) =
        withContext(Dispatchers.IO) {
            db.vaultActivityDao().insert(
                VaultActivityEntity(
                    id = "act_${UUID.randomUUID().toString().take(8)}",
                    action = action,
                    detail = detail,
                    isSuccess = isSuccess,
                ),
            )
        }

    suspend fun computeStorageBreakdownBytes(): Triple<Long, Long, Long> = withContext(Dispatchers.IO) {
        val libBytes = docsDir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
        val vaultBytes = crypto.vaultDir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
        val cacheBytes = context.cacheDir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
        Triple(libBytes, vaultBytes, cacheBytes)
    }

    suspend fun clearCache() = withContext(Dispatchers.IO) {
        File(context.cacheDir, "exports").deleteRecursively()
    }

    suspend fun clearAllLocalData() = withContext(Dispatchers.IO) {
        docsDir.deleteRecursively()
        sigDir.deleteRecursively()
        crypto.vaultDir.deleteRecursively()
        File(context.cacheDir, "exports").deleteRecursively()
        docsDir.mkdirs()
        sigDir.mkdirs()
        crypto.vaultDir.mkdirs()
        db.pageDao().deleteAll()
        db.documentDao().deleteAll()
        db.signatureDao().deleteAll()
        db.qrHistoryDao().deleteAll()
        db.vaultActivityDao().deleteAll()
    }

    private fun saveBitmapToFile(bitmap: Bitmap, dest: File, quality: Int): File {
        FileOutputStream(dest).use { bitmap.compress(Bitmap.CompressFormat.JPEG, quality, it) }
        return dest
    }
}
