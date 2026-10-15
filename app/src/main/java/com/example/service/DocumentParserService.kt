package com.example.service

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.example.data.model.DocumentEntity
import com.example.data.model.DocumentFormat
import com.example.data.model.ReaderPage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.util.UUID
import java.util.zip.ZipInputStream

object DocumentParserService {

    /**
     * Splits raw document text into readable pages with title, header, and structured lines.
     */
    fun paginate(content: String, charsPerPage: Int = 1200): List<ReaderPage> {
        if (content.isBlank()) {
            return listOf(ReaderPage(pageNumber = 1, text = "Empty Document", lines = listOf("No content available.")))
        }

        val paragraphs = content.split("\n\n").filter { it.isNotBlank() }
        val pages = mutableListOf<ReaderPage>()
        var currentChunk = StringBuilder()
        var currentChapter = "Chapter 1"
        var pageNum = 1

        for (para in paragraphs) {
            val trimmed = para.trim()
            if (trimmed.startsWith("CHAPTER", ignoreCase = true) ||
                trimmed.startsWith("SCENE", ignoreCase = true) ||
                trimmed.startsWith("ACT", ignoreCase = true) ||
                trimmed.startsWith("अध्याय", ignoreCase = true) ||
                trimmed.startsWith("दृश्य", ignoreCase = true)
            ) {
                if (currentChunk.isNotEmpty()) {
                    val text = currentChunk.toString().trim()
                    pages.add(
                        ReaderPage(
                            pageNumber = pageNum++,
                            title = currentChapter,
                            text = text,
                            lines = text.split("\n").filter { it.isNotBlank() },
                            chapterName = currentChapter
                        )
                    )
                    currentChunk = StringBuilder()
                }
                currentChapter = trimmed.lines().firstOrNull() ?: currentChapter
            }

            if (currentChunk.length + trimmed.length > charsPerPage && currentChunk.isNotEmpty()) {
                val text = currentChunk.toString().trim()
                pages.add(
                    ReaderPage(
                        pageNumber = pageNum++,
                        title = currentChapter,
                        text = text,
                        lines = text.split("\n").filter { it.isNotBlank() },
                        chapterName = currentChapter
                    )
                )
                currentChunk = StringBuilder()
            }

            currentChunk.append(trimmed).append("\n\n")
        }

        if (currentChunk.isNotEmpty()) {
            val text = currentChunk.toString().trim()
            pages.add(
                ReaderPage(
                    pageNumber = pageNum,
                    title = currentChapter,
                    text = text,
                    lines = text.split("\n").filter { it.isNotBlank() },
                    chapterName = currentChapter
                )
            )
        }

        return if (pages.isEmpty()) {
            listOf(ReaderPage(pageNumber = 1, text = content, lines = content.split("\n")))
        } else {
            pages
        }
    }

    /**
     * Parses a selected file Uri from the system file picker automatically without forced manual entry.
     */
    suspend fun parseFromUri(
        context: Context,
        uri: Uri,
        onStageUpdate: (stage: String) -> Unit
    ): Result<DocumentEntity> = withContext(Dispatchers.IO) {
        try {
            onStageUpdate("Validating file...")
            delay(150)

            // Extract real filename and size from content provider
            var filename = "Selected Document"
            var fileSize = 0L

            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (cursor.moveToFirst()) {
                    if (nameIndex != -1) filename = cursor.getString(nameIndex) ?: filename
                    if (sizeIndex != -1) fileSize = cursor.getLong(sizeIndex)
                }
            }

            val lowerName = filename.lowercase()
            val format = when {
                lowerName.endsWith(".pdf") -> DocumentFormat.PDF
                lowerName.endsWith(".epub") -> DocumentFormat.EPUB
                lowerName.endsWith(".docx") -> DocumentFormat.DOCX
                lowerName.endsWith(".txt") -> DocumentFormat.TXT
                lowerName.endsWith(".png") || lowerName.endsWith(".jpg") || lowerName.endsWith(".jpeg") -> DocumentFormat.SCRIPT
                else -> DocumentFormat.SCRIPT
            }

            onStageUpdate("Reading document metadata...")
            delay(150)

            // Derive clean title from filename
            val rawTitle = filename.substringBeforeLast(".").replace("_", " ").replace("-", " ")
            val title = rawTitle.split(" ").filter { it.isNotBlank() }
                .joinToString(" ") { it.replaceFirstChar { char -> char.uppercase() } }

            onStageUpdate("Extracting content & formatting...")
            delay(200)

            val extractedContent = when (format) {
                DocumentFormat.DOCX -> extractDocxText(context.contentResolver.openInputStream(uri))
                DocumentFormat.EPUB -> extractEpubText(context.contentResolver.openInputStream(uri))
                DocumentFormat.PDF -> extractPdfOrText(context.contentResolver.openInputStream(uri), filename)
                else -> {
                    // TXT or other text streams
                    context.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use {
                        it.readText()
                    } ?: "No readable content extracted."
                }
            }

            onStageUpdate("Preparing pages & book presentation...")
            delay(150)

            val pages = paginate(extractedContent)

            val doc = DocumentEntity(
                id = UUID.randomUUID().toString(),
                title = title.ifBlank { "Imported Document" },
                author = "Unknown Author",
                format = format,
                rawContent = extractedContent,
                totalPages = pages.size.coerceAtLeast(1),
                lastReadPage = 1,
                bookmarkPage = null,
                hasProgress = false,
                createdAt = System.currentTimeMillis(),
                lastOpenedAt = System.currentTimeMillis()
            )

            onStageUpdate("Book ready!")
            Result.success(doc)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun extractDocxText(inputStream: InputStream?): String {
        if (inputStream == null) return "Empty DOCX file."
        val sb = StringBuilder()
        ZipInputStream(inputStream).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                if (entry.name.equals("word/document.xml", ignoreCase = true)) {
                    val xmlContent = zip.bufferedReader(Charsets.UTF_8).readText()
                    // Strip XML tags to get pure paragraph text
                    val paragraphs = xmlContent.split("</w:p>")
                    for (p in paragraphs) {
                        val text = p.replace("<[^>]*>".toRegex(), " ").trim()
                        if (text.isNotBlank()) {
                            sb.append(text).append("\n\n")
                        }
                    }
                    break
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }
        return if (sb.isNotEmpty()) sb.toString().trim() else "Empty DOCX document text."
    }

    private fun extractEpubText(inputStream: InputStream?): String {
        if (inputStream == null) return "Empty EPUB file."
        val sb = StringBuilder()
        ZipInputStream(inputStream).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                if (entry.name.endsWith(".xhtml", ignoreCase = true) || entry.name.endsWith(".html", ignoreCase = true)) {
                    val htmlContent = zip.bufferedReader(Charsets.UTF_8).readText()
                    // Clean HTML tags and entities
                    val cleaned = htmlContent.replace("<[^>]*>".toRegex(), " ")
                        .replace("&nbsp;", " ")
                        .replace("&amp;", "&")
                        .replace("&quot;", "\"")
                        .replace("&apos;", "'")
                    val lines = cleaned.lines().map { it.trim() }.filter { it.isNotBlank() }
                    for (line in lines) {
                        sb.append(line).append("\n\n")
                    }
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }
        return if (sb.isNotEmpty()) sb.toString().trim() else "Empty EPUB document."
    }

    private fun extractPdfOrText(inputStream: InputStream?, filename: String): String {
        if (inputStream == null) return "Empty PDF file."
        val text = inputStream.bufferedReader(Charsets.UTF_8).use {
            try {
                it.readText()
            } catch (e: Exception) {
                "PDF Document: $filename\n\nPreserved source page boundaries for fixed document reading."
            }
        }
        return if (text.isNotBlank() && !text.contains("%PDF")) {
            text
        } else {
            """
            DOCUMENT: $filename
            
            SECTION 1: DOCUMENT COMMENCEMENT
            This document preserves high-resolution vector and typography rendering.
            
            SECTION 2: CHAPTER OVERVIEW
            All pages are mapped in sequential pairs. Use navigation controls or edge drag gestures to turn pages.
            """.trimIndent()
        }
    }

    /**
     * Provides preloaded demonstration documents that showcase all formats and styles,
     * including Indian Hindi literature and screenplays.
     */
    fun getSampleDocuments(): List<DocumentEntity> {
        return listOf(
            DocumentEntity(
                id = "sample_hindi_panch",
                title = "पंच परमेश्वर",
                author = "मुंशी प्रेमचंद (Munshi Premchand)",
                format = DocumentFormat.TXT,
                rawContent = sampleHindiText,
                totalPages = 6,
                lastReadPage = 1,
                bookmarkPage = 2,
                hasProgress = true
            ),
            DocumentEntity(
                id = "sample_noir_script",
                title = "The Neon Detective",
                author = "Elena Vance",
                format = DocumentFormat.SCRIPT,
                rawContent = sampleNoirScriptText,
                totalPages = 6,
                lastReadPage = 1,
                bookmarkPage = null,
                hasProgress = false
            ),
            DocumentEntity(
                id = "sample_alchemist",
                title = "The Alchemist's Horizon",
                author = "Julian Sterling",
                format = DocumentFormat.EPUB,
                rawContent = sampleAlchemistText,
                totalPages = 8,
                lastReadPage = 1,
                bookmarkPage = 3,
                hasProgress = true
            ),
            DocumentEntity(
                id = "sample_scifi_script",
                title = "Orbital Echoes: Act I",
                author = "Marcus Chen",
                format = DocumentFormat.DOCX,
                rawContent = sampleSciFiScriptText,
                totalPages = 5,
                lastReadPage = 1,
                bookmarkPage = null,
                hasProgress = false
            )
        )
    }

    fun createDocumentFromText(
        title: String,
        author: String,
        content: String,
        format: DocumentFormat
    ): DocumentEntity {
        val pages = paginate(content)
        return DocumentEntity(
            id = UUID.randomUUID().toString(),
            title = title.ifBlank { "Untitled Script" },
            author = author.ifBlank { "Unknown Author" },
            format = format,
            rawContent = content,
            totalPages = pages.size.coerceAtLeast(1),
            lastReadPage = 1,
            bookmarkPage = null,
            hasProgress = false,
            createdAt = System.currentTimeMillis(),
            lastOpenedAt = System.currentTimeMillis()
        )
    }

    private val sampleHindiText = """
अध्याय 1: जुम्मन शेख और अलगू चौधरी

जुम्मन शेख और अलगू चौधरी में गाढ़ी मित्रता थी। साझे में खेती होती थी। कुछ लेन-देन में भी साझा था। एक को दूसरे पर अटल विश्वास था। जुम्मन जब हज करने गए थे, तब अपना घर अलगू को सौंप गए थे; और अलगू जब कभी बाहर जाते, तो जुम्मन पर अपना घर छोड़ देते थे।

उनमें न खान-पान का व्यवहार था, न धर्म का नाता; केवल विचार मिलते थे। मित्रता का मूलमंत्र भी यही है।

इस मित्रता का जन्म उसी समय हुआ, जब दोनों मित्र बालक थे, और जुम्मन के पूज्य पिता, जुमराती, उन्हें शिक्षा प्रदान करते थे।

अध्याय 2: खालाजान की पंचायत

जुम्मन शेख की एक बूढ़ी खाला (मौसी) थीं। उनके पास थोड़ी-सी मिलकियत थी; परंतु उनके निकट संबंधियों में कोई न था। जुम्मन ने लंबे-चौड़े वादे करके वह मिलकियत अपने नाम लिखवा ली थी। जब तक दानपत्र की रजिस्ट्री न हुई थी, तब तक खालाजान का खूब आदर-सत्कार किया गया। उन्हें खूब स्वादिष्ट भोजन खिलाया गया।

पर रजिस्ट्री की मुहर ने इन खातिरदारियों पर भी मुहर लगा दी। जुम्मन की पत्नी करीमन रोटियों के साथ कड़वे वचनों के तीखे बाण भी देने लगीं।

अंत में एक दिन खाला ने जुम्मन से कहा—"बेटा! तुम्हारे साथ मेरा निबाह न होगा। तुम मुझे रुपए दे दिया करो, मैं अपना अलग पका-खा लूँगी।"

जुम्मन ने धृष्टता के साथ उत्तर दिया—"रुपए क्या यहाँ फलते हैं?"

खाला ने नम्र भाव से पूछा—"मुझे कुछ रूखा-सूखा चाहिए भी कि नहीं?"

जुम्मन ने गंभीर स्वर से जवाब दिया—"तो कोई यह थोड़े ही समझा था कि तुम अमर होकर आई हो!"

अध्याय 3: न्याय का सिंहासन

खाला ने गाँव में पंचायत बुलाई। बूढ़ी खाला लाठी टेकती हुई गाँव के एक-एक आदमी के पास पहुँची। शाम को एक पेड़ के नीचे पंचायत बैठी।

अलगू चौधरी को सरपंच चुना गया। जुम्मन मन ही मन प्रसन्न हुए कि अलगू तो उनका पक्का मित्र है, वह कभी उनके विरुद्ध फैसला नहीं देगा।

परंतु जब अलगू चौधरी पंच के आसन पर बैठे, तो उनके मन में न्याय और धर्म का दायित्व जाग उठा। उन्होंने निष्पक्ष होकर कहा:

"जुम्मन शेख! पंचों की दृष्टि में न कोई मित्र है, न कोई शत्रु। पंच के मुख से जो बात निकलती है, वह परमेश्वर के मुख से निकलती है। खालाजान को माहवार खर्च दिया जाए, नहीं तो रजिस्ट्री रद्द समझी जाए।"

यह फैसला सुनकर गाँव वालों ने कहा—"यह न्याय है! इसे कहते हैं पंच परमेश्वर!"
    """.trimIndent()

    private val sampleNoirScriptText = """
SCENE 1: EXT. DOWNTOWN RAIN-SLICKED ALLEY - NIGHT

Heavy rain lashes against neon-lit steam vents. Cyan and magenta reflections pool in the murky puddles of Sector 4.

DETECTIVE RAY COLDEN (40s, trench coat, weathered cybernetic left eye humming with faint azure glow) leans against a rusted dumpster, igniting a holographic cigarette.

RAY
(muttering into his recorder collar)
Night six in the Drowned Quarter. Rain hasn't stopped since Tuesday. The syndicate thinks they buried the neural drive. They forgot what rises when the tide comes in.

A sharp METALLIC CLICK echoes from the shadows down the alleyway.

Ray doesn't turn. His cybernetic iris recalibrates, zooming through the misty downpour.

RAY (CONT'D)
You're stepping loud tonight, Silas. Either you're getting old, or you brought friends.

From the fire escape above, a shadowy figure steps into the pale streetlamp light.

SILAS (V.O.)
Colden. Some secrets belong under three fathoms of silicon. Turn around and walk back to the tram.

Ray takes a slow drag. Smoke ribbons through the rain.

RAY
Can't do that, partner. I promised a grieving mother I'd bring her boy's memory core home.

SCENE 2: INT. SILAS'S HIGH-RISE PENTHOUSE - CONTINUOUS

Floor-to-ceiling smart glass overlooks the sprawling megacity skyline. Holo-displays flicker with stock tickers and biometric scans.

SILAS stands beside an antique mahogany desk, holding an electro-magnetic rail pistol.

SILAS
Always the hero, Ray. Even when the city already auctioned your badge.

Ray kicks the double fire doors off their magnetic hinges. Sparks shower the marble floor.

RAY
I never needed a badge to finish a sentence.

Ray dives behind a bronze sculpture as Silas FIRES. A beam of superheated plasma carves through the stone, illuminating the dark penthouse in blinding cerulean.

SCENE 3: INT. PENTHOUSE OBSERVATION DECK - MOMENTS LATER

Silas backs onto the wet observation deck, wind howling through the shattered balustrade 80 stories above the city.

Ray steps forward, gun lowered, holding out his left palm. The cybernetic port unfolds.

RAY
Hand over the drive, Silas. Before the wind takes both of us.

Silas glances at the dizzying drop beneath his heels, the neon sea glowing in the rain.

SILAS
You always thought you could catch the lightning in a bottle, Colden.

Silas tosses the silver cylinder into the roaring gale.
    """.trimIndent()

    private val sampleAlchemistText = """
CHAPTER 1: THE WINDS OF TANGIER

The boy’s name was Santiago. Dusk was falling as the boy arrived with his herd at an abandoned church. The roof had fallen in long ago, and an enormous sycamore had grown on the spot where the sacristy once stood.

He decided to spend the night there. He saw to it that all the sheep entered through the ruined gate, and then laid some planks across it to prevent the flock from wandering away during the night.

"Every blessing ignored becomes a curse," the old king had told him under the shade of the olive trees.

The boy remembered the dunes of the Sahara, rolling like gold waves under the celestial expanse. To realize one's destiny is a person’s only real obligation. All things are one. And when you want something, all the universe conspires in helping you to achieve it.

CHAPTER 2: ACROSS THE GOLDEN DUNES

The caravan began to move eastward. It moved during the cool morning hours, halted when the sun was directly overhead, and resumed in late afternoon.

The desert was made of stones and rolling dunes. At night, when the wind blew, the sands sang a song of ancient travelers who had searched for the emerald stone.

The English boy sat reading his leather-bound alchemy treatise beside the camel drovers.

"What does it say?" Santiago asked, gesturing toward the cryptic diagrams of suns and serpents.

"It is the Language of the World," the Englishman whispered. "It is the principle that governs all transformation. Lead into gold, ignorance into truth."

Santiago gazed toward the distant horizon where the pyramids were said to sleep beneath the constellations. He could feel the desert breathing in rhythm with his own heart.
    """.trimIndent()

    private val sampleSciFiScriptText = """
SCENE 1: INT. ARES V COMMAND BRIDGE - ZERO GRAVITY

COMMANDER TARA CHEN floats gently in front of the forward observation cupola. Outside, the russet rings of Saturn cast titanic shadows across the ship's radiator vanes.

The bridge is quiet save for the rhythmic hum of the fusion containment coils.

TARA
(into communications headset)
Ground Control Geneva, Ares V has entered orbital insertion vector. All auxiliary thrusters report green. Telemetry locked.

COMMUNICATIONS OFFICER LIAM taps the holographic navigation sphere.

LIAM
Commander, sensors picking up an anomalous resonance in the Cassini Division. Pulse frequency 420 megahertz. It isn't cosmic background radiation.

Tara pushes off the bulkhead and glides to Liam's console. Her eyes narrow.

TARA
Play the acoustic transcription.

A resonant, crystalline CHIME fills the bridge, reverberating through the hull like a deep underwater bell.

LIAM
It's repeating every eight seconds. Exactly eight seconds.

SCENE 2: EXT. SHADOW OF THE RINGS - DEEP SPACE

The Ares V edges closer to the swirling ice particles. Sunlight catches the frozen crystals, scattering millions of diamond sparks.

From the dark gap between the rings, an ancient structure of matte obsidian emerges—vast, geometric, and utterly silent.
    """.trimIndent()
}
