package com.phdev.quantofalta.core.designsystem.components

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import com.phdev.quantofalta.core.designsystem.theme.AppTypography

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTopBar(
    title: String,
    modifier: Modifier = Modifier,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    centerTitle: Boolean = false,
    containerColor: Color = MaterialTheme.colorScheme.background
) {
    if (centerTitle) {
        CenterAlignedTopAppBar(
            title = {
                Text(
                    text = title,
                    style = AppTypography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (containerColor == Color.Transparent) Color.White else MaterialTheme.colorScheme.onBackground
                )
            },
            navigationIcon = navigationIcon,
            actions = actions,
            modifier = modifier,
            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                containerColor = containerColor,
                scrolledContainerColor = containerColor,
                navigationIconContentColor = if (containerColor == Color.Transparent) Color.White else MaterialTheme.colorScheme.onBackground,
                actionIconContentColor = if (containerColor == Color.Transparent) Color.White else MaterialTheme.colorScheme.onBackground
            )
        )
    } else {
        TopAppBar(
            title = {
                Text(
                    text = title,
                    style = AppTypography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (containerColor == Color.Transparent) Color.White else MaterialTheme.colorScheme.onBackground
                )
            },
            navigationIcon = navigationIcon,
            actions = actions,
            modifier = modifier,
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = containerColor,
                scrolledContainerColor = containerColor,
                navigationIconContentColor = if (containerColor == Color.Transparent) Color.White else MaterialTheme.colorScheme.onBackground,
                actionIconContentColor = if (containerColor == Color.Transparent) Color.White else MaterialTheme.colorScheme.onBackground
            )
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTopBar(
    title: String,
    modifier: Modifier = Modifier,
    navigationIcon: ImageVector? = null,
    onNavigationClick: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    centerTitle: Boolean = false,
    containerColor: Color = MaterialTheme.colorScheme.background
) {
    AppTopBar(
        title = title,
        modifier = modifier,
        navigationIcon = {
            if (navigationIcon != null && onNavigationClick != null) {
                androidx.compose.material3.IconButton(onClick = onNavigationClick) {
                    androidx.compose.material3.Icon(
                        imageVector = navigationIcon,
                        contentDescription = "Voltar",
                        tint = if (containerColor == Color.Transparent) Color.White else MaterialTheme.colorScheme.onBackground
                    )
                }
            }
        },
        actions = actions,
        centerTitle = centerTitle,
        containerColor = containerColor
    )
}

