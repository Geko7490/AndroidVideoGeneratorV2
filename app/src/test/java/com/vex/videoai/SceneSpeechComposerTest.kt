package com.vex.videoai
import com.vex.videoai.media.SceneSpeechComposer
import com.vex.videoai.media.SilentWavWriter
import com.vex.videoai.media.WavAudioConcatenator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files

class SceneSpeechComposerTest {
 @Test fun parsesNarrationAndSpeakerLinesInOrder(){
  val lines=SceneSpeechComposer.utterances("The door opens.","Mia: Who is there?\nNoah: It is me.")
  assertEquals(listOf("Narrator","Mia","Noah"),lines.map{it.speaker})
  assertEquals(listOf("The door opens.","Who is there?","It is me."),lines.map{it.text})
 }
 @Test fun speakerPitchIsConsistentAndNarratorNeutral(){
  assertEquals(1f,SceneSpeechComposer.pitchFor("Narrator",true),0.0001f)
  assertEquals(SceneSpeechComposer.pitchFor("Mia"),SceneSpeechComposer.pitchFor("Mia"),0.0001f)
  assertTrue(SceneSpeechComposer.pitchFor("Mia") in .92f..1.08f)
 }
 @Test fun wavClipsCanBeJoinedWithoutLosingAudioBytes(){
  val dir=Files.createTempDirectory("vex-wav-join").toFile()
  try{
   val a=SilentWavWriter.write(File(dir,"a.wav"),1)
   val b=SilentWavWriter.write(File(dir,"b.wav"),2)
   val out=WavAudioConcatenator.concatenate(listOf(a,b),File(dir,"joined.wav"))
   assertEquals(44L+3L*16000L*2L,out.length())
   assertTrue(out.readBytes().copyOfRange(0,4).toString(Charsets.US_ASCII)=="RIFF")
  }finally{dir.deleteRecursively()}
 }
}