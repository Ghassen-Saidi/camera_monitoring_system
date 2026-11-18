# Camera Monitoring System

A camera monitoring system with a JavaFX desktop client, a Kotlin Android client, and a lightweight Java HTTP backend.

## Components

- `server/src/server/` — HTTP API, camera service, request handling, and camera-control adapter
- `src/camera/` — JavaFX desktop client
- `mobile/` — native Kotlin Android client
- `lib/` — JavaFX and Jackson runtime dependencies
- `cams/` — local runtime state; created by the server and ignored by Git

## Backend API

- `GET /api/cameras`
- `GET /api/cameras/{id}`
- `POST /api/cameras/{id}/record` with `{"action":"START"}` or `{"action":"STOP"}`
- `POST /api/cameras/all/record` with `{"action":"START"}` or `{"action":"STOP"}`

The server validates request lines, headers, transfer encoding, content lengths, JSON bodies, and actions. It rejects duplicate headers and unsupported transfer encodings, limits request sizes, applies socket timeouts, and reports clean startup failures.

Bulk commands attempt every camera and return an explicit result for each one, including success/failure counts and partial-success information. A bulk command can therefore return HTTP 200 with `success: false` when an individual camera adapter fails; clients must inspect the response body rather than treating every HTTP 200 as complete success.

## Backend build and tests

The backend has an independent Maven build using Java 17. From the repository root:

```bash
cd server
mvn clean verify
```

The GitHub Actions workflow at `.github/workflows/server-ci.yml` runs the backend integration tests on pushes and pull requests affecting the server.

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

Open the `mobile/` folder in Android Studio. The default emulator API URL for debug builds is:

```text
http://10.0.2.2:8081/api
```

For a physical device, configure the server's LAN address without editing source:

```bash
./gradlew assembleDebug -PcameraApiUrl=http://192.168.1.10:8081/api
```

Release builds intentionally have no HTTP fallback. Supply a verified HTTPS endpoint:

```bash
./gradlew assembleRelease -PcameraReleaseApiUrl=https://camera.example.com/api
```

The Android build rejects release task graphs unless `cameraReleaseApiUrl` is an HTTPS URL. Debug builds continue to use the local emulator URL or the optional `cameraApiUrl` property. The mobile client displays partial bulk-operation failures and opens streams through an installed compatible player; it does not embed a native video player yet.

## Security

For local development, `CAMERA_API_KEY` may be unset. For a shared network, set it on the server and pass the same value to clients:

- JavaFX: `-Dcamera.apiKey=...`
- Android: `-PcameraApiKey=...`

Use HTTPS before deploying outside a trusted local network. The Android release build enforces HTTPS for the configured API endpoint. An API key embedded in an Android APK can be extracted; it is suitable only as a low-assurance development gate, not as production user authentication. Production deployments should use TLS, per-user or short-lived credentials, authorization, and rate limiting.

## Repository notes

- `main` — stable merged application baseline
- `mobile-kotlin` — original Android client branch
- `fix/backend-hardening-ci` — backend protocol/lifecycle hardening, partial-result reporting, release HTTPS enforcement, and CI tests
- The Gradle wrapper has not been added because the repository currently has no verified wrapper distribution. Android Studio can generate a wrapper from its configured Gradle installation.
