package com.vex.videoai.media
import java.io.File
import kotlin.math.abs

data class SceneUtterance(val speaker:String,val text:String,val isNarration:Boolean=false)

/** Generates each narrator/dialogue line separately, varies speaker pitch slightly, then joins the PCM clips in order. */
object SceneSpeechComposer {
    fun utterances(narration:String,dialogue:String):List<SceneUtterance> = buildList {
        if(narration.isNotBlank())add(SceneUtterance("Narrator",narration.trim(),true))
        dialogue.lineSequence().map{it.trim()}.filter{it.isNotBlank()}.forEach{line->
            val split=line.indexOf(':')
            if(split>0){val speaker=line.substring(0,split).trim();val text=line.substring(split+1).trim();if(text.isNotBlank())add(SceneUtterance(speaker.ifBlank{"Character"},text))}
            else add(SceneUtterance("Character",line))
        }
    }
    fun pitchFor(speaker:String,isNarration:Boolean=false):Float {
        if(isNarration)return 1f
        return when(abs(speaker.lowercase().hashCode()%3)){0->.92f;1->1f;else->1.08f}
    }
    suspend fun synthesize(utterances:List<SceneUtterance>,output:File,rate:Float=1f,synthesizeClip:suspend (text:String,file:File,rate:Float,pitch:Float)->File):File {
        require(utterances.isNotEmpty()) { "This scene has no spoken lines." }
        val work=File(output.parentFile,output.nameWithoutExtension+"-clips").apply{deleteRecursively();mkdirs()}
        val clips=mutableListOf<File>()
        try {
            utterances.forEachIndexed{index,line->
                val clip=File(work,"line-${index.toString().padStart(3,'0')}.wav")
                synthesizeClip(line.text,clip,rate,pitchFor(line.speaker,line.isNarration))
                require(clip.isFile&&clip.length()>44L) { "Speech generation failed for ${line.speaker}." }
                clips.add(clip)
            }
            WavAudioConcatenator.concatenate(clips,output)
            return output
        } finally {work.deleteRecursively()}
    }
}