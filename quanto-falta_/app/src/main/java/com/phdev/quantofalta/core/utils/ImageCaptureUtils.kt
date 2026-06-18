package com.phdev.quantofalta.core.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.Uri
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

object ImageCaptureUtils {

    /**
     * Captures a Composable out of the screen (headlessly) to a Bitmap.
     */
    suspend fun captureComposableToBitmap(
        context: Context,
        width: Int,
        height: Int,
        content: @Composable () -> Unit
    ): Bitmap = withContext(Dispatchers.Main) {
        val composeView = ComposeView(context).apply {
            setContent {
                content()
            }
        }
        
        // Measure and layout
        val widthMeasureSpec = View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY)
        val heightMeasureSpec = View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY)
        composeView.measure(widthMeasureSpec, heightMeasureSpec)
        composeView.layout(0, 0, width, height)

        // Draw to bitmap
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        composeView.draw(canvas)
        
        bitmap
    }

    /**
     * Saves a Bitmap to cache and returns its FileProvider Uri.
     */
    suspend fun saveBitmapToCacheAndGetUri(
        context: Context,
        bitmap: Bitmap,
        filename: String = "shared_event_${System.currentTimeMillis()}.png"
    ): Uri? = withContext(Dispatchers.IO) {
        try {
            val cachePath = File(context.cacheDir, "shared_images")
            if (!cachePath.exists()) {
                cachePath.mkdirs()
            }
            val file = File(cachePath, filename)
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                out.flush()
            }
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
