@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.airsaid.toolkit

import platform.Foundation.NSCachesDirectory
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSBundle
import platform.Foundation.NSFileManager
import platform.Foundation.NSHomeDirectory
import platform.Foundation.NSUserDomainMask
import platform.Foundation.NSURL

internal actual object AppDirectoryProviderFactory {

  actual fun create(): AppDirectoryProvider {
    return IosAppDirectoryProvider
  }
}

private object IosAppDirectoryProvider : AppDirectoryProvider {

  override fun getDirectories(): AppDirectories {
    return AppDirectories(
      cacheDir = searchPath(NSCachesDirectory),
      documentsDir = searchPath(NSDocumentDirectory),
    )
  }

  override fun resolvePath(
    kind: AppDirectoryKind,
    relativePath: String,
  ): String {
    val root = getDirectories().pathOf(kind)
    val normalized = relativePath.normalizeRelativePathOrNull()
      ?: throw IllegalArgumentException("relativePath must stay inside the app directory.")
    return if (normalized.isEmpty()) root else "$root/$normalized"
  }

  override fun createDirectory(
    kind: AppDirectoryKind,
    relativePath: String,
  ): Boolean {
    val path = resolvePathOrNull(kind, relativePath) ?: return false
    return NSFileManager.defaultManager.createDirectoryAtPath(
      path = path,
      withIntermediateDirectories = true,
      attributes = null,
      error = null,
    )
  }

  override fun copyAsset(
    assetName: String,
    targetDirectory: AppDirectoryKind,
    targetRelativePath: String,
  ): Boolean {
    val safeAssetName = assetName.normalizeRelativePathOrNull()
      ?.takeIf { it.isNotEmpty() }
      ?: return false
    val bundleRoot = NSBundle.mainBundle.resourcePath ?: return false
    val sourcePath = "$bundleRoot/$safeAssetName"
    if (!NSFileManager.defaultManager.fileExistsAtPath(sourcePath)) {
      return false
    }
    val root = resolvePathOrNull(targetDirectory, targetRelativePath) ?: return false
    if (!createDirectory(targetDirectory, targetRelativePath)) {
      return false
    }
    val targetPath = "$root/${safeAssetName.substringAfterLast('/')}"
    return try {
      if (NSFileManager.defaultManager.fileExistsAtPath(targetPath)) {
        NSFileManager.defaultManager.removeItemAtPath(targetPath, error = null)
      }
      NSFileManager.defaultManager.copyItemAtPath(
        srcPath = sourcePath,
        toPath = targetPath,
        error = null,
      )
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
    return if (normalized.isEmpty()) root else "$root/$normalized"
  }
}

private fun String.normalizeRelativePathOrNull(): String? {
  val trimmed = trim().trim('/', '\\')
  if (trimmed.isEmpty() || trimmed == ".") return ""
  val parts = trimmed
    .split('/', '\\')
    .filter { part -> part.isNotEmpty() && part != "." }
  if (parts.any { part -> part == ".." }) return null
  return parts.joinToString("/")
}

private fun searchPath(directory: ULong): String {
  return NSFileManager.defaultManager
    .URLsForDirectory(directory, NSUserDomainMask)
    .firstOrNull()
    ?.let { url -> (url as? NSURL)?.path }
    ?: fallbackPath(directory)
}

private fun fallbackPath(directory: ULong): String {
  val home = NSHomeDirectory()
  return when (directory) {
    NSCachesDirectory -> "$home/Library/Caches"
    NSDocumentDirectory -> "$home/Documents"
    else -> home
  }
}
