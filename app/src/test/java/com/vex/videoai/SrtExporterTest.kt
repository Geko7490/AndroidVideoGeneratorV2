package com.vex.videoai

import com.vex.videoai.data.SubtitleLine
import com.vex.videoai.data.VideoScene
import com.vex.videoai.media.SrtExporter
import org.junit.Assert.assertTrue
import org.junit.Test

class SrtExporterTest {
    @Test fun offsetsSubtitleCuesAcrossSceneBoundaries(){
        val scenes=listOf(
            VideoScene(title="Scene 1",durationSeconds=5,subtitles=listOf(SubtitleLine(startMs=100,endMs=500,text="First."))),
            VideoScene(title="Scene 2",durationSeconds=5,subtitles=listOf(SubtitleLine(startMs=200,endMs=800,text="Second.")))
        )
        val srt=SrtExporter.toSrtForScenes(scenes)
        assertTrue(srt.contains("00:00:05,200 --> 00:00:05,800"))
    }
    @Test fun exportsValidSrtCueWithMilliseconds() {
        val srt=SrtExporter.toSrt(listOf(SubtitleLine(startMs=1250,endMs=3520,text="A light appeared.")))
        assertTrue(srt.contains("00:00:01,250 --> 00:00:03,520"))
        assertTrue(srt.contains("A light appeared."))
    }
}
