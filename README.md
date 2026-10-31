# Vex Video AI

Offline-first Android video creation studio. The interface supports Turkish and English; generated narration, dialogue, titles and subtitles are English-only.

## Current implementation
- Kotlin + Jetpack Compose Android application with a persistent local project library.
- Nine content categories: horror, educational, story, mystery, history, documentary, comedy, facts/top 10 and motivational.
- LLM-generated scene plans include narration, speaker-labelled dialogue, visual direction, layers, camera movement and transitions.
- No separate intro, logo bumper or title-card scene is requested; the first scene starts with the first meaningful story action.
- Sequential full-video pipeline: LLM storyboard → per-scene speech → Whisper English subtitles → MP4 render.
- Scenes with no narration or dialogue are kept silent instead of failing the whole project; a valid silent PCM audio segment preserves their place in the audio sequence.
- Low / Mid / High profiles change on-device LLM CPU threads, context size, token budget and automatic export resolution/frame rate.
- Local model choice supports Qwen3 0.6B and Qwen3 4B GGUF. The selected model is used only when downloaded; otherwise the configured OpenAI-compatible local-server fallback is used.
- Whisper tiny.en is loaded once for a batch of speech scenes. Silent scenes are skipped during transcription.
- On-device llama.cpp runtime for GGUF storyboard generation.
- Offline Android English system TTS currently creates scene WAV files. Kokoro ONNX files can be downloaded, but Kokoro inference and multi-speaker voice selection are not yet integrated.
- Canvas layer controls and PNG/JPEG/WebP image import; assets are copied to app-private project storage and included in MP4 rendering.
- Media3 Transformer MP4 export with selectable 480p–4K resolution and 24/30/60 fps. Full production uses the selected profile preset.

## Performance profiles
- Low: 2 LLM threads, 2048 context, 1100 token budget, 720p/24 fps.
- Mid: up to 4 LLM threads, 4096 context, 1800 token budget, 1080p/30 fps.
- High: up to 8 LLM threads, 6144 context, 2800 token budget, 1080p/60 fps.
- Actual LLM thread count is capped by available CPU cores. High may increase heat, battery use and memory pressure; it does not guarantee GPU acceleration.

## Known limitations
- Speech currently uses the Android system TTS voice, not Kokoro. Speaker names are preserved in the generated dialogue, but distinct character voices are not yet rendered.
- Voiceover clip durations are not yet automatically time-stretched to each scene duration; long speech can extend beyond its intended visual beat.
- Camera movement and transitions are storyboard metadata; animated camera effects and transition rendering are not yet complete.
- Imported image assets and native vector placeholders are supported; AI image generation is not integrated.
- Detailed audio mixing, device-specific memory safeguards, cancellation and physical-device testing remain.

Build with JDK 17 and Android SDK 36. Run `./gradlew testDebugUnitTest assembleDebug` or use GitHub Actions.