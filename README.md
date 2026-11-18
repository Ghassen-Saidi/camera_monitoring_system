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
