package com.vex.videoai.production
import android.content.Context
import com.vex.videoai.ai.LlmSettings
import com.vex.videoai.ai.LocalLlmClient
import com.vex.videoai.ai.OnDeviceLlmClient
import com.vex.videoai.data.SubtitleLine
import com.vex.videoai.data.VideoProject
import com.vex.videoai.data.VideoScene
import com.vex.videoai.media.AndroidEnglishSpeechEngine
import com.vex.videoai.media.KokoroEnglishSpeechEngine
import com.vex.videoai.media.Media3VideoExporter
import com.vex.videoai.media.SilentWavWriter
import com.vex.videoai.media.WhisperLocalEngine
import java.io.File

enum class ProductionStage(val label:String) { PREPARING("Preparing"),STORYBOARD("LLM • Script and storyboard"),VOICEOVER("Speech • Scene voiceovers"),SUBTITLES("Whisper • English subtitles"),RENDER("Render • MP4"),COMPLETE("Complete") }
data class ProductionProgress(val stage:ProductionStage,val completed:Int,val total:Int,val detail:String="")

class SequentialVideoProduction(private val context:Context) {
    suspend fun run(source:VideoProject,settings:LlmSettings,localModel:File?,whisperModel:File,kokoroModel:File?,profile:ProductionProfile,outputFile:File,onProgress:(ProductionProgress)->Unit={}):VideoProject {
        require(whisperModel.exists()&&whisperModel.length()>0L) { "Download Whisper tiny.en in Model Center before running the full pipeline." }
        onProgress(ProductionProgress(ProductionStage.PREPARING,0,4,"Checking models and output storage"))
        val cpuCount=Runtime.getRuntime().availableProcessors().coerceAtLeast(2)
        onProgress(ProductionProgress(ProductionStage.STORYBOARD,0,4,"Generating the complete English script"))
        val scenes=if(localModel?.exists()==true&&localModel.length()>0L) {
            OnDeviceLlmClient(context).createStoryboard(source,localModel,threads=profile.threadsFor(cpuCount),contextSize=profile.contextSize,maxTokens=profile.maxTokens)
        } else LocalLlmClient().createStoryboard(source,settings.copy(maxTokens=profile.maxTokens))
        onProgress(ProductionProgress(ProductionStage.STORYBOARD,1,4,"${scenes.size} scenes planned; no separate intro scene"))
        var kokoro:KokoroEnglishSpeechEngine?=null
        var kokoroFallbackReason=""
        if(kokoroModel?.isFile==true&&kokoroModel.length()>1024L) {
            val candidate=KokoroEnglishSpeechEngine(context,kokoroModel)
            try { candidate.initialize();kokoro=candidate }
            catch(error:Exception) { candidate.release();kokoroFallbackReason=error.message?: "Kokoro initialization failed" }
        }
        val androidSpeech=AndroidEnglishSpeechEngine(context)
        val speechName=if(kokoro!=null)"Kokoro neural TTS" else "Android system TTS fallback"
        val audioDir=File(context.filesDir,"vex-video-projects/"+source.id).apply{mkdirs()}
        try {
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
                    val spokenText=spokenLines.joinToString("\n")
                    val estimatedRate=(spokenText.split(Regex("\\s+")).size/(scene.durationSeconds*2.5f)).coerceIn(.85f,1.25f)
                    if(kokoro!=null)kokoro!!.synthesize(spokenText,audio,estimatedRate) else androidSpeech.synthesize(spokenText,audio,rate=estimatedRate)
                    spokenSceneIndexes.add(index)
                }
                voiced.add(scene.copy(voiceoverPath=audio.absolutePath))
                val detail=if(spokenLines.isEmpty())"Silent scene ${index+1}/${scenes.size}" else "${speechName} • ${index+1}/${scenes.size}"
                onProgress(ProductionProgress(ProductionStage.VOICEOVER,index+1,scenes.size,detail))
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
        } finally { kokoro?.release() }
    }
}