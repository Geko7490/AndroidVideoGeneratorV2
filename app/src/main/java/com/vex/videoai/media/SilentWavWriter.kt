package com.vex.videoai.media
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

/** Writes a standard mono PCM WAV silence clip for scenes with no spoken content. */
object SilentWavWriter {
    fun write(output:File,durationSeconds:Int,sampleRate:Int=16000):File {
        require(durationSeconds in 1..120)
        require(sampleRate in 8000..48000)
        output.parentFile?.mkdirs()
        val dataBytes=durationSeconds.toLong()*sampleRate*2L
        require(dataBytes<=Int.MAX_VALUE-36L)
        FileOutputStream(output).use { stream ->
            stream.write("RIFF".toByteArray(Charsets.US_ASCII));stream.writeLe32((36L+dataBytes).toInt())
            stream.write("WAVE".toByteArray(Charsets.US_ASCII));stream.write("fmt ".toByteArray(Charsets.US_ASCII))
            stream.writeLe32(16);stream.writeLe16(1);stream.writeLe16(1);stream.writeLe32(sampleRate)
            stream.writeLe32(sampleRate*2);stream.writeLe16(2);stream.writeLe16(16)
            stream.write("data".toByteArray(Charsets.US_ASCII));stream.writeLe32(dataBytes.toInt())
            val zeros=ByteArray(8192);var remaining=dataBytes
            while(remaining>0){val count=minOf(remaining,zeros.size.toLong()).toInt();stream.write(zeros,0,count);remaining-=count}
        }
        return output
    }
    private fun OutputStream.writeLe16(value:Int){write(value and 255);write((value ushr 8) and 255)}
    private fun OutputStream.writeLe32(value:Int){write(value and 255);write((value ushr 8) and 255);write((value ushr 16) and 255);write((value ushr 24) and 255)}
}