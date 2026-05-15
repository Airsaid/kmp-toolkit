package com.airsaid.toolkit

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle

/**
 * Android implementation of [AppConfigReaderFactory].
 */
internal actual object AppConfigReaderFactory {

  private var applicationContext: Context? = null

  internal fun initialize(context: Context) {
    applicationContext = context.applicationContext
  }

  actual fun create(): AppConfigReader {
    val context = applicationContext
      ?: throw IllegalStateException(
        "AppConfigReaderFactory must be initialized with Context on Android. " +
            "Call Toolkit.initialize(context) first."
      )
    return AndroidAppConfigReader(context)
  }
}

private class AndroidAppConfigReader(
  private val context: Context,
) : AppConfigReader {

  private val metadata: Bundle?
    get() {
      return try {
        val applicationInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
          context.packageManager.getApplicationInfo(
            context.packageName,
            PackageManager.ApplicationInfoFlags.of(PackageManager.GET_META_DATA.toLong()),
          )
        } else {
          @Suppress("DEPRECATION")
          context.packageManager.getApplicationInfo(context.packageName, PackageManager.GET_META_DATA)
        }
        applicationInfo.metaData
      } catch (e: PackageManager.NameNotFoundException) {
        null
      }
    }

  override fun readString(
    key: String,
    defaultValue: String?,
  ): String? {
    return metadata?.rawValue(key)?.toString() ?: defaultValue
  }

  override fun readBoolean(
    key: String,
    defaultValue: Boolean?,
  ): Boolean? {
    return parseAppConfigBoolean(metadata?.rawValue(key), defaultValue)
  }

  override fun readInt(
    key: String,
    defaultValue: Int?,
  ): Int? {
    return parseAppConfigInt(metadata?.rawValue(key), defaultValue)
  }

  override fun readLong(
    key: String,
    defaultValue: Long?,
  ): Long? {
    return parseAppConfigLong(metadata?.rawValue(key), defaultValue)
  }

  override fun readFloat(
    key: String,
    defaultValue: Float?,
  ): Float? {
    return parseAppConfigFloat(metadata?.rawValue(key), defaultValue)
  }
}

private fun Bundle.rawValue(key: String): Any? {
  if (!containsKey(key)) return null
  @Suppress("DEPRECATION")
  return get(key)
}
