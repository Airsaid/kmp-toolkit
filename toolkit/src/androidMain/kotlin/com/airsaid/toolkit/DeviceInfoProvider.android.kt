package com.airsaid.toolkit

import android.content.Context
import android.content.res.Configuration
import android.os.Build
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

    val widthPx = displayMetrics.widthPixels
    val heightPx = displayMetrics.heightPixels
    val density = displayMetrics.density
    val densityDpi = displayMetrics.densityDpi
    val widthDp = if (configuration.screenWidthDp != Configuration.SCREEN_WIDTH_DP_UNDEFINED) {
      configuration.screenWidthDp
    } else {
      (widthPx / density).roundToInt()
    }
    val heightDp = if (configuration.screenHeightDp != Configuration.SCREEN_HEIGHT_DP_UNDEFINED) {
      configuration.screenHeightDp
    } else {
      (heightPx / density).roundToInt()
    }
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
      screen = ScreenInfo(
        widthPx = widthPx,
        heightPx = heightPx,
        widthDp = widthDp,
        heightDp = heightDp,
        density = density,
        densityDpi = densityDpi,
        isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE,
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
            "Call Toolkit.initialize(ToolkitInitializer(context)) first."
      )
  }
}

private fun buildPreferredLocales(
  configuration: Configuration,
  currentLocale: LocaleInfo,
): List<LocaleInfo> {
  val localeInfos = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
    val localeList = configuration.locales
    (0 until localeList.size()).map { index ->
      val locale = localeList[index]
      buildLocaleInfo(locale.language, locale.country)
    }
  } else {
    @Suppress("DEPRECATION")
    val locale = configuration.locale
    listOf(buildLocaleInfo(locale.language, locale.country))
  }
  return if (localeInfos.isNotEmpty()) localeInfos else listOf(currentLocale)
}

private fun resolveCurrentLocale(configuration: Configuration): LocaleInfo {
  val locale = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
    val localeList = configuration.locales
    if (localeList.isEmpty) {
      Locale.getDefault()
    } else {
      localeList[0]
    }
  } else {
    @Suppress("DEPRECATION")
    configuration.locale
  }
  return buildLocaleInfo(locale.language, locale.country)
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
