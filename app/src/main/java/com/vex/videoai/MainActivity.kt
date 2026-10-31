package com.vex.videoai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ClosedCaption
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Bg = Color(0xFF101014)
private val Panel = Color(0xFF1B1A22)
private val Purple = Color(0xFFA855F7)
private val Muted = Color(0xFFAAA6B5)

private data class Category(val id: String, val en: String, val tr: String, val subEn: String, val subTr: String, val icon: String)
private val categories = listOf(
    Category("horror","Horror","Korku","Dark stories, suspense and paranormal","Karanlık hikâyeler, gerilim ve paranormal","◉"),
    Category("educational","Educational","Eğitim","Explain topics with visual storytelling","Konuları görsel anlatımla açıkla","✦"),
    Category("story","Story","Hikâye","Characters, dialogue and emotional arcs","Karakterler, diyaloglar ve duygusal akış","◇"),
    Category("mystery","Mystery","Gizem","Unsolved events, clues and theories","Çözülememiş olaylar, ipuçları ve teoriler","⌕"),
    Category("history","History","Tarih","Historical events and biographies","Tarihî olaylar ve biyografiler","◷"),
    Category("documentary","Documentary","Belgesel","Narrative non-fiction and real subjects","Gerçek konular ve anlatımlı içerikler","▤"),
    Category("comedy","Comedy","Komedi","Funny situations and short sketches","Komik durumlar ve kısa skeçler","☺"),
    Category("facts","Facts / Top 10","Bilgiler / Top 10","Rankings and surprising discoveries","Sıralamalar ve şaşırtıcı keşifler","≡"),
    Category("motivational","Motivational","Motivasyon","Inspiring stories and ideas","İlham veren hikâyeler ve fikirler","↗")
)
private data class Draft(val title: String, val category: String, val duration: String, val ratio: String)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { VideoAiHome() }
    }
}

@Composable
private fun VideoAiHome() {
    var lang by rememberSaveable { mutableStateOf("en") }
    var categoryId by rememberSaveable { mutableStateOf("horror") }
    var prompt by rememberSaveable { mutableStateOf("") }
    var duration by rememberSaveable { mutableStateOf("45s") }
    var ratio by rememberSaveable { mutableStateOf("9:16") }
    var style by rememberSaveable { mutableStateOf("Cinematic") }
    var created by rememberSaveable { mutableStateOf(false) }
    val drafts = remember { mutableStateListOf<Draft>() }
    val tr = lang == "tr"
    val category = categories.first { it.id == categoryId }

    MaterialTheme(colorScheme = darkColorScheme(primary = Purple, background = Bg, surface = Panel, onSurface = Color.White)) {
        Scaffold(containerColor = Bg) { inset ->
            LazyColumn(
                Modifier.fillMaxSize().padding(inset),
                contentPadding = PaddingValues(20.dp),
                verticalArrangement = Arrangement.spacedBy(17.dp)
            ) {
                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text("VEX VIDEO AI", color = Purple, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                            Text(if (tr) "AI içerik stüdyosu" else "AI Content Studio", color = Color.White, fontSize = 25.sp, fontWeight = FontWeight.Bold)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            LangButton("TR", tr) { lang = "tr" }
                            LangButton("EN", !tr) { lang = "en" }
                        }
                    }
                }
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF21182E)), shape = RoundedCornerShape(17.dp)) {
                        Column(Modifier.padding(17.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.AutoAwesome, null, tint = Purple)
                                Text(if (tr) "  Fikrini videoya dönüştür" else "  Turn an idea into a video", color = Color.White, fontWeight = FontWeight.SemiBold)
                            }
                            Text(if (tr) "Senaryo, sahne planı, seslendirme ve altyazı için yerel öncelikli iş akışı." else "A local-first workflow for scripts, scene plans, voiceover and subtitles.", color = Muted, fontSize = 13.sp)
                        }
                    }
                }
                item { Heading(if (tr) "İÇERİK TÜRÜ" else "CONTENT TYPE", if (tr) "Videonun formatını seç" else "Choose your video format") }
                items(categories.chunked(2)) { pair ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        pair.forEach { item -> CategoryTile(item, categoryId == item.id, tr, Modifier.weight(1f)) { categoryId = item.id } }
                        if (pair.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
                item {
                    Heading(if (tr) "VİDEO FİKRİ" else "VIDEO IDEA", if (tr) "Konunu veya promptunu yaz" else "Describe your topic or prompt")
                    OutlinedTextField(
                        value = prompt, onValueChange = { prompt = it }, modifier = Modifier.fillMaxWidth(),
                        minLines = 3, shape = RoundedCornerShape(13.dp),
                        placeholder = { Text(if (tr) "Örn. Mars'ta terk edilmiş bir robot..." else "e.g. A robot abandoned on Mars discovers...") },
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Purple, unfocusedBorderColor = Color(0xFF393642), focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )
                }
                item {
                    Heading(if (tr) "VİDEO AYARLARI" else "VIDEO SETTINGS", if (tr) "Üretim tercihleri" else "Generation preferences")
                    Text(if (tr) "Süre" else "Duration", color = Muted, fontSize = 12.sp)
                    ChoiceRow(listOf("30s","45s","60s","90s","3m"), duration) { duration = it }
                    Spacer(Modifier.height(8.dp))
                    Text(if (tr) "En-boy oranı" else "Aspect ratio", color = Muted, fontSize = 12.sp)
                    ChoiceRow(listOf("9:16","16:9","1:1"), ratio) { ratio = it }
                    Spacer(Modifier.height(8.dp))
                    Text(if (tr) "Görsel stil" else "Visual style", color = Muted, fontSize = 12.sp)
                    ChoiceRow(listOf("Cinematic","2D Art","Motion Graphics","Comic"), style) { style = it }
                }
                item {
                    Heading(if (tr) "SES VE ALTYAZI" else "VOICE & SUBTITLES", if (tr) "Video dili sabit: English" else "Video language is fixed: English")
                    Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(13.dp)) {
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            EngineRow(Icons.Rounded.MusicNote, "Kokoro TTS", if (tr) "İngilizce seslendirme" else "English voiceover")
                            HorizontalDivider(color = Color(0xFF34313B))
                            EngineRow(Icons.Rounded.ClosedCaption, "Whisper", if (tr) "İngilizce altyazı ve zamanlama" else "English subtitles and timestamps")
                        }
                    }
                    Text(if (tr) "Arayüz dili değişebilir; video anlatımı, diyaloglar ve altyazılar daima İngilizcedir." else "The interface language can change; video narration, dialogue and subtitles always remain in English.", color = Muted, fontSize = 11.sp, modifier = Modifier.padding(top = 7.dp))
                }
                item {
                    Button(
                        onClick = {
                            drafts.add(Draft(prompt.trim(), category.en, duration, ratio))
                            created = true
                        },
                        enabled = prompt.isNotBlank(),
                        modifier = Modifier.fillMaxWidth().height(53.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Purple)
                    ) {
                        Icon(Icons.Rounded.AutoAwesome, null)
                        Text(if (tr) "  Proje taslağı oluştur" else "  Create project brief", fontWeight = FontWeight.Bold)
                    }
                    Text(if (tr) "Bu ilk temel sürüm proje ayarlarını kaydeder. Yerel LLM, Kokoro, Whisper ve render motorları sonraki aşamalarda bağlanacaktır." else "This foundation build saves project settings. Local LLM, Kokoro, Whisper and rendering engines will be integrated in later milestones.", color = Muted, fontSize = 11.sp)
                }
                if (created && drafts.isNotEmpty()) {
                    item {
                        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF1B2923)), shape = RoundedCornerShape(12.dp)) {
                            Row(Modifier.padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.Check, null, tint = Color(0xFF78D6A2))
                                Column(Modifier.padding(start = 9.dp)) {
                                    Text(if (tr) "Proje taslağı oluşturuldu" else "Project brief created", color = Color(0xFF9BE7B9), fontWeight = FontWeight.Bold)
                                    Text(drafts.last().title, color = Color.White, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
                item {
                    Heading(if (tr) "SON PROJELER" else "RECENT PROJECTS", if (tr) "Bu oturumdaki taslaklar" else "Briefs from this session")
                    if (drafts.isEmpty()) Text(if (tr) "Henüz proje yok." else "No projects yet.", color = Muted, fontSize = 13.sp)
                    else drafts.reversed().forEach { draft ->
                        Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(11.dp), modifier = Modifier.padding(bottom = 7.dp)) {
                            Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.Movie, null, tint = Purple)
                                Column(Modifier.padding(start = 10.dp)) {
                                    Text(draft.title, color = Color.White, maxLines = 1, fontWeight = FontWeight.Medium)
                                    Text("${draft.category} • ${draft.duration} • ${draft.ratio}", color = Muted, fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable private fun LangButton(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(color = if (selected) Purple else Panel, shape = RoundedCornerShape(8.dp), modifier = Modifier.clickable { onClick() }) {
        Text(label, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 11.dp, vertical = 8.dp))
    }
}
@Composable private fun Heading(title: String, subtitle: String) {
    Column(Modifier.padding(bottom = 3.dp)) {
        Text(title, color = Purple, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
        Text(subtitle, color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
    }
}
@Composable private fun CategoryTile(item: Category, selected: Boolean, tr: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Surface(
        color = if (selected) Color(0xFF30203F) else Panel,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(if (selected) 1.5.dp else 1.dp, if (selected) Purple else Color(0xFF302D37)),
        modifier = modifier.clickable { onClick() }
    ) {
        Column(Modifier.padding(11.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(item.icon, color = Purple, fontSize = 18.sp)
                Spacer(Modifier.weight(1f))
                if (selected) Icon(Icons.Rounded.Check, null, tint = Purple, modifier = Modifier.size(16.dp))
            }
            Text(if (tr) item.tr else item.en, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            Text(if (tr) item.subTr else item.subEn, color = Muted, fontSize = 10.sp, lineHeight = 13.sp, minLines = 2)
        }
    }
}
@Composable private fun ChoiceRow(options: List<String>, selected: String, onSelect: (String) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(5.dp), modifier = Modifier.fillMaxWidth()) {
        options.forEach { option ->
            FilterChip(
                selected = selected == option, onClick = { onSelect(option) },
                label = { Text(option, fontSize = 10.sp) }, modifier = Modifier.weight(1f),
                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Color(0xFF392447), selectedLabelColor = Color.White, labelColor = Muted)
            )
        }
    }
}
@Composable private fun EngineRow(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = Purple, modifier = Modifier.size(22.dp))
        Column(Modifier.weight(1f).padding(start = 10.dp)) {
            Text(title, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            Text(subtitle, color = Muted, fontSize = 11.sp)
        }
        Surface(color = Color(0xFF33243F), shape = RoundedCornerShape(6.dp)) {
            Text("EN", color = Purple, fontWeight = FontWeight.Bold, fontSize = 10.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp))
        }
    }
}
