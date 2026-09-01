package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.domain.HseImageAnnotation
import com.example.domain.HseImageMedia
import com.example.domain.HseVideoChapter
import com.example.domain.HseVideoMedia
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// ============================================================================
// 1. CINEMA HSE VIDEO PLAYER DIALOG
// ============================================================================

enum class VideoTab {
    CHAPTERS,
    KEY_POINTS,
    TRANSCRIPT
}

enum class PlaybackSpeed(val label: String, val multiplier: Float) {
    SPEED_0_75("0.75x", 0.75f),
    SPEED_1_0("1.0x", 1.0f),
    SPEED_1_25("1.25x", 1.25f),
    SPEED_1_5("1.5x", 1.5f)
}

@Composable
fun HseVideoPlayerDialog(
    video: HseVideoMedia,
    onDismiss: () -> Unit
) {
    var isPlaying by remember { mutableStateOf(true) }
    var currentSeconds by remember { mutableIntStateOf(0) }
    val totalSeconds = video.durationSeconds
    var selectedSpeed by remember { mutableStateOf(PlaybackSpeed.SPEED_1_0) }
    var areSubtitlesEnabled by remember { mutableStateOf(true) }
    var isMuted by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableStateOf(VideoTab.CHAPTERS) }
    var isCertifiedWatched by remember { mutableStateOf(false) }
    var showSpeedMenu by remember { mutableStateOf(false) }
    var showExportSnackbar by remember { mutableStateOf(false) }

    val accentColor = Color(video.accentColorHex)

    // Live video simulation playback timer
    LaunchedEffect(isPlaying, selectedSpeed) {
        while (isPlaying) {
            delay((1000 / selectedSpeed.multiplier).toLong())
            if (currentSeconds < totalSeconds) {
                currentSeconds += 1
            } else {
                isPlaying = false
                isCertifiedWatched = true
            }
        }
    }

    // Identify active chapter based on currentSeconds
    val currentChapter = remember(currentSeconds, video.chapters) {
        video.chapters.lastOrNull { currentSeconds >= it.timestampSeconds } ?: video.chapters.firstOrNull()
    }

    fun formatTime(sec: Int): String {
        val m = sec / 60
        val s = sec % 60
        return String.format("%02d:%02d", m, s)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Scaffold(
            containerColor = Color(0xFF030712), // Deep cinema dark
            topBar = {
                // Cinema Player Top Toolbar
                Surface(
                    color = Color(0xFF0F172A).copy(alpha = 0.95f),
                    border = BorderStroke(1.dp, Color(0xFF1E293B)),
                    shadowElevation = 8.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            IconButton(
                                onClick = onDismiss,
                                modifier = Modifier
                                    .size(38.dp)
                                    .background(Color.White.copy(alpha = 0.1f), CircleShape)
                                    .testTag("close_video_player")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Retour",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        color = accentColor.copy(alpha = 0.2f),
                                        shape = RoundedCornerShape(4.dp),
                                        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.4f))
                                    ) {
                                        Text(
                                            text = video.category,
                                            color = accentColor,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        color = Color(0xFF1E293B),
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = video.resolution,
                                            color = HSETextSecondary,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = video.title,
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        // Top Action Tools
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Subtitles toggle
                            IconButton(
                                onClick = { areSubtitlesEnabled = !areSubtitlesEnabled },
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(
                                        if (areSubtitlesEnabled) accentColor.copy(alpha = 0.25f) else Color.Transparent,
                                        CircleShape
                                    )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ClosedCaption,
                                    contentDescription = "Sous-titres",
                                    tint = if (areSubtitlesEnabled) accentColor else HSETextMuted,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            // Speed Selector Button
                            Box {
                                TextButton(
                                    onClick = { showSpeedMenu = true },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                    modifier = Modifier.height(34.dp)
                                ) {
                                    Text(
                                        text = selectedSpeed.label,
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                DropdownMenu(
                                    expanded = showSpeedMenu,
                                    onDismissRequest = { showSpeedMenu = false },
                                    modifier = Modifier.background(Color(0xFF1E293B))
                                ) {
                                    PlaybackSpeed.entries.forEach { speed ->
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    text = "Vitesse ${speed.label}",
                                                    color = if (selectedSpeed == speed) accentColor else Color.White,
                                                    fontWeight = if (selectedSpeed == speed) FontWeight.Bold else FontWeight.Normal
                                                )
                                            },
                                            onClick = {
                                                selectedSpeed = speed
                                                showSpeedMenu = false
                                            }
                                        )
                                    }
                                }
                            }

                            // DRM & Security Info Icon
                            IconButton(
                                onClick = { showExportSnackbar = true },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = "Document Sécurisé",
                                    tint = Color(0xFF60A5FA),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // =============================================================
                // 1. CINEMA VIDEO CANVAS AREA (16:9 Realistic Screen)
                // =============================================================
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9.5f)
                        .background(Color.Black)
                ) {
                    // Video Visual Backdrop / Ambient Simulation
                    val infiniteTransition = rememberInfiniteTransition(label = "playbackAnim")
                    val glowAlpha by infiniteTransition.animateFloat(
                        initialValue = 0.25f,
                        targetValue = 0.55f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(2000, easing = LinearEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "glowAlpha"
                    )

                    // Cinematic background gradient
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        accentColor.copy(alpha = if (isPlaying) glowAlpha else 0.15f),
                                        Color(0xFF0A0F1D),
                                        Color.Black
                                    )
                                )
                            )
                    )

                    // Video content simulation graphics
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.SpaceBetween,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Top Screen HUD
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(if (isPlaying) Color(0xFFEF4444) else Color(0xFF64748B), CircleShape)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isPlaying) "LECTURE EN COURS" else "EN PAUSE",
                                    color = Color.White.copy(alpha = 0.8f),
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            currentChapter?.let { ch ->
                                Text(
                                    text = ch.title,
                                    color = accentColor,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                            }
                        }

                        // Center Controls & Play/Pause Button
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // -10s
                            IconButton(
                                onClick = { currentSeconds = (currentSeconds - 10).coerceAtLeast(0) },
                                modifier = Modifier
                                    .size(44.dp)
                                    .background(Color.Black.copy(alpha = 0.4f), CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Replay10,
                                    contentDescription = "Reculer 10s",
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(20.dp))

                            // Main Play/Pause Button
                            Box(
                                modifier = Modifier
                                    .size(62.dp)
                                    .background(accentColor, CircleShape)
                                    .clickable { isPlaying = !isPlaying }
                                    .testTag("btn_play_pause"),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = if (isPlaying) "Pause" else "Lecture",
                                    tint = Color.White,
                                    modifier = Modifier.size(34.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(20.dp))

                            // +10s
                            IconButton(
                                onClick = { currentSeconds = (currentSeconds + 10).coerceAtMost(totalSeconds) },
                                modifier = Modifier
                                    .size(44.dp)
                                    .background(Color.Black.copy(alpha = 0.4f), CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Forward10,
                                    contentDescription = "Avancer 10s",
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }

                        // Dynamic Subtitles Bar (if enabled)
                        AnimatedVisibility(
                            visible = areSubtitlesEnabled,
                            enter = fadeIn(),
                            exit = fadeOut()
                        ) {
                            Surface(
                                color = Color.Black.copy(alpha = 0.75f),
                                shape = RoundedCornerShape(6.dp),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f))
                            ) {
                                Text(
                                    text = currentChapter?.description ?: "Sous-titres officiels en cours de diffusion...",
                                    color = Color(0xFFFEF08A),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }

                    // Bottom Player Controls Overlay
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color.Transparent, Color.Black.copy(alpha = 0.9f))
                                )
                            )
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        // Interactive Video Scrubber Slider
                        Slider(
                            value = currentSeconds.toFloat(),
                            onValueChange = { currentSeconds = it.toInt() },
                            valueRange = 0f..totalSeconds.toFloat(),
                            colors = SliderDefaults.colors(
                                thumbColor = accentColor,
                                activeTrackColor = accentColor,
                                inactiveTrackColor = Color.White.copy(alpha = 0.25f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(16.dp)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "${formatTime(currentSeconds)} / ${formatTime(totalSeconds)}",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "(-${formatTime(totalSeconds - currentSeconds)})",
                                    color = HSETextMuted,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = { isMuted = !isMuted },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                                        contentDescription = "Volume",
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(
                                    imageVector = Icons.Default.Fullscreen,
                                    contentDescription = "Plein écran",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                // =============================================================
                // 2. VIDEO INFO & TAB NAVIGATION AREA
                // =============================================================
                Surface(
                    color = Color(0xFF0F172A),
                    border = BorderStroke(1.dp, Color(0xFF1E293B))
                ) {
                    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = video.title,
                                    color = Color.White,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "${video.presenter} • ${video.site}",
                                    color = HSETextSecondary,
                                    fontSize = 12.sp
                                )
                            }

                            if (video.isCertificateEligible) {
                                Surface(
                                    color = if (isCertifiedWatched) Color(0xFF065F46) else accentColor.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(1.dp, if (isCertifiedWatched) Color(0xFF10B981) else accentColor)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (isCertifiedWatched) Icons.Default.Verified else Icons.Default.School,
                                            contentDescription = null,
                                            tint = if (isCertifiedWatched) Color(0xFF6EE7B7) else accentColor,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = if (isCertifiedWatched) "Attestation Prête" else "Certifiant",
                                            color = if (isCertifiedWatched) Color(0xFF6EE7B7) else accentColor,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Tab Selection Pills
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            TabPill(
                                label = "Chapitres (${video.chapters.size})",
                                isSelected = selectedTab == VideoTab.CHAPTERS,
                                onClick = { selectedTab = VideoTab.CHAPTERS },
                                modifier = Modifier.weight(1f)
                            )
                            TabPill(
                                label = "Points Clés (${video.keyPoints.size})",
                                isSelected = selectedTab == VideoTab.KEY_POINTS,
                                onClick = { selectedTab = VideoTab.KEY_POINTS },
                                modifier = Modifier.weight(1f)
                            )
                            TabPill(
                                label = "Fiche & Résumé",
                                isSelected = selectedTab == VideoTab.TRANSCRIPT,
                                onClick = { selectedTab = VideoTab.TRANSCRIPT },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // =============================================================
                // 3. TAB CONTENT
                // =============================================================
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    when (selectedTab) {
                        VideoTab.CHAPTERS -> {
                            item {
                                Text(
                                    text = "TABLE DES CHAPITRES HORODATÉS",
                                    color = HSETextMuted,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp,
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )
                            }
                            itemsIndexed(video.chapters) { index, chapter ->
                                val isCurrent = currentChapter == chapter
                                Surface(
                                    color = if (isCurrent) accentColor.copy(alpha = 0.15f) else Color(0xFF0F172A),
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(
                                        1.dp,
                                        if (isCurrent) accentColor else Color(0xFF1E293B)
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            currentSeconds = chapter.timestampSeconds
                                            isPlaying = true
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .background(
                                                    if (isCurrent) accentColor else Color(0xFF1E293B),
                                                    RoundedCornerShape(8.dp)
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = chapter.timestampFormatted,
                                                color = Color.White,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                fontFamily = FontFamily.Monospace
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(12.dp))

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = chapter.title,
                                                color = if (isCurrent) Color.White else HSETextPrimary,
                                                fontSize = 13.sp,
                                                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = chapter.description,
                                                color = HSETextSecondary,
                                                fontSize = 11.sp,
                                                lineHeight = 15.sp
                                            )
                                        }

                                        if (isCurrent) {
                                            Icon(
                                                imageVector = Icons.Default.PlayCircle,
                                                contentDescription = null,
                                                tint = accentColor,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        VideoTab.KEY_POINTS -> {
                            item {
                                Surface(
                                    color = Color(0xFF1E293B),
                                    shape = RoundedCornerShape(14.dp),
                                    border = BorderStroke(1.dp, Color(0xFF334155)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.Lightbulb,
                                                contentDescription = null,
                                                tint = Color(0xFFFEF08A),
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "DIRECTIVES DE SÉCURITÉ MAJEURES",
                                                color = Color.White,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(12.dp))
                                        video.keyPoints.forEachIndexed { i, point ->
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 4.dp),
                                                verticalAlignment = Alignment.Top
                                            ) {
                                                Text(
                                                    text = "•",
                                                    color = accentColor,
                                                    fontSize = 16.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = point,
                                                    color = HSETextPrimary,
                                                    fontSize = 13.sp,
                                                    lineHeight = 18.sp
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        VideoTab.TRANSCRIPT -> {
                            item {
                                Surface(
                                    color = Color(0xFF0F172A),
                                    shape = RoundedCornerShape(14.dp),
                                    border = BorderStroke(1.dp, Color(0xFF1E293B)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Text(
                                            text = "RÉSUMÉ DU MODULE",
                                            color = accentColor,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = video.summary,
                                            color = HSETextPrimary,
                                            fontSize = 13.sp,
                                            lineHeight = 19.sp
                                        )

                                        Spacer(modifier = Modifier.height(14.dp))
                                        HorizontalDivider(color = Color(0xFF1E293B))
                                        Spacer(modifier = Modifier.height(14.dp))

                                        // Attestation Certification Button
                                        Button(
                                            onClick = { isCertifiedWatched = true },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = if (isCertifiedWatched) Color(0xFF059669) else accentColor
                                            ),
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Icon(
                                                imageVector = if (isCertifiedWatched) Icons.Default.CheckCircle else Icons.Default.FactCheck,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = if (isCertifiedWatched) "Visionnage Certifié Conforme (Archivé)" else "Certifier avoir suivi ce module vidéo",
                                                color = Color.White,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showExportSnackbar) {
        AlertDialog(
            onDismissRequest = { showExportSnackbar = false },
            title = { Text("Téléchargement Hors-Ligne", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Le module vidéo \"${video.title}\" (${video.resolution}) est disponible en cache haute performance pour un visionnage sans connexion sur le terrain.",
                    color = HSETextSecondary
                )
            },
            confirmButton = {
                TextButton(onClick = { showExportSnackbar = false }) {
                    Text("Télécharger (145 Mo)", color = accentColor, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showExportSnackbar = false }) {
                    Text("Fermer", color = HSETextMuted)
                }
            },
            containerColor = Color(0xFF1E293B)
        )
    }
}

@Composable
private fun TabPill(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = if (isSelected) Color(0xFF2563EB) else Color(0xFF1E293B),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, if (isSelected) Color(0xFF3B82F6) else Color(0xFF334155)),
        modifier = modifier
            .height(34.dp)
            .clickable { onClick() }
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Text(
                text = label,
                color = if (isSelected) Color.White else HSETextSecondary,
                fontSize = 11.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
            )
        }
    }
}

// ============================================================================
// 2. HIGH-DEFINITION HSE IMAGE / PLAN / POSTER LIGHTBOX VIEWER DIALOG
// ============================================================================

enum class ImageInspectionFilter(val label: String) {
    NORMAL("Normal"),
    HIGH_CONTRAST("Contraste Chantier"),
    NIGHT_VISION("Vision Nuit"),
    BLUEPRINT("Plan Blueprint")
}

@Composable
fun HseImageViewerDialog(
    image: HseImageMedia,
    onDismiss: () -> Unit
) {
    var zoomLevel by remember { mutableFloatStateOf(1.0f) }
    var selectedFilter by remember { mutableStateOf(ImageInspectionFilter.NORMAL) }
    var areAnnotationsVisible by remember { mutableStateOf(true) }
    var selectedAnnotation by remember { mutableStateOf<HseImageAnnotation?>(null) }
    var showSecurityInfoDialog by remember { mutableStateOf(false) }

    val themeColor = Color(image.themeColorHex)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Scaffold(
            containerColor = Color(0xFF060B14),
            topBar = {
                // High-End Lightbox Top Toolbar
                Surface(
                    color = Color(0xFF0F172A).copy(alpha = 0.95f),
                    border = BorderStroke(1.dp, Color(0xFF1E293B)),
                    shadowElevation = 8.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            IconButton(
                                onClick = onDismiss,
                                modifier = Modifier
                                    .size(38.dp)
                                    .background(Color.White.copy(alpha = 0.1f), CircleShape)
                                    .testTag("close_image_viewer")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Fermer",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        color = themeColor.copy(alpha = 0.2f),
                                        shape = RoundedCornerShape(4.dp),
                                        border = BorderStroke(1.dp, themeColor.copy(alpha = 0.4f))
                                    ) {
                                        Text(
                                            text = image.category,
                                            color = themeColor,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = image.site,
                                        color = HSETextMuted,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                                Text(
                                    text = image.title,
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        // Right Action Controls: Annotations, Zoom, Download
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Annotations Toggle
                            IconButton(
                                onClick = { areAnnotationsVisible = !areAnnotationsVisible },
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(
                                        if (areAnnotationsVisible) themeColor.copy(alpha = 0.25f) else Color.Transparent,
                                        CircleShape
                                    )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PinDrop,
                                    contentDescription = "Balises de Sécurité",
                                    tint = if (areAnnotationsVisible) themeColor else HSETextMuted,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            // Document Sécurisé / Protection CIMENCAM
                            IconButton(
                                onClick = { showSecurityInfoDialog = true },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = "Document Sécurisé",
                                    tint = Color(0xFF60A5FA),
                                    modifier = Modifier.size(19.dp)
                                )
                            }
                        }
                    }
                }
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // =============================================================
                // 1. FILTER & ZOOM CONTROLLER CHIPS BAR
                // =============================================================
                Surface(
                    color = Color(0xFF0F172A),
                    border = BorderStroke(1.dp, Color(0xFF1E293B))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Zoom Pill
                        Surface(
                            color = Color(0xFF1E293B),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, Color(0xFF334155))
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                IconButton(
                                    onClick = { zoomLevel = (zoomLevel - 0.25f).coerceAtLeast(0.75f) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(Icons.Default.Remove, contentDescription = "Zoom -", tint = Color.White, modifier = Modifier.size(16.dp))
                                }
                                Text(
                                    text = "${(zoomLevel * 100).toInt()}%",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier.padding(horizontal = 6.dp)
                                )
                                IconButton(
                                    onClick = { zoomLevel = (zoomLevel + 0.25f).coerceAtMost(2.5f) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = "Zoom +", tint = Color.White, modifier = Modifier.size(16.dp))
                                }
                                IconButton(
                                    onClick = { zoomLevel = 1.0f },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = "Reset 100%", tint = HSETextSecondary, modifier = Modifier.size(16.dp))
                                }
                            }
                        }

                        // Filter Chips
                        ImageInspectionFilter.entries.forEach { filter ->
                            val isSel = selectedFilter == filter
                            Surface(
                                color = if (isSel) themeColor else Color(0xFF1E293B),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, if (isSel) themeColor else Color(0xFF334155)),
                                modifier = Modifier.clickable { selectedFilter = filter }
                            ) {
                                Text(
                                    text = filter.label,
                                    color = if (isSel) Color.White else HSETextSecondary,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }

                // =============================================================
                // 2. MAIN LIGHTBOX VISUAL CANVAS
                // =============================================================
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .background(
                            when (selectedFilter) {
                                ImageInspectionFilter.NORMAL -> Color(0xFF060B14)
                                ImageInspectionFilter.HIGH_CONTRAST -> Color(0xFF000000)
                                ImageInspectionFilter.NIGHT_VISION -> Color(0xFF031A0D)
                                ImageInspectionFilter.BLUEPRINT -> Color(0xFF071938)
                            }
                        )
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // High Definition Graphic Board representing the Poster or Technical Site Plan
                    Surface(
                        color = when (selectedFilter) {
                            ImageInspectionFilter.NORMAL -> Color(0xFF0F1E36)
                            ImageInspectionFilter.HIGH_CONTRAST -> Color(0xFF18181B)
                            ImageInspectionFilter.NIGHT_VISION -> Color(0xFF062D17)
                            ImageInspectionFilter.BLUEPRINT -> Color(0xFF0B2554)
                        },
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(
                            2.dp,
                            when (selectedFilter) {
                                ImageInspectionFilter.NORMAL -> themeColor.copy(alpha = 0.5f)
                                ImageInspectionFilter.HIGH_CONTRAST -> Color.White
                                ImageInspectionFilter.NIGHT_VISION -> Color(0xFF10B981)
                                ImageInspectionFilter.BLUEPRINT -> Color(0xFF38BDF8)
                            }
                        ),
                        shadowElevation = 12.dp,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp)
                        ) {
                            // Background Blueprint Grid Pattern
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val step = 40.dp.toPx()
                                val gridColor = Color.White.copy(alpha = 0.05f)
                                for (x in 0..(size.width / step).toInt()) {
                                    drawLine(
                                        color = gridColor,
                                        start = Offset(x * step, 0f),
                                        end = Offset(x * step, size.height),
                                        strokeWidth = 1f
                                    )
                                }
                                for (y in 0..(size.height / step).toInt()) {
                                    drawLine(
                                        color = gridColor,
                                        start = Offset(0f, y * step),
                                        end = Offset(size.width, y * step),
                                        strokeWidth = 1f
                                    )
                                }
                            }

                            // Header inside the image graphic
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Column {
                                        Text(
                                            text = "CIMENCAM • DIRECTION HSE",
                                            color = themeColor,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Black,
                                            letterSpacing = 1.sp
                                        )
                                        Text(
                                            text = image.title.uppercase(),
                                            color = Color.White,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                    }

                                    // Compass Rose / Official Stamp
                                    Surface(
                                        color = Color.Black.copy(alpha = 0.4f),
                                        shape = RoundedCornerShape(8.dp),
                                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                                        ) {
                                            Icon(Icons.Default.Explore, contentDescription = null, tint = themeColor, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("NORD", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }

                                // Interactive Annotations / Pins on the visual plan
                                if (areAnnotationsVisible) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .weight(1f)
                                    ) {
                                        image.annotations.forEachIndexed { idx, ann ->
                                            val isDanger = ann.isDanger
                                            val isSelected = selectedAnnotation == ann

                                            Box(
                                                modifier = Modifier
                                                    .align(
                                                        androidx.compose.ui.BiasAlignment(
                                                            (ann.normalizedX * 2) - 1f,
                                                            (ann.normalizedY * 2) - 1f
                                                        )
                                                    )
                                                    .clickable {
                                                        selectedAnnotation = if (isSelected) null else ann
                                                    }
                                            ) {
                                                Surface(
                                                    color = if (isDanger) Color(0xFFDC2626) else if (isSelected) Color(0xFF2563EB) else Color(0xFF059669),
                                                    shape = CircleShape,
                                                    border = BorderStroke(2.dp, Color.White),
                                                    shadowElevation = 6.dp,
                                                    modifier = Modifier.size(if (isSelected) 34.dp else 28.dp)
                                                ) {
                                                    Box(contentAlignment = Alignment.Center) {
                                                        Text(
                                                            text = "${idx + 1}",
                                                            color = Color.White,
                                                            fontSize = 11.sp,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                } else {
                                    Spacer(modifier = Modifier.weight(1f))
                                }

                                // Technical Stamp / Date at bottom
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.Bottom
                                ) {
                                    Text(
                                        text = "${image.resolution} • ${image.dateText}",
                                        color = HSETextMuted,
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace
                                    )

                                    Text(
                                        text = image.author,
                                        color = HSETextSecondary,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }

                    // Floating Tooltip for clicked pin/annotation
                    selectedAnnotation?.let { ann ->
                        Surface(
                            color = Color(0xFF0F172A).copy(alpha = 0.95f),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, if (ann.isDanger) Color(0xFFEF4444) else themeColor),
                            shadowElevation = 10.dp,
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth(0.9f)
                                .padding(bottom = 12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (ann.isDanger) Icons.Default.Warning else Icons.Default.LocationOn,
                                    contentDescription = null,
                                    tint = if (ann.isDanger) Color(0xFFEF4444) else themeColor,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = ann.title,
                                        color = Color.White,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = ann.description,
                                        color = HSETextSecondary,
                                        fontSize = 11.sp,
                                        lineHeight = 15.sp
                                    )
                                }
                                IconButton(
                                    onClick = { selectedAnnotation = null },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "Fermer", tint = HSETextMuted, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }

                // =============================================================
                // 3. TECHNICAL METADATA & REPERES PANEL (Bottom Sheet)
                // =============================================================
                Surface(
                    color = Color(0xFF0F172A),
                    border = BorderStroke(1.dp, Color(0xFF1E293B)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp)
                    ) {
                        Text(
                            text = "SPÉCIFICATIONS TECHNIQUES & REPÈRES DE SÉCURITÉ",
                            color = themeColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = image.description,
                            color = HSETextSecondary,
                            fontSize = 12.sp,
                            lineHeight = 17.sp
                        )

                        if (image.technicalSpecs.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(image.technicalSpecs) { spec ->
                                    Surface(
                                        color = Color(0xFF1E293B),
                                        shape = RoundedCornerShape(8.dp),
                                        border = BorderStroke(1.dp, Color(0xFF334155))
                                    ) {
                                        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                                            Text(text = spec.first, color = HSETextMuted, fontSize = 10.sp)
                                            Text(text = spec.second, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showSecurityInfoDialog) {
        AlertDialog(
            onDismissRequest = { showSecurityInfoDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = Color(0xFF60A5FA),
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Document Protégé & Confidentiel", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            text = {
                Column {
                    Text(
                        "Le plan officiel \"${image.title}\" est une ressource interne classée confidentielle CIMENCAM.",
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        color = Color(0xFF0F1E36),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, Color(0xFF1E3A8A))
                    ) {
                        Text(
                            "🔒 Conformément aux exigences de sécurité industrielle et de protection des sites sensibles (ISO 27001), l'exportation et le téléchargement externe sont verrouillés. La consultation s'effectue exclusivement en mode protégé au sein de l'application.",
                            color = Color(0xFF93C5FD),
                            fontSize = 11.sp,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSecurityInfoDialog = false }) {
                    Text("J'ai compris", color = themeColor, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = Color(0xFF1E293B)
        )
    }
}
