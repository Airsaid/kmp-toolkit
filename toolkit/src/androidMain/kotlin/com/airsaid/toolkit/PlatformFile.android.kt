package com.airsaid.toolkit

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import androidx.documentfile.provider.DocumentFile

/**
 * Android implementation of [PlatformFile].
 */
actual class PlatformFile internal constructor(
  private val context: Context,
  internal val uri: Uri,
) {

  private val documentFile: DocumentFile? by lazy {
    val doc = DocumentFile.fromSingleUri(context, uri)
    doc ?: DocumentFile.fromTreeUri(context, uri)
  }

  actual val name: String
    get() = documentFile?.name ?: queryDisplayName().orEmpty()

  actual val extension: String
    get() = name.substringAfterLast('.', missingDelimiterValue = "")

  actual val nameWithoutExtension: String
    get() = if (extension.isBlank()) name else name.removeSuffix(".$extension")

  actual val path: String?
    get() = uri.toString()

  actual val absolutePath: String?
    get() = null

  actual suspend fun size(): Long {
    val documentSize = documentFile?.length()
    if (documentSize != null && documentSize >= 0L) return documentSize
    return querySize()
  }

  actual suspend fun mimeType(): String? {
    val type = documentFile?.type
    return type ?: context.contentResolver.getType(uri)
  }

  actual suspend fun exists(): Boolean {
    return documentFile?.exists() ?: run {
      queryDisplayName() != null
    }
  }

  actual suspend fun isDirectory(): Boolean {
    val document = documentFile
    if (document != null) return document.isDirectory
    val resolver = context.contentResolver
    val type = resolver.getType(uri)
    return type == DocumentsContract.Document.MIME_TYPE_DIR
  }

  actual suspend fun <T> withScopedAccess(block: suspend (PlatformFile) -> T): T {
    return block(this)
  }

  private fun queryDisplayName(): String? {
    return queryColumn(OpenableColumns.DISPLAY_NAME)?.takeIf { it.isNotBlank() }
  }

  private fun querySize(): Long {
    return queryColumn(OpenableColumns.SIZE)?.toLongOrNull() ?: 0L
  }

  private fun queryColumn(column: String): String? {
    val resolver: ContentResolver = context.contentResolver
    val cursor = resolver.query(uri, arrayOf(column), null, null, null) ?: return null
    cursor.use {
      if (!it.moveToFirst()) return null
      val index = it.getColumnIndex(column)
      if (index < 0) return null
      return it.getString(index)
    }
  }
}
