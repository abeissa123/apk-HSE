package com.example.ui.screens.dashboard

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entity.IncidentEntity
import com.example.ui.components.HseTopBar
import com.example.ui.components.MediaPermissionBanner
import com.example.ui.components.SiteWeatherPill
import com.example.ui.theme.*
import com.example.ui.viewmodel.HseViewModel
import com.example.ui.viewmodel.MainTab
import com.example.util.LocationHelper

@Composable
fun DashboardScreen(
    viewModel: HseViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val user by viewModel.user.collectAsState()
    val authState by viewModel.authState.collectAsState()
    val activeUser = user?.takeIf { it.email.isNotBlank() } ?: authState.currentUser
    val realtimeWeather by viewModel.realtimeWeather.collectAsState()
    val incidents by viewModel.incidents.collectAsState()
    val pendingRisks by viewModel.pendingRisksCount.collectAsState()
    val unreadCount by viewModel.unreadNotificationsCount.collectAsState()
    val selectedSite by viewModel.selectedSite.collectAsState()
    val networkStatus by viewModel.networkStatus.collectAsState()
    val isGpsWeatherActive by viewModel.isGpsWeatherActive.collectAsState()
    val assignedSite = activeUser?.site?.takeIf { it.isNotBlank() } ?: selectedSite.takeIf { it.isNotBlank() && it != "Tous sites" } ?: "Site de Figuil"

    // Location Permission launcher
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
        if (fineGranted || coarseGranted) {
            viewModel.refreshRealtimeWeatherWithLocation(context)
        } else {
            viewModel.refreshRealtimeWeather(assignedSite)
        }
    }

    LaunchedEffect(Unit) {
        if (LocationHelper.hasLocationPermission(context)) {
            viewModel.refreshRealtimeWeatherWithLocation(context)
        } else {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    var showIncidentDetail by remember { mutableStateOf<IncidentEntity?>(null) }
    var showAllIncidentsModal by remember { mutableStateOf(false) }

    // Dialog state controllers
    val isReportIncidentOpen by viewModel.isReportIncidentOpen.collectAsState()
    val isSosSheetOpen by viewModel.isSosSheetOpen.collectAsState()
    val isSafetyAuditOpen by viewModel.isSafetyAuditOpen.collectAsState()
    val isChecklistEpiOpen by viewModel.isChecklistEpiOpen.collectAsState()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(HSEDarkBg)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 80.dp),
            contentPadding = PaddingValues(vertical = 12.dp)
        ) {
            // 1. Top Bar
            item {
                HseTopBar(
                    userName = activeUser?.name?.takeIf { it.isNotBlank() } ?: "Collaborateur",
                    avatarInitials = activeUser?.avatarInitials?.takeIf { it.isNotBlank() } ?: "HSE",
                    unreadNotifications = unreadCount,
                    onNotificationsClick = { viewModel.isNotificationsScreenOpen.value = true },
                    onProfileClick = { viewModel.setTab(MainTab.PROFIL) }
                )
            }

            // Network Connectivity status badge if offline or mobile
            if (!networkStatus.isConnected) {
                item {
                    Surface(
                        color = Color(0xFFE65100).copy(alpha = 0.2f),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFF9800)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudOff,
                                contentDescription = null,
                                tint = Color(0xFFFFB74D),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Mode Hors-ligne — Synchronisation locale active",
                                color = Color(0xFFFFE082),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            // 2. Weather & Site Pill (GPS Live Weather + Assigned Site)
            item {
                SiteWeatherPill(
                    assignedSite = if (isGpsWeatherActive && realtimeWeather.siteName.isNotBlank()) realtimeWeather.siteName else assignedSite,
                    weatherData = realtimeWeather,
                    onRefreshWeather = {
                        if (LocationHelper.hasLocationPermission(context)) {
                            viewModel.refreshRealtimeWeatherWithLocation(context)
                        } else {
                            locationPermissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                )
                            )
                        }
                    }
                )
                Spacer(modifier = Modifier.height(10.dp))
            }

            // Mandatory Media & Documents Permission Banner
            item {
                MediaPermissionBanner()
            }

            // 3. Actions Rapides Header
            item {
                Text(
                    text = "Actions rapides",
                    color = HSETextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
                )
            }

            // 4. Actions Rapides Grid (2x2)
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Action 1: Signaler un incident
                        QuickActionCard(
                            title = "Signaler un\nincident",
                            icon = Icons.Default.Shield,
                            iconTint = HSEBlueLight,
                            iconBg = HSEBluePrimary.copy(alpha = 0.15f),
                            cardBg = HSECardBg,
                            modifier = Modifier.weight(1f),
                            onClick = { viewModel.isReportIncidentOpen.value = true },
                            testTag = "action_report_incident"
                        )

                        // Action 2: Appel SOS Urgence (Red highlight)
                        QuickActionCard(
                            title = "Appel SOS\nUrgence",
                            icon = Icons.Default.Call,
                            iconTint = Color.White,
                            iconBg = Color.White.copy(alpha = 0.25f),
                            cardBg = HSERedUrgent,
                            modifier = Modifier.weight(1f),
                            onClick = { viewModel.isSosSheetOpen.value = true },
                            testTag = "action_sos_call",
                            isUrgent = true
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Action 3: Audit de sécurité
                        QuickActionCard(
                            title = "Audit de sécurité",
                            icon = Icons.Default.AssignmentTurnedIn,
                            iconTint = HSEGreenSafe,
                            iconBg = HSEGreenSafe.copy(alpha = 0.15f),
                            cardBg = HSECardBg,
                            modifier = Modifier.weight(1f),
                            onClick = { viewModel.isSafetyAuditOpen.value = true },
                            testTag = "action_safety_audit"
                        )

                        // Action 4: Checklist EPI
                        QuickActionCard(
                            title = "Checklist EPI",
                            icon = Icons.Default.Engineering,
                            iconTint = HSEBlueLight,
                            iconBg = HSEBluePrimary.copy(alpha = 0.15f),
                            cardBg = HSECardBg,
                            modifier = Modifier.weight(1f),
                            onClick = { viewModel.isChecklistEpiOpen.value = true },
                            testTag = "action_checklist_epi"
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // IA Copilot Hero Interactive Banner
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .clickable { viewModel.openHseIa() }
                        .testTag("dashboard_hse_ia_banner"),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF0F2238)
                    ),
                    border = BorderStroke(1.dp, Color(0xFF0284C7).copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        listOf(Color(0xFF0284C7), Color(0xFF059669), Color(0xFF10B981))
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.AutoAwesome,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Assistant IA Polyvalent",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    color = Color(0xFF059669).copy(alpha = 0.25f),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = "AI 2.5",
                                        color = Color(0xFF34D399),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 9.sp,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Réponses à vos questions, aide à la rédaction, analyse et discussion.",
                                color = HSETextSecondary,
                                fontSize = 11.5.sp,
                                maxLines = 2,
                                lineHeight = 16.sp
                            )
                        }

                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Ouvrir IA",
                            tint = Color(0xFF38BDF8),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }

            // 5. Indicateurs de sécurité Header
            item {
                Text(
                    text = "Indicateurs de sécurité",

                    color = HSETextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
                )
            }

            // 6. Indicateurs Row (Jours sans accident & Risques en attente)
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // KPI 1: Jours sans accident
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = HSECardBg),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(HSEGreenSafe.copy(alpha = 0.15f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = HSEGreenSafe,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Jours sans accident",
                                color = HSETextSecondary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "47",
                                color = HSEGreenSafe,
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // KPI 2: Risques en attente
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = HSECardBg),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(HSERedUrgent.copy(alpha = 0.15f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = HSERedUrgent,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Risques en attente",
                                color = HSETextSecondary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Surface(
                                color = HSERedUrgent,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "$pendingRisks",
                                    color = Color.White,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // 7. KPI 3: Formations complétées
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = HSECardBg)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(HSEBluePrimary.copy(alpha = 0.15f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.TrendingUp,
                                    contentDescription = null,
                                    tint = HSEBlueLight,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Formations complétées",
                                    color = HSETextSecondary,
                                    fontSize = 12.sp
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "18 / 24",
                                    color = HSEBlueLight,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Progress Bar (75%)
                        Box(
                            modifier = Modifier
                                .width(90.dp)
                                .height(8.dp)
                                .background(HSECardElevated, RoundedCornerShape(4.dp))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(0.75f)
                                    .background(HSEBluePrimary, RoundedCornerShape(4.dp))
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }

            // 8. Activité Récente Header with "Tout voir"
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Activité récente",
                        color = HSETextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Tout voir",
                        color = HSEBlueLight,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .clickable { showAllIncidentsModal = true }
                            .testTag("see_all_activity_button")
                    )
                }
            }

            // 9. Incidents list
            val displayIncidents = incidents.take(3)
            if (displayIncidents.isEmpty()) {
                item {
                    Text(
                        text = "Aucune activité récente enregistrée.",
                        color = HSETextMuted,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                    )
                }
            } else {
                items(displayIncidents, key = { it.id }) { incident ->
                    IncidentCardItem(
                        incident = incident,
                        onClick = { showIncidentDetail = incident },
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
                    )
                }
            }
        }

        // Incident Detail Dialog
        showIncidentDetail?.let { incident ->
            AlertDialog(
                onDismissRequest = { showIncidentDetail = null },
                containerColor = HSECardBg,
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(HSERedUrgent.copy(alpha = 0.2f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = HSERedUrgent, modifier = Modifier.size(18.dp))
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(incident.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Site / Lieu : ${incident.location}", color = HSEBlueLight, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        Text("Gravité : ${incident.severity} | Catégorie : ${incident.category}", color = HSETextSecondary, fontSize = 12.sp)
                        Text("Signalé par : ${incident.reportedBy} (${incident.timestampText})", color = HSETextMuted, fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        Surface(
                            color = HSEFieldBg,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = incident.description,
                                color = HSETextPrimary,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = { showIncidentDetail = null },
                        colors = ButtonDefaults.buttonColors(containerColor = HSEBluePrimary)
                    ) {
                        Text("Fermer")
                    }
                }
            )
        }

        // All Incidents Modal
        if (showAllIncidentsModal) {
            AlertDialog(
                onDismissRequest = { showAllIncidentsModal = false },
                containerColor = HSECardBg,
                title = { Text("Tous les Incidents & Activités", color = Color.White, fontWeight = FontWeight.Bold) },
                text = {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 400.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(incidents, key = { it.id }) { incident ->
                            IncidentCardItem(
                                incident = incident,
                                onClick = {
                                    showIncidentDetail = incident
                                    showAllIncidentsModal = false
                                }
                            )
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showAllIncidentsModal = false }) {
                        Text("Fermer", color = HSEBlueLight)
                    }
                }
            )
        }

        // Sheet: Report Incident
        if (isReportIncidentOpen) {
            ReportIncidentSheet(
                currentSite = selectedSite,
                onDismiss = { viewModel.isReportIncidentOpen.value = false },
                onSubmit = { title, cat, sev, site, loc, desc ->
                    viewModel.reportIncident(title, cat, sev, site, loc, desc)
                }
            )
        }

        // Sheet: SOS Emergency
        if (isSosSheetOpen) {
            SosEmergencySheet(
                onDismiss = { viewModel.isSosSheetOpen.value = false }
            )
        }

        // Dialog: Safety Audit
        if (isSafetyAuditOpen) {
            SafetyAuditDialog(
                currentSite = selectedSite,
                onDismiss = { viewModel.isSafetyAuditOpen.value = false },
                onSave = { site, score ->
                    viewModel.saveSafetyAudit(site, score)
                }
            )
        }

        // Dialog: Checklist EPI
        if (isChecklistEpiOpen) {
            ChecklistEpiDialog(
                onDismiss = { viewModel.isChecklistEpiOpen.value = false }
            )
        }
    }
}

@Composable
fun QuickActionCard(
    title: String,
    icon: ImageVector,
    iconTint: Color,
    iconBg: Color,
    cardBg: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    testTag: String,
    isUrgent: Boolean = false
) {
    Card(
        modifier = modifier
            .height(96.dp)
            .clickable(onClick = onClick)
            .testTag(testTag),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isUrgent) 5.dp else 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .background(iconBg, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(18.dp)
                )
            }

            Text(
                text = title,
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 16.sp
            )
        }
    }
}

@Composable
fun IncidentCardItem(
    incident: IncidentEntity,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = HSECardBg)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .background(HSERedUrgent.copy(alpha = 0.15f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = HSERedUrgent,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = incident.title,
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = incident.location,
                        color = HSETextSecondary,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = incident.timestampText,
                    color = HSETextMuted,
                    fontSize = 11.sp
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = HSETextMuted,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

// -------------------------------------------------------------
// Sheet: Signaler un incident
// -------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportIncidentSheet(
    currentSite: String,
    onDismiss: () -> Unit,
    onSubmit: (title: String, category: String, severity: String, site: String, location: String, description: String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Sécurité") }
    var severity by remember { mutableStateOf("Majeur") }
    var site by remember { mutableStateOf(if (currentSite == "Tous sites") "Site Figuil" else currentSite) }
    var location by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    val categories = listOf("Sécurité", "Santé", "Environnement", "Presqu'accident")
    val severities = listOf("Critique", "Majeur", "Mineur")
    val sites = listOf("Site Figuil", "Site Bonabéri", "Site Nomayos")

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = HSECardBg,
        dragHandle = { BottomSheetDefaults.DragHandle(color = HSECardBorder) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 12.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = "Signaler un incident",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Alertez immédiatement les équipes de votre site",
                color = HSETextSecondary,
                fontSize = 12.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Titre
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Titre de l'incident (ex: Fuite d'huile)", color = HSETextMuted) },
                modifier = Modifier.fillMaxWidth().testTag("incident_title_input"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = HSEFieldBg,
                    unfocusedContainerColor = HSEFieldBg,
                    focusedBorderColor = HSEBlueLight,
                    unfocusedBorderColor = HSECardBorder,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                shape = RoundedCornerShape(10.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Site selector
            Text("Site concerné", color = HSETextSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                sites.forEach { s ->
                    val isSelected = site == s
                    Surface(
                        color = if (isSelected) HSEBluePrimary else HSEFieldBg,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { site = s }
                    ) {
                        Text(
                            text = s.replace("Site ", ""),
                            color = if (isSelected) Color.White else HSETextSecondary,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            modifier = Modifier.padding(vertical = 8.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Location
            OutlinedTextField(
                value = location,
                onValueChange = { location = it },
                label = { Text("Lieu exact (ex: Atelier Maintenance Broyeur 2)", color = HSETextMuted) },
                modifier = Modifier.fillMaxWidth().testTag("incident_location_input"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = HSEFieldBg,
                    unfocusedContainerColor = HSEFieldBg,
                    focusedBorderColor = HSEBlueLight,
                    unfocusedBorderColor = HSECardBorder,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                shape = RoundedCornerShape(10.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Severity selector
            Text("Niveau de gravité", color = HSETextSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                severities.forEach { sev ->
                    val isSelected = severity == sev
                    val badgeColor = when (sev) {
                        "Critique" -> HSERedUrgent
                        "Majeur" -> HSEOrangeAlert
                        else -> HSEGreenSafe
                    }
                    Surface(
                        color = if (isSelected) badgeColor else HSEFieldBg,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { severity = sev }
                    ) {
                        Text(
                            text = sev,
                            color = if (isSelected) Color.White else HSETextSecondary,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            modifier = Modifier.padding(vertical = 8.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Category selector
            Text("Catégorie", color = HSETextSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                categories.forEach { cat ->
                    val isSelected = category == cat
                    Surface(
                        color = if (isSelected) HSEBluePrimary else HSEFieldBg,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { category = cat }
                    ) {
                        Text(
                            text = cat,
                            color = if (isSelected) Color.White else HSETextSecondary,
                            fontSize = 10.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            modifier = Modifier.padding(vertical = 8.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Description
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description des faits et mesures immédiates prises", color = HSETextMuted) },
                modifier = Modifier.fillMaxWidth().height(100.dp).testTag("incident_description_input"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = HSEFieldBg,
                    unfocusedContainerColor = HSEFieldBg,
                    focusedBorderColor = HSEBlueLight,
                    unfocusedBorderColor = HSECardBorder,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                shape = RoundedCornerShape(10.dp)
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Submit Button
            Button(
                onClick = {
                    if (title.isNotBlank() && location.isNotBlank()) {
                        onSubmit(title, category, severity, site, "$location • $site", description)
                    }
                },
                enabled = title.isNotBlank() && location.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .testTag("submit_incident_button"),
                colors = ButtonDefaults.buttonColors(containerColor = HSEBluePrimary),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Transmettre le signalement", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

// -------------------------------------------------------------
// Sheet: SOS Urgence Call
// -------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SosEmergencySheet(
    onDismiss: () -> Unit
) {
    var callDialedMessage by remember { mutableStateOf<String?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = HSECardBg,
        dragHandle = { BottomSheetDefaults.DragHandle(color = HSECardBorder) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 12.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(HSERedUrgent, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Call, contentDescription = null, tint = Color.White)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text("Appel SOS & Numéros d'Urgence", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text("Lignes d'assistance prioritaires 24h/24", color = HSETextSecondary, fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            callDialedMessage?.let { msg ->
                Surface(
                    color = HSEGreenSafe.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                ) {
                    Text(
                        text = msg,
                        color = HSEGreenSafe,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            // Quick Call Cards
            EmergencyCallButton(
                title = "Urgence — Infirmerie Interne",
                phone = "Poste 100",
                desc = "Médecin du travail et premiers secours",
                color = HSERedUrgent,
                onClick = { callDialedMessage = "Appel d'urgence en cours vers l'Infirmerie (Poste 100)..." }
            )

            Spacer(modifier = Modifier.height(10.dp))

            EmergencyCallButton(
                title = "Poste de Garde & Sécurité Usine",
                phone = "Poste 112",
                desc = "Contrôle d'accès et alarme générale",
                color = HSEOrangeAlert,
                onClick = { callDialedMessage = "Appel d'urgence en cours vers le Poste de Garde (Poste 112)..." }
            )

            Spacer(modifier = Modifier.height(10.dp))

            EmergencyCallButton(
                title = "Sapeurs-Pompiers & Secours Externes",
                phone = "118 / +237 222 22 22 22",
                desc = "Intervention incendie et désincarcération",
                color = HSERedDark,
                onClick = { callDialedMessage = "Numérotation des Sapeurs-Pompiers (118)..." }
            )

            Spacer(modifier = Modifier.height(10.dp))

            EmergencyCallButton(
                title = "Responsable d'Astreinte",
                phone = "+237 699 00 00 01",
                desc = "Coordination des secours de crise",
                color = HSEBluePrimary,
                onClick = { callDialedMessage = "Appel vers le Responsable en cours..." }
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = HSEFieldBg),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Fermer", color = HSETextSecondary)
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun EmergencyCallButton(
    title: String,
    phone: String,
    desc: String,
    color: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = HSEFieldBg)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text(text = desc, color = HSETextMuted, fontSize = 11.sp)
            }

            Surface(
                color = color,
                shape = RoundedCornerShape(6.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Call, contentDescription = null, tint = Color.White, modifier = Modifier.size(13.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(phone, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
            }
        }
    }
}

// -------------------------------------------------------------
// Dialog: Safety Audit
// -------------------------------------------------------------
@Composable
fun SafetyAuditDialog(
    currentSite: String,
    onDismiss: () -> Unit,
    onSave: (site: String, score: Int) -> Unit
) {
    var checkPpe by remember { mutableStateOf(true) }
    var checkFire by remember { mutableStateOf(true) }
    var checkExits by remember { mutableStateOf(true) }
    var checkChems by remember { mutableStateOf(false) }
    var checkElec by remember { mutableStateOf(true) }

    val checkedCount = listOf(checkPpe, checkFire, checkExits, checkChems, checkElec).count { it }
    val score = (checkedCount * 100) / 5

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = HSECardBg,
        title = {
            Text("Audit Flash Sécurité Terrain", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text("Vérification de conformité 5 points sur $currentSite", color = HSETextSecondary, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(14.dp))

                AuditCheckItem("1. Port effectif des EPI complets", checkPpe) { checkPpe = it }
                AuditCheckItem("2. Extincteurs vérifiés et accessibles", checkFire) { checkFire = it }
                AuditCheckItem("3. Issues de secours 100% dégagées", checkExits) { checkExits = it }
                AuditCheckItem("4. Stockage produits chimiques conforme (bacs rétention)", checkChems) { checkChems = it }
                AuditCheckItem("5. Armoires électriques cadenassées & balisées", checkElec) { checkElec = it }

                Spacer(modifier = Modifier.height(14.dp))

                Surface(
                    color = if (score >= 80) HSEGreenSafe.copy(alpha = 0.2f) else HSEOrangeAlert.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Score de Conformité :", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text("$score %", color = if (score >= 80) HSEGreenSafe else HSEOrangeAlert, fontWeight = FontWeight.Black, fontSize = 18.sp)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(currentSite, score) },
                colors = ButtonDefaults.buttonColors(containerColor = HSEBluePrimary)
            ) {
                Text("Enregistrer l'audit")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annuler", color = HSETextSecondary)
            }
        }
    )
}

@Composable
private fun AuditCheckItem(text: String, isChecked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!isChecked) }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = isChecked,
            onCheckedChange = onCheckedChange,
            colors = CheckboxDefaults.colors(
                checkedColor = HSEBluePrimary,
                uncheckedColor = HSETextSecondary,
                checkmarkColor = Color.White
            )
        )
        Text(text = text, color = HSETextPrimary, fontSize = 12.sp)
    }
}

// -------------------------------------------------------------
// Dialog: Checklist EPI
// -------------------------------------------------------------
@Composable
fun ChecklistEpiDialog(
    onDismiss: () -> Unit
) {
    var ppeHelmet by remember { mutableStateOf(true) }
    var ppeShoes by remember { mutableStateOf(true) }
    var ppeGlasses by remember { mutableStateOf(true) }
    var ppeVest by remember { mutableStateOf(true) }
    var ppeGloves by remember { mutableStateOf(true) }
    var ppeEars by remember { mutableStateOf(false) }

    val allRequired = ppeHelmet && ppeShoes && ppeGlasses && ppeVest && ppeGloves

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = HSECardBg,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Engineering, contentDescription = null, tint = HSEBlueLight)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Checklist EPI Quotidienne", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
        },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text("Équipements de Protection Individuelle obligatoires avant prise de poste", color = HSETextSecondary, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(12.dp))

                AuditCheckItem("Casque de sécurité avec jugulaire", ppeHelmet) { ppeHelmet = it }
                AuditCheckItem("Chaussures de sécurité S3 coquées", ppeShoes) { ppeShoes = it }
                AuditCheckItem("Lunettes de protection anti-impact", ppeGlasses) { ppeGlasses = it }
                AuditCheckItem("Gilet haute visibilité fluo avec bandes", ppeVest) { ppeVest = it }
                AuditCheckItem("Gants de manutention adaptés", ppeGloves) { ppeGloves = it }
                AuditCheckItem("Bouchons / Casque anti-bruit (zones > 80dB)", ppeEars) { ppeEars = it }

                Spacer(modifier = Modifier.height(14.dp))

                Surface(
                    color = if (allRequired) HSEGreenSafe.copy(alpha = 0.2f) else HSERedUrgent.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (allRequired) "Conforme : Vous êtes autorisé(e) à entrer en zone industrielle." else "Non conforme : Complétez vos EPI obligatoires avant d'accéder au site.",
                        color = if (allRequired) HSEGreenSafe else HSERedUrgent,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = HSEBluePrimary)
            ) {
                Text("Valider la checklist")
            }
        }
    )
}
