package com.vex.videoai
import com.vex.videoai.media.SilentWavWriter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files

class SilentWavWriterTest {
 @Test fun writesValidPcmWavWithExpectedDuration(){
  val dir=Files.createTempDirectory("vex-silence").toFile()
  try {
   val file=SilentWavWriter.write(File(dir,"silence.wav"),2,16000)
   assertTrue(file.exists())
   assertEquals(44L+2L*16000L*2L,file.length())
   val header=file.inputStream().use{it.readNBytes(12).toString(Charsets.US_ASCII)}
   assertEquals("RIFF",header.substring(0,4))
   assertEquals("WAVE",header.substring(8,12))
  } finally {dir.deleteRecursively()}
 }
 @Test fun rejectsUnsupportedDuration(){
  val dir=Files.createTempDirectory("vex-silence-invalid").toFile()
  try {
   val result=runCatching{SilentWavWriter.write(File(dir,"bad.wav"),0)}
   assertTrue(result.isFailure)
  } finally {dir.deleteRecursively()}
 }
}