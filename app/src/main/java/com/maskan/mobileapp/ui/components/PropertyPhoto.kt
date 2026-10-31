package com.maskan.mobileapp.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import com.maskan.mobileapp.ui.theme.MaskanTheme

/**
 * A property/building photo that shows [PropertyPhotoPlaceholder] while it
 * downloads or if it fails, never a blank box (02-data-models.md → "Photo
 * placeholder"). Size and clip it via [modifier].
 */
@Composable
fun PropertyPhoto(url: String, modifier: Modifier = Modifier) {
    SubcomposeAsyncImage(
        model = url,
        contentDescription = null,
        modifier = modifier,
        contentScale = ContentScale.Crop,
        loading = { PropertyPhotoPlaceholder(Modifier.fillMaxSize()) },
        error = { PropertyPhotoPlaceholder(Modifier.fillMaxSize()) },
    )
}

/** Building icon on a soft brand-gradient tint with a gentle pulse. */
@Composable
fun PropertyPhotoPlaceholder(modifier: Modifier = Modifier) {
    val colors = MaskanTheme.colors
    val pulse by rememberInfiniteTransition(label = "photoPlaceholder").animateFloat(
        initialValue = 1f,
        targetValue = 0.55f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "photoPlaceholderAlpha",
    )
    Box(
        modifier = modifier
            .background(colors.fieldBackground)
            .background(
                Brush.linearGradient(
                    listOf(colors.gradientStart.copy(alpha = 0.14f), colors.gradientEnd.copy(alpha = 0.10f)),
                ),
            ),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier.alpha(pulse),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(Icons.Filled.Apartment, contentDescription = null, tint = colors.gradientStart, modifier = Modifier.size(34.dp))
            Icon(Icons.Filled.Image, contentDescription = null, tint = colors.textTertiary, modifier = Modifier.size(14.dp))
        }
    }
}
