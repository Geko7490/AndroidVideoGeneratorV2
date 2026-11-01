package com.vex.videoai.media

import com.vex.videoai.data.SubtitleLine

data class SceneFrameInterval(val startMs:Long,val endMs:Long,val progress:Float)

/** Creates short still-frame intervals for gentle camera motion and exact subtitle cue boundaries. */
object SceneFramePlanner {
    fun plan(durationMs:Long,cues:List<SubtitleLine>,frameStepMs:Long=500L):List<SceneFrameInterval> {
        require(durationMs>0L)
        require(frameStepMs in 100L..2000L)
        val boundaries=sortedSetOf(0L,durationMs)
        var cursor=frameStepMs
        while(cursor<durationMs){boundaries.add(cursor);cursor+=frameStepMs}
        cues.filter{it.text.isNotBlank()&&it.endMs>it.startMs}.forEach{cue->
            boundaries.add(cue.startMs.coerceIn(0L,durationMs))
            boundaries.add(cue.endMs.coerceIn(0L,durationMs))
        }
        return boundaries.toList().zipWithNext().mapNotNull{(start,end)->
            if(end<=start)null else SceneFrameInterval(start,end,((start+end)/2f/durationMs).coerceIn(0f,1f))
        }
    }
}