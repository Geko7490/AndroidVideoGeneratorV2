package com.vex.videoai.data
import android.content.Context
import org.json.JSONArray
import java.io.File
class ProjectStore(context:Context) {
 private val app=context.applicationContext
 private val prefs=app.getSharedPreferences("vex_video_projects",Context.MODE_PRIVATE)
 @Synchronized fun getAll():List<VideoProject> = runCatching { val a=JSONArray(prefs.getString(KEY,"[]")?:"[]");(0 until a.length()).mapNotNull { a.optJSONObject(it)?.let(VideoProject::fromJson) }.sortedByDescending { it.createdAt } }.getOrDefault(emptyList())
 @Synchronized fun save(project:VideoProject) { val all=getAll().filterNot { it.id==project.id }+project;val a=JSONArray();all.forEach { a.put(it.toJson()) };prefs.edit().putString(KEY,a.toString()).apply() }
 @Synchronized fun delete(id:String) {
  val a=JSONArray();getAll().filterNot { it.id==id }.forEach { a.put(it.toJson()) }
  prefs.edit().putString(KEY,a.toString()).apply()
  File(app.filesDir,"vex-video-projects/$id").deleteRecursively()
 }
 companion object { private const val KEY="projects_json" }
}