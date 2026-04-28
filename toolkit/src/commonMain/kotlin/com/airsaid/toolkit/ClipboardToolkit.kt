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
    val text: String,
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
   * Image clipboard content.
   */
  class Image(
    val bytes: ByteArray,
    val mimeType: String? = null,
  ) : ClipboardContent {

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
  fun setContents(contents: List<ClipboardContent>)

  /**
   * Writes plain text to the clipboard.
   */
  fun setText(text: String)

  /**
   * Clears the clipboard content when supported.
   */
  fun clear()
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

  private var lastSignature: Int = initialSnapshot.signature()

  fun update(newSnapshot: ClipboardSnapshot): ClipboardSnapshot? {
    val signature = newSnapshot.signature()
    if (signature == lastSignature) return null
    lastSignature = signature
    return newSnapshot
  }
}

internal fun ClipboardSnapshot.signature(): Int {
  var result = 1
  contents.forEach { content ->
    result = 31 * result + content.signature()
  }
  return result
}

internal fun ClipboardContent.signature(): Int {
  return when (this) {
    is ClipboardContent.Text -> text.hashCode()
    is ClipboardContent.RichText -> {
      var result = format.hashCode()
      result = 31 * result + text.hashCode()
      result = 31 * result + (plainText?.hashCode() ?: 0)
      result
    }
    is ClipboardContent.Uri -> uri.hashCode()
    is ClipboardContent.Image -> hashCode()
  }
}

internal fun ClipboardSnapshot.firstTextOrNull(): String? {
  return contents.firstNotNullOfOrNull { content ->
    when (content) {
      is ClipboardContent.Text -> content.text
      is ClipboardContent.RichText -> content.plainText ?: content.text
      else -> null
    }
  }
}

internal fun ClipboardSnapshot.containsText(): Boolean {
  return contents.any { content ->
    content is ClipboardContent.Text || content is ClipboardContent.RichText
  }
}
