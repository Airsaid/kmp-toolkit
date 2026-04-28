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
  fun startMonitoring()

  /**
   * Stops monitoring the network status.
   */
  fun stopMonitoring()
}

/**
 * Represents different types of network connections.
 */
enum class NetworkType {
  /** Connected via Wi-Fi network. */
  WIFI,

  /** Connected via a mobile cellular network (e.g., 4G, 5G). */
  CELLULAR,

  /** Connected via an Ethernet cable. */
  ETHERNET,

  /** Connected through a VPN tunnel. */
  VPN,

  /** Network type is unknown or cannot be determined. */
  UNKNOWN,

  /** No active network connection. */
  NONE
}

/**
 * Represents the current network status of the device.
 *
 * @property isConnected Whether the device is connected to a network.
 * @property type The type of the active network connection. Defaults to [NetworkType.UNKNOWN].
 */
data class NetworkStatus(

  /** Whether the device has an active network connection. */
  val isConnected: Boolean,

  /** The type of network connection currently in use. */
  val type: NetworkType = NetworkType.UNKNOWN
)
