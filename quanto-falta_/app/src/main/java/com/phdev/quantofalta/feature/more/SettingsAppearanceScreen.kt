package com.phdev.quantofalta.feature.more

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.phdev.quantofalta.core.AppViewModelProvider
import com.phdev.quantofalta.core.designsystem.components.AppTopBar
import com.phdev.quantofalta.core.designsystem.components.PremiumFeatureInfo
import com.phdev.quantofalta.core.designsystem.components.PremiumFeatureModal
import com.phdev.quantofalta.core.designsystem.components.PremiumLockedRow
import com.phdev.quantofalta.core.designsystem.components.SettingsItem
import com.phdev.quantofalta.core.designsystem.components.SettingsSectionTitle
import com.phdev.quantofalta.core.designsystem.theme.AppThemeMode

@Composable
fun SettingsAppearanceScreen(
    onBack: () -> Unit,
    onNavigatePremium: () -> Unit,
    viewModel: MoreViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var lockedTheme by androidx.compose.runtime.remember {
        androidx.compose.runtime.mutableStateOf<AppThemeMode?>(null)
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = "Aparência",
                navigationIcon = Icons.Filled.ArrowBack,
                onNavigationClick = onBack
            )
        }
    ) { padding ->
        LazyColumn(modifier = androidx.compose.ui.Modifier.fillMaxSize(), contentPadding = padding) {
            item { SettingsSectionTitle("Tema do aplicativo") }
            items(AppThemeMode.entries) { mode ->
                if (mode.isPremium && !state.isPremium) {
                    PremiumLockedRow(
                        title = mode.displayName,
                        description = "Tema exclusivo Premium",
                        icon = Icons.Filled.Palette,
                        isLocked = true,
                        onLockedClick = { lockedTheme = mode }
                    )
                } else {
                    SettingsItem(
                        title = mode.displayName,
                        description = if (mode == AppThemeMode.SYSTEM) "Acompanha o tema do aparelho" else null,
                        icon = if (mode == AppThemeMode.SYSTEM) Icons.Filled.DarkMode else Icons.Filled.Palette,
                        onClick = { viewModel.selectTheme(mode) },
                        action = {
                            RadioButton(
                                selected = state.themeMode == mode,
                                onClick = { viewModel.selectTheme(mode) }
                            )
                        }
                    )
                }
            }
        }
    }

    lockedTheme?.let { mode ->
        PremiumFeatureModal(
            feature = PremiumFeatureInfo(
                title = mode.displayName,
                description = "Personalize todo o aplicativo com este tema exclusivo."
            ),
            onNavigatePremium = {
                lockedTheme = null
                onNavigatePremium()
            },
            onDismiss = { lockedTheme = null }
        )
    }
}
