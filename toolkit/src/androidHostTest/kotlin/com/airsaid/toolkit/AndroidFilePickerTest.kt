package com.airsaid.toolkit

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AndroidFilePickerTest {

  @Test
  fun resolvesMimeTypesFromExtensions() {
    val resolver: (String) -> String? = { extension ->
      when (extension.removePrefix(".").trim().lowercase()) {
        "pdf" -> "application/pdf"
        "txt" -> "text/plain"
        else -> null
      }
    }

    assertEquals(listOf("application/pdf"), PlatformFileType.File(listOf("pdf")).resolveMimeTypes(resolver))
    assertEquals(listOf("application/pdf", "text/plain"), PlatformFileType.File(listOf(".pdf", "txt")).resolveMimeTypes(resolver))
    assertEquals(listOf("image/*"), PlatformFileType.Image.resolveMimeTypes(resolver))
    assertEquals(listOf("video/*"), PlatformFileType.Video.resolveMimeTypes(resolver))
    assertEquals(listOf("image/*", "video/*"), PlatformFileType.ImageAndVideo.resolveMimeTypes(resolver))
    assertEquals(listOf("*/*"), PlatformFileType.File(listOf("unknown-ext")).resolveMimeTypes(resolver))
  }

  @Test
  fun buildsSingleOpenDocumentIntentSpec() {
    val spec = OpenDocumentParams(
      mimeTypes = listOf("application/pdf", "text/plain"),
      title = "Pick document",
      initialUri = null,
    ).toOpenDocumentIntentSpec(allowMultiple = false)

    assertEquals("android.intent.action.OPEN_DOCUMENT", spec.action)
    assertEquals("application/pdf", spec.type)
    assertEquals(false, spec.allowMultiple)
    assertEquals("Pick document", spec.title)
    assertEquals(
      listOf("application/pdf", "text/plain"),
      spec.mimeTypes,
    )
  }

  @Test
  fun buildsMultipleOpenDocumentIntentSpec() {
    val spec = OpenDocumentParams(
      mimeTypes = listOf("image/*"),
      title = null,
      initialUri = null,
    ).toOpenDocumentIntentSpec(allowMultiple = true)

    assertEquals(true, spec.allowMultiple)
  }

  @Test
  fun buildsCreateDocumentIntentSpec() {
    val spec = CreateDocumentParams(
      fileName = "document.txt",
      title = null,
      initialUri = null,
      mimeType = "text/plain",
    ).toCreateDocumentIntentSpec()

    assertEquals("android.intent.action.CREATE_DOCUMENT", spec.action)
    assertEquals("text/plain", spec.type)
    assertEquals("document.txt", spec.title)
    assertNull(spec.initialUri)
  }

  @Test
  fun fallsBackToWildcardCreateDocumentType() {
    val spec = CreateDocumentParams(
      fileName = "document",
      title = null,
      initialUri = null,
      mimeType = null,
    ).toCreateDocumentIntentSpec()

    assertEquals("*/*", spec.type)
  }

  @Test
  fun detectsDocumentFileUriType() {
    assertEquals(DocumentFileUriType.TREE, resolveDocumentFileUriType(listOf("tree", "primary:folder")))
    assertEquals(DocumentFileUriType.SINGLE, resolveDocumentFileUriType(listOf("document", "primary:file.txt")))
  }
}
