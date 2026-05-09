package com.airsaid.toolkit

import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSFileManager
import platform.Foundation.NSFileSize
import platform.Foundation.NSURL
import platform.Foundation.pathExtension
import platform.UniformTypeIdentifiers.UTType

/**
 * iOS implementation of [PlatformFile].
 */
actual class PlatformFile internal constructor(
  internal val url: NSURL,
) {

  actual val name: String
    get() = url.lastPathComponent ?: ""

  actual val extension: String
    get() = url.pathExtension ?: ""

  actual val nameWithoutExtension: String
    get() = if (extension.isBlank()) name else name.removeSuffix(".$extension")

  actual val path: String?
    get() = url.path

  actual val absolutePath: String?
    get() = url.path

  @OptIn(ExperimentalForeignApi::class)
  actual suspend fun size(): Long? {
    val pathValue = url.path ?: return null
    val attrs = NSFileManager.defaultManager.attributesOfItemAtPath(pathValue, null)
    val size = attrs?.get(NSFileSize) as? Number ?: return null
    return size.toLong()
  }

  actual suspend fun mimeType(): String? {
    val fileExtension = extension.takeIf { it.isNotBlank() } ?: return null
    return UTType.typeWithFilenameExtension(fileExtension)?.preferredMIMEType
  }

  @OptIn(ExperimentalForeignApi::class)
  actual suspend fun exists(): Boolean {
    val pathValue = url.path ?: return false
    return NSFileManager.defaultManager.fileExistsAtPath(pathValue)
  }

  actual suspend fun isDirectory(): Boolean {
    return url.hasDirectoryPath
  }

  actual suspend fun <T> withScopedAccess(block: suspend (PlatformFile) -> T): T {
    val ok = url.startAccessingSecurityScopedResource()
    if (!ok) {
      throw FileAccessException("Failed to access security scoped resource.")
    }
    return try {
      block(this)
    } finally {
      url.stopAccessingSecurityScopedResource()
    }
  }
}
