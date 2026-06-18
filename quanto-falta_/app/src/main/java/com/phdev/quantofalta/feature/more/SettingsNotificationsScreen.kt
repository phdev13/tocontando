package com.phdev.quantofalta.feature.more

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.phdev.quantofalta.core.designsystem.components.AppTopBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsNotificationsScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            AppTopBar(
                title = "Notificações",
                navigationIcon = Icons.Filled.ArrowBack,
                onNavigationClick = onBack
            )
        }
    ) { contentPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .padding(16.dp)
        ) {
            Text("Configurações de Notificações (Em construção)", style = MaterialTheme.typography.bodyLarge)
        }
    }
}
