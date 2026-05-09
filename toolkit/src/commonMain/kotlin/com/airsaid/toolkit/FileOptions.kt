package com.airsaid.toolkit

/**
 * File type filters for system pickers.
 */
sealed interface PlatformFileType {

  data object Image : PlatformFileType

  data object Video : PlatformFileType

  data object ImageAndVideo : PlatformFileType

  data class File(
    val extensions: List<String> = emptyList(),
  ) : PlatformFileType
}

/**
 * File picker configuration.
 */
data class FilePickerOptions(
  val title: String? = null,
  val type: PlatformFileType = PlatformFileType.File(),
  val startLocation: PlatformFile? = null,
)

/**
 * Directory picker configuration.
 */
data class DirectoryPickerOptions(
  val title: String? = null,
  val startLocation: PlatformFile? = null,
)

/**
 * File creation configuration.
 *
 * This creates a platform file target and does not write content to it.
 */
data class FileCreateOptions(
  val suggestedName: String,
  val extension: String? = null,
  val directory: PlatformFile? = null,
  val mimeType: String? = null,
  val title: String? = null,
)

internal fun buildPlatformFileName(name: String, extension: String?): String {
  val normalized = extension?.removePrefix(".")?.trim()?.takeIf { it.isNotBlank() } ?: return name
  return if (name.endsWith(".$normalized")) name else "$name.$normalized"
}

internal fun <T> limitFileSelection(items: List<T>, maxItems: Int?): List<T> {
  return if (maxItems == null || maxItems <= 0) items else items.take(maxItems)
}
