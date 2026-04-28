package com.airsaid.toolkit

/**
 * Returns true when [AppInfo.buildType] indicates a debug build.
 */
val AppInfo.isDebug: Boolean
  get() = buildType.equals("debug", ignoreCase = true)
