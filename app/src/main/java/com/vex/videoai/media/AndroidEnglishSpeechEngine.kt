package com.vex.videoai.media

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale
import java.util.UUID
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Offline English fallback using the Android speech engine selected by the device.
 * This is deliberately not labelled Kokoro: Kokoro's ONNX graph still needs its
 * phonemizer/runtime integration before it can be activated.
 */
class AndroidEnglishSpeechEngine(private val context:Context) {
    suspend fun synthesize(text:String,output:File,rate:Float=1f,pitch:Float=1f):File = withContext(Dispatchers.Main.immediate) {
        require(text.isNotBlank()) { "Enter English narration before generating voiceover." }
        require(text.all { it.code<128 }) { "Voiceover text must be English." }
        output.parentFile?.mkdirs()
        suspendCancellableCoroutine { continuation ->
            var tts:TextToSpeech?=null
            var initialized=false
            val utteranceId="vex-"+UUID.randomUUID()
            fun finish(error:Throwable?=null) {
                if(!continuation.isActive)return
                if(error==null)continuation.resume(output) else continuation.resumeWithException(error)
            }
            tts=TextToSpeech(context.applicationContext) { status ->
                if(status!=TextToSpeech.SUCCESS) {
                    finish(IllegalStateException("Android speech engine initialization failed."))
                    tts?.shutdown()
                    return@TextToSpeech
                }
                val engine=tts ?: return@TextToSpeech
                val languageStatus=engine.setLanguage(Locale.US)
                if(languageStatus==TextToSpeech.LANG_MISSING_DATA||languageStatus==TextToSpeech.LANG_NOT_SUPPORTED) {
                    finish(IllegalStateException("The device does not have an English (US) TTS voice installed."))
                    engine.shutdown()
                    return@TextToSpeech
                }
                engine.setSpeechRate(rate.coerceIn(.75f,1.25f))
                engine.setPitch(pitch.coerceIn(.8f,1.2f))
                engine.setOnUtteranceProgressListener(object:UtteranceProgressListener() {
                    override fun onStart(id:String?) {}
                    override fun onDone(id:String?) {
                        if(id==utteranceId && output.exists() && output.length()>44L)finish()
                        else if(id==utteranceId)finish(IllegalStateException("Speech engine produced an empty audio file."))
                        engine.shutdown()
                    }
                    @Deprecated("Deprecated in Java")
                    override fun onError(id:String?) {
                        if(id==utteranceId)finish(IllegalStateException("English voice generation failed."))
                        engine.shutdown()
                    }
                    override fun onError(id:String?,errorCode:Int) {
                        if(id==utteranceId)finish(IllegalStateException("English voice generation failed (TTS error $errorCode)."))
                        engine.shutdown()
                    }
                })
                val result=engine.synthesizeToFile(text,Bundle(),output,utteranceId)
                if(result==TextToSpeech.ERROR) {
                    finish(IllegalStateException("The device TTS engine rejected the narration."))
                    engine.shutdown()
                }
                initialized=true
            }
            continuation.invokeOnCancellation { if(initialized)tts?.stop();tts?.shutdown() }
        }
    }
}
