package com.vex.videoai.production

enum class ProductionProfile(
    val id:String,
    val label:String,
    val llmThreads:Int,
    val contextSize:Int,
    val maxTokens:Int,
    val defaultResolution:String,
    val defaultFps:String,
    val description:String
) {
    LOW("low","Low",2,2048,1100,"720p","24","Lower CPU and memory use; slower generation and rendering."),
    MID("mid","Mid",4,4096,1800,"1080p","30","Balanced speed, quality and device temperature."),
    HIGH("high","High",8,6144,2800,"1080p","60","Uses more CPU threads and memory for faster, higher-detail work.");

    fun threadsFor(availableProcessors:Int):Int=llmThreads.coerceAtMost(availableProcessors.coerceAtLeast(2))
    companion object {
        fun fromId(id:String):ProductionProfile=entries.firstOrNull{it.id==id}?:MID
    }
}
