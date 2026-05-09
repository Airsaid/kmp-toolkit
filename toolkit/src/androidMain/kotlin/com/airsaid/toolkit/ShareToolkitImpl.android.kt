package com.airsaid.toolkit

import android.app.Activity
import android.app.PendingIntent
import android.content.ActivityNotFoundException
import android.content.BroadcastReceiver
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import java.io.File
import java.io.IOException
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * Android implementation of [ShareToolkit].
 */
internal class ShareToolkitImpl(
  private val context: Context,
) : ShareToolkit {

  override suspend fun share(
    contents: List<ShareContent>,
    options: ShareOptions,
  ): ShareResult {
    val draft = when (val resolved = AndroidShareContentResolver(context).resolve(contents)) {
      is AndroidShareResolveResult.Success -> resolved.draft
      is AndroidShareResolveResult.Failure -> return ShareResult.Failed(
        resolved.reason,
        resolved.cause,
      )
    }
    val spec = draft.toSpec()
      ?: return ShareResult.Failed(ShareFailureReason.EMPTY_CONTENT)
    val sendIntent = AndroidShareIntentBuilder.build(context, draft, spec)

    return suspendCancellableCoroutine { continuation ->
      val requestId = AndroidShareRequests.register(sendIntent, options.title) { result ->
        if (continuation.isActive) {
          continuation.resume(result)
        }
      }
      continuation.invokeOnCancellation {
        AndroidShareRequests.cancel(requestId)
      }

      val proxyIntent = Intent(context, ShareProxyActivity::class.java)
        .putExtra(EXTRA_REQUEST_ID, requestId)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      try {
        context.startActivity(proxyIntent)
      } catch (error: ActivityNotFoundException) {
        AndroidShareRequests.complete(
          requestId,
          ShareResult.Failed(ShareFailureReason.NO_TARGET, error),
        )
      } catch (error: SecurityException) {
        AndroidShareRequests.complete(
          requestId,
          ShareResult.Failed(ShareFailureReason.PRESENTATION_FAILED, error),
        )
      }
    }
  }
}

internal class ShareProxyActivity : Activity() {

  private var requestId: String? = null
  private var launchedChooser: Boolean = false
  private var chooserWasForeground: Boolean = false

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    requestId = intent.getStringExtra(EXTRA_REQUEST_ID)
    val id = requestId
    val request = id?.let(AndroidShareRequests::get)
    if (id == null || request == null) {
      finish()
      return
    }
    AndroidShareRequests.attachFinisher(id) {
      finish()
    }

    val callbackIntent = Intent(this, ShareTargetSelectedReceiver::class.java)
      .setPackage(packageName)
      .putExtra(EXTRA_REQUEST_ID, id)
    val callback = PendingIntent.getBroadcast(
      this,
      id.hashCode(),
      callbackIntent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE,
    )
    val chooser = Intent.createChooser(request.intent, request.title, callback.intentSender)
    try {
      launchedChooser = true
      startActivity(chooser)
    } catch (error: ActivityNotFoundException) {
      AndroidShareRequests.complete(
        id,
        ShareResult.Failed(ShareFailureReason.NO_TARGET, error),
      )
      finish()
    } catch (error: SecurityException) {
      AndroidShareRequests.complete(
        id,
        ShareResult.Failed(ShareFailureReason.PRESENTATION_FAILED, error),
      )
      finish()
    }
  }

  override fun onResume() {
    super.onResume()
    val id = requestId
    if (launchedChooser && chooserWasForeground && id != null && !AndroidShareRequests.isCompleted(id)) {
      AndroidShareRequests.complete(id, ShareResult.Cancelled)
    }
  }

  override fun onPause() {
    if (launchedChooser) {
      chooserWasForeground = true
    }
    super.onPause()
  }

  override fun onSaveInstanceState(outState: Bundle) {
    super.onSaveInstanceState(outState)
    outState.putString(EXTRA_REQUEST_ID, requestId)
    outState.putBoolean(EXTRA_CHOOSER_LAUNCHED, launchedChooser)
    outState.putBoolean(EXTRA_CHOOSER_WAS_FOREGROUND, chooserWasForeground)
  }

  override fun onRestoreInstanceState(savedInstanceState: Bundle) {
    super.onRestoreInstanceState(savedInstanceState)
    requestId = savedInstanceState.getString(EXTRA_REQUEST_ID)
    launchedChooser = savedInstanceState.getBoolean(EXTRA_CHOOSER_LAUNCHED)
    chooserWasForeground = savedInstanceState.getBoolean(EXTRA_CHOOSER_WAS_FOREGROUND)
    requestId?.let { id ->
      AndroidShareRequests.attachFinisher(id) {
        finish()
      }
    }
  }

  override fun onDestroy() {
    val id = requestId
    if (isFinishing && id != null && !AndroidShareRequests.isCompleted(id)) {
      AndroidShareRequests.complete(id, ShareResult.Cancelled)
    }
    if (id != null) {
      AndroidShareRequests.detachFinisher(id)
      AndroidShareRequests.forgetCompleted(id)
    }
    super.onDestroy()
  }
}

internal class ShareTargetSelectedReceiver : BroadcastReceiver() {
  override fun onReceive(context: Context, intent: Intent) {
    val requestId = intent.getStringExtra(EXTRA_REQUEST_ID) ?: return
    AndroidShareRequests.complete(requestId, ShareResult.Completed)
  }
}

private data class AndroidShareRequest(
  val intent: Intent,
  val title: String?,
)

private object AndroidShareRequests {
  private val requests = ConcurrentHashMap<String, AndroidShareRequest>()
  private val callbacks = ConcurrentHashMap<String, (ShareResult) -> Unit>()
  private val finishers = ConcurrentHashMap<String, () -> Unit>()
  private val completed = ConcurrentHashMap.newKeySet<String>()

  fun register(
    intent: Intent,
    title: String?,
    callback: (ShareResult) -> Unit,
  ): String {
    val id = UUID.randomUUID().toString()
    requests[id] = AndroidShareRequest(intent, title)
    callbacks[id] = callback
    return id
  }

  fun get(id: String): AndroidShareRequest? {
    return requests[id]
  }

  fun isCompleted(id: String): Boolean {
    return completed.contains(id)
  }

  fun attachFinisher(id: String, finisher: () -> Unit) {
    finishers[id] = finisher
  }

  fun detachFinisher(id: String) {
    finishers.remove(id)
  }

  fun forgetCompleted(id: String) {
    completed.remove(id)
  }

  fun complete(id: String, result: ShareResult) {
    if (!completed.add(id)) return
    requests.remove(id)
    callbacks.remove(id)?.invoke(result)
    val finisher = finishers.remove(id)
    if (finisher != null) {
      finisher()
    } else {
      forgetCompleted(id)
    }
  }

  fun cancel(id: String) {
    complete(id, ShareResult.Cancelled)
  }
}

private class AndroidShareContentResolver(
  private val context: Context,
) {

  suspend fun resolve(contents: List<ShareContent>): AndroidShareResolveResult {
    if (contents.isEmpty()) {
      return AndroidShareResolveResult.Failure(ShareFailureReason.EMPTY_CONTENT)
    }
    val textParts = mutableListOf<String>()
    val streams = mutableListOf<ShareStream<Uri>>()

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
          if (content.bytes.isEmpty()) {
            return AndroidShareResolveResult.Failure(ShareFailureReason.INVALID_CONTENT)
          }
          val uri = ShareFileStore.writeBytes(
            context = context,
            bytes = content.bytes,
            mimeType = content.mimeType,
          ) ?: return AndroidShareResolveResult.Failure(ShareFailureReason.FILE_WRITE_FAILED)
          streams += ShareStream(uri, content.mimeType)
        }
        is ShareContent.File -> {
          val stream = resolveFile(content.uri, content.mimeType)
            ?: return AndroidShareResolveResult.Failure(ShareFailureReason.FILE_NOT_FOUND)
          streams += stream
        }
        is ShareContent.PlatformFile -> {
          val stream = try {
            content.file.withScopedAccess { file ->
              if (!file.exists() || file.isDirectory()) return@withScopedAccess null
              val uri = file.uri
              val mimeType = content.mimeType ?: file.mimeType() ?: context.contentResolver.getType(uri)
              ShareStream(uri, mimeType)
            }
          } catch (error: FileAccessException) {
            return AndroidShareResolveResult.Failure(ShareFailureReason.FILE_ACCESS_DENIED, error)
          } ?: return AndroidShareResolveResult.Failure(ShareFailureReason.FILE_NOT_FOUND)
          streams += stream
        }
      }
    }

    val draft = ShareRequestDraft(textParts, streams)
    if (draft.toSpec() == null) {
      return AndroidShareResolveResult.Failure(ShareFailureReason.EMPTY_CONTENT)
    }
    return AndroidShareResolveResult.Success(draft)
  }

  private fun resolveFile(
    rawUri: String,
    mimeType: String?,
  ): ShareStream<Uri>? {
    val trimmed = rawUri.trim()
    if (trimmed.isEmpty()) return null
    val parsed = trimmed.toUri()
    return when (parsed.scheme) {
      "content" -> ShareStream(parsed, mimeType ?: context.contentResolver.getType(parsed))
      "file" -> {
        val file = File(parsed.path.orEmpty())
        val uri = ShareFileStore.ensureShareableFile(context, file, mimeType) ?: return null
        ShareStream(uri, mimeType ?: context.contentResolver.getType(uri))
      }
      else -> {
        val file = File(trimmed)
        val uri = ShareFileStore.ensureShareableFile(context, file, mimeType) ?: return null
        ShareStream(uri, mimeType ?: context.contentResolver.getType(uri))
      }
    }
  }
}

private sealed interface AndroidShareResolveResult {
  data class Success(
    val draft: ShareRequestDraft<Uri>,
  ) : AndroidShareResolveResult

  data class Failure(
    val reason: ShareFailureReason,
    val cause: Throwable? = null,
  ) : AndroidShareResolveResult
}

private object AndroidShareIntentBuilder {

  fun build(
    context: Context,
    draft: ShareRequestDraft<Uri>,
    spec: ShareRequestSpec,
  ): Intent {
    return Intent(
      if (spec.action == ShareSendAction.Multiple) {
        Intent.ACTION_SEND_MULTIPLE
      } else {
        Intent.ACTION_SEND
      },
    ).apply {
      type = spec.mimeType
      if (spec.text != null) {
        putExtra(Intent.EXTRA_TEXT, spec.text)
      }
      if (draft.streams.isNotEmpty()) {
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        clipData = buildClipData(context, draft.streams.map { it.value })
      }
      if (spec.action == ShareSendAction.Multiple) {
        putParcelableArrayListExtra(
          Intent.EXTRA_STREAM,
          ArrayList(draft.streams.map { it.value }),
        )
      } else if (draft.streams.isNotEmpty()) {
        putExtra(Intent.EXTRA_STREAM, draft.streams.first().value)
      }
    }
  }

  private fun buildClipData(context: Context, uris: List<Uri>): ClipData? {
    val first = uris.firstOrNull() ?: return null
    val clipData = ClipData.newUri(context.contentResolver, CLIP_LABEL, first)
    uris.drop(1).forEach { uri ->
      clipData.addItem(ClipData.Item(uri))
    }
    return clipData
  }
}

internal object ShareCachePolicy {
  const val MaxAgeMillis: Long = 24L * 60L * 60L * 1000L

  fun isWithinDirectory(file: File, directory: File): Boolean {
    return try {
      val directoryPath = directory.canonicalFile.path
      val filePath = file.canonicalFile.path
      filePath == directoryPath || filePath.startsWith(directoryPath + File.separator)
    } catch (_: IOException) {
      false
    }
  }

  fun shouldDelete(
    file: File,
    nowMillis: Long,
    maxAgeMillis: Long = MaxAgeMillis,
  ): Boolean {
    return nowMillis - file.lastModified() > maxAgeMillis
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
    val extension = ShareMimeTypes.extensionFrom(mimeType)
    return try {
      cleanupExpiredFiles(context)
      val file = createTargetFile(context, extension)
      file.outputStream().use { it.write(bytes) }
      buildFileUri(context, file)
    } catch (_: IOException) {
      null
    } catch (_: IllegalArgumentException) {
      null
    } catch (_: SecurityException) {
      null
    }
  }

  fun ensureShareableFile(
    context: Context,
    file: File,
    mimeType: String?,
  ): Uri? {
    return try {
      if (!file.exists() || !file.isFile || !file.canRead()) return null
      cleanupExpiredFiles(context)
      val cacheDir = cacheDir(context)
      if (ShareCachePolicy.isWithinDirectory(file, cacheDir)) {
        buildFileUri(context, file)
      } else {
        copyToCache(context, file, mimeType)
      }
    } catch (_: IOException) {
      null
    } catch (_: IllegalArgumentException) {
      null
    } catch (_: SecurityException) {
      null
    }
  }

  private fun copyToCache(
    context: Context,
    file: File,
    mimeType: String?,
  ): Uri? {
    val extension = ShareMimeTypes.extensionFrom(mimeType, file.extension.ifBlank { "bin" })
    val target = createTargetFile(context, extension)
    file.inputStream().use { input ->
      target.outputStream().use { output ->
        input.copyTo(output)
      }
    }
    return buildFileUri(context, target)
  }

  private fun cleanupExpiredFiles(context: Context) {
    val dir = cacheDir(context)
    val now = System.currentTimeMillis()
    dir.listFiles()?.forEach { file ->
      if (file.isFile && ShareCachePolicy.shouldDelete(file, now)) {
        file.delete()
      }
    }
  }

  private fun createTargetFile(context: Context, extension: String): File {
    val dir = cacheDir(context)
    if (!dir.exists()) {
      dir.mkdirs()
    }
    return File.createTempFile("share_", ".$extension", dir)
  }

  private fun cacheDir(context: Context): File {
    return File(context.cacheDir, CACHE_DIR)
  }

  private fun buildFileUri(context: Context, file: File): Uri {
    return FileProvider.getUriForFile(
      context,
      "${context.packageName}.toolkit-clipboard",
      file,
    )
  }
}

private const val CLIP_LABEL = "share"
private const val EXTRA_REQUEST_ID = "com.airsaid.toolkit.extra.SHARE_REQUEST_ID"
private const val EXTRA_CHOOSER_LAUNCHED = "com.airsaid.toolkit.extra.CHOOSER_LAUNCHED"
private const val EXTRA_CHOOSER_WAS_FOREGROUND = "com.airsaid.toolkit.extra.CHOOSER_WAS_FOREGROUND"
