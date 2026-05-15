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
      is Boolean -> value
      is NSNumber -> value.boolValue
      is String -> value.toBooleanStrictOrNull()
      else -> defaultValue
    }
  }

  override fun readInt(
    key: String,
    defaultValue: Int?,
  ): Int? {
    val value = NSBundle.mainBundle.objectForInfoDictionaryKey(key)
    return when (value) {
      is NSNumber -> value.intValue
      is Number -> value.toInt()
      is String -> value.toIntOrNull()
      else -> defaultValue
    }
  }

  override fun readLong(
    key: String,
    defaultValue: Long?,
  ): Long? {
    val value = NSBundle.mainBundle.objectForInfoDictionaryKey(key)
    return when (value) {
      is NSNumber -> value.longLongValue
      is Number -> value.toLong()
      is String -> value.toLongOrNull()
      else -> defaultValue
    }
  }

  override fun readFloat(
    key: String,
    defaultValue: Float?,
  ): Float? {
    val value = NSBundle.mainBundle.objectForInfoDictionaryKey(key)
    return when (value) {
      is NSNumber -> value.floatValue
      is Number -> value.toFloat()
      is String -> value.toFloatOrNull()
      else -> defaultValue
    }
  }
}
