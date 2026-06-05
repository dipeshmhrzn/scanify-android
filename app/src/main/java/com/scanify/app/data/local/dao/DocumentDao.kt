package com.scanify.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.scanify.app.data.local.entity.DocumentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DocumentDao {

    @Query("SELECT * FROM documents ORDER BY createdAtTimeStamp DESC")
    fun getAllDocuments(): Flow<List<DocumentEntity>>

    @Query("UPDATE documents SET name = :newName WHERE id = :id")
    suspend fun updateDocumentName(id: Long, newName: String)

    @Query("SELECT * FROM documents WHERE id = :id LIMIT 1")
    suspend fun getDocumentById(id: Long): DocumentEntity?

    @Query("SELECT * FROM documents WHERE name LIKE '%' || :searchQuery || '%' ORDER BY createdAtTimeStamp DESC")
    fun searchDocuments(searchQuery: String): Flow<List<DocumentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDocuments(document: DocumentEntity): Long

    @Delete
    suspend fun deleteDocuments(document: DocumentEntity)
}