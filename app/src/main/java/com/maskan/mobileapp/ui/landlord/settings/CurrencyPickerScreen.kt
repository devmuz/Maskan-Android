package com.maskan.mobileapp.ui.landlord.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.maskan.mobileapp.data.model.Currencies
import com.maskan.mobileapp.data.model.CurrencyOption
import com.maskan.mobileapp.ui.landlord.LandlordViewModel
import com.maskan.mobileapp.ui.theme.MaskanDimens
import com.maskan.mobileapp.ui.theme.MaskanTheme
import com.maskan.mobileapp.ui.theme.MaskanType
import kotlinx.coroutines.launch

/**
 * Full picker screen (08-landlord-settings.md). Changing currency does not
 * retroactively rewrite existing properties' `currency` field — new
 * properties get the new code, existing ones keep whatever they were
 * stamped with at creation.
 */
@Composable
fun CurrencyPickerScreen(viewModel: LandlordViewModel, onBack: () -> Unit) {
    val colors = MaskanTheme.colors
    val landlord by viewModel.landlord.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    Column(modifier = Modifier.fillMaxSize().background(colors.background).statusBarsPadding()) {
        Row(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
            IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = colors.textPrimary) }
        }
        Text(
            text = "Currency",
            style = MaskanType.screenTitle,
            color = colors.textPrimary,
            modifier = Modifier.padding(horizontal = MaskanDimens.screenHPadding, vertical = 8.dp),
        )

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(Currencies.all, key = { it.code }) { option ->
                CurrencyRow(
                    option = option,
                    selected = option.code == landlord?.currencyCode,
                    onClick = {
                        val id = viewModel.landlordUid
                        scope.launch { viewModel.landlordRepository.setCurrencyCode(id, option.code) }
                    },
                )
            }
        }
    }
}

@Composable
private fun CurrencyRow(option: CurrencyOption, selected: Boolean, onClick: () -> Unit) {
    val colors = MaskanTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = MaskanDimens.screenHPadding, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(text = option.symbol, style = MaskanType.cardTitle, color = colors.gradientStart)
            Column {
                Text(text = option.displayName, style = MaskanType.bodyMedium, color = colors.textPrimary)
                Text(text = option.code, style = MaskanType.secondary, color = colors.textSecondary)
            }
        }
        if (selected) {
            Icon(Icons.Filled.Check, contentDescription = "Selected", tint = colors.gradientStart)
        }
    }
}
