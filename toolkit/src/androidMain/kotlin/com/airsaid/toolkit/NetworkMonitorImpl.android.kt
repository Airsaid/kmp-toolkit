package com.airsaid.toolkit

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart

/**
 * Android implementation of [NetworkMonitor] using [ConnectivityManager].
 *
 * @param context The application context used to access system services.
 *
 * @author airsaid
 */
internal class NetworkMonitorImpl(
  private val context: Context
) : NetworkMonitor {

  private val connectivityManager =
    context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

  private val statusState: MutableStateFlow<NetworkStatus> =
    MutableStateFlow(
      NetworkStatus(
        isConnected = false,
      )
    )

  private val lock = Any()

  private var networkCallback: ConnectivityManager.NetworkCallback? = null
  private var isMonitoring = false
  private var observerCount = 0

  /**
   * Observes network status changes using [ConnectivityManager.NetworkCallback].
   *
   * Emits [NetworkStatus] updates whenever the connectivity changes.
   * Automatically sends the initial state when collection starts.
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
    return getNetworkStatus()
  }

  /**
   * Gets the current [NetworkStatus] by checking the active network
   * and its [NetworkCapabilities].
   */
  private fun getNetworkStatus(): NetworkStatus {
    val network = connectivityManager.activeNetwork
    val capabilities = connectivityManager.getNetworkCapabilities(network)
    return capabilities.toNetworkStatus()
  }

  private fun NetworkCapabilities?.toNetworkStatus(): NetworkStatus {
    val capabilities = this
    if (capabilities == null) {
      return NetworkStatus(
        isConnected = false,
      )
    }

    val isConnected =
      capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
          capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)

    val transports = mutableSetOf<NetworkTransport>()
    if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
      transports += NetworkTransport.WIFI
    }
    if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
      transports += NetworkTransport.CELLULAR
    }
    if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) {
      transports += NetworkTransport.ETHERNET
    }
    if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN)) {
      transports += NetworkTransport.VPN
    }
    if (transports.isEmpty()) {
      transports += NetworkTransport.UNKNOWN
    }

    return NetworkStatus(
      isConnected = isConnected,
      transports = transports,
    )
  }

  private fun onObserverStart() {
    synchronized(lock) {
      observerCount++
      if (!isMonitoring) {
        startMonitoringInternal()
      }
    }
  }

  private fun onObserverStop() {
    synchronized(lock) {
      observerCount = (observerCount - 1).coerceAtLeast(0)
      if (observerCount == 0 && isMonitoring) {
        stopMonitoringInternal()
      }
    }
  }

  private fun startMonitoringInternal() {
    if (isMonitoring) return

    val callback = object : ConnectivityManager.NetworkCallback() {
      override fun onAvailable(network: Network) {
      }

      override fun onLost(network: Network) {
        statusState.value = NetworkStatus(isConnected = false)
      }

      override fun onCapabilitiesChanged(
        network: Network,
        networkCapabilities: NetworkCapabilities,
      ) {
        updateStatus(networkCapabilities)
      }
    }

    networkCallback = callback
    connectivityManager.registerDefaultNetworkCallback(callback)
    updateStatus()
    isMonitoring = true
  }

  private fun stopMonitoringInternal() {
    val callback = networkCallback ?: return
    connectivityManager.unregisterNetworkCallback(callback)
    networkCallback = null
    isMonitoring = false
  }

  private fun updateStatus() {
    statusState.value = getNetworkStatus()
  }

  private fun updateStatus(capabilities: NetworkCapabilities?) {
    statusState.value = capabilities.toNetworkStatus()
  }
}
