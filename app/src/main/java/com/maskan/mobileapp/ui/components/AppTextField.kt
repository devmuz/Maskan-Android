package com.maskan.mobileapp.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.maskan.mobileapp.ui.theme.MaskanDimens
import com.maskan.mobileapp.ui.theme.MaskanTheme
import com.maskan.mobileapp.ui.theme.MaskanType

/** "AppTextField": labeled field with fieldBackground fill and an animated focus border. */
@Composable
fun AppTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    isPassword: Boolean = false,
    singleLine: Boolean = true,
    minLines: Int = 1,
    enabled: Boolean = true,
    isError: Boolean = false,
) {
    val colors = MaskanTheme.colors
    var isFocused by remember { mutableStateOf(false) }
    val borderColor by animateColorAsState(
        targetValue = when {
            isError -> colors.danger
            isFocused -> colors.gradientStart
            else -> colors.border
        },
        label = "fieldBorder",
    )
    val borderWidth = if (isFocused || isError) 1.5.dp else 1.dp

    Column(modifier = modifier.fillMaxWidth()) {
        Text(text = label, style = MaskanType.fieldLabel, color = colors.textSecondary)
        Box(
            modifier = Modifier
                .padding(top = 6.dp)
                .fillMaxWidth()
                .let { if (singleLine) it.height(MaskanDimens.fieldHeight) else it }
                .background(colors.fieldBackground, RoundedCornerShape(MaskanDimens.cornerRadius))
                .border(BorderStroke(borderWidth, borderColor), RoundedCornerShape(MaskanDimens.cornerRadius))
                .padding(horizontal = 16.dp),
            contentAlignment = if (singleLine) Alignment.CenterStart else Alignment.TopStart,
        ) {
            if (value.isEmpty() && placeholder != null) {
                Text(
                    text = placeholder,
                    style = MaskanType.body,
                    color = colors.textTertiary,
                    modifier = Modifier.padding(vertical = if (singleLine) 0.dp else 14.dp),
                )
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = if (singleLine) 0.dp else 14.dp)
                    .onFocusChanged { isFocused = it.isFocused },
                textStyle = MaskanType.body.copy(color = colors.textPrimary),
                cursorBrush = SolidColor(colors.gradientStart),
                keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                visualTransformation = if (isPassword) androidx.compose.ui.text.input.PasswordVisualTransformation() else VisualTransformation.None,
                singleLine = singleLine,
                minLines = minLines,
                enabled = enabled,
                interactionSource = remember { MutableInteractionSource() },
            )
        }
    }
}
