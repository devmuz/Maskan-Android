package com.maskan.mobileapp.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.maskan.mobileapp.R
import com.maskan.mobileapp.ui.theme.MaskanTheme

/**
 * Tablet Settings (iOS LandlordProfileView/TenantProfileView iPad layout): an
 * account card on the left with the actions under it, the settings sections
 * in two columns on the right, capped at 1180dp and centred.
 */
@Composable
fun SettingsTabletLayout(
    account: @Composable ColumnScope.() -> Unit,
    accountFooter: @Composable ColumnScope.() -> Unit,
    leading: @Composable ColumnScope.() -> Unit,
    trailing: @Composable ColumnScope.() -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentWidth(Alignment.CenterHorizontally)
            .widthIn(max = 1180.dp),
        horizontalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        Column(modifier = Modifier.width(340.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            MaskanCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(top = 24.dp, bottom = 24.dp)) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    content = account,
                )
            }
            accountFooter()
        }
        AdaptiveColumns(modifier = Modifier.weight(1f), leading = leading, trailing = trailing)
    }
}

/**
 * Tablet Settings backdrop: a city skyline along the bottom of the screen in
 * the brand gradient, behind the sections, so the space under them isn't empty.
 */
@Composable
fun SettingsSkyline(modifier: Modifier = Modifier) {
    val gradient = MaskanTheme.colors.primaryGradient
    Image(
        painter = painterResource(R.drawable.settings_skyline),
        contentDescription = null,
        contentScale = ContentScale.FillWidth,
        modifier = modifier
            .fillMaxWidth()
            // Half strength, so it sits back behind the sections.
            .graphicsLayer(alpha = 0.5f, compositingStrategy = CompositingStrategy.Offscreen)
            .drawWithContent {
                drawContent()
                // Recolour the black silhouette with the gradient, keeping its shape.
                drawRect(gradient, blendMode = BlendMode.SrcIn)
            },
    )
}
