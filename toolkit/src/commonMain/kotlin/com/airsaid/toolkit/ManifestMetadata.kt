package com.airsaid.toolkit

/**
 * Reads static key-value configuration packaged with the app.
 *
 * Android reads application manifest meta-data. iOS reads Info.plist values.
 */
interface AppConfigReader {

  fun readString(key: String, defaultValue: String? = null): String?

  fun readBoolean(key: String, defaultValue: Boolean? = null): Boolean?

  fun readInt(key: String, defaultValue: Int? = null): Int?

  fun readLong(key: String, defaultValue: Long? = null): Long?

  fun readFloat(key: String, defaultValue: Float? = null): Float?
}

internal expect object AppConfigReaderFactory {
  fun create(): AppConfigReader
}

internal fun parseAppConfigBoolean(
  value: Any?,
  defaultValue: Boolean?,
): Boolean? {
  return when (value) {
    is Boolean -> value
    is String -> value.toBooleanStrictOrNull() ?: defaultValue
    else -> defaultValue
  }
}

internal fun parseAppConfigInt(
  value: Any?,
  defaultValue: Int?,
): Int? {
  return when (value) {
    is Number -> value.toInt()
    is String -> value.toIntOrNull() ?: defaultValue
    else -> defaultValue
  }
}

internal fun parseAppConfigLong(
  value: Any?,
  defaultValue: Long?,
): Long? {
  return when (value) {
    is Number -> value.toLong()
    is String -> value.toLongOrNull() ?: defaultValue
    else -> defaultValue
  }
}

internal fun parseAppConfigFloat(
  value: Any?,
  defaultValue: Float?,
): Float? {
  return when (value) {
    is Number -> value.toFloat()
    is String -> value.toFloatOrNull() ?: defaultValue
    else -> defaultValue
  }
}
