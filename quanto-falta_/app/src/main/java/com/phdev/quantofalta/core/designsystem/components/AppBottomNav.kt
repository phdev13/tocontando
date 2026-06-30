package com.phdev.quantofalta.core.designsystem.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star

import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.phdev.quantofalta.core.navigation.Screen

@Composable
fun AppBottomNav(
    currentRoute: String?,
    onNavigate: (String) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface),
        contentAlignment = Alignment.Center
    ) {
    NavigationBar(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = AppContentMaxWidth),
        tonalElevation = 0.dp,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        val colors = NavigationBarItemDefaults.colors(
            selectedIconColor = MaterialTheme.colorScheme.primary,
            selectedTextColor = MaterialTheme.colorScheme.primary,
            indicatorColor = MaterialTheme.colorScheme.surfaceVariant,
            unselectedIconColor = MaterialTheme.colorScheme.outline,
            unselectedTextColor = MaterialTheme.colorScheme.outline
        )

        NavigationBarItem(
            icon = {
                val selected = currentRoute == Screen.Home.route
                AnimatedNavIcon(
                    iconSelected = Icons.Filled.DateRange,
                    iconUnselected = Icons.Filled.DateRange,
                    selected = selected,
                    contentDescription = "Eventos"
                )
            },
            label = { Text("Eventos") },
            selected = currentRoute == Screen.Home.route,
            onClick = { onNavigate(Screen.Home.route) },
            colors = colors
        )
        NavigationBarItem(
            icon = {
                val selected = currentRoute == Screen.Completed.route
                AnimatedNavIcon(
                    iconSelected = Icons.Filled.CheckCircle,
                    iconUnselected = Icons.Filled.CheckCircle,
                    selected = selected,
                    contentDescription = "Concluídos"
                )
            },
            label = { Text("Concluídos") },
            selected = currentRoute == Screen.Completed.route,
            onClick = { onNavigate(Screen.Completed.route) },
            colors = colors
        )
        NavigationBarItem(
            icon = {
                val selected = currentRoute?.startsWith("highlight") == true
                AnimatedNavIcon(
                    iconSelected = Icons.Filled.Star,
                    iconUnselected = Icons.Filled.Star,
                    selected = selected,
                    contentDescription = "Destaques"
                )
            },
            label = { Text("Destaque") },
            selected = currentRoute?.startsWith("highlight") == true,
            onClick = { onNavigate(Screen.Highlight.createRoute("1")) },
            colors = colors
        )
        NavigationBarItem(
            icon = {
                val selected = currentRoute == Screen.More.route
                AnimatedNavIcon(
                    iconSelected = Icons.Filled.Settings,
                    iconUnselected = Icons.Filled.Settings,
                    selected = selected,
                    contentDescription = "Mais"
                )
            },
            label = { Text("Mais") },
            selected = currentRoute == Screen.More.route,
            onClick = { onNavigate(Screen.More.route) },
            colors = colors
        )
    }
    }
}

@Composable
private fun AnimatedNavIcon(
    iconSelected: ImageVector,
    iconUnselected: ImageVector,
    selected: Boolean,
    contentDescription: String?
) {
    val scale by animateFloatAsState(
        targetValue = if (selected) 1.15f else 1.0f,
        animationSpec = tween(150, easing = FastOutSlowInEasing),
        label = "navIconScale"
    )

    Icon(
        imageVector = if (selected) iconSelected else iconUnselected,
        contentDescription = contentDescription,
        modifier = Modifier.graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
    )
}
