package com.vex.videoai

import android.os.Bundle
import android.app.DownloadManager
import android.webkit.MimeTypeMap
import android.graphics.BitmapFactory
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ClosedCaption
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Timeline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import com.vex.videoai.ai.LlmSettings
import com.vex.videoai.ai.LocalLlmClient
import com.vex.videoai.ai.OnDeviceLlmClient
import com.vex.videoai.data.ProjectStore
import com.vex.videoai.data.SceneLayer
import com.vex.videoai.data.SubtitleLine
import com.vex.videoai.data.VideoProject
import com.vex.videoai.data.VideoScene
import com.vex.videoai.media.EngineCatalog
import com.vex.videoai.media.AndroidEnglishSpeechEngine
import com.vex.videoai.media.WhisperLocalEngine
import com.vex.videoai.media.SrtExporter
import com.vex.videoai.media.Media3VideoExporter
import com.vex.videoai.models.ModelCatalog
import com.vex.videoai.models.ModelDownloadManager
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.delay
import java.io.File

private val Bg=Color(0xFF101014)
private val Panel=Color(0xFF1B1A22)
private val Purple=Color(0xFFA855F7)
private val Muted=Color(0xFFAAA6B5)
private data class Category(val id:String,val en:String,val tr:String,val subEn:String,val subTr:String,val icon:String)
private val categories=listOf(
 Category("horror","Horror","Korku","Suspense and paranormal","Gerilim ve paranormal","◉"),
 Category("educational","Educational","Eğitim","Visual explanations","Görsel anlatımlar","✦"),
 Category("story","Story","Hikâye","Characters and emotional arcs","Karakterler ve duygusal akış","◇"),
 Category("mystery","Mystery","Gizem","Clues and theories","İpuçları ve teoriler","⌕"),
 Category("history","History","Tarih","Events and biographies","Olaylar ve biyografiler","◷"),
 Category("documentary","Documentary","Belgesel","Real-world subjects","Gerçek konular","▤"),
 Category("comedy","Comedy","Komedi","Sketches and punchlines","Skeçler ve espriler","☺"),
 Category("facts","Facts / Top 10","Bilgiler / Top 10","Rankings and discoveries","Sıralamalar ve keşifler","≡"),
 Category("motivational","Motivational","Motivasyon","Inspiring stories","İlham veren hikâyeler","↗")
)

class MainActivity:ComponentActivity(){
 override fun onCreate(savedInstanceState:Bundle?){super.onCreate(savedInstanceState);setContent{VexVideoApp()}}
}

@Composable private fun VexVideoApp(){
 val context= LocalContext.current
 val store= remember { ProjectStore(context) }
 val modelManager=remember { ModelDownloadManager(context) }
 val prefs= remember { context.getSharedPreferences("vex_video_settings",0) }
 val projects= remember { mutableStateListOf<VideoProject>().apply { addAll(store.getAll()) } }
 var language by rememberSaveable { mutableStateOf("en") }
 var page by rememberSaveable { mutableStateOf("home") }
 var category by rememberSaveable { mutableStateOf("horror") }
 var idea by rememberSaveable { mutableStateOf("") }
 var duration by rememberSaveable { mutableStateOf("45s") }
 var ratio by rememberSaveable { mutableStateOf("9:16") }
 var style by rememberSaveable { mutableStateOf("Cinematic") }
 var activeProjectId by rememberSaveable { mutableStateOf("") }
 var sceneIndex by rememberSaveable { mutableIntStateOf(0) }
 var endpoint by rememberSaveable { mutableStateOf(prefs.getString("llm_endpoint","http://127.0.0.1:8080/v1/chat/completions")?:"") }
 var model by rememberSaveable { mutableStateOf(prefs.getString("llm_model","local-model")?:"local-model") }
 var apiKey by rememberSaveable { mutableStateOf("") }
 var busy by remember { mutableStateOf(false) }
 var renderProgress by remember { mutableIntStateOf(0) }
 var exportResolution by rememberSaveable { mutableStateOf("1080p") }
 var exportFps by rememberSaveable { mutableStateOf("30") }
 var pendingImageLayerId by rememberSaveable { mutableStateOf("") }
 var pendingImageSceneId by rememberSaveable { mutableStateOf("") }
 var notice by remember { mutableStateOf("") }
 val scope= rememberCoroutineScope()
 val tr=language=="tr"
 val active=projects.firstOrNull { it.id==activeProjectId }
 val selectedScene=active?.scenes?.getOrNull(sceneIndex)

 fun saveProject(p:VideoProject){store.save(p);val i=projects.indexOfFirst{it.id==p.id};if(i>=0)projects[i]=p else projects.add(0,p)}
 fun updateActive(transform:(VideoProject)->VideoProject){val p=active?:return;saveProject(transform(p))}
 fun createProject(){
  val seconds=when(duration){"30s"->30;"45s"->45;"60s"->60;"90s"->90;"3m"->180;else->45}
  val p=VideoProject(title=idea.trim().take(52).ifBlank{"Untitled video"},idea=idea.trim(),category=category,durationSeconds=seconds,aspectRatio=ratio,visualStyle=style,
   scenes=listOf(VideoScene(title="Opening scene",durationSeconds=6,visualDescription="Establish the location, mood and main subject.",layers=listOf(SceneLayer(name="Environment",kind="background"),SceneLayer(name="Main subject",kind="foreground")))))
  saveProject(p);activeProjectId=p.id;sceneIndex=0;page="editor"
 }
 fun updateScene(scene:VideoScene){updateActive{p->p.copy(scenes=p.scenes.mapIndexed{i,s->if(i==sceneIndex)scene else s})}}
 fun addScene(){updateActive{p->p.copy(scenes=p.scenes+VideoScene(title="Scene ${p.scenes.size+1}",visualDescription="Describe the action and composition."))};sceneIndex=(active?.scenes?.size?:0)}
 fun goEditor(p:VideoProject){activeProjectId=p.id;sceneIndex=0;page="editor"}
 val audioPicker=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()){uri->
  if(uri!=null)scope.launch{
   busy=true;notice=""
   try{
    val project=active?:error("Open a project first.")
    val scene=project.scenes.getOrNull(sceneIndex)?:error("Select a scene first.")
    val extension=MimeTypeMap.getSingleton().getExtensionFromMimeType(context.contentResolver.getType(uri))?:"wav"
    val audioDir=File(context.filesDir,"vex-video-projects/${project.id}").apply{mkdirs()}
    val audioFile=File(audioDir,"${scene.id}.${extension}")
    val input=context.contentResolver.openInputStream(uri)?:error("Could not open the selected audio file.")
    input.use{stream->audioFile.outputStream().use{out->stream.copyTo(out)}}
    val whisperAsset=ModelCatalog.assets.first{it.id=="whisper_tiny_en"}
    val cues=WhisperLocalEngine(context).transcribeEnglish(audioFile,modelManager.fileFor(whisperAsset))
    updateActive{p->p.copy(scenes=p.scenes.mapIndexed{i,current->if(i==sceneIndex)current.copy(subtitles=cues,voiceoverPath=audioFile.absolutePath)else current})}
    notice=if(tr)"Whisper ${cues.size} altyazı satırı oluşturdu; ses dosyası sahneye eklendi." else "Whisper created ${cues.size} subtitle cues and attached the voiceover."
   }catch(e:Exception){notice=e.message?:"Whisper transcription failed."}
   finally{busy=false}
  }
 }

 val srtPicker=rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/x-subrip")){uri->
  if(uri!=null){
   runCatching{
    val cues=active?.scenes?.getOrNull(sceneIndex)?.subtitles.orEmpty()
    context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use{it.write(SrtExporter.toSrt(cues))}?:error("Could not create the SRT file.")
   }.onSuccess{notice=if(tr)"SRT dosyası kaydedildi." else "SRT file saved."}.onFailure{notice=it.message?:"SRT export failed."}
  }
 }
  val imagePicker=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()){uri->
  if(uri!=null)scope.launch{
   try{
    val project=active?:error("Open a project first.")
    val layerId=pendingImageLayerId;val sceneId=pendingImageSceneId
    val extension=MimeTypeMap.getSingleton().getExtensionFromMimeType(context.contentResolver.getType(uri))?:"png"
    val assetDir=File(context.filesDir,"vex-video-projects/"+project.id+"/assets").apply{mkdirs()}
    val imageFile=File(assetDir,layerId+"."+extension)
    withContext(Dispatchers.IO){val input=context.contentResolver.openInputStream(uri)?:error("Could not open the selected image.");input.use{stream->imageFile.outputStream().use{out->stream.copyTo(out)}}}
    updateActive{p->p.copy(scenes=p.scenes.map{scene->if(scene.id==sceneId)scene.copy(layers=scene.layers.map{layer->if(layer.id==layerId)layer.copy(assetPath=imageFile.absolutePath)else layer})else scene})}
    notice=if(tr)"Görsel katmana eklendi." else "Image attached to the canvas layer."
   }catch(e:Exception){notice=e.message?: "Image import failed."}
  }
 }
 val videoPicker=rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("video/mp4")){uri->
  val projectToRender=active
  if(uri!=null&&projectToRender!=null)scope.launch{
   busy=true;renderProgress=0;notice=""
   val temp=File(context.cacheDir,"vex-render-${projectToRender.id}.mp4")
   try{
    Media3VideoExporter(context).export(projectToRender,temp,resolutionLongEdge=exportResolution.removeSuffix("p").toInt(),frameRate=exportFps.toInt()){renderProgress=it}
    val output=context.contentResolver.openOutputStream(uri)?:error("Could not write to the selected destination.")
    output.use{stream->temp.inputStream().use{input->input.copyTo(stream)}}
    val withAudio=projectToRender.scenes.all{it.voiceoverPath.isNotBlank()&&File(it.voiceoverPath).exists()};notice=if(withAudio)if(tr)"MP4 hazır: seslendirme ve altyazılar eklendi." else "MP4 exported with per-scene voiceovers and burned-in subtitles." else if(tr)"MP4 hazır ancak tüm sahnelere ses dosyası eklenmediği için sessiz dışa aktarıldı." else "MP4 exported silently. Attach a voiceover to every scene to include audio."
   }catch(e:Exception){notice=e.message?:"MP4 export failed."}
   finally{temp.delete();busy=false}
  }
 }
 MaterialTheme(colorScheme=darkColorScheme(primary=Purple,background=Bg,surface=Panel,onSurface=Color.White)){
  Scaffold(containerColor=Bg, bottomBar={
   if(page!="editor") NavigationBar(containerColor=Panel){
    NavigationBarItem(selected=page=="home",onClick={page="home"},icon={Icon(Icons.Rounded.AutoAwesome,null)},label={Text(if(tr)"Oluştur" else "Create")})
    NavigationBarItem(selected=page=="projects",onClick={page="projects"},icon={Icon(Icons.Rounded.Movie,null)},label={Text(if(tr)"Projeler" else "Projects")})
    NavigationBarItem(selected=page=="models",onClick={page="models"},icon={Icon(Icons.Rounded.Add,null)},label={Text(if(tr)"Modeller" else "Models")})
    NavigationBarItem(selected=page=="settings",onClick={page="settings"},icon={Icon(Icons.Rounded.Settings,null)},label={Text(if(tr)"Ayarlar" else "Settings")})
   }
  }){padding->
   Column(Modifier.fillMaxSize().padding(padding)){
    if(page!="editor") TopBar(tr,language){language=if(language=="tr")"en" else "tr"}
    when(page){
     "home"->LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(18.dp),verticalArrangement=Arrangement.spacedBy(15.dp)){
      item{Card(colors=CardDefaults.cardColors(containerColor=Color(0xFF21182E)),shape=RoundedCornerShape(16.dp)){Column(Modifier.padding(17.dp)){Text(if(tr)"VEX VIDEO AI" else "VEX VIDEO AI",color=Purple,fontWeight=FontWeight.Bold,letterSpacing=2.sp);Text(if(tr)"Fikrini videoya dönüştür" else "Turn an idea into a video",fontSize=23.sp,fontWeight=FontWeight.Bold);Text(if(tr)"Yerel yapay zekâ ile senaryo, storyboard, ses ve video." else "Script, storyboard, voice and video with local AI.",color=Muted,fontSize=12.sp)}}}
      item{Section(if(tr)"İÇERİK TÜRÜ" else "CONTENT TYPE",if(tr)"Bir format seç" else "Choose a format")}
      items(categories.chunked(2)){pair->Row(horizontalArrangement=Arrangement.spacedBy(9.dp)){pair.forEach{c->CategoryCard(c,category==c.id,tr,Modifier.weight(1f)){category=c.id}};if(pair.size==1)Spacer(Modifier.weight(1f))}}
      item{Section(if(tr)"VİDEO FİKRİ" else "VIDEO IDEA",if(tr)"Konu veya prompt" else "Topic or prompt");OutlinedTextField(idea,{idea=it},Modifier.fillMaxWidth(),minLines=3,placeholder={Text(if(tr)"Örn. Terk edilmiş bir kasabadan gelen sinyal..." else "e.g. A signal coming from an abandoned town...")},colors=fieldColors())}
      item{Section(if(tr)"AYARLAR" else "SETTINGS",if(tr)"Süre, oran ve stil" else "Duration, ratio and style");Text(if(tr)"Süre" else "Duration",color=Muted,fontSize=12.sp);ChoiceRow(listOf("30s","45s","60s","90s","3m"),duration){duration=it};Text(if(tr)"Oran" else "Aspect ratio",color=Muted,fontSize=12.sp);ChoiceRow(listOf("9:16","16:9","1:1"),ratio){ratio=it};Text(if(tr)"Görsel stil" else "Visual style",color=Muted,fontSize=12.sp);ChoiceRow(listOf("Cinematic","2D Art","Motion Graphics","Comic"),style){style=it}}
      item{Button(onClick={createProject()},modifier=Modifier.fillMaxWidth().height(52.dp),enabled=idea.isNotBlank(),colors=ButtonDefaults.buttonColors(containerColor=Purple)){Icon(Icons.Rounded.AutoAwesome,null);Text(if(tr)"  Proje oluştur ve düzenle" else "  Create project and edit")}}
      item{Text(if(tr)"Videolar daima İngilizce seslendirme ve İngilizce altyazı kullanır." else "Generated videos always use English voiceover and English subtitles.",color=Muted,fontSize=11.sp)}
     }
     "projects"->LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(18.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){
      item{Section(if(tr)"PROJE KÜTÜPHANESİ" else "PROJECT LIBRARY",if(tr)"Cihazda kayıtlı projelerin" else "Projects saved on this device")}
      if(projects.isEmpty())item{Text(if(tr)"Henüz proje yok." else "No projects yet.",color=Muted)}
      items(projects,key={it.id}){p->Card(Modifier.fillMaxWidth().clickable{goEditor(p)},colors=CardDefaults.cardColors(containerColor=Panel)){Row(Modifier.padding(14.dp),verticalAlignment=Alignment.CenterVertically){Icon(Icons.Rounded.Movie,null,tint=Purple);Column(Modifier.weight(1f).padding(start=10.dp)){Text(p.title,color=Color.White,fontWeight=FontWeight.SemiBold);Text("${p.category} • ${p.scenes.size} scenes • ${p.aspectRatio}",color=Muted,fontSize=11.sp)};IconButton(onClick={store.delete(p.id);projects.removeAll{it.id==p.id};File(context.filesDir,"vex-video-projects/"+p.id).deleteRecursively()}){Icon(Icons.Rounded.Delete,null,tint=Muted)}}}}
     }
     "models"->ModelCenterScreen(tr)
     "settings"->Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
      Section(if(tr)"YEREL LLM" else "LOCAL LLM",if(tr)"OpenAI uyumlu yerel sunucu" else "OpenAI-compatible local server")
      Text(if(tr)"Qwen3 GGUF modelini Model Center’dan indirirsen uygulama doğrudan cihazda çalıştırır. İstersen aşağıdaki OpenAI uyumlu yerel sunucu da kullanılabilir." else "Download a Qwen3 GGUF model in Model Center to run it directly on-device. You can also use the OpenAI-compatible local server below.",color=Muted,fontSize=12.sp)
      OutlinedTextField(endpoint,{endpoint=it},Modifier.fillMaxWidth(),label={Text("Chat completions URL")},colors=fieldColors())
      OutlinedTextField(model,{model=it},Modifier.fillMaxWidth(),label={Text(if(tr)"Model adı" else "Model name")},colors=fieldColors())
      OutlinedTextField(apiKey,{apiKey=it},Modifier.fillMaxWidth(),label={Text(if(tr)"API key (opsiyonel)" else "API key (optional)")},colors=fieldColors())
      Button(onClick={prefs.edit().putString("llm_endpoint",endpoint).putString("llm_model",model).apply();notice=if(tr)"Ayarlar kaydedildi." else "Settings saved."},modifier=Modifier.fillMaxWidth(),colors=ButtonDefaults.buttonColors(containerColor=Purple)){Text(if(tr)"Kaydet" else "Save settings")}
      if(notice.isNotBlank())Text(notice,color=Color(0xFF9BE7B9))
      Divider(color=Color(0xFF34313B))
      Section(if(tr)"ÜRETİM MOTORLARI" else "GENERATION ENGINES",if(tr)"Kurulum durumları" else "Runtime status")
      EngineCatalog.initial.forEach{e->Card(colors=CardDefaults.cardColors(containerColor=Panel)){Column(Modifier.padding(13.dp)){Text(e.displayName,color=Color.White,fontWeight=FontWeight.SemiBold);Text(e.purpose,color=Muted,fontSize=12.sp);Text(e.detail,color=Color(0xFFE6B66D),fontSize=11.sp);Text(when(e.state){com.vex.videoai.media.RuntimeState.READY->if(tr)"Hazır" else "Ready";com.vex.videoai.media.RuntimeState.RUNNING->if(tr)"Çalışıyor" else "Running";com.vex.videoai.media.RuntimeState.FAILED->if(tr)"Hata" else "Failed";else->if(tr)"Kurulum bekliyor" else "Setup required"},color=if(e.state==com.vex.videoai.media.RuntimeState.READY)Color(0xFF9BE7B9) else Muted,fontSize=10.sp)}}}
     }
     "editor"->{
      if(active==null){page="projects"}else{
       Row(Modifier.fillMaxWidth().padding(horizontal=14.dp,vertical=8.dp),verticalAlignment=Alignment.CenterVertically){Text("‹",Modifier.clickable{page="projects"}.padding(8.dp),color=Purple,fontSize=27.sp);Column(Modifier.weight(1f)){Text(active.title,color=Color.White,fontWeight=FontWeight.Bold);Text(if(tr)"STORYBOARD • CANVAS • TIMELINE" else "STORYBOARD • CANVAS • TIMELINE",color=Muted,fontSize=9.sp,letterSpacing=1.sp)};TextButton(onClick={saveProject(active);notice=if(tr)"Kaydedildi" else "Saved"}){Text(if(tr)"Kaydet" else "Save")}}
       if(selectedScene==null){Column(Modifier.padding(18.dp)){Text(if(tr)"Sahne bulunamadı" else "No scenes yet",color=Muted);Button(onClick={addScene()}){Text("Add scene")}}}
       else LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(horizontal=14.dp,vertical=5.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){
        item{Section(if(tr)"SAHNE ÖNİZLEMESİ" else "SCENE CANVAS",selectedScene.title);SceneCanvas(active.aspectRatio,selectedScene)}
        item{Section(if(tr)"SAHNE ZAMAN ÇİZELGESİ" else "SCENE TIMELINE",if(tr)"Sahne seç ve sırala" else "Select a scene");Row(Modifier.fillMaxWidth().height(68.dp).verticalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(7.dp),verticalAlignment=Alignment.CenterVertically){active.scenes.forEachIndexed{i,s->Surface(color=if(i==sceneIndex)Color(0xFF392447) else Panel,border=BorderStroke(1.dp,if(i==sceneIndex)Purple else Color(0xFF34313B)),shape=RoundedCornerShape(8.dp),modifier=Modifier.width(94.dp).fillMaxHeight().clickable{sceneIndex=i}){Column(Modifier.padding(8.dp),verticalArrangement=Arrangement.SpaceBetween){Text("SCENE ${i+1}",color=Purple,fontSize=9.sp,fontWeight=FontWeight.Bold);Text(s.title,color=Color.White,fontSize=11.sp,maxLines=1);Text("${s.durationSeconds}s",color=Muted,fontSize=10.sp)}}};Surface(color=Panel,shape=RoundedCornerShape(8.dp),modifier=Modifier.fillMaxHeight().width(48.dp).clickable{addScene()}){Box(contentAlignment=Alignment.Center){Icon(Icons.Rounded.Add,null,tint=Purple)}}}}
        item{Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){OutlinedButton(onClick={if(sceneIndex>0){updateActive{p->val list=p.scenes.toMutableList();val item=list.removeAt(sceneIndex);list.add(sceneIndex-1,item);p.copy(scenes=list)};sceneIndex--}},enabled=sceneIndex>0,modifier=Modifier.weight(1f)){Text(if(tr)"↑ Önceki" else "↑ Move earlier")};OutlinedButton(onClick={if(sceneIndex<active.scenes.lastIndex){updateActive{p->val list=p.scenes.toMutableList();val item=list.removeAt(sceneIndex);list.add(sceneIndex+1,item);p.copy(scenes=list)};sceneIndex++}},enabled=sceneIndex<active.scenes.lastIndex,modifier=Modifier.weight(1f)){Text(if(tr)"↓ Sonraki" else "↓ Move later")}}}
        item{Section(if(tr)"SAHNE DETAYLARI" else "SCENE DETAILS",if(tr)"İçeriği düzenle" else "Edit scene content")}
        item{OutlinedTextField(selectedScene.title,{updateScene(selectedScene.copy(title=it))},Modifier.fillMaxWidth(),label={Text(if(tr)"Sahne adı" else "Scene title")},singleLine=true,colors=fieldColors())}
        item{OutlinedButton(onClick={updateActive{p->p.copy(scenes=p.scenes.filterNot{it.id==selectedScene.id})};sceneIndex=(sceneIndex-1).coerceAtLeast(0)},enabled=active.scenes.size>1,modifier=Modifier.fillMaxWidth(),colors=ButtonDefaults.outlinedButtonColors(contentColor=Color(0xFFE58C8C))){Icon(Icons.Rounded.Delete,null);Text(if(tr)" Bu sahneyi sil" else " Delete this scene")}}
        item{OutlinedButton(onClick={updateActive{p->p.copy(scenes=p.scenes.filterNot{it.id==selectedScene.id})};sceneIndex=(sceneIndex-1).coerceAtLeast(0)},enabled=active.scenes.size>1,modifier=Modifier.fillMaxWidth(),colors=ButtonDefaults.outlinedButtonColors(contentColor=Color(0xFFE58C8C))){Icon(Icons.Rounded.Delete,null);Text(if(tr)" Bu sahneyi sil" else " Delete this scene")}}
        item{Text(if(tr)"Süre: ${selectedScene.durationSeconds} saniye" else "Duration: ${selectedScene.durationSeconds} seconds",color=Muted,fontSize=12.sp);Slider(selectedScene.durationSeconds.toFloat(),{updateScene(selectedScene.copy(durationSeconds=it.toInt().coerceIn(1,120)))},valueRange=1f..120f)}
        item{OutlinedTextField(selectedScene.narration,{updateScene(selectedScene.copy(narration=it))},Modifier.fillMaxWidth(),minLines=3,label={Text("English narration")},colors=fieldColors())}
        item{Button(onClick={scope.launch{
         val project=active;val scene=selectedScene
         if(project==null||scene==null){notice=if(tr)"Önce bir sahne seç." else "Select a scene first."}
         else{busy=true;notice="";try{
          val voiceText=scene.narration.ifBlank{scene.dialogue}
          require(voiceText.isNotBlank()){"Add English narration or dialogue first."}
          val audioDir=File(context.filesDir,"vex-video-projects/"+project.id).apply{mkdirs()}
          val audio=File(audioDir,scene.id+"-voice.wav")
          AndroidEnglishSpeechEngine(context).synthesize(voiceText,audio)
          val whisperAsset=ModelCatalog.assets.first{it.id=="whisper_tiny_en"}
          val cues=if(modelManager.isDownloaded(whisperAsset))WhisperLocalEngine(context).transcribeEnglish(audio,modelManager.fileFor(whisperAsset)) else scene.subtitles
          updateActive{p->p.copy(scenes=p.scenes.mapIndexed{i,current->if(i==sceneIndex)current.copy(voiceoverPath=audio.absolutePath,subtitles=cues)else current})}
          notice=if(cues.isNotEmpty())if(tr)"Ses üretildi ve "+cues.size+" altyazı satırı eklendi." else "Voiceover generated and "+cues.size+" subtitle cues added." else if(tr)"Ses üretildi. Otomatik altyazı için Model Center’dan Whisper tiny.en indir." else "Voiceover generated. Download Whisper tiny.en in Model Center to add automatic subtitles."
         }catch(e:Exception){notice=e.message?: "Voice generation failed."}finally{busy=false}}
        }},modifier=Modifier.fillMaxWidth(),enabled=!busy&&((selectedScene.narration.isNotBlank())||(selectedScene.dialogue.isNotBlank())),colors=ButtonDefaults.buttonColors(containerColor=Purple)){if(busy)CircularProgressIndicator(Modifier.size(18.dp),strokeWidth=2.dp)else Icon(Icons.Rounded.MusicNote,null);Text(if(tr)"  İngilizce ses üret (çevrimdışı TTS)" else "  Generate English voiceover (offline TTS)")}}
        item{Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Section(if(tr)"WHISPER ALTYAZILARI" else "WHISPER SUBTITLES",if(tr)"Bu sahnenin ses dosyasını seç" else "Choose this scene's voiceover audio")};Button(onClick={audioPicker.launch(arrayOf("audio/wav","audio/x-wav","audio/mpeg","audio/flac"))},enabled=!busy,colors=ButtonDefaults.buttonColors(containerColor=Purple)){if(busy)CircularProgressIndicator(Modifier.size(16.dp),strokeWidth=2.dp)else Icon(Icons.Rounded.ClosedCaption,null);Text(if(tr)" Seslendiriyi çöz" else " Transcribe audio")}}}
        item{if(selectedScene.subtitles.isEmpty())Text(if(tr)"Henüz altyazı yok. Whisper ile ses dosyasından oluşturabilir veya elle ekleyebilirsin." else "No subtitle cues yet. Transcribe an audio file with Whisper or add one manually.",color=Muted,fontSize=11.sp) else selectedScene.subtitles.forEach{cue->Column(Modifier.fillMaxWidth().padding(vertical=5.dp)){Text("${cue.startMs/1000.0}s – ${cue.endMs/1000.0}s",color=Purple,fontSize=10.sp);OutlinedTextField(cue.text,{text->updateScene(selectedScene.copy(subtitles=selectedScene.subtitles.map{if(it.id==cue.id)it.copy(text=text)else it}))},Modifier.fillMaxWidth(),minLines=1,colors=fieldColors());TextButton(onClick={updateScene(selectedScene.copy(subtitles=selectedScene.subtitles.filterNot{it.id==cue.id}))}){Text(if(tr)"Satırı sil" else "Remove cue",color=Muted)}}};Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){Button(onClick={updateScene(selectedScene.copy(subtitles=selectedScene.subtitles+SubtitleLine(startMs=0,endMs=selectedScene.durationSeconds*1000L,text="New English subtitle")))},colors=ButtonDefaults.buttonColors(containerColor=Panel)){Icon(Icons.Rounded.Add,null);Text(if(tr)" Elle altyazı ekle" else " Add cue")};if(selectedScene.subtitles.isNotEmpty())OutlinedButton(onClick={srtPicker.launch("scene-${sceneIndex+1}.srt")}){Text("Export SRT")}}}
        item{OutlinedTextField(selectedScene.dialogue,{updateScene(selectedScene.copy(dialogue=it))},Modifier.fillMaxWidth(),minLines=2,label={Text("English dialogue")},colors=fieldColors())}
        item{OutlinedTextField(selectedScene.visualDescription,{updateScene(selectedScene.copy(visualDescription=it))},Modifier.fillMaxWidth(),minLines=3,label={Text(if(tr)"Görsel açıklama" else "Visual direction")},colors=fieldColors())}
        item{Text(if(tr)"Kamera hareketi" else "Camera movement",color=Muted,fontSize=11.sp);ChoiceRow(listOf("Static","Pan","Zoom in","Zoom out","Dolly"),selectedScene.cameraMovement){v->updateScene(selectedScene.copy(cameraMovement=v))};Text(if(tr)"Sahne geçişi" else "Scene transition",color=Muted,fontSize=11.sp);ChoiceRow(listOf("Cut","Fade","Dissolve","Wipe"),selectedScene.transition){v->updateScene(selectedScene.copy(transition=v))}}
        item{Section(if(tr)"KATMANLAR" else "CANVAS LAYERS",if(tr)"Arka plan ve ön plan öğeleri" else "Background and foreground elements");selectedScene.layers.forEach{layer->Column(Modifier.fillMaxWidth().padding(vertical=5.dp)){Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){Text(if(layer.kind=="background")"BG" else "FG",color=Purple,fontSize=10.sp,fontWeight=FontWeight.Bold);Text(layer.name,Modifier.weight(1f).padding(start=9.dp),color=Color.White,fontSize=12.sp);OutlinedButton(onClick={pendingImageLayerId=layer.id;pendingImageSceneId=selectedScene.id;imagePicker.launch(arrayOf("image/png","image/jpeg","image/webp"))}){Text(if(layer.assetPath.isBlank())"Image" else "Replace",fontSize=9.sp)};TextButton(onClick={updateScene(selectedScene.copy(layers=selectedScene.layers.filterNot{it.id==layer.id}))}){Text(if(tr)"Sil" else "Remove",color=Muted)}};Text(if(tr)"Yatay konum" else "Horizontal position",color=Muted,fontSize=9.sp);Slider(value=layer.x.coerceIn(0f,1f),onValueChange={x->updateScene(selectedScene.copy(layers=selectedScene.layers.map{if(it.id==layer.id)it.copy(x=x)else it}))},valueRange=0f..1f);Text(if(tr)"Dikey konum" else "Vertical position",color=Muted,fontSize=9.sp);Slider(value=layer.y.coerceIn(0f,1f),onValueChange={y->updateScene(selectedScene.copy(layers=selectedScene.layers.map{if(it.id==layer.id)it.copy(y=y)else it}))},valueRange=0f..1f)}};Button(onClick={updateScene(selectedScene.copy(layers=selectedScene.layers+SceneLayer(name="New visual element",kind="foreground")))},colors=ButtonDefaults.buttonColors(containerColor=Panel)){Icon(Icons.Rounded.Add,null);Text(if(tr)" Katman ekle" else " Add layer")}}
        item{Button(onClick={scope.launch{busy=true;notice="";try{val starter=ModelCatalog.assets.first { it.id=="qwen3_06b" };val gguf=modelManager.fileFor(starter);val fresh=if(gguf.exists())OnDeviceLlmClient(context).createStoryboard(active,gguf,threads=4) else LocalLlmClient().createStoryboard(active,LlmSettings(endpoint,model,apiKey));saveProject(active.copy(scenes=fresh));sceneIndex=0;notice=if(tr)"AI storyboard hazır." else "AI storyboard generated."}catch(e:Exception){notice=e.message?: "Generation failed"}finally{busy=false}}},modifier=Modifier.fillMaxWidth(),enabled=!busy,colors=ButtonDefaults.buttonColors(containerColor=Purple)){if(busy)CircularProgressIndicator(Modifier.size(18.dp),strokeWidth=2.dp) else Icon(Icons.Rounded.AutoAwesome,null);Text(if(tr)"  Yerel LLM ile storyboard üret" else "  Generate storyboard with local LLM")}}
        item{Section(if(tr)"DIŞA AKTARMA AYARLARI" else "EXPORT SETTINGS",if(tr)"Çözünürlük ve kare hızı" else "Resolution and frame rate");Text(if(tr)"Uzun kenar çözünürlüğü" else "Long-edge resolution",color=Muted,fontSize=10.sp);ChoiceRow(listOf("480p","720p","1080p","1440p","2160p"),exportResolution){exportResolution=it};Text(if(tr)"Kare hızı" else "Frame rate",color=Muted,fontSize=10.sp);ChoiceRow(listOf("24","30","60"),exportFps){exportFps=it};if(exportResolution=="2160p")Text(if(tr)"4K dışa aktarma yüksek RAM ve depolama kullanır." else "4K export uses substantial memory and storage.",color=Color(0xFFE6B66D),fontSize=10.sp)}
item{Button(onClick={videoPicker.launch(active.title.replace(Regex("[^A-Za-z0-9_-]"),"_").take(40).ifBlank{"vex-video"}+".mp4")},modifier=Modifier.fillMaxWidth(),enabled=!busy&&active.scenes.isNotEmpty(),colors=ButtonDefaults.buttonColors(containerColor=Color(0xFF34303D))){Icon(Icons.Rounded.Movie,null);Text(if(tr)"  MP4 dışa aktar" else "  Export MP4")};if(busy&&renderProgress>0)Text("Render: ${renderProgress}%",color=Muted,fontSize=11.sp)}
        if(notice.isNotBlank())item{Text(notice,color=if(notice.contains("generated")||notice.contains("hazır"))Color(0xFF9BE7B9) else Color(0xFFE6B66D),fontSize=12.sp)}
        item{Card(colors=CardDefaults.cardColors(containerColor=Color(0xFF21182E))){Column(Modifier.padding(12.dp)){Text(if(tr)"Sonraki motorlar" else "Next production engines",color=Purple,fontWeight=FontWeight.Bold);Text("Kokoro TTS • Whisper subtitles • MP4 export",color=Color.White,fontSize=12.sp);Text(if(tr)"Henüz gerçek runtime bağlı değil; sahte tamamlandı durumu gösterilmez." else "Native runtimes are not connected yet; no fake completion status is shown.",color=Muted,fontSize=10.sp)}}}
       }
      }
     }
    }
   }
  }
 }
}

@Composable private fun TopBar(tr:Boolean,language:String,onLanguage:()->Unit){Row(Modifier.fillMaxWidth().padding(horizontal=18.dp,vertical=10.dp),verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text("VEX VIDEO AI",color=Purple,fontSize=11.sp,fontWeight=FontWeight.Bold,letterSpacing=2.sp);Text(if(tr)"AI içerik stüdyosu" else "AI Content Studio",color=Color.White,fontSize=22.sp,fontWeight=FontWeight.Bold)};TextButton(onClick=onLanguage){Text(language.uppercase(),color=Purple,fontWeight=FontWeight.Bold)}}}
@Composable private fun Section(title:String,subtitle:String){Column(Modifier.padding(bottom=2.dp)){Text(title,color=Purple,fontSize=10.sp,fontWeight=FontWeight.Bold,letterSpacing=1.3.sp);Text(subtitle,color=Color.White,fontSize=16.sp,fontWeight=FontWeight.SemiBold)}}
@Composable private fun CategoryCard(c:Category,selected:Boolean,tr:Boolean,modifier:Modifier,onClick:()->Unit){Surface(modifier.clickable{onClick()},color=if(selected)Color(0xFF30203F) else Panel,border=BorderStroke(1.dp,if(selected)Purple else Color(0xFF302D37)),shape=RoundedCornerShape(11.dp)){Column(Modifier.padding(11.dp),verticalArrangement=Arrangement.spacedBy(4.dp)){Row(verticalAlignment=Alignment.CenterVertically){Text(c.icon,color=Purple,fontSize=18.sp);Spacer(Modifier.weight(1f));if(selected)Icon(Icons.Rounded.Check,null,tint=Purple,modifier=Modifier.size(15.dp))};Text(if(tr)c.tr else c.en,color=Color.White,fontSize=13.sp,fontWeight=FontWeight.SemiBold);Text(if(tr)c.subTr else c.subEn,color=Muted,fontSize=10.sp,minLines=1)}}}
@Composable private fun ChoiceRow(options:List<String>,selected:String,onSelect:(String)->Unit){Row(horizontalArrangement=Arrangement.spacedBy(5.dp)){options.forEach{FilterChip(selected==it,{onSelect(it)},{Text(it,fontSize=10.sp)},modifier=Modifier.weight(1f),colors=FilterChipDefaults.filterChipColors(selectedContainerColor=Color(0xFF392447),selectedLabelColor=Color.White,labelColor=Muted))}}}
@Composable private fun fieldColors()=OutlinedTextFieldDefaults.colors(focusedBorderColor=Purple,unfocusedBorderColor=Color(0xFF393642),focusedTextColor=Color.White,unfocusedTextColor=Color.White)
@Composable private fun SceneCanvas(ratio:String,scene:VideoScene){
 val aspect=when(ratio){"16:9"->16f/9f;"1:1"->1f;else->9f/16f}
 BoxWithConstraints(Modifier.fillMaxWidth(),contentAlignment=Alignment.Center){
  val hLimit=430.dp;val w=maxWidth.coerceAtMost(340.dp).coerceAtMost(hLimit*aspect);val h=w/aspect
  val imageKeys=scene.layers.map{it.id to it.assetPath}
  val imageLayers= remember(imageKeys){scene.layers.mapNotNull{layer->if(layer.assetPath.isBlank())null else runCatching{BitmapFactory.decodeFile(layer.assetPath)?.asImageBitmap()?.let{layer.id to it}}.getOrNull()}.filterNotNull().toMap()}
  Canvas(Modifier.width(w).height(h)) {
   drawRect(Color(0xFF171522))
   drawRect(Color(0xFF29213B),topLeft=Offset(0f,size.height*.58f),size=androidx.compose.ui.geometry.Size(size.width,size.height*.42f))
   drawCircle(Color(0xFF51406B),radius=size.minDimension*.14f,center=Offset(size.width*.76f,size.height*.22f))
   scene.layers.filter{it.visible}.forEachIndexed{index,layer->
    val x=layer.x.coerceIn(.05f,.95f)*size.width
    val y=layer.y.coerceIn(.08f,.88f)*size.height
    val radius=(size.minDimension*.075f*layer.scale.coerceIn(.25f,2.5f)).coerceAtLeast(8f)
    val tint=when(layer.kind){"background"->Color(0xFF383047);"text"->Color.White;else->Color(0xFF09090D)}
    val image=imageLayers[layer.id]
    if(image!=null){val targetWidth=(size.width*.58f*layer.scale.coerceIn(.1f,2f)).toInt().coerceAtLeast(1);val targetHeight=(targetWidth*image.height/image.width.coerceAtLeast(1)).coerceAtLeast(1);drawImage(image,dstOffset=IntOffset(x.toInt()-targetWidth/2,y.toInt()-targetHeight/2),dstSize=IntSize(targetWidth,targetHeight))}
    else if(layer.kind=="background")drawRoundRect(tint,topLeft=Offset(x-radius*2,y-radius),size=androidx.compose.ui.geometry.Size(radius*4,radius*3),cornerRadius=androidx.compose.ui.geometry.CornerRadius(7.dp.toPx()))
    else drawCircle(tint,radius=radius,center=Offset(x,y))
   }
   drawLine(Purple,Offset(0f,size.height*.85f),Offset(size.width,size.height*.85f),2.dp.toPx())
  }
  Column(Modifier.width(w).height(h).padding(12.dp),verticalArrangement=Arrangement.SpaceBetween){
   Text("PREVIEW • ${scene.durationSeconds}s",color=Color.White,fontSize=9.sp,fontWeight=FontWeight.Bold)
   Column { Text(scene.title,color=Color.White,fontSize=15.sp,fontWeight=FontWeight.Bold);Text(scene.visualDescription.ifBlank{"Add a visual description to guide this scene."},color=Color(0xFFD5CADD),fontSize=10.sp,maxLines=3) }
  }
 }
}

@Composable private fun ModelCenterScreen(tr:Boolean){
 val context=LocalContext.current
 val manager= remember { ModelDownloadManager(context) }
 var refresh by remember { mutableIntStateOf(0) }
 LaunchedEffect(Unit){while(true){delay(1000);refresh++}}
 Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
  Section(if(tr)"MODEL CENTER" else "MODEL CENTER",if(tr)"Yerel ses ve altyazı modelleri" else "Local voice and subtitle models")
  Text(if(tr)"Modeller isteğe bağlı indirilir. İndirme tamamlanması, çıkarım motorunun da hazır olduğu anlamına gelmez." else "Models download on demand. A completed download does not mean the inference runtime is installed.",color=Muted,fontSize=12.sp)
  ModelCatalog.assets.forEach{asset->
   val progress=remember(refresh){manager.progress(asset)}
   val done=progress?.status==DownloadManager.STATUS_SUCCESSFUL
   val running=progress?.status==DownloadManager.STATUS_RUNNING||progress?.status==DownloadManager.STATUS_PENDING
   Card(colors=CardDefaults.cardColors(containerColor=Panel),shape=RoundedCornerShape(13.dp)){
    Column(Modifier.fillMaxWidth().padding(14.dp),verticalArrangement=Arrangement.spacedBy(7.dp)){
     Text(asset.group,color=Purple,fontSize=10.sp,fontWeight=FontWeight.Bold)
     Text(asset.name,color=Color.White,fontWeight=FontWeight.SemiBold)
     Text(asset.description,color=Muted,fontSize=11.sp)
     Text(asset.sizeLabel,color=Muted,fontSize=10.sp)
     if(running && progress!=null){LinearProgressIndicator(progress={if(progress.total>0)progress.percent/100f else 0f},modifier=Modifier.fillMaxWidth(),color=Purple);Text("${progress.percent}% • ${progress.downloaded/1024/1024} MB",color=Muted,fontSize=10.sp)}
     if(progress?.status==DownloadManager.STATUS_FAILED)Text(if(tr)"İndirme başarısız. Tekrar deneyebilirsin." else "Download failed. You can retry.",color=Color(0xFFE6B66D),fontSize=11.sp)
     Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.End){
      if(running)OutlinedButton(onClick={manager.cancel(asset);refresh++}){Text(if(tr)"İptal" else "Cancel")}
      else if(done)Text(when(asset.group){"On-device LLM"->if(tr)"İndirildi • Yerel storyboard için hazır" else "Downloaded • Ready for local storyboard";"Whisper"->if(tr)"İndirildi • Altyazı üretimi için hazır" else "Downloaded • Ready for transcription";else->if(tr)"İndirildi • Kokoro runtime bağlantısı bekleniyor" else "Downloaded • Kokoro runtime integration pending"},color=Color(0xFF9BE7B9),fontSize=10.sp,modifier=Modifier.padding(8.dp))
      else Button(onClick={manager.enqueue(asset);refresh++},colors=ButtonDefaults.buttonColors(containerColor=Purple)){Text(if(tr)"İndir" else "Download")}
     }
    }
   }
  }
  Card(colors=CardDefaults.cardColors(containerColor=Color(0xFF21182E))){Column(Modifier.padding(13.dp)){Text(if(tr)"Model güvenliği" else "Model handling",color=Purple,fontWeight=FontWeight.Bold);Text(if(tr)"İndirmeler Android Download Manager üzerinden yapılır ve uygulamanın özel dosya alanına kaydedilir. Modeller otomatik çalıştırılmaz." else "Downloads use Android Download Manager and are saved in the app-specific storage. Models are never executed automatically.",color=Muted,fontSize=11.sp)}}
 }
}
