package com.example.core.editor.dialogs

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class FileEntry(
    val name: String,
    val isDirectory: Boolean,
    val path: String,
    val sizeString: String = "",
    val lastModifiedString: String = "",
    val fileRef: File? = null,
    val uriString: String? = null
)

data class CurrentDirectory(
    val displayName: String,
    val displayPath: String,
    val uriOrPath: String,
    val isTreeUri: Boolean,
    val parent: CurrentDirectory?
)

object DirectoryLoader {
    fun loadEntriesForDirectory(context: Context, directory: CurrentDirectory): List<FileEntry> {
        val results = mutableListOf<FileEntry>()
        val dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())

        if (!directory.isTreeUri) {
            val dir = File(directory.uriOrPath)
            if (dir.exists() && dir.isDirectory) {
                val list = dir.listFiles() ?: arrayOf()
                val sorted = list.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))
                for (f in sorted) {
                    val sizeStr = if (f.isFile) {
                        val kb = (f.length() / 1024.0)
                        if (kb < 1.0) "${f.length()} B" else String.format(Locale.getDefault(), "%.1f KB", kb)
                    } else {
                        val count = f.list()?.size ?: 0
                        "$count items"
                    }
                    val dateStr = dateFormat.format(Date(f.lastModified()))
                    results.add(
                        FileEntry(
                            name = f.name,
                            isDirectory = f.isDirectory,
                            path = f.absolutePath,
                            sizeString = sizeStr,
                            lastModifiedString = dateStr,
                            fileRef = f
                        )
                    )
                }
            }
        } else {
            try {
                val treeUri = Uri.parse(directory.uriOrPath)
                val docId = DocumentsContract.getTreeDocumentId(treeUri)
                val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, docId)
                val projection = arrayOf(
                    DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                    DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                    DocumentsContract.Document.COLUMN_MIME_TYPE,
                    DocumentsContract.Document.COLUMN_SIZE,
                    DocumentsContract.Document.COLUMN_LAST_MODIFIED
                )
                context.contentResolver.query(childrenUri, projection, null, null, null)?.use { cursor ->
                    val idCol = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                    val nameCol = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                    val mimeCol = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_MIME_TYPE)
                    val sizeCol = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_SIZE)
                    val modCol = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_LAST_MODIFIED)

                    while (cursor.moveToNext()) {
                        val id = if (idCol >= 0) cursor.getString(idCol) else ""
                        val name = if (nameCol >= 0) cursor.getString(nameCol) else "Item"
                        val mime = if (mimeCol >= 0) cursor.getString(mimeCol) else ""
                        val isDir = mime == DocumentsContract.Document.MIME_TYPE_DIR
                        val size = if (sizeCol >= 0) cursor.getLong(sizeCol) else 0L
                        val mod = if (modCol >= 0) cursor.getLong(modCol) else 0L

                        val sizeStr = if (!isDir) {
                            val kb = (size / 1024.0)
                            if (kb < 1.0) "$size B" else String.format(Locale.getDefault(), "%.1f KB", kb)
                        } else "Folder"
                        val dateStr = if (mod > 0) dateFormat.format(Date(mod)) else ""

                        results.add(
                            FileEntry(
                                name = name,
                                isDirectory = isDir,
                                path = id,
                                sizeString = sizeStr,
                                lastModifiedString = dateStr,
                                uriString = directory.uriOrPath
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return results
    }
}
