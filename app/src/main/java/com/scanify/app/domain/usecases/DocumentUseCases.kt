package com.scanify.app.domain.usecases

import com.scanify.app.domain.usecases.deletedocumentusecases.DeleteDocumentUseCase
import com.scanify.app.domain.usecases.getdocumentusecases.GetDocumentByIdUseCase
import com.scanify.app.domain.usecases.getdocumentusecases.GetDocumentsUseCase
import com.scanify.app.domain.usecases.importdocumentusecase.AppendImagesToDocumentUseCase
import com.scanify.app.domain.usecases.importdocumentusecase.ImportDocumentUseCase
import com.scanify.app.domain.usecases.importdocumentusecase.ImportMultipleFilesUseCase
import com.scanify.app.domain.usecases.importdocumentusecase.ImportMultipleImagesUseCase
import com.scanify.app.domain.usecases.renamedocumentusecases.RenameDocumentUseCase
import jakarta.inject.Inject

data class DocumentUseCases @Inject constructor(
    val getDocuments: GetDocumentsUseCase,
    val getDocumentById: GetDocumentByIdUseCase,
    val importDocument: ImportDocumentUseCase,
    val deleteDocument: DeleteDocumentUseCase,
    val importMultipleFiles: ImportMultipleFilesUseCase,
    val importMultipleImages: ImportMultipleImagesUseCase,
    val appendImagesToDocument: AppendImagesToDocumentUseCase,
    val renameDocumentUseCase: RenameDocumentUseCase
)