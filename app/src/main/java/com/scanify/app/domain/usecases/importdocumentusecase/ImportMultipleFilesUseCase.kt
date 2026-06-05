package com.scanify.app.domain.usecases.importdocumentusecase

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class ImportMultipleFilesUseCase @Inject constructor(
    private val importDocumentUseCase: ImportDocumentUseCase
) {
    suspend operator fun invoke(uriStrings: List<String>): Result<List<Long>> =
        withContext(Dispatchers.IO) {
            try {
                val successfulIds: MutableList<Long> = mutableListOf()
                for (uri: String in uriStrings) {
                    importDocumentUseCase(uri)
                        .onSuccess { id: Long ->
                            successfulIds.add(id)
                        }
                        .onFailure { exception: Throwable ->
                            throw exception
                        }
                }
                Result.success(successfulIds)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
}