package com.vex.videoai.media

import android.content.Context
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

/** Safely extracts the official sherpa-onnx Kokoro INT8 English model bundle into app-private storage. */
class KokoroModelInstaller(context:Context) {
    private val root=File(context.applicationContext.filesDir,"models")
    private val modelDir=File(root,"kokoro-int8-en-v0_19")
    fun isInstalled():Boolean = File(modelDir,"model.int8.onnx").isFile && File(modelDir,"voices.bin").isFile && File(modelDir,"tokens.txt").isFile && File(modelDir,"espeak-ng-data").listFiles()?.isNotEmpty()==true
    fun install(archive:File):File {
        require(archive.isFile&&archive.length()>1024L) { "Download the complete Kokoro INT8 English bundle in Model Center first." }
        if(isInstalled())return modelDir
        root.mkdirs()
        val staging=File(root,".kokoro-installing").apply{deleteRecursively();mkdirs()}
        var totalBytes=0L
        var entries=0
        try {
            TarArchiveInputStream(BZip2CompressorInputStream(BufferedInputStream(FileInputStream(archive)))).use { tar ->
                var entry=tar.nextTarEntry
                while(entry!=null) {
                    entries++
                    require(entries<=10000) { "Kokoro archive contains too many entries." }
                    require(!entry.isSymbolicLink&&!entry.isLink) { "Kokoro archive contains unsupported link entries." }
                    val name=entry.name.replace('\\','/')
                    require(!name.startsWith("/")&&!name.split('/').any{it==".."}) { "Kokoro archive contains an unsafe path." }
                    val relative=name.removePrefix("kokoro-int8-en-v0_19/").trim('/')
                    if(relative.isNotBlank()) {
                        val target=File(staging,relative)
                        val canonicalRoot=staging.canonicalFile
                        require(target.canonicalFile.path.startsWith(canonicalRoot.path+File.separator)) { "Kokoro archive path escaped its install directory." }
                        if(entry.isDirectory)target.mkdirs() else {
                            totalBytes+=entry.size
                            require(totalBytes<=800L*1024L*1024L) { "Kokoro model bundle expands beyond the safe size limit." }
                            target.parentFile?.mkdirs()
                            FileOutputStream(target).use{output->tar.copyTo(output)}
                        }
                    }
                    entry=tar.nextTarEntry
                }
            }
            val required=listOf("model.int8.onnx","voices.bin","tokens.txt")
            required.forEach{name->require(File(staging,name).isFile&&File(staging,name).length()>0L) { "Kokoro bundle is incomplete: missing $name." }}
            require(File(staging,"espeak-ng-data").listFiles()?.isNotEmpty()==true) { "Kokoro phonemizer data is missing." }
            if(modelDir.exists())modelDir.deleteRecursively()
            check(staging.renameTo(modelDir)) { "Could not finalize the Kokoro model installation." }
            return modelDir
        } catch(error:Throwable) {
            staging.deleteRecursively()
            throw error
        }
    }
}