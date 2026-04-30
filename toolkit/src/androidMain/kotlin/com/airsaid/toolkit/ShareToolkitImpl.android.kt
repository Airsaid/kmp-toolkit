package com.airsaid.toolkit

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import java.io.File
import java.io.IOException

/**
 * Android implementation of [ShareToolkit].
 */
internal class ShareToolkitImpl(
  private val context: Context,
) : ShareToolkit {

  override fun share(
    contents: List<ShareContent>,
    options: ShareOptions,
  ): Boolean {
    if (contents.isEmpty()) return false

    val uris = mutableListOf<Uri>()
    val textParts = mutableListOf<String>()
    var resolvedMimeType: String? = null

    contents.forEach { content ->
      when (content) {
        is ShareContent.Text -> {
          if (content.text.isNotBlank()) {
            textParts += content.text
          }
        }
        is ShareContent.Url -> {
          if (content.url.isNotBlank()) {
            textParts += content.url
          }
        }
        is ShareContent.Image -> {
          val uri = ShareFileStore.writeBytes(
            context,
            content.bytes,
            content.mimeType,
          ) ?: return false
          uris += uri
          resolvedMimeType = resolveMimeType(resolvedMimeType, content.mimeType)
        }
        is ShareContent.File -> {
          val uri = resolveUri(content) ?: return false
          uris += uri
          resolvedMimeType = resolveMimeType(resolvedMimeType, content.mimeType)
        }
      }
    }

    val hasStreams = uris.isNotEmpty()
    val hasText = textParts.isNotEmpty()
    if (!hasStreams && !hasText) return false

    val intent = if (hasStreams && uris.size > 1) {
      Intent(Intent.ACTION_SEND_MULTIPLE).apply {
        putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
      }
    } else {
      Intent(Intent.ACTION_SEND).apply {
        if (hasStreams) {
          putExtra(Intent.EXTRA_STREAM, uris.first())
        }
      }
    }

    if (hasText) {
      intent.putExtra(Intent.EXTRA_TEXT, textParts.joinToString("\n"))
    }

    val mimeType = if (hasStreams) {
      resolvedMimeType ?: MIME_TYPE_ALL
    } else {
      MIME_TYPE_TEXT
    }
    intent.type = mimeType
    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    if (hasStreams) {
      intent.clipData = buildClipData(uris)
    }

    val chooser = Intent.createChooser(intent, options.title)
      .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    return startActivitySafely(chooser)
  }

  private fun buildClipData(uris: List<Uri>): ClipData? {
    val first = uris.firstOrNull() ?: return null
    val clipData = ClipData.newUri(context.contentResolver, CLIP_LABEL, first)
    uris.drop(1).forEach { uri ->
      clipData.addItem(ClipData.Item(uri))
    }
    return clipData
  }

  private fun resolveUri(content: ShareContent.File): Uri? {
    val rawUri = content.uri.trim()
    if (rawUri.isEmpty()) return null
    val parsed = rawUri.toUri()
    val scheme = parsed.scheme
    if (scheme == "content") {
      return parsed
    }
    if (scheme == "file") {
      val file = File(parsed.path.orEmpty())
      return ShareFileStore.ensureShareableFile(context, file, content.mimeType)
    }
    val file = File(rawUri)
    return ShareFileStore.ensureShareableFile(context, file, content.mimeType)
  }

  private fun resolveMimeType(current: String?, incoming: String?): String? {
    val next = incoming?.takeIf { it.isNotBlank() } ?: return current
    return when {
      current == null -> next
      current.equals(next, ignoreCase = true) -> current
      else -> MIME_TYPE_ALL
    }
  }

  private fun startActivitySafely(intent: Intent): Boolean {
    return try {
      context.startActivity(intent)
      true
    } catch (_: ActivityNotFoundException) {
      false
    } catch (_: SecurityException) {
      false
    }
  }

  companion object {
    private const val CLIP_LABEL = "share"
    private const val MIME_TYPE_TEXT = "text/plain"
    private const val MIME_TYPE_ALL = "*/*"
  }
}

private object ShareFileStore {

  private const val CACHE_DIR = "toolkit_share"

  fun writeBytes(
    context: Context,
    bytes: ByteArray,
    mimeType: String,
  ): Uri? {
    if (bytes.isEmpty()) return null
    val extension = mimeType.substringAfter('/').takeIf { it.isNotBlank() } ?: "bin"
    return try {
      val file = createTargetFile(context, extension)
      file.outputStream().use { it.write(bytes) }
      buildFileUri(context, file)
    } catch (_: IOException) {
      null
    }
  }

  fun ensureShareableFile(
    context: Context,
    file: File,
    mimeType: String?,
  ): Uri? {
    if (!file.exists()) return null
    val cacheDir = File(context.cacheDir, CACHE_DIR)
    val cachePath = cacheDir.canonicalPath
    val filePath = file.canonicalPath
    return if (filePath.startsWith(cachePath)) {
      buildFileUri(context, file)
    } else {
      copyToCache(context, file, mimeType)
    }
  }

  private fun copyToCache(
    context: Context,
    file: File,
    mimeType: String?,
  ): Uri? {
    val extension = mimeType?.substringAfter('/')?.takeIf { it.isNotBlank() }
      ?: file.extension.ifBlank { "bin" }
    return try {
      val target = createTargetFile(context, extension)
      file.inputStream().use { input ->
        target.outputStream().use { output ->
          input.copyTo(output)
        }
      }
      buildFileUri(context, target)
    } catch (_: IOException) {
      null
    }
  }

  private fun createTargetFile(context: Context, extension: String): File {
    val dir = File(context.cacheDir, CACHE_DIR)
    if (!dir.exists()) {
      dir.mkdirs()
    }
    return File.createTempFile("share_", ".$extension", dir)
  }

  private fun buildFileUri(context: Context, file: File): Uri {
    return FileProvider.getUriForFile(
      context,
      "${context.packageName}.toolkit-clipboard",
      file,
    )
  }
}
