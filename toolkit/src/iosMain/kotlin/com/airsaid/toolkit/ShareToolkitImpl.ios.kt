package com.airsaid.toolkit

import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.usePinned
import platform.CoreFoundation.CFDataCreate
import platform.CoreFoundation.kCFAllocatorDefault
import platform.Foundation.NSData
import platform.Foundation.NSError
import platform.Foundation.NSURL
import platform.UIKit.UIActivityTypeAirDrop
import platform.UIKit.UIActivityTypeCopyToPasteboard
import platform.UIKit.UIActivityTypeMail
import platform.UIKit.UIActivityTypeMessage
import platform.UIKit.UIActivityTypeSaveToCameraRoll
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIImage
import platform.UIKit.UIViewController
import platform.UIKit.popoverPresentationController
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue

/**
 * iOS implementation of [ShareToolkit].
 */
internal class ShareToolkitImpl : ShareToolkit {

  @OptIn(ExperimentalForeignApi::class)
  override suspend fun share(
    contents: List<ShareContent>,
    options: ShareOptions,
  ): ShareResult {
    if (contents.isEmpty()) return ShareResult.Failed(ShareFailureReason.EMPTY_CONTENT)
    val platformFiles = contents.mapNotNull { content ->
      (content as? ShareContent.PlatformFile)?.file
    }
    return try {
      withPlatformFileAccess(platformFiles) {
        for (file in platformFiles) {
          if (!file.exists() || file.isDirectory()) {
            return@withPlatformFileAccess ShareResult.Failed(ShareFailureReason.FILE_NOT_FOUND)
          }
        }
        val items = buildItems(contents)
        if (items.isEmpty()) return@withPlatformFileAccess ShareResult.Failed(
          ShareFailureReason.EMPTY_CONTENT,
        )
        val presenter = resolvePresenter() ?: return@withPlatformFileAccess ShareResult.Failed(
          ShareFailureReason.NO_PRESENTER,
        )

        suspendCoroutine<ShareResult> { continuation ->
          dispatch_async(dispatch_get_main_queue()) {
            val controller = UIActivityViewController(
              activityItems = items,
              applicationActivities = null,
            )
            val excluded = resolveExcludedActivities(options.excludedActivities)
            if (excluded.isNotEmpty()) {
              controller.excludedActivityTypes = excluded
            }
            (controller as UIViewController).popoverPresentationController()?.let { popover ->
              popover.setSourceView(presenter.view)
              popover.setSourceRect(presenter.view.bounds)
            }
            controller.completionWithItemsHandler = { _, completed, _, error ->
              continuation.resume(resolveCompletionResult(completed, error))
            }
            presenter.presentViewController(controller, animated = true, completion = null)
          }
        }
      }
    } catch (error: FileAccessException) {
      ShareResult.Failed(ShareFailureReason.FILE_ACCESS_DENIED, error)
    }
  }
}

private suspend fun <T> withPlatformFileAccess(
  files: List<com.airsaid.toolkit.PlatformFile>,
  block: suspend () -> T,
): T {
  return withPlatformFileAccess(files, 0, block)
}

private suspend fun <T> withPlatformFileAccess(
  files: List<com.airsaid.toolkit.PlatformFile>,
  index: Int,
  block: suspend () -> T,
): T {
  if (index >= files.size) return block()
  return files[index].withScopedAccess {
    withPlatformFileAccess(files, index + 1, block)
  }
}

private fun buildItems(contents: List<ShareContent>): List<Any> {
  val items = mutableListOf<Any>()
  contents.forEach { content ->
    when (content) {
      is ShareContent.Text -> {
        if (content.text.isNotBlank()) {
          items += content.text
        }
      }
      is ShareContent.Url -> {
        if (content.url.isNotBlank()) {
          NSURL(string = content.url)?.let { items += it }
        }
      }
      is ShareContent.Image -> {
        imageFromBytes(content.bytes)?.let { items += it }
      }
      is ShareContent.File -> {
        urlFromRawPath(content.uri)?.let { items += it }
      }
      is ShareContent.PlatformFile -> {
        items += content.file.url
      }
    }
  }
  return items
}

private fun urlFromRawPath(rawPath: String): NSURL? {
  val raw = rawPath.trim()
  if (raw.isEmpty()) return null
  return if (raw.startsWith("file://")) {
    NSURL(string = raw)
  } else {
    NSURL.fileURLWithPath(raw)
  }
}

private fun resolveCompletionResult(
  completed: Boolean,
  error: NSError?,
): ShareResult {
  if (error != null) {
    return ShareResult.Failed(
      ShareFailureReason.PRESENTATION_FAILED,
      IllegalStateException(error.localizedDescription),
    )
  }
  return if (completed) ShareResult.Completed else ShareResult.Cancelled
}

private fun resolveExcludedActivities(
  activities: List<ShareExcludedActivity>,
): List<String> {
  val resolved = mutableListOf<String>()
  activities.forEach { activity ->
    when (activity) {
      ShareExcludedActivity.AIR_DROP -> UIActivityTypeAirDrop?.let { resolved.add(it) }
      ShareExcludedActivity.COPY_TO_PASTEBOARD ->
        UIActivityTypeCopyToPasteboard?.let { resolved.add(it) }
      ShareExcludedActivity.MESSAGE -> UIActivityTypeMessage?.let { resolved.add(it) }
      ShareExcludedActivity.MAIL -> UIActivityTypeMail?.let { resolved.add(it) }
      ShareExcludedActivity.SAVE_TO_CAMERA_ROLL ->
        UIActivityTypeSaveToCameraRoll?.let { resolved.add(it) }
    }
  }
  return resolved
}

@OptIn(ExperimentalForeignApi::class)
private fun imageFromBytes(bytes: ByteArray): UIImage? {
  if (bytes.isEmpty()) return null
  val data = bytes.toNSData() ?: return null
  return UIImage(data = data)
}

@OptIn(ExperimentalForeignApi::class)
private fun ByteArray.toNSData(): NSData? {
  if (isEmpty()) return null
  return usePinned { pinned ->
    val dataRef = CFDataCreate(
      kCFAllocatorDefault,
      pinned.addressOf(0).reinterpret(),
      size.toLong(),
    )
    dataRef?.let { it as NSData }
  }
}
