package com.airsaid.toolkit

import android.content.Context
import java.io.File
import java.io.FileOutputStream

/**
 * Android implementation of [AppDirectoryProviderFactory].
 */
internal actual object AppDirectoryProviderFactory {

  private var applicationContext: Context? = null

  internal fun initialize(context: Context) {
    applicationContext = context.applicationContext
  }

  actual fun create(): AppDirectoryProvider {
    val context = applicationContext
      ?: throw IllegalStateException(
        "AppDirectoryProviderFactory must be initialized with Context on Android. " +
            "Call Toolkit.initialize(context) first."
      )
    return AndroidAppDirectoryProvider(context)
  }
}

private class AndroidAppDirectoryProvider(
  private val context: Context,
) : AppDirectoryProvider {

  override fun getDirectories(): AppDirectories {
    return AppDirectories(
      cacheDir = context.cacheDir.absolutePath,
      documentsDir = context.filesDir.absolutePath,
    )
  }

  override fun resolvePath(
    kind: AppDirectoryKind,
    relativePath: String,
  ): String {
    val root = getDirectories().pathOf(kind)
    val normalized = relativePath.normalizeRelativePathOrNull()
      ?: throw IllegalArgumentException("relativePath must stay inside the app directory.")
    return File(root, normalized).absolutePath
  }

  override fun createDirectory(
    kind: AppDirectoryKind,
    relativePath: String,
  ): Boolean {
    val path = resolvePathOrNull(kind, relativePath) ?: return false
    val file = File(path)
    return when {
      file.isDirectory -> true
      file.exists() -> false
      else -> file.mkdirs()
    }
  }

  override fun copyAsset(
    assetName: String,
    targetDirectory: AppDirectoryKind,
    targetRelativePath: String,
  ): Boolean {
    val safeAssetName = assetName.normalizeRelativePathOrNull()
      ?.takeIf { it.isNotEmpty() }
      ?: return false
    val root = resolvePathOrNull(targetDirectory, targetRelativePath) ?: return false
    val parent = File(root)
    if (!parent.isDirectory && !parent.mkdirs()) return false
    val target = File(parent, safeAssetName.substringAfterLast('/'))
    return try {
      context.assets.open(safeAssetName).use { input ->
        FileOutputStream(target).use { output ->
          input.copyTo(output)
        }
      }
      true
    } catch (e: Exception) {
      false
    }
  }

  private fun resolvePathOrNull(
    kind: AppDirectoryKind,
    relativePath: String,
  ): String? {
    val root = getDirectories().pathOf(kind)
    val normalized = relativePath.normalizeRelativePathOrNull() ?: return null
    return File(root, normalized).absolutePath
  }
}

private fun String.normalizeRelativePathOrNull(): String? {
  val trimmed = trim().trim('/', '\\')
  if (trimmed.isEmpty() || trimmed == ".") return ""
  val parts = trimmed
    .split('/', '\\')
    .filter { part -> part.isNotEmpty() && part != "." }
  if (parts.any { part -> part == ".." }) return null
  return parts.joinToString(File.separator)
}
