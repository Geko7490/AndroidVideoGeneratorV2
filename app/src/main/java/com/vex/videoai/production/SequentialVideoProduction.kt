package com.vex.videoai.production
import android.content.Context
import com.vex.videoai.ai.LlmSettings
import com.vex.videoai.ai.LocalLlmClient
import com.vex.videoai.ai.OnDeviceLlmClient
import com.vex.videoai.data.SubtitleLine
import com.vex.videoai.data.VideoProject
import com.vex.videoai.data.VideoScene
import com.vex.videoai.media.AndroidEnglishSpeechEngine
import com.vex.videoai.media.Media3VideoExporter
import com.vex.videoai.media.SilentWavWriter
import com.vex.videoai.media.WhisperLocalEngine
import java.io.File

enum class ProductionStage(val label:String) { PREPARING("Preparing"),STORYBOARD("LLM • Script and storyboard"),VOICEOVER("Speech • Scene voiceovers"),SUBTITLES("Whisper • English subtitles"),RENDER("Render • MP4"),COMPLETE("Complete") }
data class ProductionProgress(val stage:ProductionStage,val completed:Int,val total:Int,val detail:String="")

class SequentialVideoProduction(private val context:Context) {
    suspend fun run(source:VideoProject,settings:LlmSettings,localModel:File?,whisperModel:File,profile:ProductionProfile,outputFile:File,onProgress:(ProductionProgress)->Unit={}):VideoProject {
        require(whisperModel.exists()&&whisperModel.length()>0L) { "Download Whisper tiny.en in Model Center before running the full pipeline." }
        onProgress(ProductionProgress(ProductionStage.PREPARING,0,4,"Checking models and output storage"))
        val cpuCount=Runtime.getRuntime().availableProcessors().coerceAtLeast(2)
        onProgress(ProductionProgress(ProductionStage.STORYBOARD,0,4,"Generating the complete English script"))
        val scenes=if(localModel?.exists()==true&&localModel.length()>0L) {
            OnDeviceLlmClient(context).createStoryboard(source,localModel,threads=profile.threadsFor(cpuCount),contextSize=profile.contextSize,maxTokens=profile.maxTokens)
        } else LocalLlmClient().createStoryboard(source,settings.copy(maxTokens=profile.maxTokens))
        onProgress(ProductionProgress(ProductionStage.STORYBOARD,1,4,"${scenes.size} scenes planned; no separate intro scene"))
        val speech=AndroidEnglishSpeechEngine(context)
        val audioDir=File(context.filesDir,"vex-video-projects/"+source.id).apply{mkdirs()}
        val voiced=mutableListOf<VideoScene>()
        val spokenSceneIndexes=mutableListOf<Int>()
        scenes.forEachIndexed{index,scene->
            val spokenLines=buildList{
                if(scene.narration.isNotBlank())add(scene.narration)
                scene.dialogue.lines().forEach{line->
                    val spoken=line.substringAfter(':',line).trim()
                    if(spoken.isNotBlank())add(spoken)
                }
            }
            val audio=File(audioDir,scene.id+"-voice.wav")
            if(spokenLines.isEmpty()) {
                SilentWavWriter.write(audio,scene.durationSeconds)
            } else {
                speech.synthesize(spokenLines.joinToString("\n"),audio)
                spokenSceneIndexes.add(index)
            }
            voiced.add(scene.copy(voiceoverPath=audio.absolutePath))
            onProgress(ProductionProgress(ProductionStage.VOICEOVER,index+1,scenes.size,if(spokenLines.isEmpty())"Silent scene ${index+1}/${scenes.size}" else "Voiceover ${index+1}/${scenes.size}"))
        }
        require(spokenSceneIndexes.isNotEmpty()) { "The storyboard contains no narration or dialogue. Regenerate it with spoken English content." }
        val subtitlesByScene=MutableList(scenes.size){emptyList<SubtitleLine>()}
        val subtitleBatches=WhisperLocalEngine(context).transcribeEnglishBatch(spokenSceneIndexes.map{File(voiced[it].voiceoverPath)},whisperModel){completed,total->
            onProgress(ProductionProgress(ProductionStage.SUBTITLES,completed,total,"Whisper pass ${completed}/${total}"))
        }
        spokenSceneIndexes.forEachIndexed{batchIndex,sceneIndex->subtitlesByScene[sceneIndex]=subtitleBatches[batchIndex]}
        val completedProject=source.copy(scenes=voiced.mapIndexed{index,scene->scene.copy(subtitles=subtitlesByScene[index])})
        onProgress(ProductionProgress(ProductionStage.RENDER,0,1,"Rendering ${profile.defaultResolution} at ${profile.defaultFps} fps"))
        Media3VideoExporter(context).export(completedProject,outputFile,resolutionLongEdge=profile.defaultResolution.removeSuffix("p").toInt(),frameRate=profile.defaultFps.toInt()){percent->onProgress(ProductionProgress(ProductionStage.RENDER,percent,100,"MP4 render $percent%"))}
        onProgress(ProductionProgress(ProductionStage.COMPLETE,4,4,"Video exported"))
        return completedProject
    }
}