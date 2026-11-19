package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface FolderDao {
    @Query("SELECT * FROM folders ORDER BY createdAt ASC")
    fun observeAll(): Flow<List<FolderEntity>>

    @Query("SELECT * FROM folders")
    suspend fun getAllOnce(): List<FolderEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(folder: FolderEntity)

    @Query("DELETE FROM folders WHERE id = :id")
    suspend fun deleteById(id: String)
}

@Dao
interface DocumentDao {
    @Query("SELECT * FROM documents WHERE isLockedInVault = 0 ORDER BY updatedAt DESC")
    fun observeLibraryDocuments(): Flow<List<DocumentEntity>>

    @Query("SELECT * FROM documents WHERE isLockedInVault = 1 ORDER BY updatedAt DESC")
    fun observeVaultDocuments(): Flow<List<DocumentEntity>>

    @Query("SELECT * FROM documents ORDER BY updatedAt DESC")
    fun observeAllDocuments(): Flow<List<DocumentEntity>>

    @Query("SELECT * FROM documents WHERE id = :id LIMIT 1")
    fun observeById(id: String): Flow<DocumentEntity?>

    @Query("SELECT * FROM documents WHERE id = :id LIMIT 1")
    suspend fun getByIdOnce(id: String): DocumentEntity?

    @Query("SELECT COUNT(*) FROM documents")
    suspend fun countAll(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(doc: DocumentEntity)

    @Update
    suspend fun update(doc: DocumentEntity)

    @Query("DELETE FROM documents WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM documents")
    suspend fun deleteAll()
}

@Dao
interface PageDao {
    @Query("SELECT * FROM pages WHERE documentId = :docId ORDER BY pageIndex ASC")
    fun observePagesForDocument(docId: String): Flow<List<PageEntity>>

    @Query("SELECT * FROM pages WHERE documentId = :docId ORDER BY pageIndex ASC")
    suspend fun getPagesOnce(docId: String): List<PageEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(page: PageEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(pages: List<PageEntity>)

    @Query("DELETE FROM pages WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM pages WHERE documentId = :docId")
    suspend fun deleteByDocument(docId: String)

    @Query("DELETE FROM pages")
    suspend fun deleteAll()
}

@Dao
interface SignatureDao {
    @Query("SELECT * FROM signatures ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<SignatureEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(sig: SignatureEntity)

    @Query("DELETE FROM signatures WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM signatures")
    suspend fun deleteAll()
}

@Dao
interface QrHistoryDao {
    @Query("SELECT * FROM qr_history ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<QrHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: QrHistoryEntity)

    @Query("DELETE FROM qr_history WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM qr_history")
    suspend fun deleteAll()
}

@Dao
interface VaultActivityDao {
    @Query("SELECT * FROM vault_activity ORDER BY timestamp DESC LIMIT 100")
    fun observeAll(): Flow<List<VaultActivityEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: VaultActivityEntity)

    @Query("DELETE FROM vault_activity")
    suspend fun deleteAll()
}
