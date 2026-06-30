package com.phdev.quantofalta.core.designsystem.components.edit

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.RotateLeft
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.phdev.quantofalta.core.designsystem.components.PremiumSlider
import com.phdev.quantofalta.core.designsystem.components.getIconByName
import com.phdev.quantofalta.core.designsystem.theme.AppShapes
import com.phdev.quantofalta.core.designsystem.theme.AppSpacing
import com.phdev.quantofalta.core.designsystem.theme.AppTypography
import com.phdev.quantofalta.core.utils.ImageStorageHelper
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.abs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val CardCoverAspectRatio = 16f / 9f
private const val MaxZoomMultiplier = 5f
private const val EditingBitmapMaxDimension = 1_440
private const val HelpPreferenceName = "cover_photo_editor_help"
private const val HelpSeenKey = "seen_v1"
private const val MetadataPreferenceName = "cover_photo_editor_metadata"

// =============================================================================
// ARQUITETURA DO PREVIEW
// =============================================================================
// O preview usa uma camada de imagem para manter o arraste fluido:
//
//   Foto: ContentScale.Crop + zoom + pan
//     → Preenche o card sem fundo azul, funciona como backdrop desfocado
//
//   O render final ainda recria o fundo escurecido fora da interacao.
//     → Mostra a IMAGEM COMPLETA sem crop no editor
//     → O usuario ve TUDO e pode decidir como posicionar
//
// Pan limit usa coverScale, pois o editor funciona como capa 16:9.
// Ao salvar, renderizamos o que esta visivel no viewport (16:9 = resultado final).
// =============================================================================

@Composable
fun CoverPhotoEditorScreen(
    sourceUri: String?,
    cardTitle: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
    modifier: Modifier = Modifier,
    cardSubtitle: String? = null,
    cardIconName: String = "Star",
    cardPrimaryText: String? = null,
    cardSecondaryText: String? = null,
    accentColor: Color = Color(0xFF7B61FF),
    onPickPhoto: (() -> Unit)? = null,
    onRemovePhoto: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val metadataPrefs = remember {
        context.getSharedPreferences(MetadataPreferenceName, android.content.Context.MODE_PRIVATE)
    }
    var activeSourceUri by rememberSaveable(sourceUri) {
        mutableStateOf(sourceUri?.let { metadataPrefs.getString(originalKey(it), it) ?: it })
    }
    var bitmap by remember(activeSourceUri) { mutableStateOf<Bitmap?>(null) }
    var isLoading by remember(activeSourceUri) { mutableStateOf(activeSourceUri != null) }
    var isSaving by remember(sourceUri) { mutableStateOf(false) }

    // zoom 1f = imagem cobrindo todo o card (ContentScale.Crop)
    var zoomMultiplier by rememberSaveable(sourceUri) {
        mutableFloatStateOf(
            (sourceUri?.let { metadataPrefs.getFloat(zoomKey(it), 1f) } ?: 1f)
                .coerceIn(1f, MaxZoomMultiplier)
        )
    }
    var offsetX by rememberSaveable(sourceUri) {
        mutableFloatStateOf(sourceUri?.let { metadataPrefs.getFloat(offsetXKey(it), 0f) } ?: 0f)
    }
    var offsetY by rememberSaveable(sourceUri) {
        mutableFloatStateOf(sourceUri?.let { metadataPrefs.getFloat(offsetYKey(it), 0f) } ?: 0f)
    }
    var rotationDegrees by rememberSaveable(sourceUri) {
        mutableIntStateOf(sourceUri?.let { metadataPrefs.getInt(rotationKey(it), 0) } ?: 0)
    }
    var viewportSize by remember { mutableStateOf(IntSize.Zero) }
    val helpPrefs = remember {
        context.getSharedPreferences(HelpPreferenceName, android.content.Context.MODE_PRIVATE)
    }
    var showHelp by remember { mutableStateOf(false) }
    var showOnboardingHint by remember {
        mutableStateOf(!helpPrefs.getBoolean(OnboardingHintKey, false))
    }

    val currentBitmap = remember(bitmap, rotationDegrees) {
        val source = bitmap
        when {
            source == null -> null
            rotationDegrees == 0 -> source
            else -> rotateBitmap(source, rotationDegrees.toFloat())
        }
    }

    LaunchedEffect(sourceUri) {
        if (sourceUri != null) {
            activeSourceUri = metadataPrefs.getString(originalKey(sourceUri), sourceUri) ?: sourceUri
            zoomMultiplier = metadataPrefs.getFloat(zoomKey(sourceUri), 1f)
                .coerceIn(1f, MaxZoomMultiplier)
            offsetX = metadataPrefs.getFloat(offsetXKey(sourceUri), 0f)
            offsetY = metadataPrefs.getFloat(offsetYKey(sourceUri), 0f)
            rotationDegrees = metadataPrefs.getInt(rotationKey(sourceUri), 0)
        }
    }

    LaunchedEffect(activeSourceUri) {
        val uri = activeSourceUri
        if (uri == null) {
            bitmap = null
            isLoading = false
            return@LaunchedEffect
        }
        isLoading = true
        bitmap = withContext(Dispatchers.IO) {
            ImageStorageHelper.decodeBitmapForEditing(
                context = context,
                source = uri,
                maxDimension = EditingBitmapMaxDimension
            )
        }
        isLoading = false
    }

    val panLimit = remember(currentBitmap, viewportSize, zoomMultiplier, rotationDegrees) {
        if (currentBitmap != null && viewportSize.width > 0 && viewportSize.height > 0)
            computePanLimit(currentBitmap, viewportSize, zoomMultiplier)
        else Offset.Zero
    }

    LaunchedEffect(currentBitmap, viewportSize, panLimit) {
        if (currentBitmap != null && viewportSize.width > 0 && viewportSize.height > 0) {
            offsetX = offsetX.coerceIn(-panLimit.x, panLimit.x)
            offsetY = offsetY.coerceIn(-panLimit.y, panLimit.y)
        }
    }

    fun resetTransform() {
        zoomMultiplier = 1f
        offsetX = 0f
        offsetY = 0f
    }

    fun rememberEditedResult(result: String) {
        metadataPrefs.edit()
            .putString(originalKey(result), activeSourceUri ?: result)
            .putFloat(zoomKey(result), zoomMultiplier)
            .putFloat(offsetXKey(result), offsetX)
            .putFloat(offsetYKey(result), offsetY)
            .putInt(rotationKey(result), rotationDegrees)
            .apply()
    }

    if (showHelp) {
        HelpDialog(
            onDismissRequest = {
                helpPrefs.edit().putBoolean(HelpSeenKey, true).apply()
                showHelp = false
            }
        )
    }

    val saveAction: () -> Unit = saveAction@{
        val source = currentBitmap ?: return@saveAction
        isSaving = true
        scope.launch {
            val result = withContext(Dispatchers.IO) {
                val edited = renderEditedBitmap(
                    source = source,
                    viewportSize = viewportSize,
                    zoomMultiplier = zoomMultiplier,
                    offset = Offset(offsetX, offsetY)
                )
                try {
                    ImageStorageHelper.saveEditedCover(context, edited)
                } finally {
                    edited.recycle()
                }
            }
            isSaving = false
            if (result != null) {
                rememberEditedResult(result)
                onSave(result)
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            EditorTopBar(
                isSaving = isSaving,
                canApply = currentBitmap != null && !isLoading && viewportSize.width > 0,
                onDismiss = onDismiss,
                onShowHelp = { showHelp = true },
                onApply = { saveAction() }
            )
        },
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.background.copy(alpha = 0.96f),
                tonalElevation = 8.dp
            ) {
                Button(
                    enabled = currentBitmap != null && !isLoading && !isSaving && viewportSize.width > 0,
                    onClick = { saveAction() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                        .height(52.dp),
                    shape = AppShapes.large,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    AnimatedContent(
                        targetState = isSaving,
                        label = "SaveButtonContent"
                    ) { saving ->
                        if (saving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Aplicar", style = AppTypography.labelLarge.copy(fontWeight = FontWeight.Bold))
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            CoverCardPreview(
                bitmap = currentBitmap,
                isLoading = isLoading,
                cardTitle = cardTitle,
                cardSubtitle = cardSubtitle,
                cardIconName = cardIconName,
                cardPrimaryText = cardPrimaryText,
                cardSecondaryText = cardSecondaryText,
                accentColor = accentColor,
                showOnboardingHint = showOnboardingHint,
                onInteractionStarted = {
                    if (showOnboardingHint) {
                        helpPrefs.edit().putBoolean(OnboardingHintKey, true).apply()
                        showOnboardingHint = false
                    }
                },
                zoomProvider = { zoomMultiplier },
                offsetXProvider = { offsetX },
                offsetYProvider = { offsetY },
                onViewportChanged = { viewportSize = it },
                onGesture = { pan, zoomChange, centroid ->
                    val oldZoom = zoomMultiplier
                    val newZoom = (oldZoom * zoomChange).coerceIn(1f, MaxZoomMultiplier)
                    val appliedZoomChange = if (oldZoom > 0f) newZoom / oldZoom else 1f
                    zoomMultiplier = newZoom
                    val bmp = currentBitmap
                    if (bmp != null && viewportSize.width > 0) {
                        val limit = computePanLimit(bmp, viewportSize, newZoom)
                        val viewportCenter = Offset(viewportSize.width / 2f, viewportSize.height / 2f)
                        val pivot = centroid - viewportCenter
                        val currentOffset = Offset(offsetX, offsetY)
                        val nextOffset = currentOffset + pan + (pivot - currentOffset) * (1f - appliedZoomChange)
                        offsetX = nextOffset.x.coerceIn(-limit.x, limit.x)
                        offsetY = nextOffset.y.coerceIn(-limit.y, limit.y)
                    }
                }
            )

            Text(
                text = "A foto aparece completa. Use zoom ou arraste para escolher o enquadramento.",
                style = AppTypography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 2.dp)
            )

            PhotoCardBlock(
                hasImage = currentBitmap != null,
                enabled = !isSaving,
                onAddOrChange = { onPickPhoto?.invoke() },
                onRemove = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    resetTransform()
                    rotationDegrees = 0
                    activeSourceUri = null
                    bitmap = null
                    onRemovePhoto?.invoke()
                }
            )

            AnimatedVisibility(
                visible = currentBitmap != null,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                AdjustmentsBlock(
                    hasImage = true,
                    enabled = !isSaving,
                    zoomMultiplier = zoomMultiplier,
                    onZoomChange = {
                        val newZoom = it.coerceIn(1f, MaxZoomMultiplier)
                        zoomMultiplier = newZoom
                        val limit = if (currentBitmap != null && viewportSize.width > 0) 
                            computePanLimit(currentBitmap, viewportSize, newZoom) 
                        else Offset.Zero
                        offsetX = offsetX.coerceIn(-limit.x, limit.x)
                        offsetY = offsetY.coerceIn(-limit.y, limit.y)
                        if (isNear(it, 1f) || isNear(it, MaxZoomMultiplier)) {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        }
                    },
                    offsetX = offsetX,
                    offsetY = offsetY,
                    panLimit = panLimit,
                    onHorizontalChange = { 
                        offsetX = it
                        if (isNear(it, 0f) || isNear(it, -panLimit.x) || isNear(it, panLimit.x)) {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        }
                    },
                    onVerticalChange = { 
                        offsetY = it 
                        if (isNear(it, 0f) || isNear(it, -panLimit.y) || isNear(it, panLimit.y)) {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        }
                    },
                    onReset = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        resetTransform() 
                    },
                    rotationDegrees = rotationDegrees,
                    onRotateLeft = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        rotationDegrees = (rotationDegrees - 90 + 360) % 360
                        resetTransform()
                    },
                    onRotateRight = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        rotationDegrees = (rotationDegrees + 90) % 360
                        resetTransform()
                    }
                )
            }
        }
    }
}

@Composable
private fun HelpDialog(onDismissRequest: () -> Unit) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 28.dp),
            shape = AppShapes.large,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 10.dp,
            shadowElevation = 18.dp
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Text(
                    text = "Como ajustar sua foto",
                    style = AppTypography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("🤏", style = AppTypography.titleMedium)
                        Column {
                            Text("Dê zoom", style = AppTypography.bodyLarge.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface)
                            Text("Aproxime ou afaste com os dedos.", style = AppTypography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("👆", style = AppTypography.titleMedium)
                        Column {
                            Text("Arraste", style = AppTypography.bodyLarge.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface)
                            Text("Mova a foto até ficar do jeito que você quer.", style = AppTypography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("🔄", style = AppTypography.titleMedium)
                        Column {
                            Text("Gire", style = AppTypography.bodyLarge.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface)
                            Text("Use os botões para girar a foto se precisar.", style = AppTypography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("✨", style = AppTypography.titleMedium)
                        Column {
                            Text("Centralize", style = AppTypography.bodyLarge.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface)
                            Text("Volte para um enquadramento bonito quando quiser.", style = AppTypography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                Button(
                    onClick = onDismissRequest,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = AppShapes.large
                ) {
                    Text("Entendi", style = AppTypography.labelLarge.copy(fontWeight = FontWeight.Bold))
                }
            }
        }
    }
}

@Composable
private fun EditorTopBar(
    isSaving: Boolean,
    canApply: Boolean,
    onDismiss: () -> Unit,
    onShowHelp: () -> Unit,
    onApply: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = { if (!isSaving) onDismiss() }, enabled = !isSaving) {
            Icon(Icons.Filled.Close, contentDescription = "Voltar")
        }
        Text(
            text = "Ajustar foto",
            style = AppTypography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
        )
        IconButton(onClick = onShowHelp, enabled = !isSaving) {
            Icon(Icons.Filled.HelpOutline, contentDescription = "Ajuda")
        }
        Button(
            enabled = canApply && !isSaving,
            shape = AppShapes.large,
            contentPadding = PaddingValues(horizontal = 16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            onClick = {
                onApply()
            }
        ) {
            AnimatedContent(targetState = isSaving, label = "SaveTopBar") { saving ->
                if (saving) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text("Aplicar")
        }
    }
}

@Composable
private fun CoverCardPreview(
    bitmap: Bitmap?,
    isLoading: Boolean,
    cardTitle: String,
    cardSubtitle: String?,
    cardIconName: String,
    cardPrimaryText: String?,
    cardSecondaryText: String?,
    accentColor: Color,
    showOnboardingHint: Boolean,
    onInteractionStarted: () -> Unit,
    zoomProvider: () -> Float,
    offsetXProvider: () -> Float,
    offsetYProvider: () -> Float,
    onViewportChanged: (IntSize) -> Unit,
    onGesture: (pan: Offset, zoomChange: Float, centroid: Offset) -> Unit
) {
    val imageBitmap = remember(bitmap) { bitmap?.asImageBitmap() }
    var hasInteracted by remember { mutableStateOf(false) }
    var isInteracting by remember { mutableStateOf(false) }
    
    val textAlpha by animateFloatAsState(
        targetValue = if (isInteracting) 0.15f else 1f,
        animationSpec = tween(durationMillis = 200),
        label = "textAlpha"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(14.dp, AppShapes.large, clip = false)
            .clip(AppShapes.large)
            .background(Color.Black)
            .aspectRatio(CardCoverAspectRatio)
            .onSizeChanged(onViewportChanged)
            .pointerInput(bitmap) {
                if (bitmap != null) {
                    awaitEachGesture {
                        awaitFirstDown(requireUnconsumed = false)
                        isInteracting = true
                        if (!hasInteracted) {
                            hasInteracted = true
                            onInteractionStarted()
                        }
                        do {
                            val event = awaitPointerEvent()
                            val pan = event.calculatePan()
                            val zoomChange = event.calculateZoom()
                            val centroid = event.calculateCentroid(useCurrent = true)
                            if (pan != Offset.Zero || zoomChange != 1f) {
                                event.changes.forEach { it.consume() }
                                onGesture(pan, zoomChange, centroid)
                            }
                        } while (event.changes.any { it.pressed })
                        isInteracting = false
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        when {
            isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(accentColor),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color.White)
                }
            }
            imageBitmap != null -> {
                androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                    val zoom = zoomProvider().coerceIn(1f, MaxZoomMultiplier)
                    val imgW = imageBitmap.width.toFloat()
                    val imgH = imageBitmap.height.toFloat()
                    val transform = calculateCoverTransform(
                        imageWidth = imgW,
                        imageHeight = imgH,
                        viewportWidth = size.width,
                        viewportHeight = size.height,
                        zoom = zoom,
                        offset = Offset(offsetXProvider(), offsetYProvider())
                    )

                    drawImage(
                        image = imageBitmap,
                        dstOffset = IntOffset(
                            floor(transform.left).toInt(),
                            floor(transform.top).toInt()
                        ),
                        dstSize = IntSize(
                            ceil(transform.width).toInt(),
                            ceil(transform.height).toInt()
                        )
                    )
                }
            }
            else -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(listOf(accentColor, accentColor.copy(alpha = 0.72f)))
                        )
                )
            }
        }

        if (bitmap != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.18f))
            )
        }

        AnimatedVisibility(
            visible = bitmap != null && showOnboardingHint && !hasInteracted,
            enter = fadeIn(tween(500)),
            exit = fadeOut(tween(300)),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Column(
                modifier = Modifier
                    .clip(AppShapes.large)
                    .background(Color.Black.copy(alpha = 0.65f))
                    .padding(horizontal = 24.dp, vertical = 18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "👆",
                    fontSize = 32.sp
                )
                Text(
                    text = "Arraste ou dê zoom\ncom os dedos",
                    style = AppTypography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(14.dp)
                .size(42.dp)
                .graphicsLayer { alpha = textAlpha }
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.28f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = getIconByName(cardIconName),
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(23.dp)
            )
        }

        cardPrimaryText?.takeIf { it.isNotBlank() }?.let { primary ->
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = 24.dp)
                    .graphicsLayer { alpha = textAlpha },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = primary,
                    style = AppTypography.displayMedium.copy(fontWeight = FontWeight.Black),
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                cardSecondaryText?.takeIf { it.isNotBlank() }?.let { secondary ->
                    Text(
                        text = secondary.uppercase(),
                        style = AppTypography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = Color.White.copy(alpha = 0.88f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(18.dp)
                .fillMaxWidth(0.68f)
                .graphicsLayer { alpha = textAlpha }
        ) {
            Text(
                text = cardTitle.ifBlank { "Meu card" },
                style = AppTypography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            cardSubtitle?.takeIf { it.isNotBlank() }?.let {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = it,
                    style = AppTypography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = Color.White.copy(alpha = 0.82f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun PhotoCardBlock(
    hasImage: Boolean,
    enabled: Boolean,
    onAddOrChange: () -> Unit,
    onRemove: () -> Unit
) {
    EditorBlock(title = "Foto do card") {
        if (hasImage) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = onAddOrChange, enabled = enabled,
                    modifier = Modifier.weight(1f), shape = AppShapes.large
                ) {
                    Icon(Icons.Filled.SwapHoriz, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Trocar foto")
                }
                OutlinedButton(
                    onClick = onRemove, enabled = enabled,
                    modifier = Modifier.weight(1f), shape = AppShapes.large,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.Filled.DeleteOutline, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Remover foto")
                }
            }
        } else {
            Button(
                onClick = onAddOrChange, enabled = enabled,
                modifier = Modifier.fillMaxWidth(), shape = AppShapes.large,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Filled.Image, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Adicionar foto")
            }
        }
    }
}

@Composable
private fun AdjustmentsBlock(
    hasImage: Boolean,
    enabled: Boolean,
    zoomMultiplier: Float,
    onZoomChange: (Float) -> Unit,
    offsetX: Float,
    offsetY: Float,
    panLimit: Offset,
    onHorizontalChange: (Float) -> Unit,
    onVerticalChange: (Float) -> Unit,
    onReset: () -> Unit,
    rotationDegrees: Int,
    onRotateLeft: () -> Unit,
    onRotateRight: () -> Unit
) {
    var showPreciseAdjustments by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
    EditorBlock(title = "Ajuste rápido") {
        if (!hasImage) {
            Text(
                text = "Adicione uma foto para liberar os ajustes.",
                style = AppTypography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            return@EditorBlock
        }

        RotationRow(
            rotationDegrees = rotationDegrees,
            enabled = enabled,
            onRotateLeft = onRotateLeft,
            onRotateRight = onRotateRight
        )

        PremiumSlider(
            label = "Zoom",
            valueText = "${(zoomMultiplier * 100).roundToInt()}%",
            value = zoomMultiplier,
            valueRange = 1f..MaxZoomMultiplier,
            enabled = enabled,
            onValueChange = onZoomChange
        )

        OutlinedButton(
            onClick = onReset, enabled = enabled,
            modifier = Modifier.fillMaxWidth(),
            shape = AppShapes.large,
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
        ) {
            Icon(Icons.Filled.RestartAlt, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Centralizar imagem")
        }
    }

    if (hasImage) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = AppShapes.large,
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showPreciseAdjustments = !showPreciseAdjustments }
                        .padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Ajustes precisos",
                        style = AppTypography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Icon(
                        imageVector = if (showPreciseAdjustments) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                AnimatedVisibility(visible = showPreciseAdjustments) {
                    Column(
                        modifier = Modifier.padding(top = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        PremiumSlider(
                            label = "Horizontal",
                            valueText = centeredValueText(offsetX, panLimit.x),
                            value = offsetX,
                            valueRange = -panLimit.x.coerceAtLeast(1f)..panLimit.x.coerceAtLeast(1f),
                            enabled = enabled && panLimit.x > 0f,
                            onValueChange = onHorizontalChange
                        )

                        PremiumSlider(
                            label = "Vertical",
                            valueText = centeredValueText(offsetY, panLimit.y),
                            value = offsetY,
                            valueRange = -panLimit.y.coerceAtLeast(1f)..panLimit.y.coerceAtLeast(1f),
                            enabled = enabled && panLimit.y > 0f,
                            onValueChange = onVerticalChange
                        )
                    }
                }
            }
        }
    }
    }
}

@Composable
private fun RotationRow(
    rotationDegrees: Int,
    enabled: Boolean,
    onRotateLeft: () -> Unit,
    onRotateRight: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Rotação",
                style = AppTypography.labelLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "${rotationDegrees}°",
                style = AppTypography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(
                onClick = onRotateLeft, enabled = enabled,
                modifier = Modifier.weight(1f), shape = AppShapes.large
            ) {
                Icon(Icons.Filled.RotateLeft, contentDescription = "Girar esquerda", modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("-90°", style = AppTypography.labelMedium.copy(fontWeight = FontWeight.SemiBold))
            }
            OutlinedButton(
                onClick = onRotateRight, enabled = enabled,
                modifier = Modifier.weight(1f), shape = AppShapes.large
            ) {
                Icon(Icons.Filled.RotateRight, contentDescription = "Girar direita", modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("+90°", style = AppTypography.labelMedium.copy(fontWeight = FontWeight.SemiBold))
            }
        }
    }
}

@Composable
private fun EditorBlock(title: String, content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = AppShapes.large,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier.padding(AppSpacing.medium),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = title,
                style = AppTypography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            content()
        }
    }
}

private fun centeredValueText(value: Float, limit: Float): String {
    if (limit <= 0f) return "Centro"
    val percent = ((value / limit) * 100f).roundToInt()
    return when {
        percent == 0 -> "Centro"
        percent > 0 -> "+$percent%"
        else -> "$percent%"
    }
}

private fun isNear(value: Float, target: Float, tolerance: Float = 0.5f): Boolean {
    return abs(value - target) <= tolerance
}

private data class CoverTransform(
    val left: Float,
    val top: Float,
    val width: Float,
    val height: Float,
    val maxPanX: Float,
    val maxPanY: Float,
    val clampedOffset: Offset
)

private fun calculateCoverTransform(
    imageWidth: Float,
    imageHeight: Float,
    viewportWidth: Float,
    viewportHeight: Float,
    zoom: Float,
    offset: Offset
): CoverTransform {
    val safeZoom = zoom.coerceIn(1f, MaxZoomMultiplier)
    val coverScale = max(viewportWidth / imageWidth, viewportHeight / imageHeight)
    val width = imageWidth * coverScale * safeZoom
    val height = imageHeight * coverScale * safeZoom
    val maxPanX = max(0f, width - viewportWidth) / 2f
    val maxPanY = max(0f, height - viewportHeight) / 2f
    val clampedOffset = Offset(
        x = offset.x.coerceIn(-maxPanX, maxPanX),
        y = offset.y.coerceIn(-maxPanY, maxPanY)
    )

    return CoverTransform(
        left = viewportWidth / 2f - width / 2f + clampedOffset.x,
        top = viewportHeight / 2f - height / 2f + clampedOffset.y,
        width = width,
        height = height,
        maxPanX = maxPanX,
        maxPanY = maxPanY,
        clampedOffset = clampedOffset
    )
}

private fun computePanLimit(bitmap: Bitmap, viewport: IntSize, zoom: Float): Offset {
    val transform = calculateCoverTransform(
        imageWidth = bitmap.width.toFloat(),
        imageHeight = bitmap.height.toFloat(),
        viewportWidth = viewport.width.toFloat(),
        viewportHeight = viewport.height.toFloat(),
        zoom = zoom,
        offset = Offset.Zero
    )
    return Offset(
        x = transform.maxPanX,
        y = transform.maxPanY
    )
}

// Renderiza o bitmap final (1280x720) replicando EXATAMENTE o que o editor mostra.
//
// Camada 1 (fundo) : ContentScale.Crop — imagem preenche o canvas 16:9
//                    + overlay escuro 52% (igual ao editor)
// Camada 2 (frente): ContentScale.Crop * zoomMultiplier + offset (pan)
//                    — imagem completa escalada e posicionada pelo usuario
//
// Resultado: zero fundo preto; visual identico ao preview do editor.
private fun renderEditedBitmap(
    source: Bitmap,
    viewportSize: IntSize,
    zoomMultiplier: Float,
    offset: Offset
): Bitmap {
    val outputWidth = 1_280
    val outputHeight = (outputWidth / CardCoverAspectRatio).roundToInt()
    val output = Bitmap.createBitmap(outputWidth, outputHeight, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(output)
    val bitmapPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

    val imageWidth = source.width.toFloat()
    val imageHeight = source.height.toFloat()

    // ---- Camada 1: fundo (ContentScale.Crop) ----
    val backgroundTransform = calculateCoverTransform(
        imageWidth = imageWidth,
        imageHeight = imageHeight,
        viewportWidth = outputWidth.toFloat(),
        viewportHeight = outputHeight.toFloat(),
        zoom = 1f,
        offset = Offset.Zero
    )
    canvas.drawBitmap(
        source,
        null,
        RectF(
            backgroundTransform.left,
            backgroundTransform.top,
            backgroundTransform.left + backgroundTransform.width,
            backgroundTransform.top + backgroundTransform.height
        ),
        bitmapPaint
    )

    // Overlay escuro 52% sobre o fundo (replica o editor)
    canvas.drawRect(
        0f, 0f, outputWidth.toFloat(), outputHeight.toFloat(),
        Paint().apply { color = android.graphics.Color.argb(133, 0, 0, 0) } // 0.52 * 255 ≈ 133
    )

    // ---- Camada 2: frente (ContentScale.Crop + zoom + pan) ----
    val vpScaleX = outputWidth.toFloat() / viewportSize.width.toFloat()
    val vpScaleY = outputHeight.toFloat() / viewportSize.height.toFloat()
    val outputOffset = Offset(offset.x * vpScaleX, offset.y * vpScaleY)
    val foregroundTransform = calculateCoverTransform(
        imageWidth = imageWidth,
        imageHeight = imageHeight,
        viewportWidth = outputWidth.toFloat(),
        viewportHeight = outputHeight.toFloat(),
        zoom = zoomMultiplier,
        offset = outputOffset
    )
    canvas.drawBitmap(
        source,
        null,
        RectF(
            foregroundTransform.left,
            foregroundTransform.top,
            foregroundTransform.left + foregroundTransform.width,
            foregroundTransform.top + foregroundTransform.height
        ),
        bitmapPaint
    )

    return output
}

private fun rotateBitmap(source: Bitmap, degrees: Float): Bitmap {
    val matrix = Matrix().apply { postRotate(degrees) }
    return Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
}

private fun originalKey(uri: String): String = "original:$uri"
private fun zoomKey(uri: String): String = "zoom:$uri"
private fun offsetXKey(uri: String): String = "offsetX:$uri"
private fun offsetYKey(uri: String): String = "offsetY:$uri"
private fun rotationKey(uri: String): String = "rotation:$uri"

private const val OnboardingHintKey = "onboarding_hint_v2"
