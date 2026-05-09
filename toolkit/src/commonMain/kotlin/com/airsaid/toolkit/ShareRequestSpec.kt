package com.airsaid.toolkit

internal data class ShareStream<T>(
  val value: T,
  val mimeType: String?,
)

internal data class ShareRequestDraft<T>(
  val textParts: List<String> = emptyList(),
  val streams: List<ShareStream<T>> = emptyList(),
) {
  fun toSpec(): ShareRequestSpec? {
    val hasText = textParts.isNotEmpty()
    val hasStreams = streams.isNotEmpty()
    if (!hasText && !hasStreams) return null
    return ShareRequestSpec(
      action = if (streams.size > 1) ShareSendAction.Multiple else ShareSendAction.Single,
      text = textParts.joinToString("\n").takeIf { it.isNotEmpty() },
      mimeType = if (hasStreams) {
        ShareMimeTypes.resolve(streams.map { it.mimeType }) ?: ShareMimeTypes.All
      } else {
        ShareMimeTypes.Text
      },
      streamCount = streams.size,
    )
  }
}

internal data class ShareRequestSpec(
  val action: ShareSendAction,
  val text: String?,
  val mimeType: String,
  val streamCount: Int,
)

internal enum class ShareSendAction {
  Single,
  Multiple,
}

internal object ShareMimeTypes {
  const val Text = "text/plain"
  const val All = "*/*"

  fun resolve(mimeTypes: List<String?>): String? {
    val types = mimeTypes.mapNotNull { it.normalizedMimeType() }
    if (types.isEmpty()) return null
    if (types.size != mimeTypes.size) return All
    val first = types.first()
    if (types.all { it.equals(first, ignoreCase = true) }) return first
    val firstTopLevel = first.substringBefore('/', missingDelimiterValue = "")
    if (firstTopLevel.isBlank()) return All
    return if (types.all { it.substringBefore('/', missingDelimiterValue = "") == firstTopLevel }) {
      "$firstTopLevel/*"
    } else {
      All
    }
  }

  fun extensionFrom(mimeType: String?, fallback: String = "bin"): String {
    val rawExtension = mimeType.normalizedMimeType()
      ?.substringAfter('/', missingDelimiterValue = "")
      ?.substringBefore('+')
      ?.takeIf { it.isNotBlank() }
      ?: fallback
    return rawExtension.filter { it.isLetterOrDigit() }.ifBlank { fallback }
  }
}

private fun String?.normalizedMimeType(): String? {
  val value = this?.trim()?.lowercase()?.substringBefore(';') ?: return null
  if (value.isBlank()) return null
  val slashIndex = value.indexOf('/')
  if (slashIndex <= 0 || slashIndex == value.lastIndex) return null
  return value
}
