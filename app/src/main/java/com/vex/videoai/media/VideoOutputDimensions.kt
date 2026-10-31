package com.vex.videoai.media

data class OutputDimensions(val width:Int,val height:Int)

object VideoOutputDimensions {
    /** UI resolution labels are vertical pixel counts (e.g. 1080p), converted to the long edge for the chosen aspect ratio. */
    fun longEdgeForLabel(label:String):Int = when(label.lowercase()) {
        "480p" -> 854
        "720p" -> 1280
        "1080p" -> 1920
        "1440p" -> 2560
        "2160p","4k" -> 3840
        else -> label.removeSuffix("p").toIntOrNull()?.let { (it*16/9).coerceIn(480,3840) } ?: 1920
    }
    fun resolve(aspectRatio:String,longEdge:Int):OutputDimensions {
        val edge=longEdge.coerceIn(480,3840)
        return when(aspectRatio) {
            "16:9" -> OutputDimensions(edge,edge*9/16)
            "1:1" -> OutputDimensions(edge,edge)
            else -> OutputDimensions(edge*9/16,edge)
        }
    }
}