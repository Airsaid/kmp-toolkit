package com.airsaid.toolkit

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.usePinned
import platform.CoreFoundation.CFDataCreate
import platform.CoreFoundation.kCFAllocatorDefault
import platform.Foundation.NSData
import platform.Foundation.NSURL
import platform.UIKit.UIActivityTypeAirDrop
import platform.UIKit.UIActivityTypeCopyToPasteboard
import platform.UIKit.UIActivityTypeMail
import platform.UIKit.UIActivityTypeMessage
import platform.UIKit.UIActivityTypeSaveToCameraRoll
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIImage
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue

/**
 * iOS implementation of [ShareToolkit].
 */
internal class ShareToolkitImpl : ShareToolkit {

  override fun share(
    contents: List<ShareContent>,
    options: ShareOptions,
  ): Boolean {
    val items = buildItems(contents)
    if (items.isEmpty()) return false
    val presenter = resolvePresenter() ?: return false

    dispatch_async(dispatch_get_main_queue()) {
      val controller = UIActivityViewController(
        activityItems = items,
        applicationActivities = null,
      )
      val excluded = resolveExcludedActivities(options.excludedActivities)
      if (excluded.isNotEmpty()) {
        controller.excludedActivityTypes = excluded
      }
      presenter.presentViewController(controller, animated = true, completion = null)
    }
    return true
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
        val raw = content.uri.trim()
        if (raw.isNotEmpty()) {
          val url = if (raw.startsWith("file://")) {
            NSURL(string = raw)
          } else {
            NSURL.fileURLWithPath(raw)
          }
          url?.let { items += it }
        }
      }
    }
  }
  return items
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
