ESL V1 Tracker Android — Android Studio source project (beta)
============================================================
This ZIP is SOURCE CODE, not an installable APK.

BUILD ON WINDOWS:
1. Install Android Studio from developer.android.com/studio.
2. Extract this ZIP.
3. Open Android Studio > Open > select the ESL_V1_Tracker_Android folder.
4. Allow Android Studio to download Gradle, Android SDK 35 and other components.
5. Build > Build Bundle(s) / APK(s) > Build APK(s).
6. Locate the debug APK at app\build\outputs\apk\debug\app-debug.apk.
7. Transfer app-debug.apk to your Android phone and install it.

BLUETOOTH:
- Pair HC-05 in Android Settings first (often PIN 1234 or 0000).
- Open the app, allow Nearby devices permission, tap Connect HC-05,
  then choose the paired HC-05.
- Bluetooth Classic SPP UUID: 00001101-0000-1000-8000-00805F9B34FB.
- The ATmega8 UART / HC-05 must be configured to 38400 baud.
- Frames: A5 5A 20 01 + 32 x 4 ADC bytes + XOR checksum (133 bytes).

IMPORTANT ENGINEERING NOTES:
- This is an initial native Android beta, not yet tested on real HC-05 hardware.
- ADC-to-voltage/current conversion currently uses assumed Vref=5.0V,
  midpoint=2.5V, scale=0.25, Rsense=5000 ohms. Confirm against your
  V1 PCB and calibration before relying on measurements.
- Demo curves are SIMULATED, not hardware measurements.
- Current beta provides live graph, paired-device Bluetooth, draggable
  cursor readings and RMS. Reference compare, CSV export, and full V9
  measurement parity remain future work.
- A Windows/Android Studio build is required to produce the APK.
