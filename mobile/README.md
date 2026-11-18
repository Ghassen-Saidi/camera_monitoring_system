# Camera Monitoring Mobile App

Native Kotlin Android client for the existing camera monitoring backend.

## Features

- Loads all cameras from `GET /api/cameras`
- Starts/stops one camera or all cameras through the existing POST endpoints
- Displays camera status, type, and stream URL
- Opens configured HTTP, HLS, RTSP, or webcam bridge URLs in an installed player/browser

## Running

Open the `mobile` folder in Android Studio and run the `app` configuration. The default emulator API URL is:

```text
http://10.0.2.2:8081/api
```

For a physical phone, pass the computer's LAN address without editing source:

```text
./gradlew assembleDebug -PcameraApiUrl=http://192.168.1.10:8081/api -PcameraApiKey=change-me
```

Debug builds allow local HTTP for development. Release builds require HTTPS. If the server is started with `CAMERA_API_KEY`, pass the same value as `cameraApiKey`. The app checks whether an installed application can handle each stream URL before launching it and reports HTTP error responses instead of crashing.
