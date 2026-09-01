package com.example.ui.screens.profile

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
import androidx.compose.material.icons.automirrored.filled.Logout
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
import com.example.data.entity.NotificationEntity
import com.example.data.entity.SuccessStoryEntity
import com.example.data.entity.SurveyEntity
import com.example.data.entity.UserEntity
import com.example.ui.components.HseImageViewerDialog
import com.example.ui.components.HseSuccessStoryMapper
import com.example.ui.components.HseTestimonialReaderDialog
import com.example.ui.components.HseVideoPlayerDialog
import com.example.ui.screens.admin.AdminAttestationsScreen
import com.example.ui.screens.admin.AdminContentScreen
import com.example.ui.screens.admin.AdminUsersScreen
import com.example.ui.theme.*
import com.example.ui.viewmodel.HseViewModel

@Composable
fun ProfileScreen(
    viewModel: HseViewModel,
    modifier: Modifier = Modifier
) {
    val user by viewModel.user.collectAsState()
    val authState by viewModel.authState.collectAsState()
    val activeUser = user?.takeIf { it.email.isNotBlank() } ?: authState.currentUser
    val unreadCount by viewModel.unreadNotificationsCount.collectAsState()
    val isNotificationsOpen by viewModel.isNotificationsScreenOpen.collectAsState()
    val isSurveysOpen by viewModel.isSurveysScreenOpen.collectAsState()
    val isStoriesOpen by viewModel.isSuccessStoriesScreenOpen.collectAsState()
    val isLogoutOpen by viewModel.isLogoutDialogOpen.collectAsState()

    val isAdminContentOpen by viewModel.isAdminContentScreenOpen.collectAsState()
    val isAdminUsersOpen by viewModel.isAdminUsersScreenOpen.collectAsState()
    val isAdminAttestationsOpen by viewModel.isAdminAttestationsScreenOpen.collectAsState()

    // Sub-screens routing (Restricted to Admins)
    if (isAdminContentOpen && activeUser.isAdmin) {
        AdminContentScreen(
            viewModel = viewModel,
            onBack = { viewModel.isAdminContentScreenOpen.value = false }
        )
        return
    }

    if (isAdminUsersOpen && activeUser.isAdmin) {
        AdminUsersScreen(
            viewModel = viewModel,
            onBack = { viewModel.isAdminUsersScreenOpen.value = false }
        )
        return
    }

    if (isAdminAttestationsOpen && activeUser.isAdmin) {
        AdminAttestationsScreen(
            viewModel = viewModel,
            onBack = { viewModel.isAdminAttestationsScreenOpen.value = false }
        )
        return
    }

    if (isNotificationsOpen) {
        NotificationsCenterScreen(
            viewModel = viewModel,
            onBack = { viewModel.isNotificationsScreenOpen.value = false }
        )
        return
    }

    if (isSurveysOpen) {
        SurveysScreen(
            viewModel = viewModel,
            onBack = { viewModel.isSurveysScreenOpen.value = false }
        )
        return
    }

    if (isStoriesOpen) {
        SuccessStoriesScreen(
            viewModel = viewModel,
            onBack = { viewModel.isSuccessStoriesScreenOpen.value = false }
        )
        return
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
            // Hero Header (Compact & Responsive)
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
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Profil",
                                color = Color.White,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Paramètres du compte et informations",
                                color = Color.White.copy(alpha = 0.85f),
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }

            // User Info Card (Pic 12)
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp)
                        .testTag("user_profile_card"),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = HSECardBg)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Big Avatar
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .background(HSEBluePrimary, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = activeUser?.avatarInitials?.takeIf { it.isNotBlank() } ?: "U",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column {
                            Text(
                                text = activeUser?.name?.takeIf { it.isNotBlank() } ?: "Utilisateur",
                                color = Color.White,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = "${activeUser?.role?.takeIf { it.isNotBlank() } ?: "Utilisateur"} • ${activeUser?.site?.takeIf { it.isNotBlank() } ?: "Localisation"}",
                                color = HSETextSecondary,
                                fontSize = 12.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = activeUser?.email ?: "",
                                color = HSETextMuted,
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (!activeUser?.phone.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Phone,
                                        contentDescription = null,
                                        tint = HSEBlueLight,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = activeUser?.phone ?: "",
                                        color = HSEBlueLight,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
            }

            // Section: Préférences (Pic 12)
            item {
                Text(
                    text = "Préférences",
                    color = HSETextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
                )
            }

            // Push notifications toggle
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = HSECardBg)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .background(HSEBluePrimary.copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Notifications, contentDescription = null, tint = HSEBlueLight, modifier = Modifier.size(20.dp))
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Notifications push", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                                Text("Alertes urgentes et rappels", color = HSETextSecondary, fontSize = 11.sp)
                            }
                        }

                        Switch(
                            checked = user?.pushNotificationsEnabled ?: true,
                            onCheckedChange = { viewModel.togglePushNotifications(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = HSEBluePrimary,
                                uncheckedThumbColor = HSETextSecondary,
                                uncheckedTrackColor = HSEFieldBg
                            )
                        )
                    }
                }
            }

            // Centre de notifications link
            item {
                ProfileNavCard(
                    title = "Centre de notifications",
                    subtitle = if (unreadCount > 0) "$unreadCount non lue${if (unreadCount > 1) "s" else ""}" else "Toutes les alertes",
                    icon = Icons.Default.MarkChatUnread,
                    badgeText = if (unreadCount > 0) unreadCount.toString() else null,
                    badgeColor = HSERedUrgent,
                    onClick = { viewModel.isNotificationsScreenOpen.value = true },
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
                    testTag = "nav_notifications_center"
                )
                Spacer(modifier = Modifier.height(14.dp))
            }

            // Section: Engagement & Retours (Pic 12)
            item {
                Text(
                    text = "Engagement & Retours",
                    color = HSETextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
                )
            }

            item {
                ProfileNavCard(
                    title = "Sondages & Enquêtes",
                    subtitle = "Sondage Satisfaction",
                    icon = Icons.Default.Poll,
                    onClick = { viewModel.isSurveysScreenOpen.value = true },
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
                    testTag = "nav_surveys_screen"
                )
            }

            item {
                ProfileNavCard(
                    title = "Success Stories",
                    subtitle = "Partage d'expériences",
                    icon = Icons.Default.EmojiEvents,
                    onClick = { viewModel.isSuccessStoriesScreenOpen.value = true },
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
                    testTag = "nav_success_stories"
                )
                Spacer(modifier = Modifier.height(14.dp))
            }

            // Section: Administration & Gestion HSE (Visible ONLY for Administrators)
            if (activeUser.isAdmin) {
                item {
                    Text(
                        text = "Administration",
                        color = HSETextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
                    )
                }

                item {
                    ProfileNavCard(
                        title = "Gestion des Contenus",
                        subtitle = "Documents, Vidéos, Photos, Liens",
                        icon = Icons.Default.FolderSpecial,
                        badgeText = "Admin",
                        badgeColor = Color(0xFF047857),
                        onClick = { viewModel.isAdminContentScreenOpen.value = true },
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
                        testTag = "nav_admin_contents"
                    )
                }

                item {
                    ProfileNavCard(
                        title = "Gestion des Utilisateurs",
                        subtitle = "Comptes, Rôles, Validations, Accès",
                        icon = Icons.Default.AdminPanelSettings,
                        badgeText = "Admin",
                        badgeColor = Color(0xFFDC2626),
                        onClick = { viewModel.isAdminUsersScreenOpen.value = true },
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
                        testTag = "nav_admin_users"
                    )
                }

                item {
                    ProfileNavCard(
                        title = "Registre des Attestations",
                        subtitle = "Lectures certifiées & conformité",
                        icon = Icons.Default.VerifiedUser,
                        badgeText = "Certifs",
                        badgeColor = Color(0xFF0284C7),
                        onClick = { viewModel.isAdminAttestationsScreenOpen.value = true },
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
                        testTag = "nav_admin_attestations"
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                }
            }

            // Section: Services Cloud & Sécurité
            item {
                Text(
                    text = "Services Cloud & Sécurité",
                    color = HSETextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
                )
            }

            item {
                val firebaseStatus = remember { viewModel.getFirebaseProjectStatus() }
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = HSECardBg),
                    border = BorderStroke(1.dp, HSECardBorder)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(Color(0xFF047857).copy(alpha = 0.15f), RoundedCornerShape(8.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CloudDone,
                                        contentDescription = null,
                                        tint = Color(0xFF10B981),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "Firebase & Firestore",
                                        color = Color.White,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Projet : ${firebaseStatus["projectId"] ?: "abeissa-a2e66"}",
                                        color = HSETextSecondary,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                            Surface(
                                color = Color(0xFF10B981).copy(alpha = 0.15f),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.3f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .background(Color(0xFF10B981), CircleShape)
                                    )
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text(
                                        text = "Actif",
                                        color = Color(0xFF10B981),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = HSECardBorder)
                        Spacer(modifier = Modifier.height(10.dp))

                        // Row 1: Auth
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Authentification :", color = HSETextSecondary, fontSize = 11.sp)
                            Text("Google Sign-In & Auth", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                        }
                        Spacer(modifier = Modifier.height(6.dp))

                        // Row 2: Database
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Base de données :", color = HSETextSecondary, fontSize = 11.sp)
                            Text("Cloud Firestore (Temps Réel)", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                        }
                        Spacer(modifier = Modifier.height(6.dp))

                        // Row 3: Storage
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Serveur Fichiers :", color = HSETextSecondary, fontSize = 11.sp)
                            Text("Firebase Storage Multi-devices", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
            }

            // Section: À propos & Support
            item {
                Text(
                    text = "À propos & Support",
                    color = HSETextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
                )
            }

            item {
                ProfileNavCard(
                    title = "Version de l'application",
                    subtitle = "1.0.0 (Build 2026.08) • Production",
                    icon = Icons.Default.Info,
                    onClick = { /* info */ },
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
                    showArrow = false
                )
            }

            item {
                ProfileNavCard(
                    title = "Assistance technique & Support",
                    subtitle = "support@example.com",
                    icon = Icons.Default.SupportAgent,
                    onClick = { /* support */ },
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Logout Button (Pic 12)
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp)
                        .clickable { viewModel.isLogoutDialogOpen.value = true }
                        .testTag("logout_button"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = HSERedUrgent.copy(alpha = 0.15f)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, HSERedUrgent.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, tint = HSERedUrgent, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Se déconnecter",
                            color = HSERedUrgent,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                }
            }
        }

        // Logout Confirmation Dialog (Pic 16)
        if (isLogoutOpen) {
            AlertDialog(
                onDismissRequest = { viewModel.isLogoutDialogOpen.value = false },
                modifier = Modifier.testTag("logout_confirmation_dialog"),
                containerColor = HSECardBg,
                icon = {
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .background(HSERedUrgent.copy(alpha = 0.2f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, tint = HSERedUrgent, modifier = Modifier.size(24.dp))
                    }
                },
                title = {
                    Text("Déconnexion", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                },
                text = {
                    Text(
                        "Êtes-vous sûr de vouloir vous déconnecter de l'application ?",
                        color = HSETextSecondary,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                },
                confirmButton = {
                    Button(
                        onClick = { viewModel.logout() },
                        colors = ButtonDefaults.buttonColors(containerColor = HSERedUrgent),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("confirm_logout_button")
                    ) {
                        Text("Se déconnecter", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { viewModel.isLogoutDialogOpen.value = false },
                        modifier = Modifier.testTag("cancel_logout_button")
                    ) {
                        Text("Annuler", color = HSETextSecondary)
                    }
                }
            )
        }
    }
}

@Composable
fun ProfileNavCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    badgeText: String? = null,
    badgeColor: Color = HSEBluePrimary,
    showArrow: Boolean = true,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    testTag: String = ""
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag(testTag),
        shape = RoundedCornerShape(16.dp),
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
                        .size(34.dp)
                        .background(HSEBluePrimary.copy(alpha = 0.15f), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = icon, contentDescription = null, tint = HSEBlueLight, modifier = Modifier.size(18.dp))
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(text = title, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(text = subtitle, color = HSETextSecondary, fontSize = 11.sp)
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                badgeText?.let {
                    Surface(
                        color = badgeColor,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text(
                            text = it,
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }

                if (showArrow) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = HSETextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

// -------------------------------------------------------------
// Screen: Centre de Notifications (Pic 13)
// -------------------------------------------------------------
@Composable
fun NotificationsCenterScreen(
    viewModel: HseViewModel,
    onBack: () -> Unit
) {
    val notifications by viewModel.notifications.collectAsState()
    var filterType by remember { mutableStateOf("Toutes") }

    val filteredList = if (filterType == "Toutes") {
        notifications
    } else {
        notifications.filter { it.type == "Urgence" }
    }

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
                                    Color(0xFF1E3A8A),
                                    Color(0xFF0F1A30)
                                )
                            )
                        )
                        .padding(horizontal = 20.dp, vertical = 20.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(Color.White.copy(alpha = 0.2f), CircleShape)
                                    .clickable(onClick = onBack),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour", tint = Color.White)
                            }

                            TextButton(onClick = { viewModel.markAllNotificationsAsRead() }) {
                                Text("Tout marquer lu", color = HSEBlueLight, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        Text("Centre de notifications", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                        Text("Consultez les alertes et mises à jour", color = Color.White.copy(alpha = 0.85f), fontSize = 12.sp)
                    }
                }
            }

            // Filter Chips
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        color = if (filterType == "Toutes") HSEBluePrimary else HSECardBg,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.clickable { filterType = "Toutes" }
                    ) {
                        Text(
                            text = "Toutes",
                            color = if (filterType == "Toutes") Color.White else HSETextSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                        )
                    }

                    Surface(
                        color = if (filterType == "Urgences") HSERedUrgent else HSECardBg,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.clickable { filterType = "Urgences" }
                    ) {
                        Text(
                            text = "Urgences uniquement",
                            color = if (filterType == "Urgences") Color.White else HSETextSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            if (filteredList.isEmpty()) {
                item {
                    Text(
                        text = "Aucune notification à afficher.",
                        color = HSETextMuted,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(20.dp)
                    )
                }
            } else {
                items(filteredList, key = { it.id }) { notif ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 5.dp)
                            .clickable { viewModel.markNotificationAsRead(notif.id) },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (!notif.isRead) HSECardElevated else HSECardBg
                        ),
                        border = if (!notif.isRead) androidx.compose.foundation.BorderStroke(1.dp, HSEBlueLight.copy(alpha = 0.3f)) else null
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .background(
                                        when (notif.type) {
                                            "Urgence" -> HSERedUrgent.copy(alpha = 0.2f)
                                            "Formation" -> HSEBluePrimary.copy(alpha = 0.2f)
                                            else -> HSEGreenSafe.copy(alpha = 0.2f)
                                        },
                                        CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (notif.type == "Urgence") Icons.Default.Warning else Icons.Default.Info,
                                    contentDescription = null,
                                    tint = if (notif.type == "Urgence") HSERedUrgent else HSEBlueLight,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = notif.title,
                                        color = Color.White,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    if (!notif.isRead) {
                                        Box(modifier = Modifier.size(8.dp).background(HSEBlueLight, CircleShape))
                                    }
                                }

                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = notif.message,
                                    color = HSETextSecondary,
                                    fontSize = 12.sp,
                                    lineHeight = 16.sp
                                )

                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = notif.timestampText,
                                    color = HSETextMuted,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// Screen: Sondages & Enquêtes (Pic 14)
// -------------------------------------------------------------
@Composable
fun SurveysScreen(
    viewModel: HseViewModel,
    onBack: () -> Unit
) {
    val surveys by viewModel.surveys.collectAsState()
    var activeSurveyToAnswer by remember { mutableStateOf<SurveyEntity?>(null) }

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
                                    Color(0xFF1E3A8A),
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
                        Text("Sondages & Enquêtes", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                        Text("Partagez vos retours pour améliorer l'expérience globale", color = Color.White.copy(alpha = 0.85f), fontSize = 12.sp)
                    }
                }
            }

            items(surveys, key = { it.id }) { survey ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 6.dp)
                        .testTag("survey_card_${survey.id}"),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = HSECardBg)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
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
                                    text = survey.quarter,
                                    color = HSETextSecondary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }

                            Surface(
                                color = if (survey.isCompleted) HSEGreenSafe.copy(alpha = 0.2f) else HSEBluePrimary.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = if (survey.isCompleted) "Complété ✓" else "Actif",
                                    color = if (survey.isCompleted) HSEGreenSafe else HSEBlueLight,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = survey.title,
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = survey.description,
                            color = HSETextSecondary,
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        if (!survey.isCompleted) {
                            Button(
                                onClick = { activeSurveyToAnswer = survey },
                                colors = ButtonDefaults.buttonColors(containerColor = HSEBluePrimary),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth().height(38.dp)
                            ) {
                                Text("Répondre au sondage", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        } else {
                            OutlinedButton(
                                onClick = { activeSurveyToAnswer = survey },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = HSEGreenSafe),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth().height(38.dp)
                            ) {
                                Text("Voir mes réponses", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }

        // Active Survey Form Dialog
        activeSurveyToAnswer?.let { survey ->
            var q1Rating by remember { mutableStateOf(5) }
            var q2Answer by remember { mutableStateOf("Oui, tout à fait") }
            var comments by remember { mutableStateOf("") }

            AlertDialog(
                onDismissRequest = { activeSurveyToAnswer = null },
                containerColor = HSECardBg,
                title = {
                    Text(survey.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 420.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            "1. Comment évaluez-vous l'expérience globale de l'application ?",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            (1..5).forEach { star ->
                                IconButton(
                                    onClick = { q1Rating = star },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = if (star <= q1Rating) Icons.Default.Star else Icons.Default.StarBorder,
                                        contentDescription = null,
                                        tint = if (star <= q1Rating) HSEAmberElect else HSETextMuted
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            "2. Disposez-vous de toutes les fonctionnalités nécessaires ?",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        listOf("Oui, tout à fait", "Partiellement", "Non").forEach { opt ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { q2Answer = opt }
                                    .padding(vertical = 4.dp)
                            ) {
                                RadioButton(
                                    selected = q2Answer == opt,
                                    onClick = { q2Answer = opt },
                                    colors = RadioButtonDefaults.colors(selectedColor = HSEBluePrimary)
                                )
                                Text(opt, color = HSETextPrimary, fontSize = 12.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            "3. Vos suggestions d'amélioration :",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = comments,
                            onValueChange = { comments = it },
                            placeholder = { Text("Ex: Ajouter une fonctionnalité...", color = HSETextMuted) },
                            modifier = Modifier.fillMaxWidth().height(80.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = HSEFieldBg,
                                unfocusedContainerColor = HSEFieldBg,
                                focusedBorderColor = HSEBlueLight,
                                unfocusedBorderColor = HSECardBorder,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.submitSurvey(survey.id)
                            activeSurveyToAnswer = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = HSEBluePrimary)
                    ) {
                        Text("Soumettre mes réponses")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { activeSurveyToAnswer = null }) {
                        Text("Fermer", color = HSETextSecondary)
                    }
                }
            )
        }
    }
}

// -------------------------------------------------------------
// Screen: Success Stories (Pic 15 & 17)
// -------------------------------------------------------------
@Composable
fun SuccessStoriesScreen(
    viewModel: HseViewModel,
    onBack: () -> Unit
) {
    val stories by viewModel.successStories.collectAsState()
    var selectedStoryFilter by remember { mutableStateOf("Toutes") }
    var showShareModal by remember { mutableStateOf(false) }

    // Media and Testimonial Viewers State
    var selectedVideoStory by remember { mutableStateOf<SuccessStoryEntity?>(null) }
    var selectedImageStory by remember { mutableStateOf<SuccessStoryEntity?>(null) }
    var selectedTestimonialStory by remember { mutableStateOf<SuccessStoryEntity?>(null) }

    val filteredStories = if (selectedStoryFilter == "Toutes") {
        stories
    } else {
        stories.filter {
            when (selectedStoryFilter) {
                "Photos" -> it.type == "PHOTO"
                "Témoignages" -> it.type == "TEMOIGNAGE"
                "Vidéos" -> it.type == "VIDEO"
                else -> true
            }
        }
    }

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
            // Hero Banner (Pic 15)
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
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .background(Color.White.copy(alpha = 0.2f), CircleShape)
                                    .clickable(onClick = onBack),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour", tint = Color.White, modifier = Modifier.size(18.dp))
                            }

                            Button(
                                onClick = { showShareModal = true },
                                colors = ButtonDefaults.buttonColors(containerColor = HSEBluePrimary),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.height(32.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Partager", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Success Stories", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text("Célébrons nos victoires, retours d'expérience et initiatives terrain", color = Color.White.copy(alpha = 0.85f), fontSize = 11.sp)
                    }
                }
            }

            // Filter Chips (Toutes, Photos, Vidéos, Témoignages) (Pic 15)
            item {
                val chips = listOf(
                    "Toutes" to stories.size,
                    "Photos" to stories.count { it.type == "PHOTO" },
                    "Vidéos" to stories.count { it.type == "VIDEO" },
                    "Témoignages" to stories.count { it.type == "TEMOIGNAGE" }
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    chips.forEach { (chipName, count) ->
                        val isSelected = selectedStoryFilter == chipName
                        Surface(
                            color = if (isSelected) HSEBluePrimary else HSECardBg,
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(
                                1.dp,
                                if (isSelected) HSEBlueLight else HSECardBorder
                            ),
                            modifier = Modifier.clickable { selectedStoryFilter = chipName }
                        ) {
                            Text(
                                text = "$chipName ($count)",
                                color = if (isSelected) Color.White else HSETextSecondary,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }

            // Stories Feed (Pic 15 & 17)
            items(filteredStories, key = { it.id }) { story ->
                SuccessStoryCardItem(
                    story = story,
                    onOpenDetail = {
                        when (story.type) {
                            "VIDEO" -> selectedVideoStory = story
                            "PHOTO" -> selectedImageStory = story
                            "TEMOIGNAGE" -> selectedTestimonialStory = story
                            else -> selectedTestimonialStory = story
                        }
                    },
                    onToggleLike = { viewModel.toggleStoryLike(story) },
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                )
            }
        }

        // Active Video Player Modal
        selectedVideoStory?.let { story ->
            val videoMedia = remember(story.id) { HseSuccessStoryMapper.toVideoMedia(story) }
            HseVideoPlayerDialog(
                video = videoMedia,
                onDismiss = { selectedVideoStory = null }
            )
        }

        // Active Image Lightbox Modal
        selectedImageStory?.let { story ->
            val imageMedia = remember(story.id) { HseSuccessStoryMapper.toImageMedia(story) }
            HseImageViewerDialog(
                image = imageMedia,
                onDismiss = { selectedImageStory = null }
            )
        }

        // Active Testimonial Reader Modal
        selectedTestimonialStory?.let { story ->
            HseTestimonialReaderDialog(
                story = story,
                onDismiss = { selectedTestimonialStory = null },
                onToggleLike = { viewModel.toggleStoryLike(it) }
            )
        }

        if (showShareModal) {
            AlertDialog(
                onDismissRequest = { showShareModal = false },
                containerColor = HSECardBg,
                title = { Text("Partager une initiative", color = Color.White, fontWeight = FontWeight.Bold) },
                text = {
                    Text(
                        "Bravo pour votre démarche proactive ! Les témoignages, vidéos et photos de bonnes pratiques partagées sur le terrain inspirent l'ensemble des utilisateurs.",
                        color = HSETextSecondary,
                        fontSize = 13.sp
                    )
                },
                confirmButton = {
                    Button(
                        onClick = { showShareModal = false },
                        colors = ButtonDefaults.buttonColors(containerColor = HSEBluePrimary)
                    ) {
                        Text("Fermer")
                    }
                }
            )
        }
    }
}

@Composable
fun SuccessStoryCardItem(
    story: SuccessStoryEntity,
    onOpenDetail: () -> Unit,
    onToggleLike: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("story_card_${story.id}")
            .clickable(onClick = onOpenDetail),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = HSECardBg),
        border = BorderStroke(1.dp, HSECardBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Author & Type Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .background(
                                when (story.type) {
                                    "VIDEO" -> Color(0xFFDC2626)
                                    "PHOTO" -> Color(0xFF059669)
                                    else -> HSEBlueDark
                                },
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = story.authorName.take(2).uppercase(),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(story.authorName, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Text("${story.authorRole} • ${story.site}", color = HSETextMuted, fontSize = 11.sp)
                    }
                }

                // Type Badge
                Surface(
                    color = when (story.type) {
                        "VIDEO" -> Color(0xFF7F1D1D)
                        "PHOTO" -> Color(0xFF064E3B)
                        else -> Color(0xFF1E3A8A)
                    },
                    shape = RoundedCornerShape(6.dp),
                    border = BorderStroke(
                        1.dp,
                        when (story.type) {
                            "VIDEO" -> Color(0xFFEF4444)
                            "PHOTO" -> Color(0xFF10B981)
                            else -> Color(0xFF3B82F6)
                        }
                    )
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                    ) {
                        Icon(
                            imageVector = when (story.type) {
                                "VIDEO" -> Icons.Default.PlayCircle
                                "PHOTO" -> Icons.Default.PhotoCamera
                                else -> Icons.Default.MenuBook
                            },
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = when (story.type) {
                                "VIDEO" -> "VIDÉO"
                                "PHOTO" -> "PHOTO HD"
                                else -> "TÉMOIGNAGE"
                            },
                            color = Color.White,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Visual Media Thumbnail Preview Box
            Surface(
                color = Color(0xFF0F172A),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color(0xFF1E293B)),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onOpenDetail)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                ) {
                    when (story.type) {
                        "VIDEO" -> {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(46.dp)
                                        .background(Color(0xFFDC2626).copy(alpha = 0.2f), CircleShape)
                                        .border(1.5.dp, Color(0xFFEF4444), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = "Lire la vidéo",
                                        tint = Color(0xFFEF4444),
                                        modifier = Modifier.size(26.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Surface(
                                            color = Color(0xFFDC2626).copy(alpha = 0.2f),
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Text("Vidéo 4K", color = Color(0xFFFCA5A5), fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp))
                                        }
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Lecteur Haute Définition", color = HSETextMuted, fontSize = 10.sp)
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text("Appuyez pour lancer la vidéo sécurisée", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                        "PHOTO" -> {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(46.dp)
                                        .background(Color(0xFF059669).copy(alpha = 0.2f), CircleShape)
                                        .border(1.5.dp, Color(0xFF10B981), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ZoomIn,
                                        contentDescription = "Inspecter la photo",
                                        tint = Color(0xFF10B981),
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Surface(
                                            color = Color(0xFF059669).copy(alpha = 0.2f),
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Text("Photo HD 4K", color = Color(0xFF6EE7B7), fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp))
                                        }
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Annotations", color = HSETextMuted, fontSize = 10.sp)
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text("Appuyez pour inspecter en mode zoom haute fidélité", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                        else -> {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("❝", color = HSEBluePrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Témoignage REX & Conseils", color = Color(0xFF93C5FD), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Consultez le récit complet, les étapes d'action et les bénéfices.",
                                    color = HSETextSecondary,
                                    fontSize = 11.sp,
                                    lineHeight = 16.sp
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Title
            Text(
                text = story.title,
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Description
            Text(
                text = story.description,
                color = HSETextSecondary,
                fontSize = 12.sp,
                lineHeight = 17.sp,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Dedicated Open Action Button
            Button(
                onClick = onOpenDetail,
                colors = ButtonDefaults.buttonColors(
                    containerColor = when (story.type) {
                        "VIDEO" -> Color(0xFFDC2626)
                        "PHOTO" -> Color(0xFF059669)
                        else -> HSEBluePrimary
                    }
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(38.dp)
            ) {
                Icon(
                    imageVector = when (story.type) {
                        "VIDEO" -> Icons.Default.PlayArrow
                        "PHOTO" -> Icons.Default.Image
                        else -> Icons.Default.MenuBook
                    },
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = Color.White
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = when (story.type) {
                        "VIDEO" -> "Visionner la vidéo (HD)"
                        "PHOTO" -> "Inspecter la photo HD"
                        else -> "Lire le témoignage complet"
                    },
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.weight(1f))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = Color.White.copy(alpha = 0.8f)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Likes & Security Info Footer
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(onClick = onToggleLike)
                        .padding(horizontal = 6.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = if (story.isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "J'aime",
                        tint = if (story.isLiked) HSERedUrgent else HSETextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = "${story.likesCount} J'aime",
                        color = if (story.isLiked) HSERedUrgent else HSETextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // Internal Security Lock Tag
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Protégé",
                        tint = Color(0xFF60A5FA),
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Protégé • Consultation interne",
                        color = HSETextMuted,
                        fontSize = 10.sp
                    )
                }
            }
        }
    }
}

