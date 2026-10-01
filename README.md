# Coch - Game Screen Capture & Suggestions (Android)

## Overview

**Coch** is an Android application that captures your device's screen and provides simple text-based game suggestions. This is the **MVP v1** — a minimal, simple implementation focused on:

- Start/Stop controls for screen capture
- Requesting screen-capture permission via Android's MediaProjection API
- Displaying very basic text suggestions
- **NO game interaction**: no input injection, no overlays, no automation

## Current Features (v1)

- ✅ "Iniciar" (Start) and "Parar" (Stop) buttons
- ✅ Android screen-capture permission flow
- ✅ Basic placeholder text suggestions (deterministic rules-based)
- ✅ Clean state management (Idle → Permission Pending → Running → Stopped)
- ✅ Foreground service for screen capture (Android 8+)
- ✅ Safe lifecycle and permission cleanup

## Future Work

- 🚀 OCR integration for real screen-text analysis
- 🚀 Floating panel overlay with suggestions
- 🚀 Intelligent recommendation engine
- 🚀 Game-specific strategies and hints

## Prerequisites

- **Android SDK 28+** (target SDK 35+)
- **Android Studio** (latest stable)
- **Kotlin 1.9+**
- **Gradle 8.0+**

## Build & Run

1. Clone the repository:
   ```bash
   git clone https://github.com/llacskon33/coch.git
   cd coch
   ```

2. Open in Android Studio and build:
   ```bash
   ./gradlew build
   ```

3. Install on a connected device or emulator:
   ```bash
   ./gradlew installDebug
   ```

4. Launch the app:
   - Open "Coch" from your launcher
   - Tap **"Iniciar"** to request screen-capture permission
   - Android will show a system dialog; tap **Allow**
   - The app enters "Running" state and displays suggestions
   - Tap **"Parar"** to stop capture and return to idle

## Permissions

- **CAPTURE_VIDEO_OUTPUT** (MediaProjection): Requests in-app via system dialog
- **FOREGROUND_SERVICE**: Declared in manifest for screen-capture service
- **FOREGROUND_SERVICE_MEDIA_PROJECTION** (Android 14+): Declared in manifest

**Note**: Android requires explicit user consent for screen capture via a system permission prompt. The app does NOT have permission to capture silently.

## Architecture

- **MainActivity**: UI (Compose), state management, permission flow
- **ScreenCaptureService**: Foreground service handling MediaProjection lifecycle
- **SuggestionProvider**: Placeholder suggestion logic (to be replaced with OCR + ML)

## Important: No Game Control

This app **only displays suggestions**. It does **not**:
- Inject touch, keyboard, or controller input
- Draw overlays on other apps
- Use accessibility services to automate game actions
- Monitor or interfere with game logic

All interactions are confined to the Coch app window. Suggestions are informational only.

## License

TBD

## Support

See issues on GitHub for known limitations and feature requests.
