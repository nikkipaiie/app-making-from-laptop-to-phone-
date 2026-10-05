package com.nighttime.app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorManager
import android.os.IBinder
import android.speech.tts.TextToSpeech
import androidx.core.app.NotificationCompat
import java.util.Calendar
import java.util.Locale

class NightTimeService : Service(), TextToSpeech.OnInitListener {

    companion object {
        private const val CHANNEL_ID = "nighttime_channel"
        private const val NOTIFICATION_ID = 1001
    }

    private lateinit var sensorManager: SensorManager
    private lateinit var shakeDetector: ShakeDetector
    private var tts: TextToSpeech? = null
    private var ttsReady = false

    override fun onCreate() {
        super.onCreate()
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        shakeDetector = ShakeDetector { onShakeDetected() }
        tts = TextToSpeech(this, this)
        registerShakeListener()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createNotificationChannel()
        val notification = createNotification()
        startForeground(NOTIFICATION_ID, notification)
        return START_STICKY
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "NightTime Shake Listener",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Listens for shakes to announce the time in the background"
        }
        val manager = getSystemService(NotificationManager::class.java)
        manager?.createNotificationChannel(channel)
    }

    private fun createNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("NightTime is Active")
            .setContentText("Shake your phone to hear the time")
            .setSmallIcon(R.drawable.ic_moon)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun registerShakeListener() {
        val accel = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        if (accel != null) {
            sensorManager.registerListener(
                shakeDetector,
                accel,
                SensorManager.SENSOR_DELAY_UI
            )
        }
    }

    private fun unregisterShakeListener() {
        sensorManager.unregisterListener(shakeDetector)
    }

    private fun onShakeDetected() {
        val now = Calendar.getInstance()
        val hour = now.get(Calendar.HOUR_OF_DAY)
        val minute = now.get(Calendar.MINUTE)

        val amPm = if (hour < 12) "AM" else "PM"
        val displayHour = when {
            hour == 0 -> 12
            hour > 12 -> hour - 12
            else -> hour
        }
        val minuteText = when {
            minute == 0 -> "o'clock"
            minute < 10 -> "oh $minute"
            else -> "$minute"
        }

        val speech = if (minuteText == "o'clock") {
            "The time is $displayHour o'clock $amPm"
        } else {
            "The time is $displayHour $minuteText $amPm"
        }

        if (ttsReady) {
            tts?.speak(speech, TextToSpeech.QUEUE_FLUSH, null, "time_announce")
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale.getDefault())
            if (result == null || result < TextToSpeech.SUCCESS) {
                tts?.setLanguage(Locale.US)
            }
            ttsReady = true
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        unregisterShakeListener()
        tts?.stop()
        tts?.shutdown()
        tts = null
        super.onDestroy()
    }
}
