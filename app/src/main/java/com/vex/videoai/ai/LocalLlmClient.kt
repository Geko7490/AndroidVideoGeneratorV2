package com.vex.videoai.ai
import com.vex.videoai.data.SceneLayer
import com.vex.videoai.data.VideoProject
import com.vex.videoai.data.VideoScene
import com.vex.videoai.planning.ContentType
import com.vex.videoai.planning.ScenePlanningContract
import com.vex.videoai.planning.ScenePlanningRequest
import com.vex.videoai.planning.SceneDurationNormalizer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
data class LlmSettings(val endpoint:String,val model:String,val apiKey:String="",val maxTokens:Int=1800)
class LocalLlmClient {
 suspend fun createStoryboard(project:VideoProject,settings:LlmSettings):List<VideoScene> = withContext(Dispatchers.IO) {
  require(settings.endpoint.startsWith("http://")||settings.endpoint.startsWith("https://")) { "Endpoint must start with http:// or https://" }
  require(settings.model.isNotBlank()) { "Enter the local model name." }
  val category=ContentType.entries.firstOrNull { it.id==project.category }?:ContentType.STORY
  val req=ScenePlanningRequest(project.idea,category,project.durationSeconds,project.aspectRatio,project.visualStyle)
  val body=JSONObject().apply { put("model",settings.model);put("temperature",.7);put("max_tokens",settings.maxTokens.coerceIn(512,4096));put("messages",JSONArray().apply { put(JSONObject().put("role","system").put("content",ScenePlanningContract.systemPrompt()));put(JSONObject().put("role","user").put("content",ScenePlanningContract.userPrompt(req))) });put("response_format",JSONObject().put("type","json_object")) }
  val c=URL(settings.endpoint).openConnection() as HttpURLConnection
  try {
   c.requestMethod="POST";c.connectTimeout=15000;c.readTimeout=180000;c.doOutput=true;c.setRequestProperty("Content-Type","application/json")
   if(settings.apiKey.isNotBlank()) c.setRequestProperty("Authorization","Bearer "+settings.apiKey)
   c.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
   val status=c.responseCode;val stream=if(status in 200..299)c.inputStream else c.errorStream
   val response=stream?.bufferedReader()?.use { it.readText() }.orEmpty()
   if(status !in 200..299) error("Local LLM returned HTTP $status: "+response.take(500))
   val envelope=JSONObject(response)
   val message=envelope.optJSONArray("choices")?.optJSONObject(0)?.optJSONObject("message")?.optString("content")?:envelope.optString("response")
   require(message.isNotBlank()) { "The model returned an empty response." }
   val start=message.indexOf('{');val end=message.lastIndexOf('}')
   require(start>=0&&end>start) { "The model did not return a JSON scene plan." }
   val plan=JSONObject(message.substring(start,end+1));val a=plan.optJSONArray("scenes")?:error("The model response has no scenes array.")
   val result=(0 until a.length()).mapNotNull { i->a.optJSONObject(i)?.let { s->
    val d=s.optJSONArray("dialogue")?:JSONArray();val dialogue=(0 until d.length()).map { index -> val entry=d.opt(index);if(entry is JSONObject) entry.optString("speaker","Character")+": "+entry.optString("text") else entry.toString() }.joinToString("\n")
    val bg=s.optJSONArray("backgroundLayers")?:JSONArray();val fg=s.optJSONArray("foregroundLayers")?:JSONArray()
    val layers=buildList { for(k in 0 until bg.length()) add(SceneLayer(name=bg.optString(k),kind="background",x=.5f,y=.38f,scale=(1f+k*.12f).coerceAtMost(1.8f)));for(k in 0 until fg.length()) add(SceneLayer(name=fg.optString(k),kind="foreground",x=.25f+(k%3)*.25f,y=.55f+(k/3)*.12f,scale=1f)) }
    val soundArray=s.optJSONArray("soundEffects")?:JSONArray();val soundEffects=(0 until soundArray.length()).map{soundArray.optString(it)}.filter{it.isNotBlank()}
    VideoScene(title=s.optString("title","Scene "+(i+1)),durationSeconds=s.optInt("durationSeconds",6).coerceIn(1,120),narration=s.optString("narration"),dialogue=dialogue,visualDescription=s.optString("visualDescription"),cameraMovement=s.optString("cameraMovement","Static"),transition=s.optString("transition","Cut"),layers=layers,soundEffects=soundEffects)
   } }
   require(result.isNotEmpty()) { "The model returned no usable scenes." }
   require(result.all { scene->listOf(scene.narration,scene.dialogue,scene.visualDescription,scene.title,scene.cameraMovement,scene.transition).all { value->value.all { ch->ch.code<128 } }&&scene.layers.all { layer->layer.name.all { ch->ch.code<128 } } }) { "The model generated non-English narration or dialogue. Retry; video language must be English." }
   SceneDurationNormalizer.normalize(result,project.durationSeconds)
  } finally { c.disconnect() }
 }
}
