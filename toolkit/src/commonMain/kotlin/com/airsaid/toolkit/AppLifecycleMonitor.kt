package com.airsaid.toolkit

import kotlinx.coroutines.flow.Flow

/**
 * Monitors app lifecycle changes across platforms.
 */
interface AppLifecycleMonitor {

  /**
   * Observes lifecycle status updates as a [Flow].
   */
  fun observeAppLifecycle(): Flow<AppLifecycleStatus>

  /**
   * Observes app launch transitions as a [Flow].
   *
   * Emits a one-shot [AppStartType] when the app enters the foreground.
   * This flow does not replay previous launch events to new collectors.
   */
  fun observeAppStartEvents(): Flow<AppStartType>

  /**
   * Retrieves the current lifecycle status.
   */
  suspend fun getCurrentStatus(): AppLifecycleStatus
}

/**
 * Indicates whether the app launch is a cold or hot start.
 */
enum class AppStartType {
  COLD,
  HOT,
}

/**
 * Represents the current lifecycle status of the app.
 *
 * @property isInForeground True when the app is interactive.
 * @property isVisible True when the app is visible to the user.
 * @property lastStartType The most recent start type, null before any start.
 */
data class AppLifecycleStatus(
  val isInForeground: Boolean,
  val isVisible: Boolean,
  val lastStartType: AppStartType?,
)

/**
 * Factory object for creating [AppLifecycleMonitor] instances.
 */
internal expect object AppLifecycleMonitorFactory {

  /**
   * Creates a platform-specific [AppLifecycleMonitor] instance.
   */
  fun create(): AppLifecycleMonitor
}

internal class AppLifecycleStateTracker(
  initialStatus: AppLifecycleStatus = AppLifecycleStatus(
    isInForeground = false,
    isVisible = false,
    lastStartType = null,
  ),
) {

  private var status: AppLifecycleStatus = initialStatus
  private var hasStarted: Boolean = initialStatus.lastStartType != null

  val currentStatus: AppLifecycleStatus
    get() = status

  fun update(
    isInForeground: Boolean,
    isVisible: Boolean,
  ): AppLifecycleUpdate {
    val wasForeground = status.isInForeground
    var lastStartType = status.lastStartType
    var startType: AppStartType? = null

    if (!wasForeground && isInForeground) {
      startType = if (!hasStarted) AppStartType.COLD else AppStartType.HOT
      lastStartType = startType
      hasStarted = true
    }

    status = AppLifecycleStatus(
      isInForeground = isInForeground,
      isVisible = isVisible,
      lastStartType = lastStartType,
    )
    return AppLifecycleUpdate(
      status = status,
      startType = startType,
    )
  }
}

internal data class AppLifecycleUpdate(
  val status: AppLifecycleStatus,
  val startType: AppStartType?,
)
