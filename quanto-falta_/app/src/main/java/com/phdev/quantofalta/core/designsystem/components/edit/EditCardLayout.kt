package com.phdev.quantofalta.core.designsystem.components.edit

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.EditCalendar
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.phdev.quantofalta.core.designsystem.components.AppTopBar
import com.phdev.quantofalta.core.designsystem.theme.AppSpacing
import com.phdev.quantofalta.core.designsystem.theme.AppTypography

private data class EditorTab(val title: String, val icon: ImageVector)

@Composable
fun EditCardLayout(
    title: String,
    onBack: () -> Unit,
    onSave: () -> Unit,
    isSaving: Boolean,
    isValid: Boolean,
    previewContent: @Composable () -> Unit,
    basicInfoContent: @Composable () -> Unit,
    temporalContent: @Composable () -> Unit,
    appearanceContent: @Composable () -> Unit,
    remindersContent: (@Composable () -> Unit)? = null,
    additionalOptionsContent: (@Composable () -> Unit)? = null,
    auxiliaryMessage: String? = null
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var isEditorExpanded by remember { mutableStateOf(false) }
    var dragHintOn by remember { mutableStateOf(false) }
    val configuration = LocalConfiguration.current
    val expandedPreviewHeight = 76.dp
    val relaxedPreviewHeight = (configuration.screenHeightDp.dp * 0.36f).coerceIn(220.dp, 340.dp)
    val previewHeight by animateDpAsState(
        targetValue = if (isEditorExpanded) expandedPreviewHeight else relaxedPreviewHeight,
        animationSpec = tween(260),
        label = "editor_preview_height"
    )
    val handleColor by animateColorAsState(
        targetValue = if (dragHintOn) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.82f)
        } else {
            MaterialTheme.colorScheme.outline.copy(alpha = 0.26f)
        },
        animationSpec = tween(260),
        label = "editor_drag_handle_color"
    )

    LaunchedEffect(Unit) {
        repeat(5) {
            dragHintOn = true
            kotlinx.coroutines.delay(260)
            dragHintOn = false
            kotlinx.coroutines.delay(360)
        }
    }
    
    val tabs = listOf(
        EditorTab("Detalhes", Icons.Filled.EditCalendar),
        EditorTab("Estilo", Icons.Filled.ColorLens),
        EditorTab("Alertas", Icons.Filled.Notifications),
        EditorTab("Ajustes", Icons.Filled.Settings)
    )

    Scaffold(
        topBar = {
            AppTopBar(
                title = title,
                centerTitle = true,
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.Close, contentDescription = "Fechar")
                    }
                },
                actions = {
                    Button(
                        onClick = onSave,
                        enabled = !isSaving,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                            disabledContainerColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
                            disabledContentColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                        ),
                        shape = RoundedCornerShape(18.dp),
                        contentPadding = PaddingValues(horizontal = AppSpacing.medium, vertical = 0.dp),
                        modifier = Modifier
                            .height(36.dp)
                            .padding(end = AppSpacing.small)
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Salvar",
                                style = AppTypography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // TOP PREVIEW AREA
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(previewHeight)
                    .padding(horizontal = AppSpacing.medium, vertical = AppSpacing.medium),
                contentAlignment = Alignment.Center
            ) {
                previewContent()
            }

            // BOTTOM EDITOR AREA
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .pointerInput(Unit) {
                        detectVerticalDragGestures { _, dragAmount ->
                            if (dragAmount < -10f) {
                                isEditorExpanded = true
                            } else if (dragAmount > 10f) {
                                isEditorExpanded = false
                            }
                        }
                    },
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
                shadowElevation = 16.dp
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(22.dp)
                            .clickable { isEditorExpanded = !isEditorExpanded },
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .width(46.dp)
                                .height(5.dp)
                                .background(handleColor, RoundedCornerShape(999.dp))
                        )
                    }

                    // TABS
                    ScrollableTabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = Color.Transparent,
                        contentColor = MaterialTheme.colorScheme.primary,
                        edgePadding = AppSpacing.medium,
                        divider = {}, // Remove default divider for a cleaner look
                        indicator = { tabPositions ->
                            if (selectedTab < tabPositions.size) {
                                androidx.compose.material3.TabRowDefaults.Indicator(
                                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                                    color = MaterialTheme.colorScheme.primary,
                                    height = 3.dp,
                                )
                            }
                        }
                    ) {
                        tabs.forEachIndexed { index, tab ->
                            val isSelected = selectedTab == index
                            Tab(
                                selected = isSelected,
                                onClick = { selectedTab = index },
                                text = { 
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = tab.icon, 
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp),
                                            tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = tab.title,
                                            style = AppTypography.labelLarge,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                                        )
                                    }
                                }
                            )
                        }
                    }
                    
                    // TAB CONTENT
                    AnimatedContent(
                        targetState = selectedTab,
                        transitionSpec = {
                            val duration = 300
                            if (targetState > initialState) {
                                slideInHorizontally(animationSpec = tween(duration)) { width -> width } + fadeIn(animationSpec = tween(duration)) togetherWith
                                slideOutHorizontally(animationSpec = tween(duration)) { width -> -width } + fadeOut(animationSpec = tween(duration))
                            } else {
                                slideInHorizontally(animationSpec = tween(duration)) { width -> -width } + fadeIn(animationSpec = tween(duration)) togetherWith
                                slideOutHorizontally(animationSpec = tween(duration)) { width -> width } + fadeOut(animationSpec = tween(duration))
                            }
                        },
                        label = "tab_transition",
                        modifier = Modifier.fillMaxSize()
                    ) { tabIndex ->
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(horizontal = AppSpacing.medium, vertical = AppSpacing.small),
                            verticalArrangement = Arrangement.spacedBy(AppSpacing.small)
                        ) {
                            when (tabIndex) {
                                0 -> { // Detalhes
                                    EditSection(title = "Informações básicas") {
                                        basicInfoContent()
                                    }
                                    Spacer(modifier = Modifier.height(AppSpacing.small))
                                    EditSection(title = "Data e horário") {
                                        temporalContent()
                                    }
                                }
                                1 -> { // Estilo
                                    appearanceContent()
                                }
                                2 -> { // Alertas
                                    if (remindersContent != null) {
                                        remindersContent()
                                    } else {
                                        Text(
                                            text = "Nenhuma opção de lembrete disponível.",
                                            style = AppTypography.bodyMedium,
                                            color = MaterialTheme.colorScheme.outline
                                        )
                                    }
                                }
                                3 -> { // Ajustes
                                    if (additionalOptionsContent != null) {
                                        additionalOptionsContent()
                                    }
                                    if (auxiliaryMessage != null) {
                                        Spacer(modifier = Modifier.height(AppSpacing.small))
                                        Text(
                                            text = auxiliaryMessage,
                                            style = AppTypography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                                .padding(AppSpacing.small)
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Spacer(modifier = Modifier.navigationBarsPadding())
                        }
                    }
                }
            }
        }
    }
}
