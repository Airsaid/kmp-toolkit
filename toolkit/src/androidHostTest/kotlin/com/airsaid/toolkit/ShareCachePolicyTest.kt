package com.airsaid.toolkit

import java.io.File
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ShareCachePolicyTest {

  @Test
  fun detectsFilesInsideCacheDirectory() {
    val root = Files.createTempDirectory("share-cache-root").toFile()
    val cache = File(root, "toolkit_share").apply { mkdirs() }
    val inside = File(cache, "share.txt").apply { writeText("hello") }
    val outside = File(root, "toolkit_share_other/share.txt").apply {
      parentFile?.mkdirs()
      writeText("hello")
    }

    try {
      assertTrue(ShareCachePolicy.isWithinDirectory(inside, cache))
      assertFalse(ShareCachePolicy.isWithinDirectory(outside, cache))
    } finally {
      root.deleteRecursively()
    }
  }

  @Test
  fun deletesOnlyExpiredFiles() {
    val root = Files.createTempDirectory("share-cache-age").toFile()
    val fresh = File(root, "fresh.txt").apply {
      writeText("fresh")
      setLastModified(2_000L)
    }
    val expired = File(root, "expired.txt").apply {
      writeText("expired")
      setLastModified(500L)
    }

    try {
      assertFalse(ShareCachePolicy.shouldDelete(fresh, nowMillis = 2_500L, maxAgeMillis = 1_000L))
      assertTrue(ShareCachePolicy.shouldDelete(expired, nowMillis = 2_500L, maxAgeMillis = 1_000L))
    } finally {
      root.deleteRecursively()
    }
  }
}
