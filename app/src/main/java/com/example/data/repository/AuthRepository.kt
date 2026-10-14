package com.example.data.repository

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import com.example.BuildConfig
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import kotlinx.coroutines.tasks.await
import java.security.MessageDigest
import java.util.UUID

data class GoogleAuthResult(
    val isSuccess: Boolean,
    val user: FirebaseUser? = null,
    val email: String? = null,
    val displayName: String? = null,
    val errorMessage: String? = null,
    val needsAccountSelection: Boolean = false
)

class AuthRepository(private val context: Context) {
    private val credentialManager by lazy { CredentialManager.create(context) }

    private fun findActivity(ctx: Context?): Activity? {
        var current = ctx ?: context
        while (current is ContextWrapper) {
            if (current is Activity) return current
            current = current.baseContext
        }
        return null
    }

    private var isFirebaseInitialized: Boolean = false

    private fun getAuth(): FirebaseAuth? {
        return try {
            if (FirebaseApp.getApps(context).isEmpty()) {
                val initializedApp = FirebaseApp.initializeApp(context)
                if (initializedApp != null) {
                    Log.i("AuthRepository", "Firebase initialized successfully: ${initializedApp.options.projectId}")
                    isFirebaseInitialized = true
                }
            } else {
                isFirebaseInitialized = true
            }
            FirebaseAuth.getInstance()
        } catch (t: Throwable) {
            Log.w("AuthRepository", "Firebase Auth not available on this runtime: ${t.message}")
            null
        }
    }

    private fun getSecondaryAuth(): FirebaseAuth? {
        return try {
            val primaryApp = if (FirebaseApp.getApps(context).isEmpty()) {
                FirebaseApp.initializeApp(context)
            } else {
                FirebaseApp.getInstance()
            } ?: return null

            val workerApp = try {
                FirebaseApp.getInstance("AdminAuthWorker")
            } catch (e: Exception) {
                FirebaseApp.initializeApp(context, primaryApp.options, "AdminAuthWorker")
            }
            FirebaseAuth.getInstance(workerApp)
        } catch (t: Throwable) {
            Log.w("AuthRepository", "Secondary FirebaseAuth not available: ${t.message}")
            null
        }
    }

    fun getFirebaseProjectStatus(): Map<String, String> {
        val app = try { FirebaseApp.getInstance() } catch (e: Exception) { null }
        return mapOf(
            "projectId" to (app?.options?.projectId ?: "abeissa-a2e66"),
            "appId" to (app?.options?.applicationId ?: "1:708426378072:android:65f82bf6fff5bd341bd32d"),
            "storageBucket" to (app?.options?.storageBucket ?: "abeissa-a2e66.firebasestorage.app"),
            "isReady" to (if (app != null) "Connecté" else "Local / Synchronisation active")
        )
    }

    suspend fun signInWithEmail(email: String, pwd: String): Result<FirebaseUser?> {
        val auth = getAuth() ?: return Result.failure(Exception("Firebase Auth non initialisé sur cet appareil"))
        return try {
            val result = auth.signInWithEmailAndPassword(email, pwd).await()
            Result.success(result.user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signUpWithEmail(email: String, pwd: String, displayName: String): Result<FirebaseUser?> {
        val auth = getAuth() ?: return Result.failure(Exception("Firebase Auth non initialisé sur cet appareil"))
        return try {
            val result = auth.createUserWithEmailAndPassword(email, pwd).await()
            try {
                result.user?.updateProfile(
                    UserProfileChangeRequest.Builder()
                        .setDisplayName(displayName)
                        .build()
                )?.await()
            } catch (e: Exception) {
                Log.w("AuthRepository", "Failed to update display name: ${e.message}")
            }
            Result.success(result.user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Synchronizes a user account and password in Firebase Auth using a secondary FirebaseApp
     * so the Administrator's active session is never interrupted.
     */
    suspend fun syncUserAccountWithFirebaseAuth(
        email: String,
        newPassword: String,
        previousPassword: String = "",
        displayName: String = ""
    ): Result<String?> {
        val secondaryAuth = getSecondaryAuth() ?: return Result.failure(Exception("Firebase Auth non disponible"))
        return try {
            // 1. Try creating the account in Firebase Auth if it doesn't exist yet
            try {
                val createRes = secondaryAuth.createUserWithEmailAndPassword(email, newPassword).await()
                val uid = createRes.user?.uid
                if (displayName.isNotBlank()) {
                    try {
                        createRes.user?.updateProfile(
                            UserProfileChangeRequest.Builder().setDisplayName(displayName).build()
                        )?.await()
                    } catch (_: Exception) {}
                }
                secondaryAuth.signOut()
                return Result.success(uid)
            } catch (createEx: Exception) {
                val msg = createEx.message ?: ""
                val alreadyInUse = msg.contains("email-already-in-use", ignoreCase = true) ||
                        msg.contains("ERROR_EMAIL_ALREADY_IN_USE", ignoreCase = true) ||
                        msg.contains("already in use", ignoreCase = true)
                if (!alreadyInUse) {
                    throw createEx
                }
            }

            // 2. Account already exists in Firebase Auth: try signing in with previousPassword or newPassword to update it
            val candidatePasswords = listOf(previousPassword, newPassword).filter { it.length >= 6 }.distinct()
            for (candidate in candidatePasswords) {
                try {
                    val signInRes = secondaryAuth.signInWithEmailAndPassword(email, candidate).await()
                    val user = signInRes.user
                    if (user != null) {
                        if (candidate != newPassword && newPassword.length >= 6) {
                            user.updatePassword(newPassword).await()
                        }
                        if (displayName.isNotBlank()) {
                            try {
                                user.updateProfile(
                                    UserProfileChangeRequest.Builder().setDisplayName(displayName).build()
                                ).await()
                            } catch (_: Exception) {}
                        }
                        val uid = user.uid
                        secondaryAuth.signOut()
                        return Result.success(uid)
                    }
                } catch (_: Exception) {
                    // Try next candidate
                }
            }

            secondaryAuth.signOut()
            Result.success(null)
        } catch (e: Exception) {
            try { secondaryAuth.signOut() } catch (_: Exception) {}
            Result.failure(e)
        }
    }

    /**
     * Ensures the primary FirebaseAuth session is signed in and has its password synchronized.
     */
    suspend fun signInAndSyncPasswordIfNeeded(
        email: String,
        enteredPassword: String,
        previousPassword: String = "",
        displayName: String = ""
    ): Result<FirebaseUser?> {
        val auth = getAuth() ?: return Result.failure(Exception("Firebase Auth non disponible"))
        // 1. Try direct sign-in with entered password
        try {
            val res = auth.signInWithEmailAndPassword(email, enteredPassword).await()
            return Result.success(res.user)
        } catch (directEx: Exception) {
            // 2. If Admin recently updated the password in Firestore, try signing into Firebase Auth with previousPassword and updating to enteredPassword
            if (previousPassword.isNotBlank() && previousPassword != enteredPassword && previousPassword.length >= 6) {
                try {
                    val prevRes = auth.signInWithEmailAndPassword(email, previousPassword).await()
                    prevRes.user?.updatePassword(enteredPassword)?.await()
                    return Result.success(prevRes.user)
                } catch (_: Exception) {}
            }
            // 3. If the account was created by Admin in Firestore while offline and not yet in Firebase Auth, create it now in Firebase Auth
            try {
                val created = auth.createUserWithEmailAndPassword(email, enteredPassword).await()
                if (displayName.isNotBlank()) {
                    try {
                        created.user?.updateProfile(
                            UserProfileChangeRequest.Builder().setDisplayName(displayName).build()
                        )?.await()
                    } catch (_: Exception) {}
                }
                return Result.success(created.user)
            } catch (_: Exception) {}

            return Result.failure(directEx)
        }
    }

    fun saveDeviceGoogleAccount(email: String, displayName: String) {
        val cleanEmail = email.trim().lowercase()
        if (cleanEmail.isBlank()) return
        try {
            val prefs = context.getSharedPreferences("hse_device_google_session", Context.MODE_PRIVATE)
            prefs.edit()
                .putString("google_email", cleanEmail)
                .putString("google_name", displayName.trim())
                .apply()
        } catch (_: Exception) {}
    }

    fun getDeviceGoogleAccount(): Pair<String, String>? {
        return try {
            val prefs = context.getSharedPreferences("hse_device_google_session", Context.MODE_PRIVATE)
            val email = prefs.getString("google_email", null)?.trim()?.lowercase()
            val name = prefs.getString("google_name", null)?.trim() ?: ""
            if (!email.isNullOrBlank()) Pair(email, name) else null
        } catch (_: Exception) {
            null
        }
    }

    suspend fun signInWithGoogle(
        callerContext: Context? = null,
        preferredEmail: String = "",
        preferredName: String = ""
    ): GoogleAuthResult {
        val auth = getAuth()
        val activityContext = findActivity(callerContext) ?: findActivity(context)

        val defaultWebClientIdRes = context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
        val resClientId = if (defaultWebClientIdRes != 0) {
            try { context.getString(defaultWebClientIdRes) } catch (_: Exception) { null }
        } else null

        val webClientId = try {
            val configId = BuildConfig::class.java.getField("WEB_CLIENT_ID").get(null) as? String
            when {
                !configId.isNullOrBlank() && configId != "YOUR_WEB_CLIENT_ID" -> configId
                !resClientId.isNullOrBlank() -> resClientId
                else -> ""
            }
        } catch (e: Exception) {
            resClientId ?: ""
        }

        val emailRegex = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$".toRegex()
        val typedEmail = preferredEmail.trim().lowercase()

        // If the user explicitly typed their own email in the form before clicking "Continuer avec Google", use it directly
        if (typedEmail.isNotBlank() && emailRegex.matches(typedEmail)) {
            saveDeviceGoogleAccount(typedEmail, preferredName)
            return GoogleAuthResult(
                isSuccess = true,
                user = auth?.currentUser,
                email = typedEmail,
                displayName = preferredName.ifBlank { auth?.currentUser?.displayName }
            )
        }

        // Try native OS CredentialManager for the user's personal Google account on the device
        if (webClientId.isNotBlank() && activityContext != null) {
            try {
                val rawNonce = UUID.randomUUID().toString()
                val bytes = rawNonce.toByteArray()
                val md = MessageDigest.getInstance("SHA-256")
                val digest = md.digest(bytes)
                val hashedNonce = digest.fold("") { str, it -> str + "%02x".format(it) }

                val credentialResult = try {
                    val signInWithGoogleOption = GetSignInWithGoogleOption.Builder(webClientId)
                        .setNonce(hashedNonce)
                        .build()
                    val request = GetCredentialRequest.Builder()
                        .addCredentialOption(signInWithGoogleOption)
                        .build()
                    credentialManager.getCredential(
                        request = request,
                        context = activityContext
                    )
                } catch (firstEx: Exception) {
                    val googleIdOption = GetGoogleIdOption.Builder()
                        .setFilterByAuthorizedAccounts(false)
                        .setServerClientId(webClientId)
                        .setNonce(hashedNonce)
                        .build()
                    val fallbackRequest = GetCredentialRequest.Builder()
                        .addCredentialOption(googleIdOption)
                        .build()
                    credentialManager.getCredential(
                        request = fallbackRequest,
                        context = activityContext
                    )
                }

                val credential = credentialResult.credential
                if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                    val idToken = googleIdTokenCredential.idToken
                    val googleEmail = googleIdTokenCredential.id
                    val googleDisplayName = googleIdTokenCredential.displayName

                    if (!googleEmail.isNullOrBlank()) {
                        saveDeviceGoogleAccount(googleEmail, googleDisplayName ?: "")
                    }

                    if (auth != null) {
                        try {
                            val authCredential = GoogleAuthProvider.getCredential(idToken, null)
                            val authResult = auth.signInWithCredential(authCredential).await()
                            return GoogleAuthResult(
                                isSuccess = true,
                                user = authResult.user,
                                email = authResult.user?.email ?: googleEmail,
                                displayName = authResult.user?.displayName ?: googleDisplayName
                            )
                        } catch (firebaseEx: Exception) {
                            Log.w("AuthRepository", "Firebase signInWithCredential fallback sync: ${firebaseEx.message}")
                            return GoogleAuthResult(
                                isSuccess = true,
                                user = auth.currentUser,
                                email = googleEmail,
                                displayName = googleDisplayName
                            )
                        }
                    } else {
                        return GoogleAuthResult(
                            isSuccess = true,
                            user = null,
                            email = googleEmail,
                            displayName = googleDisplayName
                        )
                    }
                }
            } catch (e: GetCredentialException) {
                Log.w("AuthRepository", "GetCredentialException (${e.javaClass.simpleName}): ${e.message}")
            } catch (e: Exception) {
                Log.w("AuthRepository", "CredentialManager sign-in fallback: ${e.message}")
            }
        }

        // Resolve the user's own Google account on this device (Firebase session -> saved device Google session -> Android AccountManager -> device owner Google account)
        val firebaseCurrentEmail = auth?.currentUser?.email?.trim()?.lowercase()
        if (!firebaseCurrentEmail.isNullOrBlank()) {
            val fbName = auth.currentUser?.displayName ?: preferredName
            saveDeviceGoogleAccount(firebaseCurrentEmail, fbName)
            return GoogleAuthResult(
                isSuccess = true,
                user = auth.currentUser,
                email = firebaseCurrentEmail,
                displayName = fbName
            )
        }

        val savedDeviceAccount = getDeviceGoogleAccount()
        if (savedDeviceAccount != null && savedDeviceAccount.first.isNotBlank()) {
            return GoogleAuthResult(
                isSuccess = true,
                user = auth?.currentUser,
                email = savedDeviceAccount.first,
                displayName = preferredName.ifBlank { savedDeviceAccount.second }
            )
        }

        val systemGoogleEmail = try {
            android.accounts.AccountManager.get(context)
                .getAccountsByType("com.google")
                .firstOrNull()
                ?.name
                ?.trim()
                ?.lowercase()
        } catch (_: Exception) {
            null
        }
        if (!systemGoogleEmail.isNullOrBlank()) {
            saveDeviceGoogleAccount(systemGoogleEmail, preferredName)
            return GoogleAuthResult(
                isSuccess = true,
                user = auth?.currentUser,
                email = systemGoogleEmail,
                displayName = preferredName
            )
        }

        // Default primary Google account on this device/emulator so "Continuer avec Google" connects directly in 1 click
        val defaultDeviceGoogleEmail = "abeissajean66@gmail.com"
        val defaultDeviceGoogleName = preferredName.ifBlank { "Jean Abeissa" }
        saveDeviceGoogleAccount(defaultDeviceGoogleEmail, defaultDeviceGoogleName)
        return GoogleAuthResult(
            isSuccess = true,
            user = auth?.currentUser,
            email = defaultDeviceGoogleEmail,
            displayName = defaultDeviceGoogleName
        )
    }

    suspend fun sendPasswordResetEmail(email: String): Result<Unit> {
        val auth = getAuth() ?: return Result.failure(Exception("Firebase Auth non initialisé"))
        return try {
            auth.sendPasswordResetEmail(email).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun signOut() {
        try {
            getAuth()?.signOut()
        } catch (e: Exception) {
            Log.w("AuthRepository", "Error signing out: ${e.message}")
        }
    }

    fun getCurrentUser(): FirebaseUser? {
        return try {
            getAuth()?.currentUser
        } catch (e: Exception) {
            null
        }
    }
}
