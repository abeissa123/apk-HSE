package com.example.ui.screens.content

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.entity.ContentEntity
import com.example.ui.theme.*
import com.example.ui.viewmodel.HseViewModel
import com.example.util.MediaStorageHelper
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContentReaderDialog(
    content: ContentEntity,
    viewModel: HseViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var isAttested by remember { mutableStateOf(false) }
    var isVideoPlaying by remember { mutableStateOf(false) }
    var videoProgress by remember { mutableFloatStateOf(0.35f) }
    var activeTab by remember { mutableIntStateOf(0) } // 0: Contenu, 1: Détails Techniques & Fichier

    val (typeColor, typeIcon, typeLabel) = when (content.type.uppercase()) {
        "PDF" -> Triple(Color(0xFFEF4444), Icons.Default.Description, "Document PDF")
        "PHOTO" -> Triple(Color(0xFF10B981), Icons.Default.Image, "Photo")
        "VIDEO" -> Triple(Color(0xFF3B82F6), Icons.Default.Videocam, "Vidéo Formation")
        "LIEN" -> Triple(Color(0xFFA855F7), Icons.Default.Link, "Lien Web / Portail")
        else -> Triple(Color(0xFFF59E0B), Icons.Default.Article, "Document")
    }

    val hasLocalFile = content.fileUrl.isNotBlank() && (content.fileUrl.startsWith("/") || content.fileUrl.startsWith("content://") || content.fileUrl.startsWith("file://"))

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0B132B).copy(alpha = 0.98f))
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
            ) {
                // Top Action Bar
                Surface(
                    color = Color(0xFF1E293B),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
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
                                    .size(36.dp)
                                    .background(Color.White.copy(alpha = 0.1f), CircleShape)
                                    .testTag("content_reader_close_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Fermer",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Surface(
                                    color = typeColor.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = "$typeLabel • ${content.category}",
                                        color = typeColor,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                                Text(
                                    text = content.title,
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            if (hasLocalFile || content.type.uppercase() == "PDF") {
                                IconButton(
                                    onClick = {
                                        MediaStorageHelper.openFileWithExternalViewer(context, content.fileUrl)
                                    },
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(typeColor.copy(alpha = 0.2f), CircleShape)
                                        .testTag("open_system_viewer_btn")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.OpenInNew,
                                        contentDescription = "Ouvrir dans l'appareil",
                                        tint = typeColor,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }

                            IconButton(
                                onClick = {
                                    Toast.makeText(context, "Document partagé dans le flux", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(Color.White.copy(alpha = 0.1f), CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = "Partager",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                // Tabs for Content Reader vs File Metadata
                Surface(
                    color = Color(0xFF131D31),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TabPill(
                            label = "Lecture du Contenu",
                            icon = Icons.Default.MenuBook,
                            isSelected = activeTab == 0,
                            selectedColor = typeColor,
                            onClick = { activeTab = 0 }
                        )
                        TabPill(
                            label = "Fiche & Stockage",
                            icon = Icons.Default.Info,
                            isSelected = activeTab == 1,
                            selectedColor = typeColor,
                            onClick = { activeTab = 1 }
                        )
                    }
                }

                // Scrollable Content Reader Body
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (activeTab == 0) {
                        // Main Viewer View
                        when (content.type.uppercase()) {
                            "PDF" -> {
                                PdfReaderSection(
                                    content = content,
                                    hasLocalFile = hasLocalFile,
                                    isAttested = isAttested,
                                    onOpenExternal = {
                                        MediaStorageHelper.openFileWithExternalViewer(context, content.fileUrl)
                                    },
                                    onAttest = {
                                        isAttested = true
                                        viewModel.recordDocumentAttestation(
                                            documentId = content.id,
                                            documentTitle = content.title,
                                            documentReference = content.referenceCode.ifBlank { "REF-PDF" }
                                        )
                                        Toast.makeText(context, "Attestation de lecture enregistrée avec succès !", Toast.LENGTH_LONG).show()
                                    }
                                )
                            }
                            "VIDEO" -> {
                                VideoPlayerSection(
                                    content = content,
                                    isPlaying = isVideoPlaying,
                                    progress = videoProgress,
                                    onTogglePlay = { isVideoPlaying = !isVideoPlaying },
                                    onProgressChange = { videoProgress = it }
                                )
                            }
                            "PHOTO" -> {
                                PhotoViewerSection(content = content)
                            }
                            "LIEN" -> {
                                WebLinkViewerSection(content = content)
                            }
                            else -> {
                                GenericContentSection(content = content)
                            }
                        }
                    } else {
                        // Technical & Storage Metadata Tab
                        FileMetadataCard(content = content, hasLocalFile = hasLocalFile)
                    }

                    Spacer(modifier = Modifier.height(30.dp))
                }
            }
        }
    }
}

@Composable
fun TabPill(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    selectedColor: Color,
    onClick: () -> Unit
) {
    Surface(
        color = if (isSelected) selectedColor.copy(alpha = 0.2f) else Color(0xFF1E293B),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, if (isSelected) selectedColor else Color(0xFF334155)),
        modifier = Modifier
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = if (isSelected) selectedColor else Color(0xFF94A3B8), modifier = Modifier.size(14.dp))
            Text(text = label, color = if (isSelected) Color.White else Color(0xFF94A3B8), fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
        }
    }
}

@Composable
fun PdfReaderSection(
    content: ContentEntity,
    hasLocalFile: Boolean,
    isAttested: Boolean,
    onOpenExternal: () -> Unit,
    onAttest: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.4f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.PictureAsPdf, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(22.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Document Réglementaire", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
                Surface(
                    color = Color(0xFFEF4444).copy(alpha = 0.15f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = if (hasLocalFile) "Fichier Local" else "Document Officiel",
                        color = Color(0xFFEF4444),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Action button to open with external PDF Reader
            Button(
                onClick = onOpenExternal,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth().testTag("open_with_pdf_app_btn")
            ) {
                Icon(imageVector = Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Ouvrir dans le lecteur PDF du téléphone", fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(14.dp))

            // In-app Document Text Pages Preview
            Surface(
                color = Color(0xFF0F172A),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color(0xFF334155)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "EXTRAIT DE CONSIGNES & DISPOSITIONS",
                            color = Color(0xFF38BDF8),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(text = "Réf : ${content.referenceCode.ifBlank { "DOC-2026" }}", color = Color(0xFF64748B), fontSize = 10.sp)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "1. DISPOSITIONS OBLIGATOIRES SUR LES SITES CIMENCAM :\n" +
                                "• Port systématique et complet des Équipements de Protection Individuelle (Casque avec jugulaire, Lunettes, Chaussures S3, Gilet haute visibilité).\n" +
                                "• Respect impératif de la tolérance zéro pour les interventions non consignées.\n" +
                                "• Interdiction formelle de circuler sous les charges suspendues et balisage périmétrique strict.\n\n" +
                                "2. GESTION DES SITUATIONS D'URGENCE :\n" +
                                "• Tout collaborateur témoin d'une situation dangereuse a le devoir d'exercer son droit de retrait et d'enclencher l'alerte immédiate au poste de garde.",
                        color = Color(0xFFCBD5E1),
                        fontSize = 12.sp,
                        lineHeight = 18.sp
                    )

                    if (content.description.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        HorizontalDivider(color = Color(0xFF334155))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "NOTE DE SERVICE :\n${content.description}",
                            color = Color(0xFF94A3B8),
                            fontSize = 11.sp,
                            lineHeight = 16.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Attestation Action
            if (isAttested) {
                Surface(
                    color = Color(0xFF10B981).copy(alpha = 0.15f),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(20.dp))
                        Text(
                            text = "Lecture attestée et enregistrée dans votre passeport sécurité.",
                            color = Color(0xFF10B981),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            } else {
                Button(
                    onClick = onAttest,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF047857)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth().testTag("attest_reading_btn")
                ) {
                    Icon(imageVector = Icons.Default.Verified, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("J'atteste avoir lu et compris ce document", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun VideoPlayerSection(
    content: ContentEntity,
    isPlaying: Boolean,
    progress: Float,
    onTogglePlay: () -> Unit,
    onProgressChange: (Float) -> Unit
) {
    val context = LocalContext.current

    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color(0xFF3B82F6).copy(alpha = 0.4f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Simulated Video Screen Canvas
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color(0xFF1E3A8A),
                                Color(0xFF0F172A)
                            )
                        )
                    )
                    .clickable { onTogglePlay() },
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .background(Color.White.copy(alpha = 0.9f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Lecture",
                            tint = Color(0xFF1E3A8A),
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (isPlaying) "Vidéo en cours de lecture..." else "Appuyez pour lancer la vidéo",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                // Video Progress Bar at bottom
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Slider(
                        value = progress,
                        onValueChange = onProgressChange,
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFF38BDF8),
                            activeTrackColor = Color(0xFF38BDF8),
                            inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (content.fileUrl.isNotBlank()) {
                OutlinedButton(
                    onClick = { MediaStorageHelper.openFileWithExternalViewer(context, content.fileUrl, "video/*") },
                    border = BorderStroke(1.dp, Color(0xFF3B82F6)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(imageVector = Icons.Default.Launch, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Ouvrir dans le lecteur vidéo externe", color = Color(0xFF38BDF8), fontSize = 12.sp)
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            Text(
                text = "Points clés de la formation vidéo :",
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
            BulletPoint(text = "Vérification systématique de l'état des équipements de protection")
            BulletPoint(text = "Application des 5 règles vitales d'intervention sécurisée")
            BulletPoint(text = "Procédure d'alerte et de déclenchement du plan d'urgence")
        }
    }
}

@Composable
fun PhotoViewerSection(content: ContentEntity) {
    val context = LocalContext.current
    val hasImage = content.fileUrl.isNotBlank()

    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.4f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            if (hasImage) {
                // Real Async Image from internal storage / uri / network
                val imageModel = if (content.fileUrl.startsWith("/")) File(content.fileUrl) else content.fileUrl
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(imageModel)
                        .crossfade(true)
                        .build(),
                    contentDescription = content.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(230.dp)
                        .clip(RoundedCornerShape(12.dp))
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            Brush.radialGradient(
                                listOf(
                                    Color(0xFF047857),
                                    Color(0xFF064E3B),
                                    Color(0xFF0F172A)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(imageVector = Icons.Default.Image, contentDescription = null, tint = Color.White, modifier = Modifier.size(56.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = "Photographie de terrain", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text(text = "Site : ${content.site}", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (content.fileUrl.isNotBlank()) {
                OutlinedButton(
                    onClick = { MediaStorageHelper.openFileWithExternalViewer(context, content.fileUrl, "image/*") },
                    border = BorderStroke(1.dp, Color(0xFF10B981)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(imageVector = Icons.Default.ZoomIn, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Agrandir dans la galerie photo", color = Color(0xFF10B981), fontSize = 12.sp)
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            Text(
                text = "Observation & Description visuelle :",
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = content.description.ifBlank { "Photographie documentant les bonnes pratiques de sécurité et de propreté sur les installations." },
                color = Color(0xFF94A3B8),
                fontSize = 12.sp
            )
        }
    }
}

@Composable
fun WebLinkViewerSection(content: ContentEntity) {
    val context = LocalContext.current
    val url = content.fileUrl.ifBlank { "https://www.holcim.com" }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color(0xFFA855F7).copy(alpha = 0.4f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color(0xFFA855F7).copy(alpha = 0.2f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = Icons.Default.Link, contentDescription = null, tint = Color(0xFFA855F7), modifier = Modifier.size(22.dp))
                }
                Column {
                    Text(text = "Ressource Externe Sécurisée", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Text(text = url, color = Color(0xFF94A3B8), fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Button(
                onClick = {
                    try {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        Toast.makeText(context, "Ouverture du lien : $url", Toast.LENGTH_SHORT).show()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7E22CE)),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth().testTag("open_external_link_btn")
            ) {
                Icon(imageVector = Icons.Default.OpenInBrowser, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Ouvrir dans le navigateur", fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun GenericContentSection(content: ContentEntity) {
    val context = LocalContext.current
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color(0xFF334155)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = "Détails du document", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = content.description, color = Color(0xFF94A3B8), fontSize = 12.sp)

            if (content.fileUrl.isNotBlank()) {
                Spacer(modifier = Modifier.height(14.dp))
                Button(
                    onClick = { MediaStorageHelper.openFileWithExternalViewer(context, content.fileUrl) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(imageVector = Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Ouvrir le fichier avec le système", fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun FileMetadataCard(content: ContentEntity, hasLocalFile: Boolean) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color(0xFF334155)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = Icons.Default.SdStorage, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Propriétés & Stockage Local", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }

            MetaRow(label = "Titre officiel", value = content.title)
            MetaRow(label = "Type de ressource", value = content.type)
            MetaRow(label = "Catégorie", value = content.category)
            MetaRow(label = "Code de référence", value = content.referenceCode.ifBlank { "REF-SYS" })
            MetaRow(label = "Version", value = content.version)
            MetaRow(label = "Auteur / Émetteur", value = "${content.authorName} (${content.authorRole})")
            MetaRow(label = "Site d'application", value = content.site)
            MetaRow(label = "Date d'enregistrement", value = content.dateText)
            MetaRow(label = "Statut d'accès stockage", value = if (hasLocalFile) "Stocké localement sur l'appareil (Prêt pour lecture)" else "Hébergé sur le serveur")

            if (content.fileUrl.isNotBlank()) {
                Surface(
                    color = Color(0xFF0F172A),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text("Emplacement / URI du fichier :", color = Color(0xFF64748B), fontSize = 10.sp)
                        Text(content.fileUrl, color = Color(0xFF38BDF8), fontSize = 11.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }
}

@Composable
private fun MetaRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, color = Color(0xFF94A3B8), fontSize = 12.sp)
        Text(text = value, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun BulletPoint(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(text = "•", color = Color(0xFF38BDF8), fontSize = 14.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = text, color = Color(0xFFCBD5E1), fontSize = 12.sp, lineHeight = 16.sp)
    }
}
