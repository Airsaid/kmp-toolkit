package com.airsaid.toolkit

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.conflate
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
import platform.Network.nw_path_status_satisfiable
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
        type = NetworkType.NONE,
      )
    )

  /** Lock for thread-safe state changes. */
  private val lock = NSLock()

  /** Tracks whether monitoring is currently active. */
  private var isMonitoring = false

  /** Stores the current network path for synchronous access. */
  private var currentPath: nw_path_t? = null

  /** Tracks the number of active observers. */
  private var observerCount = 0
  private var isManuallyStarted = false
  private var isExplicitlyStopped = false

  /**
   * Observes network status changes and emits [NetworkStatus] updates as a [Flow].
   *
   * Starts monitoring automatically and stops when all [Flow] collections are cancelled.
   */
  override fun observeNetworkStatus(): Flow<NetworkStatus> =
    statusState
      .onStart { onObserverStart() }
      .onCompletion { onObserverStop() }
      .conflate()
      .distinctUntilChanged()

  /**
   * Returns the current [NetworkStatus] synchronously.
   */
  override suspend fun getCurrentNetworkStatus(): NetworkStatus {
    // If monitoring is active and we have a current path, use it
    if (isMonitoring) {
      currentPath?.let { path ->
        return getNetworkStatus(path)
      }
    }

    // Otherwise, get current status with a temporary monitor
    return suspendCancellableCoroutine { continuation ->
      val tempMonitor = nw_path_monitor_create()
      val tempQueue = dispatch_queue_create("com.airsaid.toolkit.network.temp", null)

      var resumed = false
      val updateHandler: nw_path_monitor_update_handler_t = { path ->
        if (!resumed) {
          resumed = true
          val status = path?.let { getNetworkStatus(it) }
            ?: NetworkStatus(isConnected = false, type = NetworkType.NONE)

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
   * Starts monitoring network status if not already started.
   */
  override fun startMonitoring() {
    withLock {
      isExplicitlyStopped = false
      isManuallyStarted = true
      if (!isMonitoring) {
        startMonitoringInternal()
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
      currentPath = path
      val status = path?.let { getNetworkStatus(it) }
        ?: NetworkStatus(isConnected = false, type = NetworkType.NONE)
      statusState.value = status
    }

    nw_path_monitor_set_queue(newMonitor, newQueue)
    nw_path_monitor_set_update_handler(newMonitor, updateHandler)
    nw_path_monitor_start(newMonitor)
    isMonitoring = true
  }

  /**
   * Stops monitoring network status if currently active.
   */
  override fun stopMonitoring() {
    withLock {
      isManuallyStarted = false
      isExplicitlyStopped = true
      if (isMonitoring) {
        stopMonitoringInternal()
      }
    }
  }

  /**
   * Internal method to stop monitoring.
   */
  private fun stopMonitoringInternal() {
    val currentMonitor = monitor ?: return
    nw_path_monitor_set_update_handler(currentMonitor, null)
    nw_path_monitor_cancel(currentMonitor)
    monitor = null
    currentPath = null
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
      val isConnected = status == nw_path_status_satisfied ||
          status == nw_path_status_satisfiable

      if (!isConnected) {
        return NetworkStatus(
          isConnected = false,
          type = NetworkType.NONE,
        )
      }

      val type = when {
        nw_path_uses_interface_type(path, nw_interface_type_wifi) -> NetworkType.WIFI
        nw_path_uses_interface_type(path, nw_interface_type_cellular) -> NetworkType.CELLULAR
        nw_path_uses_interface_type(path, nw_interface_type_wired) -> NetworkType.ETHERNET
        else -> NetworkType.UNKNOWN
      }

      NetworkStatus(
        isConnected = isConnected,
        type = type,
      )
    } catch (e: Exception) {
      NetworkStatus(isConnected = false, type = NetworkType.NONE)
    }
  }

  private fun onObserverStart() {
    withLock {
      observerCount++
      if (!isExplicitlyStopped && !isMonitoring) {
        startMonitoringInternal()
      }
    }
  }

  private fun onObserverStop() {
    withLock {
      observerCount = (observerCount - 1).coerceAtLeast(0)
      if (observerCount == 0 && !isManuallyStarted && isMonitoring) {
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
