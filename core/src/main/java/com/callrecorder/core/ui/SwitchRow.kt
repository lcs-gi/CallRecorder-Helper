package com.callrecorder.core.ui

import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchColors
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role

/**
 * Interrupteur contrasté : désactivé, il reste net (pouce et bord foncés) au lieu de paraître grisé
 * comme avec les couleurs Material par défaut, qui reprennent `outline`, très clair ici.
 */
@Composable
fun appSwitchColors(): SwitchColors = SwitchDefaults.colors(
    uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
    uncheckedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant,
    uncheckedTrackColor = MaterialTheme.colorScheme.surfaceContainerHigh,
)

/** Ligne de réglage avec interrupteur : toute la ligne bascule le réglage. */
@Composable
fun SwitchRow(title: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit, hint: String? = null) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = hint?.let { { Text(it) } },
        trailingContent = { Switch(checked = checked, onCheckedChange = null, colors = appSwitchColors()) },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier.toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange),
    )
}
