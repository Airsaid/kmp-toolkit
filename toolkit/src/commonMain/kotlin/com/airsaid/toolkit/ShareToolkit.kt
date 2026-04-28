package com.airsaid.toolkit

/**
 * Shareable content description.
 */
sealed interface ShareContent {

  /**
   * Plain text content.
   */
  data class Text(
    val text: String,
  ) : ShareContent

  /**
   * URL content.
   */
  data class Url(
    val url: String,
  ) : ShareContent

  /**
   * Image bytes content.
   */
  data class Image(
    val bytes: ByteArray,
    val mimeType: String,
  ) : ShareContent

  /**
   * File content described by URI string.
   */
  data class File(
    val uri: String,
    val mimeType: String? = null,
  ) : ShareContent
}

/**
 * Excluded activity types for share sheets.
 */
enum class ShareExcludedActivity {
  AIR_DROP,
  COPY_TO_PASTEBOARD,
  MESSAGE,
  MAIL,
  SAVE_TO_CAMERA_ROLL,
}

/**
 * Share options.
 *
 * @property title Optional chooser title (Android).
 * @property excludedActivities Excluded activity types (iOS).
 */
data class ShareOptions(
  val title: String? = null,
  val excludedActivities: List<ShareExcludedActivity> = emptyList(),
)

/**
 * Provides cross-platform sharing capabilities.
 */
interface ShareToolkit {

  /**
   * Presents system share sheet when supported.
   *
   * @return True if the request was handled.
   */
  fun share(
    contents: List<ShareContent>,
    options: ShareOptions = ShareOptions(),
  ): Boolean

  /**
   * Shares plain text content.
   */
  fun shareText(
    text: String,
    options: ShareOptions = ShareOptions(),
  ): Boolean {
    return share(listOf(ShareContent.Text(text)), options)
  }

  /**
   * Shares URL content.
   */
  fun shareUrl(
    url: String,
    options: ShareOptions = ShareOptions(),
  ): Boolean {
    return share(listOf(ShareContent.Url(url)), options)
  }

  /**
   * Shares image bytes content.
   */
  fun shareImage(
    bytes: ByteArray,
    mimeType: String,
    options: ShareOptions = ShareOptions(),
  ): Boolean {
    return share(listOf(ShareContent.Image(bytes, mimeType)), options)
  }

  /**
   * Shares multiple image bytes with the same mime type.
   */
  fun shareImages(
    images: List<ByteArray>,
    mimeType: String,
    options: ShareOptions = ShareOptions(),
  ): Boolean {
    return share(
      images.map { bytes -> ShareContent.Image(bytes, mimeType) },
      options,
    )
  }

  /**
   * Shares file content.
   */
  fun shareFile(
    uri: String,
    mimeType: String? = null,
    options: ShareOptions = ShareOptions(),
  ): Boolean {
    return share(listOf(ShareContent.File(uri, mimeType)), options)
  }
}

/**
 * Factory object for creating [ShareToolkit] instances.
 */
internal expect object ShareToolkitFactory {

  /**
   * Creates a platform-specific [ShareToolkit] instance.
   */
  fun create(): ShareToolkit
}
