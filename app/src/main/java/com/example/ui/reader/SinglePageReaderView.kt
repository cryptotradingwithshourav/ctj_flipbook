package com.example.ui.reader

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DocumentEntity
import com.example.data.model.ReaderFont
import com.example.data.model.ReaderPage
import com.example.data.model.ReaderPreferences
import com.example.data.model.ReaderTheme
import com.example.ui.theme.GoldFoil
import com.example.ui.theme.GoldLight
import com.example.ui.theme.MidnightBackground
import com.example.ui.theme.MidnightText
import com.example.ui.theme.MidnightTextSecondary
import com.example.ui.theme.NavyDark
import com.example.ui.theme.NavyDeep
import com.example.ui.theme.PaperBackground
import com.example.ui.theme.PaperText
import com.example.ui.theme.PaperTextSecondary
import com.example.ui.theme.SepiaBackground
import com.example.ui.theme.SepiaText
import com.example.ui.theme.SepiaTextSecondary
import kotlinx.coroutines.launch

@Composable
fun SinglePageReaderView(
    document: DocumentEntity,
    currentPage: ReaderPage,
    totalPages: Int,
    preferences: ReaderPreferences,
    onPageChange: (newPage: Int) -> Unit,
    onOpenControlPanel: () -> Unit,
    onRequestLandscape: () -> Unit,
    onOpenStoryboardStudio: () -> Unit,
    onCloseBook: () -> Unit,
    pageCurlState: PageCurlState,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()

    // Pinch-to-zoom and pan state
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    val themeBg = when (preferences.theme) {
        ReaderTheme.PAPER -> PaperBackground
        ReaderTheme.SEPIA -> SepiaBackground
        ReaderTheme.MIDNIGHT -> MidnightBackground
    }
    val themeText = when (preferences.theme) {
        ReaderTheme.PAPER -> PaperText
        ReaderTheme.SEPIA -> SepiaText
        ReaderTheme.MIDNIGHT -> MidnightText
    }
    val themeTextSecondary = when (preferences.theme) {
        ReaderTheme.PAPER -> PaperTextSecondary
        ReaderTheme.SEPIA -> SepiaTextSecondary
        ReaderTheme.MIDNIGHT -> MidnightTextSecondary
    }
    val fontFamily = when (preferences.font) {
        ReaderFont.LITERATA, ReaderFont.GEORGIA -> FontFamily.Serif
        ReaderFont.MERRIWEATHER -> FontFamily.Serif
        ReaderFont.ATKINSON -> FontFamily.SansSerif
    }

    val canTurnBackward = currentPage.pageNumber > 1
    val canTurnForward = currentPage.pageNumber < totalPages

    PageFlipContainer(
        state = pageCurlState,
        isLandscape = false,
        canTurnForward = canTurnForward,
        canTurnBackward = canTurnBackward,
        modifier = modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0A0F1A))
        ) {
            // Top Reader Navigation Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(NavyDeep)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onCloseBook,
                        modifier = Modifier.size(38.dp).testTag("close_book_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Close to Library",
                            tint = GoldLight
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    Column {
                        Text(
                            text = document.title,
                            color = GoldLight,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                        Text(
                            text = "Page ${currentPage.pageNumber} of $totalPages",
                            color = Color(0xFF9EAABF),
                            fontSize = 11.sp
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Quick Action: Storyboard Studio
                    Button(
                        onClick = onOpenStoryboardStudio,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0x33D4AF37),
                            contentColor = GoldLight
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.height(34.dp).testTag("storyboard_studio_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Movie,
                            contentDescription = null,
                            modifier = Modifier.size(15.dp),
                            tint = GoldLight
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Storyboards", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    // Switch to 16:9 Landscape Button
                    IconButton(
                        onClick = onRequestLandscape,
                        modifier = Modifier.size(38.dp).testTag("portrait_to_landscape_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ScreenRotation,
                            contentDescription = "Switch to Landscape 16:9",
                            tint = GoldLight
                        )
                    }
                }
            }

            // Main Paper Page Surface
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
                    .shadow(16.dp, RoundedCornerShape(8.dp))
                    .clip(RoundedCornerShape(8.dp))
                    .background(themeBg)
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            scale = (scale * zoom).coerceIn(1f, 3.5f)
                            if (scale > 1f) {
                                offset += pan
                            } else {
                                offset = Offset.Zero
                            }
                        }
                    }
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = {
                                // Tapping readable surface reveals orientation recommendation
                                onRequestLandscape()
                            },
                            onLongPress = {
                                // 500ms long press opens the shared control panel
                                onOpenControlPanel()
                            }
                        )
                    }
                    .testTag("single_page_reader_surface")
            ) {
                // Book Page Content with Zoom & Pan transform
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                            translationX = offset.x
                            translationY = offset.y
                        }
                        .padding(horizontal = 24.dp, vertical = 20.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    // Header Chapter Name
                    if (!currentPage.chapterName.isNullOrBlank()) {
                        Text(
                            text = currentPage.chapterName.uppercase(),
                            color = themeTextSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(themeTextSecondary.copy(alpha = 0.2f))
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                    }

                    // Main Reading Text
                    Text(
                        text = currentPage.text,
                        color = themeText,
                        fontSize = preferences.fontSizeSp.sp,
                        fontFamily = fontFamily,
                        lineHeight = (preferences.fontSizeSp * 1.55f).sp,
                        textAlign = TextAlign.Start,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(28.dp))

                    // Bottom Page Number
                    Text(
                        text = "— ${currentPage.pageNumber} —",
                        color = themeTextSecondary,
                        fontSize = 12.sp,
                        fontFamily = fontFamily,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Left Arrow Turn Button
                if (canTurnBackward) {
                    IconButton(
                        onClick = {
                            coroutineScope.launch {
                                pageCurlState.animateTurn(forward = false)
                            }
                        },
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .padding(start = 4.dp)
                            .size(40.dp)
                            .shadow(6.dp, CircleShape)
                            .background(Color(0x33000000), CircleShape)
                            .testTag("button_prev_page")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ChevronLeft,
                            contentDescription = "Previous Page",
                            tint = Color.White
                        )
                    }
                }

                // Right Arrow Turn Button
                if (canTurnForward) {
                    IconButton(
                        onClick = {
                            coroutineScope.launch {
                                pageCurlState.animateTurn(forward = true)
                            }
                        },
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(end = 4.dp)
                            .size(40.dp)
                            .shadow(6.dp, CircleShape)
                            .background(Color(0x33000000), CircleShape)
                            .testTag("button_next_page")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = "Next Page",
                            tint = Color.White
                        )
                    }
                }
            }
        }
    }
}
