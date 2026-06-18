package com.phdev.quantofalta.core.utils

import android.content.Context
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

object ImageStorageHelper {
    
    /**
     * Copia um arquivo do ContentResolver (ex: Galeria) para o armazenamento
     * interno do app, garantindo que não perderemos permissão de acesso.
     * 
     * Retorna a URI local em formato de String (ex: file://...)
     */
    fun copyImageToInternalStorage(context: Context, sourceUri: Uri): String? {
        return try {
            val dir = File(context.filesDir, "event_covers")
            if (!dir.exists()) {
                dir.mkdirs()
            }
            
            val destFile = File(dir, "cover_${UUID.randomUUID()}.jpg")
            
            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            }
            
            Uri.fromFile(destFile).toString()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
