package com.airsaid.toolkit

import android.content.pm.PackageManager
import android.os.Build
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppInfoInstrumentedTest {

  @Test
  fun appInfoReadsCurrentAndroidPackageMetadata() {
    val appContext = InstrumentationRegistry.getInstrumentation().targetContext
    Toolkit.initialize(appContext)

    val appInfo = Toolkit.appInfo()
    val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      appContext.packageManager.getPackageInfo(
        appContext.packageName,
        PackageManager.PackageInfoFlags.of(0),
      )
    } else {
      @Suppress("DEPRECATION")
      appContext.packageManager.getPackageInfo(appContext.packageName, 0)
    }
    val expectedBuildNumber = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
      packageInfo.longVersionCode.toString()
    } else {
      @Suppress("DEPRECATION")
      packageInfo.versionCode.toString()
    }

    assertEquals(appContext.packageName, appInfo.packageName)
    assertEquals(expectedBuildNumber, appInfo.buildNumber)
  }
}
