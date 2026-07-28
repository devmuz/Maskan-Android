package com.dxbaistudio.maskan.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.dxbaistudio.maskan.ui.theme.MaskanDimens
import com.dxbaistudio.maskan.ui.theme.MaskanTheme

/**
 * "MaskanCard": the single recurring surface treatment used for every card,
 * list-row group, and stat tile in the app (01-design-system.md).
 */
@Composable
fun MaskanCard(
    modifier: Modifier = Modifier,
    subtle: Boolean = false,
    cornerRadius: Dp = MaskanDimens.cornerRadius,
    shadowRadius: Dp = MaskanDimens.cardShadowRadius,
    contentPadding: PaddingValues = PaddingValues(MaskanDimens.cardPadding),
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = MaskanTheme.colors
    val shape = RoundedCornerShape(cornerRadius)
    Surface(
        modifier = modifier.shadow(
            elevation = shadowRadius,
            shape = shape,
            ambientColor = colors.shadowColor,
            spotColor = colors.shadowColor,
        ),
        shape = shape,
        color = if (subtle) colors.fieldBackground else colors.surface,
        border = BorderStroke(1.dp, colors.border),
    ) {
        Column(modifier = Modifier.padding(contentPadding)) {
            content()
        }
    }
}
