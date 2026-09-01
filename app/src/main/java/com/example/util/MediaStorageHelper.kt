package com.example.util

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import android.util.Log
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.text.DecimalFormat

data class ImportedFileResult(
    val fileName: String,
    val localFilePath: String,
    val fileUriString: String,
    val fileSizeFormatted: String,
    val mimeType: String,
    val contentType: String, // "PDF", "PHOTO", "VIDEO", "DOCUMENT"
    val isSuccess: Boolean = true,
    val textPreview: String = ""
)

object MediaStorageHelper {
    private const val TAG = "MediaStorageHelper"

    fun getRequiredMediaPermissions(): Array<String> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VIDEO,
                Manifest.permission.READ_MEDIA_AUDIO
            )
        } else {
            arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE
            )
        }
    }

    fun hasStoragePermissions(context: Context): Boolean {
        val permissions = getRequiredMediaPermissions()
        return permissions.all { perm ->
            ContextCompat.checkSelfPermission(context, perm) == PackageManager.PERMISSION_GRANTED
        }
    }

    fun importUriToAppStorage(context: Context, uri: Uri): ImportedFileResult {
        return try {
            var fileName = "document_${System.currentTimeMillis()}"
            var fileSize: Long = 0
            val contentResolver = context.contentResolver
            val mimeType = contentResolver.getType(uri) ?: "application/octet-stream"

            contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (cursor.moveToFirst()) {
                    if (nameIndex != -1) {
                        fileName = cursor.getString(nameIndex) ?: fileName
                    }
                    if (sizeIndex != -1) {
                        fileSize = cursor.getLong(sizeIndex)
                    }
                }
            }

            // Detect Content Type
            val lowerName = fileName.lowercase()
            val lowerMime = mimeType.lowercase()
            val contentType = when {
                lowerMime.contains("pdf") || lowerName.endsWith(".pdf") -> "PDF"
                lowerMime.contains("image") || lowerName.endsWith(".jpg") || lowerName.endsWith(".jpeg") || lowerName.endsWith(".png") || lowerName.endsWith(".webp") -> "PHOTO"
                lowerMime.contains("video") || lowerName.endsWith(".mp4") || lowerName.endsWith(".mkv") || lowerName.endsWith(".mov") || lowerName.endsWith(".avi") -> "VIDEO"
                else -> "PDF"
            }

            // Save to app internal storage directory
            val mediaDir = File(context.filesDir, "hse_media_documents")
            if (!mediaDir.exists()) {
                mediaDir.mkdirs()
            }

            val sanitizedName = fileName.replace("[^a-zA-Z0-9._-]".toRegex(), "_")
            val targetFile = File(mediaDir, "${System.currentTimeMillis()}_$sanitizedName")

            val inputStream: InputStream? = contentResolver.openInputStream(uri)
            inputStream?.use { input ->
                FileOutputStream(targetFile).use { output ->
                    input.copyTo(output)
                }
            }

            if (fileSize == 0L && targetFile.exists()) {
                fileSize = targetFile.length()
            }

            val formattedSize = formatFileSize(fileSize)

            ImportedFileResult(
                fileName = fileName,
                localFilePath = targetFile.absolutePath,
                fileUriString = uri.toString(),
                fileSizeFormatted = formattedSize,
                mimeType = mimeType,
                contentType = contentType,
                isSuccess = true
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error importing file from Uri: ${e.message}", e)
            ImportedFileResult(
                fileName = "Fichier importé",
                localFilePath = "",
                fileUriString = uri.toString(),
                fileSizeFormatted = "0 KB",
                mimeType = "application/octet-stream",
                contentType = "PDF",
                isSuccess = false
            )
        }
    }

    fun formatFileSize(size: Long): String {
        if (size <= 0) return "0 Ko"
        val units = arrayOf("B", "Ko", "Mo", "Go")
        val digitGroups = (Math.log10(size.toDouble()) / Math.log10(1024.0)).toInt()
        val formatted = DecimalFormat("#,##0.#").format(size / Math.pow(1024.0, digitGroups.toDouble()))
        return "$formatted ${units[digitGroups]}"
    }

    fun openFileWithExternalViewer(context: Context, filePathOrUri: String, fallbackMime: String = "*/*") {
        if (filePathOrUri.isBlank()) {
            Toast.makeText(context, "Lien ou fichier non disponible", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            if (filePathOrUri.startsWith("http://") || filePathOrUri.startsWith("https://")) {
                val webUri = Uri.parse(filePathOrUri)
                val browserIntent = Intent(Intent.ACTION_VIEW, webUri).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(Intent.createChooser(browserIntent, "Consulter / Télécharger le document"))
                return
            }

            val file = File(filePathOrUri)
            val uri: Uri = if (filePathOrUri.startsWith("content://")) {
                Uri.parse(filePathOrUri)
            } else if (file.exists()) {
                FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                )
            } else {
                Uri.parse(filePathOrUri)
            }

            val resolvedMime = when {
                filePathOrUri.contains(".pdf", ignoreCase = true) -> "application/pdf"
                filePathOrUri.contains(".png", ignoreCase = true) -> "image/png"
                filePathOrUri.contains(".jpg", ignoreCase = true) || filePathOrUri.contains(".jpeg", ignoreCase = true) -> "image/jpeg"
                filePathOrUri.contains(".webp", ignoreCase = true) -> "image/webp"
                filePathOrUri.contains(".mp4", ignoreCase = true) -> "video/mp4"
                filePathOrUri.contains(".mkv", ignoreCase = true) -> "video/x-matroska"
                filePathOrUri.contains(".doc", ignoreCase = true) || filePathOrUri.contains(".docx", ignoreCase = true) -> "application/msword"
                filePathOrUri.contains(".xls", ignoreCase = true) || filePathOrUri.contains(".xlsx", ignoreCase = true) -> "application/vnd.ms-excel"
                else -> fallbackMime
            }

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, resolvedMime)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(intent, "Ouvrir le document HSE avec"))
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open external viewer: ${e.message}")
            try {
                val genericIntent = Intent(Intent.ACTION_VIEW, Uri.parse(filePathOrUri)).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(genericIntent)
            } catch (inner: Exception) {
                Toast.makeText(context, "Impossible d'ouvrir le fichier : aucune application compatible.", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
