package com.airsaid.toolkit

/**
 * iOS implementation of [FileToolkit].
 */
internal class FileToolkitImpl(
  private val picker: IosFilePicker,
) : FileToolkit {

  override suspend fun pickFile(options: FilePickerOptions): PlatformFile? {
    return picker.openFile(options)
  }

  override suspend fun pickFiles(options: FilePickerOptions, maxItems: Int?): List<PlatformFile> {
    return picker.openFiles(options, maxItems)
  }

  override suspend fun pickDirectory(options: DirectoryPickerOptions): PlatformFile? {
    return picker.openDirectory(options)
  }

  override suspend fun createFile(options: FileCreateOptions): PlatformFile? {
    return picker.createFile(options)
  }
}

/**
 * iOS implementation of [FileToolkitFactory].
 */
internal actual object FileToolkitFactory {

  actual fun create(): FileToolkit {
    return FileToolkitImpl(IosFilePicker())
  }
}
