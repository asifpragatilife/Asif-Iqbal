package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "folders")
data class FolderEntity(
    @PrimaryKey val id: String,
    val name: String,
    val colorKey: String, // "blue", "green", "orange", "purple"
    val createdAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "documents")
data class DocumentEntity(
    @PrimaryKey val id: String,
    val title: String,
    val folderId: String,
    val docType: String, // "PDF", "DOCX", "XLSX", "JPG"
    val category: String, // "ID", "Receipt", "Business Card", "Document"
    val createdAt: Long,
    val updatedAt: Long,
    val pageCount: Int,
    val totalSizeBytes: Long,
    val isLockedInVault: Boolean = false,
    val ocrText: String = "",
    val tagsCsv: String = "",
    val thumbnailPath: String = "",
)

@Entity(tableName = "pages")
data class PageEntity(
    @PrimaryKey val id: String,
    val documentId: String,
    val pageIndex: Int,
    val imagePath: String, // Plaintext path when unlocked/library, or .enc path when locked in vault
    val isEncrypted: Boolean = false,
    val filterMode: String = "Original", // Original, Auto, B&W, Grayscale, Magic Color
    val brightness: Float = 0f,
    val contrast: Float = 1f,
    val rotationDegrees: Int = 0,
    val ocrText: String = "",
)

@Entity(tableName = "signatures")
data class SignatureEntity(
    @PrimaryKey val id: String,
    val name: String,
    val imagePath: String,
    val createdAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "qr_history")
data class QrHistoryEntity(
    @PrimaryKey val id: String,
    val rawContent: String,
    val qrType: String, // URL, WIFI, CONTACT, EMAIL, TEXT
    val summary: String,
    val isGenerated: Boolean,
    val createdAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "vault_activity")
data class VaultActivityEntity(
    @PrimaryKey val id: String,
    val action: String,
    val detail: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isSuccess: Boolean = true,
)
