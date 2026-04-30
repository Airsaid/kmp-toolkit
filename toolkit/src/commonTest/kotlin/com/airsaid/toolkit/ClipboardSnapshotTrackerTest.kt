package com.airsaid.toolkit

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ClipboardSnapshotTrackerTest {

  @Test
  fun updateReturnsNullWhenSnapshotUnchanged() {
    val snapshot = ClipboardSnapshot(
      contents = listOf(ClipboardContent.Text("hello")),
    )
    val tracker = ClipboardSnapshotTracker(snapshot)

    val result = tracker.update(snapshot)

    assertNull(result)
  }

  @Test
  fun updateReturnsSnapshotWhenChanged() {
    val snapshot = ClipboardSnapshot(
      contents = listOf(ClipboardContent.Text("hello")),
    )
    val tracker = ClipboardSnapshotTracker(snapshot)
    val updated = ClipboardSnapshot(
      contents = listOf(ClipboardContent.Text("world")),
    )

    val result = tracker.update(updated)

    assertEquals(updated, result)
  }

  @Test
  fun updateReturnsNullWhenImageContentUnchanged() {
    val snapshot = ClipboardSnapshot(
      contents = listOf(
        ClipboardContent.Image(
          id = "image-1",
          mimeType = "image/png",
          sizeBytes = 3,
          uri = "content://example/image-1",
        ),
      ),
    )
    val tracker = ClipboardSnapshotTracker(snapshot)
    val updated = ClipboardSnapshot(
      contents = listOf(
        ClipboardContent.Image(
          id = "image-1",
          mimeType = "image/png",
          sizeBytes = 3,
          uri = "content://example/image-1",
        ),
      ),
    )

    val result = tracker.update(updated)

    assertNull(result)
  }

  @Test
  fun firstTextOrNullPrefersPlainText() {
    val snapshot = ClipboardSnapshot(
      contents = listOf(
        ClipboardContent.RichText(
          content = "<b>Hello</b>",
          format = RichTextFormat.HTML,
          plainText = "Hello",
        ),
        ClipboardContent.Text("fallback"),
      ),
    )

    assertEquals("Hello", snapshot.firstTextOrNull())
  }

  @Test
  fun containsTextMatchesTextAndRichText() {
    val textSnapshot = ClipboardSnapshot(
      contents = listOf(ClipboardContent.Text("hi")),
    )
    val richSnapshot = ClipboardSnapshot(
      contents = listOf(
        ClipboardContent.RichText(
          content = "# hi",
          format = RichTextFormat.MARKDOWN,
        ),
      ),
    )
    val imageSnapshot = ClipboardSnapshot(
      contents = listOf(
        ClipboardContent.Image(
          id = "image-1",
          mimeType = "image/png",
          sizeBytes = 1,
        ),
      ),
    )

    assertTrue(textSnapshot.containsText())
    assertTrue(richSnapshot.containsText())
    assertFalse(imageSnapshot.containsText())
  }

  @Test
  fun imageReferenceDoesNotStoreBytes() {
    val image = ClipboardContent.Image(
      id = "image-1",
      mimeType = "image/png",
      sizeBytes = 1024,
      uri = "content://example/image-1",
    )

    assertEquals("image-1", image.id)
    assertEquals("image/png", image.mimeType)
    assertEquals(1024, image.sizeBytes)
    assertEquals("content://example/image-1", image.uri)
  }

  @Test
  fun writeImageContentStoresBytesForExplicitWrites() {
    val bytes = byteArrayOf(1, 2, 3)
    val image = ClipboardWriteContent.Image(bytes, "image/png")

    assertTrue(bytes.contentEquals(image.bytes))
    assertEquals("image/png", image.mimeType)
  }

  @Test
  fun defaultWriteOptionsAreNotSensitive() {
    assertFalse(ClipboardWriteOptions().isSensitive)
  }
}
