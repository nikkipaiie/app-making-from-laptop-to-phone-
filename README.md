# NightTime 🌙

An intuitive Android application built with Kotlin that helps you check the time effortlessly during night hours using accelerometer shake gestures and Text-to-Speech (TTS).

---

## ✨ Features

- **🌙 Custom Night Window**: Configure your preferred night start and end hours (supports overnight schedules like 10:00 PM to 7:00 AM).
- **📳 Shake-to-Speak**: Simply shake your device during night hours to hear the current time spoken aloud via Text-to-Speech.
- **⏰ Live Clock & Status**: Real-time clock display showing whether you are currently within the active night time window.
- **💾 Persistent Settings**: All your time preferences and toggle states are saved locally across app restarts using `SharedPreferences`.

---

## 🛠️ Tech Stack & Architecture

- **Language**: Kotlin 2.0+
- **Min SDK**: API 26 (Android 8.0 Oreo)
- **Target SDK**: API 35 (Android 15)
- **UI Components**: Material Design 3, ConstraintLayout
- **Sensors**: Android SensorManager (`TYPE_ACCELEROMETER` for shake gesture detection)
- **Speech**: Android `TextToSpeech` engine

---

## 📱 How It Works

1. Enable the NightTime monitor using the master switch.
2. Set your custom night start time and end time using the Material Time Pickers.
3. When it's nighttime, shaking your phone will trigger the Text-to-Speech engine to announce the current time.

---

## 🚀 Getting Started

1. Clone the repository:
   ```bash
   git clone https://github.com/your-username/NightTime.git
   ```
2. Open the project in **Android Studio** (Koala or newer recommended).
3. Build and run on an Android emulator or physical device.

---

## 📄 License

This project is open-source and available under the [MIT License](LICENSE).
