package com.airsaid.toolkit.demo.toolkit

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSBundle
import platform.Foundation.NSData
import platform.UIKit.UIImage
import platform.UIKit.UIImagePNGRepresentation
import platform.posix.memcpy

@Composable
actual fun rememberAppLogoBytes(): ByteArray? {
  return remember {
    loadAppLogoBytes()
  }
}

private fun loadAppLogoBytes(): ByteArray? {
  val info = NSBundle.mainBundle.infoDictionary ?: return null
  val icons = info["CFBundleIcons"] as? Map<*, *> ?: return null
  val primary = icons["CFBundlePrimaryIcon"] as? Map<*, *> ?: return null
  val files = primary["CFBundleIconFiles"] as? List<*> ?: return null
  val iconName = files.lastOrNull() as? String ?: return null
  val image = UIImage.imageNamed(iconName) ?: return null
  return image.toPngData()?.toByteArray()
}

@OptIn(ExperimentalForeignApi::class)
private fun NSData.toByteArray(): ByteArray {
  val length = this.length.toInt()
  if (length == 0) return ByteArray(0)
  val bytes = ByteArray(length)
  bytes.usePinned { pinned ->
    memcpy(pinned.addressOf(0), this.bytes, length.toULong())
  }
  return bytes
}

private fun UIImage.toPngData(): NSData? {
  return UIImagePNGRepresentation(this)
}
