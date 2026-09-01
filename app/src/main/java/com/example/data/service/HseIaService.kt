package com.example.data.service

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.util.Log
import com.example.BuildConfig
import com.example.data.entity.HseIaMessageEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.util.concurrent.TimeUnit

object HseIaService {
    private const val TAG = "HseIaService"
    private const val PRIMARY_MODEL = "gemini-3.5-flash"
    private const val FALLBACK_MODEL = "gemini-2.5-flash"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models"

    val SYSTEM_PROMPT = """
Tu es un assistant d'intelligence artificielle avancé, polyvalent et utile.
Tu es capable de répondre à toutes sortes de questions, de mener des conversations, de rédiger des textes, de coder, et d'assister l'utilisateur dans n'importe quel domaine (comme ChatGPT, Claude ou Gemini).

COMPORTEMENT
- Réponds de manière précise, utile et détaillée.
- Adapte ton ton et ton niveau de technicité à la demande de l'utilisateur.
- Tu peux aborder n'importe quel sujet : sciences, histoire, programmation, création littéraire, conseils pratiques, etc.
- Sois poli et serviable en toutes circonstances.

FORMAT DES RÉPONSES
- Utilise le format Markdown pour structurer tes réponses (titres, listes, code blocks, etc.).
- Rends l'information claire et facile à lire.
""".trimIndent()

    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()
    }

    suspend fun generateHseIaResponse(
        context: Context,
        prompt: String,
        history: List<HseIaMessageEntity> = emptyList(),
        imageUri: Uri? = null,
        category: String = "CHAT"
    ): String = withContext(Dispatchers.IO) {
        val apiKey = try {
            BuildConfig.GEMINI_API_KEY.takeIf { it.isNotBlank() && it != "MY_GEMINI_API_KEY" } ?: ""
        } catch (e: Throwable) {
            ""
        }

        if (apiKey.isBlank()) {
            Log.w(TAG, "Gemini API key is not configured, running offline logic")
            return@withContext generateExpertOfflineResponse(prompt, category, imageUri != null)
        }

        val modelsToTry = listOf(PRIMARY_MODEL, FALLBACK_MODEL)
        for (model in modelsToTry) {
            try {
                val url = "$BASE_URL/$model:generateContent?key=$apiKey"
                val requestJson = JSONObject()

                // System instruction
                val systemInstructionObj = JSONObject()
                val sysParts = JSONArray()
                sysParts.put(JSONObject().put("text", SYSTEM_PROMPT))
                systemInstructionObj.put("parts", sysParts)
                requestJson.put("systemInstruction", systemInstructionObj)

                // Contents array
                val contentsArray = JSONArray()

                // Add previous conversation turns (limit to last 6 turns for context management)
                val recentHistory = history.takeLast(6)
                for (msg in recentHistory) {
                    val turnObj = JSONObject()
                    val role = if (msg.sender == "USER") "user" else "model"
                    turnObj.put("role", role)
                    val parts = JSONArray()
                    parts.put(JSONObject().put("text", msg.text))
                    turnObj.put("parts", parts)
                    contentsArray.put(turnObj)
                }

                // Add current user prompt (and optional image)
                val currentTurn = JSONObject()
                currentTurn.put("role", "user")
                val currentParts = JSONArray()

                // Process image if attached
                if (imageUri != null) {
                    val base64Image = readImageAsBase64(context, imageUri)
                    if (base64Image != null) {
                        val inlineDataObj = JSONObject()
                        inlineDataObj.put("mimeType", "image/jpeg")
                        inlineDataObj.put("data", base64Image)

                        val imagePart = JSONObject()
                        imagePart.put("inlineData", inlineDataObj)
                        currentParts.put(imagePart)
                    }
                }

                val contextualPrompt = when (category) {
                    "ANALYSE_RISQUE" -> "ANALYSE DE RISQUE & ÉVALUATION DU DANGER :\n$prompt"
                    "CHECKLIST" -> "GÉNÉRATEUR DE CHECK-LIST D'INSPECTION DE TERRAIN :\n$prompt"
                    "PLAN_ACTION" -> "GÉNÉRATEUR DE PLAN D'ACTIONS CORRECTIVES & PRÉVENTIVES :\n$prompt"
                    "RAPPORT_INCIDENT" -> "RÉDACTION DE RAPPORT FORMEL D'INCIDENT / SYNTHÈSE :\n$prompt"
                    "EPI_CONSEIL" -> "CONSEILS & PRÉCONISATIONS ÉQUIPEMENTS DE PROTECTION (EPI / EPC) :\n$prompt"
                    "PHOTO_ANALYSE" -> "ANALYSE APPROFONDIE DE CETTE PHOTO DE TERRAIN :\n$prompt"
                    else -> prompt
                }

                currentParts.put(JSONObject().put("text", contextualPrompt))
                currentTurn.put("parts", currentParts)
                contentsArray.put(currentTurn)

                requestJson.put("contents", contentsArray)

                // Generation config
                val genConfig = JSONObject()
                genConfig.put("temperature", 0.4)
                genConfig.put("topP", 0.95)
                requestJson.put("generationConfig", genConfig)

                val body = requestJson.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
                val request = Request.Builder()
                    .url(url)
                    .post(body)
                    .build()

                val response = httpClient.newCall(request).execute()
                val responseBody = response.body?.string() ?: ""

                if (response.isSuccessful) {
                    val responseJson = JSONObject(responseBody)
                    val candidates = responseJson.optJSONArray("candidates")
                    if (candidates != null && candidates.length() > 0) {
                        val firstCandidate = candidates.getJSONObject(0)
                        val content = firstCandidate.getJSONObject("content")
                        val parts = content.getJSONArray("parts")
                        val textBuilder = StringBuilder()
                        for (i in 0 until parts.length()) {
                            val part = parts.getJSONObject(i)
                            if (part.has("text")) {
                                textBuilder.append(part.getString("text"))
                            }
                        }
                        val resultText = textBuilder.toString().trim()
                        if (resultText.isNotBlank()) {
                            return@withContext resultText
                        }
                    }
                } else {
                    Log.w(TAG, "Gemini API error with model $model (${response.code}): $responseBody")
                }
            } catch (e: Exception) {
                Log.w(TAG, "Exception during Gemini API request for model $model", e)
            }
        }

        generateExpertOfflineResponse(prompt, category, imageUri != null)
    }

    private fun readImageAsBase64(context: Context, uri: Uri): String? {
        return try {
            val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
            val originalBitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()

            if (originalBitmap == null) return null

            // Resize if too big (max 1024px on largest side)
            val maxDimension = 1024
            val width = originalBitmap.width
            val height = originalBitmap.height
            val scale = if (width > maxDimension || height > maxDimension) {
                val scaleWidth = maxDimension.toFloat() / width
                val scaleHeight = maxDimension.toFloat() / height
                minOf(scaleWidth, scaleHeight)
            } else {
                1.0f
            }

            val scaledBitmap = if (scale < 1.0f) {
                Bitmap.createScaledBitmap(
                    originalBitmap,
                    (width * scale).toInt(),
                    (height * scale).toInt(),
                    true
                )
            } else {
                originalBitmap
            }

            val outputStream = ByteArrayOutputStream()
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
            val byteArray = outputStream.toByteArray()
            Base64.encodeToString(byteArray, Base64.NO_WRAP)
        } catch (e: Exception) {
            Log.e(TAG, "Error encoding image to base64: ${e.message}")
            null
        }
    }

    private fun generateExpertOfflineResponse(prompt: String, category: String, hasImage: Boolean): String {
        return """
**Mode hors-ligne actif**

Je suis actuellement en mode hors-ligne car la connexion au serveur a échoué.
Je ne peux pas traiter votre demande complète ("$prompt"), mais je reste à votre disposition dès que la connexion sera rétablie.
        """.trimIndent()
    }
}
