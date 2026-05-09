package com.airsaid.toolkit

/**
 * Cross-platform file handle returned by system pickers.
 */
expect class PlatformFile {

  val name: String
  val extension: String
  val nameWithoutExtension: String
  val path: String?
  val absolutePath: String?

  suspend fun size(): Long?
  suspend fun mimeType(): String?
  suspend fun exists(): Boolean
  suspend fun isDirectory(): Boolean
  suspend fun <T> withScopedAccess(block: suspend (PlatformFile) -> T): T
}

/**
 * Errors thrown when file access cannot be granted.
 */
class FileAccessException(
  message: String,
) : IllegalStateException(message)
