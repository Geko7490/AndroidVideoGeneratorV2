package com.vex.videoai
import com.vex.videoai.data.SceneLayer
import com.vex.videoai.data.VideoProject
import com.vex.videoai.data.VideoScene
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
class ProjectStoreModelTest {
 @Test fun projectJsonRoundTripsScenesAndLayers() {
  val p=VideoProject(title="Night Signal",idea="A strange radio signal",category="horror",durationSeconds=45,aspectRatio="9:16",visualStyle="Cinematic",scenes=listOf(VideoScene(title="The attic",durationSeconds=7,narration="The signal returned.",layers=listOf(SceneLayer(name="Attic",kind="background"),SceneLayer(name="Radio")))))
  val restored=VideoProject.fromJson(p.toJson());assertEquals(p.id,restored.id);assertEquals(p.scenes.first().narration,restored.scenes.first().narration);assertEquals(2,restored.scenes.first().layers.size);assertTrue(restored.scenes.first().durationSeconds in 1..120)
 }
}
