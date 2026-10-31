package com.vex.videoai.media

import android.content.Context
import com.vex.videoai.data.SubtitleLine
import dev.ffmpegkit.whisper.Whisper
import dev.ffmpegkit.whisper.WhisperConfig
import java.io.File

class WhisperLocalEngine(private val context:Context) {
    suspend fun transcribeEnglish(audioFile:File,modelFile:File):List<SubtitleLine> {
        require(audioFile.exists()&&audioFile.length()>0L) { "Select a valid audio file." }
        require(modelFile.exists()&&modelFile.length()>0L) { "Download Whisper tiny.en from Model Center first." }
        val model=Whisper.loadModel(context,modelFile.absolutePath)
        try {
            val result=Whisper.transcribe(model,audioFile.absolutePath,WhisperConfig(language="en"))
            return result.segments.map { segment ->
                SubtitleLine(startMs=segment.startMs,endMs=segment.endMs,text=segment.text.trim())
            }.filter { it.text.isNotBlank() && it.endMs>it.startMs }
        } finally {
            Whisper.releaseModel(model)
        }
    }
}
