package com.maskan.mobileapp.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.maskan.mobileapp.ui.theme.MaskanTheme
import com.maskan.mobileapp.ui.theme.MaskanType

/** Shown when a "Call" icon is tapped and WhatsApp is installed on the device — lets the user pick which channel to contact through. */
@Composable
fun CallChooserDialog(onDismiss: () -> Unit, onCall: () -> Unit, onWhatsApp: () -> Unit) {
    val colors = MaskanTheme.colors
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Contact") },
        text = {
            Column {
                ChooserRow(icon = Icons.Filled.Call, label = "Call", onClick = { onDismiss(); onCall() })
                ChooserRow(icon = Icons.Filled.Chat, label = "Message on WhatsApp", onClick = { onDismiss(); onWhatsApp() })
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = colors.textSecondary) } },
    )
}

@Composable
private fun ChooserRow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    val colors = MaskanTheme.colors
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Icon(icon, contentDescription = null, tint = colors.gradientStart)
        Text(text = label, style = MaskanType.body, color = colors.textPrimary)
    }
}
