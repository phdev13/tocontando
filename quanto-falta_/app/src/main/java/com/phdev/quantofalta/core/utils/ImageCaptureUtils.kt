package com.phdev.quantofalta.core.utils

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.Uri
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

object ImageCaptureUtils {
    private const val MAX_TEMP_FILE_AGE_MS = 24L * 60L * 60L * 1000L

    suspend fun captureComposableToBitmap(
        context: Context,
        width: Int,
        height: Int,
        content: @Composable () -> Unit,
    ): Bitmap = withContext(Dispatchers.Main) {
        val activity = context.findActivity()
            ?: error("Não foi possível acessar a janela para gerar a imagem.")
        val root = activity.window.decorView as ViewGroup
        val composeView = ComposeView(context).apply {
            alpha = 0.01f // Make it virtually invisible but not 0 to avoid culling
            setContent(content)
        }

        // Add at index 0 so it's behind everything else
        root.addView(composeView, 0, FrameLayout.LayoutParams(width, height))
        try {
            val widthSpec = View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY)
            val heightSpec = View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY)
            composeView.measure(widthSpec, heightSpec)
            composeView.layout(0, 0, width, height)

            // Attached composition receives lifecycle frames and Coil loading.
            delay(500L)
            composeView.measure(widthSpec, heightSpec)
            composeView.layout(0, 0, width, height)
            delay(100L)

            Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).also {
                composeView.draw(Canvas(it))
            }
        } finally {
            root.removeView(composeView)
            composeView.disposeComposition()
        }
    }

    suspend fun saveBitmapToCacheAndGetUri(
        context: Context,
        bitmap: Bitmap,
        filename: String = "shared_event_${System.currentTimeMillis()}.png",
    ): Uri? = withContext(Dispatchers.IO) {
        try {
            val cachePath = File(context.cacheDir, "shared_images")
            check(cachePath.exists() || cachePath.mkdirs()) {
                "Não foi possível preparar o armazenamento temporário."
            }
            val file = File(cachePath, filename)
            cleanupOldSharedImages(cachePath)
            FileOutputStream(file).use { out ->
                check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)) {
                    "Falha ao compactar a imagem."
                }
                out.flush()
            }
            check(file.length() > 0L) { "Arquivo de imagem vazio." }
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        } catch (_: Exception) {
            null
        }
    }

    private fun cleanupOldSharedImages(directory: File) {
        val cutoff = System.currentTimeMillis() - MAX_TEMP_FILE_AGE_MS
        directory.listFiles()?.forEach { file ->
            if (file.isFile && file.lastModified() < cutoff) {
                runCatching { file.delete() }
            }
        }
    }

    private tailrec fun Context.findActivity(): Activity? = when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }
}
