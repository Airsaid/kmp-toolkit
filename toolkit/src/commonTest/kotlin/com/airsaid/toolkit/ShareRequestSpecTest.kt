package com.airsaid.toolkit

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ShareRequestSpecTest {

  @Test
  fun emptyDraftHasNoSpec() {
    val draft = ShareRequestDraft<Unit>()

    assertNull(draft.toSpec())
  }

  @Test
  fun textOnlyDraftUsesPlainText() {
    val spec = ShareRequestDraft<Unit>(
      textParts = listOf("hello", "world"),
    ).toSpec()

    assertEquals(ShareSendAction.Single, spec?.action)
    assertEquals("hello\nworld", spec?.text)
    assertEquals(ShareMimeTypes.Text, spec?.mimeType)
    assertEquals(0, spec?.streamCount)
  }

  @Test
  fun sameMimeTypeStaysSpecific() {
    val spec = ShareRequestDraft(
      streams = listOf(
        ShareStream(Unit, "image/png"),
        ShareStream(Unit, "image/png"),
      ),
    ).toSpec()

    assertEquals(ShareSendAction.Multiple, spec?.action)
    assertEquals("image/png", spec?.mimeType)
    assertEquals(2, spec?.streamCount)
  }

  @Test
  fun sameTopLevelMimeTypeUsesWildcard() {
    val spec = ShareRequestDraft(
      streams = listOf(
        ShareStream(Unit, "image/png"),
        ShareStream(Unit, "image/jpeg"),
      ),
    ).toSpec()

    assertEquals("image/*", spec?.mimeType)
  }

  @Test
  fun mixedTopLevelMimeTypesUseAllWildcard() {
    val spec = ShareRequestDraft(
      streams = listOf(
        ShareStream(Unit, "image/png"),
        ShareStream(Unit, "application/pdf"),
      ),
    ).toSpec()

    assertEquals(ShareMimeTypes.All, spec?.mimeType)
  }

  @Test
  fun mixedKnownAndUnknownMimeTypesUseAllWildcard() {
    val spec = ShareRequestDraft(
      streams = listOf(
        ShareStream(Unit, "image/png"),
        ShareStream(Unit, null),
      ),
    ).toSpec()

    assertEquals(ShareMimeTypes.All, spec?.mimeType)
  }

  @Test
  fun invalidMimeTypeFallsBackToAllWildcard() {
    val spec = ShareRequestDraft(
      streams = listOf(
        ShareStream(Unit, "not-a-mime-type"),
      ),
    ).toSpec()

    assertEquals(ShareMimeTypes.All, spec?.mimeType)
  }

  @Test
  fun extensionFromMimeTypeIsFilenameSafe() {
    assertEquals("png", ShareMimeTypes.extensionFrom("image/png"))
    assertEquals("svg", ShareMimeTypes.extensionFrom("image/svg+xml"))
    assertEquals("bin", ShareMimeTypes.extensionFrom(null))
  }
}
