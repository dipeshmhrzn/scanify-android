package com.scanify.app.domain.repository

import com.scanify.app.domain.model.Document
import kotlinx.coroutines.flow.Flow

interface DocumentRepository {

    fun getAllDocuments(): Flow<List<Document>>

    suspend fun getDocumentById(id: Long): Document?

    suspend fun importDocument(document: Document): Long

    suspend fun deleteDocument(document: Document)

    suspend fun renameDocument(document: Document, newName: String): Result<Unit>
}