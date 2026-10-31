package com.vex.videoai.planning

import com.vex.videoai.data.VideoScene
import kotlin.math.roundToInt

object SceneDurationNormalizer {
    fun normalize(scenes:List<VideoScene>,targetSeconds:Int):List<VideoScene> {
        require(scenes.isNotEmpty()) { "The model returned no scenes." }
        require(targetSeconds>=scenes.size && targetSeconds<=scenes.size*120) {
            "The model returned ${scenes.size} scenes, which cannot cover the requested ${targetSeconds}-second video. Regenerate the storyboard."
        }
        val original=scenes.map { it.durationSeconds.coerceIn(1,120) }
        val total=original.sum()
        val durations=original.map { ((it.toDouble()/total)*targetSeconds).roundToInt().coerceIn(1,120) }.toMutableList()
        var difference=targetSeconds-durations.sum()
        while(difference!=0) {
            val order=durations.indices.sortedByDescending { durations[it] }
            var changed=false
            for(index in order) {
                if(difference>0 && durations[index]<120) {
                    durations[index]++;difference--;changed=true
                } else if(difference<0 && durations[index]>1) {
                    durations[index]--;difference++;changed=true
                }
                if(difference==0)break
            }
            check(changed) { "Could not fit scene durations into the requested video length." }
        }
        return scenes.mapIndexed { index,scene -> scene.copy(durationSeconds=durations[index]) }
    }
}
