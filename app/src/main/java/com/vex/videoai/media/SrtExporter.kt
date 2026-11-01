package com.vex.videoai.media

import com.vex.videoai.data.SubtitleLine
import com.vex.videoai.data.VideoScene

object SrtExporter {
    fun toSrt(cues:List<SubtitleLine>):String = cues
        .filter { it.text.isNotBlank() && it.endMs>it.startMs }
        .sortedBy { it.startMs }
        .mapIndexed { index,cue ->
            "${index+1}\n${timestamp(cue.startMs)} --> ${timestamp(cue.endMs)}\n${cue.text.trim()}\n"
        }.joinToString("\n")

    fun toSrtForScenes(scenes:List<VideoScene>):String {
        var offset=0L
        val all=buildList {
            scenes.forEach { scene ->
                scene.subtitles.forEach { cue -> add(cue.copy(startMs=cue.startMs+offset,endMs=cue.endMs+offset)) }
                offset+=scene.durationSeconds*1000L
            }
        }
        return toSrt(all)
    }

    private fun timestamp(value:Long):String {
        val safe=value.coerceAtLeast(0L)
        val hours=safe/3_600_000
        val minutes=(safe%3_600_000)/60_000
        val seconds=(safe%60_000)/1_000
        val millis=safe%1_000
        return "%02d:%02d:%02d,%03d".format(hours,minutes,seconds,millis)
    }
}
