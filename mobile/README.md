# Camera Monitoring Mobile App

Native Kotlin Android client for the existing camera monitoring backend.

## Features

- Loads cameras from `GET /api/cameras`
- Starts/stops one camera or all cameras
- Shows individual failures when a bulk operation only partially succeeds
- Opens HTTP, HLS, RTSP, or webcam bridge URLs in an installed player/browser

## Running

Open the `mobile/` folder in Android Studio and run the `app` configuration. The debug emulator API URL defaults to:

```text
http://10.0.2.2:8081/api
```

For a physical phone, configure the computer's LAN address with the `cameraApiUrl` Gradle property. From the `mobile/` directory, with Gradle installed:

```bash
gradle :app:assembleDebug -PcameraApiUrl=http://192.168.1.10:8081/api
```

Release builds require an HTTPS endpoint:

```bash
gradle :app:assembleRelease -PcameraReleaseApiUrl=https://camera.example.com/api
```

The repository currently has no Gradle wrapper. Use Android Studio's configured Gradle installation or install a compatible Gradle distribution. Debug builds allow local HTTP for development; release builds reject missing or non-HTTPS API URLs.

If the server uses `CAMERA_API_KEY`, pass the same value through `-PcameraApiKey=...`. A key embedded in an APK can be extracted and is not production-grade authentication.
