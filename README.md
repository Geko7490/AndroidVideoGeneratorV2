# Vex Video AI

Offline-first Android video creation studio. The interface supports Turkish and English; generated narration, dialogue, titles, on-screen words and subtitles are English-only.

## Implemented
- Kotlin + Jetpack Compose Android app with persistent local projects and an editable scene canvas.
- Nine content categories: horror, educational, story, mystery, history, documentary, comedy, facts/top 10 and motivational.
- LLM-generated scene plans include narration, speaker-labelled dialogue, visual direction, background/foreground layers, camera direction, transitions and sound-effect cues.
- No separate intro, logo bumper or title-card scene: scene 1 starts with the first meaningful action.
- Sequential pipeline: selected LLM → scene voiceover → Whisper tiny.en → MP4 export.
- Selectable Qwen3 0.6B / 4B GGUF local models via llama.cpp. If the selected model is not downloaded, the configured OpenAI-compatible local server is used as a fallback.
- Kokoro INT8 English runs fully offline through the sherpa-onnx Android runtime. Its 11 speaker IDs are assigned consistently to speaker-labelled dialogue; narration uses its own voice. Each utterance is generated separately and joined into a scene WAV.
- Whisper tiny.en is loaded once for a batch of spoken scenes. Silent scenes are skipped; timestamped subtitle cues can be edited and exported as SRT.
- Scenes without narration or dialogue retain their visual duration with a valid silent PCM WAV segment.
- Canvas layers support PNG/JPEG/WebP image import, position, scale, visibility and layer type. Imported files are copied into app-private project storage and included in rendering.
- Renderer samples frames at profile-dependent intervals, adds exact subtitle boundaries, applies subtle camera pan/zoom, and supports lightweight fade/dissolve/wipe effects.
- MP4 export supports 480p–4K and 24/30/60 fps. Full production applies the selected profile's defaults.
- Scene sound-effect descriptions are saved as editable cues. They are not audio tracks yet.

## Performance profiles
- Low: 2 LLM threads, 2048 context, 1100 token budget, 720p/24 fps, 1 motion sample per second.
- Mid: up to 4 LLM threads, 4096 context, 1800 token budget, 1080p/30 fps, 2 motion samples per second.
- High: up to 8 LLM threads, 6144 context, 2800 token budget, 1080p/60 fps, 4 motion samples per second.
- LLM thread count is capped by available CPU cores. High increases CPU work, rendering density and memory pressure; it does not guarantee GPU acceleration.

## Model Center
- Qwen3 0.6B and Qwen3 4B GGUF files can be downloaded for local storyboard generation.
- Whisper tiny.en can be downloaded for local English transcription.
- Model Center downloads the complete Kokoro INT8 English bundle (ONNX model, 11-speaker voice bank, tokens and phonemizer data). It is safely extracted to app-private storage on first use. Android system TTS remains the fallback if model initialization fails.

## Known limitations
- Kokoro uses CPU inference through sherpa-onnx; High mode can increase thread count, heat and battery use. Android system TTS is the fallback if the bundle is missing or fails to initialize.
- Voiceover is padded to scene duration when shorter. If speech is longer than the scene, production stops with an error; automatic speech time-stretching and scene re-timing are not implemented.
- Camera moves and transitions are lightweight 2D effects, not full 3D animation.
- AI image generation and SFX synthesis/mixing are not integrated. The user can import image assets and the LLM can describe SFX cues.
- Device-specific memory safeguards, cancellation polish and physical-device testing remain.

## Build
Use JDK 17 and Android SDK 36. Run `./gradlew testDebugUnitTest assembleDebug` or use the GitHub Actions workflow. The workflow uploads the debug APK as `vex-video-ai-debug`.