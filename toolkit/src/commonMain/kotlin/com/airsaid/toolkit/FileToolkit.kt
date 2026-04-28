package com.airsaid.toolkit

/**
 * Provides cross-platform access to system file pickers.
 */
interface FileToolkit {

  suspend fun pickFile(
    options: FilePickerOptions = FilePickerOptions(),
  ): PlatformFile?

  suspend fun pickFiles(
    options: FilePickerOptions = FilePickerOptions(),
  ): List<PlatformFile>

  suspend fun pickDirectory(
    options: DirectoryPickerOptions = DirectoryPickerOptions(),
  ): PlatformFile?

  suspend fun saveFile(
    options: FileSaveOptions,
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
