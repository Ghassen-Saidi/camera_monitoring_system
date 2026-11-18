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

For a physical phone, change `API_BASE_URL` in `app/build.gradle.kts` to the computer's LAN address, for example `http://192.168.1.10:8081/api`, and make sure the server firewall allows port 8081.
