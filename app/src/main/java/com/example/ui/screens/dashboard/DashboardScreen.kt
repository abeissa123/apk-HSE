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
    val attestations by viewModel.allAttestations.collectAsState()
    val notifications by viewModel.notifications.collectAsState()
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

            // 4. Actions Rapides Grid (Gants & terrain: gros boutons contrastés)
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Action 1: Signaler un incident (Accent Orange sécurité #F57C00)
                    QuickActionCard(
                        title = "⚠ Signaler un\nincident",
                        icon = Icons.Default.Warning,
                        iconTint = Color.White,
                        iconBg = Color.White.copy(alpha = 0.25f),
                        cardBg = HSEOrangeAlert,
                        textColor = Color.White,
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.isReportIncidentOpen.value = true },
                        testTag = "action_report_incident",
                        isUrgent = true
                    )

                    // Action 2: Appel SOS Urgence (Rouge Danger/Urgence #D32F2F)
                    QuickActionCard(
                        title = "✖ Appel SOS\nUrgence",
                        icon = Icons.Default.Call,
                        iconTint = Color.White,
                        iconBg = Color.White.copy(alpha = 0.25f),
                        cardBg = HSERedUrgent,
                        textColor = Color.White,
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.isSosSheetOpen.value = true },
                        testTag = "action_sos_call",
                        isUrgent = true
                    )
                }
                Spacer(modifier = Modifier.height(20.dp))
            }

            // 8. Activités Récentes Header with "Tout voir" & Real-time Firebase Badge
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Activités récentes",
                            color = HSETextPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Synchronisées en temps réel sur Firebase pour tous les utilisateurs",
                            color = HSEGreenSafe,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
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

            // 9. User-reported Incidents & Real-Time User Activities (No Demos)
            val displayIncidents = incidents.take(5)
            val recentAttestations = attestations.take(3)
            val recentActivityNotifications = notifications
                .filter { !it.title.contains("Nouvel Incident", ignoreCase = true) }
                .take(3)

            if (displayIncidents.isEmpty() && recentAttestations.isEmpty() && recentActivityNotifications.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 8.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = HSECardBg),
                        border = BorderStroke(1.dp, HSECardBorder)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Sync,
                                contentDescription = null,
                                tint = HSEGreenSafe,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Aucune activité ou signalement pour le moment.",
                                color = HSETextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Les incidents signalés et les activités effectuées par les utilisateurs s'afficheront ici en temps réel sur tous les appareils via Firebase.",
                                color = HSETextSecondary,
                                fontSize = 12.sp,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                // 9a. Real-time User Reported Incidents
                items(displayIncidents, key = { "inc_${it.id}" }) { incident ->
                    IncidentCardItem(
                        incident = incident,
                        onClick = { showIncidentDetail = incident },
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
                    )
                }

                // 9b. Real-time Document Attestations by Users
                items(recentAttestations, key = { "att_${it.id}" }) { att ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 5.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = HSECardBg),
                        border = BorderStroke(1.dp, HSECardBorder)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .background(HSEGreenSafe.copy(alpha = 0.14f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Verified,
                                    contentDescription = null,
                                    tint = HSEGreenSafe,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "✔ Lecture certifiée : ${att.documentTitle}",
                                    color = HSETextPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "Par ${att.userName} (${att.userSite}) • ${att.timestampText}",
                                    color = HSETextSecondary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                // 9c. Other Real-time User Activities (Trainings, Audits, Stories, Account actions)
                items(recentActivityNotifications, key = { "act_${it.id}" }) { notif ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 5.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = HSECardBg),
                        border = BorderStroke(1.dp, HSECardBorder)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .background(HSEBluePrimary.copy(alpha = 0.14f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Bolt,
                                    contentDescription = null,
                                    tint = HSEBluePrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = notif.title,
                                    color = HSETextPrimary,
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = notif.message,
                                    color = HSETextSecondary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }

        // Incident Detail Dialog with Real-Time Status Update & Deletion
        showIncidentDetail?.let { incident ->
            val sevIcon = when (incident.severity) {
                "Critique" -> "✖"
                "Élevé", "Majeur" -> "⚠"
                "Moyen", "Modéré" -> "⚠"
                else -> "✔"
            }
            AlertDialog(
                onDismissRequest = { showIncidentDetail = null },
                containerColor = HSECardBg,
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(HSERedUrgent.copy(alpha = 0.15f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = HSERedUrgent, modifier = Modifier.size(18.dp))
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(incident.title, color = HSETextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Site / Lieu : ${incident.location}", color = HSEBlueLight, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        Text("Gravité : $sevIcon ${incident.severity} | Catégorie : ${incident.category}", color = HSETextSecondary, fontSize = 12.sp)
                        Text("Statut actuel : ${incident.status}", color = if (incident.status == "Résolu") HSEGreenSafe else HSEOrangeAlert, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text("Signalé par : ${incident.reportedBy} (${incident.timestampText})", color = HSETextMuted, fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        Surface(
                            color = HSEFieldBg,
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, HSECardBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = incident.description,
                                color = HSETextPrimary,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (incident.status != "Résolu") {
                                OutlinedButton(
                                    onClick = {
                                        viewModel.updateIncidentStatus(incident.id, "Résolu")
                                        showIncidentDetail = null
                                    },
                                    border = BorderStroke(1.dp, HSEGreenSafe),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = HSEGreenSafe),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("✔ Marquer Résolu", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            if (activeUser?.isAdmin == true || activeUser?.name == incident.reportedBy) {
                                OutlinedButton(
                                    onClick = {
                                        viewModel.deleteIncident(incident.id)
                                        showIncidentDetail = null
                                    },
                                    border = BorderStroke(1.dp, HSERedUrgent),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = HSERedUrgent),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Supprimer", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = { showIncidentDetail = null },
                        colors = ButtonDefaults.buttonColors(containerColor = HSEBluePrimary)
                    ) {
                        Text("Fermer", color = Color.White)
                    }
                }
            )
        }

        // All Incidents & User Activities Modal
        if (showAllIncidentsModal) {
            AlertDialog(
                onDismissRequest = { showAllIncidentsModal = false },
                containerColor = HSECardBg,
                title = { Text("Activités & Incidents Utilisateurs (Temps réel)", color = HSETextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp) },
                text = {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 420.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (incidents.isEmpty() && attestations.isEmpty()) {
                            item {
                                Text(
                                    text = "Aucune activité utilisateur enregistrée.",
                                    color = HSETextSecondary,
                                    fontSize = 13.sp,
                                    modifier = Modifier.padding(8.dp)
                                )
                            }
                        }
                        items(incidents, key = { "modal_inc_${it.id}" }) { incident ->
                            IncidentCardItem(
                                incident = incident,
                                onClick = {
                                    showIncidentDetail = incident
                                    showAllIncidentsModal = false
                                }
                            )
                        }
                        items(attestations, key = { "modal_att_${it.id}" }) { att ->
                            Surface(
                                color = HSEFieldBg,
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, HSECardBorder),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = "✔ Lecture certifiée : ${att.documentTitle}",
                                        color = HSETextPrimary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                    Text(
                                        text = "${att.userName} (${att.userSite}) • ${att.timestampText}",
                                        color = HSETextSecondary,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showAllIncidentsModal = false }) {
                        Text("Fermer", color = HSEBluePrimary, fontWeight = FontWeight.Bold)
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
    }
}

@Composable
fun QuickActionCard(
    title: String,
    icon: ImageVector,
    iconTint: Color,
    iconBg: Color,
    cardBg: Color,
    textColor: Color = Color.White,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    testTag: String,
    isUrgent: Boolean = false
) {
    Card(
        modifier = modifier
            .height(104.dp)
            .clickable(onClick = onClick)
            .testTag(testTag),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isUrgent) 5.dp else 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .background(iconBg, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(20.dp)
                )
            }

            Text(
                text = title,
                color = textColor,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 17.sp
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
    val sevColor = when {
        incident.severity.contains("Critique", ignoreCase = true) -> HSERedUrgent      // #D32F2F
        incident.severity.contains("Élevé", ignoreCase = true) || incident.severity.contains("Majeur", ignoreCase = true) -> HSEOrangeAlert // #F57C00
        incident.severity.contains("Moyen", ignoreCase = true) || incident.severity.contains("Modéré", ignoreCase = true) -> HSEAmberElect  // #FBC02D
        else -> HSEGreenSafe // Faible #2E7D32
    }
    val sevTextColor = if (sevColor == HSEAmberElect) HSETextPrimary else Color.White
    val sevSymbol = when (sevColor) {
        HSERedUrgent -> "✖"
        HSEOrangeAlert, HSEAmberElect -> "⚠"
        else -> "✔"
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = HSECardBg),
        border = BorderStroke(1.dp, HSECardBorder)
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
                        .background(sevColor.copy(alpha = 0.15f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = sevColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = incident.title,
                            color = HSETextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Surface(
                            color = sevColor,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "$sevSymbol ${incident.severity}",
                                color = sevTextColor,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = incident.location,
                        color = HSETextSecondary,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = incident.timestampText,
                    color = HSETextMuted,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
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
    var severity by remember { mutableStateOf("Élevé") }
    var site by remember { mutableStateOf(if (currentSite == "Tous sites") "Site Figuil" else currentSite) }
    var location by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    val categories = listOf("Sécurité", "Santé", "Environnement", "Presqu'accident")
    // Matrice de risques : Faible #2E7D32 → Moyen #FBC02D → Élevé #F57C00 → Critique #D32F2F
    val severities = listOf("Faible", "Moyen", "Élevé", "Critique")
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
                text = "⚠ Signaler un incident",
                color = HSETextPrimary,
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
                    focusedBorderColor = HSEBluePrimary,
                    unfocusedBorderColor = HSECardBorder,
                    focusedTextColor = HSETextPrimary,
                    unfocusedTextColor = HSETextPrimary
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
                        border = BorderStroke(1.dp, if (isSelected) HSEBluePrimary else HSECardBorder),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { site = s }
                    ) {
                        Text(
                            text = s.replace("Site ", ""),
                            color = if (isSelected) Color.White else HSETextPrimary,
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
                    focusedBorderColor = HSEBluePrimary,
                    unfocusedBorderColor = HSECardBorder,
                    focusedTextColor = HSETextPrimary,
                    unfocusedTextColor = HSETextPrimary
                ),
                shape = RoundedCornerShape(10.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Severity selector (Matrice de risques avec icônes et contraste optimal)
            Text("Niveau de gravité (Matrice de risques)", color = HSETextSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                severities.forEach { sev ->
                    val isSelected = severity == sev
                    val badgeColor = when (sev) {
                        "Critique" -> HSERedUrgent      // #D32F2F
                        "Élevé" -> HSEOrangeAlert       // #F57C00
                        "Moyen" -> HSEAmberElect        // #FBC02D
                        else -> HSEGreenSafe            // #2E7D32 Faible
                    }
                    val symbol = when (sev) {
                        "Critique" -> "✖"
                        "Élevé", "Moyen" -> "⚠"
                        else -> "✔"
                    }
                    // Texte noir sur jaune #FBC02D et blanc sur rouge, vert, orange
                    val selectedTextColor = if (sev == "Moyen") HSETextPrimary else Color.White
                    Surface(
                        color = if (isSelected) badgeColor else HSEFieldBg,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, if (isSelected) badgeColor else HSECardBorder),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { severity = sev }
                    ) {
                        Text(
                            text = "$symbol $sev",
                            color = if (isSelected) selectedTextColor else HSETextPrimary,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            modifier = Modifier.padding(vertical = 10.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Category selector (Correspondance par domaine)
            Text("Catégorie / Domaine", color = HSETextSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                categories.forEach { cat ->
                    val isSelected = category == cat
                    val domainColor = when (cat) {
                        "Santé" -> HSEHealthBlue
                        "Sécurité" -> HSEOrangeAlert
                        "Environnement" -> HSEEnvGreen
                        else -> HSEBluePrimary
                    }
                    Surface(
                        color = if (isSelected) domainColor else HSEFieldBg,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, if (isSelected) domainColor else HSECardBorder),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { category = cat }
                    ) {
                        Text(
                            text = cat,
                            color = if (isSelected) Color.White else HSETextPrimary,
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
                    focusedBorderColor = HSEBluePrimary,
                    unfocusedBorderColor = HSECardBorder,
                    focusedTextColor = HSETextPrimary,
                    unfocusedTextColor = HSETextPrimary
                ),
                shape = RoundedCornerShape(10.dp)
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Submit Button (Accent Orange sécurité #F57C00 - Gros bouton pour gants et terrain)
            Button(
                onClick = {
                    if (title.isNotBlank() && location.isNotBlank()) {
                        onSubmit(title, category, severity, site, "$location • $site", description)
                    }
                },
                enabled = title.isNotBlank() && location.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("submit_incident_button"),
                colors = ButtonDefaults.buttonColors(containerColor = HSEOrangeAlert),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("⚠ Transmettre le signalement", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
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
                    Text("✖ Appel SOS & Numéros d'Urgence", color = HSETextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text("Lignes d'assistance prioritaires 24h/24", color = HSETextSecondary, fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            callDialedMessage?.let { msg ->
                Surface(
                    color = HSEEnvBg,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, HSEGreenSafe),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                ) {
                    Text(
                        text = "✔ $msg",
                        color = HSEGreenSafe,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            // Quick Call Cards
            EmergencyCallButton(
                title = "✖ Urgence — Infirmerie Interne",
                phone = "Poste 100",
                desc = "Médecin du travail et premiers secours",
                color = HSERedUrgent,
                onClick = { callDialedMessage = "Appel d'urgence en cours vers l'Infirmerie (Poste 100)..." }
            )

            Spacer(modifier = Modifier.height(10.dp))

            EmergencyCallButton(
                title = "⚠ Poste de Garde & Sécurité Usine",
                phone = "Poste 112",
                desc = "Contrôle d'accès et alarme générale",
                color = HSEOrangeAlert,
                onClick = { callDialedMessage = "Appel d'urgence en cours vers le Poste de Garde (Poste 112)..." }
            )

            Spacer(modifier = Modifier.height(10.dp))

            EmergencyCallButton(
                title = "✖ Sapeurs-Pompiers & Secours Externes",
                phone = "118 / +237 222 22 22 22",
                desc = "Intervention incendie et désincarcération",
                color = HSERedUrgent,
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
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Text("Fermer", color = HSETextPrimary, fontWeight = FontWeight.Bold)
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
        colors = CardDefaults.cardColors(containerColor = HSEFieldBg),
        border = BorderStroke(1.dp, HSECardBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, color = HSETextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text(text = desc, color = HSETextSecondary, fontSize = 11.sp)
            }

            Surface(
                color = color,
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Call, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(phone, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }
    }
}
