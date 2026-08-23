package com.maskan.mobileapp.ui.landlord.properties

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.maskan.mobileapp.ui.components.GradientButton
import com.maskan.mobileapp.ui.components.MaskanCard
import com.maskan.mobileapp.ui.components.StatusBadge
import com.maskan.mobileapp.ui.landlord.LandlordViewModel
import com.maskan.mobileapp.ui.theme.MaskanDimens
import com.maskan.mobileapp.ui.theme.MaskanTheme
import com.maskan.mobileapp.ui.theme.MaskanType
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

@Composable
fun PropertyDetailScreen(viewModel: LandlordViewModel, propertyId: String, onBack: () -> Unit, onEdit: (String) -> Unit) {
    val colors = MaskanTheme.colors
    val properties by viewModel.properties.collectAsStateWithLifecycle()
    val property = properties.find { it.id == propertyId }
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    var copied by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var deleteError by remember { mutableStateOf<String?>(null) }
    val numberFormat = remember { NumberFormat.getNumberInstance(Locale.getDefault()) }

    if (property == null) {
        Box(modifier = Modifier.fillMaxSize().background(colors.background), contentAlignment = Alignment.Center) {
            Text(text = "Property not found", style = MaskanType.body, color = colors.textSecondary)
        }
        return
    }

    Column(modifier = Modifier.fillMaxSize().background(colors.background).statusBarsPadding()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = colors.textPrimary) }
            IconButton(onClick = { onEdit(property.id) }) { Icon(Icons.Filled.Edit, contentDescription = "Edit", tint = colors.textPrimary) }
        }

        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = MaskanDimens.screenHPadding),
            verticalArrangement = Arrangement.spacedBy(MaskanDimens.sectionSpacing),
        ) {
            if (!property.photoUrl.isNullOrBlank()) {
                AsyncImage(
                    model = property.photoUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxWidth().height(180.dp).clip(RoundedCornerShape(MaskanDimens.heroCornerRadius)),
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                )
            }

            Column {
                Text(text = property.displayBuildingName, style = MaskanType.sectionTitle, color = colors.textPrimary)
                if (property.unit.isNotBlank()) {
                    Text(text = property.unit, style = MaskanType.body, color = colors.textSecondary, modifier = Modifier.padding(top = 2.dp))
                }
                Row(modifier = Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    property.propertyType?.let { Text(text = it.label, style = MaskanType.bodyMedium, color = colors.gradientStart) }
                    StatusBadge(text = if (property.occupied) "Occupied" else "Vacant", color = if (property.occupied) colors.success else colors.warning)
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.primaryGradient, RoundedCornerShape(MaskanDimens.cornerRadius))
                    .padding(MaskanDimens.cardPadding),
            ) {
                Text(text = "PROPERTY ID", style = MaskanType.overline, color = Color.White.copy(alpha = 0.8f))
                Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(text = property.propertyIdCode, style = MaskanType.sectionTitle, color = Color.White)
                    Box(
                        modifier = Modifier
                            .background(Color.White.copy(alpha = 0.18f), RoundedCornerShape(50))
                            .clickable {
                                clipboard.setText(AnnotatedString(property.propertyIdCode))
                                copied = true
                                scope.launch { delay(2000); copied = false }
                            }
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                    ) {
                        Text(text = if (copied) "Copied" else "Copy", style = MaskanType.bodyMedium, color = Color.White)
                    }
                }
            }

            MaskanCard(modifier = Modifier.fillMaxWidth()) {
                DetailRow("Address", property.address)
                DetailRow("Monthly rent", "${numberFormat.format(property.monthlyRent)} / month")
                property.electricityAccountNumber?.takeIf { it.isNotBlank() }?.let { DetailRow("Electricity account", it) }
                property.houseTaxNumber?.takeIf { it.isNotBlank() }?.let { DetailRow("House tax number", it) }
                property.waterTaxNumber?.takeIf { it.isNotBlank() }?.let { DetailRow("Water tax number", it) }
            }

            Column(modifier = Modifier.padding(bottom = 24.dp)) {
                GradientButton(
                    text = "Delete Property",
                    onClick = { showDeleteConfirm = true },
                    enabled = !property.occupied,
                    brush = androidx.compose.ui.graphics.SolidColor(colors.danger.copy(alpha = if (property.occupied) 0.3f else 1f)),
                    contentColor = Color.White,
                )
                if (property.occupied) {
                    Text(
                        text = "Occupied properties can't be deleted — move out or remove the tenant first.",
                        style = MaskanType.secondary,
                        color = colors.textSecondary,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
                deleteError?.let {
                    Text(text = it, style = MaskanType.secondary, color = colors.danger, modifier = Modifier.padding(top = 8.dp))
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete this property?") },
            text = { Text("This permanently removes the property and frees its Property ID for reuse. Bills, payments, and old tenant history are kept.") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    scope.launch {
                        try {
                            viewModel.propertyRepository.deleteProperty(property)
                            onBack()
                        } catch (t: Throwable) {
                            deleteError = t.message ?: "Couldn't delete this property."
                        }
                    }
                }) { Text("Delete", color = colors.danger) }
            },
            dismissButton = { TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    val colors = MaskanTheme.colors
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(text = label, style = MaskanType.body, color = colors.textSecondary)
        Text(text = value, style = MaskanType.bodyMedium, color = colors.textPrimary)
    }
}
