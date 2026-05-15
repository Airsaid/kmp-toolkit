package com.airsaid.toolkit

import kotlin.test.Test
import kotlin.test.assertEquals

class AppDirectoriesTest {

  @Test
  fun appDirectoriesResolvePathByType() {
    val directories = AppDirectories(
      cacheDir = "/cache",
      documentsDir = "/documents",
    )

    assertEquals("/cache", directories.pathOf(AppDirectoryKind.CACHE))
    assertEquals("/documents", directories.pathOf(AppDirectoryKind.DOCUMENTS))
  }

  @Test
  fun appDirectoryKindsStayLimitedToCrossPlatformRoots() {
    assertEquals(
      listOf(AppDirectoryKind.CACHE, AppDirectoryKind.DOCUMENTS),
      AppDirectoryKind.entries,
    )
  }
}
