package com.airsaid.toolkit

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NetworkMonitorInstrumentedTest {
  @Test
  fun observeWithMultipleCollectors() = runBlocking {
    val appContext = InstrumentationRegistry.getInstrumentation().targetContext
    Toolkit.initialize(appContext)

    val monitor = Toolkit.network()
    monitor.startMonitoring()

    val firstCollector = async {
      monitor.observeNetworkStatus().first()
    }
    val secondCollector = async {
      monitor.observeNetworkStatus().first()
    }

    val status1 = withTimeout(5_000) { firstCollector.await() }
    val status2 = withTimeout(5_000) { secondCollector.await() }

    assertNotNull(status1)
    assertNotNull(status2)

    monitor.stopMonitoring()
    monitor.startMonitoring()
    monitor.stopMonitoring()
  }
}
