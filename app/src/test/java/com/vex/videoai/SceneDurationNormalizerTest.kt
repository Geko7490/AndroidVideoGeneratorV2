package com.vex.videoai

import com.vex.videoai.data.VideoScene
import com.vex.videoai.planning.SceneDurationNormalizer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SceneDurationNormalizerTest {
    @Test fun normalizesSceneDurationsToExactProjectLength() {
        val scenes=listOf(
            VideoScene(title="Opening",durationSeconds=5),
            VideoScene(title="Middle",durationSeconds=8),
            VideoScene(title="Ending",durationSeconds=7)
        )
        val normalized=SceneDurationNormalizer.normalize(scenes,60)
        assertEquals(60,normalized.sumOf { it.durationSeconds })
        assertTrue(normalized.all { it.durationSeconds in 1..120 })
    }

    @Test(expected=IllegalArgumentException::class)
    fun rejectsTooFewScenesForMaximumSceneDuration() {
        SceneDurationNormalizer.normalize(listOf(VideoScene(title="Only scene")),600)
    }
}
