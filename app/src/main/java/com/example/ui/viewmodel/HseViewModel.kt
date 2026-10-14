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
    val isGoogleAccountPickerOpen = MutableStateFlow(false)

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

    // Cloud File Server State & Synchronization
    val b2ConnectionStatus = MutableStateFlow("Connecté au serveur documentaire sécurisé")
    val isB2Syncing = MutableStateFlow(false)
    val b2FilesList = MutableStateFlow<List<com.example.util.B2FileItem>>(emptyList())
    val b2LastSyncMessage = MutableStateFlow<String?>(null)

    fun syncB2Files(onComplete: ((com.example.util.B2SyncResult) -> Unit)? = null) {
        viewModelScope.launch {
            isB2Syncing.value = true
            try {
                val result = com.example.util.B2CloudStorageService.syncB2FilesToDatabase(
                    repository = repository,
                    context = getApplication()
                )
                b2FilesList.value = result.files
                b2LastSyncMessage.value = result.message
                b2ConnectionStatus.value = if (result.isSuccess) {
                    "Connecté • ${result.syncedFilesCount} document(s) synchronisé(s) et accessible(s) à tous"
                } else {
                    "Statut serveur : ${result.message}"
                }
                onComplete?.invoke(result)
            } catch (e: Exception) {
                val msg = e.localizedMessage ?: e.message ?: "Information serveur"
                b2ConnectionStatus.value = "Serveur : $msg"
                b2LastSyncMessage.value = msg
                onComplete?.invoke(com.example.util.B2SyncResult(false, msg))
            } finally {
                isB2Syncing.value = false
            }
        }
    }

    fun testB2Connection(onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val res = com.example.util.B2CloudStorageService.testConnection()
            onResult(res.first, res.second)
        }
    }

    init {
        authRepository = AuthRepository(application)
        val database = AppDatabase.getInstance(application)
        repository = HseRepository(database.hseDao())


        // Check if already signed in via Firebase / Local session
        viewModelScope.launch {
            try {
                val firebaseUser = authRepository.getCurrentUser()
                if (firebaseUser != null) {
                    val userEmail = (firebaseUser.email ?: "").trim().lowercase()
                    if (userEmail.isNotBlank()) {
                        val firestoreUser = repository.fetchUserFromFirestore(userEmail)
                            ?: repository.getUserDirect(userEmail)
                        if (firestoreUser != null && firestoreUser.status != "Suspendu" && firestoreUser.status != "Inactif") {
                            val activeUser = if (firestoreUser.status != "Actif") {
                                firestoreUser.copy(status = "Actif", isOnline = true, authSynced = true)
                            } else {
                                firestoreUser.copy(isOnline = true, authSynced = true)
                            }
                            repository.insertUser(activeUser)
                            repository.setUserOnline(userEmail, true)
                            authState.value = authState.value.copy(
                                isLoggedIn = true,
                                emailInput = userEmail,
                                currentUser = activeUser
                            )
                            if (activeUser.site.isNotBlank()) {
                                setSite(activeUser.site)
                            }
                        } else {
                            // Only registered and active users can stay connected
                            authRepository.signOut()
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
            0
        )

        incidents = repository.allIncidents.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

        pendingRisksCount = repository.pendingRisksCount.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            0
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
            0
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
            // Synchronize cloud files from Backblaze B2 File Server on startup
            syncB2Files()
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
            val adminEmail = "abeissajean66@gmail.com"
            val adminPwd = "bouba jean"
            val syncRes = authRepository.syncUserAccountWithFirebaseAuth(
                email = adminEmail,
                newPassword = adminPwd,
                displayName = "Jean Abeissa (Admin Principal)"
            )
            val adminUser = UserEntity(
                email = adminEmail,
                name = "Jean Abeissa (Admin Principal)",
                role = "Admin",
                site = "Direction Générale (Tous sites)",
                department = "Direction Générale HSE",
                phone = "+237 690 00 00 00",
                avatarInitials = "JA",
                status = "Actif",
                isOnline = true,
                lastActiveMillis = System.currentTimeMillis(),
                password = adminPwd,
                firebaseUid = syncRes.getOrNull() ?: "",
                authSynced = true
            )
            authRepository.saveDeviceGoogleAccount(adminUser.email, adminUser.name)
            repository.insertUser(adminUser)
            repository.setUserOnline(adminUser.email, true)
            setSite("Direction Générale (Tous sites)")
            authState.value = authState.value.copy(
                isLoggedIn = true,
                currentUser = adminUser,
                emailInput = adminEmail,
                passwordInput = adminPwd,
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
            val u = repository.getUserDirect(email) ?: repository.fetchUserFromFirestore(email)
            if (u == null) {
                authState.value = authState.value.copy(
                    authError = "Seuls les utilisateurs ayant créé un compte peuvent se connecter."
                )
                return@launch
            }
            val activeUser = u.copy(status = "Actif", isOnline = true, lastActiveMillis = System.currentTimeMillis())
            repository.insertUser(activeUser)
            repository.setUserOnline(u.email, true)
            setSite(u.site.ifBlank { "Siège Douala" })
            authState.value = authState.value.copy(
                isLoggedIn = true,
                currentUser = activeUser,
                isMfaStep = false,
                isSubmitting = false,
                authError = null
            )
        }
    }

    fun login() {
        val email = authState.value.emailInput.trim().lowercase()
        val pwd = authState.value.passwordInput.trim()

        if (email.isEmpty() || pwd.isEmpty()) {
            authState.value = authState.value.copy(authError = "Veuillez renseigner votre adresse e-mail et votre mot de passe.", authSuccess = null)
            return
        }

        // Direct check for principal admin
        if (email.equals("abeissajean66@gmail.com", ignoreCase = true) &&
            (pwd.equals("bouba jean", ignoreCase = true) || pwd.replace(" ", "").equals("boubajean", ignoreCase = true))
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

        if (AppDatabase.isDemoUserEmail(email)) {
            authState.value = authState.value.copy(
                authError = "Les comptes de démonstration ne sont pas autorisés. Veuillez créer votre compte via 'S'inscrire'.",
                authSuccess = null
            )
            return
        }

        viewModelScope.launch {
            authState.value = authState.value.copy(isSubmitting = true, authError = null, authSuccess = null)

            // 1. Fetch registered user profile from Firestore and local Room database
            val firestoreUser = try { repository.fetchUserFromFirestore(email) } catch (e: Exception) { null }
            val localUser = repository.getUserDirect(email)
            val existingUser = firestoreUser ?: localUser

            // Check if account is blocked/inactive
            if (existingUser != null && (existingUser.status.equals("Bloqué", ignoreCase = true) ||
                        existingUser.status.equals("Suspendu", ignoreCase = true) ||
                        existingUser.status.equals("Inactif", ignoreCase = true))
            ) {
                authState.value = authState.value.copy(
                    isLoggedIn = false,
                    isSubmitting = false,
                    authError = "Ce compte utilisateur a été désactivé ou bloqué par l'administrateur. Veuillez contacter la Direction HSE."
                )
                return@launch
            }

            // 2. If user exists in Firestore/Room and entered the password saved/reset by Admin,
            // ensure Firebase Auth is synchronized with this password first
            if (existingUser != null && existingUser.password.isNotBlank() && existingUser.password == pwd) {
                authRepository.syncUserAccountWithFirebaseAuth(
                    email = email,
                    newPassword = pwd,
                    displayName = existingUser.name,
                    previousPassword = existingUser.password
                )
            }

            // 3. Authenticate with Firebase Auth
            val result = try {
                authRepository.signInWithEmail(email, pwd)
            } catch (t: Throwable) {
                Result.failure(t)
            }

            if (result.isSuccess) {
                val fbUser = result.getOrNull()
                if (existingUser == null) {
                    // Strict rule: Only users who have created a registered account in the app can connect
                    authRepository.signOut()
                    authState.value = authState.value.copy(
                        isLoggedIn = false,
                        isSubmitting = false,
                        authError = "Aucun compte utilisateur enregistré pour $email. Seuls les utilisateurs ayant créé un compte via 'S'inscrire' peuvent se connecter."
                    )
                    return@launch
                }

                val activeUser = existingUser.copy(
                    status = "Actif",
                    isOnline = true,
                    lastActiveMillis = System.currentTimeMillis(),
                    password = pwd,
                    firebaseUid = fbUser?.uid ?: existingUser.firebaseUid,
                    authSynced = true,
                    resetRequested = false,
                    resetRequestNote = ""
                )
                authRepository.saveDeviceGoogleAccount(activeUser.email, activeUser.name)
                repository.insertUser(activeUser)
                repository.setUserOnline(activeUser.email, true)
                if (activeUser.site.isNotBlank()) {
                    setSite(activeUser.site)
                }

                authState.value = authState.value.copy(
                    isLoggedIn = true,
                    currentUser = activeUser,
                    isMfaStep = false,
                    isSubmitting = false,
                    authError = null
                )
            } else {
                // If user has an existing registered account and their password matches the Admin-managed password
                if (existingUser != null && existingUser.password.isNotBlank() && existingUser.password == pwd) {
                    val syncRes = authRepository.syncUserAccountWithFirebaseAuth(
                        email = email,
                        newPassword = pwd,
                        displayName = existingUser.name
                    )
                    val activeUser = existingUser.copy(
                        status = "Actif",
                        isOnline = true,
                        lastActiveMillis = System.currentTimeMillis(),
                        firebaseUid = syncRes.getOrNull() ?: existingUser.firebaseUid,
                        authSynced = syncRes.isSuccess,
                        resetRequested = false,
                        resetRequestNote = ""
                    )
                    authRepository.saveDeviceGoogleAccount(activeUser.email, activeUser.name)
                    repository.insertUser(activeUser)
                    repository.setUserOnline(activeUser.email, true)
                    if (activeUser.site.isNotBlank()) {
                        setSite(activeUser.site)
                    }
                    authState.value = authState.value.copy(
                        isLoggedIn = true,
                        currentUser = activeUser,
                        isMfaStep = false,
                        isSubmitting = false,
                        authError = null
                    )
                } else if (existingUser == null) {
                    authState.value = authState.value.copy(
                        isLoggedIn = false,
                        isSubmitting = false,
                        authError = "Aucun compte trouvé pour $email. Seuls les utilisateurs disposant d'un compte peuvent se connecter. Cliquez sur 'S'inscrire' pour créer votre compte."
                    )
                } else {
                    authState.value = authState.value.copy(
                        isLoggedIn = false,
                        isSubmitting = false,
                        authError = "Mot de passe incorrect pour $email. En cas d'oubli, cliquez sur 'Compte ou mot de passe oublié ?' pour que l'administrateur réinitialise votre accès."
                    )
                }
            }
        }
    }

    fun register(
        name: String,
        site: String,
        phone: String,
        role: String = "Collaborateur HSE",
        department: String = "Opérations & Sécurité"
    ) {
        val trimmedName = name.trim()
        val email = authState.value.emailInput.trim().lowercase()
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
            authState.value = authState.value.copy(authError = "Le mot de passe doit comporter au moins 6 caractères pour la synchronisation Firebase Auth.", authSuccess = null)
            return
        }

        viewModelScope.launch {
            authState.value = authState.value.copy(isSubmitting = true, authError = null, authSuccess = null)

            // Check if account is already registered
            val existingFirestore = try { repository.fetchUserFromFirestore(email) } catch (e: Exception) { null }
            val existingLocal = repository.getUserDirect(email)
            if ((existingFirestore != null || existingLocal != null) && !email.equals("abeissajean66@gmail.com", ignoreCase = true)) {
                authState.value = authState.value.copy(
                    isSubmitting = false,
                    authError = "Un compte existe déjà avec l'adresse $email. Veuillez utiliser 'Se connecter' ou demander une récupération à l'administrateur."
                )
                return@launch
            }

            // 1. Create and synchronize user in Firebase Auth for security
            val signUpResult = authRepository.signUpWithEmail(email, pwd, trimmedName)
            val firebaseUid = if (signUpResult.isSuccess) {
                signUpResult.getOrNull()?.uid ?: ""
            } else {
                // Synchronize with Firebase Auth via secondary worker and sign in
                val syncRes = authRepository.syncUserAccountWithFirebaseAuth(
                    email = email,
                    newPassword = pwd,
                    displayName = trimmedName
                )
                authRepository.signInWithEmail(email, pwd)
                syncRes.getOrNull() ?: authRepository.getCurrentUser()?.uid ?: ""
            }

            val initials = trimmedName.split(" ")
                .filter { it.isNotBlank() }
                .take(2)
                .map { it.first().uppercaseChar() }
                .joinToString("")
                .ifEmpty { "UT" }

            val resolvedSite = if (trimmedSite.isNotBlank()) trimmedSite else "Site de Figuil"
            val effectiveRole = if (email.equals("abeissajean66@gmail.com", ignoreCase = true)) "Admin" else role

            val newUser = UserEntity(
                name = trimmedName,
                email = email,
                site = resolvedSite,
                phone = trimmedPhone,
                role = effectiveRole,
                department = department,
                avatarInitials = initials,
                pushNotificationsEnabled = true,
                status = "Actif", // Immediate direct access upon clicking S'inscrire
                isOnline = true,
                lastActiveMillis = System.currentTimeMillis(),
                password = pwd,
                firebaseUid = firebaseUid,
                authSynced = true,
                resetRequested = false,
                resetRequestNote = ""
            )

            // 2. Save to Room and synchronize to Firestore in real time
            authRepository.saveDeviceGoogleAccount(newUser.email, newUser.name)
            repository.insertUser(newUser)
            repository.setUserOnline(email, true)
            setSite(resolvedSite)

            // 3. Broadcast real-time notification to Admin & team
            repository.broadcastNotification(
                title = "Nouveau compte actif : $trimmedName",
                message = "$trimmedName ($email • $resolvedSite) a créé son compte et est synchronisé sur Firebase Auth.",
                type = "SYSTEM"
            )

            // 4. Grant immediate access into the application
            isRegisterOpen.value = false
            authState.value = authState.value.copy(
                isLoggedIn = true,
                currentUser = newUser,
                emailInput = email,
                passwordInput = pwd,
                isMfaStep = false,
                isSubmitting = false,
                authError = null,
                authSuccess = null
            )
            refreshRealtimeWeather(resolvedSite)
        }
    }

    fun sendPasswordReset(email: String, note: String = "", onResult: (Boolean, String) -> Unit) {
        val trimmedEmail = email.trim().lowercase()
        if (trimmedEmail.isEmpty()) {
            onResult(false, "Veuillez renseigner votre adresse e-mail ou votre identifiant.")
            return
        }
        viewModelScope.launch {
            // 1. Flag account in Firestore & notify Admin so Admin can manage/reset the user's password
            repository.requestAccountOrPasswordRecovery(
                identifier = trimmedEmail,
                note = note.ifBlank { "Oubli de compte ou mot de passe signalé depuis la page de connexion" }
            )
            // 2. If it's a valid email, also send Firebase Auth password reset link
            if (trimmedEmail.contains("@")) {
                val result = authRepository.sendPasswordResetEmail(trimmedEmail)
                if (result.isSuccess) {
                    onResult(
                        true,
                        "Un lien Firebase Auth a été envoyé à $trimmedEmail ET l'administrateur HSE a été alerté en temps réel pour gérer ou réinitialiser votre accès."
                    )
                } else {
                    onResult(
                        true,
                        "Votre demande de récupération pour $trimmedEmail a été transmise en temps réel à l'administrateur HSE qui peut réinitialiser votre mot de passe."
                    )
                }
            } else {
                onResult(
                    true,
                    "Votre demande d'assistance pour '$trimmedEmail' a été envoyée à l'administrateur HSE pour récupération de votre compte."
                )
            }
        }
    }

    fun closeGoogleAccountPicker() {
        isGoogleAccountPickerOpen.value = false
    }

    fun loginWithGoogle(
        context: android.content.Context? = null,
        isRegisterMode: Boolean = false,
        registerNameHint: String = "",
        registerSiteHint: String = "",
        registerPhoneHint: String = ""
    ) {
        viewModelScope.launch {
            authState.value = authState.value.copy(isSubmitting = true, authError = null, authSuccess = null)
            val result = authRepository.signInWithGoogle(
                callerContext = context,
                preferredEmail = authState.value.emailInput,
                preferredName = registerNameHint
            )
            if (result.isSuccess && !result.email.isNullOrBlank()) {
                completeGoogleSignIn(
                    googleEmail = result.email,
                    googleName = result.displayName?.ifBlank { registerNameHint } ?: registerNameHint,
                    selectedSiteInput = registerSiteHint,
                    phoneInput = registerPhoneHint
                )
            } else {
                authState.value = authState.value.copy(
                    isSubmitting = false,
                    authError = result.errorMessage ?: "Impossible de récupérer votre compte Google."
                )
            }
        }
    }

    fun completeGoogleSignIn(
        googleEmail: String,
        googleName: String = "",
        selectedSiteInput: String = "",
        phoneInput: String = ""
    ) {
        val cleanEmail = googleEmail.trim().lowercase()
        val emailPattern = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$".toRegex()
        if (cleanEmail.isBlank() || !emailPattern.matches(cleanEmail)) {
            authState.value = authState.value.copy(
                isSubmitting = false,
                authError = "Veuillez saisir ou sélectionner une adresse e-mail Google valide."
            )
            return
        }

        if (AppDatabase.isDemoUserEmail(cleanEmail)) {
            authState.value = authState.value.copy(
                isSubmitting = false,
                authError = "Les comptes de démonstration ne sont pas autorisés."
            )
            return
        }

        viewModelScope.launch {
            authState.value = authState.value.copy(isSubmitting = true, authError = null, authSuccess = null)

            val isPrincipalAdmin = cleanEmail.equals("abeissajean66@gmail.com", ignoreCase = true)
            if (isPrincipalAdmin) {
                isGoogleAccountPickerOpen.value = false
                loginAsPrincipalAdmin()
                return@launch
            }

            // 1. Check Firestore & Room for an existing registered account
            val firestoreUser = try { repository.fetchUserFromFirestore(cleanEmail) } catch (e: Exception) { null }
            val localUser = repository.getUserDirect(cleanEmail)
            val existingUser = firestoreUser ?: localUser

            if (existingUser != null && (existingUser.status.equals("Bloqué", ignoreCase = true) ||
                        existingUser.status.equals("Suspendu", ignoreCase = true) ||
                        existingUser.status.equals("Inactif", ignoreCase = true))
            ) {
                authRepository.signOut()
                isGoogleAccountPickerOpen.value = false
                authState.value = authState.value.copy(
                    isSubmitting = false,
                    authError = "Ce compte utilisateur a été désactivé ou suspendu par l'administrateur."
                )
                return@launch
            }

            // Derive display name if not provided
            val derivedNameFromEmail = cleanEmail.substringBefore("@")
                .replace(".", " ")
                .replace("_", " ")
                .replace("-", " ")
                .split(" ")
                .filter { it.isNotBlank() }
                .joinToString(" ") { part -> part.replaceFirstChar { it.uppercaseChar() } }
                .ifBlank { "Collaborateur HSE" }

            val resolvedName = when {
                googleName.isNotBlank() -> googleName.trim()
                existingUser?.name?.isNotBlank() == true -> existingUser.name
                else -> derivedNameFromEmail
            }

            val initials = resolvedName.split(" ")
                .filter { it.isNotBlank() }
                .take(2)
                .map { it.first().uppercaseChar() }
                .joinToString("")
                .ifEmpty { "UT" }

            val role = when {
                existingUser?.role?.isNotBlank() == true -> existingUser.role
                else -> "Collaborateur HSE"
            }

            val site = when {
                selectedSiteInput.isNotBlank() -> selectedSiteInput.trim()
                existingUser?.site?.isNotBlank() == true -> existingUser.site
                else -> "Site de Figuil"
            }

            val phone = when {
                phoneInput.isNotBlank() -> phoneInput.trim()
                existingUser?.phone?.isNotBlank() == true -> existingUser.phone
                else -> ""
            }

            val dept = existingUser?.department?.ifBlank { "Opérations & Sécurité" } ?: "Opérations & Sécurité"
            val syncedPassword = existingUser?.password?.takeIf { it.length >= 6 }
                ?: authState.value.passwordInput.trim().takeIf { it.length >= 6 }
                ?: "GoogleHse_${cleanEmail.hashCode().toUInt()}"

            // 2. Ensure account is synchronized on Firebase Auth for security and admin management
            val syncRes = authRepository.syncUserAccountWithFirebaseAuth(
                email = cleanEmail,
                newPassword = syncedPassword,
                previousPassword = existingUser?.password ?: "",
                displayName = resolvedName
            )
            if (authRepository.getCurrentUser() == null) {
                authRepository.signInAndSyncPasswordIfNeeded(
                    email = cleanEmail,
                    enteredPassword = syncedPassword,
                    previousPassword = existingUser?.password ?: "",
                    displayName = resolvedName
                )
            }

            val firebaseUid = authRepository.getCurrentUser()?.uid
                ?: syncRes.getOrNull()
                ?: existingUser?.firebaseUid
                ?: ""

            val updatedUser = (existingUser ?: UserEntity(email = cleanEmail)).copy(
                email = cleanEmail,
                name = resolvedName,
                role = role,
                site = site,
                phone = phone,
                department = dept,
                avatarInitials = existingUser?.avatarInitials?.ifBlank { initials } ?: initials,
                status = "Actif",
                isOnline = true,
                lastActiveMillis = System.currentTimeMillis(),
                password = syncedPassword,
                firebaseUid = firebaseUid,
                authSynced = true,
                resetRequested = false,
                resetRequestNote = ""
            )

            // 3. Save to Room and synchronize to Firestore in real time
            authRepository.saveDeviceGoogleAccount(cleanEmail, resolvedName)
            repository.insertUser(updatedUser)
            repository.setUserOnline(cleanEmail, true)
            setSite(site)

            if (existingUser == null) {
                repository.broadcastNotification(
                    title = "Nouveau compte Google actif : $resolvedName",
                    message = "$resolvedName ($cleanEmail • $site) s'est connecté via Google et est synchronisé sur Firebase Auth.",
                    type = "SYSTEM"
                )
            }

            // 4. Grant immediate access into the application
            isGoogleAccountPickerOpen.value = false
            isRegisterOpen.value = false
            authState.value = authState.value.copy(
                isLoggedIn = true,
                currentUser = updatedUser,
                emailInput = cleanEmail,
                isMfaStep = false,
                isSubmitting = false,
                authError = null,
                authSuccess = null
            )
            refreshRealtimeWeather(site)
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
            val cleanEmail = user.email.trim().lowercase()
            val existing = repository.getUserDirect(cleanEmail)
            val resolvedPassword = user.password.ifBlank { existing?.password ?: "Cimencam2026" }
            // Synchronize account & password with Firebase Auth using secondary worker
            val syncRes = authRepository.syncUserAccountWithFirebaseAuth(
                email = cleanEmail,
                newPassword = resolvedPassword,
                displayName = user.name,
                previousPassword = existing?.password ?: ""
            )
            val syncedUid = syncRes.getOrNull() ?: existing?.firebaseUid ?: user.firebaseUid

            val baseUser = user.copy(
                email = cleanEmail,
                password = resolvedPassword,
                firebaseUid = syncedUid,
                authSynced = true,
                resetRequested = false,
                resetRequestNote = ""
            )

            val finalUser = if (existing?.isProtectedAdmin == true) {
                baseUser.copy(
                    role = if (baseUser.role.contains("Admin", ignoreCase = true)) baseUser.role else "Admin",
                    status = "Actif"
                )
            } else {
                baseUser
            }
            repository.addUser(finalUser)
            isAddEditUserOpen.value = false
            editingUser.value = null
        }
    }

    fun adminResetUserPassword(
        targetUser: UserEntity,
        newPassword: String,
        onResult: (Boolean, String) -> Unit = { _, _ -> }
    ) {
        val cleanPwd = newPassword.trim()
        if (cleanPwd.length < 6) {
            onResult(false, "Le nouveau mot de passe doit comporter au moins 6 caractères pour Firebase Auth.")
            return
        }
        viewModelScope.launch {
            val syncRes = authRepository.syncUserAccountWithFirebaseAuth(
                email = targetUser.email,
                newPassword = cleanPwd,
                displayName = targetUser.name,
                previousPassword = targetUser.password
            )
            val uid = syncRes.getOrNull() ?: targetUser.firebaseUid
            repository.updateUserPasswordAndSync(
                email = targetUser.email,
                newPassword = cleanPwd,
                firebaseUid = uid,
                authSynced = true
            )
            repository.broadcastNotification(
                title = "🔑 Accès & Mot de passe réinitialisé",
                message = "L'administrateur a mis à jour et synchronisé sur Firebase Auth les identifiants du compte ${targetUser.name} (${targetUser.email}).",
                type = "SYSTEM"
            )
            onResult(
                true,
                "Le mot de passe de ${targetUser.name} (${targetUser.email}) a été mis à jour et synchronisé sur Firebase Auth avec succès."
            )
        }
    }

    fun adminSendFirebaseResetEmail(
        email: String,
        onResult: (Boolean, String) -> Unit = { _, _ -> }
    ) {
        val cleanEmail = email.trim().lowercase()
        viewModelScope.launch {
            val res = authRepository.sendPasswordResetEmail(cleanEmail)
            repository.clearAccountRecoveryRequest(cleanEmail)
            if (res.isSuccess) {
                onResult(true, "Un e-mail officiel Firebase Auth de réinitialisation a été envoyé à $cleanEmail.")
            } else {
                onResult(
                    false,
                    "Impossible d'envoyer l'e-mail Firebase Auth (${res.exceptionOrNull()?.localizedMessage ?: "vérifiez que le compte existe"}). Vous pouvez définir directement un nouveau mot de passe ci-dessus."
                )
            }
        }
    }

    fun adminClearRecoveryRequest(email: String) {
        viewModelScope.launch {
            repository.clearAccountRecoveryRequest(email.trim().lowercase())
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

    fun clearAllNotifications() {
        viewModelScope.launch {
            repository.clearAllNotifications()
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

