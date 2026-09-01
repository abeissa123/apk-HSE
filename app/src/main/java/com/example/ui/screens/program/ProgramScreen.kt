package com.example.ui.screens.program

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entity.EventEntity
import com.example.data.entity.TrainingEntity
import com.example.domain.HseImageMedia
import com.example.domain.HseStaticData
import com.example.domain.HseVideoMedia
import com.example.ui.components.HseImageViewerDialog
import com.example.ui.components.HseVideoPlayerDialog
import com.example.ui.theme.*
import com.example.ui.viewmodel.HseViewModel

enum class ProgramSubTab {
    EVENTS,
    TRAININGS
}

@Composable
fun ProgramScreen(
    viewModel: HseViewModel,
    modifier: Modifier = Modifier
) {
    var subTab by remember { mutableStateOf(ProgramSubTab.EVENTS) }
    var selectedFilterSite by remember { mutableStateOf("Tous sites") }
    var viewingVideo by remember { mutableStateOf<HseVideoMedia?>(null) }
    var viewingPoster by remember { mutableStateOf<HseImageMedia?>(null) }

    val events by viewModel.events.collectAsState()
    val trainings by viewModel.trainings.collectAsState()


    val filteredTrainings = if (selectedFilterSite == "Tous sites") {
        trainings
    } else {
        trainings.filter { it.site.contains(selectedFilterSite.replace("Site ", "").replace("Site de ", ""), ignoreCase = true) }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(HSEDarkBg)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 80.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            // Hero Banner (Compact & Responsive)
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color(0xFF1E3A8A),
                                    Color(0xFF0F1A30)
                                )
                            )
                        )
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(Color.White.copy(alpha = 0.15f), RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CalendarMonth,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Programme",
                                color = Color.White,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Formations, événements et causeries",
                                color = Color.White.copy(alpha = 0.85f),
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }

            // Segmented Filters (Pic 10 & 11)
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Tab 1: Événements & Campagnes
                    Surface(
                        color = if (subTab == ProgramSubTab.EVENTS) HSEBluePrimary else HSECardBg,
                        shape = RoundedCornerShape(20.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (subTab == ProgramSubTab.EVENTS) HSEBlueLight else HSECardBorder
                        ),
                        modifier = Modifier
                            .clickable { subTab = ProgramSubTab.EVENTS }
                            .testTag("tab_events_campaigns")
                    ) {
                        Text(
                            text = "Événements & Campagnes",
                            color = if (subTab == ProgramSubTab.EVENTS) Color.White else HSETextSecondary,
                            fontSize = 12.sp,
                            fontWeight = if (subTab == ProgramSubTab.EVENTS) FontWeight.Bold else FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }

                    // Tab 2: Formations
                    Surface(
                        color = if (subTab == ProgramSubTab.TRAININGS) HSEBluePrimary else HSECardBg,
                        shape = RoundedCornerShape(20.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (subTab == ProgramSubTab.TRAININGS) HSEBlueLight else HSECardBorder
                        ),
                        modifier = Modifier
                            .clickable { subTab = ProgramSubTab.TRAININGS }
                            .testTag("tab_trainings")
                    ) {
                        Text(
                            text = "Formations",
                            color = if (subTab == ProgramSubTab.TRAININGS) Color.White else HSETextSecondary,
                            fontSize = 12.sp,
                            fontWeight = if (subTab == ProgramSubTab.TRAININGS) FontWeight.Bold else FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }

                    // Site Filter Pill
                    val filterSites = listOf("Tous sites", "Figuil", "Bonabéri", "Nomayos")
                    filterSites.forEach { site ->
                        val isSelected = selectedFilterSite == site
                        Surface(
                            color = if (isSelected) HSEBlueDark else HSECardBg,
                            shape = RoundedCornerShape(20.dp),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isSelected) HSEBlueLight else HSECardBorder
                            ),
                            modifier = Modifier.clickable { selectedFilterSite = site }
                        ) {
                            Text(
                                text = "Site $site",
                                color = if (isSelected) Color.White else HSETextSecondary,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
            }

            // SubTab Content: Events (Pic 10)
            if (subTab == ProgramSubTab.EVENTS) {
                items(events, key = { it.id }) { event ->
                    EventCardItem(
                        event = event,
                        onViewPoster = {
                            viewingPoster = HseStaticData.images.find { it.id == "img_golden_rules" } ?: HseStaticData.images.first()
                        },
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
                    )
                }
            }

            // SubTab Content: Formations (Pic 11)
            if (subTab == ProgramSubTab.TRAININGS) {
                items(filteredTrainings, key = { it.id }) { training ->
                    TrainingCardItem(
                        training = training,
                        onToggleEnrollment = { viewModel.toggleTrainingEnrollment(training) },
                        onWatchVideo = {
                            val videoMatch = when {
                                training.title.contains("LOTO", ignoreCase = true) || training.title.contains("Consignation", ignoreCase = true) ->
                                    HseStaticData.videos.find { it.id == "vid_loto" }
                                training.title.contains("Secours", ignoreCase = true) || training.title.contains("SST", ignoreCase = true) ->
                                    HseStaticData.videos.find { it.id == "vid_premiers_secours" }
                                training.title.contains("Carbone", ignoreCase = true) || training.title.contains("Environnement", ignoreCase = true) ->
                                    HseStaticData.videos.find { it.id == "vid_decarbonation" }
                                else -> HseStaticData.videos.first()
                            } ?: HseStaticData.videos.first()
                            viewingVideo = videoMatch
                        },
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
                    )
                }
            }
        }

        // Cinema Video Player Dialog
        viewingVideo?.let { vid ->
            HseVideoPlayerDialog(
                video = vid,
                onDismiss = { viewingVideo = null }
            )
        }

        // Image & Plan Lightbox Dialog
        viewingPoster?.let { poster ->
            HseImageViewerDialog(
                image = poster,
                onDismiss = { viewingPoster = null }
            )
        }
    }
}

// -------------------------------------------------------------
// Card: Événement & Campagne (Pic 10)
// -------------------------------------------------------------
@Composable
fun EventCardItem(
    event: EventEntity,
    onViewPoster: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("event_card_${event.id}"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = HSECardBg)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Tag & Status Row (Pic 10)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = HSEFieldBg,
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = event.tag,
                        color = HSETextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                Surface(
                    color = if (event.status == "En cours") HSEBluePrimary.copy(alpha = 0.2f) else HSEGreenSafe.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = event.status,
                        color = if (event.status == "En cours") HSEBlueLight else HSEGreenSafe,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Title
            Text(
                text = event.title,
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Date with Calendar Icon
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.CalendarToday,
                    contentDescription = null,
                    tint = HSETextMuted,
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = event.dateText,
                    color = HSETextMuted,
                    fontSize = 12.sp
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Description
            Text(
                text = event.description,
                color = HSETextSecondary,
                fontSize = 12.sp,
                lineHeight = 16.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedButton(
                onClick = onViewPoster,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = HSEBlueLight),
                border = androidx.compose.foundation.BorderStroke(1.dp, HSEBlueLight.copy(alpha = 0.4f)),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                modifier = Modifier.fillMaxWidth().height(32.dp)
            ) {
                Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Voir l'affiche de la campagne", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// -------------------------------------------------------------
// Card: Formation (Pic 11)
// -------------------------------------------------------------
@Composable
fun TrainingCardItem(
    training: TrainingEntity,
    onToggleEnrollment: () -> Unit,
    onWatchVideo: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("training_card_${training.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = HSECardBg)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Date Badge (Day + Month) (Pic 11)
            Box(
                modifier = Modifier
                    .width(48.dp)
                    .height(54.dp)
                    .background(HSECardElevated, RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = training.dayNumber,
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = training.monthShort,
                        color = HSETextMuted,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Details
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = training.title,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = "Formateur : ${training.instructor}",
                    color = HSETextSecondary,
                    fontSize = 11.sp
                )

                Spacer(modifier = Modifier.height(2.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = HSETextMuted,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${training.site} • ${training.durationHours}h",
                        color = HSETextMuted,
                        fontSize = 11.sp
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "${training.enrolledCount} / ${training.maxCapacity} inscrits",
                    color = HSETextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Watch Video Teaser Button
                OutlinedButton(
                    onClick = onWatchVideo,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = HSEBlueLight),
                    border = androidx.compose.foundation.BorderStroke(1.dp, HSEBlueLight.copy(alpha = 0.4f)),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    modifier = Modifier.fillMaxWidth().height(32.dp)
                ) {
                    Icon(Icons.Default.PlayCircle, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Module vidéo préparatoire", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Action Button: S'inscrire / Inscrit (Pic 11)
                Button(
                    onClick = onToggleEnrollment,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (training.isEnrolled) HSEGreenSafe else HSEBluePrimary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(34.dp)
                        .testTag("btn_enroll_${training.id}")
                ) {
                    if (training.isEnrolled) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Inscrit(e) — Annuler", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Text("S'inscrire à la formation", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

