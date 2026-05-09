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

  /**
   * Platform file content returned by [FileToolkit].
   */
  data class PlatformFile(
    val file: com.airsaid.toolkit.PlatformFile,
    val mimeType: String? = null,
  ) : ShareContent
}

/**
 * Result of a system share request.
 */
sealed interface ShareResult {

  /**
   * The system share sheet was presented, but the platform cannot confirm the final outcome.
   */
  data object Presented : ShareResult

  /**
   * The platform confirmed completion or target selection.
   *
   * On Android this means the user selected a target. It does not guarantee that the
   * receiving app actually sent or saved the content.
   */
  data object Completed : ShareResult

  /**
   * The platform confirmed that the user cancelled the share request.
   */
  data object Cancelled : ShareResult

  /**
   * The share request failed before it could be completed.
   */
  data class Failed(
    val reason: ShareFailureReason,
    val cause: Throwable? = null,
  ) : ShareResult
}

/**
 * Reasons a share request can fail before completion.
 */
enum class ShareFailureReason {
  EMPTY_CONTENT,
  INVALID_CONTENT,
  FILE_NOT_FOUND,
  FILE_ACCESS_DENIED,
  FILE_WRITE_FAILED,
  NO_PRESENTER,
  NO_TARGET,
  PRESENTATION_FAILED,
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
   * @return Result describing whether the share sheet was presented, completed, cancelled,
   * or failed before presentation.
   */
  suspend fun share(
    contents: List<ShareContent>,
    options: ShareOptions = ShareOptions(),
  ): ShareResult

  /**
   * Shares plain text content.
   */
  suspend fun shareText(
    text: String,
    options: ShareOptions = ShareOptions(),
  ): ShareResult {
    return share(listOf(ShareContent.Text(text)), options)
  }

  /**
   * Shares URL content.
   */
  suspend fun shareUrl(
    url: String,
    options: ShareOptions = ShareOptions(),
  ): ShareResult {
    return share(listOf(ShareContent.Url(url)), options)
  }

  /**
   * Shares image bytes content.
   */
  suspend fun shareImage(
    bytes: ByteArray,
    mimeType: String,
    options: ShareOptions = ShareOptions(),
  ): ShareResult {
    return share(listOf(ShareContent.Image(bytes, mimeType)), options)
  }

  /**
   * Shares multiple image bytes with the same mime type.
   */
  suspend fun shareImages(
    images: List<ByteArray>,
    mimeType: String,
    options: ShareOptions = ShareOptions(),
  ): ShareResult {
    return share(
      images.map { bytes -> ShareContent.Image(bytes, mimeType) },
      options,
    )
  }

  /**
   * Shares file content.
   */
  suspend fun shareFile(
    uri: String,
    mimeType: String? = null,
    options: ShareOptions = ShareOptions(),
  ): ShareResult {
    return share(listOf(ShareContent.File(uri, mimeType)), options)
  }

  /**
   * Shares a platform file returned by [FileToolkit].
   */
  suspend fun shareFile(
    file: com.airsaid.toolkit.PlatformFile,
    mimeType: String? = null,
    options: ShareOptions = ShareOptions(),
  ): ShareResult {
    return share(listOf(ShareContent.PlatformFile(file, mimeType)), options)
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
