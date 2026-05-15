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
