package com.airsaid.toolkit

/**
 * Represents locale information for the current system.
 *
 * @property languageTag Complete BCP 47 language tag.
 * @property languageCode ISO 639 language code, or `und` when unavailable.
 * @property scriptCode ISO 15924 script code when present.
 * @property regionCode ISO 3166 region code or UN M49 numeric area when present.
 * @property variant BCP 47 variant subtags when present.
 */
data class LocaleInfo(
  val languageTag: String,
  val languageCode: String,
  val scriptCode: String? = null,
  val regionCode: String? = null,
  val variant: String? = null,
)

/**
 * Represents current and preferred locale settings.
 *
 * @property current Current system locale info.
 * @property preferred Preferred locale list ordered by system priority.
 */
data class LocaleBundle(
  val current: LocaleInfo,
  val preferred: List<LocaleInfo>,
)

/**
 * Represents device manufacturer and brand details.
 *
 * @property manufacturer Device manufacturer name.
 * @property brand Device brand name.
 */
data class ManufacturerInfo(
  val manufacturer: String,
  val brand: String,
)

/**
 * Represents device type attributes.
 *
 * @property isTablet True when the device is heuristically identified as a tablet.
 * This is only a coarse device-shape hint and should not be used for layout breakpoints.
 * @property isEmulator True when running on an emulator or simulator. This is a heuristic
 * signal and should not be used for security, licensing, or anti-abuse checks.
 */
data class DeviceTypeInfo(
  val isTablet: Boolean,
  val isEmulator: Boolean,
)

/**
 * Represents device CPU information.
 */
enum class CpuArchitecture {
  ARM64,
  ARM32,
  X64,
  X86,
  UNKNOWN
}

/**
 * Represents device CPU information.
 */
data class CpuInfo(
  val architecture: CpuArchitecture = CpuArchitecture.UNKNOWN,
  val coreCount: Int? = null,
)

/**
 * Represents display metrics and orientation information.
 *
 * @property widthPx Display width in pixels.
 * @property heightPx Display height in pixels.
 * @property widthLogical Display width in Android dp or iOS points.
 * @property heightLogical Display height in Android dp or iOS points.
 * @property density Pixel density scale factor.
 * @property densityDpi Density in dpi, 160 is the baseline on Android.
 * @property isLandscape True when the display bounds are in landscape orientation.
 */
data class DisplayInfo(
  val widthPx: Int,
  val heightPx: Int,
  val widthLogical: Int,
  val heightLogical: Int,
  val density: Float,
  val densityDpi: Int,
  val isLandscape: Boolean,
)

/**
 * Represents time zone information.
 *
 * @property id Time zone identifier (e.g., "Asia/Shanghai").
 * @property offsetMinutes Offset from GMT in minutes.
 */
data class TimeZoneInfo(
  val id: String,
  val offsetMinutes: Int,
)

/**
 * Represents device-level metadata.
 *
 * @property deviceModel Device model name.
 * @property systemName OS name, e.g. Android or iOS.
 * @property systemVersion OS version string.
 * @property systemVersionCode Numeric OS version code when available.
 * @property manufacturer Manufacturer and brand information.
 * @property deviceType Device type attributes.
 * @property window Current foreground app/window viewport metrics, or null when unavailable.
 * @property screen Platform main screen snapshot metrics.
 * @property timeZone Current time zone details.
 * @property locale Current and preferred locale information.
 * @property cpu CPU architecture and core count when available.
 */
data class DeviceInfo(
  val deviceModel: String,
  val systemName: String,
  val systemVersion: String,
  val systemVersionCode: Int?,
  val manufacturer: ManufacturerInfo,
  val deviceType: DeviceTypeInfo,
  val window: DisplayInfo?,
  val screen: DisplayInfo,
  val timeZone: TimeZoneInfo,
  val locale: LocaleBundle,
  val cpu: CpuInfo = CpuInfo(),
)

/**
 * Provides platform-specific device metadata.
 *
 * Android requires [Toolkit.initialize] before calling [getDeviceInfo].
 */
internal expect object DeviceInfoProvider {

  /**
   * Returns current [DeviceInfo].
   */
  fun getDeviceInfo(): DeviceInfo
}

internal fun buildLocaleInfo(
  languageCode: String?,
  regionCode: String?,
): LocaleInfo {
  val normalizedLanguage = normalizeLanguageCode(languageCode)
  val normalizedRegion = normalizeRegionCode(regionCode)
  val languageTag = if (normalizedRegion != null) {
    "$normalizedLanguage-$normalizedRegion"
  } else {
    normalizedLanguage
  }
  return LocaleInfo(
    languageTag = languageTag,
    languageCode = normalizedLanguage,
    regionCode = normalizedRegion,
  )
}

internal fun buildLocaleInfoFromTag(tag: String?): LocaleInfo {
  val tokens = tag
    ?.trim()
    ?.takeIf { it.isNotBlank() }
    ?.substringBefore('@')
    ?.replace('_', '-')
    ?.split('-')
    ?.filter { it.isNotBlank() }
    .orEmpty()
  if (tokens.isEmpty()) {
    return LocaleInfo(languageTag = "und", languageCode = "und")
  }

  val language = normalizeLanguageCode(tokens.firstOrNull())
  var index = 1
  val script = tokens.getOrNull(index)
    ?.takeIf(::isScriptSubtag)
    ?.toScriptCode()
    ?.also { index++ }
  val region = tokens.getOrNull(index)
    ?.takeIf(::isRegionSubtag)
    ?.toRegionCode()
    ?.also { index++ }
  val variantTokens = mutableListOf<String>()
  val extensionTokens = mutableListOf<String>()
  var isExtension = false
  while (index < tokens.size) {
    val token = tokens[index]
    if (token.length == 1) {
      isExtension = true
      extensionTokens += token.lowercase()
    } else if (isExtension) {
      extensionTokens += token.lowercase()
    } else if (isVariantSubtag(token)) {
      variantTokens += token.lowercase()
    } else {
      extensionTokens += token.lowercase()
    }
    index++
  }
  val variant = variantTokens.takeIf { it.isNotEmpty() }?.joinToString("-")
  val languageTag = buildList {
    add(language)
    script?.let(::add)
    region?.let(::add)
    variantTokens.forEach(::add)
    extensionTokens.forEach(::add)
  }.joinToString("-")
  return LocaleInfo(
    languageTag = languageTag,
    languageCode = language,
    scriptCode = script,
    regionCode = region,
    variant = variant,
  )
}

private fun normalizeLanguageCode(languageCode: String?): String {
  val normalized = languageCode
    ?.trim()
    ?.lowercase()
    ?.takeIf(::isLanguageSubtag)
  return normalized ?: "und"
}

private fun normalizeRegionCode(regionCode: String?): String? {
  return regionCode
    ?.trim()
    ?.takeIf(::isRegionSubtag)
    ?.toRegionCode()
}

private fun String.toScriptCode(): String {
  return lowercase().replaceFirstChar { char -> char.uppercase() }
}

private fun String.toRegionCode(): String {
  return if (all(Char::isDigit)) this else uppercase()
}

private fun isLanguageSubtag(value: String): Boolean {
  return value.length in 2..8 && value.all(Char::isLetter)
}

private fun isScriptSubtag(value: String): Boolean {
  return value.length == 4 && value.all(Char::isLetter)
}

private fun isRegionSubtag(value: String): Boolean {
  return (value.length == 2 && value.all(Char::isLetter)) ||
    (value.length == 3 && value.all(Char::isDigit))
}

private fun isVariantSubtag(value: String): Boolean {
  return (value.length in 5..8 && value.all(Char::isLetterOrDigit)) ||
    (value.length == 4 && value.first().isDigit() && value.all(Char::isLetterOrDigit))
}
