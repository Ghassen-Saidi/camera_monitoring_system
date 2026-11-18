# Camera Monitoring System

A Java-based camera monitoring project with a JavaFX client and an HTTP server.

## Project layout

- `src/` — JavaFX client
- `server/src/` — HTTP server
- `lib/` — bundled Java dependencies
- `cams/` — sample camera state files
- `build.ps1` — build the client
- `build-and-run.ps1` — build and launch the client

## Requirements

- JDK 17 or newer
- JavaFX SDK 25.0.1 (the scripts currently expect it at `C:\javafx-sdk-25.0.1\lib`)

The generated `out/` directories are intentionally ignored by Git.

## Configure live cameras

The server reads camera sources from environment variables. `CAMERA_COUNT` controls how many cameras are exposed; each camera uses `CAMERA_<id>_TYPE` and `CAMERA_<id>_URL`.

Examples:

```powershell
$env:CAMERA_COUNT = "2"
$env:CAMERA_0_TYPE = "HLS"
$env:CAMERA_0_URL = "http://localhost:8080/camera0/index.m3u8"
$env:CAMERA_1_TYPE = "HTTP"
$env:CAMERA_1_URL = "http://192.168.1.50/mjpeg"
```

The client can open HTTP/HTTPS, HLS, and file streams through JavaFX MediaPlayer. RTSP IP cameras and local webcam devices should be restreamed through FFmpeg, MediaMTX, or another bridge into HLS/HTTP/WebRTC before opening them in the JavaFX viewer. The API still reports `RTSP` and `WEBCAM` sources so a future native capture backend can be added without changing the camera model.

## API protection

For local development, `CAMERA_API_KEY` may be left unset. For a shared network, set it in the server environment and send the same key from the JavaFX client with `-Dcamera.apiKey=...` or from Android with `-PcameraApiKey=...`. Use HTTPS before deploying beyond a trusted local network.
