package com.example.ui.screens.auth

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.BrandHeader
import com.example.ui.theme.*
import com.example.ui.viewmodel.HseViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    viewModel: HseViewModel,
    modifier: Modifier = Modifier
) {
    val authState by viewModel.authState.collectAsState()
    val context = LocalContext.current
    var isRegisterMode by remember { mutableStateOf(false) }
    var showForgotPasswordDialog by remember { mutableStateOf(false) }
    var registerName by remember { mutableStateOf("") }
    var registerPhone by remember { mutableStateOf("") }
    var registerSite by remember { mutableStateOf("Site de Figuil") }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF132D68),
                        Color(0xFF0B1736),
                        Color(0xFF080F24)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Top Brand Header (CIMFIG | CIMENCAM | HOLCIM SPIRIT)
            BrandHeader()

            Spacer(modifier = Modifier.height(16.dp))

            // App Title
            Text(
                text = "Application Mobile",
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Subtitle Pills: Sécurité, Santé, Environnement
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HseBadgePill(text = "Sécurité")
                HseBadgePill(text = "Santé")
                HseBadgePill(text = "Environnement")
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Main Auth Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = HSECardBg.copy(alpha = 0.95f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                ) {
                    Text(
                        text = if (isRegisterMode) "Inscription" else "Connexion",
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = if (isRegisterMode) "Créez votre accès sécurisé" else "Accédez à votre espace sécurisé",
                        color = HSETextSecondary,
                        fontSize = 13.sp
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Error text if any
                    if (authState.authError != null) {
                        Surface(
                            color = HSERedUrgent.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                        ) {
                            Text(
                                text = authState.authError ?: "",
                                color = HSERedUrgent,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }

                    // Success / Pending Approval text if any
                    if (authState.authSuccess != null) {
                        Surface(
                            color = HSEGreenSafe.copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, HSEGreenSafe.copy(alpha = 0.4f)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = HSEGreenSafe, modifier = Modifier.size(18.dp))
                                Text(
                                    text = authState.authSuccess ?: "",
                                    color = HSEGreenSafe,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }

                    if (isRegisterMode) {
                        // Name Field
                        OutlinedTextField(
                            value = registerName,
                            onValueChange = { registerName = it },
                            placeholder = { Text("Nom complet (ex: Jean Dupont)", color = HSETextMuted, fontSize = 14.sp) },
                            leadingIcon = {
                                Icon(Icons.Default.Person, contentDescription = null, tint = HSETextSecondary)
                            },
                            modifier = Modifier.fillMaxWidth().testTag("register_name_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = HSEFieldBg,
                                unfocusedContainerColor = HSEFieldBg,
                                focusedBorderColor = HSEBlueLight,
                                unfocusedBorderColor = HSECardBorder,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Phone Field
                        OutlinedTextField(
                            value = registerPhone,
                            onValueChange = { registerPhone = it },
                            placeholder = { Text("Numéro de téléphone (ex: +237 690 12 34 56)", color = HSETextMuted, fontSize = 14.sp) },
                            leadingIcon = {
                                Icon(Icons.Default.Phone, contentDescription = null, tint = HSETextSecondary)
                            },
                            modifier = Modifier.fillMaxWidth().testTag("register_phone_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = HSEFieldBg,
                                unfocusedContainerColor = HSEFieldBg,
                                focusedBorderColor = HSEBlueLight,
                                unfocusedBorderColor = HSECardBorder,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Site Selection Label & Selector
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LocationOn,
                                    contentDescription = null,
                                    tint = HSEBlueLight,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Site d'affectation :",
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))

                            val sitesList = listOf(
                                "FIGUIL" to "Site de Figuil",
                                "NOMAYOS" to "Site de Nomayos",
                                "BONABERI" to "Site de Bonabéri"
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                sitesList.forEach { (siteCode, siteFullName) ->
                                    val isSelected = registerSite == siteFullName
                                    Surface(
                                        onClick = { registerSite = siteFullName },
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("site_option_${siteCode.lowercase()}"),
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (isSelected) HSEBluePrimary else HSEFieldBg,
                                        border = if (isSelected) BorderStroke(1.5.dp, HSEBlueLight) else BorderStroke(1.dp, HSECardBorder)
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 7.dp, horizontal = 4.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            Text(
                                                text = siteCode,
                                                color = if (isSelected) Color.White else HSETextSecondary,
                                                fontSize = 11.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    // Email Field
                    OutlinedTextField(
                        value = authState.emailInput,
                        onValueChange = { viewModel.onEmailChange(it) },
                        placeholder = { Text("Adresse e-mail professionnelle", color = HSETextMuted, fontSize = 14.sp) },
                        leadingIcon = {
                            Icon(Icons.Default.Email, contentDescription = null, tint = HSETextSecondary)
                        },
                        modifier = Modifier.fillMaxWidth().testTag("email_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = HSEFieldBg,
                            unfocusedContainerColor = HSEFieldBg,
                            focusedBorderColor = HSEBlueLight,
                            unfocusedBorderColor = HSECardBorder,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Password Field
                    OutlinedTextField(
                        value = authState.passwordInput,
                        onValueChange = { viewModel.onPasswordChange(it) },
                        placeholder = { Text("Mot de passe", color = HSETextMuted, fontSize = 14.sp) },
                        leadingIcon = {
                            Icon(Icons.Default.Lock, contentDescription = null, tint = HSETextSecondary)
                        },
                        trailingIcon = {
                            IconButton(
                                onClick = { viewModel.togglePasswordVisibility() },
                                modifier = Modifier.testTag("toggle_password_visibility")
                            ) {
                                Icon(
                                    imageVector = if (authState.isPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = "Toggle password visibility",
                                    tint = HSETextSecondary
                                )
                            }
                        },
                        visualTransformation = if (authState.isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth().testTag("password_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = HSEFieldBg,
                            unfocusedContainerColor = HSEFieldBg,
                            focusedBorderColor = HSEBlueLight,
                            unfocusedBorderColor = HSECardBorder,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                    )

                    if (!isRegisterMode) {
                        Spacer(modifier = Modifier.height(8.dp))
                        // Mot de passe oublié
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            contentAlignment = Alignment.CenterEnd
                        ) {
                            Text(
                                text = "Mot de passe oublié ?",
                                color = HSEBlueLight,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier
                                    .clickable { showForgotPasswordDialog = true }
                                    .testTag("forgot_password_button")
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Submit Button "Se connecter ->" / "S'inscrire ->"
                    Button(
                        onClick = {
                            if (isRegisterMode) {
                                viewModel.register(
                                    name = registerName,
                                    site = registerSite,
                                    phone = registerPhone
                                )
                                viewModel.setSite(registerSite)
                            } else {
                                viewModel.login()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .testTag("submit_auth_button"),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = HSEBluePrimary),
                        enabled = !authState.isSubmitting
                    ) {
                        if (authState.isSubmitting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = if (isRegisterMode) "S'inscrire" else "Se connecter",
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    if (!isRegisterMode) {
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Divider with "OU"
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        HorizontalDivider(
                            modifier = Modifier.weight(1f),
                            color = HSECardBorder
                        )
                        Text(
                            text = "  OU  ",
                            color = HSETextMuted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        HorizontalDivider(
                            modifier = Modifier.weight(1f),
                            color = HSECardBorder
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Google Sign-In button with Firebase Auth
                    OutlinedButton(
                        onClick = {
                            viewModel.loginWithGoogle(context)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                            .testTag("google_signin_button"),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = Color.White,
                            contentColor = Color(0xFF1F2937)
                        ),
                        border = BorderStroke(1.dp, Color(0xFFE5E7EB)),
                        enabled = !authState.isSubmitting
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Surface(
                                modifier = Modifier.size(20.dp),
                                shape = CircleShape,
                                color = Color.White
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "G",
                                        fontWeight = FontWeight.Black,
                                        fontSize = 15.sp,
                                        color = Color(0xFF4285F4)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = if (isRegisterMode) "S'inscrire avec Google" else "Continuer avec Google",
                                color = Color(0xFF1F2937),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Firebase status badge
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = HSEGreenSafe,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = "Authentification sécurisée Firebase & Firestore",
                            color = HSETextMuted,
                            fontSize = 11.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Switch between Login and Register
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isRegisterMode) "Déjà un compte ? " else "Pas encore de compte ? ",
                            color = HSETextSecondary,
                            fontSize = 13.sp
                        )
                        Text(
                            text = if (isRegisterMode) "Se connecter" else "Créer un accès",
                            color = HSEBlueLight,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .clickable { isRegisterMode = !isRegisterMode }
                                .testTag("switch_auth_mode")
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(36.dp))

            // Footer
            Text(
                text = "Groupe Holcim - CIMENCAM 2026",
                color = HSETextMuted,
                fontSize = 12.sp,
                textAlign = TextAlign.Center
            )
        }

        // Forgot Password Dialog
        if (showForgotPasswordDialog) {
            var resetEmail by remember { mutableStateOf(authState.emailInput) }
            var resetSent by remember { mutableStateOf(false) }
            var resetError by remember { mutableStateOf<String?>(null) }
            var isSendingReset by remember { mutableStateOf(false) }

            AlertDialog(
                onDismissRequest = { showForgotPasswordDialog = false },
                containerColor = HSECardBg,
                title = { Text("Réinitialisation mot de passe", color = Color.White, fontWeight = FontWeight.Bold) },
                text = {
                    Column {
                        if (!resetSent) {
                            Text(
                                text = "Entrez votre email CIMENCAM pour recevoir le lien de réinitialisation sécurisé :",
                                color = HSETextSecondary,
                                fontSize = 13.sp
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedTextField(
                                value = resetEmail,
                                onValueChange = { 
                                    resetEmail = it 
                                    resetError = null
                                },
                                placeholder = { Text("Email professionnel", color = HSETextMuted) },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = HSEFieldBg,
                                    unfocusedContainerColor = HSEFieldBg,
                                    focusedBorderColor = HSEBlueLight,
                                    unfocusedBorderColor = HSECardBorder,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            )
                            if (resetError != null) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = resetError ?: "",
                                    color = HSERedUrgent,
                                    fontSize = 12.sp
                                )
                            }
                        } else {
                            Text(
                                text = "Instructions envoyées avec succès à $resetEmail. Vérifiez votre boîte de réception.",
                                color = HSEGreenSafe,
                                fontSize = 14.sp
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (resetSent) {
                                showForgotPasswordDialog = false
                            } else {
                                isSendingReset = true
                                viewModel.sendPasswordReset(resetEmail) { success, message ->
                                    isSendingReset = false
                                    if (success) {
                                        resetSent = true
                                    } else {
                                        resetError = message
                                    }
                                }
                            }
                        },
                        enabled = !isSendingReset,
                        colors = ButtonDefaults.buttonColors(containerColor = HSEBluePrimary)
                    ) {
                        if (isSendingReset) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                        } else {
                            Text(if (resetSent) "Fermer" else "Envoyer le lien")
                        }
                    }
                },
                dismissButton = {
                    if (!resetSent) {
                        TextButton(onClick = { showForgotPasswordDialog = false }) {
                            Text("Annuler", color = HSETextSecondary)
                        }
                    }
                }
            )
        }
    }
}

@Composable
private fun HseBadgePill(text: String) {
    Surface(
        color = Color.White.copy(alpha = 0.15f),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
    ) {
        Text(
            text = text,
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
        )
    }
}
