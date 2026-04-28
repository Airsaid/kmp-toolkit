package com.airsaid.toolkit.demo

import android.app.Application
import com.airsaid.toolkit.Toolkit
import com.airsaid.toolkit.ToolkitInitializer

/**
 * @author airsaid
 */
class DemoApplication : Application() {

  override fun onCreate() {
    super.onCreate()
    Toolkit.initialize(ToolkitInitializer(applicationContext))
  }
}
