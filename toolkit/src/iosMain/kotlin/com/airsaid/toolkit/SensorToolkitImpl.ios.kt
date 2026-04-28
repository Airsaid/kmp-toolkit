@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.airsaid.toolkit

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.CoreMotion.CMAccelerometerData
import platform.CoreMotion.CMDeviceMotion
import platform.CoreMotion.CMGyroData
import platform.CoreMotion.CMMagnetometerData
import platform.CoreMotion.CMMotionManager
import platform.Foundation.NSLock
import platform.Foundation.NSNotification
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSOperationQueue
import platform.Foundation.NSProcessInfo
import platform.UIKit.UIDevice
import platform.UIKit.UIDeviceProximityStateDidChangeNotification
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.CValue
import kotlinx.cinterop.useContents
import kotlin.coroutines.resume

/**
 * iOS implementation of [SensorToolkit] using CoreMotion.
 */
internal class SensorToolkitImpl : SensorToolkit {

  private val motionManager = CMMotionManager()
  private val queue = NSOperationQueue()
  private val motionHub = DeviceMotionHub(motionManager, queue)
  private val streams = mutableMapOf<SensorType, SensorStream>()

  override fun isAvailable(type: SensorType): SensorAvailability {
    val available = when (type) {
      SensorType.ACCELEROMETER -> motionManager.accelerometerAvailable
      SensorType.GYROSCOPE -> motionManager.gyroAvailable
      SensorType.MAGNETOMETER -> motionManager.magnetometerAvailable
      SensorType.DEVICE_MOTION -> motionManager.deviceMotionAvailable
      SensorType.GRAVITY -> motionManager.deviceMotionAvailable
      SensorType.LINEAR_ACCELERATION -> motionManager.deviceMotionAvailable
      SensorType.ROTATION_VECTOR -> motionManager.deviceMotionAvailable
      SensorType.PROXIMITY -> isProximityAvailable()
      SensorType.BAROMETER,
      SensorType.STEP_COUNTER,
      SensorType.STEP_DETECTOR,
      SensorType.LIGHT,
      SensorType.GAME_ROTATION_VECTOR,
      SensorType.GEOMAGNETIC_ROTATION_VECTOR,
      SensorType.TILT_DETECTOR,
      SensorType.SIGNIFICANT_MOTION,
      SensorType.MOTION_DETECT,
      SensorType.STATIONARY_DETECT,
      SensorType.AMBIENT_TEMPERATURE,
      SensorType.RELATIVE_HUMIDITY,
      -> false
    }

    return if (available) {
      SensorAvailability(isAvailable = true)
    } else {
      SensorAvailability(
        isAvailable = false,
        reason = "Sensor not available on this device.",
      )
    }
  }

  override fun observe(
    type: SensorType,
    options: SensorOptions,
  ): Flow<SensorEvent> {
    return streamFor(type).observe(options)
  }

  override suspend fun getSnapshot(type: SensorType): SensorEvent? {
    return streamFor(type).getSnapshot()
  }

  override fun stop(type: SensorType) {
    streamFor(type).stop()
  }

  private fun streamFor(type: SensorType): SensorStream {
    return streams.getOrPut(type) {
      SensorStream(type, motionManager, queue, motionHub)
    }
  }

  private fun isProximityAvailable(): Boolean {
    val device = UIDevice.currentDevice
    val wasEnabled = device.proximityMonitoringEnabled
    device.proximityMonitoringEnabled = true
    val available = device.proximityMonitoringEnabled
    device.proximityMonitoringEnabled = wasEnabled
    return available
  }
}

private class SensorStream(
  private val type: SensorType,
  private val motionManager: CMMotionManager,
  private val queue: NSOperationQueue,
  private val motionHub: DeviceMotionHub,
) {

  private val events = MutableSharedFlow<SensorEvent>(
    extraBufferCapacity = 64,
  )

  private val lock = NSLock()

  private var observerCount = 0
  private var isMonitoring = false
  private var isExplicitlyStopped = false
  private var activeOptions: SensorOptions? = null
  private var lastEvent: SensorEvent? = null
  private var proximityObserver: Any? = null
  private var isSnapshotInProgress = false
  private val snapshotWaiters = mutableListOf<(SensorEvent) -> Unit>()

  fun observe(options: SensorOptions): Flow<SensorEvent> {
    return events
      .onStart { onObserverStart(options) }
      .onCompletion { onObserverStop() }
      .conflate()
  }

  suspend fun getSnapshot(): SensorEvent? {
    val cached = lastEvent
    if (cached != null) return cached

    if (!type.isSupportedOnIos()) return null

    return suspendCancellableCoroutine { continuation ->
      val waiter: (SensorEvent) -> Unit = { event ->
        continuation.resume(event)
      }

      var shouldStartSnapshot = false
      var isCompleted = false
      withLock {
        val cachedEvent = lastEvent
        if (cachedEvent != null) {
          continuation.resume(cachedEvent)
          isCompleted = true
          return@withLock
        }

        snapshotWaiters.add(waiter)
        if (isMonitoring) {
          return@withLock
        }

        if (isSnapshotInProgress) {
          return@withLock
        }

        isSnapshotInProgress = true
        shouldStartSnapshot = true
      }

      if (isCompleted) {
        return@suspendCancellableCoroutine
      }

      if (shouldStartSnapshot) {
        val intervalSeconds = (activeOptions ?: SensorOptions()).toUpdateIntervalSeconds()
        startUpdates(intervalSeconds) { event ->
          lastEvent = event
          val callbacks = drainSnapshotWaiters()
          callbacks.forEach { it(event) }
          stopUpdates()
          withLock {
            isSnapshotInProgress = false
          }
        }
      }

      continuation.invokeOnCancellation {
        val shouldStopSnapshot = withLock {
          snapshotWaiters.remove(waiter)
          if (!isMonitoring && isSnapshotInProgress && snapshotWaiters.isEmpty()) {
            isSnapshotInProgress = false
            return@withLock true
          }
          false
        }

        if (shouldStopSnapshot) {
          stopUpdates()
        }
      }
    }
  }

  fun stop() {
    withLock {
      isExplicitlyStopped = true
      if (isMonitoring) {
        stopMonitoringInternal()
      }
    }
  }

  private fun onObserverStart(options: SensorOptions) {
    withLock {
      observerCount++
      isExplicitlyStopped = false
      val shouldRestart = activeOptions != null && activeOptions != options
      activeOptions = options
      if (!isMonitoring) {
        startMonitoringInternal(options)
      } else if (shouldRestart) {
        stopMonitoringInternal()
        startMonitoringInternal(options)
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

  private fun startMonitoringInternal(options: SensorOptions) {
    if (isMonitoring || isExplicitlyStopped) return
    if (!type.isSupportedOnIos()) return

    val intervalSeconds = options.toUpdateIntervalSeconds()
    startUpdates(intervalSeconds) { event ->
      lastEvent = event
      val callbacks = drainSnapshotWaiters()
      callbacks.forEach { it(event) }
      events.tryEmit(event)
    }
    isMonitoring = true
  }

  private fun stopMonitoringInternal() {
    stopUpdates()
    isMonitoring = false
  }

  private fun startUpdates(
    intervalSeconds: Double,
    onEvent: (SensorEvent) -> Unit,
  ) {
    when (type) {
      SensorType.ACCELEROMETER -> {
        motionManager.accelerometerUpdateInterval = intervalSeconds
        motionManager.startAccelerometerUpdatesToQueue(queue) { data, _ ->
          data?.toSensorEvent(type)?.let(onEvent)
        }
      }
      SensorType.GYROSCOPE -> {
        motionManager.gyroUpdateInterval = intervalSeconds
        motionManager.startGyroUpdatesToQueue(queue) { data, _ ->
          data?.toSensorEvent(type)?.let(onEvent)
        }
      }
      SensorType.MAGNETOMETER -> {
        motionManager.magnetometerUpdateInterval = intervalSeconds
        motionManager.startMagnetometerUpdatesToQueue(queue) { data, _ ->
          data?.toSensorEvent(type)?.let(onEvent)
        }
      }
      SensorType.DEVICE_MOTION,
      SensorType.GRAVITY,
      SensorType.LINEAR_ACCELERATION,
      SensorType.ROTATION_VECTOR,
      -> motionHub.start(type, intervalSeconds, onEvent)
      SensorType.PROXIMITY -> startProximityUpdates(onEvent)
      SensorType.BAROMETER,
      SensorType.STEP_COUNTER,
      SensorType.STEP_DETECTOR,
      SensorType.LIGHT,
      SensorType.GAME_ROTATION_VECTOR,
      SensorType.GEOMAGNETIC_ROTATION_VECTOR,
      SensorType.TILT_DETECTOR,
      SensorType.SIGNIFICANT_MOTION,
      SensorType.MOTION_DETECT,
      SensorType.STATIONARY_DETECT,
      SensorType.AMBIENT_TEMPERATURE,
      SensorType.RELATIVE_HUMIDITY,
      -> Unit
    }
  }

  private fun stopUpdates() {
    when (type) {
      SensorType.ACCELEROMETER -> motionManager.stopAccelerometerUpdates()
      SensorType.GYROSCOPE -> motionManager.stopGyroUpdates()
      SensorType.MAGNETOMETER -> motionManager.stopMagnetometerUpdates()
      SensorType.DEVICE_MOTION,
      SensorType.GRAVITY,
      SensorType.LINEAR_ACCELERATION,
      SensorType.ROTATION_VECTOR,
      -> motionHub.stop(type)
      SensorType.PROXIMITY -> stopProximityUpdates()
      SensorType.BAROMETER,
      SensorType.STEP_COUNTER,
      SensorType.STEP_DETECTOR,
      SensorType.LIGHT,
      SensorType.GAME_ROTATION_VECTOR,
      SensorType.GEOMAGNETIC_ROTATION_VECTOR,
      SensorType.TILT_DETECTOR,
      SensorType.SIGNIFICANT_MOTION,
      SensorType.MOTION_DETECT,
      SensorType.STATIONARY_DETECT,
      SensorType.AMBIENT_TEMPERATURE,
      SensorType.RELATIVE_HUMIDITY,
      -> Unit
    }
  }

  private fun startProximityUpdates(onEvent: (SensorEvent) -> Unit) {
    val device = UIDevice.currentDevice
    device.proximityMonitoringEnabled = true

    val center = NSNotificationCenter.defaultCenter
    proximityObserver = center.addObserverForName(
      name = UIDeviceProximityStateDidChangeNotification,
      `object` = null,
      queue = null,
      usingBlock = { _: NSNotification? ->
        emitProximityState(onEvent)
      },
    )

    emitProximityState(onEvent)
  }

  private fun stopProximityUpdates() {
    proximityObserver?.let { observer ->
      NSNotificationCenter.defaultCenter.removeObserver(observer)
    }
    proximityObserver = null
    UIDevice.currentDevice.proximityMonitoringEnabled = false
  }

  private fun emitProximityState(onEvent: (SensorEvent) -> Unit) {
    val isClose = UIDevice.currentDevice.proximityState
    onEvent(
      SensorEvent(
        type = type,
        values = floatArrayOf(if (isClose) 1f else 0f),
        timestampNanos = currentTimestampNanos(),
        accuracy = SensorAccuracy.UNRELIABLE,
      )
    )
  }

  private fun drainSnapshotWaiters(): List<(SensorEvent) -> Unit> {
    return withLock {
      if (snapshotWaiters.isEmpty()) {
        return@withLock emptyList()
      }
      isSnapshotInProgress = false
      val callbacks = snapshotWaiters.toList()
      snapshotWaiters.clear()
      callbacks
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

private class DeviceMotionHub(
  private val motionManager: CMMotionManager,
  private val queue: NSOperationQueue,
) {

  private val lock = NSLock()
  private val observers = mutableMapOf<SensorType, (SensorEvent) -> Unit>()
  private val intervals = mutableMapOf<SensorType, Double>()
  private var isMonitoring = false

  fun start(
    type: SensorType,
    intervalSeconds: Double,
    onEvent: (SensorEvent) -> Unit,
  ) {
    withLock {
      observers[type] = onEvent
      intervals[type] = intervalSeconds
      val minInterval = intervals.values.minOrNull() ?: intervalSeconds
      motionManager.deviceMotionUpdateInterval = minInterval
      if (!isMonitoring) {
        motionManager.startDeviceMotionUpdatesToQueue(queue) { data, _ ->
          data?.let { dispatch(it) }
        }
        isMonitoring = true
      }
    }
  }

  fun stop(type: SensorType) {
    withLock {
      observers.remove(type)
      intervals.remove(type)
      if (observers.isEmpty()) {
        motionManager.stopDeviceMotionUpdates()
        isMonitoring = false
      } else {
        val minInterval = intervals.values.minOrNull()
        if (minInterval != null) {
          motionManager.deviceMotionUpdateInterval = minInterval
        }
      }
    }
  }

  private fun dispatch(data: CMDeviceMotion) {
    val snapshot = observers.toMap()
    snapshot.forEach { (type, callback) ->
      callback(data.toSensorEvent(type))
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

private fun SensorType.isSupportedOnIos(): Boolean {
  return when (this) {
    SensorType.ACCELEROMETER,
    SensorType.GYROSCOPE,
    SensorType.MAGNETOMETER,
    SensorType.DEVICE_MOTION,
    SensorType.GRAVITY,
    SensorType.LINEAR_ACCELERATION,
    SensorType.ROTATION_VECTOR,
    SensorType.PROXIMITY,
    -> true
    SensorType.BAROMETER,
    SensorType.STEP_COUNTER,
    SensorType.STEP_DETECTOR,
    SensorType.LIGHT,
    SensorType.GAME_ROTATION_VECTOR,
    SensorType.GEOMAGNETIC_ROTATION_VECTOR,
    SensorType.TILT_DETECTOR,
    SensorType.SIGNIFICANT_MOTION,
    SensorType.MOTION_DETECT,
    SensorType.STATIONARY_DETECT,
    SensorType.AMBIENT_TEMPERATURE,
    SensorType.RELATIVE_HUMIDITY,
    -> false
  }
}

private fun SensorOptions.toUpdateIntervalSeconds(): Double {
  val rate = samplingRateHz
  if (rate != null && rate > 0) {
    return 1.0 / rate.toDouble()
  }

  return when (delay) {
    SensorDelay.FASTEST -> 0.01
    SensorDelay.GAME -> 0.02
    SensorDelay.UI -> 0.05
    SensorDelay.NORMAL -> 0.2
  }
}

private fun CMAccelerometerData.toSensorEvent(type: SensorType): SensorEvent {
  val values = acceleration.toFloatArray()
  return SensorEvent(
    type = type,
    values = values,
    timestampNanos = (timestamp * 1_000_000_000.0).toLong(),
    accuracy = SensorAccuracy.UNRELIABLE,
  )
}

private fun CMGyroData.toSensorEvent(type: SensorType): SensorEvent {
  val values = rotationRate.toFloatArray()
  return SensorEvent(
    type = type,
    values = values,
    timestampNanos = (timestamp * 1_000_000_000.0).toLong(),
    accuracy = SensorAccuracy.UNRELIABLE,
  )
}

private fun CMMagnetometerData.toSensorEvent(type: SensorType): SensorEvent {
  val values = magneticField.toFloatArray()
  return SensorEvent(
    type = type,
    values = values,
    timestampNanos = (timestamp * 1_000_000_000.0).toLong(),
    accuracy = SensorAccuracy.UNRELIABLE,
  )
}

private fun CMDeviceMotion.toSensorEvent(type: SensorType): SensorEvent {
  return when (type) {
    SensorType.GRAVITY -> {
      SensorEvent(
        type = type,
        values = gravity.toFloatArray(),
        timestampNanos = (timestamp * 1_000_000_000.0).toLong(),
        accuracy = SensorAccuracy.UNRELIABLE,
      )
    }
    SensorType.LINEAR_ACCELERATION -> {
      SensorEvent(
        type = type,
        values = userAcceleration.toFloatArray(),
        timestampNanos = (timestamp * 1_000_000_000.0).toLong(),
        accuracy = SensorAccuracy.UNRELIABLE,
      )
    }
    SensorType.ROTATION_VECTOR -> {
      SensorEvent(
        type = type,
        values = attitude.quaternion.toFloatArray(),
        timestampNanos = (timestamp * 1_000_000_000.0).toLong(),
        accuracy = SensorAccuracy.UNRELIABLE,
      )
    }
    else -> {
      val attitude = attitude
      SensorEvent(
        type = type,
        values = floatArrayOf(
          attitude.roll.toFloat(),
          attitude.pitch.toFloat(),
          attitude.yaw.toFloat(),
        ),
        timestampNanos = (timestamp * 1_000_000_000.0).toLong(),
        accuracy = SensorAccuracy.UNRELIABLE,
      )
    }
  }
}

private fun currentTimestampNanos(): Long {
  return (NSProcessInfo.processInfo.systemUptime * 1_000_000_000.0).toLong()
}

private fun CValue<platform.CoreMotion.CMAcceleration>.toFloatArray(): FloatArray {
  return useContents {
    floatArrayOf(
      x.toFloat(),
      y.toFloat(),
      z.toFloat(),
    )
  }
}

private fun CValue<platform.CoreMotion.CMRotationRate>.toFloatArray(): FloatArray {
  return useContents {
    floatArrayOf(
      x.toFloat(),
      y.toFloat(),
      z.toFloat(),
    )
  }
}

private fun CValue<platform.CoreMotion.CMMagneticField>.toFloatArray(): FloatArray {
  return useContents {
    floatArrayOf(
      x.toFloat(),
      y.toFloat(),
      z.toFloat(),
    )
  }
}

private fun CValue<platform.CoreMotion.CMQuaternion>.toFloatArray(): FloatArray {
  return useContents {
    floatArrayOf(
      x.toFloat(),
      y.toFloat(),
      z.toFloat(),
      w.toFloat(),
    )
  }
}
