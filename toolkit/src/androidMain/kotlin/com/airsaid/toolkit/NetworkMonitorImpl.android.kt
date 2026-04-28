package com.airsaid.toolkit

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.conflate
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
        type = NetworkType.NONE,
      )
    )

  private val lock = Any()

  private var networkCallback: ConnectivityManager.NetworkCallback? = null
  private var isMonitoring = false
  private var isManuallyStarted = false
  private var isExplicitlyStopped = false
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
      .conflate()
      .distinctUntilChanged()

  /**
   * Returns the current [NetworkStatus] synchronously.
   */
  override suspend fun getCurrentNetworkStatus(): NetworkStatus {
    return getNetworkStatus()
  }

  /**
   * Starts monitoring network status.
   *
   * On Android, monitoring automatically begins when [observeNetworkStatus] is collected.
   */
  override fun startMonitoring() {
    synchronized(lock) {
      isExplicitlyStopped = false
      isManuallyStarted = true
      if (!isMonitoring) {
        startMonitoringInternal()
      }
    }
  }

  /**
   * Stops monitoring network status by unregistering the callback.
   */
  override fun stopMonitoring() {
    synchronized(lock) {
      isManuallyStarted = false
      isExplicitlyStopped = true
      if (isMonitoring) {
        stopMonitoringInternal()
      }
    }
  }

  /**
   * Gets the current [NetworkStatus] by checking the active network
   * and its [NetworkCapabilities].
   */
  private fun getNetworkStatus(): NetworkStatus {
    val network = connectivityManager.activeNetwork
    val capabilities = connectivityManager.getNetworkCapabilities(network)

    if (capabilities == null) {
      return NetworkStatus(
        isConnected = false,
        type = NetworkType.NONE,
      )
    }

    val isConnected =
      capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
          capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)

    val type = when {
      capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> NetworkType.WIFI
      capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> NetworkType.CELLULAR
      capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> NetworkType.ETHERNET
      capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN) -> NetworkType.VPN
      else -> NetworkType.UNKNOWN
    }

    return NetworkStatus(
      isConnected = isConnected,
      type = if (isConnected) type else NetworkType.NONE,
    )
  }

  private fun onObserverStart() {
    synchronized(lock) {
      observerCount++
      if (!isExplicitlyStopped && !isMonitoring) {
        startMonitoringInternal()
      }
    }
  }

  private fun onObserverStop() {
    synchronized(lock) {
      observerCount = (observerCount - 1).coerceAtLeast(0)
      if (observerCount == 0 && !isManuallyStarted && isMonitoring) {
        stopMonitoringInternal()
      }
    }
  }

  private fun startMonitoringInternal() {
    if (isMonitoring) return

    val callback = object : ConnectivityManager.NetworkCallback() {
      override fun onAvailable(network: Network) {
        updateStatus()
      }

      override fun onLost(network: Network) {
        updateStatus()
      }

      override fun onCapabilitiesChanged(
        network: Network,
        networkCapabilities: NetworkCapabilities,
      ) {
        updateStatus()
      }
    }

    networkCallback = callback

    val request = NetworkRequest.Builder()
      .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
      .build()

    connectivityManager.registerNetworkCallback(request, callback)
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
}
