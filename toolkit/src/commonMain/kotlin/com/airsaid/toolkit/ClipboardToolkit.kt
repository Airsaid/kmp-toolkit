package com.airsaid.toolkit

import kotlinx.coroutines.flow.Flow

/**
 * Clipboard content representation across platforms.
 */
sealed interface ClipboardContent {

  /**
   * Plain text clipboard content.
   */
  data class Text(
    val text: String,
  ) : ClipboardContent

  /**
   * Rich text clipboard content, such as HTML or RTF.
   */
  data class RichText(
    val content: String,
    val format: RichTextFormat,
    val plainText: String? = null,
  ) : ClipboardContent

  /**
   * URI clipboard content.
   */
  data class Uri(
    val uri: String,
  ) : ClipboardContent

  /**
   * Image clipboard content reference.
   */
  data class Image(
    val id: String,
    val mimeType: String? = null,
    val sizeBytes: Long? = null,
    val uri: String? = null,
  ) : ClipboardContent
}

/**
 * Clipboard content representation used for writes.
 */
sealed interface ClipboardWriteContent {

  /**
   * Plain text clipboard content.
   */
  data class Text(
    val text: String,
  ) : ClipboardWriteContent

  /**
   * Rich text clipboard content, such as HTML or RTF.
   */
  data class RichText(
    val content: String,
    val format: RichTextFormat,
    val plainText: String? = null,
  ) : ClipboardWriteContent

  /**
   * URI clipboard content.
   */
  data class Uri(
    val uri: String,
  ) : ClipboardWriteContent

  /**
   * Image clipboard content.
   */
  data class Image(
    val bytes: ByteArray,
    val mimeType: String? = null,
  ) : ClipboardWriteContent {

    override fun equals(other: Any?): Boolean {
      if (this === other) return true
      if (other !is Image) return false
      if (mimeType != other.mimeType) return false
      return bytes.contentEquals(other.bytes)
    }

    override fun hashCode(): Int {
      var result = bytes.contentHashCode()
      result = 31 * result + (mimeType?.hashCode() ?: 0)
      return result
    }
  }
}

/**
 * Options applied when writing clipboard contents.
 */
data class ClipboardWriteOptions(
  val isSensitive: Boolean = false,
)

/**
 * Rich text formats supported by clipboard.
 */
enum class RichTextFormat {
  HTML,
  RTF,
  MARKDOWN,
  UNKNOWN,
}

/**
 * Snapshot of clipboard contents.
 */
data class ClipboardSnapshot(
  val contents: List<ClipboardContent>,
) {

  val isEmpty: Boolean
    get() = contents.isEmpty()
}

const val DEFAULT_MAX_IMAGE_BYTES: Long = 10L * 1024L * 1024L

/**
 * Provides cross-platform clipboard access.
 */
interface ClipboardToolkit {

  /**
   * Observes clipboard content changes as a [Flow].
   */
  fun observeClipboard(): Flow<ClipboardSnapshot>

  /**
   * Reads the current clipboard snapshot.
   */
  suspend fun getSnapshot(): ClipboardSnapshot

  /**
   * Reads the first available clipboard text.
   */
  suspend fun getText(): String?

  /**
   * Returns true when clipboard contains text or rich text.
   */
  suspend fun hasText(): Boolean

  /**
   * Writes contents to the clipboard.
   */
  suspend fun setContents(
    contents: List<ClipboardWriteContent>,
    options: ClipboardWriteOptions = ClipboardWriteOptions(),
  )

  /**
   * Writes plain text to the clipboard.
   */
  suspend fun setText(
    text: String,
    options: ClipboardWriteOptions = ClipboardWriteOptions(),
  )

  /**
   * Clears the clipboard content when supported.
   */
  suspend fun clear()

  /**
   * Reads image bytes for a clipboard image reference.
   */
  suspend fun readImageBytes(
    image: ClipboardContent.Image,
    maxBytes: Long = DEFAULT_MAX_IMAGE_BYTES,
  ): ByteArray?
}

/**
 * Factory object for creating [ClipboardToolkit] instances.
 */
internal expect object ClipboardToolkitFactory {

  /**
   * Creates a platform-specific [ClipboardToolkit] instance.
   */
  fun create(): ClipboardToolkit
}

internal class ClipboardSnapshotTracker(
  initialSnapshot: ClipboardSnapshot,
) {

  private var lastSnapshot: ClipboardSnapshot = initialSnapshot

  fun update(newSnapshot: ClipboardSnapshot): ClipboardSnapshot? {
    if (newSnapshot == lastSnapshot) return null
    lastSnapshot = newSnapshot
    return newSnapshot
  }
}

internal fun ClipboardSnapshot.firstTextOrNull(): String? {
  return contents.firstNotNullOfOrNull { content ->
    when (content) {
      is ClipboardContent.Text -> content.text
      is ClipboardContent.RichText -> content.plainText ?: content.content
      else -> null
    }
  }
}

internal fun ClipboardSnapshot.containsText(): Boolean {
  return contents.any { content ->
    content is ClipboardContent.Text || content is ClipboardContent.RichText
  }
}
