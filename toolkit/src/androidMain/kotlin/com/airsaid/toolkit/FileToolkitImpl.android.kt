package com.airsaid.toolkit

import android.content.Context

/**
 * Android implementation of [FileToolkit].
 */
internal class FileToolkitImpl(
  private val picker: AndroidFilePicker,
) : FileToolkit {

  override suspend fun pickFile(options: FilePickerOptions): PlatformFile? {
    return picker.openFile(options)
  }

  override suspend fun pickFiles(options: FilePickerOptions): List<PlatformFile> {
    return picker.openFiles(options)
  }

  override suspend fun pickDirectory(options: DirectoryPickerOptions): PlatformFile? {
    return picker.openDirectory(options)
  }

  override suspend fun saveFile(options: FileSaveOptions): PlatformFile? {
    return picker.saveFile(options)
  }
}

/**
 * Android implementation of [FileToolkitFactory].
 */
internal actual object FileToolkitFactory {

  private var applicationContext: Context? = null

  internal fun initialize(context: Context) {
    val appContext = context.applicationContext
    ActivityLifecycleRegistry.initialize(appContext)
    applicationContext = appContext
  }

  actual fun create(): FileToolkit {
    val context = applicationContext
      ?: throw IllegalStateException(
        "FileToolkitFactory must be initialized with Context on Android. " +
            "Call Toolkit.initialize(ToolkitInitializer(context)) first."
      )
    val picker = AndroidFilePicker(
      activityProvider = { ActivityLifecycleRegistry.getCurrentActivity() },
      context = context,
    )
    return FileToolkitImpl(picker)
  }
}
