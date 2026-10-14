package com.example.ui.screens.emergency

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entity.ProcedureStepEntity
import com.example.domain.EmergencyFicheMeta
import com.example.domain.HseImageMedia
import com.example.domain.HseStaticData
import com.example.domain.HseVideoMedia
import com.example.domain.SitePassagePlan
import com.example.ui.components.HseImageViewerDialog
import com.example.ui.components.HseVideoPlayerDialog
import com.example.ui.theme.*
import com.example.ui.viewmodel.HseViewModel

@Composable
fun EmergencyScreen(
    viewModel: HseViewModel,
    modifier: Modifier = Modifier
) {
    val activeProcedureId by viewModel.activeProcedureId.collectAsState()
    val activeSitePlanId by viewModel.activeSitePassageId.collectAsState()

    var viewingPlanImage by remember { mutableStateOf<HseImageMedia?>(null) }
    var viewingEmergencyVideo by remember { mutableStateOf<HseVideoMedia?>(null) }

    if (activeProcedureId != null) {
        val selectedFiche = HseStaticData.emergencyFiches.firstOrNull { it.id == activeProcedureId }
        if (selectedFiche != null) {
            FicheReflexeDetailScreen(
                fiche = selectedFiche,
                viewModel = viewModel,
                onBack = { viewModel.activeProcedureId.value = null },
                onWatchVideo = {
                    viewingEmergencyVideo = HseStaticData.videos.firstOrNull { it.id == "vid_premiers_secours" } ?: HseStaticData.videos.first()
                }
            )

            viewingEmergencyVideo?.let { vid ->
                HseVideoPlayerDialog(
                    video = vid,
                    onDismiss = { viewingEmergencyVideo = null }
                )
            }
            return
        }
    }

    if (activeSitePlanId != null) {
        val selectedPlan = HseStaticData.sitePassagePlans.firstOrNull { it.id == activeSitePlanId }
        if (selectedPlan != null) {
            SitePassageDetailScreen(
                plan = selectedPlan,
                onBack = { viewModel.activeSitePassageId.value = null },
                onInspectHdPlan = {
                    val matchingImage = when {
                        selectedPlan.id.contains("bonaberi", ignoreCase = true) -> HseStaticData.images.find { it.id == "img_plan_bonaberi" }
                        selectedPlan.id.contains("figuil", ignoreCase = true) -> HseStaticData.images.find { it.id == "img_plan_figuil" }
                        else -> HseStaticData.images.first()
                    } ?: HseStaticData.images.first()
                    viewingPlanImage = matchingImage
                }
            )

            viewingPlanImage?.let { img ->
                HseImageViewerDialog(
                    image = img,
                    onDismiss = { viewingPlanImage = null }
                )
            }
            return
        }
    }

    val sitePlans = HseStaticData.sitePassagePlans
    val fiches = HseStaticData.emergencyFiches


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
            // 1. Hero Red Banner (Pic 7)
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color(0xFFDC2626),
                                    Color(0xFF991B1B),
                                    Color(0xFF0F1A30)
                                )
                            )
                        )
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LocalFireDepartment,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Plan d'Urgence",
                                    color = Color.White,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Consignes, fiches réflexes et plans par site",
                                    color = Color.White,
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Urgent Call Box (Pic 7)
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.isSosSheetOpen.value = true }
                                .testTag("urgent_hse_infirmary_button"),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Call,
                                        contentDescription = null,
                                        tint = HSERedUrgent,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = "Urgence",
                                            color = HSERedUrgent,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "Poste 100 • Infirmerie",
                                            color = Color(0xFF334155),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }

                                Surface(
                                    color = HSERedUrgent,
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = "Appeler",
                                        color = Color.White,
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // IA Quick Emergency Guidance
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    viewModel.openHseIa("CHAT", "Procédure d'urgence et premiers secours : ")
                                }
                                .testTag("emergency_consult_hse_ia"),
                            color = Color(0xFF1E293B),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, Color(0xFF0284C7).copy(alpha = 0.5f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Filled.AutoAwesome,
                                        contentDescription = null,
                                        tint = Color(0xFF38BDF8),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Un doute ? Demandez à l'IA",
                                        color = Color(0xFFF1F5F9),
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = null,
                                    tint = Color(0xFF38BDF8),
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }
            }


            // 2. Plans de passage par site Section Header (Pic 7)
            item {
                Text(
                    text = "Plans de passage par site",
                    color = HSETextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
                )
            }

            // Site Passage Plan Cards
            items(sitePlans, key = { it.id }) { plan ->
                SitePassageCardItem(
                    plan = plan,
                    onClick = { viewModel.activeSitePassageId.value = plan.id },
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
                )
            }

            item {
                Spacer(modifier = Modifier.height(20.dp))
            }

            // 3. Fiches réflexes d'urgence Header (Pic 8)
            item {
                Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)) {
                    Text(
                        text = "Fiches réflexes d'urgence",
                        color = HSETextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Procédures à suivre en cas d'incident — consultez immédiatement",
                        color = HSETextSecondary,
                        fontSize = 12.sp
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            // 4. Grid of 6 Fiches réflexes (Pic 8: 2 columns)
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    for (i in fiches.indices step 2) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            FicheReflexeGridCard(
                                fiche = fiches[i],
                                onClick = { viewModel.activeProcedureId.value = fiches[i].id },
                                modifier = Modifier.weight(1f)
                            )
                            if (i + 1 < fiches.size) {
                                FicheReflexeGridCard(
                                    fiche = fiches[i + 1],
                                    onClick = { viewModel.activeProcedureId.value = fiches[i + 1].id },
                                    modifier = Modifier.weight(1f)
                                )
                            } else {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SitePassageCardItem(
    plan: SitePassagePlan,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("plan_card_${plan.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = HSECardBg)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(HSERedUrgent, RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = plan.title,
                        color = HSETextPrimary,
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = plan.description,
                        color = HSETextSecondary,
                        fontSize = 12.5.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = HSETextSecondary,
                modifier = Modifier.size(15.dp)
            )
        }
    }
}

@Composable
fun FicheReflexeGridCard(
    fiche: EmergencyFicheMeta,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .height(108.dp)
            .clickable(onClick = onClick)
            .testTag("fiche_card_${fiche.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = HSECardBg)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(11.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(Color(fiche.accentColorHex), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                val icon = when (fiche.iconType) {
                    "ambulance" -> Icons.Default.MedicalServices
                    "flask" -> Icons.Default.Science
                    "bolt" -> Icons.Default.FlashOn
                    "exit" -> Icons.Default.MeetingRoom
                    "fire" -> Icons.Default.LocalFireDepartment
                    else -> Icons.Default.AddBox
                }
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }

            Column {
                Text(
                    text = fiche.title,
                    color = HSETextPrimary,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 17.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = "${fiche.stepsCount} étapes",
                    color = HSETextSecondary,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

// -------------------------------------------------------------
// Screen: Fiche Réflexe Detail (Pic 9)
// -------------------------------------------------------------
@Composable
fun FicheReflexeDetailScreen(
    fiche: EmergencyFicheMeta,
    viewModel: HseViewModel,
    onBack: () -> Unit,
    onWatchVideo: () -> Unit = {}
) {
    val stepsFlow = remember(fiche.id) { viewModel.getProcedureSteps(fiche.id) }
    val steps by stepsFlow.collectAsState(initial = emptyList())
    val completedCount = steps.count { it.isCompleted }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(HSEDarkBg)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 80.dp)
        ) {
            // Orange / Accent Hero Banner (Pic 9)
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color(fiche.accentColorHex),
                                    Color(fiche.accentColorHex).copy(alpha = 0.85f),
                                    Color(0xFF0F1A30)
                                )
                            )
                        )
                        .padding(horizontal = 20.dp, vertical = 20.dp)
                ) {
                    Column {
                        // Back Arrow
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(Color.White.copy(alpha = 0.2f), CircleShape)
                                .clickable(onClick = onBack)
                                .testTag("fiche_back_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Retour",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Large Icon Box
                        Box(
                            modifier = Modifier
                                .size(54.dp)
                                .background(Color.White.copy(alpha = 0.25f), RoundedCornerShape(14.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            val icon = when (fiche.iconType) {
                                "ambulance" -> Icons.Default.MedicalServices
                                "flask" -> Icons.Default.Science
                                "bolt" -> Icons.Default.FlashOn
                                "exit" -> Icons.Default.MeetingRoom
                                "fire" -> Icons.Default.LocalFireDepartment
                                else -> Icons.Default.AddBox
                            }
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(30.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = fiche.title,
                            color = Color.White,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = fiche.subtitle,
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 13.sp
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Video Tutorial Launcher Button
                        Button(
                            onClick = onWatchVideo,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White.copy(alpha = 0.2f),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Icon(Icons.Default.PlayCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Voir le tutoriel vidéo des gestes", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }


            // Procedure Section Header (Pic 9)
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Procédure — ${steps.size} étapes",
                            color = HSETextPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "$completedCount / ${steps.size} complétées",
                            color = if (completedCount == steps.size && steps.isNotEmpty()) HSEGreenSafe else HSEBlueLight,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Touchez chaque étape pour la cocher",
                        color = HSETextSecondary,
                        fontSize = 12.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Progress indicator
                    LinearProgressIndicator(
                        progress = { if (steps.isEmpty()) 0f else completedCount.toFloat() / steps.size },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = Color(fiche.accentColorHex),
                        trackColor = HSECardBg,
                    )
                }
            }

            // Interactive Step Checkboxes (Pic 9)
            items(steps, key = { it.id }) { step ->
                ProcedureStepCardItem(
                    step = step,
                    accentColor = Color(fiche.accentColorHex),
                    onToggle = { viewModel.toggleStep(step.id, !step.isCompleted) },
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
                )
            }

            // Reset Procedure & SOS Call Buttons
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = { viewModel.resetProcedure(fiche.id) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = HSETextSecondary),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Réinitialiser")
                    }

                    Button(
                        onClick = { viewModel.isSosSheetOpen.value = true },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = HSERedUrgent),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Appel Urgence")
                    }
                }
            }
        }
    }
}

@Composable
fun ProcedureStepCardItem(
    step: ProcedureStepEntity,
    accentColor: Color,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .testTag("step_item_${step.stepNumber}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (step.isCompleted) HSECardElevated else HSECardBg
        ),
        border = if (step.isCompleted) androidx.compose.foundation.BorderStroke(1.dp, HSEGreenSafe.copy(alpha = 0.5f)) else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Radio / Checkbox Circle (Pic 9)
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .background(
                        if (step.isCompleted) HSEGreenSafe else Color.Transparent,
                        CircleShape
                    )
                    .border(
                        width = 2.dp,
                        color = if (step.isCompleted) HSEGreenSafe else HSETextMuted,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (step.isCompleted) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Complété",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Étape ${step.stepNumber}",
                    color = HSETextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = step.title,
                    color = if (step.isCompleted) Color.White else HSETextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    lineHeight = 18.sp
                )
            }
        }
    }
}

// -------------------------------------------------------------
// Screen: Site Passage Detail
// -------------------------------------------------------------
@Composable
fun SitePassageDetailScreen(
    plan: SitePassagePlan,
    onBack: () -> Unit,
    onInspectHdPlan: () -> Unit = {}
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(HSEDarkBg)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 80.dp)
        ) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color(0xFFDC2626),
                                    Color(0xFF0F1A30)
                                )
                            )
                        )
                        .padding(horizontal = 20.dp, vertical = 20.dp)
                ) {
                    Column {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(Color.White.copy(alpha = 0.2f), CircleShape)
                                .clickable(onClick = onBack),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour", tint = Color.White)
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        Text(plan.title, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Text(plan.description, color = Color.White.copy(alpha = 0.85f), fontSize = 12.sp)

                        Spacer(modifier = Modifier.height(14.dp))

                        Button(
                            onClick = onInspectHdPlan,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White.copy(alpha = 0.2f),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Icon(Icons.Default.ZoomIn, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Inspecter le plan officiel 4K (Lightbox)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }


            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Points de Rassemblement
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = HSECardBg)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Groups, contentDescription = null, tint = HSEGreenSafe)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Points de Rassemblement", color = HSETextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            plan.musterPoints.forEach { point ->
                                Text("• $point", color = HSETextPrimary, fontSize = 13.5.sp, modifier = Modifier.padding(vertical = 2.dp))
                            }
                        }
                    }

                    // Accès & Circulation
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = HSECardBg)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.DirectionsCar, contentDescription = null, tint = HSEBlueLight)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Accès & Vitesse Maximale (${plan.speedLimitKmH} km/h)", color = HSETextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            plan.accessPoints.forEach { acc ->
                                Text("• $acc", color = HSETextPrimary, fontSize = 13.5.sp, modifier = Modifier.padding(vertical = 2.dp))
                            }
                        }
                    }

                    // Zones à Haut Risque
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = HSECardBg)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Warning, contentDescription = null, tint = HSERedUrgent)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Zones d'Attention Prioritaire", color = HSETextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            plan.highRiskZones.forEach { zone ->
                                Text("• $zone", color = HSEOrangeAlert, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(vertical = 2.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}
