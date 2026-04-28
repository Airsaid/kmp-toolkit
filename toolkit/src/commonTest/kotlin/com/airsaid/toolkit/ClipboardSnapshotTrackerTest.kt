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
        ClipboardContent.Image(byteArrayOf(1, 2, 3), "image/png"),
      ),
    )
    val tracker = ClipboardSnapshotTracker(snapshot)
    val updated = ClipboardSnapshot(
      contents = listOf(
        ClipboardContent.Image(byteArrayOf(1, 2, 3), "image/png"),
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
          text = "<b>Hello</b>",
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
          text = "# hi",
          format = RichTextFormat.MARKDOWN,
        ),
      ),
    )
    val imageSnapshot = ClipboardSnapshot(
      contents = listOf(
        ClipboardContent.Image(byteArrayOf(1), "image/png"),
      ),
    )

    assertTrue(textSnapshot.containsText())
    assertTrue(richSnapshot.containsText())
    assertFalse(imageSnapshot.containsText())
  }
}
