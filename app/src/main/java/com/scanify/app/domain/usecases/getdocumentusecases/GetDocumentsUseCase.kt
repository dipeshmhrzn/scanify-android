package com.scanify.app.domain.usecases.getdocumentusecases

import com.scanify.app.domain.model.Document
import com.scanify.app.domain.repository.DocumentRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetDocumentsUseCase @Inject constructor(
    private val repository: DocumentRepository
) {
    operator fun invoke(): Flow<List<Document>> = repository.getAllDocuments()
}