package com.phdev.quantofalta.core.designsystem.components.edit

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.phdev.quantofalta.core.designsystem.components.AppTextField
import com.phdev.quantofalta.core.designsystem.components.getIconByName
import com.phdev.quantofalta.core.designsystem.theme.AppSpacing
import com.phdev.quantofalta.core.designsystem.theme.AppTypography

@Composable
fun EditFieldRow(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String = "",
    iconName: String,
    onClick: (() -> Unit)? = null,
    isError: Boolean = false,
    errorMessage: String? = null
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = label.uppercase(),
            style = AppTypography.labelLarge.copy(fontWeight = FontWeight.Bold),
            color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
        AppTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = placeholder,
            icon = getIconByName(iconName),
            onClick = onClick,
            isError = isError
        )
        if (isError && errorMessage != null) {
            Text(
                text = errorMessage,
                style = AppTypography.labelMedium,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
fun EditToggleRow(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    isPremiumLocked: Boolean = false,
    onPremiumLockedClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { 
                if (isPremiumLocked) {
                    onPremiumLockedClick()
                } else {
                    onCheckedChange(!checked)
                }
            }
            .padding(vertical = AppSpacing.small),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = AppTypography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = description,
                style = AppTypography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (isPremiumLocked) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "⭐ Exclusivo Premium", 
                    color = Color(0xFFD4AF37), 
                    style = AppTypography.labelSmall, 
                    fontWeight = FontWeight.Bold
                )
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = { 
                if (isPremiumLocked) {
                    onPremiumLockedClick()
                } else {
                    onCheckedChange(it) 
                }
            }
        )
    }
}
