# Vremea

Vremea is available as the original static web app and as a native Android app in `app/`.

## Build the Android APK

The Android app uses Java platform views, Android location permissions, and the Open-Meteo API without an API key.

```bash
./gradlew assembleDebug
```

The debug APK is written to `app/build/outputs/apk/debug/app-debug.apk`.

Install it on a connected device with:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```
