package com.airsaid.toolkit

import platform.Foundation.NSBundle
import platform.Foundation.NSNumber

internal actual object AppConfigReaderFactory {

  actual fun create(): AppConfigReader {
    return IosAppConfigReader
  }
}

private object IosAppConfigReader : AppConfigReader {

  override fun readString(
    key: String,
    defaultValue: String?,
  ): String? {
    return NSBundle.mainBundle.objectForInfoDictionaryKey(key)?.toString() ?: defaultValue
  }

  override fun readBoolean(
    key: String,
    defaultValue: Boolean?,
  ): Boolean? {
    val value = NSBundle.mainBundle.objectForInfoDictionaryKey(key)
    return when (value) {
      is NSNumber -> value.boolValue
      else -> parseAppConfigBoolean(value, defaultValue)
    }
  }

  override fun readInt(
    key: String,
    defaultValue: Int?,
  ): Int? {
    val value = NSBundle.mainBundle.objectForInfoDictionaryKey(key)
    return when (value) {
      is NSNumber -> value.intValue
      else -> parseAppConfigInt(value, defaultValue)
    }
  }

  override fun readLong(
    key: String,
    defaultValue: Long?,
  ): Long? {
    val value = NSBundle.mainBundle.objectForInfoDictionaryKey(key)
    return when (value) {
      is NSNumber -> value.longLongValue
      else -> parseAppConfigLong(value, defaultValue)
    }
  }

  override fun readFloat(
    key: String,
    defaultValue: Float?,
  ): Float? {
    val value = NSBundle.mainBundle.objectForInfoDictionaryKey(key)
    return when (value) {
      is NSNumber -> value.floatValue
      else -> parseAppConfigFloat(value, defaultValue)
    }
  }
}
