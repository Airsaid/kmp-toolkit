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

  /**
   * Starts monitoring lifecycle changes.
   */
  @Deprecated(
    message = "Lifecycle monitoring now starts automatically while observeAppLifecycle() or " +
      "observeAppStartEvents() is collected.",
  )
  fun startMonitoring()

  /**
   * Stops monitoring lifecycle changes.
   */
  @Deprecated(
    message = "Lifecycle monitoring now stops automatically when observeAppLifecycle() and " +
      "observeAppStartEvents() have no collectors.",
  )
  fun stopMonitoring()
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
 * @property isFirstLaunch Legacy transient flag for the first foreground entry.
 * Prefer [AppLifecycleMonitor.observeAppStartEvents] for one-shot launch handling.
 * @property coldStartCount Number of cold starts in this process.
 * @property hotStartCount Number of hot starts in this process.
 * @property lastStartType The most recent start type, null before any start.
 */
data class AppLifecycleStatus(
  val isInForeground: Boolean,
  val isVisible: Boolean,
  @Deprecated(
    message = "Use observeAppStartEvents() for one-shot launch handling.",
  )
  val isFirstLaunch: Boolean,
  val coldStartCount: Int,
  val hotStartCount: Int,
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

@Suppress("DEPRECATION")
internal class AppLifecycleStateTracker(
  initialStatus: AppLifecycleStatus = AppLifecycleStatus(
    isInForeground = false,
    isVisible = false,
    isFirstLaunch = false,
    coldStartCount = 0,
    hotStartCount = 0,
    lastStartType = null,
  ),
) {

  private var status: AppLifecycleStatus = initialStatus
  private var hasStarted: Boolean =
    initialStatus.coldStartCount > 0 || initialStatus.hotStartCount > 0

  val currentStatus: AppLifecycleStatus
    get() = status

  fun update(
    isInForeground: Boolean,
    isVisible: Boolean,
  ): AppLifecycleUpdate {
    val wasForeground = status.isInForeground
    var coldStarts = status.coldStartCount
    var hotStarts = status.hotStartCount
    var lastStartType = status.lastStartType
    var isFirstLaunch = false
    var startType: AppStartType? = null

    if (!wasForeground && isInForeground) {
      startType = if (!hasStarted) AppStartType.COLD else AppStartType.HOT
      if (startType == AppStartType.COLD) {
        coldStarts += 1
      } else {
        hotStarts += 1
      }
      lastStartType = startType
      hasStarted = true
      isFirstLaunch = startType == AppStartType.COLD && coldStarts == 1
    }

    status = AppLifecycleStatus(
      isInForeground = isInForeground,
      isVisible = isVisible,
      isFirstLaunch = isFirstLaunch,
      coldStartCount = coldStarts,
      hotStartCount = hotStarts,
      lastStartType = lastStartType,
    )
    return AppLifecycleUpdate(
      status = status,
      startType = startType,
    )
  }

  fun clearFirstLaunchFlag() {
    if (!status.isFirstLaunch) return
    status = status.copy(isFirstLaunch = false)
  }
}

internal data class AppLifecycleUpdate(
  val status: AppLifecycleStatus,
  val startType: AppStartType?,
)
