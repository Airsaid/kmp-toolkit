package com.airsaid.toolkit

/**
 * Well-known app-owned directory roots.
 */
enum class AppDirectoryKind {
  /** Small temporary files private to the app. */
  CACHE,

  /** Durable user/app files private to the app. */
  DOCUMENTS
}

/**
 * Snapshot of app-owned directory paths.
 */
data class AppDirectories(
  val cacheDir: String,
  val documentsDir: String,
) {
  fun pathOf(kind: AppDirectoryKind): String {
    return when (kind) {
      AppDirectoryKind.CACHE -> cacheDir
      AppDirectoryKind.DOCUMENTS -> documentsDir
    }
  }
}

/**
 * Provides app-owned directory and asset-copy helpers.
 */
interface AppDirectoryProvider {

  /**
   * Returns a snapshot of app-owned directory roots.
   */
  fun getDirectories(): AppDirectories

  /**
   * Resolves [relativePath] under [kind].
   */
  fun resolvePath(
    kind: AppDirectoryKind,
    relativePath: String = "",
  ): String

  /**
   * Creates [relativePath] under [kind].
   */
  fun createDirectory(
    kind: AppDirectoryKind,
    relativePath: String = "",
  ): Boolean

  /**
   * Copies a bundled app asset/resource into an app-owned directory.
   */
  fun copyAsset(
    assetName: String,
    targetDirectory: AppDirectoryKind = AppDirectoryKind.DOCUMENTS,
    targetRelativePath: String = "",
  ): Boolean
}

internal expect object AppDirectoryProviderFactory {
  fun create(): AppDirectoryProvider
}
