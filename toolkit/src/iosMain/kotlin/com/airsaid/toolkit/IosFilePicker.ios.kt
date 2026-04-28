package com.airsaid.toolkit

import platform.Foundation.NSFileManager
import platform.Foundation.NSURL
import platform.Foundation.NSTemporaryDirectory
import platform.UIKit.UIDocumentPickerDelegateProtocol
import platform.UIKit.UIDocumentPickerMode
import platform.UIKit.UIDocumentPickerViewController
import platform.UIKit.UIViewController
import platform.darwin.NSObject
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue
import kotlinx.coroutines.CancellableContinuation
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

internal class IosFilePicker {

  private val activeDelegates = mutableSetOf<DocumentPickerDelegate>()

  suspend fun openFile(options: FilePickerOptions): PlatformFile? {
    val urls = openDocuments(
      types = options.type.resolveTypes(),
      allowsMultiple = false,
    )
    return urls.firstOrNull()?.let { PlatformFile(it) }
  }

  suspend fun openFiles(options: FilePickerOptions): List<PlatformFile> {
    val urls = openDocuments(
      types = options.type.resolveTypes(),
      allowsMultiple = true,
    )
    val limited = options.mode.limit(urls)
    return limited.map { PlatformFile(it) }
  }

  suspend fun openDirectory(options: DirectoryPickerOptions): PlatformFile? {
    val urls = openDocuments(
      types = listOf("public.folder"),
      allowsMultiple = false,
    )
    return urls.firstOrNull()?.let { PlatformFile(it) }
  }

  suspend fun saveFile(options: FileSaveOptions): PlatformFile? {
    val fileName = buildFileName(options.suggestedName, options.extension)
    val tempUrl = temporaryUrl(fileName)
    val urls = openExporter(
      url = tempUrl,
    )
    return urls.firstOrNull()?.let { PlatformFile(it) }
  }

  private suspend fun openDocuments(
    types: List<String>,
    allowsMultiple: Boolean,
  ): List<NSURL> {
    return suspendCancellableCoroutine { continuation ->
      val presenter = resolvePresenterOrThrow(continuation) ?: return@suspendCancellableCoroutine
      val controller = UIDocumentPickerViewController(
        documentTypes = types,
        inMode = UIDocumentPickerMode.UIDocumentPickerModeImport,
      )
      controller.allowsMultipleSelection = allowsMultiple
      presentPicker(controller, presenter, continuation)
    }
  }

  private suspend fun openExporter(
    url: NSURL,
  ): List<NSURL> {
    return suspendCancellableCoroutine { continuation ->
      val presenter = resolvePresenterOrThrow(continuation) ?: return@suspendCancellableCoroutine
      val controller = UIDocumentPickerViewController(
        uRLs = listOf(url),
        inMode = UIDocumentPickerMode.UIDocumentPickerModeExportToService,
      )
      presentPicker(controller, presenter, continuation)
    }
  }

  private fun presentPicker(
    controller: UIDocumentPickerViewController,
    presenter: UIViewController,
    continuation: CancellableContinuation<List<NSURL>>,
  ) {
    val delegate = DocumentPickerDelegate(continuation) {
      activeDelegates.remove(it)
    }
    activeDelegates.add(delegate)
    continuation.invokeOnCancellation {
      activeDelegates.remove(delegate)
    }
    controller.delegate = delegate
    dispatch_async(dispatch_get_main_queue()) {
      presenter.presentViewController(controller, animated = true, completion = null)
    }
  }

  private fun temporaryUrl(fileName: String): NSURL {
    val tempPath = NSTemporaryDirectory()
    val url = NSURL.fileURLWithPath("$tempPath/$fileName")
    val path = url.path ?: return url
    NSFileManager.defaultManager.createFileAtPath(path, null, null)
    return url
  }

  private fun resolvePresenterOrThrow(
    continuation: CancellableContinuation<List<NSURL>>,
  ): UIViewController? {
    val presenter = resolvePresenter()
    if (presenter == null) {
      continuation.resumeWithException(
        FileAccessException("No available presenter for document picker.")
      )
      return null
    }
    return presenter
  }
}

private class DocumentPickerDelegate(
  private val continuation: CancellableContinuation<List<NSURL>>,
  private val onComplete: (DocumentPickerDelegate) -> Unit,
) : NSObject(), UIDocumentPickerDelegateProtocol {

  override fun documentPicker(
    controller: UIDocumentPickerViewController,
    didPickDocumentsAtURLs: List<*>,
  ) {
    val urls = didPickDocumentsAtURLs.filterIsInstance<NSURL>()
    if (continuation.isActive) {
      continuation.resume(urls)
    }
    onComplete(this)
  }

  override fun documentPickerWasCancelled(
    controller: UIDocumentPickerViewController,
  ) {
    if (continuation.isActive) {
      continuation.resume(emptyList())
    }
    onComplete(this)
  }
}

private fun PlatformFileType.resolveTypes(): List<String> {
  return when (this) {
    PlatformFileType.Image -> listOf("public.image")
    PlatformFileType.Video -> listOf("public.movie")
    PlatformFileType.ImageAndVideo -> listOf("public.image", "public.movie")
    is PlatformFileType.File -> listOf("public.data")
  }
}

private fun FilePickerMode.limit(urls: List<NSURL>): List<NSURL> {
  return when (this) {
    is FilePickerMode.Multiple -> {
      val maxItems = maxItems
      if (maxItems == null || maxItems <= 0) urls else urls.take(maxItems)
    }
    else -> urls
  }
}

private fun buildFileName(name: String, extension: String?): String {
  if (extension.isNullOrBlank()) return name
  val normalized = extension.removePrefix(".")
  return if (name.endsWith(".$normalized")) name else "$name.$normalized"
}
