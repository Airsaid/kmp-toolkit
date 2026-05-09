package com.airsaid.toolkit

import platform.Foundation.NSFileManager
import platform.Foundation.NSURL
import platform.Foundation.NSTemporaryDirectory
import platform.UniformTypeIdentifiers.UTType
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
      title = options.title,
      directoryUrl = options.startLocation?.url,
    )
    return urls.firstOrNull()?.let { PlatformFile(it) }
  }

  suspend fun openFiles(options: FilePickerOptions, maxItems: Int?): List<PlatformFile> {
    val urls = openDocuments(
      types = options.type.resolveTypes(),
      allowsMultiple = true,
      title = options.title,
      directoryUrl = options.startLocation?.url,
    )
    val limited = limitFileSelection(urls, maxItems)
    return limited.map { PlatformFile(it) }
  }

  suspend fun openDirectory(options: DirectoryPickerOptions): PlatformFile? {
    val urls = openDocuments(
      types = listOf("public.folder"),
      allowsMultiple = false,
      title = options.title,
      directoryUrl = options.startLocation?.url,
    )
    return urls.firstOrNull()?.let { PlatformFile(it) }
  }

  suspend fun createFile(options: FileCreateOptions): PlatformFile? {
    val fileName = buildPlatformFileName(options.suggestedName, options.extension)
    val tempUrl = temporaryUrl(fileName)
    val urls = openExporter(
      url = tempUrl,
      title = options.title,
      directoryUrl = options.directory?.url,
    )
    return urls.firstOrNull()?.let { PlatformFile(it) }
  }

  private suspend fun openDocuments(
    types: List<String>,
    allowsMultiple: Boolean,
    title: String?,
    directoryUrl: NSURL?,
  ): List<NSURL> {
    return suspendCancellableCoroutine { continuation ->
      dispatch_async(dispatch_get_main_queue()) {
        if (!continuation.isActive) return@dispatch_async
        val presenter = resolvePresenterOrThrow(continuation) ?: return@dispatch_async
        val controller = createOpeningController(types, allowsMultiple)
        controller.title = title
        controller.directoryURL = directoryUrl
        presentPicker(controller, presenter, continuation)
      }
    }
  }

  private suspend fun openExporter(
    url: NSURL,
    title: String?,
    directoryUrl: NSURL?,
  ): List<NSURL> {
    return suspendCancellableCoroutine { continuation ->
      dispatch_async(dispatch_get_main_queue()) {
        if (!continuation.isActive) return@dispatch_async
        val presenter = resolvePresenterOrThrow(continuation) ?: return@dispatch_async
        val controller = createExportController(url)
        controller.title = title
        controller.directoryURL = directoryUrl
        presentPicker(controller, presenter, continuation)
      }
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
    presenter.presentViewController(controller, animated = true, completion = null)
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

  private fun createOpeningController(
    types: List<String>,
    allowsMultiple: Boolean,
  ): UIDocumentPickerViewController {
    val contentTypes = types.mapNotNull { UTType.typeWithIdentifier(it) }
    val controller = UIDocumentPickerViewController(
      forOpeningContentTypes = contentTypes,
      asCopy = true,
    )
    controller.allowsMultipleSelection = allowsMultiple
    return controller
  }

  private fun createExportController(url: NSURL): UIDocumentPickerViewController {
    return UIDocumentPickerViewController(
      forExportingURLs = listOf(url),
      asCopy = true,
    )
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
    is PlatformFileType.File -> {
      val resolved = extensions.mapNotNull { it.toUniformTypeIdentifier() }.distinct()
      if (resolved.isNotEmpty()) resolved else listOf(PUBLIC_DATA_TYPE)
    }
  }
}

private const val PUBLIC_DATA_TYPE = "public.data"

private fun String.toUniformTypeIdentifier(): String? {
  val normalized = removePrefix(".").trim().lowercase()
  if (normalized.isEmpty()) return null
  return UTType.typeWithFilenameExtension(normalized)?.identifier
}
