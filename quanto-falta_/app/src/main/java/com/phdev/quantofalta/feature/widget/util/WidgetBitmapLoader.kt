package com.phdev.quantofalta.feature.widget.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import coil.ImageLoader
import coil.request.ImageRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object WidgetBitmapLoader {

    suspend fun loadBitmap(context: Context, uri: String, size: Int = 500): Bitmap? {
        return withContext(Dispatchers.IO) {
            try {
                val loader = ImageLoader(context)
                val request = ImageRequest.Builder(context)
                    .data(uri)
                    .size(size, size)
                    .allowHardware(false) // Hardware bitmaps cause crashes in widgets (RemoteViews)
                    .build()
                
                val result = loader.execute(request)
                (result.drawable as? BitmapDrawable)?.bitmap
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }
}
