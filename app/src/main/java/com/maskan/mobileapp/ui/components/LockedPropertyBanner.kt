package com.maskan.mobileapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.maskan.mobileapp.ui.theme.MaskanDimens
import com.maskan.mobileapp.ui.theme.MaskanTheme
import com.maskan.mobileapp.ui.theme.MaskanType

/**
 * Shown on a property/building that's over the free-plan limit (mirrors iOS
 * LockedPropertyBanner). It can still be viewed, and the property or its
 * tenant can still be removed — everything else needs an active Pro
 * subscription. Tapping opens the paywall.
 */
@Composable
fun LockedPropertyBanner(onUpgrade: () -> Unit, modifier: Modifier = Modifier) {
    val colors = MaskanTheme.colors
    val shape = RoundedCornerShape(MaskanDimens.cornerRadius)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.warning.copy(alpha = 0.1f), shape)
            .clickable(onClick = onUpgrade)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Filled.Lock, contentDescription = null, tint = colors.warning, modifier = Modifier.size(18.dp))
        Column(modifier = Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Text(
                text = "Locked on the free plan",
                style = MaskanType.secondary.copy(fontWeight = FontWeight.SemiBold),
                color = colors.textPrimary,
            )
            Text(
                text = "You can view this property and remove it or its tenant, but editing needs Pro.",
                style = MaskanType.caption.copy(fontWeight = FontWeight.Normal),
                color = colors.textSecondary,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = colors.textTertiary, modifier = Modifier.size(18.dp))
    }
}
