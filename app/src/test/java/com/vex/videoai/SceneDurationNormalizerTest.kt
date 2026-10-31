package com.vex.videoai
import com.vex.videoai.data.VideoScene
import com.vex.videoai.planning.SceneDurationNormalizer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SceneDurationNormalizerTest {
 @Test fun normalizesSceneDurationsToExactProjectLength(){
  val scenes=listOf(VideoScene(title="Opening",durationSeconds=5),VideoScene(title="Middle",durationSeconds=8),VideoScene(title="Ending",durationSeconds=7))
  val normalized=SceneDurationNormalizer.normalize(scenes,60)
  assertEquals(60,normalized.sumOf{it.durationSeconds})
  assertTrue(normalized.all{it.durationSeconds in 1..120})
 }
 @Test fun reservesEnoughTimeForLongerSpokenScenes(){
  val narration=(1..30).joinToString(" "){"word$it"}
  val scenes=listOf(VideoScene(title="Spoken",durationSeconds=5,narration=narration),VideoScene(title="Silent",durationSeconds=5))
  val normalized=SceneDurationNormalizer.normalize(scenes,20)
  assertTrue(normalized[0].durationSeconds>=12)
  assertTrue(normalized[0].durationSeconds>normalized[1].durationSeconds)
  assertEquals(20,normalized.sumOf{it.durationSeconds})
 }
 @Test(expected=IllegalArgumentException::class)
 fun rejectsSpeechThatCannotFitTheRequestedDuration(){
  val narration=(1..80).joinToString(" "){"word$it"}
  SceneDurationNormalizer.normalize(listOf(VideoScene(title="Talk",narration=narration)),10)
 }
 @Test(expected=IllegalArgumentException::class)
 fun rejectsTooFewScenesForMaximumSceneDuration(){SceneDurationNormalizer.normalize(listOf(VideoScene(title="Only scene")),600)}
}