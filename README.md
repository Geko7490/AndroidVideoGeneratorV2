# Vex Video AI

Offline-first Android video creation studio. The interface supports Turkish and English; generated narration, dialogue, titles and subtitles are English-only.

## Current implementation
- Kotlin + Jetpack Compose Android application with a persistent local project library.
- Nine content categories: horror, educational, story, mystery, history, documentary, comedy, facts/top 10 and motivational.
- LLM-generated scene plans include narration, speaker-labelled dialogue, visual direction, layers, camera movement and transitions.
- The generated video begins directly with its first story action; no separate intro, logo bumper or title-card scene is requested.
- Sequential full-video pipeline: LLM storyboard → per-scene offline speech → Whisper English subtitles → MP4 render.
- Low / Mid / High performance profiles change local LLM CPU threads, context size, output token budget and automatic export resolution/frame rate.
- Low: 2 LLM threads, 2048 context, 720p/24 fps. Mid: up to 4 threads, 4096 context, 1080p/30 fps. High: up to 8 threads, 6144 context, 1080p/60 fps. Actual thread count is capped by available CPU cores.
- The full pipeline reports its current stage and per-scene progress. Whisper loads its model once for the batch rather than once per scene.
- On-device llama.cpp runtime for GGUF storyboard generation, plus an OpenAI-compatible local-server fallback.
- Offline Android English system TTS currently creates the scene WAV files. Kokoro ONNX model files can be downloaded, but Kokoro inference and multi-speaker voice selection are not yet integrated; the current speech fallback uses one system voice.
- On-device Whisper tiny.en transcription with editable timestamped subtitle cues and SRT export.
- Canvas layer controls and PNG/JPEG/WebP image import; assets are copied to app-private project storage and included in MP4 rendering.
- Media3 Transformer MP4 export with selectable 480p–4K long-edge resolution and 24/30/60 fps. The full pipeline uses the selected performance profile's export preset.

## Performance profiles
Low prioritizes responsiveness and lower heat. Mid balances speed and quality. High allocates more CPU threads and model context and uses a higher frame rate. High may increase temperature, battery use and memory pressure; it does not guarantee GPU acceleration.

## Known limitations
- Speech is currently Android system TTS, not Kokoro. Dialogue speaker names are preserved in the script, but separate character voices are not yet rendered.
- Whisper transcription follows the produced voice track; scene-level timing is currently normalized to each clip and precise multi-speaker lip-sync is not implemented.
- Camera movement and transitions are storyboard metadata; animated camera effects and transition rendering are not yet complete.
- Imported image assets and native vector placeholders are supported; AI image generation is not integrated.
- More detailed audio mixing, device-specific memory safeguards, cancellation and physical-device testing remain.

Build with JDK 17 and Android SDK 36. Run `./gradlew testDebugUnitTest assembleDebug` or use GitHub Actions.