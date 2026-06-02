package com.scanify.app.domain.usecases.getdocumentusecases

import com.scanify.app.domain.model.Document
import com.scanify.app.domain.repository.DocumentRepository
import javax.inject.Inject

class GetDocumentByIdUseCase @Inject constructor(
    private val repository: DocumentRepository
) {
    suspend operator fun invoke(id: Long): Document? = repository.getDocumentById(id)
}