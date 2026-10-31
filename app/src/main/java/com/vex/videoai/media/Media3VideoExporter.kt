package com.vex.videoai.media

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.graphics.RectF
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.EditedMediaItemSequence
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.ProgressHolder
import androidx.media3.transformer.Transformer
import com.vex.videoai.data.VideoProject
import com.vex.videoai.data.VideoScene
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

private data class RenderFrame(val file:File,val durationMs:Long)

@OptIn(UnstableApi::class)
class Media3VideoExporter(context:Context) {
    private val app=context.applicationContext

    suspend fun export(project:VideoProject,outputFile:File,resolutionLongEdge:Int=1080,frameRate:Int=30,onProgress:(Int)->Unit={}):File {
        require(project.scenes.isNotEmpty()) { "Add at least one scene before exporting." }
        outputFile.parentFile?.mkdirs()
        val workDir=File(app.cacheDir,"vex-render-${project.id}").apply { deleteRecursively();mkdirs() }
        return try {
            val frames=withContext(Dispatchers.IO) { renderFrames(project,workDir,resolutionLongEdge.coerceIn(480,2160)) }
            withContext(Dispatchers.Main.immediate) { onProgress(25) }
            val items=frames.map { frame ->
                val mediaItem=MediaItem.Builder().setUri(frame.file.toUri()).setImageDurationMs(frame.durationMs).build()
                EditedMediaItem.Builder(mediaItem).setFrameRate(frameRate.coerceIn(24,60)).build()
            }
            val sequence=EditedMediaItemSequence.withVideoFrom(items)
            val audioFiles=project.scenes.map { scene ->
                scene.voiceoverPath.takeIf { it.isNotBlank() }?.let(::File)
            }
            val hasCompleteAudio=audioFiles.isNotEmpty()&&audioFiles.all { file -> file!=null&&file.isFile&&file.length()>44L }
            val composition=if(hasCompleteAudio) {
                val audioItems=audioFiles.filterNotNull().map { file ->
                    EditedMediaItem.Builder(MediaItem.Builder().setUri(file.toUri()).build()).build()
                }
                val audioSequence=EditedMediaItemSequence.withAudioFrom(audioItems)
                Composition.Builder(sequence,audioSequence).build()
            } else {
                Composition.Builder(sequence).build()
            }
            withContext(Dispatchers.Main.immediate) {
                suspendCancellableCoroutine { continuation ->
                    val transformer=Transformer.Builder(app).build()
                    var progressJob:Job?=null
                    val listener=object:Transformer.Listener {
                        override fun onCompleted(composition:Composition,exportResult:ExportResult) {
                            progressJob?.cancel()
                            transformer.removeListener(this)
                            onProgress(100)
                            if(continuation.isActive)continuation.resume(outputFile)
                        }
                        override fun onError(composition:Composition,exportResult:ExportResult,exportException:ExportException) {
                            progressJob?.cancel()
                            transformer.removeListener(this)
                            if(continuation.isActive)continuation.resumeWithException(exportException)
                        }
                    }
                    transformer.addListener(listener)
                    continuation.invokeOnCancellation {
                        progressJob?.cancel()
                        transformer.cancel()
                        transformer.removeListener(listener)
                    }
                    try {
                        onProgress(35)
                        transformer.start(composition,outputFile.absolutePath)
                        progressJob=CoroutineScope(continuation.context).launch {
                            while(continuation.isActive) {
                                val holder=ProgressHolder()
                                val state=withContext(Dispatchers.Main.immediate) { transformer.getProgress(holder) }
                                if(state==Transformer.PROGRESS_STATE_AVAILABLE) {
                                    withContext(Dispatchers.Main.immediate) { onProgress(35+(holder.progress*65/100).coerceIn(0,64)) }
                                }
                                delay(500)
                            }
                        }
                    } catch(error:Throwable) {
                        transformer.removeListener(listener)
                        if(continuation.isActive)continuation.resumeWithException(error)
                    }
                }
            }
        } finally {
            workDir.deleteRecursively()
        }
    }

    private fun renderFrames(project:VideoProject,dir:File,longEdge:Int):List<RenderFrame> {
        val dimensions=VideoOutputDimensions.resolve(project.aspectRatio,longEdge)
        val width=dimensions.width
        val height=dimensions.height
        val result=mutableListOf<RenderFrame>()
        var index=0
        project.scenes.forEach { scene ->
            val totalMs=scene.durationSeconds.coerceAtLeast(1)*1000L
            val cues=scene.subtitles.filter { it.text.isNotBlank() && it.endMs>it.startMs }
                .map { it.copy(startMs=it.startMs.coerceIn(0,totalMs),endMs=it.endMs.coerceIn(0,totalMs)) }
                .filter { it.endMs>it.startMs }
            val points=(listOf(0L,totalMs)+cues.flatMap { listOf(it.startMs,it.endMs) }).distinct().sorted()
            points.zipWithNext().forEach { (start,end) ->
                if(end<=start)return@forEach
                val caption=cues.firstOrNull { it.startMs<end && it.endMs>start }?.text.orEmpty()
                val bitmap=drawScene(project,scene,caption,width,height)
                val file=File(dir,"frame-${index.toString().padStart(4,'0')}.png");index++
                FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.PNG,100,it) }
                bitmap.recycle()
                result.add(RenderFrame(file,end-start))
            }
        }
        require(result.isNotEmpty()) { "No frames could be generated." }
        return result
    }

    private fun drawScene(project:VideoProject,scene:VideoScene,caption:String,width:Int,height:Int):Bitmap {
        val bitmap=Bitmap.createBitmap(width,height,Bitmap.Config.ARGB_8888)
        val canvas=AndroidCanvas(bitmap)
        val paint=Paint(Paint.ANTI_ALIAS_FLAG)
        canvas.drawColor(AndroidColor.rgb(16,14,24))
        paint.shader=android.graphics.LinearGradient(0f,0f,width.toFloat(),height.toFloat(),AndroidColor.rgb(26,19,43),AndroidColor.rgb(9,12,20),android.graphics.Shader.TileMode.CLAMP)
        canvas.drawRect(0f,0f,width.toFloat(),height.toFloat(),paint)
        paint.shader=null
        paint.color=AndroidColor.argb(65,168,85,247)
        canvas.drawCircle(width*.78f,height*.22f,width*.13f,paint)
        paint.color=AndroidColor.argb(100,40,32,58)
        canvas.drawRect(0f,height*.66f,width.toFloat(),height.toFloat(),paint)
        scene.layers.filter { it.visible }.forEachIndexed { layerIndex,layer ->
            val x=layer.x.coerceIn(0f,1f)*width
            val y=layer.y.coerceIn(0f,1f)*height
            val radius=(width*.045f*layer.scale.coerceIn(.25f,3f)).coerceAtLeast(12f)
            paint.color=when(layer.kind) {
                "background" -> AndroidColor.argb(105,74,58,105)
                "text" -> AndroidColor.WHITE
                else -> AndroidColor.rgb(95+(layerIndex*17%100),65+(layerIndex*13%90),135+(layerIndex*11%100))
            }
            val asset=if(layer.assetPath.isNotBlank())BitmapFactory.decodeFile(layer.assetPath) else null
            if(asset!=null) {
                paint.color=AndroidColor.WHITE
                val targetWidth=(width*.58f*layer.scale.coerceIn(.1f,2f)).coerceAtMost(width*.9f)
                val targetHeight=targetWidth*asset.height.toFloat()/asset.width.coerceAtLeast(1)
                canvas.drawBitmap(asset,null,RectF(x-targetWidth/2,y-targetHeight/2,x+targetWidth/2,y+targetHeight/2),paint)
                asset.recycle()
            } else if(layer.kind=="background") {
                canvas.drawRoundRect(RectF(x-radius*2,y-radius,width-radius*.2f,y+radius*3),radius,radius,paint)
                paint.color=AndroidColor.argb(210,245,241,255)
                paint.textSize=width*.026f
                paint.typeface=android.graphics.Typeface.create("sans-serif-medium",android.graphics.Typeface.NORMAL)
                canvas.drawText(layer.name.take(30),x+radius*.15f,y+radius*.35f,paint)
            } else if(layer.kind=="text") {
                paint.textSize=width*.052f;paint.typeface=android.graphics.Typeface.create("sans-serif-bold",android.graphics.Typeface.BOLD)
                paint.setShadowLayer(5f,2f,2f,AndroidColor.BLACK)
                canvas.drawText(layer.name.take(36),x,y,paint)
                paint.clearShadowLayer()
            } else {
                canvas.drawCircle(x,y,radius,paint)
                paint.color=AndroidColor.WHITE
                paint.textSize=width*.025f
                paint.typeface=android.graphics.Typeface.create("sans-serif-bold",android.graphics.Typeface.BOLD)
                val label=layer.name.take(22)
                val labelWidth=paint.measureText(label)
                canvas.drawText(label,(x-labelWidth/2).coerceIn(8f,width-labelWidth-8f),(y+radius+width*.035f).coerceAtMost(height*.78f),paint)
            }
        }
        paint.color=AndroidColor.WHITE
        paint.textSize=width*.055f
        paint.typeface=android.graphics.Typeface.create("sans-serif-bold",android.graphics.Typeface.BOLD)
        canvas.drawText(project.title.take(42),width*.07f,height*.09f,paint)
        paint.color=AndroidColor.LTGRAY
        paint.textSize=width*.027f
        paint.typeface=android.graphics.Typeface.create("sans-serif",android.graphics.Typeface.NORMAL)
        val description=scene.visualDescription.take(110)
        canvas.drawText(description,width*.07f,height*.14f,paint)
        if(caption.isNotBlank()) {
            val maxWidth=width*.82f
            paint.textSize=width*.043f
            paint.typeface=android.graphics.Typeface.create("sans-serif-bold",android.graphics.Typeface.BOLD)
            val lines=wrapText(caption,paint,maxWidth)
            val lineHeight=paint.textSize*1.25f
            val boxHeight=lines.size*lineHeight+height*.035f
            val left=width*.06f;val top=height*.86f-boxHeight
            paint.color=AndroidColor.argb(205,0,0,0)
            canvas.drawRoundRect(RectF(left,top,width*.94f,height*.86f),width*.018f,width*.018f,paint)
            paint.color=AndroidColor.WHITE
            lines.forEachIndexed { i,line->canvas.drawText(line,width*.09f,top+height*.025f+lineHeight*(i+1),paint) }
        }
        return bitmap
    }

    private fun wrapText(text:String,paint:Paint,maxWidth:Float):List<String> {
        val words=text.split(Regex("\\s+"))
        val lines=mutableListOf<String>();var current=""
        words.forEach { word ->
            val candidate=if(current.isEmpty())word else "$current $word"
            if(current.isNotEmpty()&&paint.measureText(candidate)>maxWidth){lines.add(current);current=word}else current=candidate
        }
        if(current.isNotEmpty())lines.add(current)
        return lines.take(4)
    }
}
