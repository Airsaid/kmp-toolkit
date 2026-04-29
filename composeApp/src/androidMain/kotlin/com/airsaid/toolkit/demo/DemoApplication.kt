package com.airsaid.toolkit.demo

import android.app.Application
import com.airsaid.toolkit.Toolkit

/**
 * @author airsaid
 */
class DemoApplication : Application() {

  override fun onCreate() {
    super.onCreate()
    Toolkit.initialize(applicationContext)
  }
}
