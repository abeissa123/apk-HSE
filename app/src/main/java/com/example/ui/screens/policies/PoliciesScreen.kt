package com.example.ui.screens.policies

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

    val authState by viewModel.authState.collectAsState()
    val userState by viewModel.user.collectAsState()
    val activeUser = userState?.takeIf { it.email.isNotBlank() } ?: authState.currentUser
    val isUserAdmin = activeUser.isAdmin

    // Pending Imported File State for Confirmation & Categorization Dialog
    var pendingImportResult by remember { mutableStateOf<com.example.util.ImportedFileResult?>(null) }
    var pendingImportTitle by remember { mutableStateOf("") }
    var pendingImportCategory by remember { mutableStateOf("Sécurité") }
    var pendingImportDescription by remember { mutableStateOf("") }
    var isImportDialogOpen by remember { mutableStateOf(false) }

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
            // Header Hero Banner (Modern Dynamic Gradient)
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color(0xFF1E3A8A),
                                    Color(0xFF0F172A)
                                )
                            )
                        )
                        .padding(horizontal = 20.dp, vertical = 16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .background(
                                    Brush.linearGradient(
                                        listOf(Color(0xFF3B82F6), Color(0xFF1D4ED8))
                                    ),
                                    RoundedCornerShape(12.dp)
                                )
                                .border(1.dp, Color(0xFF60A5FA).copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Politiques & Référentiels",
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Chartes, livrets officiels et base documentaire",
                                color = Color(0xFF94A3B8),
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }

            // Search Bar & Import Action
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Search Input
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Rechercher un document, guide, réf...", color = HSETextMuted, fontSize = 13.sp) },
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = HSEBlueLight, modifier = Modifier.size(20.dp))
                        },
                        trailingIcon = {
                            if (searchQuery.isNotBlank()) {
                                IconButton(onClick = { searchQuery = "" }, modifier = Modifier.size(24.dp)) {
                                    Icon(imageVector = Icons.Default.Clear, contentDescription = "Effacer", tint = HSETextSecondary)
                                }
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = HSEFieldBg,
                            unfocusedContainerColor = HSEFieldBg,
                            focusedBorderColor = HSEBlueLight,
                            unfocusedBorderColor = HSECardBorder,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        shape = RoundedCornerShape(14.dp),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("policy_search_input")
                    )

                    // Quick Action: Import & Read Local Documents from Device
                    if (isUserAdmin) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.4f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    filePickerLauncher.launch("*/*")
                                }
                                .testTag("import_local_doc_btn")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .background(Color(0xFF38BDF8).copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CloudUpload,
                                        contentDescription = null,
                                        tint = Color(0xFF38BDF8),
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Importer & Enregistrer un document",
                                        color = Color.White,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Ajout immédiat dans la base de données Room et synchronisation Cloud",
                                        color = Color(0xFF94A3B8),
                                        fontSize = 11.sp
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Default.AddCircle,
                                    contentDescription = null,
                                    tint = Color(0xFF38BDF8),
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Filter Chips (Horizontal)
            item {
                val filterChips = listOf("Tous", "Politique", "Santé", "Sécurité", "Environnement", "Induction", "Documents Importés")
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    filterChips.forEach { chip ->
                        val isSelected = selectedCategoryFilter == chip
                        Surface(
                            color = if (isSelected) HSEBluePrimary else HSECardBg,
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(
                                1.dp,
                                if (isSelected) HSEBlueLight else HSECardBorder
                            ),
                            modifier = Modifier
                                .clickable { selectedCategoryFilter = chip }
                                .testTag("filter_chip_$chip")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (chip != "Tous") {
                                    val icon = when (chip) {
                                        "Politique" -> Icons.Default.Shield
                                        "Santé" -> Icons.Default.Favorite
                                        "Sécurité" -> Icons.Default.Engineering
                                        "Environnement" -> Icons.Default.Eco
                                        "Induction" -> Icons.Default.School
                                        else -> Icons.Default.FolderOpen
                                    }
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = null,
                                        tint = if (isSelected) Color.White else HSETextSecondary,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(5.dp))
                                }
                                Text(
                                    text = chip,
                                    color = if (isSelected) Color.White else HSETextSecondary,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }

            // Catégories Section Header (When showing all and no search query)
            if (selectedCategoryFilter == "Tous" && searchQuery.isBlank()) {
                item {
                    Text(
                        text = "Catégories Thématiques",
                        color = HSETextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
                    )
                }

                // Category items
                items(categories, key = { it.title }) { cat ->
                    CategoryCardItem(
                        category = cat,
                        onClick = { selectedCategoryFilter = cat.title },
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(10.dp))
                }
            }

            // Documents récents Section Header
            if (filteredDocuments.isNotEmpty()) {
                item {
                    Text(
                        text = if (selectedCategoryFilter == "Tous") "Livrets Officiels & Guides (${filteredDocuments.size})" else "Guides & Référentiels (${filteredDocuments.size})",
                        color = HSETextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
                    )
                }

                // Documents list
                items(filteredDocuments, key = { it.id }) { doc ->
                    DocumentCardItem(
                        document = doc,
                        onClick = { viewingDocument = doc },
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
                    )
                }
            }

            // Multimedia Contents from Database / Firebase (PDF, Photos, Videos, Liens)
            if (filteredMedia.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Base Documentaire & Médias (${filteredMedia.size})",
                        color = HSETextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
                    )
                }

                items(filteredMedia, key = { it.id }) { media ->
                    val (badgeColor, badgeIcon) = when (media.type.uppercase()) {
                        "PDF" -> Pair(Color(0xFFEF4444), Icons.Default.Description)
                        "PHOTO" -> Pair(Color(0xFF10B981), Icons.Default.Image)
                        "VIDEO" -> Pair(Color(0xFF3B82F6), Icons.Default.Videocam)
                        "LIEN" -> Pair(Color(0xFFA855F7), Icons.Default.Link)
                        else -> Pair(Color(0xFFF59E0B), Icons.Default.Article)
                    }

                    Card(
                        colors = CardDefaults.cardColors(containerColor = HSECardBg),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, HSECardBorder),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 4.dp)
                            .clickable { viewModel.openContentReader(media) }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .background(badgeColor.copy(alpha = 0.2f), RoundedCornerShape(10.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(imageVector = badgeIcon, contentDescription = null, tint = badgeColor, modifier = Modifier.size(20.dp))
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        color = badgeColor.copy(alpha = 0.15f),
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = "${media.type} • ${media.category}",
                                            color = badgeColor,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = media.title,
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${media.viewsCount} consultations • ${media.dateText}",
                                    color = HSETextMuted,
                                    fontSize = 11.sp
                                )
                            }
                            Icon(imageVector = Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = HSETextSecondary, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            if (filteredDocuments.isEmpty() && filteredMedia.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp, horizontal = 20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.SearchOff,
                                contentDescription = null,
                                tint = HSETextMuted,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Aucun document trouvé",
                                color = HSETextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                            Text(
                                text = "Essayez un autre mot-clé ou réinitialisez les filtres",
                                color = HSETextMuted,
                                fontSize = 12.sp
                            )
                        }
                    }
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

        // Import & Publish Confirmation Dialog
        if (isImportDialogOpen && pendingImportResult != null) {
            val res = pendingImportResult!!
            val categoriesList = listOf("Sécurité", "Politique de Sécurité", "Santé", "Environnement", "Induction", "Documents Importés")

            Dialog(
                onDismissRequest = { isImportDialogOpen = false },
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
                            Text(
                                text = "Enregistrer le document",
                                color = Color.White,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold
                            )
                            IconButton(onClick = { isImportDialogOpen = false }, modifier = Modifier.size(28.dp)) {
                                Icon(imageVector = Icons.Default.Close, contentDescription = "Fermer", tint = Color(0xFF94A3B8))
                            }
                        }

                        // File Preview Banner
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = when (res.contentType) {
                                        "PDF" -> Icons.Default.Description
                                        "PHOTO" -> Icons.Default.Image
                                        "VIDEO" -> Icons.Default.Videocam
                                        else -> Icons.Default.Article
                                    },
                                    contentDescription = null,
                                    tint = Color(0xFF38BDF8),
                                    modifier = Modifier.size(28.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = res.fileName, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(text = "Taille : ${res.fileSizeFormatted} • Type : ${res.contentType}", color = Color(0xFF94A3B8), fontSize = 11.sp)
                                }
                            }
                        }

                        // Title field
                        Text("Titre du document", color = Color(0xFFCBD5E1), fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        OutlinedTextField(
                            value = pendingImportTitle,
                            onValueChange = { pendingImportTitle = it },
                            placeholder = { Text("Ex: Procédure de travail...", color = Color(0xFF64748B)) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color(0xFF0F172A),
                                unfocusedContainerColor = Color(0xFF0F172A),
                                focusedBorderColor = Color(0xFF38BDF8),
                                unfocusedBorderColor = Color(0xFF334155),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Category Selector
                        Text("Catégorie HSE", color = Color(0xFFCBD5E1), fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            categoriesList.forEach { cat ->
                                val isCatSelected = pendingImportCategory == cat
                                FilterChip(
                                    selected = isCatSelected,
                                    onClick = { pendingImportCategory = cat },
                                    label = { Text(cat, fontSize = 11.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = Color(0xFF0284C7),
                                        selectedLabelColor = Color.White,
                                        containerColor = Color(0xFF0F172A),
                                        labelColor = Color(0xFF94A3B8)
                                    )
                                )
                            }
                        }

                        // Description field
                        Text("Description / Note", color = Color(0xFFCBD5E1), fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        OutlinedTextField(
                            value = pendingImportDescription,
                            onValueChange = { pendingImportDescription = it },
                            placeholder = { Text("Brève description du contenu...", color = Color(0xFF64748B)) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color(0xFF0F172A),
                                unfocusedContainerColor = Color(0xFF0F172A),
                                focusedBorderColor = Color(0xFF38BDF8),
                                unfocusedBorderColor = Color(0xFF334155),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            shape = RoundedCornerShape(10.dp),
                            maxLines = 3,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        // Submit Button
                        Button(
                            onClick = {
                                if (pendingImportTitle.isNotBlank()) {
                                    val newContent = ContentEntity(
                                        id = "doc_import_${System.currentTimeMillis()}",
                                        title = pendingImportTitle,
                                        description = pendingImportDescription,
                                        type = res.contentType,
                                        category = pendingImportCategory,
                                        fileUrl = res.localFilePath.ifBlank { res.fileUriString },
                                        viewsCount = 1,
                                        dateText = "Aujourd'hui",
                                        referenceCode = "DOC-${System.currentTimeMillis() % 10000}",
                                        authorName = activeUser.name.ifBlank { "Administrateur HSE" },
                                        authorRole = activeUser.role.ifBlank { "Admin" },
                                        site = activeUser.site.ifBlank { "Tous les sites" }
                                    )
                                    viewModel.saveContent(newContent) { success ->
                                        if (success) {
                                            viewModel.openContentReader(newContent)
                                        }
                                    }
                                    isImportDialogOpen = false
                                    Toast.makeText(context, "Document enregistré dans la base de données !", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF047857)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                        ) {
                            Icon(imageVector = Icons.Default.CloudUpload, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Enregistrer dans la base de données", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
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
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("cat_card_${category.id}"),
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
                        .size(38.dp)
                        .background(Color(category.colorHex), RoundedCornerShape(10.dp)),
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
                        color = Color.White,
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
                        color = HSETextMuted,
                        fontSize = 10.sp
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
fun DocumentCardItem(
    document: HseDocument,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("doc_card_${document.id}"),
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
                        .background(HSERedUrgent.copy(alpha = 0.15f), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Description,
                        contentDescription = null,
                        tint = HSERedUrgent,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = document.title,
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = document.subtitle,
                        color = HSETextSecondary,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = document.format,
                        color = HSERedUrgent,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
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

