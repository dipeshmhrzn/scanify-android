package com.scanify.app.domain.usecases.importdocumentusecase

import com.scanify.app.domain.model.Document
import com.scanify.app.domain.repository.DocumentRepository
import javax.inject.Inject

class SaveDocumentToDeviceUseCase @Inject constructor(
    private val repository: DocumentRepository
) {
    suspend operator fun invoke(document: Document): Result<Unit> {
        return repository.saveDocumentToExternalStorage(document)
    }
}