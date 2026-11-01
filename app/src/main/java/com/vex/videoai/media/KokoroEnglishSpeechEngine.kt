package com.vex.videoai.media

import android.content.Context
import com.k2fsa.sherpa.onnx.GenerationConfig
import com.k2fsa.sherpa.onnx.OfflineTts
import com.k2fsa.sherpa.onnx.OfflineTtsConfig
import com.k2fsa.sherpa.onnx.OfflineTtsKokoroModelConfig
import com.k2fsa.sherpa.onnx.OfflineTtsModelConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/** Offline Kokoro INT8 English synthesis powered by sherpa-onnx, with 11 selectable speaker IDs. */
class KokoroEnglishSpeechEngine(private val context:Context,private val archiveFile:File,private val threads:Int=2) {
    @Volatile private var engine:OfflineTts?=null
    suspend fun initialize()=withContext(Dispatchers.IO) {
        if(engine!=null)return@withContext
        val directory=KokoroModelInstaller(context).install(archiveFile)
        val config=OfflineTtsConfig(
            model=OfflineTtsModelConfig(
                kokoro=OfflineTtsKokoroModelConfig(
                    model=File(directory,"model.int8.onnx").absolutePath,
                    voices=File(directory,"voices.bin").absolutePath,
                    tokens=File(directory,"tokens.txt").absolutePath,
                    dataDir=File(directory,"espeak-ng-data").absolutePath
                ),
                numThreads=threads.coerceIn(1,8),
                debug=false,
                provider="cpu"
            ),
            silenceScale=0.2f
        )
        val loaded=OfflineTts(config=config)
        require(loaded.numSpeakers()>=11) { "The Kokoro voice pack does not contain all 11 English speakers." }
        engine=loaded
    }
    suspend fun synthesize(text:String,output:File,speed:Float=1f,speakerId:Int=0):File=withContext(Dispatchers.IO) {
        require(text.isNotBlank()) { "Kokoro cannot synthesize empty text." }
        require(text.all{it.code<128}) { "Kokoro voiceover text must be English." }
        val tts=engine?:error("Kokoro is not initialized. Download its full model bundle in Model Center.")
        output.parentFile?.mkdirs()
        val generated=tts.generateWithConfig(text,GenerationConfig(sid=speakerId.coerceIn(0,10),speed=speed.coerceIn(.75f,1.35f),silenceScale=0.2f))
        require(generated.samples.isNotEmpty()) { "Kokoro generated an empty audio clip." }
        check(generated.save(output.absolutePath)) { "Kokoro could not write its WAV voiceover." }
        output
    }
    fun release(){engine?.release();engine=null}
}