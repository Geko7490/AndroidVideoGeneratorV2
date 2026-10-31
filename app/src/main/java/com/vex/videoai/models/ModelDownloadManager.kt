package com.vex.videoai.models

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import java.io.File

data class ModelAsset(val id:String,val name:String,val description:String,val sizeLabel:String,val url:String,val fileName:String,val group:String)
data class DownloadProgress(val status:Int,val downloaded:Long,val total:Long,val file:File?) {
    val percent:Int get()=if(total>0)((downloaded*100)/total).toInt().coerceIn(0,100) else 0
}
object ModelCatalog {
    val assets=listOf(
        ModelAsset("qwen3_06b","Qwen3 0.6B Q4_K_M","On-device story planner • fast starter model","~484 MB","https://huggingface.co/QuantFactory/Qwen3-0.6B-GGUF/resolve/main/Qwen3-0.6B.Q4_K_M.gguf","Qwen3-0.6B.Q4_K_M.gguf","On-device LLM"),
        ModelAsset("qwen3_4b","Qwen3 4B Q4_K_M","Higher-quality local story planning • needs more RAM","~2.5 GB","https://huggingface.co/Qwen/Qwen3-4B-GGUF/resolve/main/Qwen3-4B-Q4_K_M.gguf","Qwen3-4B-Q4_K_M.gguf","On-device LLM"),
        ModelAsset("kokoro_int8","Kokoro INT8 ONNX","Experimental model file only • Android inference runtime not integrated yet","~109 MiB","https://github.com/thewh1teagle/kokoro-onnx/releases/download/model-files-v1.1/kokoro-v1.0.int8.onnx","kokoro-v1.0.int8.onnx","Kokoro TTS"),
        
        ModelAsset("whisper_tiny_en","Whisper tiny.en","English-only transcription model • smallest recommended tier","~78 MB","https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-tiny.en.bin","ggml-tiny.en.bin","Whisper")
    )
}
class ModelDownloadManager(context:Context) {
    private val app=context.applicationContext
    private val manager=app.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
    private val prefs=app.getSharedPreferences("vex_model_downloads",Context.MODE_PRIVATE)
    fun enqueue(asset:ModelAsset):Long {
        val request=DownloadManager.Request(Uri.parse(asset.url))
            .setTitle(asset.name)
            .setDescription("Vex Video AI • ${asset.sizeLabel}")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setAllowedOverRoaming(false)
            .setDestinationInExternalFilesDir(app,Environment.DIRECTORY_DOWNLOADS,asset.fileName)
        val id=manager.enqueue(request)
        prefs.edit().putLong(asset.id,id).apply()
        return id
    }
    fun progress(asset:ModelAsset):DownloadProgress? {
        val id=prefs.getLong(asset.id,-1L)
        if(id<0)return null
        val query=DownloadManager.Query().setFilterById(id)
        manager.query(query).use { cursor ->
            if(!cursor.moveToFirst())return null
            val status=cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
            val downloaded=cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
            val total=cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
            val uri=cursor.getString(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_LOCAL_URI))
            val file=if(status==DownloadManager.STATUS_SUCCESSFUL&&uri!=null)runCatching{File(Uri.parse(uri).path.orEmpty())}.getOrNull() else null
            return DownloadProgress(status,downloaded,total,file)
        }
    }
    fun fileFor(asset:ModelAsset):File = File(app.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),asset.fileName)
    fun isDownloaded(asset:ModelAsset):Boolean {
        val p=progress(asset)
        return p?.status==DownloadManager.STATUS_SUCCESSFUL && fileFor(asset).exists()
    }
    fun cancel(asset:ModelAsset) {
        val id=prefs.getLong(asset.id,-1L)
        if(id>=0)manager.remove(id)
        prefs.edit().remove(asset.id).apply()
    }
}
