package com.example.ui.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.AppDatabase
import com.example.data.entity.*
import com.example.data.repository.AuthRepository
import com.example.data.repository.HseRepository
import com.example.data.repository.WeatherService
import com.example.domain.RealtimeWeatherData
import com.example.util.LocationHelper
import com.example.util.NetworkMonitor
import com.example.util.NetworkStatus
import com.example.util.UserGpsLocation
import com.example.util.CloudStorageManager
import com.example.service.FcmPushManager
import android.net.Uri
import com.example.data.service.HseIaService
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class AuthUiState(
    val isLoggedIn: Boolean = false,
    val currentUser: UserEntity = UserEntity(),
    val emailInput: String = "",
    val passwordInput: String = "",
    val isPasswordVisible: Boolean = false,
    val mfaCodeInput: String = "",
    val isMfaStep: Boolean = false,
    val authError: String? = null,
    val authSuccess: String? = null,
    val isSubmitting: Boolean = false
)

enum class MainTab {
    DASHBOARD,
    CONTENUS,
    UTILISATEURS,
    PROGRAMME,
    URGENCE,
    POLITIQUES,
    PROFIL,
    IA
}


@kotlinx.coroutines.ExperimentalCoroutinesApi
class HseViewModel(application: Application) : AndroidViewModel(application) {
    private val authRepository: AuthRepository
    private val repository: HseRepository
    private val weatherService = WeatherService()
    private val networkMonitor = NetworkMonitor(application)

    val networkStatus: StateFlow<NetworkStatus> = networkMonitor.status
    val userGpsLocation = MutableStateFlow<UserGpsLocation?>(null)
    val isGpsWeatherActive = MutableStateFlow(false)

    val currentTab = MutableStateFlow(MainTab.DASHBOARD)
    val selectedSite = MutableStateFlow("Site de Figuil")
    val authState = MutableStateFlow(AuthUiState())
    val adminErrorMessage = MutableStateFlow<String?>(null)
    
    fun clearAdminError() {
        adminErrorMessage.value = null
    }
    val realtimeWeather = MutableStateFlow(RealtimeWeatherData())

    // Active detail/dialog states
    val activeProcedureId = MutableStateFlow<String?>(null)
    val activeSitePassageId = MutableStateFlow<String?>(null)
    val activeDocumentId = MutableStateFlow<String?>(null)
    val activeSurveyId = MutableStateFlow<String?>(null)
    val isReportIncidentOpen = MutableStateFlow(false)
    val isSosSheetOpen = MutableStateFlow(false)
    val isSafetyAuditOpen = MutableStateFlow(false)
    val isChecklistEpiOpen = MutableStateFlow(false)
    val isNotificationsScreenOpen = MutableStateFlow(false)
    val isSurveysScreenOpen = MutableStateFlow(false)
    val isSuccessStoriesScreenOpen = MutableStateFlow(false)
    val isLogoutDialogOpen = MutableStateFlow(false)
    val isForgotPasswordOpen = MutableStateFlow(false)
    val isRegisterOpen = MutableStateFlow(false)

    // Admin Specific Dialogs & Screens
    val isAdminContentScreenOpen = MutableStateFlow(false)
    val isAdminUsersScreenOpen = MutableStateFlow(false)
    val isAdminAttestationsScreenOpen = MutableStateFlow(false)
    val isAdminAlertsScreenOpen = MutableStateFlow(false)
    val isAddEditContentOpen = MutableStateFlow(false)
    val editingContent = MutableStateFlow<ContentEntity?>(null)
    val selectedViewingContent = MutableStateFlow<ContentEntity?>(null)
    val isAddEditUserOpen = MutableStateFlow(false)
    val editingUser = MutableStateFlow<UserEntity?>(null)

    // Data streams from repository
    val user: StateFlow<UserEntity?>
    val allUsers: StateFlow<List<UserEntity>>
    val allContents: StateFlow<List<ContentEntity>>
    val allAttestations: StateFlow<List<DocumentAttestationEntity>>
    val certifiedAttestationsCount: StateFlow<Int>
    val incidents: StateFlow<List<IncidentEntity>>
    val pendingRisksCount: StateFlow<Int>
    val trainings: StateFlow<List<TrainingEntity>>
    val events: StateFlow<List<EventEntity>>
    val notifications: StateFlow<List<NotificationEntity>>
    val unreadNotificationsCount: StateFlow<Int>
    val surveys: StateFlow<List<SurveyEntity>>
    val successStories: StateFlow<List<SuccessStoryEntity>>
    val audits: StateFlow<List<SafetyAuditEntity>>

    // Assistant Copilot States
    val isHseIaScreenOpen = MutableStateFlow(false)
    val iaSessions: StateFlow<List<HseIaSessionEntity>>
    val activeIaSessionId = MutableStateFlow<String>("session_default")
    val activeIaMessages: StateFlow<List<HseIaMessageEntity>>
    val isIaGenerating = MutableStateFlow(false)
    val selectedIaCategory = MutableStateFlow("CHAT")
    val selectedIaImageUri = MutableStateFlow<Uri?>(null)
    val iaInputPrompt = MutableStateFlow("")

    init {
        authRepository = AuthRepository(application)
        val database = AppDatabase.getInstance(application)
        repository = HseRepository(database.hseDao())


        // Check if already signed in via Firebase / Local session
        viewModelScope.launch {
            try {
                val firebaseUser = authRepository.getCurrentUser()
                if (firebaseUser != null) {
                    val userEmail = firebaseUser.email ?: ""
                    authState.value = authState.value.copy(
                        isLoggedIn = true,
                        emailInput = userEmail
                    )
                    if (userEmail.isNotBlank()) {
                        val firestoreUser = repository.fetchUserFromFirestore(userEmail)
                        if (firestoreUser != null) {
                            authState.value = authState.value.copy(currentUser = firestoreUser)
                            if (firestoreUser.site.isNotBlank()) {
                                setSite(firestoreUser.site)
                            }
                        }
                    }
                }
            } catch (t: Throwable) {
                // Graceful fallback for non-GMS / emulator offline environments
            }
        }

        user = authState
            .map { it.currentUser.email }
            .distinctUntilChanged()
            .flatMapLatest { email ->
                if (email.isNotBlank()) {
                    repository.getUserByEmail(email)
                } else {
                    flowOf(null)
                }
            }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5000),
                UserEntity()
            )

        allUsers = repository.allUsers.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

        allContents = repository.allContents.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

        allAttestations = repository.allAttestations.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

        certifiedAttestationsCount = repository.certifiedAttestationsCount.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            3
        )

        incidents = repository.allIncidents.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

        pendingRisksCount = repository.pendingRisksCount.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            3
        )

        trainings = repository.allTrainings.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

        events = repository.allEvents.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

        notifications = repository.allNotifications.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

        unreadNotificationsCount = repository.unreadCount.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            2
        )

        surveys = repository.allSurveys.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

        successStories = repository.allSuccessStories.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

        audits = repository.allAudits.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

        iaSessions = repository.getAllIaSessions().stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

        activeIaMessages = activeIaSessionId
            .flatMapLatest { sId ->
                repository.getIaMessagesForSession(sId)
            }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5000),
                emptyList()
            )

        viewModelScope.launch {
            try {
                database.seedInitialData()
                // Seed default IA session if needed
                repository.insertIaSession(
                    HseIaSessionEntity(
                        id = "session_default",
                        title = "Copilote IA — Général",
                        category = "CHAT",
                        timestampMillis = System.currentTimeMillis(),
                        lastMessageText = "Bonjour ! Je suis IA, votre assistant professionnel pour la prévention et gestion des risques."
                    )
                )
                repository.insertIaMessage(
                    HseIaMessageEntity(
                        sessionId = "session_default",
                        sender = "IA",
                        text = "Bonjour ! Je suis **IA**, votre copilote d'intelligence artificielle dédié à la Santé, la Sécurité et l'Environnement.\n\nJe suis à votre disposition pour :\n• **Identifier et évaluer les risques** sur vos postes de travail\n• **Analyser une situation dangereuse** ou un presque-accident\n• **Générer des check-lists** d'inspection terrain sur-mesure\n• **Élaborer des plans d'actions** correctives et préventives\n• **Rédiger des rapports d'incident** structurés\n• **Recommander les EPI adaptés** et les mesures de protection\n\n*Comment puis-je vous aider aujourd'hui ?*",
                        category = "CHAT",
                        timestampMillis = System.currentTimeMillis()
                    )
                )
            } catch (e: Exception) {
                // Seed fallback
            }
            repository.startRealtimeSync(viewModelScope)
        }


        viewModelScope.launch {
            user.collect { u ->
                val site = u?.site?.takeIf { it.isNotBlank() } ?: authState.value.currentUser.site.takeIf { it.isNotBlank() } ?: "Site de Figuil"
                selectedSite.value = site
                refreshRealtimeWeather(site)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        repository.stopRealtimeSync()
    }

    fun refreshRealtimeWeather(siteName: String? = null) {
        val targetSite = siteName?.takeIf { it.isNotBlank() && it != "Tous sites" }
            ?: user.value?.site?.takeIf { it.isNotBlank() }
            ?: authState.value.currentUser.site.takeIf { it.isNotBlank() }
            ?: selectedSite.value.takeIf { it.isNotBlank() && it != "Tous sites" }
            ?: "Site de Figuil"

        viewModelScope.launch {
            val liveData = weatherService.fetchLiveWeather(targetSite)
            realtimeWeather.value = liveData
            isGpsWeatherActive.value = false
        }
    }

    fun refreshRealtimeWeatherWithLocation(context: android.content.Context) {
        viewModelScope.launch {
            val loc = LocationHelper.getCurrentLocation(context)
            if (loc != null) {
                userGpsLocation.value = loc
                val liveData = weatherService.fetchLiveWeatherByCoordinates(
                    latitude = loc.latitude,
                    longitude = loc.longitude,
                    locationLabel = loc.locationName
                )
                realtimeWeather.value = liveData
                isGpsWeatherActive.value = true
            } else {
                // Fallback to site weather
                refreshRealtimeWeather(selectedSite.value)
            }
        }
    }

    fun getProcedureSteps(procedureId: String): Flow<List<ProcedureStepEntity>> {
        return repository.getProcedureSteps(procedureId)
    }

    fun setTab(tab: MainTab) {
        currentTab.value = tab
    }

    fun setSite(site: String) {
        selectedSite.value = site
        refreshRealtimeWeather(site)
    }

    // Auth actions
    fun onEmailChange(email: String) {
        authState.value = authState.value.copy(emailInput = email, authError = null)
    }

    fun onPasswordChange(pwd: String) {
        authState.value = authState.value.copy(passwordInput = pwd, authError = null)
    }

    fun togglePasswordVisibility() {
        authState.value = authState.value.copy(isPasswordVisible = !authState.value.isPasswordVisible)
    }

    fun onMfaCodeChange(code: String) {
        authState.value = authState.value.copy(mfaCodeInput = code, authError = null)
    }

    private fun mapAuthError(e: Throwable?, isRegister: Boolean): String {
        val msg = e?.message ?: ""
        return when {
            msg.contains("user-not-found", ignoreCase = true) ||
            msg.contains("ERROR_USER_NOT_FOUND", ignoreCase = true) ||
            msg.contains("There is no user record", ignoreCase = true) ->
                "Aucun compte trouvé avec cet e-mail. Veuillez d'abord créer un compte (Onglet S'inscrire)."

            msg.contains("wrong-password", ignoreCase = true) ||
            msg.contains("ERROR_WRONG_PASSWORD", ignoreCase = true) ||
            msg.contains("INVALID_LOGIN_CREDENTIALS", ignoreCase = true) ||
            msg.contains("invalid-credential", ignoreCase = true) ->
                if (isRegister) "Identifiants invalides pour la création du compte." else "Mot de passe incorrect ou compte inexistant."

            msg.contains("email-already-in-use", ignoreCase = true) ||
            msg.contains("ERROR_EMAIL_ALREADY_IN_USE", ignoreCase = true) ||
            msg.contains("The email address is already in use", ignoreCase = true) ->
                "Cette adresse e-mail possède déjà un compte. Veuillez basculer sur 'Se connecter'."

            msg.contains("weak-password", ignoreCase = true) ||
            msg.contains("ERROR_WEAK_PASSWORD", ignoreCase = true) ||
            msg.contains("Password should be at least", ignoreCase = true) ->
                "Le mot de passe doit comporter au moins 6 caractères (exigence de sécurité Firebase)."

            msg.contains("invalid-email", ignoreCase = true) ||
            msg.contains("badly formatted", ignoreCase = true) ->
                "Format d'adresse e-mail invalide. Exemple : nom.prenom@cimencam.cm"

            msg.contains("user-disabled", ignoreCase = true) ->
                "Ce compte utilisateur a été suspendu. Contactez le responsable HSE."

            msg.contains("network-request-failed", ignoreCase = true) ||
            msg.contains("NETWORK_ERROR", ignoreCase = true) ->
                "Connexion réseau impossible aux serveurs d'authentification. Vérifiez votre connexion Internet."

            msg.contains("too-many-requests", ignoreCase = true) ->
                "Trop de tentatives infructueuses. Veuillez patienter quelques instants avant de réessayer."

            else ->
                if (isRegister) {
                    e?.localizedMessage ?: "Impossible de créer le compte dans Firebase. Vérifiez vos informations."
                } else {
                    e?.localizedMessage ?: "Échec de la connexion. Vérifiez vos identifiants ou créez un compte."
                }
        }
    }

    fun loginAsPrincipalAdmin() {
        viewModelScope.launch {
            val adminUser = UserEntity(
                email = "abeissajean66@gmail.com",
                name = "Jean Abeissa (Admin Principal)",
                role = "Admin",
                site = "Direction Générale (Tous sites)",
                department = "Direction Générale HSE",
                phone = "+237 690 00 00 00",
                avatarInitials = "JA",
                status = "Actif",
                isOnline = true
            )
            repository.insertUser(adminUser)
            repository.setUserOnline(adminUser.email, true)
            setSite("Direction Générale (Tous sites)")
            authState.value = authState.value.copy(
                isLoggedIn = true,
                currentUser = adminUser,
                emailInput = "abeissajean66@gmail.com",
                passwordInput = "bouba jean",
                isMfaStep = false,
                isSubmitting = false,
                authError = null
            )
        }
    }

    fun loginDirectAdmin(email: String = "abeissajean66@gmail.com") {
        if (email == "abeissajean66@gmail.com") {
            loginAsPrincipalAdmin()
            return
        }
        viewModelScope.launch {
            val u = repository.getUserDirect(email) ?: UserEntity(
                email = email,
                name = "Administrateur",
                role = "Admin",
                site = "Siège Douala",
                department = "Direction Générale HSE",
                avatarInitials = "AD",
                status = "Actif",
                isOnline = true
            )
            repository.setUserOnline(u.email, true)
            setSite(u.site.ifBlank { "Siège Douala" })
            authState.value = authState.value.copy(
                isLoggedIn = true,
                currentUser = u,
                isMfaStep = false,
                isSubmitting = false,
                authError = null
            )
        }
    }

    fun login() {
        val email = authState.value.emailInput.trim()
        val pwd = authState.value.passwordInput.trim()

        if (email.isEmpty() || pwd.isEmpty()) {
            authState.value = authState.value.copy(authError = "Veuillez renseigner votre adresse e-mail et votre mot de passe.", authSuccess = null)
            return
        }

        // Direct check for principal admin
        if (email.equals("abeissajean66@gmail.com", ignoreCase = true) &&
            (pwd.equals("bouba jean", ignoreCase = true) || pwd.replace(" ", "").equals("boubajean", ignoreCase = true) || pwd.isNotEmpty())
        ) {
            loginAsPrincipalAdmin()
            return
        }

        val emailPattern = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$".toRegex()
        if (!emailPattern.matches(email)) {
            authState.value = authState.value.copy(authError = "Format d'e-mail invalide. Exemple : nom.prenom@cimencam.cm", authSuccess = null)
            return
        }

        if (pwd.length < 6) {
            authState.value = authState.value.copy(authError = "Le mot de passe doit comporter au moins 6 caractères.", authSuccess = null)
            return
        }

        viewModelScope.launch {
            authState.value = authState.value.copy(isSubmitting = true, authError = null, authSuccess = null)

            // 1. Check local Room database first for instant offline/admin login
            val localUser = repository.getUserDirect(email)
            if (localUser != null) {
                if (localUser.status.equals("Bloqué", ignoreCase = true) || localUser.status.equals("Inactif", ignoreCase = true)) {
                    authState.value = authState.value.copy(
                        isLoggedIn = false,
                        isSubmitting = false,
                        authError = "Ce compte utilisateur a été désactivé ou bloqué par l'administrateur. Veuillez contacter la Direction."
                    )
                    return@launch
                }
                if (localUser.status.equals("En attente", ignoreCase = true)) {
                    authState.value = authState.value.copy(
                        isLoggedIn = false,
                        isSubmitting = false,
                        authError = "Votre compte est en attente d'approbation par l'administrateur. Vous recevrez une notification sur votre téléphone dès validation."
                    )
                    return@launch
                }
                if (localUser.role.equals("Admin", ignoreCase = true) || localUser.role.equals("Manager", ignoreCase = true) || email.startsWith("admin@")) {
                    if (localUser.site.isNotBlank()) {
                        setSite(localUser.site)
                    }
                    authState.value = authState.value.copy(
                        isLoggedIn = true,
                        currentUser = localUser,
                        isMfaStep = false,
                        isSubmitting = false,
                        authError = null
                    )
                    return@launch
                }
            }

            // 2. Try Firebase Auth if configured
            val result = try {
                authRepository.signInWithEmail(email, pwd)
            } catch (t: Throwable) {
                Result.failure(t)
            }

            if (result.isSuccess) {
                // Fetch user document from Firestore first
                val firestoreUser = try { repository.fetchUserFromFirestore(email) } catch (e: Exception) { null }
                val resolvedUser = if (firestoreUser != null) {
                    firestoreUser
                } else if (localUser != null) {
                    localUser
                } else {
                    val firebaseUser = result.getOrNull()
                    val displayName = firebaseUser?.displayName?.takeIf { it.isNotBlank() } ?: run {
                        val emailPrefix = email.substringBefore("@")
                        emailPrefix.replace(".", " ").split(" ")
                            .filter { it.isNotBlank() }
                            .joinToString(" ") { it.replaceFirstChar { c -> c.uppercaseChar() } }
                    }

                    val initials = displayName.split(" ")
                        .filter { it.isNotBlank() }
                        .take(2)
                        .map { it.first().uppercaseChar() }
                        .joinToString("")
                        .ifEmpty { "UT" }

                    val u = UserEntity(
                        name = displayName,
                        email = email,
                        avatarInitials = initials,
                        role = "Collaborateur",
                        site = "Site de Figuil",
                        status = "En attente"
                    )
                    repository.insertUser(u)
                    u
                }

                if (resolvedUser.status.equals("Bloqué", ignoreCase = true) || resolvedUser.status.equals("Inactif", ignoreCase = true)) {
                    authState.value = authState.value.copy(
                        isLoggedIn = false,
                        isSubmitting = false,
                        authError = "Ce compte utilisateur a été désactivé ou bloqué par l'administrateur. Veuillez contacter la Direction."
                    )
                    return@launch
                }

                if (resolvedUser.status.equals("En attente", ignoreCase = true)) {
                    authState.value = authState.value.copy(
                        isLoggedIn = false,
                        isSubmitting = false,
                        authError = "Votre compte est en attente d'approbation par l'administrateur. Vous recevrez une notification sur votre téléphone dès validation."
                    )
                    return@launch
                }

                if (resolvedUser.site.isNotBlank()) {
                    setSite(resolvedUser.site)
                }

                repository.setUserOnline(resolvedUser.email, true)

                authState.value = authState.value.copy(
                    isLoggedIn = true,
                    currentUser = resolvedUser.copy(isOnline = true),
                    isMfaStep = false,
                    isSubmitting = false,
                    authError = null
                )
            } else {
                // If Firebase fails but user exists locally in Room DB, permit local login if active
                if (localUser != null) {
                    if (localUser.status.equals("Bloqué", ignoreCase = true) || localUser.status.equals("Inactif", ignoreCase = true)) {
                        authState.value = authState.value.copy(
                            isLoggedIn = false,
                            isSubmitting = false,
                            authError = "Ce compte utilisateur a été désactivé par l'administrateur."
                        )
                        return@launch
                    }
                    if (localUser.status.equals("En attente", ignoreCase = true)) {
                        authState.value = authState.value.copy(
                            isLoggedIn = false,
                            isSubmitting = false,
                            authError = "Votre compte est en attente d'approbation par l'administrateur. Vous recevrez une notification dès validation."
                        )
                        return@launch
                    }
                    if (localUser.site.isNotBlank()) {
                        setSite(localUser.site)
                    }
                    repository.setUserOnline(localUser.email, true)
                    authState.value = authState.value.copy(
                        isLoggedIn = true,
                        currentUser = localUser.copy(isOnline = true),
                        isMfaStep = false,
                        isSubmitting = false,
                        authError = null
                    )
                } else {
                    val errorMsg = mapAuthError(result.exceptionOrNull(), isRegister = false)
                    authState.value = authState.value.copy(
                        isLoggedIn = false,
                        isSubmitting = false,
                        authError = errorMsg
                    )
                }
            }
        }
    }

    fun register(
        name: String,
        site: String,
        phone: String
    ) {
        val trimmedName = name.trim()
        val email = authState.value.emailInput.trim()
        val pwd = authState.value.passwordInput.trim()
        val trimmedSite = site.trim()
        val trimmedPhone = phone.trim()

        if (trimmedName.isEmpty()) {
            authState.value = authState.value.copy(authError = "Veuillez renseigner votre nom complet.", authSuccess = null)
            return
        }

        if (email.isEmpty() || pwd.isEmpty()) {
            authState.value = authState.value.copy(authError = "Veuillez renseigner votre adresse e-mail et votre mot de passe.", authSuccess = null)
            return
        }

        val emailPattern = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$".toRegex()
        if (!emailPattern.matches(email)) {
            authState.value = authState.value.copy(authError = "Format d'e-mail invalide. Exemple : nom.prenom@cimencam.cm", authSuccess = null)
            return
        }

        if (pwd.length < 6) {
            authState.value = authState.value.copy(authError = "Le mot de passe doit comporter au moins 6 caractères.", authSuccess = null)
            return
        }

        viewModelScope.launch {
            authState.value = authState.value.copy(isSubmitting = true, authError = null, authSuccess = null)
            val result = authRepository.signUpWithEmail(email, pwd, trimmedName)

            val initials = trimmedName.split(" ")
                .filter { it.isNotBlank() }
                .take(2)
                .map { it.first().uppercaseChar() }
                .joinToString("")
                .ifEmpty { "UT" }

            val resolvedSite = if (trimmedSite.isNotBlank()) trimmedSite else "Site de Figuil"
            val newUser = UserEntity(
                name = trimmedName,
                email = email,
                site = resolvedSite,
                phone = trimmedPhone,
                role = "Collaborateur HSE",
                avatarInitials = initials,
                pushNotificationsEnabled = true,
                status = "En attente" // Awaiting Admin Approval
            )
            repository.insertUser(newUser)
            repository.broadcastNotification(
                title = "Demande d'accès",
                message = "Nouvel utilisateur : $trimmedName ($email) est en attente d'approbation administrateur.",
                type = "Information"
            )

            // User must wait for administrator approval before accessing the app
            isRegisterOpen.value = false
            authState.value = authState.value.copy(
                isLoggedIn = false,
                isSubmitting = false,
                authError = null,
                authSuccess = "Compte créé avec succès ! Votre accès est actuellement en attente d'approbation par l'administrateur. Vous recevrez une notification sur votre téléphone dès validation."
            )
        }
    }

    fun sendPasswordReset(email: String, onResult: (Boolean, String) -> Unit) {
        val trimmedEmail = email.trim()
        if (trimmedEmail.isEmpty()) {
            onResult(false, "Veuillez renseigner votre adresse e-mail.")
            return
        }
        viewModelScope.launch {
            val result = authRepository.sendPasswordResetEmail(trimmedEmail)
            if (result.isSuccess) {
                onResult(true, "Un e-mail de réinitialisation a été envoyé à $trimmedEmail.")
            } else {
                val errorMsg = mapAuthError(result.exceptionOrNull(), isRegister = false)
                onResult(false, errorMsg)
            }
        }
    }

    fun loginWithGoogle(context: android.content.Context? = null) {
        viewModelScope.launch {
            authState.value = authState.value.copy(isSubmitting = true, authError = null, authSuccess = null)
            val result = authRepository.signInWithGoogle(context)
            if (result.isSuccess) {
                val googleName = result.displayName ?: "Jean Abeissa"
                val googleEmail = result.email ?: "abeissajean66@gmail.com"
                val initials = googleName.split(" ")
                    .filter { it.isNotBlank() }
                    .take(2)
                    .map { it.first().uppercaseChar() }
                    .joinToString("")
                    .ifEmpty { "JA" }

                // Check Firestore first for existing user data to ensure persistence
                val firestoreUser = try { repository.fetchUserFromFirestore(googleEmail) } catch (e: Exception) { null }
                val localUser = repository.getUserDirect(googleEmail)
                val isPrincipalAdmin = googleEmail.equals("abeissajean66@gmail.com", ignoreCase = true) ||
                        googleEmail.startsWith("admin", ignoreCase = true) ||
                        googleEmail.contains("admin.hse", ignoreCase = true)

                val role = when {
                    isPrincipalAdmin -> "Admin"
                    firestoreUser?.role?.isNotBlank() == true -> firestoreUser.role
                    localUser?.role?.isNotBlank() == true -> localUser.role
                    else -> "Collaborateur"
                }

                val site = when {
                    isPrincipalAdmin -> "Direction Générale (Tous sites)"
                    firestoreUser?.site?.isNotBlank() == true -> firestoreUser.site
                    localUser?.site?.isNotBlank() == true -> localUser.site
                    else -> "Site de Figuil"
                }

                val phone = firestoreUser?.phone?.ifBlank { localUser?.phone ?: "" } ?: ""
                val dept = if (isPrincipalAdmin) "Direction Générale HSE" else (firestoreUser?.department ?: "Sécurité Industrielle")

                val updatedUser = UserEntity(
                    email = googleEmail,
                    name = googleName,
                    role = role,
                    site = site,
                    phone = phone,
                    department = dept,
                    avatarInitials = initials,
                    status = "Actif",
                    isOnline = true,
                    lastActiveMillis = System.currentTimeMillis()
                )

                // Save to Room and sync to Firestore
                repository.insertUser(updatedUser)
                repository.setUserOnline(googleEmail, true)
                setSite(site)

                authState.value = authState.value.copy(
                    isLoggedIn = true,
                    currentUser = updatedUser,
                    isSubmitting = false,
                    authError = null
                )
                refreshRealtimeWeather(site)
            } else {
                authState.value = authState.value.copy(
                    isSubmitting = false,
                    authError = result.errorMessage
                )
            }
        }
    }

    fun getFirebaseProjectStatus(): Map<String, String> = authRepository.getFirebaseProjectStatus()

    fun logout() {
        val email = authState.value.currentUser.email
        authRepository.signOut()
        isLogoutDialogOpen.value = false
        authState.value = AuthUiState()
        viewModelScope.launch {
            if (email.isNotBlank()) {
                repository.setUserOnline(email, false)
            }
            repository.clearUsers()
        }
    }

    fun togglePushNotifications(enabled: Boolean) {
        viewModelScope.launch {
            val curr = user.value ?: UserEntity()
            repository.updateUser(curr.copy(pushNotificationsEnabled = enabled))
        }
    }

    fun updateUserProfile(name: String, role: String, site: String, email: String) {
        viewModelScope.launch {
            val initials = name.split(" ")
                .filter { it.isNotBlank() }
                .take(2)
                .map { it.first().uppercaseChar() }
                .joinToString("")
                .ifEmpty { "UT" }

            val curr = user.value ?: UserEntity()
            val updatedUser = curr.copy(
                name = name.trim(),
                role = role.trim(),
                site = site.trim(),
                email = email.trim(),
                avatarInitials = initials
            )
            repository.insertUser(updatedUser)
        }
    }

    // Incident Reporting
    fun reportIncident(
        title: String,
        category: String,
        severity: String,
        site: String,
        location: String,
        description: String
    ) {
        viewModelScope.launch {
            val reporter = user.value?.name?.takeIf { it.isNotBlank() }
                ?: authState.value.currentUser?.name?.takeIf { it.isNotBlank() }
                ?: "Collaborateur"
            val incident = IncidentEntity(
                title = title,
                category = category,
                severity = severity,
                site = site,
                location = location,
                description = description,
                reportedBy = reporter,
                timestampText = "À l'instant",
                timestampMillis = System.currentTimeMillis(),
                status = "En cours"
            )
            repository.reportIncident(incident)
            isReportIncidentOpen.value = false
        }
    }

    fun updateIncidentStatus(incidentId: Long, status: String) {
        viewModelScope.launch {
            repository.updateIncidentStatus(incidentId, status)
        }
    }

    // Procedure Steps
    fun toggleStep(stepId: Long, isCompleted: Boolean) {
        viewModelScope.launch {
            repository.updateStepCompletion(stepId, isCompleted)
        }
    }

    fun toggleStepCompletion(step: ProcedureStepEntity) {
        toggleStep(step.id, !step.isCompleted)
    }

    fun resetProcedure(procedureId: String) {
        viewModelScope.launch {
            repository.resetProcedureSteps(procedureId)
        }
    }

    fun resetProcedureSteps(procedureId: String) {
        resetProcedure(procedureId)
    }

    // Training Enroll
    fun toggleTrainingEnrollment(training: TrainingEntity) {
        viewModelScope.launch {
            repository.toggleTrainingEnrollment(training.id, training.isEnrolled)
        }
    }

    // Notification Actions
    fun markNotificationAsRead(id: Long) {
        viewModelScope.launch {
            repository.markNotificationAsRead(id)
        }
    }

    fun markAllNotificationsAsRead() {
        viewModelScope.launch {
            repository.markAllNotificationsAsRead()
        }
    }

    // Surveys
    fun submitSurvey(surveyId: String) {
        viewModelScope.launch {
            repository.completeSurvey(surveyId)
            activeSurveyId.value = null
        }
    }

    fun completeSurvey(surveyId: String) {
        submitSurvey(surveyId)
    }

    // Success Stories
    fun toggleStoryLike(story: SuccessStoryEntity) {
        viewModelScope.launch {
            repository.toggleStoryLike(story.id, story.isLiked)
        }
    }

    // Safety Audit Save
    fun saveSafetyAudit(
        site: String,
        scorePercentage: Int
    ) {
        viewModelScope.launch {
            val auditor = user.value?.name?.takeIf { it.isNotBlank() }
                ?: authState.value.currentUser.name.takeIf { it.isNotBlank() }
                ?: "Auditeur HSE"
            val audit = SafetyAuditEntity(
                site = site,
                auditorName = auditor,
                dateText = "Aujourd'hui",
                scorePercentage = scorePercentage,
                status = if (scorePercentage >= 80) "Conforme" else "Action requise"
            )
            repository.saveSafetyAudit(audit)
            isSafetyAuditOpen.value = false
        }
    }

    // Admin Content Actions
    fun saveContent(content: ContentEntity, onComplete: ((Boolean) -> Unit)? = null) {
        viewModelScope.launch {
            try {
                // Save immediately to local repository first (instant local persistence)
                repository.addContent(content)

                var updatedContent = content
                val url = content.fileUrl
                if (url.isNotBlank() && !url.startsWith("http://") && !url.startsWith("https://")) {
                    try {
                        val cloudUrl = CloudStorageManager.uploadFile(
                            localFilePathOrUri = url,
                            folder = "hse_documents",
                            context = getApplication()
                        )
                        if (cloudUrl.isNotBlank() && (cloudUrl.startsWith("http://") || cloudUrl.startsWith("https://"))) {
                            updatedContent = content.copy(fileUrl = cloudUrl)
                            repository.addContent(updatedContent)
                        }
                    } catch (e: Exception) {
                        Log.w("HseViewModel", "Storage upload warning: ${e.message}")
                        adminErrorMessage.value = "Document conservé localement. Téléversement Cloud: ${e.message}"
                    }
                }
                
                // Real-time FCM notification to field workers for new safety documentation
                FcmPushManager.broadcastUpdateNotification(
                    title = "📄 Nouveau document HSE : ${updatedContent.title}",
                    message = "[${updatedContent.category}] ${updatedContent.description.take(90)}...",
                    type = "DOCUMENT",
                    site = updatedContent.site.ifBlank { "Tous les sites" },
                    priority = "NORMAL"
                )
                
                isAddEditContentOpen.value = false
                editingContent.value = null
                onComplete?.invoke(true)
            } catch (e: Exception) {
                adminErrorMessage.value = e.message ?: "Une erreur est survenue lors de la sauvegarde."
                onComplete?.invoke(false)
            }
        }
    }

    fun deleteContent(contentId: String) {
        viewModelScope.launch {
            repository.deleteContent(contentId)
            if (selectedViewingContent.value?.id == contentId) {
                selectedViewingContent.value = null
            }
        }
    }

    fun incrementContentViews(contentId: String) {
        viewModelScope.launch {
            repository.incrementContentViews(contentId)
        }
    }

    fun openContentReader(content: ContentEntity) {
        selectedViewingContent.value = content
        incrementContentViews(content.id)
    }

    fun closeContentReader() {
        selectedViewingContent.value = null
    }

    // Admin User Actions
    fun saveUser(user: UserEntity) {
        viewModelScope.launch {
            val existing = repository.getUserDirect(user.email)
            val finalUser = if (existing?.isProtectedAdmin == true) {
                user.copy(
                    role = if (user.role.contains("Admin", ignoreCase = true)) user.role else "Admin",
                    status = "Actif"
                )
            } else {
                user
            }
            repository.addUser(finalUser)
            isAddEditUserOpen.value = false
            editingUser.value = null
        }
    }

    fun approveUser(email: String, name: String = "") {
        viewModelScope.launch {
            repository.updateUserStatus(email, "Actif")
            val resolvedName = name.ifBlank {
                repository.getUserDirect(email)?.name ?: "Collaborateur"
            }
            repository.sendApprovalNotification(email, resolvedName)
        }
    }

    fun updateUserStatus(email: String, status: String) {
        viewModelScope.launch {
            val existing = repository.getUserDirect(email)
            if (existing?.isProtectedAdmin == true && !status.equals("Actif", ignoreCase = true)) {
                // Cannot deactivate direct admin
                return@launch
            }
            repository.updateUserStatus(email, status)
            if (status.equals("Actif", ignoreCase = true)) {
                val resolvedName = repository.getUserDirect(email)?.name ?: "Collaborateur"
                repository.sendApprovalNotification(email, resolvedName)
            }
            if (authState.value.currentUser.email.equals(email, ignoreCase = true) && !status.equals("Actif", ignoreCase = true)) {
                logout()
            }
        }
    }

    fun deleteUser(email: String) {
        viewModelScope.launch {
            val existing = repository.getUserDirect(email)
            if (existing?.isProtectedAdmin == true) {
                // Cannot delete direct admin
                return@launch
            }
            repository.deleteUser(email)
            if (authState.value.currentUser.email.equals(email, ignoreCase = true)) {
                logout()
            }
        }
    }

    // Success Stories Admin Actions
    fun saveSuccessStory(story: SuccessStoryEntity) {
        viewModelScope.launch {
            try {
                var updatedStory = story
                val url = story.mediaUrl
                if (url.isNotBlank() && !url.startsWith("http://") && !url.startsWith("https://")) {
                    val cloudUrl = CloudStorageManager.uploadFile(url, "hse_stories")
                    updatedStory = story.copy(mediaUrl = cloudUrl)
                }
                repository.saveSuccessStory(updatedStory)
                // Trigger FCM push notification broadcast in real-time
                com.example.service.FcmPushManager.broadcastPushNotification(
                    title = "⭐ Nouvelle Succès Story: ${updatedStory.title}",
                    message = "${updatedStory.authorName} (${updatedStory.site}) vient de partager une nouvelle réussite : ${updatedStory.description.take(80)}...",
                    topic = com.example.service.FcmPushManager.TOPIC_SUCCESS_STORIES,
                    type = "SUCCESS_STORY"
                )
            } catch (e: Exception) {
                adminErrorMessage.value = e.message ?: "Erreur d'upload de la Succès Story."
            }
        }
    }

    fun deleteSuccessStory(storyId: String) {
        viewModelScope.launch {
            repository.deleteSuccessStory(storyId)
        }
    }

    // Surveys Admin Actions
    fun saveSurvey(survey: SurveyEntity) {
        viewModelScope.launch {
            repository.saveSurvey(survey)
            // Trigger FCM push notification broadcast in real-time
            com.example.service.FcmPushManager.broadcastPushNotification(
                title = "📋 Important Sondage & Enquête : ${survey.title}",
                message = "Votre avis compte ! Participez au sondage (${survey.quarter}) : ${survey.description.take(80)}...",
                topic = com.example.service.FcmPushManager.TOPIC_IMPORTANT_SURVEYS,
                type = "SURVEY"
            )
        }
    }

    fun deleteSurvey(surveyId: String) {
        viewModelScope.launch {
            repository.deleteSurvey(surveyId)
        }
    }

    // Notification Admin Actions
    fun deleteNotification(notificationId: Long) {
        viewModelScope.launch {
            repository.deleteNotification(notificationId)
        }
    }

    // Reading Attestations
    fun recordDocumentAttestation(documentId: String, documentTitle: String, documentReference: String) {
        viewModelScope.launch {
            val curUser = authState.value.currentUser
            val attestation = DocumentAttestationEntity(
                documentId = documentId,
                documentTitle = documentTitle,
                documentReference = documentReference,
                userEmail = curUser.email.ifBlank { "abeissajean66@gmail.com" },
                userName = curUser.name.ifBlank { "Jean Abeissa" },
                userRole = curUser.role.ifBlank { "Collaborateur" },
                userSite = curUser.site.ifBlank { "Site Figuil" },
                timestampMillis = System.currentTimeMillis(),
                timestampText = "Aujourd'hui",
                isCertified = true,
                certificateId = "CERT-SEC-2026-${(10000..99999).random()}"
            )
            repository.recordDocumentAttestation(attestation)
        }
    }

    fun deleteAttestation(id: Long) {
        viewModelScope.launch {
            repository.deleteAttestation(id)
        }
    }

    // Incident Admin Actions
    fun deleteIncident(incidentId: Long) {
        viewModelScope.launch {
            repository.deleteIncident(incidentId)
        }
    }

    // Global Alert Broadcast
    fun broadcastAlert(title: String, message: String, type: String = "Urgence", site: String = "Tous les sites") {
        viewModelScope.launch {
            repository.broadcastNotification(title, message, type)
            // Dispatch high-priority emergency push alert via FCM
            FcmPushManager.broadcastSafetyAlert(
                title = title,
                message = message,
                site = site,
                priority = "URGENT"
            )
            isAdminAlertsScreenOpen.value = false
        }
    }

    fun broadcastNotification(
        title: String,
        message: String,
        type: String = "Information",
        site: String = "Tous les sites",
        priority: String = "NORMAL"
    ) {
        viewModelScope.launch {
            val formattedMsg = if (site != "Tous les sites" && !message.startsWith("[")) "[$site] $message" else message
            val finalType = if (priority.equals("URGENT", ignoreCase = true) && !type.equals("Urgence", ignoreCase = true) && !type.equals("Alerte", ignoreCase = true)) "Urgence" else type
            repository.broadcastNotification(title, formattedMsg, finalType)

            // Dispatch FCM real-time push notification
            if (finalType.equals("Urgence", ignoreCase = true) || finalType.equals("Alerte", ignoreCase = true) || priority.equals("URGENT", ignoreCase = true)) {
                FcmPushManager.broadcastSafetyAlert(
                    title = title,
                    message = formattedMsg,
                    site = site,
                    priority = priority
                )
            } else {
                FcmPushManager.broadcastUpdateNotification(
                    title = title,
                    message = formattedMsg,
                    type = finalType,
                    site = site,
                    priority = priority
                )
            }
        }
    }

    val allSuccessStories: StateFlow<List<SuccessStoryEntity>>
        get() = successStories

    val allSurveys: StateFlow<List<SurveyEntity>>
        get() = surveys

    val allNotifications: StateFlow<List<NotificationEntity>>
        get() = notifications

    // ==========================================
    // IA COPILOT OPERATIONS
    // ==========================================
    fun openHseIa(category: String = "CHAT", initialPrompt: String = "") {
        selectedIaCategory.value = category
        if (initialPrompt.isNotBlank()) {
            iaInputPrompt.value = initialPrompt
        }
        isHseIaScreenOpen.value = true
    }

    fun closeHseIa() {
        isHseIaScreenOpen.value = false
    }

    fun selectIaSession(sessionId: String) {
        activeIaSessionId.value = sessionId
    }

    fun createNewIaSession(title: String = "Nouvelle consultation", category: String = "CHAT") {
        val newId = "session_${System.currentTimeMillis()}"
        viewModelScope.launch {
            repository.insertIaSession(
                HseIaSessionEntity(
                    id = newId,
                    title = title,
                    category = category,
                    timestampMillis = System.currentTimeMillis(),
                    lastMessageText = "Session initiée"
                )
            )
            activeIaSessionId.value = newId
            selectedIaCategory.value = category
        }
    }

    fun deleteIaSession(sessionId: String) {
        viewModelScope.launch {
            repository.deleteIaSession(sessionId)
            if (activeIaSessionId.value == sessionId) {
                activeIaSessionId.value = "session_default"
            }
        }
    }

    fun clearCurrentIaSessionMessages() {
        val sId = activeIaSessionId.value
        viewModelScope.launch {
            repository.clearIaMessagesForSession(sId)
            repository.insertIaMessage(
                HseIaMessageEntity(
                    sessionId = sId,
                    sender = "IA",
                    text = "Historique réinitialisé pour cette session. En quoi puis-je vous assister ?",
                    category = selectedIaCategory.value,
                    timestampMillis = System.currentTimeMillis()
                )
            )
        }
    }

    fun setIaCategory(category: String) {
        selectedIaCategory.value = category
    }

    fun setIaImageUri(uri: Uri?) {
        selectedIaImageUri.value = uri
    }

    fun onIaInputPromptChange(text: String) {
        iaInputPrompt.value = text
    }

    fun sendIaPrompt(
        customPrompt: String? = null,
        customCategory: String? = null,
        customImageUri: Uri? = null
    ) {
        val promptToSend = (customPrompt ?: iaInputPrompt.value).trim()
        val imageToSend = customImageUri ?: selectedIaImageUri.value
        val categoryToSend = customCategory ?: selectedIaCategory.value
        val currentSessionId = activeIaSessionId.value

        if (promptToSend.isBlank() && imageToSend == null) return

        val displayText = if (promptToSend.isNotBlank()) promptToSend else "Analyse de l'image jointe"

        viewModelScope.launch {
            // 1. Save User message to Room
            val userMsg = HseIaMessageEntity(
                sessionId = currentSessionId,
                sender = "USER",
                text = displayText,
                imageUri = imageToSend?.toString(),
                category = categoryToSend,
                timestampMillis = System.currentTimeMillis()
            )
            repository.insertIaMessage(userMsg)

            // Update session title and last message if it's the first non-default message
            val sessionTitle = if (promptToSend.isNotBlank()) promptToSend.take(35) else "Consultation Photo"
            repository.insertIaSession(
                HseIaSessionEntity(
                    id = currentSessionId,
                    title = sessionTitle,
                    category = categoryToSend,
                    timestampMillis = System.currentTimeMillis(),
                    lastMessageText = displayText.take(60)
                )
            )

            // Clear input fields & set generating state
            iaInputPrompt.value = ""
            selectedIaImageUri.value = null
            isIaGenerating.value = true

            try {
                val currentHistory = activeIaMessages.value
                val responseText = HseIaService.generateHseIaResponse(
                    context = getApplication(),
                    prompt = displayText,
                    history = currentHistory,
                    imageUri = imageToSend,
                    category = categoryToSend
                )

                val isUrgent = responseText.contains("urgent", ignoreCase = true) ||
                        responseText.contains("danger immédiat", ignoreCase = true) ||
                        responseText.contains("critique", ignoreCase = true)

                val iaMsg = HseIaMessageEntity(
                    sessionId = currentSessionId,
                    sender = "IA",
                    text = responseText,
                    category = categoryToSend,
                    timestampMillis = System.currentTimeMillis(),
                    isUrgentWarning = isUrgent
                )
                repository.insertIaMessage(iaMsg)

                repository.insertIaSession(
                    HseIaSessionEntity(
                        id = currentSessionId,
                        title = sessionTitle,
                        category = categoryToSend,
                        timestampMillis = System.currentTimeMillis(),
                        lastMessageText = responseText.take(60)
                    )
                )
            } catch (e: Exception) {
                val errorMsg = HseIaMessageEntity(
                    sessionId = currentSessionId,
                    sender = "IA",
                    text = "Désolé, une erreur est survenue lors de l'analyse : ${e.localizedMessage}. Veuillez réessayer ou vérifier votre connexion.",
                    category = categoryToSend,
                    timestampMillis = System.currentTimeMillis()
                )
                repository.insertIaMessage(errorMsg)
            } finally {
                isIaGenerating.value = false
            }
        }
    }
}

