package com.scanify.app.domain.usecases.searchdocumentusecase

import com.scanify.app.domain.model.Document
import com.scanify.app.domain.repository.DocumentRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class SearchDocumentsUseCase @Inject constructor(
    private val repository: DocumentRepository
) {
    operator fun invoke(query: String): Flow<List<Document>> {
        return repository.searchDocuments(query)
    }
}