package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import java.util.UUID

object CloudStorageManager {
    private const val TAG = "CloudStorageManager"
    private const val MAX_IMAGE_DIMENSION = 1920
    private const val JPEG_QUALITY = 82

    suspend fun uploadFile(
        localFilePathOrUri: String, 
        folder: String = "hse_documents",
        context: Context? = null
    ): String = withContext(Dispatchers.IO) {
        if (localFilePathOrUri.isBlank()) {
            return@withContext ""
        }

        // If it's already an online URL (Firebase Storage / HTTP / HTTPS), return it directly
        if (localFilePathOrUri.startsWith("http://") || localFilePathOrUri.startsWith("https://")) {
            return@withContext localFilePathOrUri
        }

        try {
            val appCtx = context ?: try { FirebaseApp.getInstance().applicationContext } catch (e: Exception) { null }

            // Attempt anonymous sign-in if no active Firebase Auth session exists
            val auth = FirebaseAuth.getInstance()
            if (auth.currentUser == null) {
                try {
                    auth.signInAnonymously().await()
                } catch (e: Exception) {
                    Log.d(TAG, "Connexion anonyme optionnelle ignorée: ${e.message}")
                }
            }

            val storage = FirebaseStorage.getInstance()
            val storageRef = storage.reference

            // Resolve file extension & mime type
            val lowerPath = localFilePathOrUri.lowercase()
            val isImage = lowerPath.endsWith(".jpg") || lowerPath.endsWith(".jpeg") || 
                          lowerPath.endsWith(".png") || lowerPath.endsWith(".webp") ||
                          lowerPath.contains("image") || lowerPath.contains("photo")

            val fileExt = when {
                lowerPath.endsWith(".pdf") || lowerPath.contains("pdf") -> "pdf"
                isImage -> "jpg"
                lowerPath.endsWith(".mp4") || lowerPath.contains("video") -> "mp4"
                lowerPath.endsWith(".mkv") -> "mkv"
                lowerPath.endsWith(".mov") -> "mov"
                lowerPath.endsWith(".avi") -> "avi"
                lowerPath.endsWith(".doc") || lowerPath.endsWith(".docx") -> "docx"
                lowerPath.endsWith(".xls") || lowerPath.endsWith(".xlsx") -> "xlsx"
                else -> "bin"
            }

            val mimeType = when (fileExt) {
                "pdf" -> "application/pdf"
                "jpg", "jpeg" -> "image/jpeg"
                "png" -> "image/png"
                "webp" -> "image/webp"
                "mp4" -> "video/mp4"
                "mkv" -> "video/x-matroska"
                "mov" -> "video/quicktime"
                "avi" -> "video/x-msvideo"
                "docx", "doc" -> "application/msword"
                "xlsx", "xls" -> "application/vnd.ms-excel"
                else -> "application/octet-stream"
            }

            val fileName = "${UUID.randomUUID()}_file.$fileExt"
            val fileRef = storageRef.child("$folder/$fileName")

            val metadata = StorageMetadata.Builder()
                .setContentType(mimeType)
                .setCustomMetadata("uploadedAt", System.currentTimeMillis().toString())
                .setCustomMetadata("originalType", fileExt)
                .build()

            // Ultra-fast pipeline: If image, compress & resize to reduce transfer size from ~15MB to ~300KB
            var compressedBytes: ByteArray? = null
            if (isImage && appCtx != null) {
                compressedBytes = compressImageIfNeeded(appCtx, localFilePathOrUri)
            }

            // First attempt upload via Backblaze B2 File Server
            try {
                val fileBytes: ByteArray? = when {
                    compressedBytes != null -> compressedBytes
                    localFilePathOrUri.startsWith("content://") && appCtx != null -> {
                        val uri = Uri.parse(localFilePathOrUri)
                        appCtx.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                    }
                    else -> {
                        val file = File(localFilePathOrUri)
                        if (file.exists()) file.readBytes() else null
                    }
                }

                if (fileBytes != null) {
                    val b2ResultUrl = B2CloudStorageService.uploadFile(
                        bytes = fileBytes,
                        fileName = fileName,
                        contentType = mimeType
                    )
                    if (!b2ResultUrl.isNullOrBlank()) {
                        Log.d(TAG, "Fichier téléversé sur Backblaze B2 avec succès: $b2ResultUrl")
                        return@withContext b2ResultUrl
                    }
                }
            } catch (b2Ex: Exception) {
                Log.w(TAG, "Tentative B2 ignorée, repli vers Firebase Storage: ${b2Ex.message}")
            }

            if (compressedBytes != null) {
                Log.d(TAG, "Image compressée pour téléversement ultra-rapide (${compressedBytes.size / 1024} KB)")
                fileRef.putBytes(compressedBytes, metadata).await()
            } else if (localFilePathOrUri.startsWith("content://") && appCtx != null) {
                val uri = Uri.parse(localFilePathOrUri)
                val inputStream: InputStream? = appCtx.contentResolver.openInputStream(uri)
                if (inputStream != null) {
                    fileRef.putStream(inputStream, metadata).await()
                } else {
                    fileRef.putFile(uri, metadata).await()
                }
            } else {
                val file = File(localFilePathOrUri)
                if (file.exists()) {
                    val uri = Uri.fromFile(file)
                    fileRef.putFile(uri, metadata).await()
                } else {
                    val uri = Uri.parse(localFilePathOrUri)
                    fileRef.putFile(uri, metadata).await()
                }
            }

            // Retrieve the public HTTPS download URL accessible by any mobile device
            val downloadUrl = fileRef.downloadUrl.await()
            val resultUrl = downloadUrl.toString()
            Log.d(TAG, "Fichier téléversé sur Firebase Storage avec succès: $resultUrl")
            return@withContext resultUrl
        } catch (storageException: Exception) {
            Log.e(TAG, "Firebase Storage upload error: ${storageException.message}")
            throw Exception("Échec de l'envoi vers Firebase Storage (${storageException.localizedMessage ?: storageException.message}).")
        }
    }

    private fun compressImageIfNeeded(context: Context, localFilePathOrUri: String): ByteArray? {
        return try {
            val uri = if (localFilePathOrUri.startsWith("content://") || localFilePathOrUri.startsWith("file://")) {
                Uri.parse(localFilePathOrUri)
            } else {
                Uri.fromFile(File(localFilePathOrUri))
            }

            // 1. Decode bounds only
            val boundsOptions = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, boundsOptions)
            }

            val origWidth = boundsOptions.outWidth
            val origHeight = boundsOptions.outHeight
            if (origWidth <= 0 || origHeight <= 0) return null

            // 2. Calculate inSampleSize
            var sampleSize = 1
            var maxDim = Math.max(origWidth, origHeight)
            while (maxDim / 2 >= MAX_IMAGE_DIMENSION) {
                sampleSize *= 2
                maxDim /= 2
            }

            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }

            var bitmap = context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, decodeOptions)
            } ?: return null

            // 3. Fix EXIF orientation if needed
            try {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    val exif = ExifInterface(stream)
                    val orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
                    val matrix = Matrix()
                    when (orientation) {
                        ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                        ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                        ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
                    }
                    if (!matrix.isIdentity) {
                        val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
                        if (rotated != bitmap) {
                            bitmap.recycle()
                            bitmap = rotated
                        }
                    }
                }
            } catch (exifError: Exception) {
                Log.d(TAG, "Exif reading skipped: ${exifError.message}")
            }

            // 4. Downscale precisely if still > MAX_IMAGE_DIMENSION
            if (bitmap.width > MAX_IMAGE_DIMENSION || bitmap.height > MAX_IMAGE_DIMENSION) {
                val ratio = Math.min(
                    MAX_IMAGE_DIMENSION.toFloat() / bitmap.width,
                    MAX_IMAGE_DIMENSION.toFloat() / bitmap.height
                )
                val newW = (bitmap.width * ratio).toInt()
                val newH = (bitmap.height * ratio).toInt()
                val scaled = Bitmap.createScaledBitmap(bitmap, newW, newH, true)
                if (scaled != bitmap) {
                    bitmap.recycle()
                    bitmap = scaled
                }
            }

            // 5. Compress to high-efficiency JPEG
            val outputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, outputStream)
            bitmap.recycle()
            outputStream.toByteArray()
        } catch (e: Exception) {
            Log.w(TAG, "Fast compression skipped, falling back to original stream: ${e.message}")
            null
        }
    }

    suspend fun syncB2ServerFiles(repository: com.example.data.repository.HseRepository, context: Context? = null): B2SyncResult {
        return B2CloudStorageService.syncB2FilesToDatabase(repository, context)
    }

    suspend fun testB2Connection(): Pair<Boolean, String> {
        return B2CloudStorageService.testConnection()
    }

    suspend fun listB2Files(): List<B2FileItem> {
        return try {
            B2CloudStorageService.listBucketFiles()
        } catch (e: Exception) {
            emptyList()
        }
    }
}


