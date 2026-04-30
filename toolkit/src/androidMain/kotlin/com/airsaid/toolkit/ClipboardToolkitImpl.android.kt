package com.airsaid.toolkit

import android.app.Activity
import android.app.Application
import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PersistableBundle
import android.provider.OpenableColumns
import androidx.core.content.FileProvider
import androidx.core.net.toUri
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
import java.io.ByteArrayOutputStream
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
  private var lifecycleCallback: Application.ActivityLifecycleCallbacks? = null
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

  override suspend fun setContents(
    contents: List<ClipboardWriteContent>,
    options: ClipboardWriteOptions,
  ) {
    withContext(Dispatchers.IO) {
      if (contents.isEmpty()) {
        clear()
        return@withContext
      }
      val clipData = buildClipData(contents)
      clipData.applyOptions(options)
      clipboardManager.setPrimaryClip(clipData)
    }
  }

  override suspend fun setText(
    text: String,
    options: ClipboardWriteOptions,
  ) {
    setContents(listOf(ClipboardWriteContent.Text(text)), options)
  }

  override suspend fun clear() {
    withContext(Dispatchers.IO) {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        clipboardManager.clearPrimaryClip()
      } else {
        val clipData = ClipData.newPlainText(CLIP_LABEL, "")
        clipboardManager.setPrimaryClip(clipData)
      }
    }
  }

  override suspend fun readImageBytes(
    image: ClipboardContent.Image,
    maxBytes: Long,
  ): ByteArray? {
    if (maxBytes < 0) return null
    return withContext(Dispatchers.IO) {
      if (image.sizeBytes != null && image.sizeBytes > maxBytes) return@withContext null
      val uri = image.uri?.toUri() ?: image.id.toUri()
      readBytes(uri, maxBytes)
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
    val newLifecycleCallback = object : Application.ActivityLifecycleCallbacks {
      override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit

      override fun onActivityStarted(activity: Activity) = Unit

      override fun onActivityResumed(activity: Activity) {
        updateSnapshotState()
      }

      override fun onActivityPaused(activity: Activity) = Unit

      override fun onActivityStopped(activity: Activity) = Unit

      override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit

      override fun onActivityDestroyed(activity: Activity) = Unit
    }
    listener = newListener
    lifecycleCallback = newLifecycleCallback
    clipboardManager.addPrimaryClipChangedListener(newListener)
    ActivityLifecycleRegistry.initialize(context)
    ActivityLifecycleRegistry.register(newLifecycleCallback)
    updateSnapshotState()
    isMonitoring = true
  }

  private fun stopMonitoringInternal() {
    val currentListener = listener ?: return
    clipboardManager.removePrimaryClipChangedListener(currentListener)
    lifecycleCallback?.let(ActivityLifecycleRegistry::unregister)
    listener = null
    lifecycleCallback = null
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
        return ClipboardContent.Image(
          id = uri.toString(),
          mimeType = mimeType,
          sizeBytes = querySize(uri),
          uri = uri.toString(),
        )
      }
      return ClipboardContent.Uri(uri.toString())
    }

    val htmlText = item.htmlText
    if (htmlText != null) {
      return ClipboardContent.RichText(
        content = htmlText,
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

  private fun buildClipData(contents: List<ClipboardWriteContent>): ClipData {
    val first = contents.first()
    val clipData = buildClipDataForContent(first)
    contents.drop(1).forEach { content ->
      clipData.addItem(buildClipItem(content))
    }
    return clipData
  }

  private fun buildClipDataForContent(content: ClipboardWriteContent): ClipData {
    return when (content) {
      is ClipboardWriteContent.Text ->
        ClipData.newPlainText(CLIP_LABEL, content.text)
      is ClipboardWriteContent.RichText -> buildRichTextClipData(content)
      is ClipboardWriteContent.Uri -> buildUriClipData(content)
      is ClipboardWriteContent.Image -> buildImageClipData(content)
    }
  }

  private fun buildClipItem(content: ClipboardWriteContent): ClipData.Item {
    return when (content) {
      is ClipboardWriteContent.Text -> ClipData.Item(content.text)
      is ClipboardWriteContent.RichText -> ClipData.Item(
        content.plainText ?: content.content,
        content.content,
      )
      is ClipboardWriteContent.Uri -> ClipData.Item(content.uri.toUri())
      is ClipboardWriteContent.Image -> ClipData.Item(
        requireNotNull(createImageUri(content)) {
          "Clipboard image cannot be written without a valid URI."
        }
      )
    }
  }

  private fun buildRichTextClipData(content: ClipboardWriteContent.RichText): ClipData {
    if (content.format == RichTextFormat.HTML) {
      return ClipData.newHtmlText(
        CLIP_LABEL,
        content.plainText ?: content.content,
        content.content,
      )
    }
    return ClipData.newPlainText(
      CLIP_LABEL,
      content.plainText ?: content.content,
    )
  }

  private fun buildUriClipData(content: ClipboardWriteContent.Uri): ClipData {
    val uri = content.uri.toUri()
    return ClipData.newUri(context.contentResolver, CLIP_LABEL, uri)
  }

  private fun buildImageClipData(content: ClipboardWriteContent.Image): ClipData {
    val uri = requireNotNull(createImageUri(content)) {
      "Clipboard image cannot be written without a valid URI."
    }
    return ClipData.newUri(context.contentResolver, CLIP_LABEL, uri)
  }

  private fun createImageUri(content: ClipboardWriteContent.Image): Uri? {
    return ClipboardImageStore.writeImage(context, content.bytes, content.mimeType)
  }

  private fun readBytes(uri: Uri, maxBytes: Long): ByteArray? {
    return try {
      context.contentResolver.openInputStream(uri)?.use { input ->
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        var total = 0L
        while (true) {
          val read = input.read(buffer)
          if (read == -1) break
          total += read
          if (total > maxBytes) return null
          output.write(buffer, 0, read)
        }
        output.toByteArray()
      }
    } catch (_: IOException) {
      null
    } catch (_: SecurityException) {
      null
    }
  }

  private fun querySize(uri: Uri): Long? {
    return try {
      context.contentResolver.query(
        uri,
        arrayOf(OpenableColumns.SIZE),
        null,
        null,
        null,
      )?.use { cursor ->
        if (!cursor.moveToFirst()) return@use null
        val index = cursor.getColumnIndex(OpenableColumns.SIZE)
        if (index == -1 || cursor.isNull(index)) null else cursor.getLong(index)
      }
    } catch (_: SecurityException) {
      null
    }
  }

  private fun ClipData.applyOptions(options: ClipboardWriteOptions) {
    if (!options.isSensitive) return
    description.extras = PersistableBundle().apply {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        putBoolean(ClipDescription.EXTRA_IS_SENSITIVE, true)
      } else {
        putBoolean(EXTRA_IS_SENSITIVE_LEGACY, true)
      }
    }
  }

  companion object {
    private const val CLIP_LABEL = "clipboard"
    private const val EXTRA_IS_SENSITIVE_LEGACY = "android.content.extra.IS_SENSITIVE"
  }
}

private object ClipboardImageStore {

  private const val CACHE_DIR = "toolkit_clipboard"
  private const val MAX_CACHE_FILES = 8
  private const val MAX_CACHE_AGE_MILLIS = 24L * 60L * 60L * 1000L
  private const val FILE_PREFIX = "clipboard_"

  fun writeImage(context: Context, bytes: ByteArray, mimeType: String?): Uri? {
    val extension = mimeType?.substringAfter('/')?.takeIf { it.isNotBlank() } ?: "png"
    return try {
      prune(context)
      val file = createTargetFile(context, extension)
      file.outputStream().use { it.write(bytes) }
      prune(context)
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
    return File.createTempFile(FILE_PREFIX, ".$extension", dir)
  }

  private fun prune(context: Context) {
    val dir = File(context.cacheDir, CACHE_DIR)
    val files = dir.listFiles()
      ?.filter { it.isFile && it.name.startsWith(FILE_PREFIX) }
      ?.sortedByDescending { it.lastModified() }
      ?: return
    val now = System.currentTimeMillis()
    files.forEachIndexed { index, file ->
      val isStale = now - file.lastModified() > MAX_CACHE_AGE_MILLIS
      if (isStale || index >= MAX_CACHE_FILES) {
        file.delete()
      }
    }
  }
}
