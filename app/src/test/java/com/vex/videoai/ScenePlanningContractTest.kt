package com.vex.videoai

import com.vex.videoai.planning.ContentType
import com.vex.videoai.planning.ScenePlanningContract
import com.vex.videoai.planning.ScenePlanningRequest
import org.junit.Assert.assertTrue
import org.junit.Test

class ScenePlanningContractTest {
    @Test
    fun systemPromptLocksGeneratedVideoLanguageToEnglish() {
        val prompt = ScenePlanningContract.systemPrompt()
        assertTrue(prompt.contains("MUST be in English"))
        assertTrue(prompt.contains("Never translate the generated video content"))
        assertTrue(prompt.contains("Return valid JSON only"))
    }

    @Test
    fun categoryDirectionIsIncludedInUserPrompt() {
        val prompt = ScenePlanningContract.userPrompt(
            ScenePlanningRequest("A strange signal from an empty house", ContentType.HORROR, 45, "9:16", "Cinematic")
        )
        assertTrue(prompt.contains("Content type: horror"))
        assertTrue(prompt.contains("Build dread progressively"))
        assertTrue(prompt.contains("Target duration: 45 seconds"))
    }

    @Test
    fun generatedVideoStartsWithStoryActionAndHasNoIntro() {
        val prompt=ScenePlanningContract.systemPrompt()
        assertTrue(prompt.contains("Do NOT create a separate intro"))
        assertTrue(prompt.contains("first meaningful action"))
    }

    @Test
    fun dialogueRequiresSpeakerIdentity() {
        val prompt=ScenePlanningContract.systemPrompt()
        assertTrue(prompt.contains("speaker"))
        assertTrue(prompt.contains("exact English line"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun blankIdeaIsRejected() {
        ScenePlanningContract.userPrompt(
            ScenePlanningRequest(" ", ContentType.STORY, 30, "9:16", "2D Art")
        )
    }
}
