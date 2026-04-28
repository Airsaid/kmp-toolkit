package com.airsaid.toolkit

/**
 * Represents locale information for the current system.
 *
 * @property languageCode ISO 639 language code (lowercase).
 * @property regionCode ISO 3166 region code (uppercase), empty if unavailable.
 */
data class LocaleInfo(
  val languageCode: String,
  val regionCode: String,
) {

  /**
   * Locale tag in the form of "language-region" when region is available.
   */
  val tag: String
    get() = if (regionCode.isNotBlank()) "$languageCode-$regionCode" else languageCode
}

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
 * @property isTablet True when the device is identified as a tablet.
 * @property isEmulator True when running on an emulator or simulator.
 */
data class DeviceTypeInfo(
  val isTablet: Boolean,
  val isEmulator: Boolean,
)

/**
 * Represents screen metrics and orientation information.
 *
 * @property widthPx Screen width in pixels.
 * @property heightPx Screen height in pixels.
 * @property widthDp Screen width in dp (or points on iOS).
 * @property heightDp Screen height in dp (or points on iOS).
 * @property density Pixel density scale factor.
 * @property densityDpi Density in dpi, 160 is the baseline on Android.
 * @property isLandscape True when the screen is in landscape orientation.
 */
data class ScreenInfo(
  val widthPx: Int,
  val heightPx: Int,
  val widthDp: Int,
  val heightDp: Int,
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
 * @property systemVersionCode Numeric OS version code if available, otherwise -1.
 * @property manufacturer Manufacturer and brand information.
 * @property deviceType Device type attributes.
 * @property screen Screen metrics and orientation information.
 * @property timeZone Current time zone details.
 * @property locale Current and preferred locale information.
 */
data class DeviceInfo(
  val deviceModel: String,
  val systemName: String,
  val systemVersion: String,
  val systemVersionCode: Int,
  val manufacturer: ManufacturerInfo,
  val deviceType: DeviceTypeInfo,
  val screen: ScreenInfo,
  val timeZone: TimeZoneInfo,
  val locale: LocaleBundle,
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
  val normalizedLanguage = languageCode
    ?.trim()
    ?.lowercase()
    ?.takeIf { it.isNotBlank() }
    ?: "und"
  val normalizedRegion = regionCode
    ?.trim()
    ?.uppercase()
    ?.takeIf { it.isNotBlank() }
    ?: ""
  return LocaleInfo(
    languageCode = normalizedLanguage,
    regionCode = normalizedRegion,
  )
}

internal fun buildLocaleInfoFromTag(tag: String?): LocaleInfo {
  if (tag.isNullOrBlank()) {
    return buildLocaleInfo(null, null)
  }
  val tokens = tag
    .trim()
    .replace('_', '-')
    .split('-')
    .filter { it.isNotBlank() }
  val language = tokens.firstOrNull()
  val region = tokens.lastOrNull { token ->
    token.length in 2..3 && token != language
  }
  return buildLocaleInfo(language, region)
}
