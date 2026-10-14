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
            .background(HSEDarkBg)
    ) {
        // Top Brand Banner Background
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            HSEBlueDark,
                            HSEBluePrimary
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Top Brand Header (CIMFIG | CIMENCAM | HOLCIM SPIRIT)
            BrandHeader()

            Spacer(modifier = Modifier.height(12.dp))

            // App Title
            Text(
                text = "Application Mobile HSE",
                color = Color.White,
                fontSize = 23.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Subtitle Pills: Sécurité, Santé, Environnement
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HseBadgePill(text = "Sécurité")
                HseBadgePill(text = "Santé")
                HseBadgePill(text = "Environnement")
            }

            Spacer(modifier = Modifier.height(22.dp))

            // Main Auth Card - 100% Solid White for maximum clarity & contrast
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.5.dp, HSECardBorder),
                elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                ) {
                    Text(
                        text = if (isRegisterMode) "Inscription" else "Connexion",
                        color = HSETextPrimary,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = if (isRegisterMode) "Créez votre accès sécurisé CIMENCAM" else "Accédez à votre espace sécurisé HSE",
                        color = HSETextSecondary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Error text if any
                    if (authState.authError != null) {
                        Surface(
                            color = HSERedUrgent.copy(alpha = 0.12f),
                            border = BorderStroke(1.dp, HSERedUrgent),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = HSERedUrgent,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = authState.authError ?: "",
                                    color = HSERedDark,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    // Success / Pending Approval text if any
                    if (authState.authSuccess != null) {
                        Surface(
                            color = HSEEnvBg,
                            border = BorderStroke(1.dp, HSEGreenSafe),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = HSEGreenSafe, modifier = Modifier.size(20.dp))
                                Text(
                                    text = authState.authSuccess ?: "",
                                    color = HSEBlueDark,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    if (isRegisterMode) {
                        // Name Field Label + Input
                        Text(
                            text = "Nom complet",
                            color = HSETextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                        OutlinedTextField(
                            value = registerName,
                            onValueChange = { registerName = it },
                            placeholder = {
                                Text(
                                    "Ex: Jean Dupont",
                                    color = HSETextSecondary.copy(alpha = 0.75f),
                                    fontSize = 14.sp
                                )
                            },
                            leadingIcon = {
                                Icon(Icons.Default.Person, contentDescription = null, tint = HSEBluePrimary)
                            },
                            textStyle = LocalTextStyle.current.copy(
                                color = HSETextPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold
                            ),
                            modifier = Modifier.fillMaxWidth().testTag("register_name_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = HSEFieldBg,
                                focusedBorderColor = HSEBluePrimary,
                                unfocusedBorderColor = Color(0xFFB0BEC5),
                                focusedTextColor = HSETextPrimary,
                                unfocusedTextColor = HSETextPrimary,
                                cursorColor = HSEBluePrimary
                            ),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Phone Field Label + Input
                        Text(
                            text = "Numéro de téléphone",
                            color = HSETextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                        OutlinedTextField(
                            value = registerPhone,
                            onValueChange = { registerPhone = it },
                            placeholder = {
                                Text(
                                    "Ex: +237 690 12 34 56",
                                    color = HSETextSecondary.copy(alpha = 0.75f),
                                    fontSize = 14.sp
                                )
                            },
                            leadingIcon = {
                                Icon(Icons.Default.Phone, contentDescription = null, tint = HSEBluePrimary)
                            },
                            textStyle = LocalTextStyle.current.copy(
                                color = HSETextPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold
                            ),
                            modifier = Modifier.fillMaxWidth().testTag("register_phone_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = HSEFieldBg,
                                focusedBorderColor = HSEBluePrimary,
                                unfocusedBorderColor = Color(0xFFB0BEC5),
                                focusedTextColor = HSETextPrimary,
                                unfocusedTextColor = HSETextPrimary,
                                cursorColor = HSEBluePrimary
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
                                    tint = HSEBluePrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "Site d'affectation :",
                                    color = HSETextPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
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
                                        border = if (isSelected) BorderStroke(2.dp, HSEBlueDark) else BorderStroke(1.dp, Color(0xFFB0BEC5))
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 10.dp, horizontal = 4.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            Text(
                                                text = if (isSelected) "✔ $siteCode" else siteCode,
                                                color = if (isSelected) Color.White else HSETextPrimary,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                    }

                    // Email Field Label + Input
                    Text(
                        text = "Adresse e-mail professionnelle",
                        color = HSETextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    OutlinedTextField(
                        value = authState.emailInput,
                        onValueChange = { viewModel.onEmailChange(it) },
                        placeholder = {
                            Text(
                                "prenom.nom@cimencam.cm",
                                color = HSETextSecondary.copy(alpha = 0.75f),
                                fontSize = 14.sp
                            )
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Email, contentDescription = null, tint = HSEBluePrimary)
                        },
                        textStyle = LocalTextStyle.current.copy(
                            color = HSETextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("email_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = HSEFieldBg,
                            focusedBorderColor = HSEBluePrimary,
                            unfocusedBorderColor = Color(0xFFB0BEC5),
                            focusedTextColor = HSETextPrimary,
                            unfocusedTextColor = HSETextPrimary,
                            cursorColor = HSEBluePrimary
                        ),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Password Field Label + Input
                    Text(
                        text = "Mot de passe",
                        color = HSETextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    OutlinedTextField(
                        value = authState.passwordInput,
                        onValueChange = { viewModel.onPasswordChange(it) },
                        placeholder = {
                            Text(
                                "Votre mot de passe",
                                color = HSETextSecondary.copy(alpha = 0.75f),
                                fontSize = 14.sp
                            )
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Lock, contentDescription = null, tint = HSEBluePrimary)
                        },
                        trailingIcon = {
                            IconButton(
                                onClick = { viewModel.togglePasswordVisibility() },
                                modifier = Modifier.testTag("toggle_password_visibility")
                            ) {
                                Icon(
                                    imageVector = if (authState.isPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = "Afficher ou masquer le mot de passe",
                                    tint = HSETextPrimary
                                )
                            }
                        },
                        textStyle = LocalTextStyle.current.copy(
                            color = HSETextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        ),
                        visualTransformation = if (authState.isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth().testTag("password_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = HSEFieldBg,
                            focusedBorderColor = HSEBluePrimary,
                            unfocusedBorderColor = Color(0xFFB0BEC5),
                            focusedTextColor = HSETextPrimary,
                            unfocusedTextColor = HSETextPrimary,
                            cursorColor = HSEBluePrimary
                        ),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                    )

                    if (!isRegisterMode) {
                        Spacer(modifier = Modifier.height(8.dp))
                        // Compte ou mot de passe oublié
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            contentAlignment = Alignment.CenterEnd
                        ) {
                            Text(
                                text = "Compte ou mot de passe oublié ?",
                                color = HSEBlueLight,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .clickable { showForgotPasswordDialog = true }
                                    .testTag("forgot_password_button")
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Submit Button "Se connecter ->" / "S'inscrire ->"
                    Button(
                        onClick = {
                            if (isRegisterMode) {
                                viewModel.register(
                                    name = registerName,
                                    site = registerSite,
                                    phone = registerPhone
                                )
                            } else {
                                viewModel.login()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("submit_auth_button"),
                        shape = RoundedCornerShape(12.dp),
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
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
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
                            color = HSETextSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
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
                            viewModel.loginWithGoogle(
                                context = context,
                                isRegisterMode = isRegisterMode,
                                registerNameHint = registerName,
                                registerSiteHint = registerSite,
                                registerPhoneHint = registerPhone
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("google_signin_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = Color.White,
                            contentColor = HSETextPrimary
                        ),
                        border = BorderStroke(1.5.dp, Color(0xFFB0BEC5)),
                        enabled = !authState.isSubmitting
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Surface(
                                modifier = Modifier.size(22.dp),
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
                                color = HSETextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

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
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = "Comptes synchronisés sur Firebase Auth & Firestore",
                            color = HSETextSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Switch between Login and Register
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isRegisterMode) "Déjà un compte ? " else "Pas encore de compte ? ",
                            color = HSETextSecondary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = if (isRegisterMode) "Se connecter" else "Créer un accès",
                            color = HSEBluePrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier
                                .clickable { isRegisterMode = !isRegisterMode }
                                .testTag("switch_auth_mode")
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Footer
            Text(
                text = "Groupe Holcim - CIMENCAM 2026",
                color = HSETextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )
        }

        // Forgot Account / Password Dialog (Firebase Auth + Admin Account Recovery)
        if (showForgotPasswordDialog) {
            var resetIdentifier by remember { mutableStateOf(authState.emailInput) }
            var resetNote by remember { mutableStateOf("") }
            var resetSent by remember { mutableStateOf(false) }
            var resetSuccessMsg by remember { mutableStateOf("") }
            var resetError by remember { mutableStateOf<String?>(null) }
            var isSendingReset by remember { mutableStateOf(false) }

            AlertDialog(
                onDismissRequest = { showForgotPasswordDialog = false },
                containerColor = Color.White,
                title = {
                    Text(
                        "Récupération de compte ou mot de passe",
                        color = HSETextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp
                    )
                },
                text = {
                    Column {
                        if (!resetSent) {
                            Text(
                                text = "Indiquez votre e-mail (ou votre nom / téléphone en cas d'oubli d'e-mail). Un lien Firebase Auth sera envoyé et l'administrateur HSE pourra gérer ou réinitialiser votre accès :",
                                color = HSETextSecondary,
                                fontSize = 13.sp,
                                lineHeight = 18.sp
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedTextField(
                                value = resetIdentifier,
                                onValueChange = {
                                    resetIdentifier = it
                                    resetError = null
                                },
                                label = { Text("E-mail, Nom ou Téléphone", color = HSETextPrimary) },
                                placeholder = { Text("Ex: nom@cimencam.cm ou +237...", color = HSETextSecondary.copy(alpha = 0.75f)) },
                                textStyle = LocalTextStyle.current.copy(
                                    color = HSETextPrimary,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold
                                ),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = Color.White,
                                    unfocusedContainerColor = HSEFieldBg,
                                    focusedBorderColor = HSEBluePrimary,
                                    unfocusedBorderColor = Color(0xFFB0BEC5),
                                    focusedTextColor = HSETextPrimary,
                                    unfocusedTextColor = HSETextPrimary,
                                    cursorColor = HSEBluePrimary
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            OutlinedTextField(
                                value = resetNote,
                                onValueChange = { resetNote = it },
                                label = { Text("Message pour l'administrateur (optionnel)", color = HSETextPrimary) },
                                placeholder = { Text("Ex: Mot de passe oublié / E-mail oublié", color = HSETextSecondary.copy(alpha = 0.75f)) },
                                textStyle = LocalTextStyle.current.copy(
                                    color = HSETextPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                ),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = Color.White,
                                    unfocusedContainerColor = HSEFieldBg,
                                    focusedBorderColor = HSEBluePrimary,
                                    unfocusedBorderColor = Color(0xFFB0BEC5),
                                    focusedTextColor = HSETextPrimary,
                                    unfocusedTextColor = HSETextPrimary,
                                    cursorColor = HSEBluePrimary
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                            if (resetError != null) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = resetError ?: "",
                                    color = HSERedUrgent,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        } else {
                            Text(
                                text = resetSuccessMsg,
                                color = HSEGreenSafe,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                lineHeight = 20.sp
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
                                viewModel.sendPasswordReset(resetIdentifier, resetNote) { success, message ->
                                    isSendingReset = false
                                    if (success) {
                                        resetSuccessMsg = message
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
                            Text(if (resetSent) "Fermer" else "Demander la récupération", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                },
                dismissButton = {
                    if (!resetSent) {
                        TextButton(onClick = { showForgotPasswordDialog = false }) {
                            Text("Annuler", color = HSETextSecondary, fontWeight = FontWeight.SemiBold)
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
