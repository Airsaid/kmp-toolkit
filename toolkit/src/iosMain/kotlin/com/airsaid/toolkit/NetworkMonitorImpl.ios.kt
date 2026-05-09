package com.airsaid.toolkit

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.Network.nw_interface_type_cellular
import platform.Network.nw_interface_type_wifi
import platform.Network.nw_interface_type_wired
import platform.Network.nw_path_get_status
import platform.Network.nw_path_monitor_cancel
import platform.Network.nw_path_monitor_create
import platform.Network.nw_path_monitor_set_queue
import platform.Network.nw_path_monitor_set_update_handler
import platform.Network.nw_path_monitor_start
import platform.Network.nw_path_monitor_t
import platform.Network.nw_path_monitor_update_handler_t
import platform.Network.nw_path_status_satisfied
import platform.Network.nw_path_t
import platform.Network.nw_path_uses_interface_type
import platform.Foundation.NSLock
import platform.darwin.dispatch_queue_create
import kotlin.coroutines.resume

/**
 * iOS implementation of [NetworkMonitor] using Apple's Network framework (nw_path_monitor).
 *
 * @author airsaid
 */
internal class NetworkMonitorImpl : NetworkMonitor {

  /** Network path monitor instance. */
  private var monitor: nw_path_monitor_t? = null

  /** Current network status for observers. */
  private val statusState: MutableStateFlow<NetworkStatus> =
    MutableStateFlow(
      NetworkStatus(
        isConnected = false,
      )
    )

  /** Lock for thread-safe state changes. */
  private val lock = NSLock()

  /** Tracks whether monitoring is currently active. */
  private var isMonitoring = false

  /** Tracks the number of active observers. */
  private var observerCount = 0

  /**
   * Observes network status changes and emits [NetworkStatus] updates as a [Flow].
   *
   * Starts monitoring automatically and stops when all [Flow] collections are cancelled.
   */
  override fun observeNetworkStatus(): Flow<NetworkStatus> =
    statusState
      .onStart { onObserverStart() }
      .onCompletion { onObserverStop() }
      .distinctUntilChanged()

  /**
   * Returns the current [NetworkStatus] synchronously.
   */
  override suspend fun getCurrentNetworkStatus(): NetworkStatus {
    return suspendCancellableCoroutine { continuation ->
      val tempMonitor = nw_path_monitor_create()
      val tempQueue = dispatch_queue_create("com.airsaid.toolkit.network.temp", null)

      var resumed = false
      val updateHandler: nw_path_monitor_update_handler_t = { path ->
        if (!resumed) {
          resumed = true
          val status = path?.let { getNetworkStatus(it) }
            ?: NetworkStatus(isConnected = false)

          nw_path_monitor_cancel(tempMonitor)
          continuation.resume(status)
        }
      }

      nw_path_monitor_set_update_handler(tempMonitor, updateHandler)
      nw_path_monitor_set_queue(tempMonitor, tempQueue)
      nw_path_monitor_start(tempMonitor)

      continuation.invokeOnCancellation {
        if (!resumed) {
          nw_path_monitor_cancel(tempMonitor)
        }
      }
    }
  }

  /**
   * Internal method to start monitoring.
   */
  private fun startMonitoringInternal() {
    if (isMonitoring) return

    val newMonitor = nw_path_monitor_create()
    val newQueue = dispatch_queue_create("com.airsaid.toolkit.network.monitor", null)

    monitor = newMonitor
    val updateHandler: nw_path_monitor_update_handler_t = { path ->
      val status = path?.let { getNetworkStatus(it) }
        ?: NetworkStatus(isConnected = false)
      statusState.value = status
    }

    nw_path_monitor_set_queue(newMonitor, newQueue)
    nw_path_monitor_set_update_handler(newMonitor, updateHandler)
    nw_path_monitor_start(newMonitor)
    isMonitoring = true
  }

  /**
   * Internal method to stop monitoring.
   */
  private fun stopMonitoringInternal() {
    val currentMonitor = monitor ?: return
    nw_path_monitor_set_update_handler(currentMonitor, null)
    nw_path_monitor_cancel(currentMonitor)
    monitor = null
    isMonitoring = false
  }

  /**
   * Converts an [nw_path_t] instance to [NetworkStatus].
   *
   * @param path The network path from the Network framework.
   * @return Corresponding [NetworkStatus] reflecting connectivity and transport type.
   */
  private fun getNetworkStatus(path: nw_path_t): NetworkStatus {
    return try {
      val status = nw_path_get_status(path)
      val isConnected = status == nw_path_status_satisfied

      if (!isConnected) {
        return NetworkStatus(
          isConnected = false,
        )
      }

      val transports = mutableSetOf<NetworkTransport>()
      if (nw_path_uses_interface_type(path, nw_interface_type_wifi)) {
        transports += NetworkTransport.WIFI
      }
      if (nw_path_uses_interface_type(path, nw_interface_type_cellular)) {
        transports += NetworkTransport.CELLULAR
      }
      if (nw_path_uses_interface_type(path, nw_interface_type_wired)) {
        transports += NetworkTransport.ETHERNET
      }
      if (transports.isEmpty()) {
        transports += NetworkTransport.UNKNOWN
      }

      NetworkStatus(
        isConnected = isConnected,
        transports = transports,
      )
    } catch (e: Exception) {
      NetworkStatus(isConnected = false)
    }
  }

  private fun onObserverStart() {
    withLock {
      observerCount++
      if (!isMonitoring) {
        startMonitoringInternal()
      }
    }
  }

  private fun onObserverStop() {
    withLock {
      observerCount = (observerCount - 1).coerceAtLeast(0)
      if (observerCount == 0 && isMonitoring) {
        stopMonitoringInternal()
      }
    }
  }

  private inline fun <T> withLock(block: () -> T): T {
    lock.lock()
    try {
      return block()
    } finally {
      lock.unlock()
    }
  }
}
