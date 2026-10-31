# Vex Video AI

An offline-first Android AI video creation studio. The app UI supports Turkish and English; generated video narration, dialogue, titles, and subtitles are English-only by design.

## Planned capabilities

- AI-assisted scripts and scene planning for horror, educational, story, mystery, history, documentary, comedy, facts, and motivational videos.
- Layered Canvas/SVG scenes, storyboard, animation and multi-track timeline.
- English voice generation through a pluggable Kokoro TTS runtime.
- English subtitle generation through a pluggable Whisper runtime.
- Local model management and offline-first workflows.
- MP4 export with configurable aspect ratio and duration.

## Build

Open the project in Android Studio or run `./gradlew assembleDebug` with JDK 17 and Android SDK 35/36 installed.

The first milestone establishes the Android app foundation and bilingual AI Studio UI. Model runtimes and final video rendering will be integrated incrementally rather than simulated as if already available.
