package com.vex.videoai.media
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

/** Joins PCM WAV clips while retaining a valid RIFF header and their original audio format. */
object WavAudioConcatenator {
    private data class PcmWav(val format:ByteArray,val audio:ByteArray)
    fun concatenate(inputs:List<File>,output:File):File {
        require(inputs.isNotEmpty()) { "No voice clips were provided." }
        val parsed=inputs.map(::readPcmWav)
        val first=parsed.first()
        require(parsed.all{it.format.contentEquals(first.format)}) { "Voice clips use different WAV formats and cannot be joined safely." }
        val total=parsed.sumOf{it.audio.size.toLong()}
        require(total<=Int.MAX_VALUE-64L) { "Combined voiceover is too large." }
        val fmt=first.format
        output.parentFile?.mkdirs()
        FileOutputStream(output).use{stream->
            stream.write("RIFF".toByteArray(Charsets.US_ASCII));stream.writeLe32((20L+fmt.size+total).toInt())
            stream.write("WAVE".toByteArray(Charsets.US_ASCII));stream.write("fmt ".toByteArray(Charsets.US_ASCII));stream.writeLe32(fmt.size);stream.write(fmt)
            stream.write("data".toByteArray(Charsets.US_ASCII));stream.writeLe32(total.toInt())
            parsed.forEach{stream.write(it.audio)}
        }
        return output
    }
    private fun readPcmWav(file:File):PcmWav {
        val bytes=file.readBytes()
        require(bytes.size>=44&&ascii(bytes,0,4)=="RIFF"&&ascii(bytes,8,4)=="WAVE") { "A speech clip is not a valid WAV file." }
        var offset=12;var format:ByteArray?=null;var audio:ByteArray?=null
        while(offset+8<=bytes.size){
            val id=ascii(bytes,offset,4);val size=readLe32(bytes,offset+4)
            require(size>=0&&offset+8L+size<=bytes.size) { "A speech clip has a broken WAV chunk." }
            if(id=="fmt "){require(size>=16&&readLe16(bytes,offset+8)==1) { "Only PCM WAV voice clips are supported." };format=bytes.copyOfRange(offset+8,offset+8+size)}
            else if(id=="data"){audio=bytes.copyOfRange(offset+8,offset+8+size);break}
            offset+=8+size+(size and 1)
        }
        return PcmWav(format?:error("WAV format chunk is missing."),audio?:error("WAV audio data is missing."))
    }
    private fun ascii(b:ByteArray,i:Int,n:Int)=String(b,i,n,Charsets.US_ASCII)
    private fun readLe16(b:ByteArray,i:Int)=(b[i].toInt() and 255) or ((b[i+1].toInt() and 255) shl 8)
    private fun readLe32(b:ByteArray,i:Int)=(b[i].toInt() and 255) or ((b[i+1].toInt() and 255) shl 8) or ((b[i+2].toInt() and 255) shl 16) or ((b[i+3].toInt() and 255) shl 24)
    private fun OutputStream.writeLe32(v:Int){write(v and 255);write((v ushr 8) and 255);write((v ushr 16) and 255);write((v ushr 24) and 255)}
}