package com.phdev.quantofalta.feature.more

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.phdev.quantofalta.BuildConfig
import com.phdev.quantofalta.core.designsystem.components.AppTopBar
import com.phdev.quantofalta.core.designsystem.components.SettingsItem
import com.phdev.quantofalta.core.designsystem.components.SettingsSectionTitle

@Composable
fun SettingsAboutScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    Scaffold(
        topBar = {
            AppTopBar(
                title = "Sobre o Tô Contando",
                navigationIcon = Icons.Filled.ArrowBack,
                onNavigationClick = onBack
            )
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = padding) {
            item { SettingsSectionTitle("Aplicativo") }
            item {
                SettingsItem(
                    title = "Tô Contando",
                    description = "Cada dia conta. Versão ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE}).",
                    icon = Icons.Filled.Info
                )
            }
            item { SettingsSectionTitle("Links") }
            item {
                SettingsItem(
                    title = "Site oficial",
                    description = BuildConfig.SITE_BASE_URL,
                    icon = Icons.Filled.Language,
                    onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(BuildConfig.SITE_BASE_URL))) }
                )
            }
        }
    }
}
