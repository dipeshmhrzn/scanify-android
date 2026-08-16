package com.scanify.app.presentation.util

import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.scanify.app.domain.model.Document
import com.scanify.app.presentation.components.filecomponents.SaveFormatChoiceDialog
import com.scanify.app.presentation.viewmodels.FileViewModel

@Composable
fun rememberSaveDocumentHandler(fileViewModel: FileViewModel): (Document) -> Unit {

    var docPendingFormatChoice by remember { mutableStateOf<Document?>(null) }

    var docPendingPdfUri by remember { mutableStateOf<Document?>(null) }
    var docPendingImageFolder by remember { mutableStateOf<Document?>(null) }

    val exportPdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("*/*")
    ) { uri ->
        uri?.let { safeUri ->
            docPendingPdfUri?.let { doc -> fileViewModel.saveToSelectedUri(doc, safeUri) }
        }
        docPendingPdfUri = null
    }

    val pickFolderForImagesLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { treeUri ->
        treeUri?.let { safeUri ->
            docPendingImageFolder?.let { doc -> fileViewModel.saveImagesToDeviceLegacy(doc, safeUri) }
        }
        docPendingImageFolder = null
    }

    fun savePdf(document: Document) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            fileViewModel.autoSaveToDocuments(document)
        } else {
            docPendingPdfUri = document
            val fileNameWithExtension = "${document.name}.${document.fileType.lowercase()}"
            exportPdfLauncher.launch(fileNameWithExtension)
        }
    }

    fun saveImages(document: Document) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            fileViewModel.saveImagesToDevice(document)
        } else {
            docPendingImageFolder = document
            pickFolderForImagesLauncher.launch(null)
        }
    }

    docPendingFormatChoice?.let { document ->
        SaveFormatChoiceDialog(
            onDismiss = { docPendingFormatChoice = null },
            onChoosePdf = {
                docPendingFormatChoice = null
                savePdf(document)
            },
            onChooseImages = {
                docPendingFormatChoice = null
                saveImages(document)
            }
        )
    }

    return remember(fileViewModel) {
        { document: Document ->
            if (document.isImageBundle) {
                docPendingFormatChoice = document
            } else {
                savePdf(document)
            }
        }
    }
}