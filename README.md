# Camera Monitoring System

A camera monitoring system with a JavaFX desktop client, a Kotlin Android client, and a lightweight Java HTTP backend.

## Components

- `server/src/server/` — HTTP API, camera service, request handling, and camera-control adapter
- `src/camera/` — JavaFX desktop client
- `mobile/` — native Kotlin Android client
- `lib/` — Jackson and Java runtime dependencies
- `cams/` — local runtime state; created by the server and ignored by Git

## Backend API

- `GET /api/cameras`
- `GET /api/cameras/{id}`
- `POST /api/cameras/{id}/record` with `{"action":"START"}` or `{"action":"STOP"}`
- `POST /api/cameras/all/record` with `{"action":"START"}` or `{"action":"STOP"}`

The server validates actions, returns appropriate HTTP status codes, limits request sizes, applies socket timeouts, and shuts down its listener and worker pool cleanly.

## Live camera configuration

Set the number of cameras and each source through environment variables:

```powershell
$env:CAMERA_COUNT = "2"
$env:CAMERA_0_TYPE = "HLS"
$env:CAMERA_0_URL = "http://localhost:8080/camera0/index.m3u8"
$env:CAMERA_1_TYPE = "HTTP"
$env:CAMERA_1_URL = "http://192.168.1.50/mjpeg"
```

The current `FileCameraControl` is a development simulator. Replace it with an FFmpeg, RTSP, or webcam adapter to control real hardware. RTSP and local webcam sources should normally be restreamed to HLS, HTTP, or WebRTC for JavaFX playback.

## Desktop client

Requirements:

- JDK 17+
- JavaFX SDK 25.0.1, configurable with `JAVAFX_PATH`

The desktop client uses Jackson for JSON parsing, supports configurable API URLs with `-Dcamera.api=...`, and reports asynchronous media playback errors.

## Android client

Open `mobile/` in Android Studio. The default emulator API URL is:

```text
http://10.0.2.2:8081/api
```

For a physical device, configure the server's LAN address without editing source:

```text
./gradlew assembleDebug -PcameraApiUrl=http://192.168.1.10:8081/api
```

The mobile client lists cameras, sends control commands, and opens streams through an installed compatible player. It does not embed a native video player yet.

## Security

For local development, `CAMERA_API_KEY` may be unset. For a shared network, set it on the server and pass the same value to clients:

- JavaFX: `-Dcamera.apiKey=...`
- Android: `-PcameraApiKey=...`

Use HTTPS before deploying outside a trusted local network. Debug Android builds allow local cleartext HTTP; release builds do not.

## Branches

- `main` — merged application baseline
- `mobile-kotlin` — Kotlin Android client and reliability fixes
