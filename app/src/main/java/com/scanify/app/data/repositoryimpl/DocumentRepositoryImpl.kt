package com.scanify.app.data.repositoryimpl

import android.content.Context
import com.scanify.app.data.local.dao.DocumentDao
import com.scanify.app.data.mapper.toDomain
import com.scanify.app.data.mapper.toEntity
import com.scanify.app.domain.model.Document
import com.scanify.app.domain.repository.DocumentRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject

class DocumentRepositoryImpl @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val dao: DocumentDao
) : DocumentRepository {

    override fun getAllDocuments(): Flow<List<Document>> = dao.getAllDocuments().map { list ->
        list.map { it.toDomain() }
    }

    override suspend fun getDocumentById(id: Long): Document? = dao.getDocumentById(id)?.toDomain()

    override suspend fun importDocument(document: Document): Long =
        dao.insertDocuments(document.toEntity())

    override suspend fun deleteDocument(document: Document) =
        dao.deleteDocuments(document.toEntity())

    override suspend fun renameDocument(document: Document, newName: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                dao.updateDocumentName(document.id, newName)
            }
        }
}