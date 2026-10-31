package com.vex.videoai.media

import android.content.Context
import dev.ffmpegkit.kokoro.KokoroConfig
import dev.ffmpegkit.kokoro.KokoroTTS
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/** On-device Kokoro neural speech. The bundled free AAR currently exposes the af_heart voice. */
class KokoroEnglishSpeechEngine(private val context:Context,private val modelFile:File) {
    private var initialized=false

    suspend fun initialize() {
        require(modelFile.isFile&&modelFile.length()>1024L) { "Download the Kokoro INT8 ONNX model in Model Center first." }
        KokoroTTS.initialize(context.applicationContext,modelFile.absolutePath)
        initialized=true
    }

    suspend fun synthesize(text:String,output:File,speed:Float=1f):File {
        require(initialized) { "Kokoro engine is not initialized." }
        require(text.isNotBlank()&&text.all{it.code<128}) { "Kokoro voiceover text must be English." }
        val result=KokoroTTS.speak(text,KokoroConfig(speed=speed.coerceIn(.75f,1.25f)))
        require(result.audioData.size>44) { "Kokoro returned an empty WAV file." }
        withContext(Dispatchers.IO) { output.parentFile?.mkdirs();output.writeBytes(result.audioData) }
        return output
    }

    fun release() { if(initialized){KokoroTTS.release();initialized=false} }
}