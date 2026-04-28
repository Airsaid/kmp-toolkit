package com.airsaid.toolkit

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.hardware.TriggerEvent
import android.hardware.TriggerEventListener
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Android implementation of [SensorToolkit].
 */
internal class SensorToolkitImpl(
  context: Context,
) : SensorToolkit {

  private val sensorManager =
    context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

  private val streams = mutableMapOf<SensorType, SensorStream>()

  override fun isAvailable(type: SensorType): SensorAvailability {
    return if (resolveSensor(type) != null) {
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
      SensorStream(type, sensorManager)
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

  private var observerCount = 0
  private var isMonitoring = false
  private var isExplicitlyStopped = false
  private var activeOptions: SensorOptions? = null
  private var lastEvent: SensorEvent? = null
  private var listener: SensorEventListener? = null
  private var triggerListener: TriggerEventListener? = null

  fun observe(options: SensorOptions): Flow<SensorEvent> {
    return events
      .onStart { onObserverStart(options) }
      .onCompletion { onObserverStop() }
      .conflate()
  }

  suspend fun getSnapshot(): SensorEvent? {
    val cached = lastEvent
    if (cached != null) return cached

    val sensor = resolveSensor() ?: return null
    val options = activeOptions ?: SensorOptions()
    val samplingPeriodUs = options.toSamplingPeriodUs()

    return suspendCancellableCoroutine { continuation ->
      if (type.isTriggerSensor()) {
        val oneShotTriggerListener = object : TriggerEventListener() {
          override fun onTrigger(event: TriggerEvent?) {
            val mapped = event?.toSensorEvent(type)
            if (mapped != null) {
              lastEvent = mapped
              continuation.resume(mapped)
            } else {
              continuation.resume(null)
            }
          }
        }

        sensorManager.requestTriggerSensor(oneShotTriggerListener, sensor)

        continuation.invokeOnCancellation {
          sensorManager.cancelTriggerSensor(oneShotTriggerListener, sensor)
        }
      } else {
        val oneShotListener = object : SensorEventListener {
          override fun onSensorChanged(event: android.hardware.SensorEvent) {
            val mapped = event.toSensorEvent(type)
            lastEvent = mapped
            sensorManager.unregisterListener(this, sensor)
            continuation.resume(mapped)
          }

          override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }

        registerListener(
          listener = oneShotListener,
          sensor = sensor,
          samplingPeriodUs = samplingPeriodUs,
          batchEnabled = options.batchEnabled,
        )

        continuation.invokeOnCancellation {
          sensorManager.unregisterListener(oneShotListener, sensor)
        }
      }
    }
  }

  fun stop() {
    synchronized(lock) {
      isExplicitlyStopped = true
      if (isMonitoring) {
        stopMonitoringInternal()
      }
    }
  }

  private fun onObserverStart(options: SensorOptions) {
    synchronized(lock) {
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
    synchronized(lock) {
      observerCount = (observerCount - 1).coerceAtLeast(0)
      if (observerCount == 0 && isMonitoring) {
        stopMonitoringInternal()
      }
    }
  }

  private fun startMonitoringInternal(options: SensorOptions) {
    if (isMonitoring || isExplicitlyStopped) return

    val sensor = resolveSensor() ?: return
    val samplingPeriodUs = options.toSamplingPeriodUs()

    if (type.isTriggerSensor()) {
      val newTriggerListener = object : TriggerEventListener() {
        override fun onTrigger(event: TriggerEvent?) {
          val mapped = event?.toSensorEvent(type) ?: return
          lastEvent = mapped
          events.tryEmit(mapped)
          synchronized(lock) {
            if (isMonitoring && !isExplicitlyStopped) {
              sensorManager.requestTriggerSensor(this, sensor)
            }
          }
        }
      }

      triggerListener = newTriggerListener
      sensorManager.requestTriggerSensor(newTriggerListener, sensor)
      isMonitoring = true
      return
    }

    val newListener = object : SensorEventListener {
      override fun onSensorChanged(event: android.hardware.SensorEvent) {
        val mapped = event.toSensorEvent(type)
        lastEvent = mapped
        events.tryEmit(mapped)
      }

      override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
    }

    listener = newListener

    registerListener(
      listener = newListener,
      sensor = sensor,
      samplingPeriodUs = samplingPeriodUs,
      batchEnabled = options.batchEnabled,
    )

    isMonitoring = true
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
    batchEnabled: Boolean,
  ) {
    if (batchEnabled) {
      sensorManager.registerListener(
        listener,
        sensor,
        samplingPeriodUs,
        samplingPeriodUs,
      )
    } else {
      sensorManager.registerListener(
        listener,
        sensor,
        samplingPeriodUs,
      )
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

private fun android.hardware.SensorEvent.toSensorEvent(type: SensorType): SensorEvent {
  return SensorEvent(
    type = type,
    values = values.copyOf(),
    timestampNanos = timestamp,
    accuracy = accuracy.toSensorAccuracy(),
  )
}

private fun TriggerEvent.toSensorEvent(type: SensorType): SensorEvent {
  return SensorEvent(
    type = type,
    values = values.copyOf(),
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
