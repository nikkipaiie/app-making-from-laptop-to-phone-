package com.nighttime.app

import android.hardware.Sensor
import android.hardware.SensorManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    private lateinit var sensorManager: SensorManager
    private lateinit var nightTimeManager: NightTimeManager
    private lateinit var shakeDetector: ShakeDetector

    private var tts: TextToSpeech? = null
    private var ttsReady = false

    // UI elements
    private lateinit var tvStatus: TextView
    private lateinit var tvCurrentTime: TextView
    private lateinit var tvLastAnnouncement: TextView
    private lateinit var switchEnable: SwitchMaterial
    private lateinit var btnStartTime: MaterialButton
    private lateinit var btnEndTime: MaterialButton

    // Clock updater
    private val handler = Handler(Looper.getMainLooper())
    private val clockRunnable = object : Runnable {
        override fun run() {
            updateUI()
            handler.postDelayed(this, 1_000L)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        nightTimeManager = NightTimeManager(this)

        // Initialize TTS
        tts = TextToSpeech(this, this)

        // Bind views
        tvStatus = findViewById(R.id.tvStatus)
        tvCurrentTime = findViewById(R.id.tvCurrentTime)
        tvLastAnnouncement = findViewById(R.id.tvLastAnnouncement)
        switchEnable = findViewById(R.id.switchEnable)
        btnStartTime = findViewById(R.id.btnStartTime)
        btnEndTime = findViewById(R.id.btnEndTime)

        // Setup shake detector
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        shakeDetector = ShakeDetector { onShakeDetected() }

        // Switch
        switchEnable.isChecked = nightTimeManager.isEnabled
        switchEnable.setOnCheckedChangeListener { _, isChecked ->
            nightTimeManager.isEnabled = isChecked
            updateUI()
            if (isChecked) registerShakeListener() else unregisterShakeListener()
        }

        // Time pickers
        btnStartTime.text = nightTimeManager.formattedStart()
        btnEndTime.text = nightTimeManager.formattedEnd()

        btnStartTime.setOnClickListener { showTimePicker(isStart = true) }
        btnEndTime.setOnClickListener { showTimePicker(isStart = false) }

        updateUI()
    }

    override fun onResume() {
        super.onResume()
        if (nightTimeManager.isEnabled) {
            registerShakeListener()
        }
        handler.post(clockRunnable)
    }

    override fun onPause() {
        super.onPause()
        unregisterShakeListener()
        handler.removeCallbacks(clockRunnable)
    }

    override fun onDestroy() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        super.onDestroy()
    }

    // ── TextToSpeech.OnInitListener ──────────────────────────────────────

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale.getDefault()
            ttsReady = true
        } else {
            Toast.makeText(this, "Text-to-Speech not available", Toast.LENGTH_LONG).show()
        }
    }

    // ── Sensor registration ──────────────────────────────────────────────

    private fun registerShakeListener() {
        val accel = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        if (accel != null) {
            sensorManager.registerListener(
                shakeDetector,
                accel,
                SensorManager.SENSOR_DELAY_UI
            )
        } else {
            Toast.makeText(this, "No accelerometer found on this device", Toast.LENGTH_LONG).show()
        }
    }

    private fun unregisterShakeListener() {
        sensorManager.unregisterListener(shakeDetector)
    }

    // ── Shake handler ────────────────────────────────────────────────────

    private fun onShakeDetected() {
        if (!nightTimeManager.isNightTimeNow()) {
            // Outside night hours — silently ignore
            runOnUiThread {
                tvLastAnnouncement.text = "Shake detected — outside night hours"
            }
            return
        }

        val now = Calendar.getInstance()
        val hour = now.get(Calendar.HOUR_OF_DAY)
        val minute = now.get(Calendar.MINUTE)

        // Build a natural-sounding time string
        val amPm = if (hour < 12) "A M" else "P M"
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

        // Speak the time
        if (ttsReady) {
            tts?.speak(speech, TextToSpeech.QUEUE_FLUSH, null, "time_announce")
        }

        val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
        val formattedTime = timeFormat.format(Date())
        runOnUiThread {
            tvLastAnnouncement.text = "Last announced: $formattedTime"
        }
    }

    // ── Time picker ──────────────────────────────────────────────────────

    private fun showTimePicker(isStart: Boolean) {
        val currentHour = if (isStart) nightTimeManager.startHour else nightTimeManager.endHour
        val currentMinute = if (isStart) nightTimeManager.startMinute else nightTimeManager.endMinute

        val picker = MaterialTimePicker.Builder()
            .setTimeFormat(TimeFormat.CLOCK_12H)
            .setHour(currentHour)
            .setMinute(currentMinute)
            .setTitleText(if (isStart) "Night starts at" else "Night ends at")
            .build()

        picker.addOnPositiveButtonClickListener {
            if (isStart) {
                nightTimeManager.startHour = picker.hour
                nightTimeManager.startMinute = picker.minute
                btnStartTime.text = nightTimeManager.formattedStart()
            } else {
                nightTimeManager.endHour = picker.hour
                nightTimeManager.endMinute = picker.minute
                btnEndTime.text = nightTimeManager.formattedEnd()
            }
            updateUI()
        }

        picker.show(supportFragmentManager, "time_picker")
    }

    // ── UI updates ───────────────────────────────────────────────────────

    private fun updateUI() {
        val timeFormat = SimpleDateFormat("h:mm:ss a", Locale.getDefault())
        tvCurrentTime.text = "Current time: ${timeFormat.format(Date())}"

        if (!nightTimeManager.isEnabled) {
            tvStatus.text = "Disabled"
            tvStatus.setTextColor(getColor(R.color.status_inactive))
        } else if (nightTimeManager.isNightTimeNow()) {
            tvStatus.text = "Listening… shake to hear the time"
            tvStatus.setTextColor(getColor(R.color.status_active))
        } else {
            tvStatus.text = "Outside night hours"
            tvStatus.setTextColor(getColor(R.color.text_secondary))
        }
    }
}
