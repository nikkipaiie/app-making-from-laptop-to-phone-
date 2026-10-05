package com.nighttime.app

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.sqrt

/**
 * Detects a "shake" gesture from the accelerometer.
 *
 * Uses a cooldown to prevent multiple rapid-fire triggers.
 */
class ShakeDetector(private val onShake: () -> Unit) : SensorEventListener {

    companion object {
        /** Minimum acceleration (m/s²) beyond gravity to count as a shake. */
        private const val SHAKE_THRESHOLD = 12.0f

        /** Cooldown between accepted shakes (ms). */
        private const val SHAKE_COOLDOWN_MS = 2_000L
    }

    private var lastShakeTime: Long = 0

    override fun onSensorChanged(event: SensorEvent?) {
        event ?: return
        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]

        // Compute acceleration magnitude, subtracting gravity (~9.8).
        val acceleration = sqrt((x * x + y * y + z * z).toDouble()).toFloat() - SensorManager.GRAVITY_EARTH

        if (acceleration > SHAKE_THRESHOLD) {
            val now = System.currentTimeMillis()
            if (now - lastShakeTime > SHAKE_COOLDOWN_MS) {
                lastShakeTime = now
                onShake()
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Not needed
    }
}
