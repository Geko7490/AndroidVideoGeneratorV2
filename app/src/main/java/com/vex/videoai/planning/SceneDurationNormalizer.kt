package com.vex.videoai.planning
import com.vex.videoai.data.VideoScene
import kotlin.math.ceil

/** Allocates exact project duration while reserving enough speaking time for each scene. */
object SceneDurationNormalizer {
    private const val MAX_WORDS_PER_SECOND=2.5
    fun normalize(scenes:List<VideoScene>,targetSeconds:Int):List<VideoScene> {
        require(scenes.isNotEmpty()) { "The model returned no scenes." }
        require(targetSeconds>=scenes.size && targetSeconds<=scenes.size*120) {
            "The model returned ${scenes.size} scenes, which cannot cover the requested ${targetSeconds}-second video. Regenerate the storyboard."
        }
        val weights=scenes.map{it.durationSeconds.coerceIn(1,120)}
        val minimums=scenes.map{scene->
            val spoken=(scene.narration+" "+scene.dialogue.lines().joinToString(" "){it.substringAfter(':',it)}).trim()
            val words=if(spoken.isBlank())0 else spoken.split(Regex("\\s+")).size
            ceil(words/MAX_WORDS_PER_SECOND).toInt().coerceAtLeast(1).coerceAtMost(120)
        }
        require(minimums.sum()<=targetSeconds) {
            "The generated narration and dialogue need at least ${minimums.sum()} seconds at a natural speaking pace, but the selected video is only ${targetSeconds} seconds. Increase the duration or regenerate the script."
        }
        val durations=minimums.toMutableList()
        var remaining=targetSeconds-durations.sum()
        while(remaining>0) {
            val eligible=durations.indices.filter{durations[it]<120}
            check(eligible.isNotEmpty()) { "Could not fit scene durations into the requested video length." }
            val index=eligible.maxBy{weights[it].toDouble()/(durations[it]-minimums[it]+1)}
            durations[index]++
            remaining--
        }
        return scenes.mapIndexed{index,scene->scene.copy(durationSeconds=durations[index])}
    }
}