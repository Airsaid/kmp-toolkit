package com.airsaid.toolkit

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Android implementation of [PlatformFile].
 */
actual class PlatformFile internal constructor(
  private val context: Context,
  internal val uri: Uri,
) {

  private val documentFile: DocumentFile? by lazy {
    when (resolveDocumentFileUriType(uri)) {
      DocumentFileUriType.TREE -> DocumentFile.fromTreeUri(context, uri)
      DocumentFileUriType.SINGLE -> DocumentFile.fromSingleUri(context, uri)
    }
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

  actual suspend fun size(): Long? = withContext(Dispatchers.IO) {
    val queriedSize = querySize()
    if (queriedSize != null) return@withContext queriedSize
    val documentSize = documentFile?.length()
    documentSize?.takeIf { it > 0L }
  }

  actual suspend fun mimeType(): String? = withContext(Dispatchers.IO) {
    val type = documentFile?.type
    type ?: runCatching { context.contentResolver.getType(uri) }.getOrNull()
  }

  actual suspend fun exists(): Boolean = withContext(Dispatchers.IO) {
    documentFile?.exists() ?: run {
      queryDisplayName() != null
    }
  }

  actual suspend fun isDirectory(): Boolean = withContext(Dispatchers.IO) {
    val document = documentFile
    if (document != null) return@withContext document.isDirectory
    val resolver = context.contentResolver
    val type = runCatching { resolver.getType(uri) }.getOrNull()
    type == DocumentsContract.Document.MIME_TYPE_DIR
  }

  actual suspend fun <T> withScopedAccess(block: suspend (PlatformFile) -> T): T {
    return block(this)
  }

  private fun queryDisplayName(): String? {
    return queryColumn(OpenableColumns.DISPLAY_NAME)?.takeIf { it.isNotBlank() }
  }

  private fun querySize(): Long? {
    return queryColumn(OpenableColumns.SIZE)?.toLongOrNull()
  }

  private fun queryColumn(column: String): String? {
    val resolver: ContentResolver = context.contentResolver
    val cursor = try {
      resolver.query(uri, arrayOf(column), null, null, null) ?: return null
    } catch (_: SecurityException) {
      return null
    } catch (_: IllegalArgumentException) {
      return null
    }
    cursor.use {
      if (!it.moveToFirst()) return null
      val index = it.getColumnIndex(column)
      if (index < 0) return null
      return runCatching { it.getString(index) }.getOrNull()
    }
  }
}

internal enum class DocumentFileUriType {
  SINGLE,
  TREE,
}

internal fun resolveDocumentFileUriType(uri: Uri): DocumentFileUriType {
  return if (DocumentsContract.isTreeUri(uri) || resolveDocumentFileUriType(uri.pathSegments) == DocumentFileUriType.TREE) {
    DocumentFileUriType.TREE
  } else {
    DocumentFileUriType.SINGLE
  }
}

internal fun resolveDocumentFileUriType(pathSegments: List<String>): DocumentFileUriType {
  return if (pathSegments.firstOrNull() == "tree") {
    DocumentFileUriType.TREE
  } else {
    DocumentFileUriType.SINGLE
  }
}
