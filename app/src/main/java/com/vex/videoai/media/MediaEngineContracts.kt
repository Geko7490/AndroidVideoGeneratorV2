package com.vex.videoai.media
import java.io.File
data class GeneratedSpeech(val audioFile:File,val durationMs:Long)
data class SubtitleCue(val startMs:Long,val endMs:Long,val text:String)
interface EnglishSpeechEngine { val engineName:String; suspend fun synthesizeEnglish(text:String,voice:String,outputFile:File):GeneratedSpeech }
interface EnglishTranscriptionEngine { val engineName:String; suspend fun transcribeEnglish(audioFile:File):List<SubtitleCue> }
interface VideoExportEngine { val engineName:String; suspend fun export(projectId:String,outputFile:File,progress:(Int)->Unit) }
enum class RuntimeState { NOT_INSTALLED,READY,RUNNING,FAILED }
data class EngineDescriptor(val id:String,val displayName:String,val purpose:String,val state:RuntimeState,val detail:String)
object EngineCatalog { val initial=listOf(
 EngineDescriptor("llama","llama.cpp","On-device GGUF storyboard generation",RuntimeState.NOT_INSTALLED,"Download a GGUF model in Model Center to activate"),
 EngineDescriptor("kokoro","Kokoro TTS","English voice generation",RuntimeState.NOT_INSTALLED,"ONNX model and voice assets required"),
 EngineDescriptor("whisper","Whisper","English transcription and word timing",RuntimeState.NOT_INSTALLED,"Download the tiny.en model in Model Center to activate"),
 EngineDescriptor("renderer","Video Renderer","Layered image-sequence MP4 export",RuntimeState.READY,"Media3 renderer supports scene images, burned subtitles and per-scene voiceover tracks when all scenes have audio")
) }
