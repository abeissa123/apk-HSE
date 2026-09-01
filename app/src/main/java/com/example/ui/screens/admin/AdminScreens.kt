package com.example.ui.screens.admin

import android.widget.Toast
import androidx.compose.runtime.LaunchedEffect
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.entity.ContentEntity
import com.example.data.entity.DocumentAttestationEntity
import com.example.data.entity.NotificationEntity
import com.example.data.entity.SuccessStoryEntity
import com.example.data.entity.SurveyEntity
import com.example.data.entity.UserEntity
import com.example.ui.components.MediaPermissionBanner
import com.example.ui.viewmodel.HseViewModel
import com.example.util.MediaStorageHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminContentScreen(
    viewModel: HseViewModel,
    onBack: () -> Unit
) {
    val contents by viewModel.allContents.collectAsState()
    val stories by viewModel.allSuccessStories.collectAsState()
    val surveys by viewModel.allSurveys.collectAsState()
    val notifications by viewModel.allNotifications.collectAsState()

    val authState by viewModel.authState.collectAsState()
    val userState by viewModel.user.collectAsState()
    val activeUser = userState?.takeIf { it.email.isNotBlank() } ?: authState.currentUser

    if (!activeUser.isAdmin) {
        AdminAccessRestrictedView(
            title = "Gestion des Contenus & Médias",
            onBack = onBack
        )
        return
    }

    // Tabs: 0 -> Medias & Docs, 1 -> Succes Stories, 2 -> Sondages & Enquêtes, 3 -> Centre de Notification
    var selectedTab by remember { mutableStateOf(0) }

    // State for Content
    var selectedTypeFilter by remember { mutableStateOf("Tous") }
    var selectedCategoryFilter by remember { mutableStateOf("Toutes catégories") }
    var contentSearchQuery by remember { mutableStateOf("") }
    val isAddEditContentOpen by viewModel.isAddEditContentOpen.collectAsState()
    val editingContent by viewModel.editingContent.collectAsState()
    val adminErrorMessage by viewModel.adminErrorMessage.collectAsState()

    val context = LocalContext.current
    LaunchedEffect(adminErrorMessage) {
        if (adminErrorMessage != null) {
            Toast.makeText(context, adminErrorMessage, Toast.LENGTH_LONG).show()
            viewModel.clearAdminError()
        }
    }

    // State for Story
    var isAddEditStoryOpen by remember { mutableStateOf(false) }
    var editingStory by remember { mutableStateOf<SuccessStoryEntity?>(null) }
    var storySearchQuery by remember { mutableStateOf("") }

    // State for Survey
    var isAddEditSurveyOpen by remember { mutableStateOf(false) }
    var editingSurvey by remember { mutableStateOf<SurveyEntity?>(null) }
    var surveySearchQuery by remember { mutableStateOf("") }

    // State for Notification
    var isSendNotificationOpen by remember { mutableStateOf(false) }

    // Confirmation dialog state
    var itemToDelete by remember { mutableStateOf<DeleteTarget?>(null) }

    val totalCount = contents.size
    val totalViews = contents.sumOf { it.viewsCount }
    val pdfCount = contents.count { it.type == "PDF" }
    val photoCount = contents.count { it.type == "PHOTO" }
    val videoCount = contents.count { it.type == "VIDEO" }
    val linkCount = contents.count { it.type == "LIEN" }

    val filteredContents = remember(contents, selectedTypeFilter, selectedCategoryFilter, contentSearchQuery) {
        contents.filter { item ->
            val matchType = selectedTypeFilter == "Tous" || item.type.equals(selectedTypeFilter, ignoreCase = true)
            val matchCat = selectedCategoryFilter == "Toutes catégories" || item.category.equals(selectedCategoryFilter, ignoreCase = true)
            val matchQuery = contentSearchQuery.isBlank() || item.title.contains(contentSearchQuery, ignoreCase = true) || item.description.contains(contentSearchQuery, ignoreCase = true)
            matchType && matchCat && matchQuery
        }
    }

    val filteredStories = remember(stories, storySearchQuery) {
        stories.filter { s ->
            storySearchQuery.isBlank() || s.title.contains(storySearchQuery, ignoreCase = true) || s.description.contains(storySearchQuery, ignoreCase = true) || s.authorName.contains(storySearchQuery, ignoreCase = true) || s.site.contains(storySearchQuery, ignoreCase = true)
        }
    }

    val filteredSurveys = remember(surveys, surveySearchQuery) {
        surveys.filter { sv ->
            surveySearchQuery.isBlank() || sv.title.contains(surveySearchQuery, ignoreCase = true) || sv.description.contains(surveySearchQuery, ignoreCase = true) || sv.quarter.contains(surveySearchQuery, ignoreCase = true)
        }
    }

    Scaffold(
        containerColor = Color(0xFF0F172A),
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF047857)) // Emerald Green header
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier
                                .size(36.dp)
                                .background(Color.White.copy(alpha = 0.15f), CircleShape)
                                .testTag("admin_content_back_btn")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Retour",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Gestion Multimédia & Contenus",
                                color = Color.White,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Synchronisation temps réel Firestore",
                                color = Color.White.copy(alpha = 0.85f),
                                fontSize = 11.sp
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .background(Color.White.copy(alpha = 0.2f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = "Admin",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Permissions Banner
            MediaPermissionBanner()

            // 4-Tab Segmented Selector
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color(0xFF1E293B),
                contentColor = Color.White,
                edgePadding = 0.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, Color(0xFF334155), RoundedCornerShape(12.dp))
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("📁 Médias (${contents.size})", fontSize = 12.sp, fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal) },
                    selectedContentColor = Color(0xFF10B981),
                    unselectedContentColor = Color(0xFF94A3B8)
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("🏆 Succès Stories (${stories.size})", fontSize = 12.sp, fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal) },
                    selectedContentColor = Color(0xFFF59E0B),
                    unselectedContentColor = Color(0xFF94A3B8)
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("📊 Sondages (${surveys.size})", fontSize = 12.sp, fontWeight = if (selectedTab == 2) FontWeight.Bold else FontWeight.Normal) },
                    selectedContentColor = Color(0xFF38BDF8),
                    unselectedContentColor = Color(0xFF94A3B8)
                )
                Tab(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    text = { Text("🔔 Notifications (${notifications.size})", fontSize = 12.sp, fontWeight = if (selectedTab == 3) FontWeight.Bold else FontWeight.Normal) },
                    selectedContentColor = Color(0xFFEF4444),
                    unselectedContentColor = Color(0xFF94A3B8)
                )
            }

            // ==================== TAB 0: MÉDIAS & DOCUMENTS ====================
            if (selectedTab == 0) {
                // Overview & Stats
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Default.Layers, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("PUBLIÉS", color = Color(0xFF10B981), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("$totalCount", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Black)
                            Text("Fichiers multimédias", color = Color(0xFF64748B), fontSize = 10.sp)
                        }
                    }

                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Default.Visibility, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("CONSULTATIONS", color = Color(0xFF38BDF8), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(if (totalViews >= 1000) String.format("%.1fK", totalViews / 1000f) else "$totalViews", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Black)
                            Text("Vues cumulées", color = Color(0xFF64748B), fontSize = 10.sp)
                        }
                    }
                }

                // Small stats badges
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatBadgeItem(icon = Icons.Default.Description, count = pdfCount, label = "PDFs", color = Color(0xFFEF4444), modifier = Modifier.weight(1f))
                    StatBadgeItem(icon = Icons.Default.Image, count = photoCount, label = "Photos", color = Color(0xFF10B981), modifier = Modifier.weight(1f))
                    StatBadgeItem(icon = Icons.Default.Videocam, count = videoCount, label = "Vidéos", color = Color(0xFF3B82F6), modifier = Modifier.weight(1f))
                    StatBadgeItem(icon = Icons.Default.Link, count = linkCount, label = "Liens", color = Color(0xFFA855F7), modifier = Modifier.weight(1f))
                }

                // Add button
                Button(
                    onClick = {
                        viewModel.editingContent.value = null
                        viewModel.isAddEditContentOpen.value = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF047857)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .testTag("admin_add_content_btn")
                ) {
                    Icon(imageVector = Icons.Default.AddCircleOutline, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Ajouter un contenu multimédia", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }

                // Search & Filters
                OutlinedTextField(
                    value = contentSearchQuery,
                    onValueChange = { contentSearchQuery = it },
                    placeholder = { Text("Rechercher un média ou document...", color = Color(0xFF64748B), fontSize = 13.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFF64748B)) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedContainerColor = Color(0xFF1E293B),
                        unfocusedContainerColor = Color(0xFF1E293B),
                        focusedBorderColor = Color(0xFF10B981),
                        unfocusedBorderColor = Color(0xFF334155)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                // Type filter chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("Tous", "PDF", "Photo", "Vidéo", "Lien").forEach { type ->
                        val isSel = selectedTypeFilter == type
                        FilterChip(
                            selected = isSel,
                            onClick = { selectedTypeFilter = type },
                            label = { Text(type, fontSize = 12.sp, fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFF047857),
                                selectedLabelColor = Color.White,
                                containerColor = Color(0xFF1E293B),
                                labelColor = Color(0xFF94A3B8)
                            ),
                            border = BorderStroke(1.dp, if (isSel) Color(0xFF10B981) else Color(0xFF334155))
                        )
                    }
                }

                // List
                filteredContents.forEach { item ->
                    ContentCardItem(
                        content = item,
                        onEdit = {
                            viewModel.editingContent.value = item
                            viewModel.isAddEditContentOpen.value = true
                        },
                        onDelete = {
                            itemToDelete = DeleteTarget(
                                id = item.id,
                                title = item.title,
                                typeLabel = "Contenu Multimédia",
                                onConfirmDelete = { viewModel.deleteContent(item.id) }
                            )
                        },
                        onOpen = { viewModel.openContentReader(item) }
                    )
                }

                if (filteredContents.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp), contentAlignment = Alignment.Center) {
                        Text("Aucun contenu multimédia trouvé", color = Color(0xFF64748B), fontSize = 13.sp)
                    }
                }
            }

            // ==================== TAB 1: SUCCÈS STORIES ====================
            if (selectedTab == 1) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("BONNES PRATIQUES & SUCCÈS DU TERRAIN", color = Color(0xFF94A3B8), fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    Surface(color = Color(0xFFF59E0B).copy(alpha = 0.15f), shape = RoundedCornerShape(8.dp)) {
                        Text("${stories.size} publiées", color = Color(0xFFF59E0B), fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                    }
                }

                Button(
                    onClick = {
                        editingStory = null
                        isAddEditStoryOpen = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .testTag("admin_add_story_btn")
                ) {
                    Icon(imageVector = Icons.Default.EmojiEvents, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Ajouter une Succès Story", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }

                OutlinedTextField(
                    value = storySearchQuery,
                    onValueChange = { storySearchQuery = it },
                    placeholder = { Text("Rechercher par titre, auteur ou site...", color = Color(0xFF64748B), fontSize = 13.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFF64748B)) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedContainerColor = Color(0xFF1E293B),
                        unfocusedContainerColor = Color(0xFF1E293B),
                        focusedBorderColor = Color(0xFFF59E0B),
                        unfocusedBorderColor = Color(0xFF334155)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                filteredStories.forEach { story ->
                    StoryAdminCard(
                        story = story,
                        onEdit = {
                            editingStory = story
                            isAddEditStoryOpen = true
                        },
                        onDelete = {
                            itemToDelete = DeleteTarget(
                                id = story.id,
                                title = story.title,
                                typeLabel = "Succès Story",
                                onConfirmDelete = { viewModel.deleteSuccessStory(story.id) }
                            )
                        }
                    )
                }

                if (filteredStories.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp), contentAlignment = Alignment.Center) {
                        Text("Aucune Succès Story trouvée", color = Color(0xFF64748B), fontSize = 13.sp)
                    }
                }
            }

            // ==================== TAB 2: SONDAGES & ENQUÊTES ====================
            if (selectedTab == 2) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("CONSULTATIONS DU PERSONNEL & ENQUÊTES", color = Color(0xFF94A3B8), fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    Surface(color = Color(0xFF38BDF8).copy(alpha = 0.15f), shape = RoundedCornerShape(8.dp)) {
                        Text("${surveys.size} sondage(s)", color = Color(0xFF38BDF8), fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                    }
                }

                Button(
                    onClick = {
                        editingSurvey = null
                        isAddEditSurveyOpen = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .testTag("admin_add_survey_btn")
                ) {
                    Icon(imageVector = Icons.Default.Poll, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Créer un Sondage / Enquête", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }

                OutlinedTextField(
                    value = surveySearchQuery,
                    onValueChange = { surveySearchQuery = it },
                    placeholder = { Text("Rechercher un sondage...", color = Color(0xFF64748B), fontSize = 13.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFF64748B)) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedContainerColor = Color(0xFF1E293B),
                        unfocusedContainerColor = Color(0xFF1E293B),
                        focusedBorderColor = Color(0xFF38BDF8),
                        unfocusedBorderColor = Color(0xFF334155)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                filteredSurveys.forEach { survey ->
                    SurveyAdminCard(
                        survey = survey,
                        onEdit = {
                            editingSurvey = survey
                            isAddEditSurveyOpen = true
                        },
                        onToggleActive = {
                            val newStatus = if (survey.status.equals("Actif", ignoreCase = true)) "Clôturé" else "Actif"
                            viewModel.saveSurvey(survey.copy(status = newStatus))
                        },
                        onDelete = {
                            itemToDelete = DeleteTarget(
                                id = survey.id,
                                title = survey.title,
                                typeLabel = "Sondage / Enquête",
                                onConfirmDelete = { viewModel.deleteSurvey(survey.id) }
                            )
                        }
                    )
                }

                if (filteredSurveys.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp), contentAlignment = Alignment.Center) {
                        Text("Aucun sondage enregistré", color = Color(0xFF64748B), fontSize = 13.sp)
                    }
                }
            }

            // ==================== TAB 3: CENTRE DE NOTIFICATIONS ====================
            if (selectedTab == 3) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("DIFFUSION DES ALERTES & NOTIFICATIONS FLASH", color = Color(0xFF94A3B8), fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    Surface(color = Color(0xFFEF4444).copy(alpha = 0.15f), shape = RoundedCornerShape(8.dp)) {
                        Text("${notifications.size} alertes", color = Color(0xFFEF4444), fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                    }
                }

                Button(
                    onClick = { isSendNotificationOpen = true },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .testTag("admin_send_notif_btn")
                ) {
                    Icon(imageVector = Icons.Default.Campaign, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Diffuser une Alerte / Notification Flash", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }

                notifications.forEach { notif ->
                    NotificationAdminCard(
                        notification = notif,
                        onDelete = {
                            itemToDelete = DeleteTarget(
                                id = notif.id.toString(),
                                title = notif.title,
                                typeLabel = "Notification / Alerte",
                                onConfirmDelete = { viewModel.deleteNotification(notif.id) }
                            )
                        }
                    )
                }

                if (notifications.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp), contentAlignment = Alignment.Center) {
                        Text("Aucune notification diffusée pour le moment", color = Color(0xFF64748B), fontSize = 13.sp)
                    }
                }
            }
        }
    }

    // ==================== CONFIRMATION DIALOG FOR DELETIONS ====================
    itemToDelete?.let { target ->
        AlertDialog(
            onDismissRequest = { itemToDelete = null },
            containerColor = Color(0xFF1E293B),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Warning, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Confirmer la suppression", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Text(
                    text = "Êtes-vous sûr de vouloir supprimer définitivement ce ${target.typeLabel} :\n\n« ${target.title} » ?\n\nCette action supprimera l'élément de la base de données Firestore et mettra à jour en direct les interfaces de tous les collaborateurs.",
                    color = Color(0xFFCBD5E1),
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val toExecute = target.onConfirmDelete
                        itemToDelete = null
                        toExecute()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Supprimer définitivement", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { itemToDelete = null },
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF94A3B8)),
                    border = BorderStroke(1.dp, Color(0xFF334155))
                ) {
                    Text("Annuler")
                }
            }
        )
    }

    // Dialogs
    if (isAddEditContentOpen) {
        AddEditContentDialog(
            content = editingContent,
            onDismiss = { viewModel.isAddEditContentOpen.value = false },
            onSave = { updated -> viewModel.saveContent(updated) }
        )
    }

    if (isAddEditStoryOpen) {
        AddEditSuccessStoryDialog(
            story = editingStory,
            onDismiss = { isAddEditStoryOpen = false },
            onSave = { updated ->
                viewModel.saveSuccessStory(updated)
                isAddEditStoryOpen = false
            }
        )
    }

    if (isAddEditSurveyOpen) {
        AddEditSurveyDialog(
            survey = editingSurvey,
            onDismiss = { isAddEditSurveyOpen = false },
            onSave = { updated ->
                viewModel.saveSurvey(updated)
                isAddEditSurveyOpen = false
            }
        )
    }

    if (isSendNotificationOpen) {
        SendNotificationDialog(
            onDismiss = { isSendNotificationOpen = false },
            onSend = { title, message, type, site, priority ->
                viewModel.broadcastNotification(title, message, type, site, priority)
                isSendNotificationOpen = false
            }
        )
    }
}

// Data holder for Delete confirmation
private data class DeleteTarget(
    val id: String,
    val title: String,
    val typeLabel: String,
    val onConfirmDelete: () -> Unit
)

@Composable
fun StoryAdminCard(
    story: SuccessStoryEntity,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color(0xFF334155)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(Color(0xFFF59E0B).copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = Icons.Default.EmojiEvents, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(24.dp))
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(text = story.title, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(text = "Par ${story.authorName} (${story.authorRole}) • ${story.site}", color = Color(0xFFFBBF24), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = story.description, color = Color(0xFF94A3B8), fontSize = 12.sp, maxLines = 3, overflow = TextOverflow.Ellipsis)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.ThumbUp, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "${story.likesCount} mention(s) j'aime  •  ${story.dateText}  •  ${story.type}", color = Color(0xFF64748B), fontSize = 11.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onEdit,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFF59E0B)),
                    border = BorderStroke(1.dp, Color(0xFFD97706).copy(alpha = 0.5f)),
                    modifier = Modifier.weight(1f).height(34.dp)
                ) {
                    Icon(imageVector = Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Modifier", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = onDelete,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF4444)),
                    border = BorderStroke(1.dp, Color(0xFFDC2626).copy(alpha = 0.5f)),
                    modifier = Modifier.weight(1f).height(34.dp)
                ) {
                    Icon(imageVector = Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Supprimer", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun SurveyAdminCard(
    survey: SurveyEntity,
    onEdit: () -> Unit,
    onToggleActive: () -> Unit,
    onDelete: () -> Unit
) {
    val isActive = survey.status.equals("Actif", ignoreCase = true)
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color(0xFF334155)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(Color(0xFF38BDF8).copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = Icons.Default.Poll, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(24.dp))
                }

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(color = Color(0xFF0284C7).copy(alpha = 0.2f), shape = RoundedCornerShape(4.dp)) {
                            Text(survey.quarter, color = Color(0xFF38BDF8), fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            color = if (isActive) Color(0xFF10B981).copy(alpha = 0.15f) else Color(0xFF64748B).copy(alpha = 0.15f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = if (isActive) "ACTIF" else "CLÔTURÉ",
                                color = if (isActive) Color(0xFF10B981) else Color(0xFF94A3B8),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = survey.title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(text = survey.description, color = Color(0xFF94A3B8), fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = "${survey.questionsCount} questions • ${if (survey.isCompleted) "Complété" else "En attente de réponses"}", color = Color(0xFF64748B), fontSize = 11.sp)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onToggleActive,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = if (isActive) Color(0xFFFBBF24) else Color(0xFF10B981)
                    ),
                    border = BorderStroke(1.dp, if (isActive) Color(0xFFD97706).copy(alpha = 0.5f) else Color(0xFF047857).copy(alpha = 0.5f)),
                    modifier = Modifier.weight(1f).height(34.dp)
                ) {
                    Text(if (isActive) "Clôturer" else "Réactiver", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = onEdit,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF38BDF8)),
                    border = BorderStroke(1.dp, Color(0xFF0284C7).copy(alpha = 0.5f)),
                    modifier = Modifier.weight(1f).height(34.dp)
                ) {
                    Text("Modifier", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = onDelete,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF4444)),
                    border = BorderStroke(1.dp, Color(0xFFDC2626).copy(alpha = 0.5f)),
                    modifier = Modifier.weight(1f).height(34.dp)
                ) {
                    Text("Supprimer", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun NotificationAdminCard(
    notification: NotificationEntity,
    onDelete: () -> Unit
) {
    val typeColor = when (notification.type.uppercase()) {
        "ALERTE", "URGENCE" -> Color(0xFFEF4444)
        "SUCCES", "SUCCÈS", "FORMATION" -> Color(0xFF10B981)
        "SONDAGE" -> Color(0xFF38BDF8)
        else -> Color(0xFFF59E0B)
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color(0xFF334155)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(typeColor.copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = Icons.Default.Campaign, contentDescription = null, tint = typeColor, modifier = Modifier.size(22.dp))
                }

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(color = typeColor.copy(alpha = 0.2f), shape = RoundedCornerShape(4.dp)) {
                            Text(notification.type.uppercase(), color = typeColor, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = notification.title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(text = notification.message, color = Color(0xFFCBD5E1), fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(text = notification.timestampText, color = Color(0xFF64748B), fontSize = 10.sp)
                }

                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(imageVector = Icons.Default.Delete, contentDescription = "Supprimer", tint = Color(0xFFF87171), modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
fun StatBadgeItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    count: Int,
    label: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = Color(0xFF1E293B),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Color(0xFF334155)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = "$count", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Text(text = label, color = Color(0xFF94A3B8), fontSize = 10.sp)
        }
    }
}

@Composable
fun ContentCardItem(
    content: ContentEntity,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onOpen: () -> Unit
) {
    val (typeColor, typeIcon, typeLabel) = when (content.type.uppercase()) {
        "PDF" -> Triple(Color(0xFFEF4444), Icons.Default.Description, "PDF")
        "PHOTO" -> Triple(Color(0xFF10B981), Icons.Default.Image, "IMG")
        "VIDEO" -> Triple(Color(0xFF3B82F6), Icons.Default.Videocam, "VID")
        "LIEN" -> Triple(Color(0xFFA855F7), Icons.Default.Link, "LIEN")
        else -> Triple(Color(0xFFF59E0B), Icons.Default.FormatQuote, "DOC")
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color(0xFF334155)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(typeColor.copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(imageVector = typeIcon, contentDescription = null, tint = typeColor, modifier = Modifier.size(20.dp))
                        Text(text = typeLabel, color = typeColor, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Surface(
                        color = Color(0xFFEF4444).copy(alpha = 0.15f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = content.category.uppercase(),
                            color = Color(0xFFEF4444),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = content.title,
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = content.description,
                        color = Color(0xFF94A3B8),
                        fontSize = 12.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Visibility, contentDescription = null, tint = Color(0xFF64748B), modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "${content.viewsCount} vues  •  ${content.dateText}", color = Color(0xFF64748B), fontSize = 11.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onOpen,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF38BDF8)),
                    border = BorderStroke(1.dp, Color(0xFF0284C7).copy(alpha = 0.5f)),
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp)
                ) {
                    val actionLabel = when (content.type.uppercase()) {
                        "PDF" -> "Lire"
                        "VIDEO" -> "Lire"
                        "PHOTO" -> "Aperçu"
                        "LIEN" -> "Ouvrir"
                        else -> "Voir"
                    }
                    Text(actionLabel, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = onEdit,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFCD34D)),
                    border = BorderStroke(1.dp, Color(0xFFD97706).copy(alpha = 0.5f)),
                    modifier = Modifier.height(36.dp)
                ) {
                    Icon(imageVector = Icons.Default.Edit, contentDescription = "Modifier", modifier = Modifier.size(14.dp))
                }

                OutlinedButton(
                    onClick = onDelete,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFF87171)),
                    border = BorderStroke(1.dp, Color(0xFFDC2626).copy(alpha = 0.5f)),
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp)
                ) {
                    Icon(imageVector = Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Supprimer", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditContentDialog(
    content: ContentEntity?,
    onDismiss: () -> Unit,
    onSave: (ContentEntity) -> Unit
) {
    val context = LocalContext.current
    var type by remember { mutableStateOf(content?.type ?: "PDF") }
    var title by remember { mutableStateOf(content?.title ?: "") }
    var description by remember { mutableStateOf(content?.description ?: "") }
    var category by remember { mutableStateOf(content?.category ?: "HSE") }
    var linkUrl by remember { mutableStateOf(content?.fileUrl ?: "") }
    var expandedCat by remember { mutableStateOf(false) }

    var selectedFileName by remember { mutableStateOf<String?>(null) }
    var selectedFileSize by remember { mutableStateOf<String?>(null) }
    var selectedUri by remember { mutableStateOf<Uri?>(null) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedUri = uri
            val importResult = MediaStorageHelper.importUriToAppStorage(context, uri)
            linkUrl = importResult.localFilePath.ifBlank { uri.toString() }
            selectedFileName = importResult.fileName
            selectedFileSize = importResult.fileSizeFormatted
            if (title.isBlank()) {
                title = importResult.fileName.substringBeforeLast(".")
            }
        }
    }

    val isEdit = content != null

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            color = Color(0xFF1E293B),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, Color(0xFF334155)),
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(vertical = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isEdit) "Modifier le contenu" else "Ajouter un contenu",
                        color = Color.White,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Fermer", tint = Color(0xFF94A3B8))
                    }
                }

                // Type Selector tabs
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        Triple("PDF", Color(0xFFEF4444), Icons.Default.Description),
                        Triple("Photo", Color(0xFF10B981), Icons.Default.Image),
                        Triple("Vidéo", Color(0xFF3B82F6), Icons.Default.Videocam),
                        Triple("Lien", Color(0xFFA855F7), Icons.Default.Link)
                    ).forEach { (t, c, ic) ->
                        val isSel = type.equals(t, ignoreCase = true)
                        Surface(
                            color = if (isSel) c.copy(alpha = 0.15f) else Color(0xFF0F172A),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, if (isSel) c else Color(0xFF334155)),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { type = t.uppercase() }
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(imageVector = ic, contentDescription = null, tint = if (isSel) c else Color(0xFF94A3B8), modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(text = t, color = if (isSel) c else Color(0xFF94A3B8), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // Title field
                Text("Titre du contenu", color = Color(0xFF94A3B8), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    placeholder = { Text("Titre du document / contenu", color = Color(0xFF64748B), fontSize = 13.sp) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedContainerColor = Color(0xFF0F172A),
                        unfocusedContainerColor = Color(0xFF0F172A),
                        focusedBorderColor = Color(0xFF10B981),
                        unfocusedBorderColor = Color(0xFF334155)
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth().testTag("content_title_input")
                )

                // Description field
                Text("Description", color = Color(0xFF94A3B8), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    placeholder = { Text("Description courte...", color = Color(0xFF64748B), fontSize = 13.sp) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedContainerColor = Color(0xFF0F172A),
                        unfocusedContainerColor = Color(0xFF0F172A),
                        focusedBorderColor = Color(0xFF10B981),
                        unfocusedBorderColor = Color(0xFF334155)
                    ),
                    minLines = 3,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth().testTag("content_desc_input")
                )

                // Category dropdown
                Text("Catégorie", color = Color(0xFF94A3B8), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                ExposedDropdownMenuBox(
                    expanded = expandedCat,
                    onExpandedChange = { expandedCat = it }
                ) {
                    OutlinedTextField(
                        value = category,
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedCat) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedContainerColor = Color(0xFF0F172A),
                            unfocusedContainerColor = Color(0xFF0F172A),
                            focusedBorderColor = Color(0xFF10B981),
                            unfocusedBorderColor = Color(0xFF334155)
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = expandedCat,
                        onDismissRequest = { expandedCat = false },
                        modifier = Modifier.background(Color(0xFF1E293B))
                    ) {
                        listOf("HSE", "Sécurité", "Santé", "Environnement", "Induction", "Qualité").forEach { catOption ->
                            DropdownMenuItem(
                                text = { Text(catOption, color = Color.White) },
                                onClick = {
                                    category = catOption
                                    expandedCat = false
                                }
                            )
                        }
                    }
                }

                if (type.uppercase() == "LIEN") {
                    Text("URL du lien / sondage", color = Color(0xFF94A3B8), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    OutlinedTextField(
                        value = linkUrl,
                        onValueChange = { linkUrl = it },
                        placeholder = { Text("https://...", color = Color(0xFF64748B), fontSize = 13.sp) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedContainerColor = Color(0xFF0F172A),
                            unfocusedContainerColor = Color(0xFF0F172A),
                            focusedBorderColor = Color(0xFF10B981),
                            unfocusedBorderColor = Color(0xFF334155)
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().testTag("content_link_input")
                    )
                } else {
                    Text("Fichier & Données du téléphone", color = Color(0xFF94A3B8), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Surface(
                        color = Color(0xFF0F172A),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, if (selectedFileName != null) Color(0xFF10B981) else Color(0xFF10B981).copy(alpha = 0.5f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val mimeType = when (type.uppercase()) {
                                    "PDF" -> "application/pdf"
                                    "PHOTO" -> "image/*"
                                    "VIDEO" -> "video/*"
                                    else -> "*/*"
                                }
                                filePickerLauncher.launch(mimeType)
                            }
                            .testTag("import_file_from_phone_btn")
                    ) {
                        if (selectedFileName != null) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .background(Color(0xFF10B981).copy(alpha = 0.2f), RoundedCornerShape(8.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = Color(0xFF10B981),
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = selectedFileName ?: "Fichier sélectionné",
                                        color = Color.White,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${selectedFileSize ?: "Prêt pour l'import"} • Stockage Téléphone",
                                        color = Color(0xFF10B981),
                                        fontSize = 11.sp
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        val mimeType = when (type.uppercase()) {
                                            "PDF" -> "application/pdf"
                                            "PHOTO" -> "image/*"
                                            "VIDEO" -> "video/*"
                                            else -> "*/*"
                                        }
                                        filePickerLauncher.launch(mimeType)
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.Refresh, contentDescription = "Changer", tint = Color(0xFF94A3B8), modifier = Modifier.size(18.dp))
                                }
                            }
                        } else {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 18.dp, horizontal = 12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(imageVector = Icons.Default.CloudUpload, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(28.dp))
                                Spacer(modifier = Modifier.height(6.dp))
                                Text("Importer depuis le téléphone", color = Color(0xFF10B981), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                Text("Accès direct aux documents, photos et vidéos de l'appareil", color = Color(0xFF64748B), fontSize = 10.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Submit button
                Button(
                    onClick = {
                        if (title.isNotBlank()) {
                            val newOrUpdated = ContentEntity(
                                id = content?.id ?: "cnt_${System.currentTimeMillis()}",
                                title = title,
                                description = description,
                                type = type.uppercase(),
                                category = category,
                                fileUrl = linkUrl,
                                viewsCount = content?.viewsCount ?: 0,
                                dateText = content?.dateText ?: "Aujourd'hui",
                                referenceCode = content?.referenceCode ?: "ADM-${System.currentTimeMillis() % 10000}",
                                version = content?.version ?: "v1.0",
                                authorName = content?.authorName ?: "Administrateur HSE",
                                authorRole = content?.authorRole ?: "Direction HSE",
                                site = content?.site ?: "Tous les sites",
                                timestampMillis = content?.timestampMillis ?: System.currentTimeMillis()
                            )
                            onSave(newOrUpdated)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF047857)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .testTag("save_content_btn")
                ) {
                    Icon(imageVector = Icons.Default.CloudUpload, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isEdit) "Enregistrer les modifications" else "Publier le contenu",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditSuccessStoryDialog(
    story: SuccessStoryEntity?,
    onDismiss: () -> Unit,
    onSave: (SuccessStoryEntity) -> Unit
) {
    var title by remember { mutableStateOf(story?.title ?: "") }
    var authorName by remember { mutableStateOf(story?.authorName ?: "") }
    var authorRole by remember { mutableStateOf(story?.authorRole ?: "") }
    var site by remember { mutableStateOf(story?.site ?: "Bonabéri") }
    var description by remember { mutableStateOf(story?.description ?: "") }
    var type by remember { mutableStateOf(story?.type ?: "TEMOIGNAGE") }
    var expandedSite by remember { mutableStateOf(false) }
    var expandedType by remember { mutableStateOf(false) }

    val isEdit = story != null

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            color = Color(0xFF1E293B),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, Color(0xFF334155)),
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(vertical = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.EmojiEvents, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(22.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isEdit) "Modifier Succès Story" else "Nouvelle Succès Story",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Fermer", tint = Color(0xFF94A3B8))
                    }
                }

                Text("Titre de la Succès Story", color = Color(0xFF94A3B8), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    placeholder = { Text("Ex: 1000 jours sans accident avec arrêt à Nomayos", color = Color(0xFF64748B), fontSize = 12.sp) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedContainerColor = Color(0xFF0F172A),
                        unfocusedContainerColor = Color(0xFF0F172A),
                        focusedBorderColor = Color(0xFFF59E0B),
                        unfocusedBorderColor = Color(0xFF334155)
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Auteur / Porteur", color = Color(0xFF94A3B8), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        OutlinedTextField(
                            value = authorName,
                            onValueChange = { authorName = it },
                            placeholder = { Text("Nom & Prénom", color = Color(0xFF64748B), fontSize = 12.sp) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedContainerColor = Color(0xFF0F172A),
                                unfocusedContainerColor = Color(0xFF0F172A),
                                focusedBorderColor = Color(0xFFF59E0B),
                                unfocusedBorderColor = Color(0xFF334155)
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text("Poste / Fonction", color = Color(0xFF94A3B8), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        OutlinedTextField(
                            value = authorRole,
                            onValueChange = { authorRole = it },
                            placeholder = { Text("Ex: Chef d'équipe", color = Color(0xFF64748B), fontSize = 12.sp) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedContainerColor = Color(0xFF0F172A),
                                unfocusedContainerColor = Color(0xFF0F172A),
                                focusedBorderColor = Color(0xFFF59E0B),
                                unfocusedBorderColor = Color(0xFF334155)
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Site CIMENCAM", color = Color(0xFF94A3B8), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        ExposedDropdownMenuBox(
                            expanded = expandedSite,
                            onExpandedChange = { expandedSite = it }
                        ) {
                            OutlinedTextField(
                                value = site,
                                onValueChange = {},
                                readOnly = true,
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedSite) },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedContainerColor = Color(0xFF0F172A),
                                    unfocusedContainerColor = Color(0xFF0F172A),
                                    focusedBorderColor = Color(0xFFF59E0B),
                                    unfocusedBorderColor = Color(0xFF334155)
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth().menuAnchor()
                            )
                            ExposedDropdownMenu(
                                expanded = expandedSite,
                                onDismissRequest = { expandedSite = false },
                                modifier = Modifier.background(Color(0xFF1E293B))
                            ) {
                                listOf("Bonabéri", "Nomayos", "Figuil", "Siège Douala", "Tous les sites").forEach { s ->
                                    DropdownMenuItem(
                                        text = { Text(s, color = Color.White) },
                                        onClick = {
                                            site = s
                                            expandedSite = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text("Type / Thématique", color = Color(0xFF94A3B8), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        ExposedDropdownMenuBox(
                            expanded = expandedType,
                            onExpandedChange = { expandedType = it }
                        ) {
                            OutlinedTextField(
                                value = type,
                                onValueChange = {},
                                readOnly = true,
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedType) },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedContainerColor = Color(0xFF0F172A),
                                    unfocusedContainerColor = Color(0xFF0F172A),
                                    focusedBorderColor = Color(0xFFF59E0B),
                                    unfocusedBorderColor = Color(0xFF334155)
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth().menuAnchor()
                            )
                            ExposedDropdownMenu(
                                expanded = expandedType,
                                onDismissRequest = { expandedType = false },
                                modifier = Modifier.background(Color(0xFF1E293B))
                            ) {
                                listOf("TEMOIGNAGE", "BONNE_PRATIQUE", "INNOVATION", "EXCELLENCE", "SECURITE").forEach { c ->
                                    DropdownMenuItem(
                                        text = { Text(c, color = Color.White) },
                                        onClick = {
                                            type = c
                                            expandedType = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                Text("Description / Récit du succès", color = Color(0xFF94A3B8), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    placeholder = { Text("Décrivez les actions menées, les résultats obtenus et l'impact positif sur la sécurité...", color = Color(0xFF64748B), fontSize = 12.sp) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedContainerColor = Color(0xFF0F172A),
                        unfocusedContainerColor = Color(0xFF0F172A),
                        focusedBorderColor = Color(0xFFF59E0B),
                        unfocusedBorderColor = Color(0xFF334155)
                    ),
                    minLines = 3,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(6.dp))

                Button(
                    onClick = {
                        if (title.isNotBlank() && description.isNotBlank()) {
                            val entity = SuccessStoryEntity(
                                id = story?.id ?: "story_${System.currentTimeMillis()}",
                                title = title,
                                description = description,
                                authorName = authorName.ifBlank { "Équipe" },
                                authorRole = authorRole.ifBlank { "Collaborateur" },
                                site = site,
                                dateText = story?.dateText ?: "Aujourd'hui",
                                likesCount = story?.likesCount ?: 0,
                                isLiked = story?.isLiked ?: false,
                                type = type
                            )
                            onSave(entity)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                ) {
                    Icon(imageVector = Icons.Default.EmojiEvents, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isEdit) "Mettre à jour la Succès Story" else "Publier dans le fil d'actualité",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditSurveyDialog(
    survey: SurveyEntity?,
    onDismiss: () -> Unit,
    onSave: (SurveyEntity) -> Unit
) {
    var title by remember { mutableStateOf(survey?.title ?: "") }
    var description by remember { mutableStateOf(survey?.description ?: "") }
    var quarter by remember { mutableStateOf(survey?.quarter ?: "T1 2026") }
    var questionsCountText by remember { mutableStateOf(survey?.questionsCount?.toString() ?: "5") }
    var isActive by remember { mutableStateOf(survey?.status?.equals("Actif", ignoreCase = true) ?: true) }

    val isEdit = survey != null

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            color = Color(0xFF1E293B),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, Color(0xFF334155)),
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(vertical = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Poll, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(22.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isEdit) "Modifier le Sondage" else "Nouveau Sondage / Enquête",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Fermer", tint = Color(0xFF94A3B8))
                    }
                }

                Text("Titre de l'enquête ou du sondage", color = Color(0xFF94A3B8), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    placeholder = { Text("Ex: Évaluation du port des EPI & Culture Sécurité", color = Color(0xFF64748B), fontSize = 12.sp) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedContainerColor = Color(0xFF0F172A),
                        unfocusedContainerColor = Color(0xFF0F172A),
                        focusedBorderColor = Color(0xFF38BDF8),
                        unfocusedBorderColor = Color(0xFF334155)
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Description / Objectif", color = Color(0xFF94A3B8), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    placeholder = { Text("Ex: Donnez votre avis sur l'application des règles et l'état des équipements de protection.", color = Color(0xFF64748B), fontSize = 12.sp) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedContainerColor = Color(0xFF0F172A),
                        unfocusedContainerColor = Color(0xFF0F172A),
                        focusedBorderColor = Color(0xFF38BDF8),
                        unfocusedBorderColor = Color(0xFF334155)
                    ),
                    minLines = 2,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Période / Trimestre", color = Color(0xFF94A3B8), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        OutlinedTextField(
                            value = quarter,
                            onValueChange = { quarter = it },
                            placeholder = { Text("Ex: T1 2026", color = Color(0xFF64748B), fontSize = 12.sp) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedContainerColor = Color(0xFF0F172A),
                                unfocusedContainerColor = Color(0xFF0F172A),
                                focusedBorderColor = Color(0xFF38BDF8),
                                unfocusedBorderColor = Color(0xFF334155)
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text("Nombre de questions", color = Color(0xFF94A3B8), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        OutlinedTextField(
                            value = questionsCountText,
                            onValueChange = { questionsCountText = it.filter { ch -> ch.isDigit() } },
                            placeholder = { Text("Ex: 5", color = Color(0xFF64748B), fontSize = 12.sp) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedContainerColor = Color(0xFF0F172A),
                                unfocusedContainerColor = Color(0xFF0F172A),
                                focusedBorderColor = Color(0xFF38BDF8),
                                unfocusedBorderColor = Color(0xFF334155)
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF0F172A), RoundedCornerShape(10.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Statut du sondage", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        Text(if (isActive) "Ouvert aux votes des collaborateurs" else "Clôturé / Archivé", color = Color(0xFF94A3B8), fontSize = 11.sp)
                    }
                    Switch(
                        checked = isActive,
                        onCheckedChange = { isActive = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Color(0xFF10B981),
                            uncheckedThumbColor = Color(0xFF94A3B8),
                            uncheckedTrackColor = Color(0xFF334155)
                        )
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Button(
                    onClick = {
                        if (title.isNotBlank()) {
                            val count = questionsCountText.toIntOrNull() ?: 5
                            val newSurvey = SurveyEntity(
                                id = survey?.id ?: "survey_${System.currentTimeMillis()}",
                                title = title,
                                description = description.ifBlank { "Participez au sondage HSE" },
                                quarter = quarter.ifBlank { "T1 2026" },
                                status = if (isActive) "Actif" else "Clôturé",
                                questionsCount = count,
                                isCompleted = survey?.isCompleted ?: false
                            )
                            onSave(newSurvey)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                ) {
                    Icon(imageVector = Icons.Default.Poll, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isEdit) "Enregistrer le Sondage" else "Publier le Sondage aux équipes",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SendNotificationDialog(
    onDismiss: () -> Unit,
    onSend: (title: String, message: String, type: String, site: String, priority: String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("ALERTE") }
    var targetSite by remember { mutableStateOf("Tous les sites") }
    var priority by remember { mutableStateOf("URGENT") }
    var expandedSite by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            color = Color(0xFF1E293B),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, Color(0xFF334155)),
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(vertical = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Campaign, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(22.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Diffuser une Notification Flash", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Fermer", tint = Color(0xFF94A3B8))
                    }
                }

                Text("Type de notification", color = Color(0xFF94A3B8), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("ALERTE", "SUCCÈS", "SONDAGE", "INFO", "URGENCE").forEach { t ->
                        val isSel = type == t
                        val col = when (t) {
                            "ALERTE", "URGENCE" -> Color(0xFFEF4444)
                            "SUCCÈS" -> Color(0xFF10B981)
                            "SONDAGE" -> Color(0xFF38BDF8)
                            else -> Color(0xFFF59E0B)
                        }
                        Surface(
                            color = if (isSel) col.copy(alpha = 0.2f) else Color(0xFF0F172A),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, if (isSel) col else Color(0xFF334155)),
                            modifier = Modifier.weight(1f).clickable { type = t }
                        ) {
                            Text(
                                text = t,
                                color = if (isSel) col else Color(0xFF94A3B8),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                    }
                }

                Text("Titre de la notification", color = Color(0xFF94A3B8), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    placeholder = { Text("Ex: ALERTE MÉTÉO - Vent violent sur Bonabéri", color = Color(0xFF64748B), fontSize = 12.sp) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedContainerColor = Color(0xFF0F172A),
                        unfocusedContainerColor = Color(0xFF0F172A),
                        focusedBorderColor = Color(0xFFEF4444),
                        unfocusedBorderColor = Color(0xFF334155)
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Message / Consignes", color = Color(0xFF94A3B8), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                OutlinedTextField(
                    value = message,
                    onValueChange = { message = it },
                    placeholder = { Text("Rédigez les instructions claires et immédiates...", color = Color(0xFF64748B), fontSize = 12.sp) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedContainerColor = Color(0xFF0F172A),
                        unfocusedContainerColor = Color(0xFF0F172A),
                        focusedBorderColor = Color(0xFFEF4444),
                        unfocusedBorderColor = Color(0xFF334155)
                    ),
                    minLines = 3,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Site ciblé", color = Color(0xFF94A3B8), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        ExposedDropdownMenuBox(
                            expanded = expandedSite,
                            onExpandedChange = { expandedSite = it }
                        ) {
                            OutlinedTextField(
                                value = targetSite,
                                onValueChange = {},
                                readOnly = true,
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedSite) },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedContainerColor = Color(0xFF0F172A),
                                    unfocusedContainerColor = Color(0xFF0F172A),
                                    focusedBorderColor = Color(0xFFEF4444),
                                    unfocusedBorderColor = Color(0xFF334155)
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth().menuAnchor()
                            )
                            ExposedDropdownMenu(
                                expanded = expandedSite,
                                onDismissRequest = { expandedSite = false },
                                modifier = Modifier.background(Color(0xFF1E293B))
                            ) {
                                listOf("Tous les sites", "Bonabéri", "Nomayos", "Figuil", "Siège Douala").forEach { s ->
                                    DropdownMenuItem(
                                        text = { Text(s, color = Color.White) },
                                        onClick = {
                                            targetSite = s
                                            expandedSite = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text("Priorité", color = Color(0xFF94A3B8), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        Row(modifier = Modifier.fillMaxWidth().height(56.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                            listOf("NORMAL", "URGENT").forEach { p ->
                                val isSel = priority == p
                                Surface(
                                    color = if (isSel) (if (p == "URGENT") Color(0xFFEF4444).copy(alpha = 0.2f) else Color(0xFF38BDF8).copy(alpha = 0.2f)) else Color(0xFF0F172A),
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(1.dp, if (isSel) (if (p == "URGENT") Color(0xFFEF4444) else Color(0xFF38BDF8)) else Color(0xFF334155)),
                                    modifier = Modifier.weight(1f).clickable { priority = p }
                                ) {
                                    Text(
                                        text = p,
                                        color = if (isSel) (if (p == "URGENT") Color(0xFFEF4444) else Color(0xFF38BDF8)) else Color(0xFF94A3B8),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                        modifier = Modifier.padding(vertical = 10.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Button(
                    onClick = {
                        if (title.isNotBlank() && message.isNotBlank()) {
                            onSend(title, message, type, targetSite, priority)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                ) {
                    Icon(imageVector = Icons.Default.Send, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Diffuser immédiatement aux collaborateurs",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

@Composable
fun AdminAccessRestrictedView(
    title: String,
    onBack: () -> Unit
) {
    Scaffold(
        containerColor = Color(0xFF0F172A)
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, Color(0xFFDC2626).copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .background(Color(0xFFDC2626).copy(alpha = 0.15f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Accès Restreint",
                            tint = Color(0xFFDC2626),
                            modifier = Modifier.size(34.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Accès Administrateur Restreint",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "L'interface « $title » est strictement réservée aux administrateurs de la Direction HSE CIMENCAM.",
                        color = Color(0xFF94A3B8),
                        fontSize = 13.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = onBack,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Retour à l'accueil",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}
