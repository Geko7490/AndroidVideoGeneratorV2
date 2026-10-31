package com.vex.videoai
import com.vex.videoai.media.SilentWavWriter
import com.vex.videoai.media.WavDurationAligner
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File
import java.nio.file.Files

class WavDurationAlignerTest {
 @Test fun padsAudioToExactTargetDuration(){
  val dir=Files.createTempDirectory("vex-wav-align").toFile()
  try {
   val file=SilentWavWriter.write(File(dir,"clip.wav"),1,16000)
   WavDurationAligner.padToDuration(file,3)
   assertEquals(44L+3L*16000L*2L,file.length())
  } finally {dir.deleteRecursively()}
 }
 @Test(expected=IllegalArgumentException::class)
 fun refusesToCutOffSpeechThatExceedsSceneLength(){
  val dir=Files.createTempDirectory("vex-wav-long").toFile()
  try {
   val file=SilentWavWriter.write(File(dir,"clip.wav"),3,16000)
   WavDurationAligner.padToDuration(file,2)
  } finally {dir.deleteRecursively()}
 }
}