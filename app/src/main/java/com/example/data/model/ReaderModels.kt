package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Supported document formats for import and reading.
 */
enum class DocumentFormat {
    PDF, TXT, EPUB, DOCX, SCRIPT
}

/**
 * Reader visual themes.
 */
enum class ReaderTheme(val displayName: String) {
    PAPER("Paper"),
    SEPIA("Sepia"),
    MIDNIGHT("Midnight")
}

/**
 * Reader typography font family options.
 */
enum class ReaderFont(val displayName: String) {
    LITERATA("Literata"),
    MERRIWEATHER("Merriweather"),
    GEORGIA("Georgia"),
    ATKINSON("Atkinson Hyperlegible")
}

/**
 * Camera shot framing types for storyboards.
 */
enum class ShotType(val label: String) {
    EXTREME_WIDE("Extreme Wide"),
    WIDE_SHOT("Wide Shot"),
    MEDIUM_SHOT("Medium Shot"),
    CLOSE_UP("Close Up"),
    EXTREME_CLOSE_UP("Extreme Close Up"),
    OVER_SHOULDER("Over-the-Shoulder"),
    POV("Point of View"),
    DUTCH_ANGLE("Dutch Angle")
}

/**
 * Camera movement styles.
 */
enum class CameraMovement(val label: String) {
    STATIC("Static"),
    PAN_LEFT("Pan Left"),
    PAN_RIGHT("Pan Right"),
    TILT_UP("Tilt Up"),
    TILT_DOWN("Tilt Down"),
    DOLLY_IN("Dolly In"),
    TRACKING("Tracking"),
    CRANE("Crane / Boom")
}

/**
 * Room Entity representing a stored document or script.
 */
@Entity(tableName = "documents")
data class DocumentEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val author: String = "Unknown",
    val format: DocumentFormat = DocumentFormat.SCRIPT,
    val rawContent: String,
    val totalPages: Int = 1,
    val lastReadPage: Int = 1,      // 1-based index
    val bookmarkPage: Int? = null,   // 1-based index, null if unread
    val hasProgress: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val lastOpenedAt: Long = System.currentTimeMillis()
)

/**
 * In-memory representation of a single rendered page.
 */
data class ReaderPage(
    val pageNumber: Int,            // 1-based page number
    val title: String = "",
    val text: String,
    val lines: List<String> = emptyList(),
    val chapterName: String? = null
)

/**
 * Storyboard shot item for cinematic pre-visualization.
 */
data class StoryboardShot(
    val id: String,
    val sceneNumber: Int,
    val shotNumber: Int,
    val shotType: ShotType,
    val cameraMovement: CameraMovement,
    val visualDescription: String,
    val actionNotes: String,
    val dialogue: String? = null,
    val visualPrompt: String,
    val imageUrl: String? = null,
    val sketchSeed: Int = 1
)

/**
 * A scene grouping storyboard shots.
 */
data class StoryboardScene(
    val sceneNumber: Int,
    val heading: String,
    val location: String,
    val timeOfDay: String,
    val summary: String,
    val shots: List<StoryboardShot>
)

/**
 * 6 grab zones for the natural 3D page curl physics.
 */
enum class PageCurlAnchor {
    TOP_LEFT,
    MIDDLE_LEFT,
    BOTTOM_LEFT,
    TOP_RIGHT,
    MIDDLE_RIGHT,
    BOTTOM_RIGHT
}

/**
 * Orientation layout modes.
 */
enum class ReaderLayoutMode {
    COVER_PORTRAIT,
    PORTRAIT_SINGLE_PAGE,
    LANDSCAPE_DOUBLE_PAGE
}

/**
 * Supported reading narration languages with emphasis on Indian languages and English variants.
 */
enum class ReadingLanguage(val code: String, val displayName: String, val flag: String) {
    HINDI("hi-IN", "हिन्दी (Hindi)", "🇮🇳"),
    ENGLISH_INDIA("en-IN", "English (India)", "🇮🇳"),
    ENGLISH_US("en-US", "English (US)", "🇺🇸"),
    ENGLISH_UK("en-GB", "English (UK)", "🇬🇧"),
    BENGALI("bn-IN", "বাংলা (Bengali)", "🇮🇳"),
    TAMIL("ta-IN", "தமிழ் (Tamil)", "🇮🇳"),
    TELUGU("te-IN", "తెలుగు (Telugu)", "🇮🇳"),
    MARATHI("mr-IN", "मराठी (Marathi)", "🇮🇳")
}

/**
 * Natural voice personas.
 */
enum class VoicePreset(val id: String, val title: String, val persona: String) {
    FEMALE_NARRATOR("female_narrator", "Priya / Elena", "Clear, elegant expressive narrator"),
    MALE_NARRATOR("male_narrator", "Aarav / Marcus", "Warm, authoritative deep voice"),
    STORYTELLER("storyteller", "Dada-ji (Elder)", "Gentle, wise fireside storyteller"),
    YOUTHFUL("youthful", "Kabir / Maya", "Energetic, bright youthful cadence")
}

/**
 * User reader preferences.
 */
data class ReaderPreferences(
    val font: ReaderFont = ReaderFont.LITERATA,
    val fontSizeSp: Float = 19f,     // Range 14 to 32
    val theme: ReaderTheme = ReaderTheme.PAPER,
    val speechRate: Float = 1.0f,    // 0.5x, 0.75x, 1.0x, 1.25x, 1.5x, 2.0x
    val language: ReadingLanguage = ReadingLanguage.HINDI,
    val voicePreset: VoicePreset = VoicePreset.FEMALE_NARRATOR,
    val turnSound: Boolean = true,
    val autoBookmark: Boolean = true
)
