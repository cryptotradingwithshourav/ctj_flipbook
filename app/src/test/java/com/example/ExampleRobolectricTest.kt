package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.DocumentFormat
import com.example.service.DocumentParserService
import com.example.service.GeminiStoryboardService
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context matches app name`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("StoryFlip", appName)
    }

    @Test
    fun `document pagination creates correct sequential pages`() {
        val sampleText = """
            SCENE 1: EXT. ALLEY - NIGHT
            Rain falls steadily onto the dark cobblestones.
            
            SCENE 2: INT. WAREHOUSE - CONTINUOUS
            Sparks shower from the ceiling. Silas waits in the dark.
        """.trimIndent()

        val pages = DocumentParserService.paginate(sampleText, charsPerPage = 50)
        assertTrue(pages.isNotEmpty())
        assertEquals(1, pages[0].pageNumber)
    }

    @Test
    fun `storyboard breakdown extracts scenes and shots`() {
        val scriptText = """
            SCENE 1: EXT. NEON ALLEYWAY - NIGHT
            Detective Ray walks through the smoke.
            RAY
            I found the missing drive.
        """.trimIndent()

        val scenes = GeminiStoryboardService.generateLocalScriptBreakdown(scriptText)
        assertTrue(scenes.isNotEmpty())
        assertNotNull(scenes[0].shots)
        assertTrue(scenes[0].shots.isNotEmpty())
        assertEquals(1, scenes[0].shots[0].shotNumber)
    }
}
