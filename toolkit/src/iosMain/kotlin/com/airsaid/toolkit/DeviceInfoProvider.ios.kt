@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.airsaid.toolkit

import kotlinx.cinterop.CValue
import kotlinx.cinterop.useContents
import platform.CoreGraphics.CGRect
import platform.Foundation.NSLocale
import platform.Foundation.NSLocaleIdentifier
import platform.Foundation.NSProcessInfo
import platform.Foundation.NSTimeZone
import platform.Foundation.currentLocale
import platform.Foundation.localTimeZone
import platform.Foundation.preferredLanguages
import platform.Foundation.secondsFromGMT
import platform.UIKit.UIDevice
import platform.UIKit.UIScreen
import platform.UIKit.UIUserInterfaceIdiomPad
import platform.UIKit.UIView
import kotlin.math.roundToInt

/**
 * iOS implementation of [DeviceInfoProvider].
 */
internal actual object DeviceInfoProvider {

  /**
   * Returns current [DeviceInfo] for iOS.
   */
  actual fun getDeviceInfo(): DeviceInfo {
    val device = UIDevice.currentDevice
    val localeIdentifier = NSLocale.currentLocale.objectForKey(NSLocaleIdentifier) as? String
    val currentLocale = buildLocaleInfoFromTag(localeIdentifier)
    val preferredLocales = buildPreferredLocales(currentLocale)

    val screen = UIScreen.mainScreen
    val timeZone = NSTimeZone.localTimeZone
    val timeZoneOffsetMinutes = (timeZone.secondsFromGMT / 60).toInt()

    return DeviceInfo(
      deviceModel = device.model,
      systemName = device.systemName,
      systemVersion = device.systemVersion,
      systemVersionCode = null,
      manufacturer = ManufacturerInfo(
        manufacturer = "Apple",
        brand = "Apple",
      ),
      deviceType = DeviceTypeInfo(
        isTablet = device.userInterfaceIdiom == UIUserInterfaceIdiomPad,
        isEmulator = isSimulator(),
      ),
      window = resolveKeyWindow()?.let { window ->
        buildAppleDisplayInfo(window, window.screen.scale)
      },
      screen = buildAppleDisplayInfo(screen.bounds, screen.scale),
      timeZone = TimeZoneInfo(
        id = timeZone.name ?: "",
        offsetMinutes = timeZoneOffsetMinutes,
      ),
      locale = LocaleBundle(
        current = currentLocale,
        preferred = preferredLocales,
      ),
    )
  }
}

private fun buildPreferredLocales(currentLocale: LocaleInfo): List<LocaleInfo> {
  val preferred = (NSLocale.preferredLanguages as? List<*>)?.mapNotNull { entry ->
    val tag = entry as? String
    buildLocaleInfoFromTag(tag)
  }.orEmpty()
  return if (preferred.isNotEmpty()) preferred else listOf(currentLocale)
}

private fun buildAppleDisplayInfo(view: UIView, scale: Double): DisplayInfo {
  return buildAppleDisplayInfo(view.bounds, scale)
}

private fun buildAppleDisplayInfo(
  bounds: CValue<CGRect>,
  scale: Double,
): DisplayInfo {
  var widthLogical = 0
  var heightLogical = 0
  bounds.useContents {
    widthLogical = size.width.roundToInt()
    heightLogical = size.height.roundToInt()
  }
  val density = scale.toFloat()
  return DisplayInfo(
    widthPx = (widthLogical * density).roundToInt(),
    heightPx = (heightLogical * density).roundToInt(),
    widthLogical = widthLogical,
    heightLogical = heightLogical,
    density = density,
    densityDpi = (density * 160f).roundToInt(),
    isLandscape = widthLogical >= heightLogical,
  )
}

private fun isSimulator(): Boolean {
  val environment = NSProcessInfo.processInfo.environment
  return environment["SIMULATOR_DEVICE_NAME"] != null ||
    environment["SIMULATOR_MODEL_IDENTIFIER"] != null
}
