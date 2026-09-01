package com.example.ui.screens.admin

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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.entity.DocumentAttestationEntity
import com.example.data.entity.UserEntity
import com.example.ui.viewmodel.HseViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminUsersScreen(
    viewModel: HseViewModel,
    onBack: () -> Unit
) {
    val users by viewModel.allUsers.collectAsState()
    val authState by viewModel.authState.collectAsState()
    val userState by viewModel.user.collectAsState()
    val activeUser = userState?.takeIf { it.email.isNotBlank() } ?: authState.currentUser

    if (!activeUser.isAdmin) {
        AdminAccessRestrictedView(
            title = "Gestion des Utilisateurs",
            onBack = onBack
        )
        return
    }

    var searchQuery by remember { mutableStateOf("") }
    var selectedRoleFilter by remember { mutableStateOf("Tous") }
    var selectedSiteFilter by remember { mutableStateOf("Tous") }
    var userToDelete by remember { mutableStateOf<UserEntity?>(null) }

    val isAddEditOpen by viewModel.isAddEditUserOpen.collectAsState()
    val editingUser by viewModel.editingUser.collectAsState()

    val totalUsers = users.size
    val onlineCount = users.count { it.isOnline }
    val adminCount = users.count { it.role.equals("Admin", ignoreCase = true) }
    val managerCount = users.count { it.role.equals("Manager", ignoreCase = true) }
    val collabCount = users.count { it.role.equals("Collaborateur", ignoreCase = true) || (!it.role.equals("Admin", ignoreCase = true) && !it.role.equals("Manager", ignoreCase = true)) }

    val filteredUsers = remember(users, searchQuery, selectedRoleFilter, selectedSiteFilter) {
        users.filter { u ->
            val matchQuery = searchQuery.isBlank() || u.name.contains(searchQuery, ignoreCase = true) || u.email.contains(searchQuery, ignoreCase = true)
            val matchRole = when (selectedRoleFilter) {
                "Tous" -> true
                "En ligne 🟢" -> u.isOnline
                "Admin", "Admins" -> u.role.equals("Admin", ignoreCase = true)
                "Manager", "Managers" -> u.role.equals("Manager", ignoreCase = true)
                "Collaborateur", "Collaborateurs" -> u.role.equals("Collaborateur", ignoreCase = true) || (!u.role.equals("Admin", ignoreCase = true) && !u.role.equals("Manager", ignoreCase = true))
                "En attente" -> u.status.equals("En attente", ignoreCase = true)
                else -> true
            }
            val matchSite = selectedSiteFilter == "Tous" || u.site.contains(selectedSiteFilter, ignoreCase = true)
            matchQuery && matchRole && matchSite
        }
    }

    Scaffold(
        containerColor = Color(0xFF0F172A),
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF991B1B)) // Crimson Red header
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
                                .testTag("admin_users_back_btn")
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
                                text = "Gestion des Utilisateurs",
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Temps réel Firestore & Firebase Auth",
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 12.sp
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
            // VUE D'ENSEMBLE
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "VUE D'ENSEMBLE DU PERSONNEL",
                    color = Color(0xFF94A3B8),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Surface(
                    color = Color(0xFF10B981).copy(alpha = 0.15f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(Color(0xFF10B981), CircleShape)
                        )
                        Text(
                            text = "$onlineCount connecté(s) en direct",
                            color = Color(0xFF10B981),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Stats grid 2x2
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    UserStatCard(
                        icon = Icons.Default.Group,
                        label = "TOTAL INSCRITS",
                        count = "$totalUsers",
                        subLabel = "Comptes enregistrés",
                        tint = Color(0xFF38BDF8),
                        modifier = Modifier.weight(1f)
                    )
                    UserStatCard(
                        icon = Icons.Default.SignalWifiStatusbar4Bar,
                        label = "CONNECTÉS (LIVE)",
                        count = "$onlineCount",
                        subLabel = "Actuellement en ligne",
                        tint = Color(0xFF10B981),
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    UserStatCard(
                        icon = Icons.Default.Shield,
                        label = "ADMINS",
                        count = "$adminCount",
                        subLabel = "Comptes protégés & admins",
                        tint = Color(0xFFEF4444),
                        modifier = Modifier.weight(1f)
                    )
                    UserStatCard(
                        icon = Icons.Default.Person,
                        label = "COLLABS & MGRS",
                        count = "${collabCount + managerCount}",
                        subLabel = "Collaborateurs & Managers",
                        tint = Color(0xFFF59E0B),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Search bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Rechercher par nom ou email...", color = Color(0xFF64748B), fontSize = 13.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFF64748B)) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedContainerColor = Color(0xFF1E293B),
                    unfocusedContainerColor = Color(0xFF1E293B),
                    focusedBorderColor = Color(0xFFDC2626),
                    unfocusedBorderColor = Color(0xFF334155)
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )

            // Filter chips row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("Tous", "En ligne 🟢", "Admin", "Manager", "Collaborateur", "En attente").forEach { r ->
                    val isSel = selectedRoleFilter == r
                    FilterChip(
                        selected = isSel,
                        onClick = { selectedRoleFilter = r },
                        label = { Text(r, fontSize = 12.sp, fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFFDC2626),
                            selectedLabelColor = Color.White,
                            containerColor = Color(0xFF1E293B),
                            labelColor = Color(0xFF94A3B8)
                        ),
                        border = BorderStroke(1.dp, if (isSel) Color(0xFFEF4444) else Color(0xFF334155))
                    )
                }
            }

            // Red Button "+ Ajouter un utilisateur"
            Button(
                onClick = {
                    viewModel.editingUser.value = null
                    viewModel.isAddEditUserOpen.value = true
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF991B1B).copy(alpha = 0.4f)),
                border = BorderStroke(1.dp, Color(0xFFDC2626).copy(alpha = 0.6f)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .testTag("admin_add_user_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.PersonAdd,
                    contentDescription = null,
                    tint = Color(0xFFEF4444),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Ajouter un utilisateur",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }

            // User Cards List
            filteredUsers.forEach { u ->
                UserCardItem(
                    user = u,
                    onEdit = {
                        viewModel.editingUser.value = u
                        viewModel.isAddEditUserOpen.value = true
                    },
                    onToggleStatus = {
                        if (u.isProtectedAdmin) return@UserCardItem
                        if (u.status.equals("En attente", ignoreCase = true)) {
                            viewModel.approveUser(u.email, u.name)
                        } else {
                            val newStatus = if (u.status == "Actif") "Inactif" else "Actif"
                            viewModel.updateUserStatus(u.email, newStatus)
                        }
                    },
                    onDelete = {
                        if (!u.isProtectedAdmin) {
                            userToDelete = u
                        }
                    }
                )
            }

            if (filteredUsers.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Aucun utilisateur trouvé", color = Color(0xFF64748B), fontSize = 13.sp)
                }
            }
        }
    }

    // Confirmation Dialog before Deleting User
    userToDelete?.let { targetUser ->
        AlertDialog(
            onDismissRequest = { userToDelete = null },
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
                    text = "Êtes-vous sûr de vouloir supprimer l'utilisateur \"${targetUser.name}\" (${targetUser.email}) ?\n\nCette action supprimera définitivement le compte de la base de données Firestore et de l'application.",
                    color = Color(0xFFCBD5E1),
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val email = targetUser.email
                        userToDelete = null
                        viewModel.deleteUser(email)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Supprimer définitivement", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { userToDelete = null },
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF94A3B8)),
                    border = BorderStroke(1.dp, Color(0xFF334155))
                ) {
                    Text("Annuler")
                }
            }
        )
    }

    if (isAddEditOpen) {
        AddEditUserDialog(
            user = editingUser,
            onDismiss = { viewModel.isAddEditUserOpen.value = false },
            onSave = { updated ->
                viewModel.saveUser(updated)
            }
        )
    }
}

@Composable
fun UserStatCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    count: String,
    subLabel: String,
    tint: Color,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color(0xFF334155)),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(15.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = label, color = tint, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(text = count, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Black)
            Text(text = subLabel, color = Color(0xFF64748B), fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
fun UserCardItem(
    user: UserEntity,
    onEdit: () -> Unit,
    onToggleStatus: () -> Unit,
    onDelete: () -> Unit
) {
    val isProtected = user.isProtectedAdmin
    val roleColor = when {
        isProtected -> Color(0xFFE11D48)
        user.role.uppercase() == "ADMIN" -> Color(0xFFEF4444)
        user.role.uppercase() == "MANAGER" -> Color(0xFF10B981)
        else -> Color(0xFF3B82F6)
    }

    val isPending = user.status.equals("En attente", ignoreCase = true)
    val isActive = user.status.equals("Actif", ignoreCase = true)

    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(
            1.dp,
            if (isProtected) Color(0xFFF59E0B).copy(alpha = 0.8f) else Color(0xFF334155)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Protected Admin Banner
            if (isProtected) {
                Surface(
                    color = Color(0xFF7C2D12).copy(alpha = 0.4f),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.6f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            tint = Color(0xFFFBBF24),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "COMPTE DIRECT PROTÉGÉ (NON MODIFIABLE / NON SUPPRIMABLE)",
                            color = Color(0xFFFDE68A),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(roleColor.copy(alpha = 0.2f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = user.avatarInitials.ifBlank { "US" },
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = user.name,
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            color = roleColor.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = if (isProtected) "ADMIN PROTÉGÉ" else user.role.uppercase(),
                                color = if (isProtected) Color(0xFFFBBF24) else roleColor,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(text = user.email, color = Color(0xFF94A3B8), fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.LocationOn, contentDescription = null, tint = Color(0xFF64748B), modifier = Modifier.size(11.dp))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(text = user.site, color = Color(0xFF64748B), fontSize = 11.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(imageVector = Icons.Default.Business, contentDescription = null, tint = Color(0xFF64748B), modifier = Modifier.size(11.dp))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(text = user.department, color = Color(0xFF64748B), fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }

                Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Surface(
                        color = if (isActive) Color(0xFF10B981).copy(alpha = 0.15f) else Color(0xFFEF4444).copy(alpha = 0.15f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = user.status,
                            color = if (isActive) Color(0xFF10B981) else Color(0xFFEF4444),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }

                    // Live Online presence badge
                    Surface(
                        color = if (user.isOnline) Color(0xFF10B981).copy(alpha = 0.2f) else Color(0xFF64748B).copy(alpha = 0.2f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(if (user.isOnline) Color(0xFF10B981) else Color(0xFF64748B), CircleShape)
                            )
                            Text(
                                text = if (user.isOnline) "En ligne" else "Hors ligne",
                                color = if (user.isOnline) Color(0xFF10B981) else Color(0xFF94A3B8),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (isProtected) {
                    Surface(
                        color = Color(0xFF334155).copy(alpha = 0.5f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(34.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(imageVector = Icons.Default.Lock, contentDescription = null, tint = Color(0xFFFBBF24), modifier = Modifier.size(13.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Compte Protégé", color = Color(0xFFFBBF24), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    OutlinedButton(
                        onClick = onToggleStatus,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = if (isActive) Color(0xFFFBBF24) else Color(0xFF10B981)
                        ),
                        border = BorderStroke(1.dp, if (isActive) Color(0xFFD97706).copy(alpha = 0.4f) else Color(0xFF047857).copy(alpha = 0.4f)),
                        modifier = Modifier
                            .weight(1f)
                            .height(34.dp)
                    ) {
                        Text(
                            text = if (isPending) "Valider" else if (isActive) "Désactiver" else "Activer",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                OutlinedButton(
                    onClick = onEdit,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF38BDF8)),
                    border = BorderStroke(1.dp, Color(0xFF0284C7).copy(alpha = 0.4f)),
                    modifier = Modifier
                        .weight(1f)
                        .height(34.dp)
                ) {
                    Icon(imageVector = Icons.Default.Edit, contentDescription = "Modifier", modifier = Modifier.size(13.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Modifier", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                if (!isProtected && !user.role.equals("Admin", ignoreCase = true)) {
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Supprimer", tint = Color(0xFFF87171), modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditUserDialog(
    user: UserEntity?,
    onDismiss: () -> Unit,
    onSave: (UserEntity) -> Unit
) {
    val isProtected = user?.isProtectedAdmin == true
    var name by remember { mutableStateOf(user?.name ?: "") }
    var email by remember { mutableStateOf(user?.email ?: "") }
    var role by remember { mutableStateOf(if (isProtected) "Admin" else user?.role ?: "Collaborateur") }
    var site by remember { mutableStateOf(user?.site ?: "Bonabéri") }
    var department by remember { mutableStateOf(user?.department ?: "Sécurité Industrielle") }
    var status by remember { mutableStateOf(if (isProtected) "Actif" else user?.status ?: "Actif") }

    var expandedRole by remember { mutableStateOf(false) }
    var expandedSite by remember { mutableStateOf(false) }
    var expandedStatus by remember { mutableStateOf(false) }

    val isEdit = user != null

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            color = Color(0xFF1E293B),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, if (isProtected) Color(0xFFF59E0B) else Color(0xFF334155)),
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
                        text = if (isProtected) "Modifier Administrateur Protégé" else if (isEdit) "Modifier l'utilisateur" else "Ajouter un utilisateur",
                        color = Color.White,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Fermer", tint = Color(0xFF94A3B8))
                    }
                }

                if (isProtected) {
                    Surface(
                        color = Color(0xFF7C2D12).copy(alpha = 0.5f),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.6f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "🛡️ Compte Administrateur Direct : Le rôle \"Admin\" et le statut \"Actif\" sont verrouillés et ne peuvent être ni modifiés ni désactivés.",
                            color = Color(0xFFFDE68A),
                            fontSize = 11.sp,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }

                // Nom complet
                Text("Nom complet", color = Color(0xFF94A3B8), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    placeholder = { Text("Nom et prénom", color = Color(0xFF64748B), fontSize = 13.sp) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedContainerColor = Color(0xFF0F172A),
                        unfocusedContainerColor = Color(0xFF0F172A),
                        focusedBorderColor = Color(0xFFDC2626),
                        unfocusedBorderColor = Color(0xFF334155)
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                // Email
                Text("Email", color = Color(0xFF94A3B8), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    enabled = !isEdit,
                    placeholder = { Text("email@cimencam.cm", color = Color(0xFF64748B), fontSize = 13.sp) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedContainerColor = Color(0xFF0F172A),
                        unfocusedContainerColor = Color(0xFF0F172A),
                        focusedBorderColor = Color(0xFFDC2626),
                        unfocusedBorderColor = Color(0xFF334155),
                        disabledContainerColor = Color(0xFF1E293B).copy(alpha = 0.5f),
                        disabledTextColor = Color(0xFF94A3B8)
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                // Role dropdown
                Text("Rôle", color = Color(0xFF94A3B8), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                if (isProtected) {
                    OutlinedTextField(
                        value = "Admin (Verrouillé)",
                        onValueChange = {},
                        readOnly = true,
                        enabled = false,
                        colors = OutlinedTextFieldDefaults.colors(
                            disabledTextColor = Color(0xFFFBBF24),
                            disabledContainerColor = Color(0xFF0F172A),
                            disabledBorderColor = Color(0xFFB45309)
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    ExposedDropdownMenuBox(
                        expanded = expandedRole,
                        onExpandedChange = { expandedRole = it }
                    ) {
                        OutlinedTextField(
                            value = role,
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedRole) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedContainerColor = Color(0xFF0F172A),
                                unfocusedContainerColor = Color(0xFF0F172A),
                                focusedBorderColor = Color(0xFFDC2626),
                                unfocusedBorderColor = Color(0xFF334155)
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = expandedRole,
                            onDismissRequest = { expandedRole = false },
                            modifier = Modifier.background(Color(0xFF1E293B))
                        ) {
                            listOf("Admin", "Manager", "Collaborateur").forEach { r ->
                                DropdownMenuItem(
                                    text = { Text(r, color = Color.White) },
                                    onClick = {
                                        role = r
                                        expandedRole = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Site dropdown
                Text("Site", color = Color(0xFF94A3B8), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
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
                            focusedBorderColor = Color(0xFFDC2626),
                            unfocusedBorderColor = Color(0xFF334155)
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = expandedSite,
                        onDismissRequest = { expandedSite = false },
                        modifier = Modifier.background(Color(0xFF1E293B))
                    ) {
                        listOf("Bonabéri", "Nomayos", "Figuil", "Siège Douala", "Direction Générale (Tous sites)", "Tous sites").forEach { s ->
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

                // Department
                Text("Département", color = Color(0xFF94A3B8), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                OutlinedTextField(
                    value = department,
                    onValueChange = { department = it },
                    placeholder = { Text("Ex: Sécurité Industrielle", color = Color(0xFF64748B), fontSize = 13.sp) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedContainerColor = Color(0xFF0F172A),
                        unfocusedContainerColor = Color(0xFF0F172A),
                        focusedBorderColor = Color(0xFFDC2626),
                        unfocusedBorderColor = Color(0xFF334155)
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                // Status dropdown
                Text("Statut du compte", color = Color(0xFF94A3B8), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                if (isProtected) {
                    OutlinedTextField(
                        value = "Actif (Verrouillé)",
                        onValueChange = {},
                        readOnly = true,
                        enabled = false,
                        colors = OutlinedTextFieldDefaults.colors(
                            disabledTextColor = Color(0xFF10B981),
                            disabledContainerColor = Color(0xFF0F172A),
                            disabledBorderColor = Color(0xFF047857)
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    ExposedDropdownMenuBox(
                        expanded = expandedStatus,
                        onExpandedChange = { expandedStatus = it }
                    ) {
                        OutlinedTextField(
                            value = status,
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedStatus) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedContainerColor = Color(0xFF0F172A),
                                unfocusedContainerColor = Color(0xFF0F172A),
                                focusedBorderColor = Color(0xFFDC2626),
                                unfocusedBorderColor = Color(0xFF334155)
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = expandedStatus,
                            onDismissRequest = { expandedStatus = false },
                            modifier = Modifier.background(Color(0xFF1E293B))
                        ) {
                            listOf("Actif", "Inactif", "En attente").forEach { st ->
                                DropdownMenuItem(
                                    text = { Text(st, color = Color.White) },
                                    onClick = {
                                        status = st
                                        expandedStatus = false
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Submit button
                Button(
                    onClick = {
                        if (name.isNotBlank() && email.isNotBlank()) {
                            val initials = name.split(" ")
                                .filter { it.isNotBlank() }
                                .take(2)
                                .map { it.first().uppercaseChar() }
                                .joinToString("")
                                .ifEmpty { "US" }

                            val updatedUser = UserEntity(
                                email = email.trim(),
                                name = name.trim(),
                                role = if (isProtected) "Admin" else role,
                                site = site,
                                department = department.trim(),
                                avatarInitials = initials,
                                status = if (isProtected) "Actif" else status
                            )
                            onSave(updatedUser)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                ) {
                    Icon(imageVector = Icons.Default.Save, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isEdit) "Enregistrer les modifications" else "Créer l'utilisateur",
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
fun AdminAttestationsScreen(
    viewModel: HseViewModel,
    onBack: () -> Unit
) {
    val attestations by viewModel.allAttestations.collectAsState()
    val authState by viewModel.authState.collectAsState()
    val userState by viewModel.user.collectAsState()
    val activeUser = userState?.takeIf { it.email.isNotBlank() } ?: authState.currentUser

    if (!activeUser.isAdmin) {
        AdminAccessRestrictedView(
            title = "Registre des Attestations",
            onBack = onBack
        )
        return
    }

    var searchQuery by remember { mutableStateOf("") }

    val filteredList = remember(attestations, searchQuery) {
        attestations.filter { att ->
            searchQuery.isBlank() || att.userName.contains(searchQuery, ignoreCase = true) || att.documentTitle.contains(searchQuery, ignoreCase = true) || att.certificateId.contains(searchQuery, ignoreCase = true)
        }
    }

    Scaffold(
        containerColor = Color(0xFF0F172A),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Registre des Attestations", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                        Text("${attestations.size} lectures certifiées conformes", color = Color(0xFF10B981), fontSize = 12.sp)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF1E293B))
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Rechercher par collaborateur, document...", color = Color(0xFF64748B), fontSize = 13.sp) },
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

            filteredList.forEach { att ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                color = Color(0xFF10B981).copy(alpha = 0.15f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.Verified, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(13.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Certifié Conforme", color = Color(0xFF10B981), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            Text(text = att.timestampText, color = Color(0xFF64748B), fontSize = 11.sp)
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = att.documentTitle, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Text(text = "Réf: ${att.documentReference}", color = Color(0xFF38BDF8), fontSize = 11.sp)

                        Spacer(modifier = Modifier.height(8.dp))
                        HorizontalDivider(color = Color(0xFF334155))
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(text = "${att.userName} (${att.userRole})", color = Color(0xFFE2E8F0), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                Text(text = "${att.userEmail} • ${att.userSite}", color = Color(0xFF94A3B8), fontSize = 11.sp)
                            }
                            IconButton(onClick = { viewModel.deleteAttestation(att.id) }, modifier = Modifier.size(28.dp)) {
                                Icon(imageVector = Icons.Default.Delete, contentDescription = "Supprimer", tint = Color(0xFFF87171), modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}
