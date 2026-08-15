package com.scanify.app.domain.usecases.importdocumentusecase

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import javax.inject.Inject

class ImportMultipleFilesUseCase @Inject constructor(
    private val importDocumentUseCase: ImportDocumentUseCase
) {
    data class ImportSummary(val successfulIds: List<Long>, val failures: List<Throwable>)

    suspend operator fun invoke(uriStrings: List<String>): Result<ImportSummary> =
        withContext(Dispatchers.IO) {
            try {
                val outcomes = coroutineScope {
                    uriStrings.map { uri -> async { importDocumentUseCase(uri) } }.awaitAll()
                }

                val successfulIds = outcomes.mapNotNull { it.getOrNull() }
                val failures = outcomes.mapNotNull { it.exceptionOrNull() }

                Result.success(ImportSummary(successfulIds, failures))
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
}