package com.vex.videoai.ai

import android.content.Context
import com.vex.videoai.data.SceneLayer
import com.vex.videoai.data.VideoProject
import com.vex.videoai.data.VideoScene
import com.vex.videoai.planning.ContentType
import com.vex.videoai.planning.ScenePlanningContract
import com.vex.videoai.planning.ScenePlanningRequest
import com.vex.videoai.planning.SceneDurationNormalizer
import dev.ffmpegkit.llama.Llama
import dev.ffmpegkit.llama.LlamaConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class OnDeviceLlmClient(private val context:Context) {
    suspend fun createStoryboard(project:VideoProject,modelFile:File,threads:Int=4,contextSize:Int=4096,maxTokens:Int=1800):List<VideoScene> = withContext(Dispatchers.IO) {
        require(modelFile.exists()&&modelFile.length()>0L) { "Download a GGUF model in Model Center first." }
        val category=ContentType.entries.firstOrNull{it.id==project.category}?:ContentType.STORY
        val request=ScenePlanningRequest(project.idea,category,project.durationSeconds,project.aspectRatio,project.visualStyle)
        val model=Llama.loadModel(modelPath=modelFile.absolutePath,config=LlamaConfig(contextSize=contextSize.coerceIn(1024,8192),threads=threads.coerceIn(2,8)))
        try {
            val result=Llama.complete(model,prompt=ScenePlanningContract.userPrompt(request),systemPrompt=ScenePlanningContract.systemPrompt(),maxTokens=maxTokens.coerceIn(512,4096))
            parseEnglishScenes(result.text,project.durationSeconds)
        } finally { Llama.releaseModel(model) }
    }

    private fun parseEnglishScenes(text:String,targetDurationSeconds:Int):List<VideoScene> {
        val start=text.indexOf('{');val end=text.lastIndexOf('}')
        require(start>=0&&end>start) { "The on-device model did not return a JSON scene plan. Try the larger model." }
        val plan=JSONObject(text.substring(start,end+1))
        val scenes=plan.optJSONArray("scenes")?:error("The model response has no scenes array.")
        val parsed=(0 until scenes.length()).mapNotNull{index->scenes.optJSONObject(index)?.let{s->
            val dialogueArray=s.optJSONArray("dialogue")?:JSONArray()
            val dialogue=(0 until dialogueArray.length()).map{index->val entry=dialogueArray.opt(index);if(entry is JSONObject)entry.optString("speaker","Character")+": "+entry.optString("text") else entry.toString()}.joinToString("\n")
            val backgrounds=s.optJSONArray("backgroundLayers")?:JSONArray()
            val foregrounds=s.optJSONArray("foregroundLayers")?:JSONArray()
            val layers=buildList{
                for(i in 0 until backgrounds.length())add(SceneLayer(name=backgrounds.optString(i),kind="background",x=.5f,y=.35f,scale=(1f+i*.12f).coerceAtMost(1.8f)))
                for(i in 0 until foregrounds.length())add(SceneLayer(name=foregrounds.optString(i),kind="foreground",x=.25f+(i%3)*.25f,y=.58f+(i/3)*.12f,scale=1f))
            }
            VideoScene(title=s.optString("title","Scene ${index+1}"),durationSeconds=s.optInt("durationSeconds",6).coerceIn(1,120),narration=s.optString("narration"),dialogue=dialogue,visualDescription=s.optString("visualDescription"),cameraMovement=s.optString("cameraMovement","Static"),transition=s.optString("transition","Cut"),layers=layers)
        }}
        require(parsed.isNotEmpty()) { "The on-device model returned no usable scenes." }
        require(parsed.all{scene->scene.narration.all{it.code<128}&&scene.dialogue.all{it.code<128}&&scene.visualDescription.all{it.code<128}&&scene.title.all{it.code<128}}) { "The model generated non-English video content. Retry; output must be English." }
        return SceneDurationNormalizer.normalize(parsed,targetDurationSeconds)
    }
}