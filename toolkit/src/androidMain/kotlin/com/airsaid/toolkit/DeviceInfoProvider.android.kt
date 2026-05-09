package com.airsaid.toolkit

import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import android.graphics.Rect
import android.os.Build
import androidx.window.layout.WindowMetricsCalculator
import java.util.Locale
import java.util.TimeZone
import kotlin.math.roundToInt

/**
 * Android implementation of [DeviceInfoProvider].
 */
internal actual object DeviceInfoProvider {

  private var applicationContext: Context? = null

  /**
   * Must be initialized with an Android [Context] before accessing [getDeviceInfo].
   */
  internal fun initialize(context: Context) {
    applicationContext = context.applicationContext
  }

  /**
   * Returns current [DeviceInfo] for Android.
   */
  actual fun getDeviceInfo(): DeviceInfo {
    val resources = requireContext().resources
    val configuration = resources.configuration
    val displayMetrics = resources.displayMetrics
    val currentLocale = resolveCurrentLocale(configuration)
    val preferredLocales = buildPreferredLocales(configuration, currentLocale)

    val timeZone = TimeZone.getDefault()
    val timeZoneOffsetMinutes = timeZone.getOffset(System.currentTimeMillis()) / 60000

    return DeviceInfo(
      deviceModel = Build.MODEL.orEmpty(),
      systemName = "Android",
      systemVersion = Build.VERSION.RELEASE.orEmpty(),
      systemVersionCode = Build.VERSION.SDK_INT,
      manufacturer = ManufacturerInfo(
        manufacturer = Build.MANUFACTURER.orEmpty(),
        brand = Build.BRAND.orEmpty(),
      ),
      deviceType = DeviceTypeInfo(
        isTablet = isTablet(configuration),
        isEmulator = isEmulator(),
      ),
      window = resolveCurrentWindowDisplayInfo(ActivityLifecycleRegistry.getCurrentActivity()),
      screen = buildAndroidDisplayInfo(
        widthPx = displayMetrics.widthPixels,
        heightPx = displayMetrics.heightPixels,
        density = displayMetrics.density,
        densityDpi = resolveDensityDpi(configuration, displayMetrics.densityDpi),
        widthLogical = configuration.screenWidthDp.takeUnless {
          it == Configuration.SCREEN_WIDTH_DP_UNDEFINED
        },
        heightLogical = configuration.screenHeightDp.takeUnless {
          it == Configuration.SCREEN_HEIGHT_DP_UNDEFINED
        },
      ),
      timeZone = TimeZoneInfo(
        id = timeZone.id.orEmpty(),
        offsetMinutes = timeZoneOffsetMinutes,
      ),
      locale = LocaleBundle(
        current = currentLocale,
        preferred = preferredLocales,
      ),
    )
  }

  private fun requireContext(): Context {
    return applicationContext
      ?: throw IllegalStateException(
        "DeviceInfoProvider must be initialized with Context on Android. " +
            "Call Toolkit.initialize(context) first."
      )
  }
}

private fun buildPreferredLocales(
  configuration: Configuration,
  currentLocale: LocaleInfo,
): List<LocaleInfo> {
  val localeList = configuration.locales
  val localeInfos = (0 until localeList.size()).map { index ->
    val locale = localeList[index]
    buildLocaleInfoFromTag(locale.toLanguageTag())
  }
  return if (localeInfos.isNotEmpty()) localeInfos else listOf(currentLocale)
}

private fun resolveCurrentLocale(configuration: Configuration): LocaleInfo {
  val localeList = configuration.locales
  val locale = if (localeList.isEmpty) {
    Locale.getDefault()
  } else {
    localeList[0]
  }
  return buildLocaleInfoFromTag(locale.toLanguageTag())
}

internal fun resolveCurrentWindowDisplayInfo(activity: Activity?): DisplayInfo? {
  if (activity == null) return null
  val windowMetrics = WindowMetricsCalculator
    .getOrCreate()
    .computeCurrentWindowMetrics(activity)
  val configuration = activity.resources.configuration
  val displayMetrics = activity.resources.displayMetrics
  val densityDpi = resolveDensityDpi(configuration, displayMetrics.densityDpi)
  return buildAndroidDisplayInfo(
    bounds = windowMetrics.bounds,
    densityDpi = densityDpi,
  )
}

internal fun buildAndroidDisplayInfo(
  bounds: Rect,
  densityDpi: Int,
): DisplayInfo {
  val density = densityDpi / 160f
  return buildAndroidDisplayInfo(
    widthPx = bounds.width(),
    heightPx = bounds.height(),
    density = density,
    densityDpi = densityDpi,
  )
}

internal fun buildAndroidDisplayInfo(
  widthPx: Int,
  heightPx: Int,
  density: Float,
  densityDpi: Int,
  widthLogical: Int? = null,
  heightLogical: Int? = null,
): DisplayInfo {
  val safeDensity = density.takeIf { it > 0f } ?: 1f
  val safeDensityDpi = densityDpi.takeIf { it > 0 } ?: (safeDensity * 160f).roundToInt()
  return DisplayInfo(
    widthPx = widthPx,
    heightPx = heightPx,
    widthLogical = widthLogical ?: (widthPx / safeDensity).roundToInt(),
    heightLogical = heightLogical ?: (heightPx / safeDensity).roundToInt(),
    density = safeDensity,
    densityDpi = safeDensityDpi,
    isLandscape = widthPx >= heightPx,
  )
}

private fun resolveDensityDpi(configuration: Configuration, fallbackDensityDpi: Int): Int {
  return configuration.densityDpi.takeIf { it > 0 } ?: fallbackDensityDpi
}

private fun isTablet(configuration: Configuration): Boolean {
  val screenLayout = configuration.screenLayout and Configuration.SCREENLAYOUT_SIZE_MASK
  return screenLayout >= Configuration.SCREENLAYOUT_SIZE_LARGE
}

private fun isEmulator(): Boolean {
  val fingerprint = Build.FINGERPRINT
  val model = Build.MODEL
  val manufacturer = Build.MANUFACTURER
  val brand = Build.BRAND
  val device = Build.DEVICE
  val product = Build.PRODUCT
  return fingerprint.startsWith("generic") || fingerprint.startsWith("unknown") ||
    model.contains("google_sdk", ignoreCase = true) ||
    model.contains("Emulator", ignoreCase = true) ||
    model.contains("Android SDK built for x86", ignoreCase = true) ||
    manufacturer.contains("Genymotion", ignoreCase = true) ||
    (brand.startsWith("generic") && device.startsWith("generic")) ||
    product.contains("sdk_gphone", ignoreCase = true)
}
