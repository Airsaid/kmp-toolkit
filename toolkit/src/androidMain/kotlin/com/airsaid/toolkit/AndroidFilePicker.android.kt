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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
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
    val result = launch(OpenDocumentContract(), params) ?: return null
    val uri = result.uri ?: return null
    persistPermission(uri, result.grantFlags)
    return PlatformFile(context, uri)
  }

  suspend fun openFiles(options: FilePickerOptions, maxItems: Int?): List<PlatformFile> {
    val mimeTypes = options.type.resolveMimeTypes()
    val params = OpenDocumentParams(
      mimeTypes = mimeTypes,
      title = options.title,
      initialUri = options.startLocation?.asInitialUri(),
    )
    val result = launch(OpenMultipleDocumentsContract(), params)
    val uris = result?.uris.orEmpty()
    val limited = limitFileSelection(uris, maxItems)
    limited.forEach { persistPermission(it, result?.grantFlags ?: 0) }
    return limited.map { PlatformFile(context, it) }
  }

  suspend fun openDirectory(options: DirectoryPickerOptions): PlatformFile? {
    val params = OpenDocumentTreeParams(
      title = options.title,
      initialUri = options.startLocation?.asInitialUri(),
    )
    val result = launch(OpenDocumentTreeContract(), params) ?: return null
    val uri = result.uri ?: return null
    persistPermission(uri, result.grantFlags)
    return PlatformFile(context, uri)
  }

  suspend fun createFile(options: FileCreateOptions): PlatformFile? {
    val name = buildPlatformFileName(options.suggestedName, options.extension)
    val mimeType = options.mimeType ?: options.extension?.let { extensionToMimeType(it) }
    val params = CreateDocumentParams(
      fileName = name,
      title = options.title,
      initialUri = options.directory?.asInitialUri(),
      mimeType = mimeType,
    )
    val result = launch(CreateDocumentContract(), params) ?: return null
    val uri = result.uri ?: return null
    persistPermission(uri, result.grantFlags)
    return PlatformFile(context, uri)
  }

  private fun persistPermission(uri: Uri, grantFlags: Int) {
    val resolver = context.contentResolver
    val flags = grantFlags and PERSISTABLE_PERMISSION_FLAGS
    if (flags == 0) return
    try {
      resolver.takePersistableUriPermission(uri, flags)
    } catch (_: SecurityException) {
    } catch (_: IllegalArgumentException) {
    }
  }

  private suspend fun <I, O> launch(
    contract: ActivityResultContract<I, O>,
    input: I,
  ): O? = withContext(Dispatchers.Main.immediate) {
    val activity = activityProvider()?.let { it as? ComponentActivity }
      ?: throw IllegalStateException(
        "FileToolkit requires a ComponentActivity to launch system pickers."
      )
    suspendCancellableCoroutine { continuation ->
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

private const val PERSISTABLE_PERMISSION_FLAGS =
  Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION

internal data class DocumentResult(
  val uri: Uri?,
  val grantFlags: Int,
)

internal data class MultipleDocumentResult(
  val uris: List<Uri>,
  val grantFlags: Int,
)

internal data class OpenDocumentParams(
  val mimeTypes: List<String>,
  val title: String?,
  val initialUri: Uri?,
)

internal data class OpenDocumentTreeParams(
  val title: String?,
  val initialUri: Uri?,
)

internal data class CreateDocumentParams(
  val fileName: String,
  val title: String?,
  val initialUri: Uri?,
  val mimeType: String?,
)

internal data class DocumentIntentSpec(
  val action: String,
  val type: String,
  val mimeTypes: List<String> = emptyList(),
  val title: String? = null,
  val initialUri: String? = null,
  val allowMultiple: Boolean = false,
)

private class OpenDocumentContract : ActivityResultContract<OpenDocumentParams, DocumentResult>() {
  override fun createIntent(context: Context, input: OpenDocumentParams): Intent {
    return buildOpenDocumentIntent(input, allowMultiple = false)
  }

  override fun parseResult(resultCode: Int, intent: Intent?): DocumentResult {
    if (resultCode != Activity.RESULT_OK) return DocumentResult(null, 0)
    return DocumentResult(
      uri = intent?.data,
      grantFlags = intent?.flags ?: 0,
    )
  }
}

private class OpenMultipleDocumentsContract :
  ActivityResultContract<OpenDocumentParams, MultipleDocumentResult>() {
  override fun createIntent(context: Context, input: OpenDocumentParams): Intent {
    return buildOpenDocumentIntent(input, allowMultiple = true)
  }

  override fun parseResult(resultCode: Int, intent: Intent?): MultipleDocumentResult {
    if (resultCode != Activity.RESULT_OK || intent == null) {
      return MultipleDocumentResult(emptyList(), 0)
    }
    val clipData = intent.clipData
    val uris = if (clipData != null) {
      List(clipData.itemCount) { index -> clipData.getItemAt(index).uri }
    } else {
      intent.data?.let { listOf(it) } ?: emptyList()
    }
    return MultipleDocumentResult(
      uris = uris,
      grantFlags = intent.flags,
    )
  }
}

private class OpenDocumentTreeContract :
  ActivityResultContract<OpenDocumentTreeParams, DocumentResult>() {
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

  override fun parseResult(resultCode: Int, intent: Intent?): DocumentResult {
    if (resultCode != Activity.RESULT_OK) return DocumentResult(null, 0)
    return DocumentResult(
      uri = intent?.data,
      grantFlags = intent?.flags ?: 0,
    )
  }
}

private class CreateDocumentContract : ActivityResultContract<CreateDocumentParams, DocumentResult>() {
  override fun createIntent(context: Context, input: CreateDocumentParams): Intent {
    val spec = input.toCreateDocumentIntentSpec()
    return Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
      addCategory(Intent.CATEGORY_OPENABLE)
      type = spec.type
      addFlags(
        Intent.FLAG_GRANT_READ_URI_PERMISSION or
          Intent.FLAG_GRANT_WRITE_URI_PERMISSION or
          Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
      )
      putExtra(Intent.EXTRA_TITLE, spec.title)
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        input.initialUri?.let { putExtra(DocumentsContract.EXTRA_INITIAL_URI, it) }
      }
    }
  }

  override fun parseResult(resultCode: Int, intent: Intent?): DocumentResult {
    if (resultCode != Activity.RESULT_OK) return DocumentResult(null, 0)
    return DocumentResult(
      uri = intent?.data,
      grantFlags = intent?.flags ?: 0,
    )
  }
}

private fun PlatformFile.asInitialUri(): Uri? {
  return uri
}

internal fun PlatformFileType.resolveMimeTypes(): List<String> {
  return resolveMimeTypes(::extensionToMimeType)
}

internal fun PlatformFileType.resolveMimeTypes(
  extensionMimeType: (String) -> String?,
): List<String> {
  return when (this) {
    PlatformFileType.Image -> listOf("image/*")
    PlatformFileType.Video -> listOf("video/*")
    PlatformFileType.ImageAndVideo -> listOf("image/*", "video/*")
    is PlatformFileType.File -> {
      val resolved = extensions.mapNotNull { extensionMimeType(it) }.distinct()
      if (resolved.isNotEmpty()) resolved else listOf("*/*")
    }
  }
}

internal fun buildOpenDocumentIntent(
  input: OpenDocumentParams,
  allowMultiple: Boolean,
): Intent {
  val spec = input.toOpenDocumentIntentSpec(allowMultiple)
  return Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
    addCategory(Intent.CATEGORY_OPENABLE)
    type = spec.type
    addFlags(
      Intent.FLAG_GRANT_READ_URI_PERMISSION or
        Intent.FLAG_GRANT_WRITE_URI_PERMISSION or
        Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
    )
    putExtra(Intent.EXTRA_MIME_TYPES, spec.mimeTypes.toTypedArray())
    if (spec.allowMultiple) {
      putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
    }
    spec.title?.let { putExtra(Intent.EXTRA_TITLE, it) }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      input.initialUri?.let { putExtra(DocumentsContract.EXTRA_INITIAL_URI, it) }
    }
  }
}

internal fun OpenDocumentParams.toOpenDocumentIntentSpec(
  allowMultiple: Boolean,
): DocumentIntentSpec {
  return DocumentIntentSpec(
    action = Intent.ACTION_OPEN_DOCUMENT,
    type = mimeTypes.firstOrNull() ?: "*/*",
    mimeTypes = mimeTypes,
    title = title,
    initialUri = initialUri?.toString(),
    allowMultiple = allowMultiple,
  )
}

internal fun CreateDocumentParams.toCreateDocumentIntentSpec(): DocumentIntentSpec {
  return DocumentIntentSpec(
    action = Intent.ACTION_CREATE_DOCUMENT,
    type = mimeType ?: "*/*",
    title = fileName,
    initialUri = initialUri?.toString(),
  )
}

internal fun extensionToMimeType(extension: String): String? {
  val normalized = extension.removePrefix(".").trim().lowercase()
  if (normalized.isEmpty()) return null
  return MimeTypeMap.getSingleton().getMimeTypeFromExtension(normalized)
}
