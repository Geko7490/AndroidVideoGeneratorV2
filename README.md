# Vex Video AI

Offline-first Android video creation studio. The interface supports Turkish and English; generated narration, dialogue, titles and subtitles are English-only.

## Current implementation
- Kotlin + Jetpack Compose Android application.
- Turkish / English interface and nine content categories.
- Persistent local project library.
- Editable storyboard scene data with narration, dialogue, visual direction and layer metadata.
- Layered canvas preview and scene timeline.
- On-device llama.cpp runtime for GGUF storyboard generation, plus an OpenAI-compatible local-server fallback.
- Explicit Kokoro TTS, Whisper transcription and MP4 export runtime contracts. Missing runtimes are reported honestly, not simulated.

## Local LLM
Configure an OpenAI-compatible chat-completions endpoint in Settings. Example local endpoint: http://127.0.0.1:8080/v1/chat/completions. The local server must be running and expose the selected model. No cloud API key is required for local inference.

## Production milestones
1. Persistent project library.
2. Storyboard editor.
3. Layered Canvas editor.
4. Scene timeline and transitions.
5. Local LLM adapter and model manager.
6. Kokoro ONNX runtime and voice assets.
7. Whisper runtime and subtitle editor.
8. Android MediaCodec / Media3 Transformer export.
9. Model Center, downloads, progress and device-aware performance profiles.

Build with JDK 17 and Android SDK 36. Run ./gradlew testDebugUnitTest assembleDebug or use GitHub Actions.
