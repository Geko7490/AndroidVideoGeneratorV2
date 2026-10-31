package com.vex.videoai.media

import android.content.Context
import com.vex.videoai.data.SubtitleLine
import dev.ffmpegkit.whisper.Whisper
import dev.ffmpegkit.whisper.WhisperConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class WhisperLocalEngine(private val context:Context) {
    suspend fun transcribeEnglish(audioFile:File,modelFile:File):List<SubtitleLine> = transcribeEnglishBatch(listOf(audioFile),modelFile).first()

    suspend fun transcribeEnglishBatch(audioFiles:List<File>,modelFile:File,onProgress:(Int,Int)->Unit={}):List<List<SubtitleLine>> = withContext(Dispatchers.IO) {
        require(audioFiles.isNotEmpty()) { "There are no voiceover files to transcribe." }
        require(audioFiles.all{it.exists()&&it.length()>0L}) { "One or more voiceover files are missing." }
        require(modelFile.exists()&&modelFile.length()>0L) { "Download Whisper tiny.en from Model Center first." }
        val model=Whisper.loadModel(context,modelFile.absolutePath)
        try {
            audioFiles.mapIndexed{index,audioFile->
                val result=Whisper.transcribe(model,audioFile.absolutePath,WhisperConfig(language="en"))
                onProgress(index+1,audioFiles.size)
                result.segments.map{segment->SubtitleLine(startMs=segment.startMs,endMs=segment.endMs,text=segment.text.trim())}.filter{it.text.isNotBlank()&&it.endMs>it.startMs}
            }
        } finally { Whisper.releaseModel(model) }
    }
}