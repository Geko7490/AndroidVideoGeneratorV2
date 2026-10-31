package com.vex.videoai.media
import java.io.File

/** Pads PCM WAV clips to the exact scene length so sequential audio remains in sync with video scenes. */
object WavDurationAligner {
    fun padToDuration(file:File,targetSeconds:Int):File {
        require(targetSeconds in 1..120)
        val bytes=file.readBytes()
        require(bytes.size>=44&&ascii(bytes,0,4)=="RIFF"&&ascii(bytes,8,4)=="WAVE") { "Speech engine did not produce a valid WAV file." }
        var offset=12;var byteRate=0;var dataOffset=-1;var dataSize=-1;var dataSizeField=-1
        while(offset+8<=bytes.size) {
            val chunk=ascii(bytes,offset,4);val chunkSize=readLe32(bytes,offset+4)
            require(chunkSize>=0&&offset+8L+chunkSize<=bytes.size) { "Invalid WAV chunk length." }
            if(chunk=="fmt ") {
                require(chunkSize>=16) { "Unsupported WAV format header." }
                val format=readLe16(bytes,offset+8)
                require(format==1) { "Only PCM WAV voiceovers are supported." }
                byteRate=readLe32(bytes,offset+16)
            } else if(chunk=="data") { dataOffset=offset+8;dataSize=chunkSize;dataSizeField=offset+4;break }
            offset+=8+chunkSize+(chunkSize and 1)
        }
        require(byteRate>0&&dataOffset>=0&&dataSize>=0) { "WAV file is missing its PCM audio data." }
        require(dataOffset+dataSize==bytes.size) { "Unsupported WAV layout with trailing chunks." }
        val targetSizeLong=byteRate.toLong()*targetSeconds
        require(targetSizeLong<=Int.MAX_VALUE-dataOffset) { "Scene audio is too large to align." }
        val targetSize=targetSizeLong.toInt()
        require(dataSize<=targetSize) { "Voiceover is longer than its scene. Shorten the script or increase the video duration." }
        if(dataSize==targetSize)return file
        val padded=ByteArray(dataOffset+targetSize)
        bytes.copyInto(padded,0,0,dataOffset+dataSize)
        writeLe32(padded,dataSizeField,targetSize);writeLe32(padded,4,padded.size-8)
        val temp=File(file.parentFile,file.name+".aligning");temp.writeBytes(padded)
        if(file.exists()&&!file.delete()){temp.delete();error("Could not replace the scene voiceover file.")}
        if(!temp.renameTo(file)){temp.delete();error("Could not finalize the aligned scene voiceover.")}
        return file
    }
    private fun ascii(bytes:ByteArray,start:Int,length:Int)=String(bytes,start,length,Charsets.US_ASCII)
    private fun readLe16(bytes:ByteArray,index:Int)=(bytes[index].toInt() and 255) or ((bytes[index+1].toInt() and 255) shl 8)
    private fun readLe32(bytes:ByteArray,index:Int)=(bytes[index].toInt() and 255) or ((bytes[index+1].toInt() and 255) shl 8) or ((bytes[index+2].toInt() and 255) shl 16) or ((bytes[index+3].toInt() and 255) shl 24)
    private fun writeLe32(bytes:ByteArray,index:Int,value:Int){for(i in 0..3)bytes[index+i]=(value ushr (8*i)).toByte()}
}