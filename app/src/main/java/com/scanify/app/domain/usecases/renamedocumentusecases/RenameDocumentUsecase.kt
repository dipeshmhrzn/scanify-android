package com.scanify.app.domain.usecases.renamedocumentusecases

import com.scanify.app.domain.model.Document
import com.scanify.app.domain.repository.DocumentRepository
import javax.inject.Inject

class RenameDocumentUseCase @Inject constructor(
    private val repository: DocumentRepository
) {
    suspend operator fun invoke(document: Document, newName: String): Result<Unit> =
        repository.renameDocument(document, newName)
}