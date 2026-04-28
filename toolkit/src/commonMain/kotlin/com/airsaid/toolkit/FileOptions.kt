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
 * Picker selection mode.
 */
sealed interface FilePickerMode {

  data object Single : FilePickerMode

  data class Multiple(
    val maxItems: Int? = null,
  ) : FilePickerMode
}

/**
 * File picker configuration.
 */
data class FilePickerOptions(
  val title: String? = null,
  val type: PlatformFileType = PlatformFileType.File(),
  val mode: FilePickerMode = FilePickerMode.Single,
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
 * File save configuration.
 */
data class FileSaveOptions(
  val suggestedName: String,
  val extension: String? = null,
  val directory: PlatformFile? = null,
  val mimeType: String? = null,
  val title: String? = null,
)
