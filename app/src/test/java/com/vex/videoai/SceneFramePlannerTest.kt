package com.vex.videoai
import com.vex.videoai.data.SubtitleLine
import com.vex.videoai.media.SceneFramePlanner
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SceneFramePlannerTest {
 @Test fun createsMotionIntervalsAndKeepsSubtitleBoundaries(){
  val cues=listOf(SubtitleLine(startMs=620,endMs=1180,text="Hello"))
  val frames=SceneFramePlanner.plan(2000,cues,500)
  assertEquals(5,frames.size)
  assertTrue(frames.any{it.startMs==620L})
  assertTrue(frames.any{it.endMs==1180L})
  assertTrue(frames.first().progress<frames.last().progress)
 }
 @Test fun coversEntireSceneWithoutGaps(){
  val frames=SceneFramePlanner.plan(2300,emptyList(),500)
  assertEquals(0L,frames.first().startMs)
  assertEquals(2300L,frames.last().endMs)
  frames.zipWithNext().forEach{(a,b)->assertEquals(a.endMs,b.startMs)}
 }
}