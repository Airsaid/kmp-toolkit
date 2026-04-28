package com.airsaid.toolkit

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.net.Uri
import androidx.core.content.FileProvider
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
import java.io.File
import java.io.IOException

/**
 * Android implementation of [ClipboardToolkit].
 */
internal class ClipboardToolkitImpl(
  private val context: Context,
) : ClipboardToolkit {

  private val clipboardManager =
    context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

  private val initialSnapshot = readSnapshot()
  private val tracker = ClipboardSnapshotTracker(initialSnapshot)
  private val snapshotState = MutableStateFlow(initialSnapshot)
  private val lock = Any()
  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO.limitedParallelism(1))

  private var listener: ClipboardManager.OnPrimaryClipChangedListener? = null
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
    return withContext(Dispatchers.IO) { readSnapshot() }
  }

  override suspend fun getText(): String? {
    return withContext(Dispatchers.IO) { readSnapshot().firstTextOrNull() }
  }

  override suspend fun hasText(): Boolean {
    return withContext(Dispatchers.IO) { readSnapshot().containsText() }
  }

  override fun setContents(contents: List<ClipboardContent>) {
    if (contents.isEmpty()) {
      clear()
      return
    }
    val clipData = buildClipData(contents)
    clipboardManager.setPrimaryClip(clipData)
  }

  override fun setText(text: String) {
    setContents(listOf(ClipboardContent.Text(text)))
  }

  override fun clear() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
      clipboardManager.clearPrimaryClip()
    } else {
      val clipData = ClipData.newPlainText(CLIP_LABEL, "")
      clipboardManager.setPrimaryClip(clipData)
    }
  }

  private fun onObserverStart() {
    synchronized(lock) {
      observerCount += 1
      if (!isMonitoring) {
        startMonitoringInternal()
      }
    }
  }

  private fun onObserverStop() {
    synchronized(lock) {
      observerCount = (observerCount - 1).coerceAtLeast(0)
      if (observerCount == 0 && isMonitoring) {
        stopMonitoringInternal()
      }
    }
  }

  private fun startMonitoringInternal() {
    if (isMonitoring) return

    val newListener = ClipboardManager.OnPrimaryClipChangedListener {
      updateSnapshotState()
    }
    listener = newListener
    clipboardManager.addPrimaryClipChangedListener(newListener)
    updateSnapshotState()
    isMonitoring = true
  }

  private fun stopMonitoringInternal() {
    val currentListener = listener ?: return
    clipboardManager.removePrimaryClipChangedListener(currentListener)
    listener = null
    isMonitoring = false
  }

  private fun updateSnapshotState() {
    scope.launch {
      val snapshot = readSnapshot()
      val updated = synchronized(lock) { tracker.update(snapshot) }
      if (updated != null) {
        snapshotState.value = updated
      }
    }
  }

  private fun readSnapshot(): ClipboardSnapshot {
    if (!clipboardManager.hasPrimaryClip()) return ClipboardSnapshot(emptyList())
    val clip = clipboardManager.primaryClip ?: return ClipboardSnapshot(emptyList())
    if (clip.itemCount == 0) return ClipboardSnapshot(emptyList())
    val contents = buildList {
      for (index in 0 until clip.itemCount) {
        resolveContent(clip.getItemAt(index))?.let { add(it) }
      }
    }
    return ClipboardSnapshot(contents)
  }

  private fun resolveContent(item: ClipData.Item): ClipboardContent? {
    val uri = item.uri
    if (uri != null) {
      val mimeType = context.contentResolver.getType(uri)
      if (mimeType?.startsWith("image/") == true) {
        val bytes = readBytes(uri) ?: return ClipboardContent.Uri(uri.toString())
        return ClipboardContent.Image(bytes, mimeType)
      }
      return ClipboardContent.Uri(uri.toString())
    }

    val htmlText = item.htmlText
    if (htmlText != null) {
      return ClipboardContent.RichText(
        text = htmlText,
        format = RichTextFormat.HTML,
        plainText = item.text?.toString(),
      )
    }

    val text = item.text?.toString()
      ?: item.coerceToText(context)?.toString()
    if (!text.isNullOrEmpty()) {
      return ClipboardContent.Text(text)
    }
    return null
  }

  private fun buildClipData(contents: List<ClipboardContent>): ClipData {
    val first = contents.first()
    val clipData = buildClipDataForContent(first)
    contents.drop(1).forEach { content ->
      clipData.addItem(buildClipItem(content))
    }
    return clipData
  }

  private fun buildClipDataForContent(content: ClipboardContent): ClipData {
    return when (content) {
      is ClipboardContent.Text ->
        ClipData.newPlainText(CLIP_LABEL, content.text)
      is ClipboardContent.RichText -> buildRichTextClipData(content)
      is ClipboardContent.Uri -> buildUriClipData(content)
      is ClipboardContent.Image -> buildImageClipData(content)
    }
  }

  private fun buildClipItem(content: ClipboardContent): ClipData.Item {
    return when (content) {
      is ClipboardContent.Text -> ClipData.Item(content.text)
      is ClipboardContent.RichText -> ClipData.Item(
        content.plainText ?: content.text,
        content.text,
      )
      is ClipboardContent.Uri -> ClipData.Item(Uri.parse(content.uri))
      is ClipboardContent.Image -> ClipData.Item(
        requireNotNull(createImageUri(content)) {
          "Clipboard image cannot be written without a valid URI."
        }
      )
    }
  }

  private fun buildRichTextClipData(content: ClipboardContent.RichText): ClipData {
    if (content.format == RichTextFormat.HTML) {
      return ClipData.newHtmlText(
        CLIP_LABEL,
        content.plainText ?: content.text,
        content.text,
      )
    }
    return ClipData.newPlainText(
      CLIP_LABEL,
      content.plainText ?: content.text,
    )
  }

  private fun buildUriClipData(content: ClipboardContent.Uri): ClipData {
    val uri = Uri.parse(content.uri)
    return ClipData.newUri(context.contentResolver, CLIP_LABEL, uri)
  }

  private fun buildImageClipData(content: ClipboardContent.Image): ClipData {
    val uri = requireNotNull(createImageUri(content)) {
      "Clipboard image cannot be written without a valid URI."
    }
    return ClipData.newUri(context.contentResolver, CLIP_LABEL, uri)
  }

  private fun createImageUri(content: ClipboardContent.Image): Uri? {
    return ClipboardImageStore.writeImage(context, content.bytes, content.mimeType)
  }

  private fun readBytes(uri: Uri): ByteArray? {
    return try {
      context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
    } catch (_: IOException) {
      null
    } catch (_: SecurityException) {
      null
    }
  }

  companion object {
    private const val CLIP_LABEL = "clipboard"
  }
}

private object ClipboardImageStore {

  private const val CACHE_DIR = "toolkit_clipboard"

  fun writeImage(context: Context, bytes: ByteArray, mimeType: String?): Uri? {
    val extension = mimeType?.substringAfter('/')?.takeIf { it.isNotBlank() } ?: "png"
    val file = createTargetFile(context, extension)
    return try {
      file.outputStream().use { it.write(bytes) }
      FileProvider.getUriForFile(
        context,
        "${context.packageName}.toolkit-clipboard",
        file,
      )
    } catch (_: IOException) {
      null
    }
  }

  private fun createTargetFile(context: Context, extension: String): File {
    val dir = File(context.cacheDir, CACHE_DIR)
    if (!dir.exists()) {
      dir.mkdirs()
    }
    val fileName = "clipboard_${System.currentTimeMillis()}.$extension"
    return File(dir, fileName)
  }
}
