package com.example.core.editor

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import com.example.data.SessionDraft
import com.example.data.SettingsRepository
import com.example.data.StarredFolder
import com.example.logic.DocumentFileHandler
import com.example.logic.SupportedFileType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

/**
 * Handles all document file input/output, Storage Access Framework (SAF) URI permissions,
 * file creation/overwrites (txt, md, docx), and session draft persistence.
 */
class DocumentFileManager(
    private val application: Application,
    private val settingsRepo: SettingsRepository
) {
    suspend fun readDocumentFromUri(uri: Uri): Pair<String, String> = withContext(Dispatchers.IO) {
        val resolver = application.contentResolver
        try {
            resolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
        } catch (_: Exception) {}

        val resolvedName = getFileName(uri) ?: "document.txt"
        val fileType = SupportedFileType.fromFileName(resolvedName)

        val text = resolver.openInputStream(uri)?.use { stream ->
            DocumentFileHandler.readDocument(stream, fileType)
        } ?: ""

        Pair(resolvedName, text)
    }

    suspend fun writeDocumentToUri(uri: Uri, fileName: String, text: String): String = withContext(Dispatchers.IO) {
        val resolver = application.contentResolver
        try {
            resolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
        } catch (_: Exception) {}

        val resolvedName = getFileName(uri) ?: fileName
        val fileType = SupportedFileType.fromFileName(resolvedName)

        val existingBytes = if (fileType == SupportedFileType.DOCX) {
            try {
                resolver.openInputStream(uri)?.use { it.readBytes() }
            } catch (_: Exception) {
                null
            }
        } else null

        val outputStream = resolver.openOutputStream(uri, "rwt") ?: resolver.openOutputStream(uri)
            ?: throw IllegalStateException("Cannot open output stream for URI: $uri")

        outputStream.use { out ->
            val existingStream = existingBytes?.let { ByteArrayInputStream(it) }
            DocumentFileHandler.writeDocument(
                text = text,
                outputStream = out,
                fileType = fileType,
                existingInputStream = existingStream
            )
        }
        resolvedName
    }

    suspend fun writeDocumentToFile(file: File, text: String): String = withContext(Dispatchers.IO) {
        if (file.parentFile != null && !file.parentFile!!.exists()) {
            file.parentFile!!.mkdirs()
        }
        val fileType = SupportedFileType.fromFileName(file.name)
        val existingBytes = if (file.exists() && fileType == SupportedFileType.DOCX) {
            try { file.readBytes() } catch (_: Exception) { null }
        } else null

        FileOutputStream(file).use { out ->
            val existingStream = existingBytes?.let { ByteArrayInputStream(it) }
            DocumentFileHandler.writeDocument(
                text = text,
                outputStream = out,
                fileType = fileType,
                existingInputStream = existingStream
            )
        }
        file.name
    }

    suspend fun writeDocumentToStarredFolder(
        folder: StarredFolder,
        customFileName: String,
        defaultFileName: String,
        text: String
    ): Pair<Uri, String> = withContext(Dispatchers.IO) {
        val rawName = if (customFileName.isNotBlank()) customFileName.trim() else defaultFileName
        val finalFileName = if (rawName.contains('.')) rawName else "$rawName.txt"
        val fileType = SupportedFileType.fromFileName(finalFileName)

        if (folder.uriString.startsWith("content://")) {
            val treeUri = Uri.parse(folder.uriString)
            try {
                application.contentResolver.takePersistableUriPermission(
                    treeUri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
            } catch (_: Exception) {}

            val docUri = DocumentsContract.buildDocumentUriUsingTree(
                treeUri,
                DocumentsContract.getTreeDocumentId(treeUri)
            )
            val newFileUri = DocumentsContract.createDocument(
                application.contentResolver,
                docUri,
                fileType.mimeType,
                finalFileName
            ) ?: throw IllegalStateException("Failed to create document in tree: $treeUri")

            application.contentResolver.openOutputStream(newFileUri)?.use { out ->
                DocumentFileHandler.writeDocument(
                    text = text,
                    outputStream = out,
                    fileType = fileType
                )
            } ?: throw IllegalStateException("Failed to open output stream for created document")

            Pair(newFileUri, finalFileName)
        } else {
            val dir = File(folder.uriString)
            if (!dir.exists()) dir.mkdirs()
            val file = File(dir, finalFileName)
            writeDocumentToFile(file, text)
            Pair(Uri.fromFile(file), finalFileName)
        }
    }

    suspend fun addStarredFolderFromTreeUri(treeUri: Uri): String = withContext(Dispatchers.IO) {
        try {
            application.contentResolver.takePersistableUriPermission(
                treeUri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
        } catch (_: Exception) {}

        val docId = try { DocumentsContract.getTreeDocumentId(treeUri) } catch (_: Exception) { "Folder" }
        val rawFolderName = docId.substringAfterLast(':').substringAfterLast('/')
        val folderName = if (rawFolderName.isNotBlank()) rawFolderName else "Custom Folder"
        val pathDisplay = docId.replace(':', '/')

        val newFolder = StarredFolder(
            id = UUID.randomUUID().toString(),
            name = folderName,
            pathDisplay = pathDisplay,
            uriString = treeUri.toString(),
            isDefault = false
        )
        settingsRepo.addStarredFolder(newFolder)
        folderName
    }

    suspend fun removeStarredFolder(folderId: String) {
        settingsRepo.removeStarredFolder(folderId)
    }

    suspend fun saveSessionDraft(draft: SessionDraft) = withContext(Dispatchers.IO) {
        settingsRepo.saveSessionDraft(draft)
    }

    suspend fun clearSessionDraft() = withContext(Dispatchers.IO) {
        settingsRepo.clearSessionDraft()
    }

    fun getFileName(uri: Uri): String? {
        var result: String? = null
        if (uri.scheme == "content") {
            try {
                application.contentResolver.query(
                    uri,
                    arrayOf(OpenableColumns.DISPLAY_NAME),
                    null,
                    null,
                    null
                )?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (index != -1) {
                            result = cursor.getString(index)
                        }
                    }
                }
            } catch (_: Exception) {}
        }
        if (result.isNullOrBlank()) {
            result = uri.lastPathSegment?.let { path ->
                val cut = path.lastIndexOf('/')
                if (cut != -1) path.substring(cut + 1) else path
            }
        }
        return result
    }
}
