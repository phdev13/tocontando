package com.phdev.quantofalta.core.utils

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Provides a safe image picking flow for premium cover photos.
 *
 * The previous flow launched the third-party cropper after the system picker.
 * On some devices/Android builds that cropper Activity can crash the host app,
 * so the production-safe path stores the selected image directly.
 */
@Composable
fun rememberCoverImagePicker(
    isPremium: Boolean,
    onImageReady: (String) -> Unit,
    onError: (() -> Unit)? = null
): () -> Unit {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val localUri = withContext(Dispatchers.IO) {
                ImageStorageHelper.copyImageToInternalStorage(context, uri)
            }
            if (localUri != null) onImageReady(localUri) else onError?.invoke()
        }
    }

    return {
        if (!isPremium) {
            onError?.invoke()
        } else {
            runCatching {
                photoPicker.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            }.onFailure {
                onError?.invoke()
            }
        }
    }
}

@Composable
fun rememberCoverImageEditor(
    isPremium: Boolean,
    onImageReady: (String) -> Unit,
    onError: (() -> Unit)? = null
): (String) -> Unit {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    return { sourceUri ->
        if (!isPremium) {
            onError?.invoke()
        } else {
            scope.launch {
                val localUri = withContext(Dispatchers.IO) {
                    ImageStorageHelper.copyImageToInternalStorage(context, Uri.parse(sourceUri))
                }
                if (localUri != null) onImageReady(localUri) else onError?.invoke()
            }
        }
    }
}
