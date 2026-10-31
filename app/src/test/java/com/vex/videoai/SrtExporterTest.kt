package com.vex.videoai

import com.vex.videoai.data.SubtitleLine
import com.vex.videoai.media.SrtExporter
import org.junit.Assert.assertTrue
import org.junit.Test

class SrtExporterTest {
    @Test fun exportsValidSrtCueWithMilliseconds() {
        val srt=SrtExporter.toSrt(listOf(SubtitleLine(startMs=1250,endMs=3520,text="A light appeared.")))
        assertTrue(srt.contains("00:00:01,250 --> 00:00:03,520"))
        assertTrue(srt.contains("A light appeared."))
    }
}
