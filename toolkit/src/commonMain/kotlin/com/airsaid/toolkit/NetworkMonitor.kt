package com.airsaid.toolkit

import kotlinx.coroutines.flow.Flow

/**
 * Defines an interface for monitoring network connectivity status.
 *
 * @author airsaid
 */
interface NetworkMonitor {

  /**
   * Observes changes in the network status as a [Flow].
   *
   * @return A [Flow] that emits [NetworkStatus] whenever the network state changes.
   */
  fun observeNetworkStatus(): Flow<NetworkStatus>

  /**
   * Retrieves the current network status.
   *
   * @return The current [NetworkStatus].
   */
  suspend fun getCurrentNetworkStatus(): NetworkStatus

  /**
   * Starts monitoring the network status.
   */
  @Deprecated(
    message = "Network monitoring now starts automatically while observeNetworkStatus() is collected.",
  )
  fun startMonitoring()

  /**
   * Stops monitoring the network status.
   */
  @Deprecated(
    message = "Network monitoring now stops automatically when observeNetworkStatus() has no collectors.",
  )
  fun stopMonitoring()
}

/**
 * Represents network transports used by the current default network.
 */
enum class NetworkTransport {
  /** Connected via Wi-Fi network. */
  WIFI,

  /** Connected via a mobile cellular network (e.g., 4G, 5G). */
  CELLULAR,

  /** Connected via an Ethernet cable. */
  ETHERNET,

  /** Connected through a VPN tunnel. */
  VPN,

  /** Network type is unknown or cannot be determined. */
  UNKNOWN
}

/**
 * Represents the current network status of the device.
 *
 * @property isConnected Whether the current default network can reach the internet.
 * @property transports The transports used by the current default network.
 */
data class NetworkStatus(

  /** Whether the current default network can reach the internet. */
  val isConnected: Boolean,

  /** The transports used by the current default network. */
  val transports: Set<NetworkTransport> = emptySet(),
) {

  /**
   * The transport most useful for compact UI display.
   */
  val primaryTransport: NetworkTransport?
    get() = resolvePrimaryNetworkTransport(transports)
}

internal fun resolvePrimaryNetworkTransport(
  transports: Set<NetworkTransport>,
): NetworkTransport? {
  return when {
    NetworkTransport.VPN in transports -> NetworkTransport.VPN
    NetworkTransport.WIFI in transports -> NetworkTransport.WIFI
    NetworkTransport.CELLULAR in transports -> NetworkTransport.CELLULAR
    NetworkTransport.ETHERNET in transports -> NetworkTransport.ETHERNET
    NetworkTransport.UNKNOWN in transports -> NetworkTransport.UNKNOWN
    else -> null
  }
}
