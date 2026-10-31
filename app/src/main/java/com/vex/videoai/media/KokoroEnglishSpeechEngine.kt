package com.vex.videoai.media

import android.content.Context
import java.io.File

/**
 * Explicit runtime gate for the optional Kokoro model download.
 * The current Android dependency is not available from the configured repositories,
 * so this adapter refuses activation and lets the pipeline use offline Android TTS.
 * It intentionally contains no fake neural inference implementation.
 */
class KokoroEnglishSpeechEngine(private val context:Context,private val modelFile:File) {
    suspend fun initialize() {
        require(modelFile.isFile&&modelFile.length()>1024L) { "Download the Kokoro INT8 ONNX model first." }
        throw UnsupportedOperationException("Kokoro ONNX Android inference runtime is not bundled in this build. Android offline TTS will be used instead.")
    }
    suspend fun synthesize(text:String,output:File,speed:Float=1f):File {
        error("Kokoro runtime is unavailable in this build.")
    }
    fun release() = Unit
}