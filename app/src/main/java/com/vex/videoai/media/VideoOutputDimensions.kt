package com.vex.videoai.media

data class OutputDimensions(val width:Int,val height:Int)

object VideoOutputDimensions {
    fun resolve(aspectRatio:String,longEdge:Int):OutputDimensions {
        val edge=longEdge.coerceIn(480,2160)
        return when(aspectRatio) {
            "16:9" -> OutputDimensions(edge,edge*9/16)
            "1:1" -> OutputDimensions(edge,edge)
            else -> OutputDimensions(edge*9/16,edge)
        }
    }
}