# Vex Video AI

Offline-first Android video creation studio. The interface supports Turkish and English; generated narration, dialogue, titles and subtitles are English-only.

## Current implementation
- Kotlin + Jetpack Compose Android application with a persistent local project library.
- Nine content categories: horror, educational, story, mystery, history, documentary, comedy, facts/top 10 and motivational.
- LLM-generated scene plans include narration, speaker-labelled dialogue, visual direction, layers, camera movement and transitions.
- No separate intro, logo bumper or title-card scene is requested; the first scene starts with the first meaningful story action.
- Sequential full-video pipeline: LLM storyboard → per-scene speech → Whisper English subtitles → MP4 render.
- The selected local LLM model can be Qwen3 0.6B or Qwen3 4B GGUF; the selected model is used when downloaded, otherwise the configured OpenAI-compatible local-server fallback is used.
- Low / Mid / High profiles change on-device LLM CPU threads, context size, token budget and automatic export resolution/frame rate.
- Kokoro Android ONNX neural TTS is integrated. Download the INT8 ONNX model in Model Center to use it; if it is not installed or cannot initialize, Android system TTS is used as a fallback.
- Kokoro free runtime currently bundles one English voice (`af_heart`); separate character voices are not yet available in this integration.
- Scenes with no narration or dialogue remain silent instead of failing; a valid silent PCM WAV clip preserves their place in the audio sequence.
- Whisper tiny.en is loaded once for a batch of speech scenes. Silent scenes are skipped during transcription.
- On-device llama.cpp runtime for GGUF storyboard generation.
- On-device Whisper English transcription with editable timestamped subtitle cues and SRT export.
- Canvas layer controls and PNG/JPEG/WebP image import; assets are copied to app-private project storage and included in MP4 rendering.
- The renderer samples scenes into short frames with subtitle-boundary accuracy and applies subtle Ken Burns-style zoom/pan from the camera direction; JPEG intermediate frames reduce temporary storage compared with PNG.
- Media3 Transformer MP4 export with selectable 480p–4K resolution and 24/30/60 fps. Full production uses the selected profile preset.

## Performance profiles
- Low: 2 LLM threads, 2048 context, 1100 token budget, 720p/24 fps.
- Mid: up to 4 LLM threads, 4096 context, 1800 token budget, 1080p/30 fps.
- High: up to 8 LLM threads, 6144 context, 2800 token budget, 1080p/60 fps, and 4 camera-motion samples per second.
- Frame sampling also scales with the profile: Low 1 fps, Mid 2 fps, High 4 fps; subtitles add exact frame boundaries.
- Actual LLM thread count is capped by available CPU cores. High may increase heat, battery use and memory pressure; it does not guarantee GPU acceleration.

## Known limitations
- The current free Kokoro Android AAR bundles one voice; multi-speaker character voice assignment is not yet implemented.
- Voiceover clip durations are not yet automatically time-stretched to exact scene duration; unusually long speech may extend beyond its visual beat.
- Camera direction now drives subtle pan/zoom motion and fade/dissolve/wipe scene effects in the frame renderer. These are lightweight 2D moves, not full 3D animation.
- Sound effects are generated and saved as editable scene cues, but a dedicated SFX synthesis/mixing engine is not integrated yet.
- Imported image assets and native vector placeholders are supported; AI image generation is not integrated.
- Detailed audio mixing, device-specific memory safeguards, cancellation and physical-device testing remain.

Build with JDK 17 and Android SDK 36. Run `./gradlew testDebugUnitTest assembleDebug` or use GitHub Actions.