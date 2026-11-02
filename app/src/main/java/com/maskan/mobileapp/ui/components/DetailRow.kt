package com.maskan.mobileapp.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.maskan.mobileapp.ui.theme.MaskanTheme
import com.maskan.mobileapp.ui.theme.MaskanType

/** Values longer than this go on their own line under the label. */
private const val STACK_THRESHOLD = 24

/**
 * Label/value row for detail cards. Short values sit on the right of the
 * label on one line; long ones (property names, addresses, notes) are
 * stacked under the label, left-aligned, so neither side gets squeezed into
 * a narrow column.
 */
@Composable
fun DetailRow(label: String, value: String, modifier: Modifier = Modifier, verticalPadding: Dp = 6.dp) {
    val colors = MaskanTheme.colors
    if (value.length > STACK_THRESHOLD || '\n' in value) {
        Column(modifier = modifier.fillMaxWidth().padding(vertical = verticalPadding)) {
            Text(text = label, style = MaskanType.body, color = colors.textSecondary)
            Text(text = value, style = MaskanType.bodyMedium, color = colors.textPrimary, modifier = Modifier.padding(top = 2.dp))
        }
    } else {
        Row(modifier = modifier.fillMaxWidth().padding(vertical = verticalPadding), verticalAlignment = Alignment.CenterVertically) {
            Text(text = label, style = MaskanType.body, color = colors.textSecondary, maxLines = 1, softWrap = false)
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = value,
                style = MaskanType.bodyMedium,
                color = colors.textPrimary,
                textAlign = TextAlign.End,
                modifier = Modifier.weight(1f),
            )
        }
    }
}
