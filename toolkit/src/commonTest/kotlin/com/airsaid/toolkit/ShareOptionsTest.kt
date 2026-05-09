package com.airsaid.toolkit

import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class ShareOptionsTest {

  @Test
  fun defaultsAreEmpty() {
    val options = ShareOptions()

    assertEquals(null, options.title)
    assertTrue(options.excludedActivities.isEmpty())
  }

  @Test
  fun failedResultCarriesReasonAndCause() {
    val cause = IllegalStateException("missing")
    val result = ShareResult.Failed(ShareFailureReason.FILE_NOT_FOUND, cause)

    assertEquals(ShareFailureReason.FILE_NOT_FOUND, result.reason)
    assertEquals(cause, result.cause)
  }

  @Test
  fun shareConvenienceMethodsBuildExpectedContent() {
    val toolkit = RecordingShareToolkit()

    toolkit.shareTextForTest("hello")
    assertEquals(listOf(ShareContent.Text("hello")), toolkit.lastContents)

    toolkit.shareUrlForTest("https://example.com")
    assertEquals(listOf(ShareContent.Url("https://example.com")), toolkit.lastContents)

    toolkit.shareFileForTest("/tmp/file.txt", "text/plain")
    assertEquals(listOf(ShareContent.File("/tmp/file.txt", "text/plain")), toolkit.lastContents)
  }

  @Test
  fun shareResultObjectsAreAvailable() {
    assertIs<ShareResult.Presented>(ShareResult.Presented)
    assertIs<ShareResult.Completed>(ShareResult.Completed)
    assertIs<ShareResult.Cancelled>(ShareResult.Cancelled)
  }

  private class RecordingShareToolkit : ShareToolkit {
    var lastContents: List<ShareContent> = emptyList()

    override suspend fun share(
      contents: List<ShareContent>,
      options: ShareOptions,
    ): ShareResult {
      lastContents = contents
      return ShareResult.Completed
    }

    fun shareTextForTest(text: String) = runBlocking {
      shareText(text)
    }

    fun shareUrlForTest(url: String) = runBlocking {
      shareUrl(url)
    }

    fun shareFileForTest(uri: String, mimeType: String?) = runBlocking {
      shareFile(uri, mimeType)
    }
  }
}
