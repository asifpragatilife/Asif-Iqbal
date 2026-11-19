package com.example.ui.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.AsifArafApplication
import com.example.data.local.DocumentEntity
import com.example.data.local.FolderEntity
import com.example.data.local.PageEntity
import com.example.data.local.QrHistoryEntity
import com.example.data.local.SignatureEntity
import com.example.data.local.VaultActivityEntity
import com.example.data.preferences.AppLanguage
import com.example.data.preferences.ScanQuality
import com.example.data.preferences.UserPreferencesState
import com.example.pdf.DocumentExportEngine
import com.example.pdf.ExportFormat
import com.example.pdf.NormalizedRect
import com.example.vision.OnDeviceOcrEngine
import com.example.vision.SmartClassification
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val container = (application as AsifArafApplication).container
    val repo = container.repository
    val prefs = container.preferences
    val crypto = container.cryptoManager

    val preferencesState: StateFlow<UserPreferencesState> = prefs.preferencesFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, UserPreferencesState())

    val folders: StateFlow<List<FolderEntity>> = repo.foldersFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val libraryDocuments: StateFlow<List<DocumentEntity>> = repo.libraryDocumentsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val vaultDocuments: StateFlow<List<DocumentEntity>> = repo.vaultDocumentsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allDocuments: StateFlow<List<DocumentEntity>> = repo.allDocumentsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val signatures: StateFlow<List<SignatureEntity>> = repo.signaturesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val qrHistory: StateFlow<List<QrHistoryEntity>> = repo.qrHistoryFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val vaultActivities: StateFlow<List<VaultActivityEntity>> = repo.vaultActivityFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _vaultUnlocked = MutableStateFlow(false)
    val vaultUnlocked: StateFlow<Boolean> = _vaultUnlocked.asStateFlow()

    private val _vaultError = MutableStateFlow<String?>(null)
    val vaultError: StateFlow<String?> = _vaultError.asStateFlow()

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    private var autoLockJob: Job? = null

    fun observeDocument(docId: String): Flow<DocumentEntity?> = repo.observeDocument(docId)
    fun observePages(docId: String): Flow<List<PageEntity>> = repo.observePages(docId)

    fun showMessage(msg: String) {
        _toastMessage.value = msg
    }

    fun clearMessage() {
        _toastMessage.value = null
    }

    fun completeOnboarding() {
        viewModelScope.launch { prefs.setOnboardingCompleted(true) }
    }

    fun setDarkTheme(dark: Boolean) {
        viewModelScope.launch { prefs.setDarkTheme(dark) }
    }

    fun setLanguage(lang: AppLanguage) {
        viewModelScope.launch { prefs.setLanguage(lang) }
    }

    fun setScanQuality(quality: ScanQuality) {
        viewModelScope.launch { prefs.setScanQuality(quality) }
    }

    fun setAutoLockSeconds(seconds: Int) {
        viewModelScope.launch {
            prefs.setAutoLockSeconds(seconds)
            if (_vaultUnlocked.value) scheduleAutoLock(seconds)
        }
    }

    fun setBiometricEnabled(enabled: Boolean) {
        viewModelScope.launch { prefs.setBiometricEnabled(enabled) }
    }

    fun unlockVaultViaBiometric() {
        viewModelScope.launch {
            prefs.resetPinFailures()
            _vaultError.value = null
            _vaultUnlocked.value = true
            repo.logVaultActivity("Vault Unlocked", "Hardware Biometric authentication succeeded", true)
            scheduleAutoLock(preferencesState.value.autoLockSeconds)
        }
    }

    fun attemptPinUnlock(pin: String): Boolean {
        val state = preferencesState.value
        val now = System.currentTimeMillis()
        if (state.lockoutUntilMs > now) {
            val sec = ((state.lockoutUntilMs - now) / 1000L).coerceAtLeast(1)
            _vaultError.value = "Locked out for ${sec}s after failed attempts"
            return false
        }
        val ok = crypto.verifyPin(pin, state.pinHash, state.pinSalt)
        viewModelScope.launch {
            if (ok) {
                prefs.resetPinFailures()
                _vaultError.value = null
                _vaultUnlocked.value = true
                repo.logVaultActivity("Vault Unlocked", "PIN verified via PBKDF2-SHA256", true)
                scheduleAutoLock(state.autoLockSeconds)
            } else {
                val nextAttempts = state.failedPinAttempts + 1
                val lockout = if (nextAttempts >= 5) now + 30_000L else 0L
                prefs.recordFailedPin(nextAttempts, lockout)
                _vaultError.value = if (lockout > 0) {
                    "Too many attempts. Vault locked for 30s."
                } else {
                    "Incorrect PIN (${5 - nextAttempts} tries left)"
                }
                repo.logVaultActivity("Failed Unlock Attempt", "Invalid PIN (#$nextAttempts)", false)
            }
        }
        return ok
    }

    fun lockVault(reason: String = "Manual Lock") {
        autoLockJob?.cancel()
        _vaultUnlocked.value = false
        viewModelScope.launch {
            repo.logVaultActivity("Vault Locked", reason, true)
        }
    }

    fun changeVaultPin(newPin: String) {
        if (newPin.length < 4) return
        viewModelScope.launch {
            val salt = crypto.generateSaltBase64()
            val hash = crypto.hashPinBase64(newPin, salt)
            prefs.savePinCredentials(hash, salt)
            repo.logVaultActivity("PIN Updated", "PBKDF2-SHA256 credentials rotated", true)
            showMessage("Vault PIN updated")
        }
    }

    private fun scheduleAutoLock(seconds: Int) {
        autoLockJob?.cancel()
        if (seconds <= 0) return
        autoLockJob = viewModelScope.launch {
            delay(seconds * 1000L)
            if (_vaultUnlocked.value) {
                lockVault("Auto-locked after ${seconds}s inactivity")
            }
        }
    }

    fun createFolder(name: String, colorKey: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            repo.createFolder(name, colorKey)
            showMessage("Folder created")
        }
    }

    fun lockDocumentsToVault(ids: List<String>) {
        viewModelScope.launch {
            ids.forEach { repo.lockDocumentToVault(it) }
            showMessage("${ids.size} item(s) encrypted in Vault")
        }
    }

    fun unlockDocumentFromVault(id: String) {
        viewModelScope.launch {
            repo.unlockDocumentFromVault(id)
            showMessage("Moved back to Library")
        }
    }

    fun moveDocumentsToFolder(ids: List<String>, folderId: String) {
        viewModelScope.launch {
            repo.moveDocumentsToFolder(ids, folderId)
            showMessage("Moved ${ids.size} document(s)")
        }
    }

    fun deleteDocuments(ids: List<String>) {
        viewModelScope.launch {
            repo.deleteDocuments(ids)
            showMessage("Deleted ${ids.size} document(s)")
        }
    }

    fun renameDocument(docId: String, newTitle: String) {
        if (newTitle.isBlank()) return
        viewModelScope.launch {
            repo.renameDocument(docId, newTitle)
            showMessage("Renamed")
        }
    }

    fun updateTags(docId: String, tags: List<String>) {
        viewModelScope.launch {
            repo.updateDocumentTags(docId, tags)
        }
    }

    fun runOcr(docId: String) {
        viewModelScope.launch {
            showMessage("Running on-device OCR...")
            val text = repo.runOcrForDocument(docId)
            showMessage(if (text.isNotBlank()) "OCR text extracted offline" else "No text detected")
        }
    }

    fun classifyAndSaveScan(
        title: String,
        folderId: String,
        bitmaps: List<Bitmap>,
        filters: List<String>,
        lockToVault: Boolean,
        onSaved: (DocumentEntity) -> Unit,
    ) {
        viewModelScope.launch {
            val firstText = bitmaps.firstOrNull()?.let { OnDeviceOcrEngine.recognizeText(it) } ?: ""
            val cls: SmartClassification = OnDeviceOcrEngine.classify(firstText)
            val saved = repo.saveScannedDocument(
                title = title.ifBlank { cls.suggestedTitle },
                folderId = folderId.ifBlank { cls.suggestedFolderId },
                docType = "PDF",
                category = cls.category,
                tags = cls.tags,
                pageBitmaps = bitmaps,
                pageFilters = filters,
                lockToVault = lockToVault,
                quality = preferencesState.value.defaultScanQuality,
            )
            onSaved(saved)
        }
    }

    fun exportAndShare(docId: String, format: ExportFormat, quality: Int = 90) {
        viewModelScope.launch {
            val doc = repo.getDocumentOnce(docId) ?: return@launch
            val file: File? = repo.exportDocument(docId, format, quality)
            if (file != null) {
                DocumentExportEngine.shareFile(getApplication(), file, format.mimeType, doc.title)
            } else {
                showMessage("Unable to export ${format.label}")
            }
        }
    }

    fun importPdf(uri: Uri, title: String, onDone: (DocumentEntity?) -> Unit) {
        viewModelScope.launch {
            val doc = repo.importPdfUri(uri, title)
            showMessage(if (doc != null) "Imported ${doc.pageCount} page(s)" else "Failed to read PDF")
            onDone(doc)
        }
    }

    fun mergeDocuments(docIds: List<String>, title: String, onDone: (DocumentEntity?) -> Unit) {
        viewModelScope.launch {
            val merged = repo.mergeDocuments(docIds, title)
            showMessage(if (merged != null) "Merged into ${merged.title}" else "Select 2+ documents")
            onDone(merged)
        }
    }

    fun splitDocument(docId: String, splitAfterIndex: Int, onDone: (Boolean) -> Unit) {
        viewModelScope.launch {
            val (a, b) = repo.splitDocument(docId, splitAfterIndex)
            val ok = a != null && b != null
            showMessage(if (ok) "Split into 2 PDFs" else "Needs 2+ pages to split")
            onDone(ok)
        }
    }

    fun applyPermanentRedaction(
        docId: String,
        pageIndex: Int,
        boxes: List<NormalizedRect>,
        onDone: (DocumentEntity?, File?) -> Unit,
    ) {
        viewModelScope.launch {
            val (doc, file) = repo.applyPermanentRedaction(docId, pageIndex, boxes, saveAsNewCopy = true)
            showMessage("Redactions burned permanently")
            onDone(doc, file)
        }
    }

    fun saveSignature(name: String, bitmap: Bitmap) {
        viewModelScope.launch {
            repo.saveSignature(name, bitmap)
            showMessage("Signature saved")
        }
    }

    fun deleteSignature(sig: SignatureEntity) {
        viewModelScope.launch { repo.deleteSignature(sig) }
    }

    fun flattenSignatureAndShare(
        docId: String,
        pageIndex: Int,
        signatureBitmap: Bitmap,
        centerXNorm: Float,
        centerYNorm: Float,
        widthNorm: Float,
    ) {
        viewModelScope.launch {
            val pdf = repo.applySignatureToDocument(
                docId, pageIndex, signatureBitmap, centerXNorm, centerYNorm, widthNorm,
            )
            if (pdf != null) {
                showMessage("Signature flattened into PDF")
                DocumentExportEngine.shareFile(getApplication(), pdf, ExportFormat.PDF.mimeType, pdf.nameWithoutExtension)
            }
        }
    }

    fun saveQr(raw: String, type: String, summary: String, isGenerated: Boolean) {
        viewModelScope.launch { repo.saveQrRecord(raw, type, summary, isGenerated) }
    }

    fun deleteQr(id: String) {
        viewModelScope.launch { repo.deleteQrRecord(id) }
    }

    fun setSecureCloudOptIn(enabled: Boolean) {
        viewModelScope.launch {
            prefs.setSecureCloudOptIn(enabled)
            repo.logVaultActivity(
                if (enabled) "Secure Cloud Opt-In Enabled" else "Local-Only Enforced",
                if (enabled) "User explicitly opted into cloud backup metadata" else "All sync disabled",
                true,
            )
        }
    }

    fun setGoogleDriveOptIn(enabled: Boolean) {
        viewModelScope.launch { prefs.setGoogleDriveOptIn(enabled) }
    }

    fun setDropboxOptIn(enabled: Boolean) {
        viewModelScope.launch { prefs.setDropboxOptIn(enabled) }
    }

    fun clearCache() {
        viewModelScope.launch {
            repo.clearCache()
            showMessage("Export cache cleared")
        }
    }

    fun clearAllData() {
        viewModelScope.launch {
            repo.clearAllLocalData()
            _vaultUnlocked.value = false
            showMessage("All local documents & keys wiped")
        }
    }
}
