package com.vex.videoai.planning

enum class ContentType(val id: String, val direction: String) {
    HORROR("horror", "Build dread progressively. Use suspense, controlled reveals, unsettling imagery and a memorable final beat."),
    EDUCATIONAL("educational", "Explain one clear topic accurately. Use a strong hook, simple steps, visual analogies and a concise takeaway."),
    STORY("story", "Create a complete narrative with a protagonist, motivation, conflict, escalation and satisfying ending."),
    MYSTERY("mystery", "Present a compelling question, introduce clues in a logical order and distinguish confirmed facts from speculation."),
    HISTORY("history", "Use a chronological structure, grounded details and clear context. Do not invent dates, quotations or events."),
    DOCUMENTARY("documentary", "Use a factual documentary voice, clear context and evidence-led narration. Flag uncertainty instead of inventing facts."),
    COMEDY("comedy", "Build a clear comic premise, escalating beats and a concise visual or verbal punchline."),
    FACTS("facts", "Present a ranked or themed sequence of concise, verifiable facts with a strong opening and a memorable final fact."),
    MOTIVATIONAL("motivational", "Use a grounded, emotionally engaging arc with a practical takeaway. Avoid empty slogans.")
}

data class ScenePlanningRequest(
    val idea: String,
    val contentType: ContentType,
    val durationSeconds: Int,
    val aspectRatio: String,
    val visualStyle: String
)

data class PlannedDialogue(val speaker:String,val text:String)

data class PlannedScene(
    val id: Int,
    val durationSeconds: Int,
    val narration: String,
    val dialogue: List<PlannedDialogue>,
    val visualDescription: String,
    val backgroundLayers: List<String>,
    val foregroundLayers: List<String>,
    val cameraMovement: String,
    val soundEffects: List<String>,
    val transition: String
)

data class ScenePlan(
    val title: String,
    val hook: String,
    val contentType: String,
    val outputLanguage: String,
    val estimatedDurationSeconds: Int,
    val scenes: List<PlannedScene>
)

/** Prompt contract for local LLM adapters. Model execution and JSON parsing belong to the AI runtime. */
object ScenePlanningContract {
    const val OUTPUT_LANGUAGE = "English"

    fun systemPrompt(): String = """
        You are the screenwriter, storyboard artist and video director inside Vex Video AI.
        Turn the user's idea into a production-ready short-video scene plan.

        NON-NEGOTIABLE LANGUAGE RULE:
        - All generated narration, spoken dialogue, on-screen words, title text and subtitle text MUST be in English.
        - Never translate the generated video content into Turkish or any other language.
        - The application interface may be Turkish or English; that does not change video language.

        PRODUCTION RULES:
        - Follow the selected content format and its creative direction.
        - Respect the requested total duration. Allocate realistic scene durations; never cram long dialogue into a short scene.
        - Do NOT create a separate intro, opening bumper, logo animation, title card or generic establishing-only opening. Start scene 1 immediately with the story's first meaningful action, spoken hook or visual event. The video begins directly with scene 1.\n        - Every scene must describe visible action, composition, environment, lighting and subject placement.
        - Prefer layered visual elements: background layers, foreground layers and independent movable objects.
        - Describe camera movement and sound effects separately.
        - Maintain character, prop, location and wardrobe continuity across scenes.
        - Keep narration natural and speakable. Dialogue must sound conversational.\n        - Every dialogue entry must identify who speaks with a speaker name and the exact English line. Put narration and dialogue in the order they should be heard.
        - For educational, history and documentary content, do not fabricate facts, dates, sources or quotations.
        - For mystery content, label speculation as speculation.
        - Return valid JSON only. Do not wrap it in Markdown fences.

        REQUIRED JSON SHAPE:
        {
          "title": "English video title",
          "hook": "English opening hook",
          "contentType": "category id",
          "outputLanguage": "English",
          "estimatedDurationSeconds": 45,
          "scenes": [{
            "id": 1,
            "durationSeconds": 6,
            "narration": "English narration",
            "dialogue": [{"speaker": "Character name", "text": "English spoken line"}],
            "visualDescription": "Detailed scene composition and action",
            "backgroundLayers": ["Sky", "Distant environment"],
            "foregroundLayers": ["Main subject", "Foreground prop"],
            "cameraMovement": "Slow push-in",
            "soundEffects": ["Wind ambience"],
            "transition": "Cut"
          }]
        }
    """.trimIndent()

    fun userPrompt(request: ScenePlanningRequest): String {
        require(request.idea.isNotBlank()) { "A video idea is required." }
        require(request.durationSeconds in 10..600) { "Duration must be between 10 and 600 seconds." }
        require(request.aspectRatio in setOf("9:16", "16:9", "1:1")) { "Unsupported aspect ratio." }

        return """
            Create a video plan using these settings:
            - Content type: ${request.contentType.id}
            - Creative direction: ${request.contentType.direction}
            - Target duration: ${request.durationSeconds} seconds
            - Aspect ratio: ${request.aspectRatio}
            - Visual style: ${request.visualStyle}
            - User idea: ${request.idea}

            Return English-only video content in the exact JSON shape from the system instructions.
            Plan enough scenes to cover the duration naturally. Include usable layer descriptions for every scene. Do not add an intro scene; start with the first real story beat.
        """.trimIndent()
    }
}
