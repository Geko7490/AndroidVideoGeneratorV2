# Vex Video AI

Offline-first Android video creation studio. The interface supports Turkish and English; generated narration, dialogue, titles and subtitles are English-only.

## Current implementation
- Kotlin + Jetpack Compose Android application.
- Turkish / English interface and nine content categories.
- Persistent local project library.
- Editable storyboard scene data with narration, dialogue, visual direction and layer metadata.
- Canvas layer position controls with live preview updates.
- Scene-by-scene offline English voiceover generation; Whisper can automatically transcribe the generated audio when its model is installed.
- Layered canvas preview and scene timeline.
- On-device llama.cpp runtime for GGUF storyboard generation, plus an OpenAI-compatible local-server fallback.
- On-device Whisper transcription with English-only language selection, editable timestamped subtitle cues and SRT export.
- Android offline English TTS fallback that generates and attaches per-scene WAV voiceovers; Kokoro ONNX models are downloadable, but native Kokoro inference is still pending integration.
- Media3 Transformer MP4 export with selectable 480p–4K long-edge resolution and 24/30/60 fps; subtitle cues are burned into the rendered frames. If every scene has an attached voiceover file, audio clips are included sequentially; otherwise the export is silent.

## Voice generation and subtitles

The current offline voiceover button uses Android's installed English system voice and writes a per-scene audio file. It is a functional fallback, not Kokoro. Whisper tiny.en runs locally to create timestamped English subtitle cues; cues can be edited and exported as SRT.

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
