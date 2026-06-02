package com.scanify.app.domain.usecases.deletedocumentusecases

import com.scanify.app.domain.model.Document
import com.scanify.app.domain.repository.DocumentRepository
import java.io.File
import javax.inject.Inject

class DeleteDocumentUseCase @Inject constructor(private val repository: DocumentRepository) {
    suspend operator fun invoke(document: Document): Result<Unit> = try {
        val file = File(document.filePath)
        if (file.exists()) file.delete()
        repository.deleteDocument(document)
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

}