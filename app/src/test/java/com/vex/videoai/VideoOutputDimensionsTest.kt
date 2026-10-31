package com.vex.videoai

import com.vex.videoai.media.VideoOutputDimensions
import org.junit.Assert.assertEquals
import org.junit.Test

class VideoOutputDimensionsTest {
    @Test fun portrait1080pUsesExpectedDimensions() {
        assertEquals(607,VideoOutputDimensions.resolve("9:16",1080).width)
        assertEquals(1080,VideoOutputDimensions.resolve("9:16",1080).height)
    }
    @Test fun landscape720pUsesExpectedDimensions() {
        assertEquals(720,VideoOutputDimensions.resolve("16:9",720).width)
        assertEquals(405,VideoOutputDimensions.resolve("16:9",720).height)
    }
    @Test fun squareStaysSquare() {
        val result=VideoOutputDimensions.resolve("1:1",1440)
        assertEquals(result.width,result.height)
    }
    @Test fun resolutionIsClampedToSupportedRange() {
        assertEquals(2160,VideoOutputDimensions.resolve("1:1",8000).width)
        assertEquals(480,VideoOutputDimensions.resolve("1:1",100).height)
    }
}