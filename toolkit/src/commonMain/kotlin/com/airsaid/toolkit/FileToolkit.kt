package com.airsaid.toolkit

/**
 * Provides cross-platform access to system file pickers.
 */
interface FileToolkit {

  suspend fun pickFile(
    options: FilePickerOptions = FilePickerOptions(),
  ): PlatformFile?

  /**
   * Opens a multiple-file picker.
   *
   * @param maxItems Maximum returned items. `null` or non-positive values do not limit selection.
   */
  suspend fun pickFiles(
    options: FilePickerOptions = FilePickerOptions(),
    maxItems: Int? = null,
  ): List<PlatformFile>

  suspend fun pickDirectory(
    options: DirectoryPickerOptions = DirectoryPickerOptions(),
  ): PlatformFile?

  /**
   * Creates or selects a writable file target without writing content to it.
   */
  suspend fun createFile(
    options: FileCreateOptions,
  ): PlatformFile?
}

/**
 * Factory object for creating [FileToolkit] instances.
 */
internal expect object FileToolkitFactory {

  /**
   * Creates a platform-specific [FileToolkit] instance.
   */
  fun create(): FileToolkit
}
