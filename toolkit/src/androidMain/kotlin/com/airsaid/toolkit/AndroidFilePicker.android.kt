package com.airsaid.toolkit

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.DocumentsContract
import android.webkit.MimeTypeMap
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContract
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

internal class AndroidFilePicker(
  private val activityProvider: () -> Activity?,
  private val context: Context,
) {

  suspend fun openFile(options: FilePickerOptions): PlatformFile? {
    val mimeTypes = options.type.resolveMimeTypes()
    val params = OpenDocumentParams(
      mimeTypes = mimeTypes,
      title = options.title,
      initialUri = options.startLocation?.asInitialUri(),
    )
    val uri = launch(OpenDocumentContract(), params) ?: return null
    persistPermission(uri)
    return PlatformFile(context, uri)
  }

  suspend fun openFiles(options: FilePickerOptions): List<PlatformFile> {
    val mimeTypes = options.type.resolveMimeTypes()
    val params = OpenDocumentParams(
      mimeTypes = mimeTypes,
      title = options.title,
      initialUri = options.startLocation?.asInitialUri(),
    )
    val uris = launch(OpenMultipleDocumentsContract(), params).orEmpty()
    val limited = options.mode.limit(uris)
    limited.forEach { persistPermission(it) }
    return limited.map { PlatformFile(context, it) }
  }

  suspend fun openDirectory(options: DirectoryPickerOptions): PlatformFile? {
    val params = OpenDocumentTreeParams(
      title = options.title,
      initialUri = options.startLocation?.asInitialUri(),
    )
    val uri = launch(OpenDocumentTreeContract(), params) ?: return null
    persistPermission(uri)
    return PlatformFile(context, uri)
  }

  suspend fun saveFile(options: FileSaveOptions): PlatformFile? {
    val name = buildFileName(options.suggestedName, options.extension)
    val mimeType = options.mimeType ?: options.extension?.let { extensionToMimeType(it) }
    val params = CreateDocumentParams(
      fileName = name,
      title = options.title,
      initialUri = options.directory?.asInitialUri(),
      mimeType = mimeType,
    )
    val uri = launch(CreateDocumentContract(), params) ?: return null
    persistPermission(uri)
    return PlatformFile(context, uri)
  }

  private fun persistPermission(uri: Uri) {
    val resolver = context.contentResolver
    val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
    try {
      resolver.takePersistableUriPermission(uri, flags)
    } catch (_: SecurityException) {
    } catch (_: IllegalArgumentException) {
    }
  }

  private suspend fun <I, O> launch(
    contract: ActivityResultContract<I, O>,
    input: I,
  ): O? {
    val activity = activityProvider()?.let { it as? ComponentActivity }
      ?: throw IllegalStateException(
        "FileToolkit requires a ComponentActivity to launch system pickers."
      )
    return suspendCancellableCoroutine { continuation ->
      val key = "toolkit-file-picker-${System.nanoTime()}"
      var launcher: ActivityResultLauncher<I>? = null
      launcher = activity.activityResultRegistry.register(key, contract) { result ->
        launcher?.unregister()
        if (continuation.isActive) {
          continuation.resume(result)
        }
      }
      continuation.invokeOnCancellation {
        launcher?.unregister()
      }
      launcher.launch(input)
    }
  }
}

private data class OpenDocumentParams(
  val mimeTypes: List<String>,
  val title: String?,
  val initialUri: Uri?,
)

private data class OpenDocumentTreeParams(
  val title: String?,
  val initialUri: Uri?,
)

private data class CreateDocumentParams(
  val fileName: String,
  val title: String?,
  val initialUri: Uri?,
  val mimeType: String?,
)

private class OpenDocumentContract : ActivityResultContract<OpenDocumentParams, Uri?>() {
  override fun createIntent(context: Context, input: OpenDocumentParams): Intent {
    return buildOpenDocumentIntent(input, allowMultiple = false)
  }

  override fun parseResult(resultCode: Int, intent: Intent?): Uri? {
    if (resultCode != Activity.RESULT_OK) return null
    return intent?.data
  }
}

private class OpenMultipleDocumentsContract : ActivityResultContract<OpenDocumentParams, List<Uri>>() {
  override fun createIntent(context: Context, input: OpenDocumentParams): Intent {
    return buildOpenDocumentIntent(input, allowMultiple = true)
  }

  override fun parseResult(resultCode: Int, intent: Intent?): List<Uri> {
    if (resultCode != Activity.RESULT_OK || intent == null) return emptyList()
    val clipData = intent.clipData
    if (clipData != null) {
      return List(clipData.itemCount) { index -> clipData.getItemAt(index).uri }
    }
    return intent.data?.let { listOf(it) } ?: emptyList()
  }
}

private class OpenDocumentTreeContract : ActivityResultContract<OpenDocumentTreeParams, Uri?>() {
  override fun createIntent(context: Context, input: OpenDocumentTreeParams): Intent {
    return Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
      addFlags(
        Intent.FLAG_GRANT_READ_URI_PERMISSION or
          Intent.FLAG_GRANT_WRITE_URI_PERMISSION or
          Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
      )
      input.title?.let { putExtra(Intent.EXTRA_TITLE, it) }
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        input.initialUri?.let { putExtra(DocumentsContract.EXTRA_INITIAL_URI, it) }
      }
    }
  }

  override fun parseResult(resultCode: Int, intent: Intent?): Uri? {
    if (resultCode != Activity.RESULT_OK) return null
    return intent?.data
  }
}

private class CreateDocumentContract : ActivityResultContract<CreateDocumentParams, Uri?>() {
  override fun createIntent(context: Context, input: CreateDocumentParams): Intent {
    return Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
      addCategory(Intent.CATEGORY_OPENABLE)
      type = input.mimeType ?: "*/*"
      addFlags(
        Intent.FLAG_GRANT_READ_URI_PERMISSION or
          Intent.FLAG_GRANT_WRITE_URI_PERMISSION or
          Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
      )
      putExtra(Intent.EXTRA_TITLE, input.fileName)
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        input.initialUri?.let { putExtra(DocumentsContract.EXTRA_INITIAL_URI, it) }
      }
    }
  }

  override fun parseResult(resultCode: Int, intent: Intent?): Uri? {
    if (resultCode != Activity.RESULT_OK) return null
    return intent?.data
  }
}

private fun PlatformFile.asInitialUri(): Uri? {
  return uri
}

private fun PlatformFileType.resolveMimeTypes(): List<String> {
  return when (this) {
    PlatformFileType.Image -> listOf("image/*")
    PlatformFileType.Video -> listOf("video/*")
    PlatformFileType.ImageAndVideo -> listOf("image/*", "video/*")
    is PlatformFileType.File -> {
      val resolved = extensions.mapNotNull { extensionToMimeType(it) }.distinct()
      if (resolved.isNotEmpty()) resolved else listOf("*/*")
    }
  }
}

private fun FilePickerMode.limit(uris: List<Uri>): List<Uri> {
  return when (this) {
    is FilePickerMode.Multiple -> {
      val maxItems = maxItems
      if (maxItems == null || maxItems <= 0) uris else uris.take(maxItems)
    }
    else -> uris
  }
}

private fun buildOpenDocumentIntent(
  input: OpenDocumentParams,
  allowMultiple: Boolean,
): Intent {
  return Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
    addCategory(Intent.CATEGORY_OPENABLE)
    type = input.mimeTypes.firstOrNull() ?: "*/*"
    addFlags(
      Intent.FLAG_GRANT_READ_URI_PERMISSION or
        Intent.FLAG_GRANT_WRITE_URI_PERMISSION or
        Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
    )
    putExtra(Intent.EXTRA_MIME_TYPES, input.mimeTypes.toTypedArray())
    if (allowMultiple) {
      putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
    }
    input.title?.let { putExtra(Intent.EXTRA_TITLE, it) }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      input.initialUri?.let { putExtra(DocumentsContract.EXTRA_INITIAL_URI, it) }
    }
  }
}

private fun extensionToMimeType(extension: String): String? {
  val normalized = extension.removePrefix(".").lowercase()
  return MimeTypeMap.getSingleton().getMimeTypeFromExtension(normalized)
}

private fun buildFileName(name: String, extension: String?): String {
  if (extension.isNullOrBlank()) return name
  val normalized = extension.removePrefix(".")
  return if (name.endsWith(".$normalized")) name else "$name.$normalized"
}
