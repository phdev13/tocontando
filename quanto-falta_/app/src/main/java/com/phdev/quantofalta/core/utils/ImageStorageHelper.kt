package com.phdev.quantofalta.core.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.UUID

object ImageStorageHelper {
    private const val MAX_IMAGE_BYTES = 15L * 1024L * 1024L
    private const val MAX_IMAGE_DIMENSION = 12_000
    private const val MAX_IMAGE_PIXELS = 40_000_000L
    private val allowedMimeTypes = setOf("image/jpeg", "image/png", "image/webp", "image/heic", "image/heif")
    
    /**
     * Copia um arquivo do ContentResolver (ex: Galeria) para o armazenamento
     * interno do app, garantindo que não perderemos permissão de acesso.
     * 
     * Retorna a URI local em formato de String (ex: file://...)
     */
    fun copyImageToInternalStorage(context: Context, sourceUri: Uri): String? {
        var destFile: File? = null
        return try {
            val mimeType = context.contentResolver.getType(sourceUri)?.lowercase()
            if (mimeType != null && mimeType !in allowedMimeTypes) return null
            val dir = File(context.filesDir, "event_covers")
            if (!dir.exists() && !dir.mkdirs()) return null
            
            val destination = File(dir, "cover_${UUID.randomUUID()}.img")
            destFile = destination
            
            val input = context.contentResolver.openInputStream(sourceUri) ?: return null
            input.use {
                FileOutputStream(destination).use { output ->
                    val buffer = ByteArray(8_192)
                    var total = 0L
                    while (true) {
                        val count = it.read(buffer)
                        if (count < 0) break
                        total += count
                        if (total > MAX_IMAGE_BYTES) throw IllegalArgumentException("Image is too large")
                        output.write(buffer, 0, count)
                    }
                }
            }
            if (destination.length() == 0L) throw IllegalArgumentException("Empty image")

            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(destination.absolutePath, options)
            if (
                options.outWidth <= 0 || options.outHeight <= 0 ||
                options.outWidth > MAX_IMAGE_DIMENSION || options.outHeight > MAX_IMAGE_DIMENSION ||
                options.outWidth.toLong() * options.outHeight.toLong() > MAX_IMAGE_PIXELS
            ) throw IllegalArgumentException("Invalid image dimensions")
            
            Uri.fromFile(destination).toString()
        } catch (e: Exception) {
            destFile?.delete()
            e.printStackTrace()
            null
        }
    }

    fun duplicateInternalImage(context: Context, source: String?): String? {
        if (source.isNullOrBlank()) return null
        return try {
            val sourceFile = File(Uri.parse(source).path ?: return null)
            val coversDir = File(context.filesDir, "event_covers").canonicalFile
            if (!sourceFile.exists() || !isInside(sourceFile.canonicalFile, coversDir)) return source
            val destination = File(coversDir, "cover_${UUID.randomUUID()}.img")
            sourceFile.copyTo(destination, overwrite = false)
            Uri.fromFile(destination).toString()
        } catch (_: Exception) {
            source
        }
    }

    fun decodeBitmapForEditing(
        context: Context,
        source: String,
        maxDimension: Int = 1920
    ): Bitmap? {
        return try {
            val uri = Uri.parse(source)
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            openInputStream(context, uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

            var sampleSize = 1
            while (
                bounds.outWidth / sampleSize > maxDimension ||
                bounds.outHeight / sampleSize > maxDimension
            ) {
                sampleSize *= 2
            }

            val options = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            openInputStream(context, uri)?.use { BitmapFactory.decodeStream(it, null, options) }
        } catch (_: Exception) {
            null
        }
    }

    fun saveEditedCover(context: Context, bitmap: Bitmap): String? {
        var destFile: File? = null
        return try {
            val dir = File(context.filesDir, "event_covers")
            if (!dir.exists() && !dir.mkdirs()) return null

            val destination = File(dir, "cover_${UUID.randomUUID()}.jpg")
            destFile = destination
            FileOutputStream(destination).use { output ->
                if (!bitmap.compress(Bitmap.CompressFormat.JPEG, 90, output)) {
                    throw IllegalArgumentException("Could not encode edited cover")
                }
            }
            if (destination.length() == 0L || destination.length() > MAX_IMAGE_BYTES) {
                throw IllegalArgumentException("Invalid edited image")
            }
            Uri.fromFile(destination).toString()
        } catch (_: Exception) {
            destFile?.delete()
            null
        }
    }

    fun deleteInternalImage(context: Context, value: String?) {
        if (value.isNullOrBlank()) return
        runCatching {
            val file = File(Uri.parse(value).path ?: return)
            val coversDir = File(context.filesDir, "event_covers").canonicalFile
            if (isInside(file.canonicalFile, coversDir)) {
                file.delete()
            }
        }
    }

    fun isAvailable(value: String?): Boolean {
        if (value.isNullOrBlank()) return false
        return runCatching {
            val uri = Uri.parse(value)
            uri.scheme != "file" || File(uri.path ?: return false).exists()
        }.getOrDefault(false)
    }

    private fun isInside(file: File, directory: File): Boolean =
        file.path == directory.path || file.path.startsWith(directory.path + File.separator)

    private fun openInputStream(context: Context, uri: Uri): java.io.InputStream? {
        return if (uri.scheme == "file") {
            val path = uri.path ?: return null
            FileInputStream(File(path))
        } else {
            context.contentResolver.openInputStream(uri)
        }
    }
}
