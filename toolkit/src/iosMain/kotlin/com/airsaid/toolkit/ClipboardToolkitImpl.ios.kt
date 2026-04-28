package com.airsaid.toolkit

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.usePinned
import platform.CoreFoundation.CFDataCreate
import platform.CoreFoundation.kCFAllocatorDefault
import platform.Foundation.NSData
import platform.Foundation.NSLock
import platform.Foundation.NSNotification
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSURL
import platform.UIKit.UIPasteboard
import platform.UIKit.UIPasteboardChangedNotification
import platform.UIKit.UIPasteboardRemovedNotification
import platform.UIKit.UIImage
import platform.UIKit.UIImageJPEGRepresentation
import platform.UIKit.UIImagePNGRepresentation
import platform.posix.memcpy

/**
 * iOS implementation of [ClipboardToolkit].
 */
internal class ClipboardToolkitImpl : ClipboardToolkit {

  private val pasteboard = UIPasteboard.generalPasteboard
  private val initialSnapshot = readSnapshot()
  private val tracker = ClipboardSnapshotTracker(initialSnapshot)
  private val snapshotState = MutableStateFlow(initialSnapshot)
  private val lock = NSLock()
  private val observers = mutableListOf<Any>()
  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default.limitedParallelism(1))

  private var isMonitoring = false
  private var observerCount = 0

  override fun observeClipboard(): Flow<ClipboardSnapshot> {
    return snapshotState
      .onStart { onObserverStart() }
      .onCompletion { onObserverStop() }
      .conflate()
      .distinctUntilChanged()
  }

  override suspend fun getSnapshot(): ClipboardSnapshot {
    return withContext(Dispatchers.Default) { readSnapshot() }
  }

  override suspend fun getText(): String? {
    return withContext(Dispatchers.Default) { readSnapshot().firstTextOrNull() }
  }

  override suspend fun hasText(): Boolean {
    return withContext(Dispatchers.Default) { readSnapshot().containsText() }
  }

  override fun setContents(contents: List<ClipboardContent>) {
    if (contents.isEmpty()) {
      clear()
      return
    }
    val items = contents.mapNotNull { buildPasteboardItem(it) }
    if (items.isEmpty()) {
      clear()
      return
    }
    pasteboard.items = items
  }

  override fun setText(text: String) {
    setContents(listOf(ClipboardContent.Text(text)))
  }

  override fun clear() {
    pasteboard.string = null
  }

  private fun onObserverStart() {
    withLock {
      observerCount += 1
      if (!isMonitoring) {
        startMonitoringInternal()
      }
    }
  }

  private fun onObserverStop() {
    withLock {
      observerCount = (observerCount - 1).coerceAtLeast(0)
      if (observerCount == 0 && isMonitoring) {
        stopMonitoringInternal()
      }
    }
  }

  private fun startMonitoringInternal() {
    if (isMonitoring) return

    updateSnapshotState()
    val center = NSNotificationCenter.defaultCenter
    observers += center.addObserverForName(
      name = UIPasteboardChangedNotification,
      `object` = pasteboard,
      queue = null,
      usingBlock = { _: NSNotification? ->
        updateSnapshotState()
      },
    )
    observers += center.addObserverForName(
      name = UIPasteboardRemovedNotification,
      `object` = pasteboard,
      queue = null,
      usingBlock = { _: NSNotification? ->
        updateSnapshotState()
      },
    )

    isMonitoring = true
  }

  private fun stopMonitoringInternal() {
    val center = NSNotificationCenter.defaultCenter
    observers.forEach { observer ->
      center.removeObserver(observer)
    }
    observers.clear()
    isMonitoring = false
  }

  private fun updateSnapshotState() {
    scope.launch {
      val snapshot = readSnapshot()
      val updated = withLock { tracker.update(snapshot) }
      if (updated != null) {
        snapshotState.value = updated
      }
    }
  }

  private fun readSnapshot(): ClipboardSnapshot {
    val contents = mutableListOf<ClipboardContent>()
    val items = pasteboard.items
    if (items.isNotEmpty()) {
      items.forEach { item ->
        val map = item as? Map<*, *> ?: return@forEach
        resolveContent(map)?.let { contents.add(it) }
      }
    }
    if (contents.isEmpty()) {
      pasteboard.string?.let { contents.add(ClipboardContent.Text(it)) }
      pasteboard.URL?.absoluteString?.let { contents.add(ClipboardContent.Uri(it)) }
      pasteboard.image?.let { image ->
        image.toPngData()?.let { data ->
          contents.add(ClipboardContent.Image(data.toByteArray(), MIME_TYPE_PNG))
        }
      }
    }
    return ClipboardSnapshot(contents)
  }

  private fun resolveContent(item: Map<*, *>): ClipboardContent? {
    val htmlData = item[UTI_HTML] as? NSData
    if (htmlData != null) {
      return ClipboardContent.RichText(
        text = htmlData.toUtf8String().orEmpty(),
        format = RichTextFormat.HTML,
      )
    }

    val rtfData = item[UTI_RTF] as? NSData
    if (rtfData != null) {
      return ClipboardContent.RichText(
        text = rtfData.toUtf8String().orEmpty(),
        format = RichTextFormat.RTF,
      )
    }

    val pngData = item[UTI_PNG] as? NSData
    if (pngData != null) {
      return ClipboardContent.Image(pngData.toByteArray(), MIME_TYPE_PNG)
    }

    val jpegData = item[UTI_JPEG] as? NSData
    if (jpegData != null) {
      return ClipboardContent.Image(jpegData.toByteArray(), MIME_TYPE_JPEG)
    }

    val urlValue = item[UTI_URL]
    if (urlValue is NSURL) {
      return ClipboardContent.Uri(urlValue.absoluteString.orEmpty())
    }
    if (urlValue is String) {
      return ClipboardContent.Uri(urlValue)
    }

    val textValue = item[UTI_PLAIN_TEXT] as? String
    if (!textValue.isNullOrEmpty()) {
      return ClipboardContent.Text(textValue)
    }
    return null
  }

  private fun buildPasteboardItem(content: ClipboardContent): Map<Any?, Any?>? {
    return when (content) {
      is ClipboardContent.Text -> mapOf(UTI_PLAIN_TEXT to content.text)
      is ClipboardContent.RichText -> buildRichTextItem(content)
      is ClipboardContent.Uri -> {
        val url = NSURL(string = content.uri) ?: return null
        mapOf(UTI_URL to url)
      }
      is ClipboardContent.Image -> buildImageItem(content)
    }
  }

  private fun buildRichTextItem(content: ClipboardContent.RichText): Map<Any?, Any?> {
    return when (content.format) {
      RichTextFormat.HTML -> {
        buildStringFallback(UTI_HTML, content.text, content.plainText)
      }
      RichTextFormat.RTF -> {
        buildStringFallback(UTI_RTF, content.text, content.plainText)
      }
      RichTextFormat.MARKDOWN,
      RichTextFormat.UNKNOWN,
      -> mapOf(UTI_PLAIN_TEXT to (content.plainText ?: content.text))
    }
  }

  private fun buildStringFallback(
    key: String,
    value: String,
    plainText: String?,
  ): Map<Any?, Any?> {
    val data = value.toUtf8Data()
    val map = mutableMapOf<Any?, Any?>(key to (data ?: value))
    if (!plainText.isNullOrEmpty()) {
      map[UTI_PLAIN_TEXT] = plainText
    }
    return map
  }

  private fun buildImageItem(content: ClipboardContent.Image): Map<Any?, Any?>? {
    val image = imageFromBytes(content.bytes) ?: return null
    val isJpeg = content.mimeType?.lowercase() == MIME_TYPE_JPEG
    val data = if (isJpeg) {
      UIImageJPEGRepresentation(image, 1.0)
    } else {
      UIImagePNGRepresentation(image)
    } ?: return null
    val key = if (isJpeg) UTI_JPEG else UTI_PNG
    return mapOf(key to data)
  }

  private inline fun <T> withLock(block: () -> T): T {
    lock.lock()
    try {
      return block()
    } finally {
      lock.unlock()
    }
  }

}

private const val UTI_PLAIN_TEXT = "public.utf8-plain-text"
private const val UTI_URL = "public.url"
private const val UTI_HTML = "public.html"
private const val UTI_RTF = "public.rtf"
private const val UTI_PNG = "public.png"
private const val UTI_JPEG = "public.jpeg"
private const val MIME_TYPE_PNG = "image/png"
private const val MIME_TYPE_JPEG = "image/jpeg"

@OptIn(ExperimentalForeignApi::class)
private fun NSData.toByteArray(): ByteArray {
  val length = this.length.toInt()
  if (length == 0) return ByteArray(0)
  val bytes = ByteArray(length)
  bytes.usePinned { pinned ->
    memcpy(pinned.addressOf(0), this.bytes, length.toULong())
  }
  return bytes
}

private fun NSData.toUtf8String(): String? {
  return toByteArray().decodeToString()
}

private fun UIImage.toPngData(): NSData? {
  return UIImagePNGRepresentation(this)
}

@OptIn(ExperimentalForeignApi::class)
private fun imageFromBytes(bytes: ByteArray): UIImage? {
  if (bytes.isEmpty()) return null
  val data = bytes.toNSData() ?: return null
  return UIImage(data = data)
}

@OptIn(ExperimentalForeignApi::class)
private fun String.toUtf8Data(): NSData? {
  return encodeToByteArray().toNSData()
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
