package com.airsaid.toolkit

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class FileOptionsTest {

  @Test
  fun buildsFileNameWithExtension() {
    assertEquals("document.txt", buildPlatformFileName("document", "txt"))
    assertEquals("document.txt", buildPlatformFileName("document", ".txt"))
    assertEquals("document.txt", buildPlatformFileName("document", " txt "))
    assertEquals("document.txt", buildPlatformFileName("document.txt", "txt"))
    assertEquals("document", buildPlatformFileName("document", null))
    assertEquals("document", buildPlatformFileName("document", ""))
  }

  @Test
  fun limitsFileSelectionWhenMaxItemsIsPositive() {
    val items = listOf("a", "b", "c")

    assertEquals(listOf("a", "b"), limitFileSelection(items, maxItems = 2))
    assertEquals(items, limitFileSelection(items, maxItems = null))
    assertEquals(items, limitFileSelection(items, maxItems = 0))
    assertEquals(items, limitFileSelection(items, maxItems = -1))
  }

  @Test
  fun fileSizeCanBeUnknown() {
    val unknownSize: Long? = null
    val emptyFileSize: Long? = 0L

    assertNull(unknownSize)
    assertEquals(0L, emptyFileSize)
  }
}
