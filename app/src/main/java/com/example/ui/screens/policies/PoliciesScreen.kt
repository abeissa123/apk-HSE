package com.example.ui.screens.policies

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.entity.ContentEntity
import com.example.domain.HseDocument
import com.example.domain.HsePolicyCategory
import com.example.domain.HseStaticData
import com.example.ui.components.MediaPermissionBanner
import com.example.ui.theme.*
import com.example.ui.viewmodel.HseViewModel
import com.example.ui.viewmodel.MainTab
import com.example.util.MediaStorageHelper
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun PoliciesScreen(
    viewModel: HseViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedCategoryFilter by remember { mutableStateOf("Tous") }
    var searchQuery by remember { mutableStateOf("") }
    var viewingDocument by remember { mutableStateOf<HseDocument?>(null) }
    val multimediaContents by viewModel.allContents.collectAsState()

    var showPlanUrgenceDialog by remember { mutableStateOf(false) }
    var showEnvironnementDialog by remember { mutableStateOf(false) }
    var showInductionDialog by remember { mutableStateOf(false) }
    var showSanteDialog by remember { mutableStateOf(false) }
    var showSecuriteDialog by remember { mutableStateOf(false) }
    var showProgrammeDialog by remember { mutableStateOf(false) }
    var showPolitiqueDialog by remember { mutableStateOf(false) }
    var showSuccesStoriesDialog by remember { mutableStateOf(false) }
    var isGridLayout by remember { mutableStateOf(true) }

    val authState by viewModel.authState.collectAsState()
    val userState by viewModel.user.collectAsState()
    val activeUser = userState?.takeIf { it.email.isNotBlank() } ?: authState.currentUser
    val isUserAdmin = activeUser.isAdmin
    val isB2Syncing by viewModel.isB2Syncing.collectAsState()

    // Pending Imported File State for Confirmation & Categorization Dialog
    var pendingImportResult by remember { mutableStateOf<com.example.util.ImportedFileResult?>(null) }
    var pendingImportTitle by remember { mutableStateOf("") }
    var pendingImportCategory by remember { mutableStateOf("Sécurité") }
    var pendingImportDescription by remember { mutableStateOf("") }
    var isImportDialogOpen by remember { mutableStateOf(false) }
    var isServerDocsDialogOpen by remember { mutableStateOf(false) }
    val b2Files by viewModel.b2FilesList.collectAsState()

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val importResult = MediaStorageHelper.importUriToAppStorage(context, uri)
            pendingImportResult = importResult
            pendingImportTitle = importResult.fileName.substringBeforeLast(".")
            pendingImportCategory = if (selectedCategoryFilter != "Tous") selectedCategoryFilter else "Sécurité"
            pendingImportDescription = "Document importé (${importResult.fileSizeFormatted}). Disponible pour consultation hors-ligne et archivage."
            isImportDialogOpen = true
        }
    }

    val activeDocId by viewModel.activeDocumentId.collectAsState()
    LaunchedEffect(activeDocId) {
        if (activeDocId != null) {
            viewingDocument = HseStaticData.documents.find { it.id == activeDocId }
        }
    }

    val categories = HseStaticData.categories
    val documents = HseStaticData.documents

    // Filter static documents
    val filteredDocuments = remember(selectedCategoryFilter, searchQuery) {
        documents.filter { doc ->
            val matchCat = when (selectedCategoryFilter) {
                "Tous" -> true
                "Politique" -> doc.category.contains("Politique", ignoreCase = true)
                else -> doc.category.contains(selectedCategoryFilter, ignoreCase = true)
            }
            val matchSearch = searchQuery.isBlank() ||
                    doc.title.contains(searchQuery, ignoreCase = true) ||
                    doc.summary.contains(searchQuery, ignoreCase = true) ||
                    doc.referenceCode.contains(searchQuery, ignoreCase = true)
            matchCat && matchSearch
        }
    }

    // Filter multimedia contents from DB
    val filteredMedia = remember(multimediaContents, selectedCategoryFilter, searchQuery) {
        multimediaContents.filter { media ->
            val matchCat = when (selectedCategoryFilter) {
                "Tous" -> true
                "Politique" -> media.category.contains("Politique", ignoreCase = true)
                else -> media.category.contains(selectedCategoryFilter, ignoreCase = true)
            }
            val matchSearch = searchQuery.isBlank() ||
                    media.title.contains(searchQuery, ignoreCase = true) ||
                    media.description.contains(searchQuery, ignoreCase = true) ||
                    media.referenceCode.contains(searchQuery, ignoreCase = true) ||
                    media.authorName.contains(searchQuery, ignoreCase = true)
            matchCat && matchSearch
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
    ) {
        val columnsCount = when {
            !isGridLayout -> 1
            maxWidth >= 700.dp -> 3
            maxWidth >= 330.dp -> 2
            else -> 1
        }
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 80.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            // Header Hero Banner (Vert forêt #1B7F4B -> Vert profond #0F5132)
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    HSEBluePrimary, // #1B7F4B Vert forêt
                                    HSEBlueDark     // #0F5132 Vert profond
                                )
                            )
                        )
                        .padding(horizontal = 20.dp, vertical = 18.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(
                                    Color.White.copy(alpha = 0.2f),
                                    RoundedCornerShape(14.dp)
                                )
                                .border(1.2.dp, Color.White.copy(alpha = 0.45f), RoundedCornerShape(14.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "Rubrique Références",
                                    color = Color.White,
                                    fontSize = 19.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Surface(
                                    color = Color.White.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = "HSE",
                                        color = Color.White,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = "Directives officielles, procédures d'urgence & référentiels",
                                color = Color.White.copy(alpha = 0.95f),
                                fontSize = 13.sp,
                                lineHeight = 17.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            // Champ de Recherche Compact & Lisible
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = {
                        Text(
                            text = "Rechercher un module, consigne, fiche...",
                            color = Color(0xFFCBD5E1),
                            fontSize = 14.sp
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = HSEGreenSafe,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotBlank()) {
                            IconButton(
                                onClick = { searchQuery = "" },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Effacer la recherche",
                                    tint = Color.White
                                )
                            }
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFF1E293B),
                        unfocusedContainerColor = Color(0xFF1E293B),
                        focusedBorderColor = HSEGreenSafe,
                        unfocusedBorderColor = Color(0xFF475569),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    textStyle = TextStyle(fontSize = 14.sp, color = Color.White, fontWeight = FontWeight.Medium),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .testTag("policy_search_input")
                )
            }

            // Filtres thématiques horizontaux compacts
            item {
                val filterChips = listOf(
                    "Tous" to "Tous les modules (8)",
                    "Politique" to "📜 Politique",
                    "Succès" to "⭐ Succès Stories",
                    "Urgence" to "🚨 Plan d'urgence",
                    "Environnement" to "🌿 Environnement",
                    "Induction" to "🎓 Induction",
                    "Santé" to "❤️ Santé",
                    "Sécurité" to "🛡️ Sécurité",
                    "Programme" to "📅 Programme"
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    filterChips.forEach { (key, label) ->
                        val isSelected = selectedCategoryFilter == key
                        Surface(
                            onClick = { selectedCategoryFilter = key },
                            shape = RoundedCornerShape(16.dp),
                            color = if (isSelected) HSEBluePrimary else Color(0xFF1E293B),
                            border = BorderStroke(
                                1.dp,
                                if (isSelected) HSEGreenSafe else Color(0xFF475569)
                            )
                        ) {
                            Text(
                                text = label,
                                color = if (isSelected) Color.White else Color(0xFFF1F5F9),
                                fontSize = 12.5.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }

            // 8 Boutons Politiques Responsifs & Compacts
            val buttonItems = listOf(
                PolicyButtonItem(
                    title = "Politique",
                    subtitle = "Engagements HSE & guide officiel",
                    badge = "GUIDE HSE",
                    icon = Icons.Default.AutoStories,
                    accentColor = Color(0xFF0284C7),
                    testTag = "button_politique",
                    filterCategory = "Politique",
                    searchKeywords = "politique guide charte vision direction engagements management hse holcim",
                    onClick = { showPolitiqueDialog = true }
                ),
                PolicyButtonItem(
                    title = "Succès Stories",
                    subtitle = "Bonnes pratiques & REX terrain",
                    badge = "REX TERRAIN",
                    icon = Icons.Default.Star,
                    accentColor = Color(0xFFD97706),
                    testTag = "button_succes_stories",
                    filterCategory = "Succès",
                    searchKeywords = "succès succes stories rex bonnes pratiques retours expérience innovation reconnaissance",
                    onClick = { showSuccesStoriesDialog = true }
                ),
                PolicyButtonItem(
                    title = "Plan d'urgence",
                    subtitle = "Contacts 24h/24 & secours",
                    badge = "24/7",
                    icon = Icons.Default.Warning,
                    accentColor = Color(0xFFEF4444),
                    testTag = "button_plan_urgence",
                    filterCategory = "Urgence",
                    searchKeywords = "plan urgence secours rassemblement 24/7 hotline appel fiches incendie accident",
                    onClick = { showPlanUrgenceDialog = true }
                ),
                PolicyButtonItem(
                    title = "Environnement",
                    subtitle = "ISO 14001 & transition",
                    badge = "ISO 14001",
                    icon = Icons.Default.Eco,
                    accentColor = Color(0xFF10B981),
                    testTag = "button_environnement",
                    filterCategory = "Environnement",
                    searchKeywords = "environnement durable écologie iso 14001 eau décarbonation déchets recyclage",
                    onClick = { showEnvironnementDialog = true }
                ),
                PolicyButtonItem(
                    title = "Induction",
                    subtitle = "Accueil & livret officiel",
                    badge = "ACCUEIL",
                    icon = Icons.Default.School,
                    accentColor = Color(0xFF2563EB),
                    testTag = "button_induction",
                    filterCategory = "Induction",
                    searchKeywords = "induction accueil nouveaux arrivants prestataires livret guérite visiteur",
                    onClick = { showInductionDialog = true }
                ),
                PolicyButtonItem(
                    title = "Santé",
                    subtitle = "Prévention & ergonomie",
                    badge = "SANTÉ",
                    icon = Icons.Default.Favorite,
                    accentColor = Color(0xFFE11D48),
                    testTag = "button_sante",
                    filterCategory = "Santé",
                    searchKeywords = "santé prévention sanitaire hygiène poussière chaleur bruit ergonomie médical",
                    onClick = { showSanteDialog = true }
                ),
                PolicyButtonItem(
                    title = "Sécurité",
                    subtitle = "12 Règles d'Or & standards",
                    badge = "12 RÈGLES",
                    icon = Icons.Default.Shield,
                    accentColor = HSEBluePrimary,
                    testTag = "button_securite",
                    filterCategory = "Sécurité",
                    searchKeywords = "sécurité règles d'or loto epi hauteur retrait consignation permis",
                    onClick = { showSecuriteDialog = true }
                ),
                PolicyButtonItem(
                    title = "Programme",
                    subtitle = "Campagnes 2026 & rituels",
                    badge = "2026",
                    icon = Icons.Default.CalendarMonth,
                    accentColor = Color(0xFF6366F1),
                    testTag = "button_programme",
                    filterCategory = "Programme",
                    searchKeywords = "programme campagnes formations 2026 calendrier quart d'heure hse vfl rituels",
                    onClick = { showProgrammeDialog = true }
                )
            )

            val filteredButtons = buttonItems.filter { btn ->
                val matchesCategory = selectedCategoryFilter == "Tous" || selectedCategoryFilter == btn.filterCategory
                val q = searchQuery.trim().lowercase()
                val matchesSearch = q.isBlank() || btn.title.lowercase().contains(q) || btn.searchKeywords.lowercase().contains(q)
                matchesCategory && matchesSearch
            }

            // Section Header Responsif avec sélecteur Grille / Liste - Lisibilité Optimale
            item {
                Surface(
                    color = Color(0xFF1E293B),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.2.dp, Color(0xFF334155)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .background(HSEBluePrimary.copy(alpha = 0.25f), RoundedCornerShape(8.dp))
                                        .border(1.dp, HSEBluePrimary, RoundedCornerShape(8.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.FolderOpen,
                                        contentDescription = null,
                                        tint = Color(0xFF38BDF8),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "Rubriques & Références Clés",
                                        color = Color.White,
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.Black,
                                        letterSpacing = 0.2.sp
                                    )
                                    Surface(
                                        color = HSEGreenSafe.copy(alpha = 0.25f),
                                        shape = RoundedCornerShape(6.dp),
                                        border = BorderStroke(1.dp, HSEGreenSafe)
                                    ) {
                                        Text(
                                            text = "${filteredButtons.size} modules",
                                            color = Color(0xFF4ADE80),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }

                            // Sélecteur de vue Responsive (Grille / Liste)
                            Row(
                                modifier = Modifier
                                    .background(Color(0xFF0F172A), RoundedCornerShape(8.dp))
                                    .border(1.dp, Color(0xFF475569), RoundedCornerShape(8.dp))
                                    .padding(2.dp),
                                horizontalArrangement = Arrangement.spacedBy(2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    onClick = { isGridLayout = true },
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (isGridLayout) HSEBluePrimary else Color.Transparent
                                ) {
                                    Text(
                                        text = "Grille",
                                        color = if (isGridLayout) Color.White else Color(0xFFE2E8F0),
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                                    )
                                }
                                Surface(
                                    onClick = { isGridLayout = false },
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (!isGridLayout) HSEBluePrimary else Color.Transparent
                                ) {
                                    Text(
                                        text = "Liste",
                                        color = if (!isGridLayout) Color.White else Color(0xFFE2E8F0),
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Sélectionnez un module pour consulter ses consignes et procédures :",
                            color = Color.White,
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold,
                            lineHeight = 19.sp
                        )
                    }
                }
            }

            // Boutons responsifs : Grille multi-colonnes ou Liste compacte
            if (columnsCount > 1) {
                val chunked = filteredButtons.chunked(columnsCount)
                items(chunked) { rowButtons ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowButtons.forEach { btn ->
                            Box(modifier = Modifier.weight(1f)) {
                                PolicyGridCard(
                                    title = btn.title,
                                    subtitle = btn.subtitle,
                                    badge = btn.badge,
                                    icon = btn.icon,
                                    accentColor = btn.accentColor,
                                    testTag = btn.testTag,
                                    onClick = btn.onClick,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                        val emptySlots = columnsCount - rowButtons.size
                        repeat(emptySlots) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            } else {
                items(filteredButtons, key = { it.title }) { btn ->
                    PolicyListCard(
                        title = btn.title,
                        subtitle = btn.subtitle,
                        badge = btn.badge,
                        icon = btn.icon,
                        accentColor = btn.accentColor,
                        testTag = btn.testTag,
                        onClick = btn.onClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 3.dp)
                    )
                }
            }
        }

        // Full In-App Document Reader Dialog
        viewingDocument?.let { doc ->
            DocumentReaderDialog(
                document = doc,
                viewModel = viewModel,
                onDismiss = {
                    viewingDocument = null
                    viewModel.activeDocumentId.value = null
                }
            )
        }

        // Dialog Plan d'urgence
        if (showPlanUrgenceDialog) {
            PlanUrgenceDialog(
                onDismiss = { showPlanUrgenceDialog = false }
            )
        }

        // Dialog Environnement
        if (showEnvironnementDialog) {
            EnvironnementDialog(
                onDismiss = { showEnvironnementDialog = false },
                onOpenDocument = { docId ->
                    showEnvironnementDialog = false
                    viewingDocument = HseStaticData.documents.find { it.id == docId }
                }
            )
        }

        // Dialog Induction
        if (showInductionDialog) {
            InductionDialog(
                onDismiss = { showInductionDialog = false },
                onOpenDocument = { docId ->
                    showInductionDialog = false
                    viewingDocument = HseStaticData.documents.find { it.id == docId }
                }
            )
        }

        // Dialog Santé
        if (showSanteDialog) {
            SanteDialog(
                onDismiss = { showSanteDialog = false },
                onOpenDocument = { docId ->
                    showSanteDialog = false
                    viewingDocument = HseStaticData.documents.find { it.id == docId }
                }
            )
        }

        // Dialog Sécurité
        if (showSecuriteDialog) {
            SecuriteDialog(
                onDismiss = { showSecuriteDialog = false },
                onOpenDocument = { docId ->
                    showSecuriteDialog = false
                    viewingDocument = HseStaticData.documents.find { it.id == docId }
                }
            )
        }

        // Dialog Programme
        if (showProgrammeDialog) {
            ProgrammeDialog(
                onDismiss = { showProgrammeDialog = false },
                onNavigateToProgramme = {
                    showProgrammeDialog = false
                    viewModel.setTab(MainTab.PROGRAMME)
                }
            )
        }

        // Dialog Politique
        if (showPolitiqueDialog) {
            PolitiqueDialog(
                onDismiss = { showPolitiqueDialog = false }
            )
        }

        // Dialog Succès Stories
        if (showSuccesStoriesDialog) {
            SuccesStoriesDialog(
                onDismiss = { showSuccesStoriesDialog = false }
            )
        }

        // Dialog Serveur Documentaire Cloud
        if (isServerDocsDialogOpen) {
            ServerDocumentsDialog(
                viewModel = viewModel,
                isB2Syncing = isB2Syncing,
                b2Files = b2Files,
                onDismiss = { isServerDocsDialogOpen = false },
                onOpenDocument = { b2File ->
                    try {
                        val viewIntent = Intent(Intent.ACTION_VIEW).apply {
                            setDataAndType(
                                Uri.parse(b2File.downloadUrl),
                                if (b2File.fileName.endsWith(".pdf", ignoreCase = true)) "application/pdf" else "*/*"
                            )
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(viewIntent)
                    } catch (e: Exception) {
                        Toast.makeText(context, "Ouverture impossible: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            )
        }
    }
}

@Composable
fun ServerDocumentsDialog(
    viewModel: HseViewModel,
    isB2Syncing: Boolean,
    b2Files: List<com.example.util.B2FileItem>,
    onDismiss: () -> Unit,
    onOpenDocument: (com.example.util.B2FileItem) -> Unit
) {
    val context = LocalContext.current
    val b2Status by viewModel.b2ConnectionStatus.collectAsState()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, Color(0xFF0284C7).copy(alpha = 0.5f)),
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.85f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .background(Color(0xFF0284C7).copy(alpha = 0.2f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudDone,
                                contentDescription = null,
                                tint = Color(0xFF38BDF8),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Serveur Documentaire HSE Cloud",
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Accessible à tous les collaborateurs",
                                color = Color(0xFFCBD5E1),
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Fermer",
                            tint = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Masked Server Security Information
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFF334155)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(Color(0xFF10B981), CircleShape)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "OPÉRATIONNEL",
                                    color = Color(0xFF10B981),
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Surface(
                                color = Color(0xFF38BDF8).copy(alpha = 0.15f),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = "AES-256",
                                    color = Color(0xFF38BDF8),
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "• Stockage Cloud : Centralisé & Hautement Sécurisé\n" +
                                   "• Liaison sécurisée : ${com.example.util.B2CloudStorageService.MASKED_KEY_ID}\n" +
                                   "• Accès : Ouvert à tous les utilisateurs (Lecture & Streaming)\n" +
                                   "• $b2Status",
                            color = Color(0xFFF1F5F9),
                            fontSize = 12.5.sp,
                            lineHeight = 18.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Synchronization Action Button
                Button(
                    onClick = {
                        viewModel.syncB2Files { res ->
                            Toast.makeText(context, res.message, Toast.LENGTH_SHORT).show()
                        }
                    },
                    enabled = !isB2Syncing,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp)
                        .testTag("dialog_sync_b2_btn")
                ) {
                    if (isB2Syncing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Synchronisation des documents...", fontSize = 12.sp, color = Color.White)
                    } else {
                        Icon(
                            imageVector = Icons.Default.Sync,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Actualiser la liste des documents", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Documents List Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Documents sur le serveur",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${b2Files.size} document(s)",
                        color = Color(0xFF38BDF8),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Documents LazyColumn
                if (b2Files.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.CloudQueue,
                                contentDescription = null,
                                tint = Color(0xFF64748B),
                                modifier = Modifier.size(44.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Aucun document synchronisé pour l'instant",
                                color = Color(0xFF94A3B8),
                                fontSize = 12.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Cliquez sur « Actualiser » pour récupérer les fichiers du serveur",
                                color = Color(0xFF64748B),
                                fontSize = 11.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(b2Files, key = { it.fileId }) { file ->
                            val isPdf = file.fileName.endsWith(".pdf", ignoreCase = true)
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, Color(0xFF334155)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
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
                                                .background(
                                                    if (isPdf) Color(0xFFEF4444).copy(alpha = 0.2f) else Color(0xFF3B82F6).copy(alpha = 0.2f),
                                                    RoundedCornerShape(8.dp)
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = if (isPdf) Icons.Default.PictureAsPdf else Icons.Default.Description,
                                                contentDescription = null,
                                                tint = if (isPdf) Color(0xFFEF4444) else Color(0xFF38BDF8),
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(
                                                text = file.fileName,
                                                color = Color.White,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = "${MediaStorageHelper.formatFileSize(file.contentLength)} • Document officiel",
                                                color = Color(0xFFCBD5E1),
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    }

                                    Button(
                                        onClick = { onOpenDocument(file) },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                        modifier = Modifier.height(34.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Visibility,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Consulter", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Close Button
                OutlinedButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFF475569)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp)
                ) {
                    Text("Fermer", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
fun CategoryCardItem(
    category: HsePolicyCategory,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val domainColor = when {
        category.title.contains("Santé", ignoreCase = true) -> HSEHealthBlue         // #1565C0
        category.title.contains("Sécurité", ignoreCase = true) -> HSEOrangeAlert     // #F57C00
        category.title.contains("Environnement", ignoreCase = true) -> HSEEnvGreen   // #2E7D32
        category.title.contains("Induction", ignoreCase = true) -> HSEObligationBlue // #0D47A1
        else -> HSEBluePrimary // #1B7F4B
    }
    val domainCardBg = when {
        category.title.contains("Santé", ignoreCase = true) -> HSEHealthBg           // #E3F2FD
        category.title.contains("Environnement", ignoreCase = true) -> HSEEnvBg      // #E8F5E9
        else -> HSECardBg                                                            // #FFFFFF
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("cat_card_${category.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = domainCardBg),
        border = BorderStroke(1.dp, HSECardBorder)
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
                        .size(38.dp)
                        .background(domainColor, RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    val icon = when (category.iconName) {
                        "shield" -> Icons.Default.Shield
                        "heart" -> Icons.Default.Favorite
                        "helmet" -> Icons.Default.Engineering
                        "leaf" -> Icons.Default.Eco
                        else -> Icons.Default.School
                    }
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = category.title,
                        color = HSETextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = category.subtitle,
                        color = HSETextSecondary,
                        fontSize = 11.sp
                    )
                    Text(
                        text = "${category.documentCount} document${if (category.documentCount > 1) "s" else ""}",
                        color = domainColor,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 10.sp
                    )
                }
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = domainColor,
                modifier = Modifier.size(15.dp)
            )
        }
    }
}

@Composable
fun DocumentCardItem(
    document: HseDocument,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val docColor = when {
        document.category.contains("Santé", ignoreCase = true) -> Color(0xFFE11D48)
        document.category.contains("Environnement", ignoreCase = true) -> Color(0xFF10B981)
        document.category.contains("Sécurité", ignoreCase = true) -> HSEBluePrimary
        else -> Color(0xFF2563EB)
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("doc_card_${document.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        border = BorderStroke(1.2.dp, docColor.copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .background(docColor.copy(alpha = 0.2f), RoundedCornerShape(10.dp))
                        .border(1.dp, docColor.copy(alpha = 0.45f), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Description,
                        contentDescription = null,
                        tint = docColor,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = document.title,
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = document.subtitle,
                        color = Color(0xFFCBD5E1),
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            color = docColor.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = document.referenceCode,
                                color = docColor,
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.5.dp)
                            )
                        }
                        Text(
                            text = "• ${document.format} • ${document.pagesCount} pages",
                            color = Color(0xFF94A3B8),
                            fontSize = 10.5.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))
            Surface(
                color = docColor.copy(alpha = 0.15f),
                shape = CircleShape,
                border = BorderStroke(1.dp, docColor.copy(alpha = 0.35f))
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Consulter",
                    tint = docColor,
                    modifier = Modifier
                        .padding(6.dp)
                        .size(15.dp)
                )
            }
        }
    }
}

enum class ReaderTheme(
    val label: String,
    val canvasBg: Color,
    val sheetBg: Color,
    val sheetBorder: Color,
    val sheetShadow: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val accent: Color,
    val highlightColor: Color,
    val warningBg: Color,
    val warningBorder: Color,
    val warningText: Color,
    val successBg: Color,
    val successBorder: Color,
    val successText: Color,
    val watermarkColor: Color
) {
    STUDIO_LIGHT(
        label = "Blanc Studio (PDF)",
        canvasBg = Color(0xFF1E293B),
        sheetBg = Color(0xFFFFFFFF),
        sheetBorder = Color(0xFFCBD5E1),
        sheetShadow = Color(0x66000000),
        textPrimary = Color(0xFF0F172A),
        textSecondary = Color(0xFF334155),
        textMuted = Color(0xFF64748B),
        accent = Color(0xFF1D4ED8),
        highlightColor = Color(0xFFFEF08A),
        warningBg = Color(0xFFFEF2F2),
        warningBorder = Color(0xFFDC2626),
        warningText = Color(0xFF991B1B),
        successBg = Color(0xFFECFDF5),
        successBorder = Color(0xFF059669),
        successText = Color(0xFF065F46),
        watermarkColor = Color(0x0C000000)
    ),
    WARM_SEPIA(
        label = "Sépia Papier Confort",
        canvasBg = Color(0xFF382F2D),
        sheetBg = Color(0xFFFFFDF5),
        sheetBorder = Color(0xFFDCC8A7),
        sheetShadow = Color(0x55000000),
        textPrimary = Color(0xFF2A1C14),
        textSecondary = Color(0xFF5A4434),
        textMuted = Color(0xFF8B6F55),
        accent = Color(0xFFB45309),
        highlightColor = Color(0xFFFED7AA),
        warningBg = Color(0xFFFDF2E9),
        warningBorder = Color(0xFFEA580C),
        warningText = Color(0xFF9A3412),
        successBg = Color(0xFFF0FDF4),
        successBorder = Color(0xFF16A34A),
        successText = Color(0xFF166534),
        watermarkColor = Color(0x0F451A03)
    ),
    DARK_PRO(
        label = "Sombre Nuit Pro",
        canvasBg = Color(0xFF060B18),
        sheetBg = Color(0xFF0F1A30),
        sheetBorder = Color(0xFF22355A),
        sheetShadow = Color(0x99000000),
        textPrimary = Color(0xFFF8FAFC),
        textSecondary = Color(0xFF94A3B8),
        textMuted = Color(0xFF64748B),
        accent = Color(0xFF3B82F6),
        highlightColor = Color(0xFF1E3A8A),
        warningBg = Color(0xFF281315),
        warningBorder = Color(0xFFEF4444),
        warningText = Color(0xFFFCA5A5),
        successBg = Color(0xFF0B291D),
        successBorder = Color(0xFF10B981),
        successText = Color(0xFF6EE7B7),
        watermarkColor = Color(0x12FFFFFF)
    ),
    OLED_BLACK(
        label = "Noir OLED Éco",
        canvasBg = Color(0xFF000000),
        sheetBg = Color(0xFF0A0A0A),
        sheetBorder = Color(0xFF262626),
        sheetShadow = Color(0xCC000000),
        textPrimary = Color(0xFFFAFAFA),
        textSecondary = Color(0xFFA3A3A3),
        textMuted = Color(0xFF737373),
        accent = Color(0xFF38BDF8),
        highlightColor = Color(0xFF0369A1),
        warningBg = Color(0xFF1F0D0E),
        warningBorder = Color(0xFFF87171),
        warningText = Color(0xFFFECACA),
        successBg = Color(0xFF062117),
        successBorder = Color(0xFF34D399),
        successText = Color(0xFFA7F3D0),
        watermarkColor = Color(0x18FFFFFF)
    )
}

enum class ReaderViewMode(val label: String, val icon: ImageVector) {
    PAGINATED("Page par page", Icons.Default.AutoStories),
    CONTINUOUS("Défilement continu", Icons.Default.ViewStream)
}

enum class ReaderZoom(val label: String, val scale: Float, val fontSizeSp: Int, val lineHeightSp: Int) {
    COMPACT("85%", 0.85f, 13, 21),
    STANDARD("100%", 1.0f, 15, 25),
    COMFORT("115%", 1.15f, 17, 28),
    LARGE("130%", 1.30f, 19, 32),
    EXTRA_LARGE("145%", 1.45f, 21, 35)
}

enum class ReaderHighlightColor(val label: String, val color: Color) {
    YELLOW("Jaune Sécurité", Color(0xFFFEF08A)),
    BLUE("Bleu Info", Color(0xFFBAE6FD)),
    GREEN("Vert Conforme", Color(0xFFA7F3D0)),
    ORANGE("Orange Vigilance", Color(0xFFFED7AA))
}

data class ParsedPdfPage(
    val pageIndex: Int,
    val totalPages: Int,
    val subtitle: String,
    val sections: List<PdfSectionItem>
)

data class PdfSectionItem(
    val title: String?,
    val paragraphs: List<String>,
    val alerts: List<String>,
    val commitments: List<String>,
    val bullets: List<String>
)

fun parseDocumentToPdfPages(document: HseDocument): List<ParsedPdfPage> {
    val rawSections = document.content.split("\n\n").filter { it.isNotBlank() }
    val sectionItems = mutableListOf<PdfSectionItem>()

    var currentTitle: String? = null
    val currentParagraphs = mutableListOf<String>()
    val currentAlerts = mutableListOf<String>()
    val currentCommitments = mutableListOf<String>()
    val currentBullets = mutableListOf<String>()

    fun flushSection() {
        if (currentTitle != null || currentParagraphs.isNotEmpty() || currentAlerts.isNotEmpty() || currentCommitments.isNotEmpty() || currentBullets.isNotEmpty()) {
            sectionItems.add(
                PdfSectionItem(
                    title = currentTitle,
                    paragraphs = currentParagraphs.toList(),
                    alerts = currentAlerts.toList(),
                    commitments = currentCommitments.toList(),
                    bullets = currentBullets.toList()
                )
            )
            currentTitle = null
            currentParagraphs.clear()
            currentAlerts.clear()
            currentCommitments.clear()
            currentBullets.clear()
        }
    }

    rawSections.forEach { block ->
        val trimmed = block.trim()
        if (trimmed.startsWith("#")) {
            flushSection()
            currentTitle = trimmed.removePrefix("#").trim()
        } else if (trimmed.startsWith("⚠️")) {
            currentAlerts.add(trimmed.removePrefix("⚠️").trim())
        } else if (trimmed.startsWith("✅")) {
            currentCommitments.add(trimmed.removePrefix("✅").trim())
        } else {
            val lines = trimmed.split("\n")
            lines.forEach { line ->
                val lTrim = line.trim()
                if (lTrim.startsWith("-")) {
                    currentBullets.add(lTrim.removePrefix("-").trim())
                } else if (lTrim.isNotBlank()) {
                    currentParagraphs.add(lTrim)
                }
            }
        }
    }
    flushSection()

    if (sectionItems.isEmpty()) {
        sectionItems.add(
            PdfSectionItem(
                title = "Contenu Officiel",
                paragraphs = listOf(document.content),
                alerts = emptyList(),
                commitments = emptyList(),
                bullets = emptyList()
            )
        )
    }

    // Group into 2 to 3 balanced PDF pages
    val pages = mutableListOf<ParsedPdfPage>()
    val itemsPerPage = if (sectionItems.size <= 2) 1 else if (sectionItems.size <= 5) 2 else 3
    val chunks = sectionItems.chunked(itemsPerPage.coerceAtLeast(1))

    chunks.forEachIndexed { idx, chunk ->
        pages.add(
            ParsedPdfPage(
                pageIndex = idx + 1,
                totalPages = chunks.size,
                subtitle = when (idx) {
                    0 -> "Section I : Objet, Champ d'application & Définitions"
                    1 -> "Section II : Directives Techniques & Règles Opérationnelles"
                    2 -> "Section III : Contrôles, Sanctions & Dispositions Finales"
                    else -> "Section ${idx + 1}"
                },
                sections = chunk
            )
        )
    }

    return pages
}

@Composable
fun DocumentReaderDialog(
    document: HseDocument,
    viewModel: HseViewModel? = null,
    onDismiss: () -> Unit
) {
    var selectedTheme by remember { mutableStateOf(ReaderTheme.STUDIO_LIGHT) }
    var viewMode by remember { mutableStateOf(ReaderViewMode.PAGINATED) }
    var selectedZoom by remember { mutableStateOf(ReaderZoom.STANDARD) }
    var isHighlighterActive by remember { mutableStateOf(false) }
    var activeHighlightColor by remember { mutableStateOf(ReaderHighlightColor.YELLOW) }
    var isImmersiveMode by remember { mutableStateOf(false) }

    var currentPageIndex by remember { mutableIntStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }
    var showSecurityInfoDialog by remember { mutableStateOf(false) }
    var showSettingsSheet by remember { mutableStateOf(false) }
    var isAcknowledged by remember { mutableStateOf(false) }

    val pdfPages = remember(document) { parseDocumentToPdfPages(document) }
    val totalPages = pdfPages.size

    val scrollStateContinuous = rememberScrollState()
    val scrollStatePaginated = rememberScrollState()

    // Reset scroll when page changes
    LaunchedEffect(currentPageIndex) {
        scrollStatePaginated.scrollTo(0)
    }

    // Count search matches
    val matchCount = remember(searchQuery, document.content) {
        if (searchQuery.isBlank() || searchQuery.length < 2) 0
        else {
            val query = searchQuery.lowercase()
            val text = document.content.lowercase()
            var count = 0
            var idx = 0
            while (idx != -1) {
                idx = text.indexOf(query, idx)
                if (idx != -1) {
                    count++
                    idx += query.length
                }
            }
            count
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(selectedTheme.canvasBg)
        ) {
            val isCompactWidth = maxWidth < 380.dp
            val isTablet = maxWidth > 600.dp
            val sheetHorizontalPadding = if (isCompactWidth) 8.dp else if (isTablet) 24.dp else 14.dp
            val readingProgress = ((currentPageIndex + 1).toFloat() / totalPages.toFloat()).coerceIn(0f, 1f)

            Scaffold(
                containerColor = selectedTheme.canvasBg,
                topBar = {
                    if (!isImmersiveMode) {
                        Surface(
                            color = selectedTheme.sheetBg,
                            border = BorderStroke(1.dp, selectedTheme.sheetBorder),
                            shadowElevation = 4.dp
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .statusBarsPadding()
                            ) {
                                // Reading Progress Bar at the top edge
                                LinearProgressIndicator(
                                    progress = { readingProgress },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(3.dp),
                                    color = selectedTheme.accent,
                                    trackColor = selectedTheme.sheetBorder.copy(alpha = 0.4f)
                                )

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(
                                            horizontal = if (isCompactWidth) 8.dp else 12.dp,
                                            vertical = 6.dp
                                        ),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    // Left: Back button + Doc Meta & Security Badge
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        IconButton(
                                            onClick = onDismiss,
                                            modifier = Modifier
                                                .size(if (isCompactWidth) 34.dp else 38.dp)
                                                .background(selectedTheme.canvasBg.copy(alpha = 0.08f), CircleShape)
                                                .testTag("close_reader_button")
                                        ) {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                                contentDescription = "Fermer le lecteur",
                                                tint = selectedTheme.textPrimary,
                                                modifier = Modifier.size(if (isCompactWidth) 18.dp else 20.dp)
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(if (isCompactWidth) 6.dp else 10.dp))

                                        Column {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Surface(
                                                    color = Color(0xFFDC2626).copy(alpha = 0.12f),
                                                    shape = RoundedCornerShape(4.dp),
                                                    border = BorderStroke(1.dp, Color(0xFFDC2626).copy(alpha = 0.3f))
                                                ) {
                                                    Text(
                                                        text = "PDF HSE",
                                                        color = Color(0xFFDC2626),
                                                        fontSize = 9.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                    )
                                                }
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = document.referenceCode,
                                                    color = selectedTheme.textMuted,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                            Text(
                                                text = document.title,
                                                color = selectedTheme.textPrimary,
                                                fontSize = if (isCompactWidth) 12.sp else 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }

                                    // Right Action Controls (Search, Highlighter, Theme/Settings, Security DRM Info)
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(if (isCompactWidth) 2.dp else 4.dp)
                                    ) {
                                        // Search Toggle
                                        IconButton(
                                            onClick = { isSearchActive = !isSearchActive },
                                            modifier = Modifier.size(if (isCompactWidth) 32.dp else 36.dp)
                                        ) {
                                            Icon(
                                                imageVector = if (isSearchActive) Icons.Default.Close else Icons.Default.Search,
                                                contentDescription = "Rechercher",
                                                tint = if (isSearchActive) selectedTheme.accent else selectedTheme.textSecondary,
                                                modifier = Modifier.size(19.dp)
                                            )
                                        }

                                        // Highlighter Toggle
                                        IconButton(
                                            onClick = { isHighlighterActive = !isHighlighterActive },
                                            modifier = Modifier
                                                .size(if (isCompactWidth) 32.dp else 36.dp)
                                                .background(
                                                    if (isHighlighterActive) activeHighlightColor.color.copy(alpha = 0.45f) else Color.Transparent,
                                                    CircleShape
                                                )
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.BorderColor,
                                                contentDescription = "Surligneur",
                                                tint = if (isHighlighterActive) selectedTheme.accent else selectedTheme.textMuted,
                                                modifier = Modifier.size(17.dp)
                                            )
                                        }

                                        // Reader Preferences (Zoom & Themes)
                                        IconButton(
                                            onClick = { showSettingsSheet = true },
                                            modifier = Modifier.size(if (isCompactWidth) 32.dp else 36.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.FormatSize,
                                                contentDescription = "Affichage et typographie",
                                                tint = selectedTheme.textSecondary,
                                                modifier = Modifier.size(19.dp)
                                            )
                                        }

                                        // Security DRM Notice (Replaces Download)
                                        IconButton(
                                            onClick = { showSecurityInfoDialog = true },
                                            modifier = Modifier
                                                .size(if (isCompactWidth) 32.dp else 36.dp)
                                                .background(Color(0xFF0284C7).copy(alpha = 0.12f), CircleShape)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Lock,
                                                contentDescription = "Document Sécurisé & Protégé (Téléchargement désactivé)",
                                                tint = Color(0xFF0284C7),
                                                modifier = Modifier.size(17.dp)
                                            )
                                        }

                                        // Immersive Fullscreen Focus Mode
                                        IconButton(
                                            onClick = { isImmersiveMode = true },
                                            modifier = Modifier.size(if (isCompactWidth) 32.dp else 36.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Fullscreen,
                                                contentDescription = "Mode Plein Écran Immersif",
                                                tint = selectedTheme.textSecondary,
                                                modifier = Modifier.size(19.dp)
                                            )
                                        }
                                    }
                                }

                                // Interactive Search Bar
                                AnimatedVisibility(
                                    visible = isSearchActive,
                                    enter = expandVertically() + fadeIn(),
                                    exit = shrinkVertically() + fadeOut()
                                ) {
                                    Surface(
                                        color = selectedTheme.canvasBg.copy(alpha = 0.05f),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 6.dp),
                                        shape = RoundedCornerShape(10.dp),
                                        border = BorderStroke(1.dp, selectedTheme.sheetBorder)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Search,
                                                contentDescription = null,
                                                tint = selectedTheme.accent,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            TextField(
                                                value = searchQuery,
                                                onValueChange = { searchQuery = it },
                                                placeholder = {
                                                    Text(
                                                        "Chercher dans le document...",
                                                        color = selectedTheme.textMuted,
                                                        fontSize = 12.sp
                                                    )
                                                },
                                                colors = TextFieldDefaults.colors(
                                                    focusedContainerColor = Color.Transparent,
                                                    unfocusedContainerColor = Color.Transparent,
                                                    focusedIndicatorColor = Color.Transparent,
                                                    unfocusedIndicatorColor = Color.Transparent,
                                                    focusedTextColor = selectedTheme.textPrimary,
                                                    unfocusedTextColor = selectedTheme.textPrimary
                                                ),
                                                singleLine = true,
                                                modifier = Modifier.weight(1f)
                                            )

                                            if (searchQuery.isNotBlank()) {
                                                Surface(
                                                    color = if (matchCount > 0) selectedTheme.accent.copy(alpha = 0.15f) else Color(0xFFDC2626).copy(alpha = 0.15f),
                                                    shape = RoundedCornerShape(6.dp),
                                                    modifier = Modifier.padding(end = 6.dp)
                                                ) {
                                                    Text(
                                                        text = if (matchCount > 0) "$matchCount résultat${if (matchCount > 1) "s" else ""}" else "0 résultat",
                                                        color = if (matchCount > 0) selectedTheme.accent else Color(0xFFDC2626),
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                                    )
                                                }

                                                IconButton(
                                                    onClick = { searchQuery = "" },
                                                    modifier = Modifier.size(24.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Close,
                                                        contentDescription = "Effacer",
                                                        tint = selectedTheme.textMuted,
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                // Highlighter Swatches Palette (if active)
                                AnimatedVisibility(
                                    visible = isHighlighterActive,
                                    enter = expandVertically() + fadeIn(),
                                    exit = shrinkVertically() + fadeOut()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(selectedTheme.sheetBg)
                                            .padding(horizontal = 16.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            "Couleur du marqueur :",
                                            color = selectedTheme.textMuted,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium
                                        )

                                        ReaderHighlightColor.values().forEach { hlColor ->
                                            val isSelected = activeHighlightColor == hlColor
                                            Box(
                                                modifier = Modifier
                                                    .size(24.dp)
                                                    .background(hlColor.color, CircleShape)
                                                    .border(
                                                        width = if (isSelected) 2.dp else 1.dp,
                                                        color = if (isSelected) selectedTheme.textPrimary else Color.Black.copy(alpha = 0.2f),
                                                        shape = CircleShape
                                                    )
                                                    .clickable { activeHighlightColor = hlColor },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                if (isSelected) {
                                                    Icon(
                                                        Icons.Default.Check,
                                                        contentDescription = null,
                                                        tint = Color.Black,
                                                        modifier = Modifier.size(14.dp)
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
            ) { paddingValues ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(if (isImmersiveMode) PaddingValues(0.dp) else paddingValues)
                        .background(selectedTheme.canvasBg)
                ) {
                    // Floating Exit Immersive Mode Pill
                    if (isImmersiveMode) {
                        Surface(
                            color = selectedTheme.sheetBg.copy(alpha = 0.92f),
                            shape = RoundedCornerShape(20.dp),
                            border = BorderStroke(1.dp, selectedTheme.sheetBorder),
                            shadowElevation = 8.dp,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .statusBarsPadding()
                                .padding(top = 12.dp, end = 12.dp)
                                .clickable { isImmersiveMode = false }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FullscreenExit,
                                    contentDescription = "Quitter plein écran",
                                    tint = selectedTheme.accent,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Quitter plein écran",
                                    color = selectedTheme.textPrimary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // PDF Viewport (Paginated vs Continuous)
                    if (viewMode == ReaderViewMode.PAGINATED) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(scrollStatePaginated)
                                .padding(horizontal = sheetHorizontalPadding, vertical = 14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            val currentPage = pdfPages.getOrElse(currentPageIndex) { pdfPages[0] }

                            PdfSheetComponent(
                                document = document,
                                page = currentPage,
                                theme = selectedTheme,
                                zoom = selectedZoom,
                                isHighlighterActive = isHighlighterActive,
                                highlightColor = activeHighlightColor.color,
                                searchQuery = searchQuery,
                                isLastPage = currentPageIndex == totalPages - 1,
                                isAcknowledged = isAcknowledged,
                                onAcknowledge = {
                                    isAcknowledged = true
                                    viewModel?.recordDocumentAttestation(document.id, document.title, document.referenceCode)
                                },
                                isCompact = isCompactWidth
                            )

                            Spacer(modifier = Modifier.height(110.dp))
                        }
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(scrollStateContinuous)
                                .padding(horizontal = sheetHorizontalPadding, vertical = 14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            pdfPages.forEachIndexed { index, page ->
                                PdfSheetComponent(
                                    document = document,
                                    page = page,
                                    theme = selectedTheme,
                                    zoom = selectedZoom,
                                    isHighlighterActive = isHighlighterActive,
                                    highlightColor = activeHighlightColor.color,
                                    searchQuery = searchQuery,
                                    isLastPage = index == totalPages - 1,
                                    isAcknowledged = isAcknowledged,
                                    onAcknowledge = {
                                        isAcknowledged = true
                                        viewModel?.recordDocumentAttestation(document.id, document.title, document.referenceCode)
                                    },
                                    isCompact = isCompactWidth
                                )

                                if (index < totalPages - 1) {
                                    Spacer(modifier = Modifier.height(18.dp))
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(vertical = 4.dp)
                                    ) {
                                        HorizontalDivider(
                                            modifier = Modifier.width(40.dp),
                                            color = selectedTheme.sheetBorder.copy(alpha = 0.5f)
                                        )
                                        Text(
                                            text = "  Feuille ${index + 1} / $totalPages  ",
                                            color = selectedTheme.textMuted,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        HorizontalDivider(
                                            modifier = Modifier.width(40.dp),
                                            color = selectedTheme.sheetBorder.copy(alpha = 0.5f)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(18.dp))
                                }
                            }

                            Spacer(modifier = Modifier.height(110.dp))
                        }
                    }

                    // Floating PDF Bottom Controller
                    if (!isImmersiveMode) {
                        Surface(
                            color = selectedTheme.sheetBg,
                            shape = RoundedCornerShape(24.dp),
                            border = BorderStroke(1.dp, selectedTheme.sheetBorder),
                            shadowElevation = 12.dp,
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(
                                    bottom = 16.dp,
                                    start = if (isCompactWidth) 8.dp else 16.dp,
                                    end = if (isCompactWidth) 8.dp else 16.dp
                                )
                                .navigationBarsPadding()
                        ) {
                            Row(
                                modifier = Modifier.padding(
                                    horizontal = if (isCompactWidth) 8.dp else 12.dp,
                                    vertical = 6.dp
                                ),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(if (isCompactWidth) 4.dp else 8.dp)
                            ) {
                                // Page Navigation Controls (Paginated)
                                if (viewMode == ReaderViewMode.PAGINATED) {
                                    IconButton(
                                        onClick = {
                                            if (currentPageIndex > 0) currentPageIndex--
                                        },
                                        enabled = currentPageIndex > 0,
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                            contentDescription = "Page précédente",
                                            tint = if (currentPageIndex > 0) selectedTheme.textPrimary else selectedTheme.textMuted.copy(alpha = 0.3f),
                                            modifier = Modifier.size(17.dp)
                                        )
                                    }

                                    Surface(
                                        color = selectedTheme.canvasBg.copy(alpha = 0.08f),
                                        shape = RoundedCornerShape(8.dp),
                                        border = BorderStroke(1.dp, selectedTheme.sheetBorder)
                                    ) {
                                        Text(
                                            text = "${currentPageIndex + 1} / $totalPages",
                                            color = selectedTheme.textPrimary,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }

                                    IconButton(
                                        onClick = {
                                            if (currentPageIndex < totalPages - 1) currentPageIndex++
                                        },
                                        enabled = currentPageIndex < totalPages - 1,
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                            contentDescription = "Page suivante",
                                            tint = if (currentPageIndex < totalPages - 1) selectedTheme.textPrimary else selectedTheme.textMuted.copy(alpha = 0.3f),
                                            modifier = Modifier.size(17.dp)
                                        )
                                    }

                                    VerticalDivider(
                                        modifier = Modifier.height(18.dp),
                                        color = selectedTheme.sheetBorder
                                    )
                                }

                                // Quick Zoom A- / A+
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = {
                                            val zooms = ReaderZoom.values()
                                            val idx = zooms.indexOf(selectedZoom)
                                            if (idx > 0) selectedZoom = zooms[idx - 1]
                                        },
                                        enabled = selectedZoom != ReaderZoom.COMPACT,
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Text(
                                            text = "A-",
                                            color = if (selectedZoom != ReaderZoom.COMPACT) selectedTheme.textPrimary else selectedTheme.textMuted.copy(alpha = 0.3f),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp
                                        )
                                    }

                                    Text(
                                        text = selectedZoom.label,
                                        color = selectedTheme.textSecondary,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )

                                    IconButton(
                                        onClick = {
                                            val zooms = ReaderZoom.values()
                                            val idx = zooms.indexOf(selectedZoom)
                                            if (idx < zooms.size - 1) selectedZoom = zooms[idx + 1]
                                        },
                                        enabled = selectedZoom != ReaderZoom.EXTRA_LARGE,
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Text(
                                            text = "A+",
                                            color = if (selectedZoom != ReaderZoom.EXTRA_LARGE) selectedTheme.textPrimary else selectedTheme.textMuted.copy(alpha = 0.3f),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        )
                                    }
                                }

                                VerticalDivider(
                                    modifier = Modifier.height(18.dp),
                                    color = selectedTheme.sheetBorder
                                )

                                // View Mode Toggle (Paginated vs Continuous)
                                IconButton(
                                    onClick = {
                                        viewMode = if (viewMode == ReaderViewMode.PAGINATED) ReaderViewMode.CONTINUOUS else ReaderViewMode.PAGINATED
                                    },
                                    modifier = Modifier.size(30.dp)
                                ) {
                                    Icon(
                                        imageVector = viewMode.icon,
                                        contentDescription = "Basculer mode d'affichage",
                                        tint = selectedTheme.textSecondary,
                                        modifier = Modifier.size(17.dp)
                                    )
                                }

                                // Quick Theme Cycle
                                IconButton(
                                    onClick = {
                                        selectedTheme = when (selectedTheme) {
                                            ReaderTheme.STUDIO_LIGHT -> ReaderTheme.WARM_SEPIA
                                            ReaderTheme.WARM_SEPIA -> ReaderTheme.DARK_PRO
                                            ReaderTheme.DARK_PRO -> ReaderTheme.OLED_BLACK
                                            ReaderTheme.OLED_BLACK -> ReaderTheme.STUDIO_LIGHT
                                        }
                                    },
                                    modifier = Modifier
                                        .size(30.dp)
                                        .background(selectedTheme.canvasBg.copy(alpha = 0.1f), CircleShape)
                                ) {
                                    Icon(
                                        imageVector = when (selectedTheme) {
                                            ReaderTheme.STUDIO_LIGHT -> Icons.Default.LightMode
                                            ReaderTheme.WARM_SEPIA -> Icons.Default.Palette
                                            ReaderTheme.DARK_PRO -> Icons.Default.DarkMode
                                            ReaderTheme.OLED_BLACK -> Icons.Default.Contrast
                                        },
                                        contentDescription = "Changer le style de papier",
                                        tint = selectedTheme.accent,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Security DRM & Protection Dialog (Explains No Download Policy)
                    if (showSecurityInfoDialog) {
                        AlertDialog(
                            onDismissRequest = { showSecurityInfoDialog = false },
                            containerColor = selectedTheme.sheetBg,
                            title = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .background(Color(0xFF0284C7).copy(alpha = 0.15f), RoundedCornerShape(8.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Lock,
                                            contentDescription = null,
                                            tint = Color(0xFF0284C7),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = "Consultation Sécurisée In-App",
                                        color = selectedTheme.textPrimary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp
                                    )
                                }
                            },
                            text = {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Text(
                                        text = "Document officiel : ${document.title}",
                                        color = selectedTheme.textPrimary,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Réf. ${document.referenceCode} • ${document.version} • CIMENCAM S.A.",
                                        color = selectedTheme.textMuted,
                                        fontSize = 11.sp
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))

                                    Surface(
                                        color = selectedTheme.canvasBg.copy(alpha = 0.06f),
                                        shape = RoundedCornerShape(10.dp),
                                        border = BorderStroke(1.dp, selectedTheme.sheetBorder)
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = Icons.Default.Shield,
                                                    contentDescription = null,
                                                    tint = Color(0xFF0284C7),
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = "TÉLÉCHARGEMENT & EXPORT DÉSACTIVÉS",
                                                    color = Color(0xFF0284C7),
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Black
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text(
                                                text = "Afin de garantir la conformité aux référentiels ISO 45001 & ISO 14001 et d'éviter la prolifération de versions obsolètes non contrôlées, les documents HSE internes sont réservés à la lecture sécurisée en ligne.",
                                                color = selectedTheme.textSecondary,
                                                fontSize = 11.sp,
                                                lineHeight = 16.sp
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    Surface(
                                        color = selectedTheme.successBg,
                                        shape = RoundedCornerShape(8.dp),
                                        border = BorderStroke(1.dp, selectedTheme.successBorder.copy(alpha = 0.4f))
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Verified,
                                                contentDescription = null,
                                                tint = selectedTheme.successBorder,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "Cette version affichée est la copie officielle certifiée en vigueur sur l'ensemble des sites CIMENCAM.",
                                                color = selectedTheme.successText,
                                                fontSize = 11.sp
                                            )
                                        }
                                    }
                                }
                            },
                            confirmButton = {
                                Button(
                                    onClick = { showSecurityInfoDialog = false },
                                    colors = ButtonDefaults.buttonColors(containerColor = selectedTheme.accent)
                                ) {
                                    Text("J'ai compris", fontWeight = FontWeight.Bold)
                                }
                            }
                        )
                    }

                    // Preferences & Typography Bottom Sheet / Dialog
                    if (showSettingsSheet) {
                        AlertDialog(
                            onDismissRequest = { showSettingsSheet = false },
                            containerColor = selectedTheme.sheetBg,
                            title = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Tune, contentDescription = null, tint = selectedTheme.accent, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Préférences de Lecture", color = selectedTheme.textPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                }
                            },
                            text = {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Text("Thème & Papier :", color = selectedTheme.textMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(8.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        ReaderTheme.values().forEach { th ->
                                            val isSelected = selectedTheme == th
                                            Surface(
                                                color = if (isSelected) selectedTheme.accent.copy(alpha = 0.15f) else selectedTheme.canvasBg.copy(alpha = 0.05f),
                                                shape = RoundedCornerShape(8.dp),
                                                border = BorderStroke(
                                                    1.dp,
                                                    if (isSelected) selectedTheme.accent else selectedTheme.sheetBorder
                                                ),
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .clickable { selectedTheme = th }
                                            ) {
                                                Column(
                                                    modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
                                                    horizontalAlignment = Alignment.CenterHorizontally
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(22.dp)
                                                            .background(th.sheetBg, CircleShape)
                                                            .border(1.dp, th.sheetBorder, CircleShape)
                                                    )
                                                    Spacer(modifier = Modifier.height(4.dp))
                                                    Text(
                                                        text = th.label.split(" ").first(),
                                                        color = if (isSelected) selectedTheme.accent else selectedTheme.textPrimary,
                                                        fontSize = 10.sp,
                                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                        maxLines = 1
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(16.dp))

                                    Text("Taille du texte & Zoom :", color = selectedTheme.textMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(8.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        ReaderZoom.values().forEach { z ->
                                            val isSelected = selectedZoom == z
                                            Surface(
                                                color = if (isSelected) selectedTheme.accent else selectedTheme.canvasBg.copy(alpha = 0.05f),
                                                shape = RoundedCornerShape(8.dp),
                                                border = BorderStroke(1.dp, if (isSelected) selectedTheme.accent else selectedTheme.sheetBorder),
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .clickable { selectedZoom = z }
                                            ) {
                                                Text(
                                                    text = z.label,
                                                    color = if (isSelected) Color.White else selectedTheme.textPrimary,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    textAlign = TextAlign.Center,
                                                    modifier = Modifier.padding(vertical = 8.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            },
                            confirmButton = {
                                TextButton(onClick = { showSettingsSheet = false }) {
                                    Text("Appliquer", color = selectedTheme.accent, fontWeight = FontWeight.Bold)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Authentic Responsive A4 PDF Sheet Composable with Corporate Letterhead, Structured Metadata, Content Blocks, Highlighting, and Official Stamp
 */
@Composable
fun PdfSheetComponent(
    document: HseDocument,
    page: ParsedPdfPage,
    theme: ReaderTheme,
    zoom: ReaderZoom,
    isHighlighterActive: Boolean,
    highlightColor: Color,
    searchQuery: String,
    isLastPage: Boolean,
    isAcknowledged: Boolean,
    onAcknowledge: () -> Unit,
    isCompact: Boolean = false
) {
    Surface(
        color = theme.sheetBg,
        shape = RoundedCornerShape(if (isCompact) 12.dp else 16.dp),
        border = BorderStroke(1.dp, theme.sheetBorder),
        shadowElevation = 8.dp,
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 700.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            // Subtle Security Watermark in background
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 80.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "CIMENCAM • SÉCURITÉ INTERNE",
                    color = theme.watermarkColor,
                    fontSize = if (isCompact) 18.sp else 24.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp
                )
                Spacer(modifier = Modifier.height(140.dp))
                Text(
                    text = "DIFFUSION INTERNE STRICTE • NON EXPORTABLE",
                    color = theme.watermarkColor,
                    fontSize = if (isCompact) 13.sp else 16.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.5.sp
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = if (isCompact) 14.dp else 22.dp,
                        vertical = if (isCompact) 14.dp else 20.dp
                    )
            ) {
                // 1. Corporate Official Letterhead
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(if (isCompact) 30.dp else 34.dp)
                                .background(Color(0xFF0F2B48), RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(if (isCompact) 16.dp else 18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "CIMENCAM • GROUPE HOLCIM",
                                color = Color(0xFF0F2B48),
                                fontSize = if (isCompact) 10.sp else 11.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                text = "DIRECTION HYGIÈNE, SÉCURITÉ & ENVIRONNEMENT",
                                color = theme.textMuted,
                                fontSize = if (isCompact) 8.sp else 9.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.2.sp
                            )
                        }
                    }

                    Surface(
                        color = theme.canvasBg.copy(alpha = 0.08f),
                        shape = RoundedCornerShape(6.dp),
                        border = BorderStroke(1.dp, theme.sheetBorder)
                    ) {
                        Text(
                            text = "RÉF. ${document.referenceCode}",
                            color = theme.textPrimary,
                            fontSize = if (isCompact) 9.sp else 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = theme.sheetBorder, thickness = 1.dp)
                Spacer(modifier = Modifier.height(14.dp))

                // 2. Page 1 Hero Header & Official Doc Metadata Table
                if (page.pageIndex == 1) {
                    Surface(
                        color = theme.accent.copy(alpha = 0.06f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, theme.accent.copy(alpha = 0.25f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(if (isCompact) 10.dp else 14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = document.category.uppercase(),
                                    color = theme.accent,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 0.8.sp
                                )
                                Text(
                                    text = document.version,
                                    color = theme.textMuted,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = document.title,
                                color = theme.textPrimary,
                                fontSize = (zoom.fontSizeSp + if (isCompact) 4 else 6).sp,
                                lineHeight = (zoom.lineHeightSp + if (isCompact) 4 else 6).sp,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = document.subtitle,
                                color = theme.textSecondary,
                                fontSize = (zoom.fontSizeSp - 2).sp,
                                lineHeight = (zoom.lineHeightSp - 4).sp
                            )

                            Spacer(modifier = Modifier.height(10.dp))
                            HorizontalDivider(color = theme.sheetBorder.copy(alpha = 0.5f), thickness = 1.dp)
                            Spacer(modifier = Modifier.height(10.dp))

                            // Metadata Grid
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text("Auteur :", color = theme.textMuted, fontSize = 9.sp)
                                    Text(document.author, color = theme.textPrimary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                                Column {
                                    Text("Temps estimé :", color = theme.textMuted, fontSize = 9.sp)
                                    Text("${document.readingTimeMinutes} min", color = theme.textPrimary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                                Column {
                                    Text("Conformité :", color = theme.textMuted, fontSize = 9.sp)
                                    Text("ISO 45001 & 14001", color = theme.successBorder, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Executive Summary Box
                    if (document.summary.isNotBlank()) {
                        Surface(
                            color = theme.canvasBg.copy(alpha = 0.04f),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, theme.sheetBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(if (isCompact) 10.dp else 12.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Box(
                                    modifier = Modifier
                                        .width(3.dp)
                                        .height(30.dp)
                                        .background(theme.accent, RoundedCornerShape(2.dp))
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "RÉSUMÉ EXÉCUTIF",
                                        color = theme.accent,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.5.sp
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = document.summary,
                                        color = theme.textPrimary,
                                        fontSize = (zoom.fontSizeSp - 2).sp,
                                        lineHeight = (zoom.lineHeightSp - 4).sp
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                    }
                } else {
                    // Page Section Subtitle Indicator for pages > 1
                    Surface(
                        color = theme.canvasBg.copy(alpha = 0.05f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = page.subtitle,
                            color = theme.accent,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                }

                // 3. Document Sections & Clauses
                page.sections.forEach { section ->
                    // Section Title
                    section.title?.let { secTitle ->
                        Surface(
                            color = theme.accent.copy(alpha = 0.08f),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, theme.accent.copy(alpha = 0.2f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Bookmark,
                                    contentDescription = null,
                                    tint = theme.accent,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = secTitle,
                                    color = theme.textPrimary,
                                    fontSize = (zoom.fontSizeSp + 1).sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // Safety Alerts (⚠️)
                    section.alerts.forEach { alertText ->
                        Surface(
                            color = theme.warningBg,
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, theme.warningBorder),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(if (isCompact) 10.dp else 12.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = theme.warningBorder,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "RÈGLE CRITIQUE NON-NÉGOCIABLE",
                                        color = theme.warningBorder,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.5.sp
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    HighlightableText(
                                        text = alertText,
                                        textColor = theme.warningText,
                                        searchQuery = searchQuery,
                                        isHighlighterActive = isHighlighterActive,
                                        highlightBg = highlightColor,
                                        fontSizeSp = zoom.fontSizeSp,
                                        lineHeightSp = zoom.lineHeightSp
                                    )
                                }
                            }
                        }
                    }

                    // ISO Commitments (✅)
                    section.commitments.forEach { commText ->
                        Surface(
                            color = theme.successBg,
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, theme.successBorder),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(if (isCompact) 10.dp else 12.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = theme.successBorder,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                HighlightableText(
                                    text = commText,
                                    textColor = theme.successText,
                                    searchQuery = searchQuery,
                                    isHighlighterActive = isHighlighterActive,
                                    highlightBg = highlightColor,
                                    fontSizeSp = zoom.fontSizeSp,
                                    lineHeightSp = zoom.lineHeightSp
                                )
                            }
                        }
                    }

                    // Paragraphs
                    section.paragraphs.forEach { pText ->
                        Box(modifier = Modifier.padding(vertical = 4.dp)) {
                            HighlightableText(
                                text = pText,
                                textColor = theme.textPrimary,
                                searchQuery = searchQuery,
                                isHighlighterActive = isHighlighterActive,
                                highlightBg = highlightColor,
                                fontSizeSp = zoom.fontSizeSp,
                                lineHeightSp = zoom.lineHeightSp
                            )
                        }
                    }

                    // Bullet points
                    section.bullets.forEach { bText ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Box(
                                modifier = Modifier
                                    .padding(top = 8.dp)
                                    .size(5.dp)
                                    .background(theme.accent, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            HighlightableText(
                                text = bText,
                                textColor = theme.textPrimary,
                                searchQuery = searchQuery,
                                isHighlighterActive = isHighlighterActive,
                                highlightBg = highlightColor,
                                fontSizeSp = zoom.fontSizeSp,
                                lineHeightSp = zoom.lineHeightSp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // 4. Official Seal & Attestation (On Last Page)
                if (isLastPage) {
                    Surface(
                        color = theme.canvasBg.copy(alpha = 0.04f),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, if (isAcknowledged) theme.successBorder else theme.sheetBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(if (isCompact) 12.dp else 16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .background(
                                        if (isAcknowledged) theme.successBorder.copy(alpha = 0.15f) else theme.accent.copy(alpha = 0.12f),
                                        CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isAcknowledged) Icons.Default.Verified else Icons.Default.FactCheck,
                                    contentDescription = null,
                                    tint = if (isAcknowledged) theme.successBorder else theme.accent,
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = if (isAcknowledged) "Document Attesté & Validé" else "Attestation de Lecture Obligatoire",
                                color = theme.textPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = if (isAcknowledged)
                                    "Votre prise de connaissance a été enregistrée et horodatée pour votre traçabilité."
                                else
                                    "En tant qu'agent ou intervenant CIMENCAM, confirmez avoir lu et compris l'intégralité de ces prescriptions.",
                                color = theme.textSecondary,
                                fontSize = 11.sp,
                                textAlign = TextAlign.Center,
                                lineHeight = 16.sp
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            if (!isAcknowledged) {
                                Button(
                                    onClick = onAcknowledge,
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = theme.accent),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("acknowledge_document_button")
                                ) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("J'atteste avoir lu et compris", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                            } else {
                                val todayFormatted = remember {
                                    SimpleDateFormat("dd/MM/yyyy 'à' HH:mm", java.util.Locale.getDefault()).format(Date())
                                }
                                Surface(
                                    color = theme.successBg,
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(1.dp, theme.successBorder)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = theme.successBorder,
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Certifié conforme le $todayFormatted",
                                            color = theme.successText,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                }

                // 5. Official PDF Page Footer
                HorizontalDivider(color = theme.sheetBorder, thickness = 1.dp)
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "CIMENCAM S.A. • Document interne confidentiel",
                        color = theme.textMuted,
                        fontSize = if (isCompact) 8.sp else 9.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Page ${page.pageIndex} sur ${page.totalPages}",
                        color = theme.textPrimary,
                        fontSize = if (isCompact) 9.sp else 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

/**
 * Text renderer with interactive search highlighting and keyword highlighter effect
 */
@Composable
fun HighlightableText(
    text: String,
    textColor: Color,
    searchQuery: String,
    isHighlighterActive: Boolean,
    highlightBg: Color,
    fontSizeSp: Int,
    lineHeightSp: Int
) {
    if (searchQuery.isNotBlank() && text.contains(searchQuery, ignoreCase = true)) {
        val annotated = buildAnnotatedString {
            var startIndex = 0
            val lowerText = text.lowercase()
            val lowerQuery = searchQuery.lowercase()

            while (startIndex < text.length) {
                val matchIndex = lowerText.indexOf(lowerQuery, startIndex)
                if (matchIndex == -1) {
                    append(text.substring(startIndex))
                    break
                } else {
                    if (matchIndex > startIndex) {
                        append(text.substring(startIndex, matchIndex))
                    }
                    withStyle(
                        SpanStyle(
                            background = Color(0xFFFDE047),
                            color = Color(0xFF000000),
                            fontWeight = FontWeight.Bold
                        )
                    ) {
                        append(text.substring(matchIndex, matchIndex + lowerQuery.length))
                    }
                    startIndex = matchIndex + lowerQuery.length
                }
            }
        }
        Text(
            text = annotated,
            color = textColor,
            fontSize = fontSizeSp.sp,
            lineHeight = lineHeightSp.sp
        )
    } else {
        Text(
            text = text,
            color = textColor,
            fontSize = fontSizeSp.sp,
            lineHeight = lineHeightSp.sp
        )
    }
}

// ==========================================
// THEMATIC POLICY BUTTONS & DEDICATED DIALOGS
// ==========================================

private data class PolicyButtonItem(
    val title: String,
    val subtitle: String,
    val badge: String,
    val icon: ImageVector,
    val accentColor: Color,
    val testTag: String,
    val filterCategory: String,
    val searchKeywords: String,
    val onClick: () -> Unit
)

@Composable
private fun PolicyGridCard(
    title: String,
    subtitle: String,
    badge: String,
    icon: ImageVector,
    accentColor: Color,
    testTag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier.testTag(testTag),
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF1E293B),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.40f)),
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 9.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .background(accentColor.copy(alpha = 0.18f), RoundedCornerShape(7.dp))
                        .border(1.dp, accentColor.copy(alpha = 0.45f), RoundedCornerShape(7.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Surface(
                    color = accentColor.copy(alpha = 0.18f),
                    shape = RoundedCornerShape(4.dp),
                    border = BorderStroke(0.6.dp, accentColor.copy(alpha = 0.45f))
                ) {
                    Text(
                        text = badge,
                        color = accentColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        maxLines = 1
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                color = Color.White,
                fontSize = 14.5.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                color = Color(0xFFF1F5F9),
                fontSize = 12.sp,
                lineHeight = 16.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                minLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun PolicyListCard(
    title: String,
    subtitle: String,
    badge: String,
    icon: ImageVector,
    accentColor: Color,
    testTag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier.testTag(testTag),
        shape = RoundedCornerShape(10.dp),
        color = Color(0xFF1E293B),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.40f)),
        shadowElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .background(accentColor.copy(alpha = 0.18f), RoundedCornerShape(6.dp))
                    .border(1.dp, accentColor.copy(alpha = 0.45f), RoundedCornerShape(6.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = title,
                        color = Color.White,
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Surface(
                        color = accentColor.copy(alpha = 0.18f),
                        shape = RoundedCornerShape(4.dp),
                        border = BorderStroke(0.6.dp, accentColor.copy(alpha = 0.40f))
                    ) {
                        Text(
                            text = badge,
                            color = accentColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = subtitle,
                    color = Color(0xFFF1F5F9),
                    fontSize = 12.5.sp,
                    lineHeight = 16.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .background(accentColor.copy(alpha = 0.15f), CircleShape)
                    .border(0.8.dp, accentColor.copy(alpha = 0.35f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Ouvrir",
                    tint = accentColor,
                    modifier = Modifier.size(12.dp)
                )
            }
        }
    }
}

@Composable
private fun PolicyNavigationCard(
    title: String,
    subtitle: String,
    badge: String,
    icon: ImageVector,
    accentColor: Color,
    testTag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    PolicyListCard(
        title = title,
        subtitle = subtitle,
        badge = badge,
        icon = icon,
        accentColor = accentColor,
        testTag = testTag,
        onClick = onClick,
        modifier = modifier
    )
}

private fun openWebLink(context: Context, url: String) {
    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        context.startActivity(intent)
    } catch (_: Exception) {
        Toast.makeText(context, "Impossible d'ouvrir le lien Drive", Toast.LENGTH_SHORT).show()
    }
}

// ------------------------------------------
// 1. DIALOG PLAN D'URGENCE
// ------------------------------------------
@Composable
fun PlanUrgenceDialog(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val emergencyFiches = HseStaticData.emergencyFiches
    var expandedFicheId by remember { mutableStateOf<String?>(null) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.5f)),
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.90f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .background(Color(0xFFEF4444).copy(alpha = 0.2f), CircleShape)
                                .border(1.dp, Color(0xFFEF4444).copy(alpha = 0.5f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = Color(0xFFEF4444),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Plan d'Urgence & Secours",
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Procédures immédiates & contacts 24/7",
                                color = Color(0xFFFCA5A5),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Fermer", tint = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Scrollable Content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Bouton Accès aux Documents (Google Drive)
                    Button(
                        onClick = {
                            openWebLink(context, "https://drive.google.com/drive/folders/1wMjQkrZrpnQ3sIq4SUdk0FO-74SfPGVI?usp=drive_link")
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Icon(Icons.Default.CloudQueue, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Accès aux documents",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }

                    // Emergency Reflex Fiches
                    Text(
                        text = "Fiches Réflexes d'Urgence :",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )

                    emergencyFiches.forEach { fiche ->
                        val isExpanded = expandedFicheId == fiche.id
                        Card(
                            onClick = { expandedFicheId = if (isExpanded) null else fiche.id },
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, if (isExpanded) Color(fiche.accentColorHex) else Color(0xFF334155))
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
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
                                                .background(Color(fiche.accentColorHex).copy(alpha = 0.2f), CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = when (fiche.iconType) {
                                                    "fire" -> Icons.Default.LocalFireDepartment
                                                    "ambulance" -> Icons.Default.MedicalServices
                                                    "flask" -> Icons.Default.Science
                                                    "bolt" -> Icons.Default.Bolt
                                                    "exit" -> Icons.Default.MeetingRoom
                                                    else -> Icons.Default.HealthAndSafety
                                                },
                                                contentDescription = null,
                                                tint = Color(fiche.accentColorHex),
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(
                                                text = fiche.title,
                                                color = Color.White,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = fiche.subtitle,
                                                color = Color(0xFFCBD5E1),
                                                fontSize = 12.sp
                                            )
                                        }
                                    }
                                    Icon(
                                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                        contentDescription = null,
                                        tint = Color.White
                                    )
                                }

                                if (isExpanded) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    HorizontalDivider(color = Color(0xFF334155))
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = when (fiche.id) {
                                            "accident_corporel" -> "1. Protéger : Baliser la zone et couper les énergies si nécessaire.\n2. Alerter : Contacter immédiatement l'infirmerie ou le poste de secours.\n3. Secourir : Pratiquer les gestes de premiers secours sans déplacer la victime sauf péril imminent.\n4. Accompagner : Rester auprès du blessé jusqu'à l'arrivée des secours."
                                            "incendie_evacuation" -> "1. Alerte : Déclencher le bris de glace le plus proche.\n2. Attaque : Tenter d'éteindre le feu naissant avec l'extincteur approprié si possible.\n3. Évacuation : Quitter les lieux calmement en fermant portes et fenêtres.\n4. Rassemblement : Rejoindre le point d'appel et répondre à l'appel nominal."
                                            "risques_chimiques" -> "1. Éloigner : Se placer vent debout par rapport au nuage ou déversement.\n2. Douche : En cas de contact, se rincer sous la douche de sécurité au moins 15 min.\n3. Rétention : Utiliser les kits absorbants disponibles en station.\n4. FDS : Consulter la Fiche de Données de Sécurité du produit."
                                            "risques_electriques" -> "1. Coupure : Actionner immédiatement l'arrêt d'urgence coup de poing.\n2. Ne jamais toucher la victime en contact avec la source de courant.\n3. Isoler avec un objet non conducteur (perche isolante certifiée).\n4. Alerter les électriciens et le service médical."
                                            "evacuation_generale" -> "1. Dès retentissement de la sirène modulée continue : stopper toute tâche.\n2. Sécuriser les installations si procédure prévue.\n3. Emprunter les allées de secours balisées sans courir.\n4. Point de rassemblement : ne quitter le lieu sous aucun prétexte sans autorisation."
                                            else -> "1. Vérifier l'état de conscience et la respiration de la victime.\n2. En cas d'inconscience mais respiration présente : Position Latérale de Sécurité (PLS).\n3. En cas d'arrêt cardiorespiratoire : 30 compressions thoraciques, 2 insufflations et DAE.\n4. Hémorragie : compression manuelle directe avec gant."
                                        },
                                        color = Color(0xFFF1F5F9),
                                        fontSize = 13.5.sp,
                                        lineHeight = 20.sp
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Close Button
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = HSEBluePrimary),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth().height(44.dp)
                ) {
                    Text("Fermer le Plan d'urgence", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }
    }
}

// ------------------------------------------
// 2. DIALOG ENVIRONNEMENT
// ------------------------------------------
@Composable
fun EnvironnementDialog(
    onDismiss: () -> Unit,
    onOpenDocument: (String) -> Unit = {}
) {
    val context = LocalContext.current
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.5f)),
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.85f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .background(Color(0xFF10B981).copy(alpha = 0.2f), CircleShape)
                                .border(1.dp, Color(0xFF10B981).copy(alpha = 0.5f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Eco, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(24.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Politique Environnementale", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            Text("Groupe Holcim • CIMENCAM 2026", color = Color(0xFF6EE7B7), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Fermer", tint = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        color = Color(0xFF064E3B).copy(alpha = 0.5f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.2.dp, Color(0xFF059669))
                    ) {
                        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Verified, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(22.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Engagement ISO 14001 • Transition Écologique & Économie Circulaire",
                                color = Color.White,
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // 4 Axes
                    val axes = listOf(
                        Triple("1. Décarbonation & Climat", "Remplacement des énergies fossiles par la biomasse locale (coques de cacao, balles de riz) et réduction de l'empreinte carbone.", Icons.Default.Co2),
                        Triple("2. Préservation de l'Eau", "Recyclage à 100% en circuit fermé de toutes les eaux industrielles et contrôle rigoureux des nappes.", Icons.Default.WaterDrop),
                        Triple("3. Économie Circulaire & Déchets", "Tri sélectif rigoureux, traitement certifié des déchets industriels et zéro mise en décharge sauvage.", Icons.Default.Autorenew),
                        Triple("4. Biodiversité & Carrières", "Plan de réhabilitation continue des fronts de carrières avec reboisement d'essences végétales locales.", Icons.Default.Park)
                    )

                    axes.forEach { (titre, desc, icon) ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.40f))
                        ) {
                            Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.Top) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .background(Color(0xFF10B981).copy(alpha = 0.2f), RoundedCornerShape(8.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(icon, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(22.dp))
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(titre, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(desc, color = Color(0xFFE2E8F0), fontSize = 13.5.sp, lineHeight = 18.5.sp)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Action: Accès aux Documents (Google Drive)
                Button(
                    onClick = {
                        openWebLink(context, "https://drive.google.com/drive/folders/1sxsnmiGZeYLJBUHFR_zBVutlaWCV6yFm?usp=drive_link")
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth().height(46.dp)
                ) {
                    Icon(Icons.Default.CloudQueue, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Accès aux documents", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
                }
            }
        }
    }
}

// ------------------------------------------
// 3. DIALOG INDUCTION
// ------------------------------------------
@Composable
fun InductionDialog(
    onDismiss: () -> Unit,
    onOpenDocument: (String) -> Unit = {}
) {
    val context = LocalContext.current
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, Color(0xFF2563EB).copy(alpha = 0.5f)),
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.85f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .background(Color(0xFF2563EB).copy(alpha = 0.2f), CircleShape)
                                .border(1.dp, Color(0xFF2563EB).copy(alpha = 0.5f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.School, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(24.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Induction Sécurité", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            Text("Nouveaux Arrivants & Prestataires", color = Color(0xFF93C5FD), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Fermer", tint = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        color = Color(0xFF1E3A8A).copy(alpha = 0.5f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.2.dp, Color(0xFF2563EB))
                    ) {
                        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Badge, contentDescription = null, tint = Color(0xFF60A5FA), modifier = Modifier.size(22.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Passage Obligatoire avant tout accès aux zones de production et carrières",
                                color = Color.White,
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    val etapes = listOf(
                        Pair("1. Procédure d'Accès Guérite", "Présentation obligatoire, vérification d'identité, badge visiteur et enregistrement dans le registre de sécurité."),
                        Pair("2. Port Obligatoire des EPI", "Casque avec jugulaire, lunettes de sécurité, chaussures S3 à semelle anti-perforation, gilet haute visibilité."),
                        Pair("3. Règles de Circulation", "Priorité piétonne sur trottoirs balisés jaune/vert, vitesse maximale 20 km/h, contact visuel obligatoire avec conducteurs d'engins."),
                        Pair("4. Procédure d'Alerte & Évacuation", "Reconnaissance de la sirène modulée, évacuation dans le calme vers le Point de Rassemblement désigné.")
                    )

                    etapes.forEach { (titre, desc) ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Color(0xFF2563EB).copy(alpha = 0.40f))
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(titre, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(desc, color = Color(0xFFE2E8F0), fontSize = 13.5.sp, lineHeight = 18.5.sp)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Action: Consulter le Livret d'Accueil complet (Google Drive)
                Button(
                    onClick = {
                        openWebLink(context, "https://drive.google.com/drive/folders/1o4YteiLSmnuYhiTB_JiAGrWVT5JfLMnS?usp=drive_link")
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth().height(46.dp)
                ) {
                    Icon(Icons.Default.MenuBook, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Consulter le Livret d'Accueil complet", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
                }
            }
        }
    }
}

// ------------------------------------------
// 4. DIALOG SANTÉ
// ------------------------------------------
@Composable
fun SanteDialog(
    onDismiss: () -> Unit,
    onOpenDocument: (String) -> Unit = {}
) {
    val context = LocalContext.current
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, Color(0xFFE11D48).copy(alpha = 0.5f)),
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.85f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .background(Color(0xFFE11D48).copy(alpha = 0.2f), CircleShape)
                                .border(1.dp, Color(0xFFE11D48).copy(alpha = 0.5f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Favorite, contentDescription = null, tint = Color(0xFFFB7185), modifier = Modifier.size(24.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Santé au Travail", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            Text("Prévention Sanitaire & Bien-être", color = Color(0xFFFDA4AF), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Fermer", tint = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        color = Color(0xFF881337).copy(alpha = 0.5f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.2.dp, Color(0xFFE11D48))
                    ) {
                        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.HealthAndSafety, contentDescription = null, tint = Color(0xFFFB7185), modifier = Modifier.size(22.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Protocoles Médicaux Obligatoires & Suivi de Santé des Collaborateurs",
                                color = Color.White,
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    val santePoints = listOf(
                        Pair("1. Protection Respiratoire & Poussières", "Port des masques FFP2/FFP3 obligatoire dans les zones de broyage, d'ensachage et de transfert de clinker et ciment."),
                        Pair("2. Prévention du Stress Thermique", "Hydratation minimale de 2,5 à 3L d'eau potable/jour lors des fortes chaleurs à Figuil/Nomayos et pauses de repos à l'ombre."),
                        Pair("3. Ergonomie & Manutention", "Charge maximale autorisée par opérateur : 25 kg. Adopter la flexion des genoux et le dos droit pour toute levée manuelle."),
                        Pair("4. Suivi Médical Périodique", "Visites annuelles obligatoires à l'infirmerie du site : audiométrie (bruit > 85dB), spirométrie et bilan biologique.")
                    )

                    santePoints.forEach { (titre, desc) ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Color(0xFFE11D48).copy(alpha = 0.40f))
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(titre, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(desc, color = Color(0xFFE2E8F0), fontSize = 13.5.sp, lineHeight = 18.5.sp)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Action: Consulter le Guide Sanitaire complet (Google Drive)
                Button(
                    onClick = {
                        openWebLink(context, "https://drive.google.com/drive/folders/1dQw0Ep93Xs-9qxoi53zj52oCltSEx24K?usp=drive_link")
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE11D48)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth().height(46.dp)
                ) {
                    Icon(Icons.Default.MedicalServices, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Consulter le Guide Sanitaire complet", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
                }
            }
        }
    }
}

// ------------------------------------------
// 5. DIALOG SÉCURITÉ
// ------------------------------------------
@Composable
fun SecuriteDialog(
    onDismiss: () -> Unit,
    onOpenDocument: (String) -> Unit = {}
) {
    val context = LocalContext.current
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, HSEBluePrimary.copy(alpha = 0.5f)),
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.88f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .background(HSEBluePrimary.copy(alpha = 0.2f), CircleShape)
                                .border(1.dp, HSEBluePrimary.copy(alpha = 0.5f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Shield, contentDescription = null, tint = HSEGreenSafe, modifier = Modifier.size(24.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Sécurité Industrielle", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            Text("Les 12 Règles d'Or & Standards Vitaux", color = Color(0xFF86EFAC), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Fermer", tint = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Fundamental Right
                    Surface(
                        color = Color(0xFF0F5132).copy(alpha = 0.5f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.2.dp, HSEGreenSafe)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text("⚠️ RÈGLE FONDAMENTALE : DROIT DE RETRAIT", color = Color.White, fontSize = 13.5.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Tout collaborateur a le droit et le DEVOIR d'interrompre immédiatement tout travail comportant un risque non maîtrisé, sans aucune sanction ni pénalité.",
                                color = Color.White,
                                fontSize = 13.sp,
                                lineHeight = 18.sp
                            )
                        }
                    }

                    // 12 Rules Overview
                    val regles = listOf(
                        "1. EPI" to "Port obligatoire dès la zone industrielle (Casque, S3, lunettes, gilet).",
                        "2. Consignation LOTO" to "Cadenassage et vérification d'absence totale d'énergie.",
                        "3. Travail en Hauteur" to "Harnais double longe raccordé dès 1,80 m.",
                        "4. Espaces Confinés" to "Permis obligatoire, ventilation et surveillant extérieur.",
                        "5. Conduite & Engins" to "Vitesse limitée à 20 km/h, ceinture, téléphone interdit.",
                        "6. Levage Mécanique" to "Rayon balisé, interdiction de stationner sous une charge.",
                        "7. Travaux à Chaud" to "Permis de feu obligatoire, surveillance 2h post-travaux.",
                        "8. Risques Chimiques" to "Consultation des FDS et bacs de rétention.",
                        "9. Voies d'Évacuation" to "Interdiction d'encombrer sorties, extincteurs et RIA.",
                        "10. Tolérance Zéro Alcool" to "Interdiction formelle de toute substance.",
                        "11. Droit d'Alerte" to "Interrompre immédiatement tout travail à risque grave.",
                        "12. Signalement sous 2h" to "Déclaration systématique de tout presqu'accident."
                    )

                    regles.forEach { (num, desc) ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, HSEBluePrimary.copy(alpha = 0.40f))
                        ) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(26.dp)
                                        .background(HSEBluePrimary.copy(alpha = 0.25f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = HSEGreenSafe, modifier = Modifier.size(16.dp))
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(num, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(desc, color = Color(0xFFE2E8F0), fontSize = 12.5.sp, lineHeight = 17.sp)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Action: Consulter le Guide de Sécurité (Google Drive)
                Button(
                    onClick = {
                        openWebLink(context, "https://drive.google.com/drive/folders/1c0Bxx2BhaxG4B3PnE2Eyf8-1WFb7KEAx?usp=drive_link")
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = HSEBluePrimary),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth().height(46.dp)
                ) {
                    Icon(Icons.Default.Shield, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Consulter le Guide de Sécurité", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
                }
            }
        }
    }
}

// ------------------------------------------
// 6. DIALOG PROGRAMME
// ------------------------------------------
@Composable
fun ProgrammeDialog(
    onDismiss: () -> Unit,
    onNavigateToProgramme: () -> Unit = {}
) {
    val context = LocalContext.current
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, Color(0xFF6366F1).copy(alpha = 0.5f)),
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.82f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .background(Color(0xFF6366F1).copy(alpha = 0.2f), CircleShape)
                                .border(1.dp, Color(0xFF6366F1).copy(alpha = 0.5f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = Color(0xFFA5B4FC), modifier = Modifier.size(24.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Programme HSE 2026", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            Text("Campagnes, Formations & Événements", color = Color(0xFFC7D2FE), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Fermer", tint = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        color = Color(0xFF312E81).copy(alpha = 0.5f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.2.dp, Color(0xFF6366F1))
                    ) {
                        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Assignment, contentDescription = null, tint = Color(0xFFA5B4FC), modifier = Modifier.size(22.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Plan d'Action HSE 2026 • Renforcement de la Culture de Vigilance Partagée",
                                color = Color.White,
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    val programmes = listOf(
                        Triple("Campagnes de Sécurité Annuelles", "Safety Days, Mois de l'Environnement, Semaine de la Santé & Célébration des records sans accident.", Icons.Default.Campaign),
                        Triple("Formations Obligatoires 2026", "Consignation LOTO, Permis Travail en Hauteur, CACES Chariots et Recyclage SST Premiers Secours.", Icons.Default.School),
                        Triple("Rituels Opérationnels Quotidiens", "Quarts d'heure HSE journaliers, causeries de poste et visites managériales VFL hebdomadaires.", Icons.Default.Schedule)
                    )

                    programmes.forEach { (titre, desc, icon) ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Color(0xFF6366F1).copy(alpha = 0.40f))
                        ) {
                            Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.Top) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .background(Color(0xFF6366F1).copy(alpha = 0.2f), RoundedCornerShape(8.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(icon, contentDescription = null, tint = Color(0xFFA5B4FC), modifier = Modifier.size(22.dp))
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(titre, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(desc, color = Color(0xFFE2E8F0), fontSize = 13.5.sp, lineHeight = 18.5.sp)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Action: Accéder au calendrier complet du Programme (Google Drive)
                Button(
                    onClick = {
                        openWebLink(context, "https://drive.google.com/drive/folders/1B-UQwjv3TlDJLcFeEOySQY4uagVGMpVP?usp=drive_link")
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth().height(46.dp)
                ) {
                    Icon(Icons.Default.EventNote, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Accéder au calendrier complet du Programme", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
                }
            }
        }
    }
}

// ------------------------------------------
// 7. DIALOG POLITIQUE HSE
// ------------------------------------------
@Composable
fun PolitiqueDialog(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, Color(0xFF0284C7).copy(alpha = 0.5f)),
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.85f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .background(Color(0xFF0284C7).copy(alpha = 0.2f), CircleShape)
                                .border(1.dp, Color(0xFF0284C7).copy(alpha = 0.5f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.AutoStories, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(24.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Politique Santé, Sécurité & Environnement", color = Color.White, fontSize = 17.5.sp, fontWeight = FontWeight.Bold)
                            Text("Engagements de la Direction & Vision Zéro Incident", color = Color(0xFFBAE6FD), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Fermer", tint = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Vision Surface
                    Surface(
                        color = Color(0xFF0369A1).copy(alpha = 0.35f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.2.dp, Color(0xFF0284C7))
                    ) {
                        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Gavel, contentDescription = null, tint = Color(0xFF7DD3FC), modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Engagement Fondamental : Aucun travail ne justifie qu'un collaborateur soit blessé ou exposé à un danger.",
                                color = Color.White,
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Bold,
                                lineHeight = 18.5.sp
                            )
                        }
                    }

                    val engagements = listOf(
                        Triple(
                            "1. Priorité Absolue à l'Humain",
                            "La santé et la sécurité de nos collaborateurs, partenaires et sous-traitants priment sur toute considération de production ou de rentabilité financière.",
                            Icons.Default.Favorite
                        ),
                        Triple(
                            "2. Objectif Zéro Incident & Zéro Préjudice",
                            "Viser continuellement l'élimination de tout accident avec ou sans arrêt, de toute maladie professionnelle et de tout impact négatif sur les écosystèmes.",
                            Icons.Default.CheckCircle
                        ),
                        Triple(
                            "3. Exemplarité Managériale & Rituels VFL",
                            "Chaque manager incarne la culture de sécurité par sa présence terrain, le dialogue constructif (VFL) et le traitement prioritaire des signalements.",
                            Icons.Default.Groups
                        ),
                        Triple(
                            "4. Droit & Devoir de Retrait Inconditionnel",
                            "Tout intervenant est habilité à stopper sur-le-champ toute tâche non maîtrisée sans risquer la moindre sanction hiérarchique.",
                            Icons.Default.PanTool
                        ),
                        Triple(
                            "5. Conformité Réglementaire & Normes ISO",
                            "Respect strict des lois nationales, des standards internationaux ISO 45001 (Santé/Sécurité) et ISO 14001 (Environnement).",
                            Icons.Default.Verified
                        )
                    )

                    engagements.forEach { (titre, desc, icon) ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Color(0xFF0284C7).copy(alpha = 0.40f))
                        ) {
                            Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.Top) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .background(Color(0xFF0284C7).copy(alpha = 0.2f), RoundedCornerShape(8.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(icon, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(20.dp))
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(titre, color = Color.White, fontSize = 14.5.sp, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(desc, color = Color(0xFFE2E8F0), fontSize = 13.sp, lineHeight = 18.sp)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Action: Consulter le Guide Politique (Lien Drive officiel)
                Button(
                    onClick = {
                        openWebLink(context, "https://drive.google.com/drive/folders/1RCnaR0fJpPuEB6K2qg-9Yqs_SjArg8rr?usp=drive_link")
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth().height(46.dp)
                ) {
                    Icon(Icons.Default.AutoStories, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Consulter le Guide Politique", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
                }
            }
        }
    }
}

// ------------------------------------------
// 8. DIALOG SUCCÈS STORIES & REX
// ------------------------------------------
@Composable
fun SuccesStoriesDialog(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, Color(0xFFD97706).copy(alpha = 0.5f)),
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.85f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .background(Color(0xFFD97706).copy(alpha = 0.2f), CircleShape)
                                .border(1.dp, Color(0xFFD97706).copy(alpha = 0.5f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFBBF24), modifier = Modifier.size(24.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Succès Stories & REX HSE", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            Text("Bonnes Pratiques, Innovations & Retours d'Expérience", color = Color(0xFFFDE68A), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Fermer", tint = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Banner Surface
                    Surface(
                        color = Color(0xFF78350F).copy(alpha = 0.45f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.2.dp, Color(0xFFD97706))
                    ) {
                        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = Color(0xFFFBBF24), modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Partage d'Excellence : Valorisons les victoires quotidiennes et les initiatives concrètes de nos équipes.",
                                color = Color.White,
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Bold,
                                lineHeight = 18.5.sp
                            )
                        }
                    }

                    val stories = listOf(
                        Triple(
                            "🏆 Cap Historique : 1 000 Jours Sans Accident",
                            "Performance remarquable réalisée grâce à la vigilance partagée, aux causeries 5 minutes quotidiennes et au signalement immédiat des situations dangereuses.",
                            Icons.Default.MilitaryTech
                        ),
                        Triple(
                            "💡 Innovation Terrain : Consignation LOTO 2.0",
                            "Création par les équipes de maintenance d'une station mobile de cadenassage simplifiant la vérification VAT et réduisant les risques d'omission.",
                            Icons.Default.Lightbulb
                        ),
                        Triple(
                            "🌱 Initiative Zéro Déchet & Recyclage Eaux",
                            "Valorisation de 95% des sous-produits de filtration et réutilisation intégrale des eaux industrielles en circuit fermé.",
                            Icons.Default.Recycling
                        ),
                        Triple(
                            "🤝 Challenge 'Voisins Vigilants' en Carrière",
                            "Implication réussie des chauffeurs routiers externes et transporteurs partenaires dans la remontée de presqu'accidents et l'amélioration continue.",
                            Icons.Default.Handshake
                        )
                    )

                    stories.forEach { (titre, desc, icon) ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Color(0xFFD97706).copy(alpha = 0.40f))
                        ) {
                            Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.Top) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .background(Color(0xFFD97706).copy(alpha = 0.2f), RoundedCornerShape(8.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(icon, contentDescription = null, tint = Color(0xFFFBBF24), modifier = Modifier.size(20.dp))
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(titre, color = Color.White, fontSize = 14.5.sp, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(desc, color = Color(0xFFE2E8F0), fontSize = 13.sp, lineHeight = 18.sp)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Action: Accéder aux Succès Stories (Lien Drive officiel)
                Button(
                    onClick = {
                        openWebLink(context, "https://drive.google.com/drive/folders/1ds9Vv1yVM6LhxhkbWSjjlrgKm0VpWfCV?usp=drive_link")
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth().height(46.dp)
                ) {
                    Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Accéder aux Succès Stories", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
                }
            }
        }
    }
}

