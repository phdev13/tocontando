package com.phdev.quantofalta.feature.more

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.phdev.quantofalta.core.AppViewModelProvider
import com.phdev.quantofalta.core.designsystem.components.*
import com.phdev.quantofalta.core.designsystem.theme.AppSpacing
import com.phdev.quantofalta.core.designsystem.theme.AppTypography
import com.phdev.quantofalta.core.navigation.Screen

@Composable
fun SettingsSection(title: String) {
    Spacer(modifier = Modifier.height(32.dp))
    Text(
        text = title.uppercase(),
        style = AppTypography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(horizontal = AppSpacing.extraLarge, vertical = AppSpacing.small)
    )
}

@Composable
fun SettingsRow(
    title: String,
    description: String? = null,
    icon: ImageVector,
    iconColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    titleColor: Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                try { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove) } catch (e: Exception) {}
                onClick()
            }
            .padding(horizontal = AppSpacing.extraLarge, vertical = AppSpacing.medium),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconColor,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(AppSpacing.large))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = AppTypography.bodyLarge,
                color = titleColor,
                fontWeight = FontWeight.Medium
            )
            if (description != null) {
                Text(
                    text = description,
                    style = AppTypography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun PremiumCard(premiumStatus: String, onClick: () -> Unit) {
    val haptic = LocalHapticFeedback.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = AppSpacing.extraLarge, vertical = AppSpacing.medium)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.primaryContainer)
            .clickable {
                try { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove) } catch (e: Exception) {}
                onClick()
            }
            .padding(AppSpacing.large)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Star,
                    contentDescription = "Premium",
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(28.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Tô Contando Premium",
                    style = AppTypography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = premiumStatus,
                    style = AppTypography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
            }
            
            Icon(
                imageVector = Icons.Filled.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoreScreen(
    onNavigate: (String) -> Unit,
    viewModel: MoreViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            AppTopBar(
                title = "Ajustes"
            )
        },
        bottomBar = {
            AppBottomNav(
                currentRoute = Screen.More.route,
                onNavigate = onNavigate
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { contentPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = contentPadding.calculateTopPadding(),
                bottom = contentPadding.calculateBottomPadding() + 24.dp
            )
        ) {
            
            // CARD PREMIUM SUPERIOR
            item(key = "premium_card") {
                Spacer(modifier = Modifier.height(8.dp))
                PremiumCard(
                    premiumStatus = uiState.premiumStatus,
                    onClick = { onNavigate(Screen.Premium.route) }
                )
            }

            // 1. PREFERÊNCIAS
            item(key = "section_preferencias", contentType = "section_title") {
                SettingsSection("Preferências")
            }
            item(key = "appearance", contentType = "settings_row") {
                SettingsRow(
                    title = "Aparência",
                    icon = Icons.Filled.Palette,
                    onClick = { onNavigate(Screen.SettingsAppearance.route) }
                )
            }
            item(key = "notifications", contentType = "settings_row") {
                SettingsRow(
                    title = "Notificações",
                    icon = Icons.Filled.Notifications,
                    onClick = { onNavigate(Screen.SettingsNotifications.route) }
                )
            }

            // 2. DADOS
            item(key = "section_dados", contentType = "section_title") {
                SettingsSection("Dados e Nuvem")
            }
            item(key = "sync", contentType = "settings_row") {
                SettingsRow(
                    title = "Conta e Sincronização",
                    icon = Icons.Filled.AccountCircle,
                    onClick = { onNavigate(Screen.SettingsSync.route) }
                )
            }
            item(key = "backup", contentType = "settings_row") {
                SettingsRow(
                    title = "Backup e restauração",
                    icon = Icons.Filled.CloudSync,
                    onClick = { onNavigate(Screen.SettingsBackup.route) }
                )
            }
            item(key = "clear_data", contentType = "settings_row") {
                SettingsRow(
                    title = "Apagar todos os dados",
                    icon = Icons.Filled.DeleteSweep,
                    iconColor = MaterialTheme.colorScheme.error,
                    titleColor = MaterialTheme.colorScheme.error,
                    onClick = { onNavigate(Screen.SettingsClearData.route) }
                )
            }

            // 3. AJUDA
            item(key = "section_ajuda", contentType = "section_title") {
                SettingsSection("Ajuda")
            }
            item(key = "support_feedback", contentType = "settings_row") {
                SettingsRow(
                    title = "Suporte e feedback",
                    icon = Icons.Filled.HelpOutline,
                    onClick = { onNavigate(Screen.SettingsSupport.route) }
                )
            }

            // 4. SOBRE
            item(key = "section_sobre", contentType = "section_title") {
                SettingsSection("Sobre")
            }
            item(key = "updates", contentType = "settings_row") {
                SettingsRow(
                    title = "Atualizações",
                    icon = Icons.Filled.SystemUpdate,
                    onClick = { onNavigate(Screen.SettingsUpdates.route) }
                )
            }
            item(key = "intro", contentType = "settings_row") {
                SettingsRow(
                    title = "Rever introdução",
                    icon = Icons.Filled.RestartAlt,
                    onClick = { 
                        viewModel.resetIntro()
                        onNavigate(Screen.Intro.route)
                    }
                )
            }
            item(key = "testers", contentType = "settings_row") {
                SettingsRow(
                    title = "Agradecimentos",
                    icon = Icons.Filled.Star,
                    onClick = { onNavigate(Screen.Testers.route) }
                )
            }
            item(key = "privacy_terms", contentType = "settings_row") {
                SettingsRow(
                    title = "Privacidade e termos",
                    icon = Icons.Filled.PrivacyTip,
                    onClick = { onNavigate(Screen.SettingsPrivacy.route) }
                )
            }
            item(key = "about", contentType = "settings_row") {
                SettingsRow(
                    title = "Sobre o Tô Contando",
                    icon = Icons.Filled.Info,
                    onClick = { onNavigate(Screen.SettingsAbout.route) }
                )
            }
        }
    }
}
