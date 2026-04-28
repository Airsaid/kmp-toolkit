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
   * Retrieves the current lifecycle status.
   */
  suspend fun getCurrentStatus(): AppLifecycleStatus

  /**
   * Starts monitoring lifecycle changes.
   */
  fun startMonitoring()

  /**
   * Stops monitoring lifecycle changes.
   */
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
 * @property isFirstLaunch True only for the first foreground entry of the process.
 * @property coldStartCount Number of cold starts in this process.
 * @property hotStartCount Number of hot starts in this process.
 * @property lastStartType The most recent start type, null before any start.
 */
data class AppLifecycleStatus(
  val isInForeground: Boolean,
  val isVisible: Boolean,
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
  ): AppLifecycleStatus {
    val wasForeground = status.isInForeground
    var coldStarts = status.coldStartCount
    var hotStarts = status.hotStartCount
    var lastStartType = status.lastStartType
    var isFirstLaunch = false

    if (!wasForeground && isInForeground) {
      val startType = if (!hasStarted) AppStartType.COLD else AppStartType.HOT
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
    return status
  }
}
