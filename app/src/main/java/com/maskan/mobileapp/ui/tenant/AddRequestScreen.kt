package com.maskan.mobileapp.ui.tenant

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.maskan.mobileapp.data.model.ServiceRequestCategory
import com.maskan.mobileapp.ui.components.AppTextField
import com.maskan.mobileapp.ui.theme.MaskanDimens
import com.maskan.mobileapp.ui.theme.MaskanTheme
import com.maskan.mobileapp.ui.theme.MaskanType
import kotlinx.coroutines.launch

/** Toolbar "+" on Home/Requests -> here (09-tenant-app.md). */
@Composable
fun AddRequestScreen(viewModel: TenantSessionViewModel, onDone: () -> Unit, onCancel: () -> Unit) {
    val colors = MaskanTheme.colors
    val scope = rememberCoroutineScope()

    var category by remember { mutableStateOf(ServiceRequestCategory.GENERAL) }
    var title by remember { mutableStateOf("") }
    var details by remember { mutableStateOf("") }
    var isSaving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val isValid = title.isNotBlank()

    Column(modifier = Modifier.fillMaxSize().background(colors.background).statusBarsPadding()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp).padding(top = 12.dp, bottom = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onCancel) { Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = colors.textPrimary) }
            Text(text = "New Request", style = MaskanType.cardTitle, color = colors.textPrimary)
            Text(
                text = if (isSaving) "Saving…" else "Submit",
                style = MaskanType.body.copy(fontWeight = FontWeight.SemiBold),
                color = if (isValid && !isSaving) colors.gradientStart else colors.textTertiary,
                modifier = Modifier.clickable(enabled = isValid && !isSaving) {
                    isSaving = true
                    error = null
                    scope.launch {
                        try {
                            viewModel.submitRequest(title.trim(), details.trim(), category)
                            onDone()
                        } catch (t: Throwable) {
                            error = t.message ?: "Couldn't submit this request."
                        } finally {
                            isSaving = false
                        }
                    }
                }.padding(end = 12.dp),
            )
        }

        Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = MaskanDimens.screenHPadding),
            verticalArrangement = Arrangement.spacedBy(MaskanDimens.sectionSpacing),
        ) {
            Column {
                Text(text = "CATEGORY", style = MaskanType.overline, color = colors.textTertiary, modifier = Modifier.padding(bottom = 8.dp))
                ServiceRequestCategory.entries.toList().chunked(3).forEach { row ->
                    Row(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { option ->
                            CategoryChip(
                                category = option,
                                selected = option == category,
                                onClick = { category = option },
                                modifier = Modifier.weight(1f),
                            )
                        }
                        repeat(3 - row.size) { Box(modifier = Modifier.weight(1f)) }
                    }
                }
            }

            AppTextField(value = title, onValueChange = { title = it }, label = "Issue Title", placeholder = "e.g. Leaking kitchen tap")
            AppTextField(
                value = details,
                onValueChange = { details = it },
                label = "Details (optional)",
                placeholder = "Describe the issue…",
                singleLine = false,
                minLines = 4,
            )

            error?.let { Text(text = it, style = MaskanType.secondary, color = colors.danger) }
            Box(modifier = Modifier.padding(bottom = 24.dp))
        }
    }
}

@Composable
private fun CategoryChip(category: ServiceRequestCategory, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = MaskanTheme.colors
    val shape = RoundedCornerShape(MaskanDimens.cornerRadius)
    Column(
        modifier = modifier
            .height(76.dp)
            .clip(shape)
            .let { if (selected) it.background(colors.primaryGradient) else it.background(colors.fieldBackground) }
            .clickable(onClick = onClick)
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = requestCategoryIcon(category),
            contentDescription = null,
            tint = if (selected) Color.White else colors.gradientStart,
        )
        Text(
            text = category.label,
            style = MaskanType.caption,
            color = if (selected) Color.White else colors.textSecondary,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}
