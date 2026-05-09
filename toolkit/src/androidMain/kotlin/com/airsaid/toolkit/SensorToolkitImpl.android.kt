package com.airsaid.toolkit

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.hardware.TriggerEvent
import android.hardware.TriggerEventListener
import android.os.Build
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow

/**
 * Android implementation of [SensorToolkit].
 */
internal class SensorToolkitImpl(
  context: Context,
) : SensorToolkit {

  private val applicationContext = context.applicationContext ?: context

  private val sensorManager =
    applicationContext.getSystemService(Context.SENSOR_SERVICE) as SensorManager

  private val streamLock = Any()
  private val streams = mutableMapOf<SensorType, SensorStream>()

  override fun isAvailable(type: SensorType): SensorAvailability {
    if (resolveSensor(type) == null) {
      return SensorAvailability(
        isAvailable = false,
        reason = "Sensor not available on this device.",
      )
    }

    val requiredPermission = type.requiredAndroidPermission()
    if (
      requiredPermission != null &&
      applicationContext.checkSelfPermission(requiredPermission) !=
      PackageManager.PERMISSION_GRANTED
    ) {
      return SensorAvailability(
        isAvailable = false,
        reason = "Missing required permission.",
        requiredPermission = requiredPermission,
      )
    }

    return SensorAvailability(isAvailable = true)
  }

  override fun observe(
    type: SensorType,
    options: SensorOptions,
  ): Flow<SensorEvent> {
    if (!isAvailable(type).isAvailable) return emptyFlow()
    return streamFor(type).observe(options)
  }

  override suspend fun getSnapshot(
    type: SensorType,
    options: SensorOptions,
  ): SensorEvent? {
    if (!isAvailable(type).isAvailable) return null
    return streamFor(type).getSnapshot(options)
  }

  private fun streamFor(type: SensorType): SensorStream {
    return synchronized(streamLock) {
      streams.getOrPut(type) {
        SensorStream(type, sensorManager)
      }
    }
  }

  private fun resolveSensor(type: SensorType): Sensor? {
    val sensorType = type.toAndroidSensorType().takeIf { it != SENSOR_TYPE_UNSUPPORTED }
      ?: return null
    return sensorManager.getDefaultSensor(sensorType)
  }
}

private class SensorStream(
  private val type: SensorType,
  private val sensorManager: SensorManager,
) {

  private val events = MutableSharedFlow<SensorEvent>(
    extraBufferCapacity = 64,
  )

  private val lock = Any()

  private val observerOptions = mutableMapOf<Long, SensorOptions>()
  private var nextObserverId = 0L
  private var isMonitoring = false
  private var activeOptions: SensorOptions? = null
  private var lastEvent: SensorEvent? = null
  private var listener: SensorEventListener? = null
  private var triggerListener: TriggerEventListener? = null

  fun observe(options: SensorOptions): Flow<SensorEvent> {
    return flow {
      val observerId = onObserverStart(options)
      try {
        val cached = synchronized(lock) { lastEvent }
        cached?.let { emit(it) }
        if (cached == null && synchronized(lock) { !isMonitoring }) return@flow
        emitAll(events)
      } finally {
        onObserverStop(observerId)
      }
    }.conflate()
  }

  suspend fun getSnapshot(options: SensorOptions): SensorEvent? {
    val cached = synchronized(lock) { lastEvent }
    if (cached != null) return cached

    return observe(options).firstOrNull()
  }

  private fun onObserverStart(options: SensorOptions): Long {
    synchronized(lock) {
      val observerId = nextObserverId++
      observerOptions[observerId] = options
      restartMonitoringIfNeeded()
      return observerId
    }
  }

  private fun onObserverStop(observerId: Long) {
    synchronized(lock) {
      observerOptions.remove(observerId)
      if (observerOptions.isEmpty()) {
        if (isMonitoring) {
          stopMonitoringInternal()
        }
        activeOptions = null
      } else {
        restartMonitoringIfNeeded()
      }
    }
  }

  private fun restartMonitoringIfNeeded() {
    val nextOptions = observerOptions.values.effectiveOptions()
    if (nextOptions == null) {
      return
    }

    if (!isMonitoring) {
      activeOptions = nextOptions
      startMonitoringInternal(nextOptions)
      return
    }

    if (activeOptions != nextOptions) {
      stopMonitoringInternal()
      activeOptions = nextOptions
      startMonitoringInternal(nextOptions)
    }
  }

  private fun startMonitoringInternal(options: SensorOptions) {
    if (isMonitoring) return

    val sensor = resolveSensor() ?: return
    val samplingPeriodUs = options.toSamplingPeriodUs()
    val maxReportLatencyUs = options.toMaxReportLatencyUs()

    if (type.isTriggerSensor()) {
      val newTriggerListener = object : TriggerEventListener() {
        override fun onTrigger(event: TriggerEvent?) {
          val mapped = event?.toSensorEvent(type) ?: return
          synchronized(lock) {
            lastEvent = mapped
          }
          events.tryEmit(mapped)
          synchronized(lock) {
            if (isMonitoring) {
              isMonitoring = try {
                sensorManager.requestTriggerSensor(this, sensor)
              } catch (_: SecurityException) {
                false
              }
            }
          }
        }
      }

      triggerListener = newTriggerListener
      isMonitoring = try {
        sensorManager.requestTriggerSensor(newTriggerListener, sensor)
      } catch (_: SecurityException) {
        false
      }
      if (!isMonitoring) {
        triggerListener = null
      }
      return
    }

    val newListener = object : SensorEventListener {
      override fun onSensorChanged(event: android.hardware.SensorEvent) {
        val mapped = event.toSensorEvent(type)
        synchronized(lock) {
          lastEvent = mapped
        }
        events.tryEmit(mapped)
      }

      override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
    }

    listener = newListener

    isMonitoring = registerListener(
      listener = newListener,
      sensor = sensor,
      samplingPeriodUs = samplingPeriodUs,
      maxReportLatencyUs = maxReportLatencyUs,
    )
    if (!isMonitoring) {
      listener = null
    }
  }

  private fun stopMonitoringInternal() {
    triggerListener?.let { currentTriggerListener ->
      val sensor = sensorManager.getDefaultSensor(type.toAndroidSensorType())
      if (sensor != null) {
        sensorManager.cancelTriggerSensor(currentTriggerListener, sensor)
      }
    }
    triggerListener = null

    listener?.let { currentListener ->
      sensorManager.unregisterListener(currentListener)
    }
    listener = null
    isMonitoring = false
  }

  private fun resolveSensor(): Sensor? {
    val sensorType = type.toAndroidSensorType().takeIf { it != SENSOR_TYPE_UNSUPPORTED }
      ?: return null
    return sensorManager.getDefaultSensor(sensorType)
  }

  private fun registerListener(
    listener: SensorEventListener,
    sensor: Sensor,
    samplingPeriodUs: Int,
    maxReportLatencyUs: Int,
  ): Boolean {
    return try {
      if (maxReportLatencyUs > 0) {
        sensorManager.registerListener(
          listener,
          sensor,
          samplingPeriodUs,
          maxReportLatencyUs,
        )
      } else {
        sensorManager.registerListener(
          listener,
          sensor,
          samplingPeriodUs,
        )
      }
    } catch (_: SecurityException) {
      false
    }
  }
}

private fun SensorType.toAndroidSensorType(): Int {
  return when (this) {
    SensorType.ACCELEROMETER -> Sensor.TYPE_ACCELEROMETER
    SensorType.GYROSCOPE -> Sensor.TYPE_GYROSCOPE
    SensorType.MAGNETOMETER -> Sensor.TYPE_MAGNETIC_FIELD
    SensorType.BAROMETER -> Sensor.TYPE_PRESSURE
    SensorType.STEP_COUNTER -> Sensor.TYPE_STEP_COUNTER
    SensorType.STEP_DETECTOR -> Sensor.TYPE_STEP_DETECTOR
    SensorType.DEVICE_MOTION -> Sensor.TYPE_ROTATION_VECTOR
    SensorType.LIGHT -> Sensor.TYPE_LIGHT
    SensorType.PROXIMITY -> Sensor.TYPE_PROXIMITY
    SensorType.GRAVITY -> Sensor.TYPE_GRAVITY
    SensorType.LINEAR_ACCELERATION -> Sensor.TYPE_LINEAR_ACCELERATION
    SensorType.ROTATION_VECTOR -> Sensor.TYPE_ROTATION_VECTOR
    SensorType.GAME_ROTATION_VECTOR -> Sensor.TYPE_GAME_ROTATION_VECTOR
    SensorType.GEOMAGNETIC_ROTATION_VECTOR -> Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR
    SensorType.TILT_DETECTOR -> SENSOR_TYPE_UNSUPPORTED
    SensorType.SIGNIFICANT_MOTION -> Sensor.TYPE_SIGNIFICANT_MOTION
    SensorType.MOTION_DETECT -> Sensor.TYPE_MOTION_DETECT
    SensorType.STATIONARY_DETECT -> Sensor.TYPE_STATIONARY_DETECT
    SensorType.AMBIENT_TEMPERATURE -> Sensor.TYPE_AMBIENT_TEMPERATURE
    SensorType.RELATIVE_HUMIDITY -> Sensor.TYPE_RELATIVE_HUMIDITY
  }
}

private fun SensorType.requiredAndroidPermission(): String? {
  return when (this) {
    SensorType.STEP_COUNTER,
    SensorType.STEP_DETECTOR,
    -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
      Manifest.permission.ACTIVITY_RECOGNITION
    } else {
      null
    }
    else -> null
  }
}

private const val SENSOR_TYPE_UNSUPPORTED = -1

private fun SensorType.isTriggerSensor(): Boolean {
  return when (this) {
    SensorType.SIGNIFICANT_MOTION,
    SensorType.TILT_DETECTOR,
    SensorType.MOTION_DETECT,
    SensorType.STATIONARY_DETECT,
    -> true
    else -> false
  }
}

private fun SensorOptions.toSamplingPeriodUs(): Int {
  val rate = samplingRateHz
  if (rate != null && rate > 0) {
    return (1_000_000 / rate).coerceAtLeast(1)
  }

  return when (delay) {
    SensorDelay.FASTEST -> SensorManager.SENSOR_DELAY_FASTEST
    SensorDelay.GAME -> SensorManager.SENSOR_DELAY_GAME
    SensorDelay.UI -> SensorManager.SENSOR_DELAY_UI
    SensorDelay.NORMAL -> SensorManager.SENSOR_DELAY_NORMAL
  }
}

private fun SensorOptions.toMaxReportLatencyUs(): Int {
  return maxReportLatencyMillis
    .coerceAtMost(Int.MAX_VALUE / 1_000L)
    .times(1_000L)
    .toInt()
}

private fun Collection<SensorOptions>.effectiveOptions(): SensorOptions? {
  if (isEmpty()) return null

  return reduce { current, next ->
    val currentSamplingPeriodUs = current.toSamplingPeriodUs()
    val nextSamplingPeriodUs = next.toSamplingPeriodUs()
    val fastest = if (nextSamplingPeriodUs < currentSamplingPeriodUs) next else current

    fastest.copy(
      maxReportLatencyMillis = minOf(
        current.maxReportLatencyMillis,
        next.maxReportLatencyMillis,
      ),
    )
  }
}

private fun android.hardware.SensorEvent.toSensorEvent(type: SensorType): SensorEvent {
  return SensorEvent(
    type = type,
    values = values.toList(),
    timestampNanos = timestamp,
    accuracy = accuracy.toSensorAccuracy(),
  )
}

private fun TriggerEvent.toSensorEvent(type: SensorType): SensorEvent {
  return SensorEvent(
    type = type,
    values = values.toList(),
    timestampNanos = timestamp,
    accuracy = SensorAccuracy.UNRELIABLE,
  )
}

private fun Int.toSensorAccuracy(): SensorAccuracy {
  return when (this) {
    SensorManager.SENSOR_STATUS_ACCURACY_HIGH -> SensorAccuracy.HIGH
    SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM -> SensorAccuracy.MEDIUM
    SensorManager.SENSOR_STATUS_ACCURACY_LOW -> SensorAccuracy.LOW
    else -> SensorAccuracy.UNRELIABLE
  }
}
