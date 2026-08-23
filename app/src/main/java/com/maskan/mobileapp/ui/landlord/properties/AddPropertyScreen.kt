package com.maskan.mobileapp.ui.landlord.properties

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.maskan.mobileapp.data.model.PropertyType
import com.maskan.mobileapp.data.repository.NewFlatInput
import com.maskan.mobileapp.data.repository.NewPropertyInput
import com.maskan.mobileapp.data.util.PropertyIdCodeGenerator
import com.maskan.mobileapp.ui.components.AppTextField
import com.maskan.mobileapp.ui.components.GradientButton
import com.maskan.mobileapp.ui.landlord.LandlordViewModel
import com.maskan.mobileapp.ui.theme.MaskanDimens
import com.maskan.mobileapp.ui.theme.MaskanTheme
import com.maskan.mobileapp.ui.theme.MaskanType
import kotlinx.coroutines.launch

private class FlatFormState {
    val session = PropertyIdCodeGenerator.Session()
    var unitName by mutableStateOf("")
    var monthlyRent by mutableStateOf("")
    var manualCode by mutableStateOf<String?>(null)
}

private fun resolvedCode(flat: FlatFormState, buildingName: String, address: String): String =
    flat.manualCode ?: flat.session.derive(buildingName + address)

@Composable
fun AddPropertyScreen(viewModel: LandlordViewModel, onDone: () -> Unit, onCancel: () -> Unit) {
    val colors = MaskanTheme.colors
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var propertyType by remember { mutableStateOf(PropertyType.FLAT) }
    var buildingName by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    val flats = remember { mutableStateListOf(FlatFormState()) }
    var electricity by remember { mutableStateOf("") }
    var houseTax by remember { mutableStateOf("") }
    var waterTax by remember { mutableStateOf("") }
    var photoUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var isSaving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri -> photoUri = uri }

    val isMultiUnit = propertyType.supportsMultipleUnits

    fun isValid(): Boolean {
        if (buildingName.isBlank() || address.isBlank()) return false
        val codes = flats.map { resolvedCode(it, buildingName, address).uppercase() }
        if (codes.toSet().size != codes.size) return false
        for (flat in flats) {
            val rent = flat.monthlyRent.toDoubleOrNull() ?: return false
            if (rent <= 0) return false
            if (isMultiUnit && flats.size > 1 && flat.unitName.isBlank()) return false
        }
        if (codes.any { it.length < 4 }) return false
        return true
    }

    Column(modifier = Modifier.fillMaxSize().background(colors.background).statusBarsPadding()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = "Cancel", style = MaskanType.body, color = colors.gradientStart, modifier = Modifier.clickable(onClick = onCancel))
            Text(text = "Add Property", style = MaskanType.cardTitle, color = colors.textPrimary)
            Box(modifier = Modifier.width(56.dp))
        }

        Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = MaskanDimens.screenHPadding),
            verticalArrangement = Arrangement.spacedBy(MaskanDimens.sectionSpacing),
        ) {
            Text(
                text = "Each flat gets its own permanent Property ID when you save — you'll share it with that flat's tenant for login.",
                style = MaskanType.secondary,
                color = colors.textSecondary,
            )

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(text = "PROPERTY TYPE", style = MaskanType.overline, color = colors.textTertiary)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PropertyType.entries.forEach { type ->
                        TypeChip(
                            label = type.label,
                            selected = type == propertyType,
                            onClick = {
                                propertyType = type
                                if (!type.supportsMultipleUnits && flats.size > 1) {
                                    flats.retainAll(listOf(flats.first()))
                                }
                            },
                        )
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(text = if (isMultiUnit) "BUILDING" else "PROPERTY", style = MaskanType.overline, color = colors.textTertiary)
                AppTextField(
                    value = buildingName,
                    onValueChange = { buildingName = it },
                    label = if (isMultiUnit) "Building name" else "Property name",
                    placeholder = "e.g. Willow Apartments",
                )
                AppTextField(value = address, onValueChange = { address = it }, label = "Address", placeholder = "Street, area, city")
            }

            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(text = if (isMultiUnit) "FLATS / UNITS" else "DETAILS", style = MaskanType.overline, color = colors.textTertiary)
                flats.forEachIndexed { index, flat ->
                    FlatEntryCard(
                        index = index,
                        flat = flat,
                        showUnitName = isMultiUnit,
                        requireUnitName = isMultiUnit && flats.size > 1,
                        code = resolvedCode(flat, buildingName, address),
                        onRemove = if (isMultiUnit && flats.size > 1) ({ flats.removeAt(index) }) else null,
                    )
                }
                if (isMultiUnit) {
                    Text(
                        text = "+ Add another flat",
                        style = MaskanType.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = colors.gradientStart,
                        modifier = Modifier.clickable { flats.add(FlatFormState()) },
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(text = "OPTIONAL DETAILS", style = MaskanType.overline, color = colors.textTertiary)
                AppTextField(value = electricity, onValueChange = { electricity = it }, label = "Electricity account number", placeholder = "e.g. 100234567890", keyboardType = KeyboardType.Number)
                AppTextField(value = houseTax, onValueChange = { houseTax = it }, label = "House tax number", placeholder = "e.g. HT-889021")
                AppTextField(value = waterTax, onValueChange = { waterTax = it }, label = "Water tax number", placeholder = "e.g. WT-102938")

                Text(text = "Photo (optional)", style = MaskanType.fieldLabel, color = colors.textSecondary)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(2.2f)
                        .clip(RoundedCornerShape(MaskanDimens.cornerRadius))
                        .background(colors.fieldBackground)
                        .clickable { photoPicker.launch("image/*") },
                    contentAlignment = Alignment.Center,
                ) {
                    if (photoUri != null) {
                        AsyncImage(model = photoUri, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = androidx.compose.ui.layout.ContentScale.Crop)
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Filled.AddPhotoAlternate, contentDescription = null, tint = colors.gradientStart)
                            Text(text = "Add a photo of the building", style = MaskanType.secondary, color = colors.textSecondary)
                        }
                    }
                }
            }

            error?.let { Text(text = it, style = MaskanType.secondary, color = colors.danger) }

            GradientButton(
                text = "Add Property",
                isLoading = isSaving,
                loadingText = "Adding…",
                enabled = isValid(),
                modifier = Modifier.padding(bottom = 32.dp),
                onClick = {
                    isSaving = true
                    error = null
                    scope.launch {
                        try {
                            for (flat in flats) {
                                val code = resolvedCode(flat, buildingName, address).uppercase()
                                if (!viewModel.propertyRepository.isCodeAvailable(code)) {
                                    error = "Property ID \"$code\" is already in use. Please choose another."
                                    isSaving = false
                                    return@launch
                                }
                            }
                            val input = NewPropertyInput(
                                propertyType = propertyType,
                                buildingName = buildingName,
                                address = address,
                                flats = flats.map { flat ->
                                    NewFlatInput(
                                        unitName = if (isMultiUnit) flat.unitName else "",
                                        monthlyRent = flat.monthlyRent.toDoubleOrNull() ?: 0.0,
                                        propertyIdCode = resolvedCode(flat, buildingName, address).uppercase(),
                                    )
                                },
                                electricityAccountNumber = electricity.ifBlank { null },
                                houseTaxNumber = houseTax.ifBlank { null },
                                waterTaxNumber = waterTax.ifBlank { null },
                            )
                            val createdIds = viewModel.propertyRepository.addProperty(viewModel.landlordUid, input)
                            onDone()

                            val uri = photoUri
                            if (uri != null) {
                                viewModel.markUploadingPhoto(createdIds)
                                scope.launch {
                                    try {
                                        val bytes = compressImageForUpload(context, uri)
                                        if (bytes != null) {
                                            viewModel.propertyRepository.uploadPhoto(viewModel.landlordUid, createdIds, bytes)
                                        }
                                    } finally {
                                        viewModel.clearUploadingPhoto(createdIds)
                                    }
                                }
                            }
                        } catch (t: Throwable) {
                            error = t.message ?: "Couldn't save this property."
                        } finally {
                            isSaving = false
                        }
                    }
                },
            )
        }
    }
}

@Composable
private fun TypeChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val colors = MaskanTheme.colors
    val brush = if (selected) colors.primaryGradient else androidx.compose.ui.graphics.SolidColor(colors.fieldBackground)
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(brush)
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 10.dp),
    ) {
        Text(text = label, style = MaskanType.bodyMedium, color = if (selected) androidx.compose.ui.graphics.Color.White else colors.textSecondary)
    }
}

@Composable
private fun FlatEntryCard(
    index: Int,
    flat: FlatFormState,
    showUnitName: Boolean,
    requireUnitName: Boolean,
    code: String,
    onRemove: (() -> Unit)?,
) {
    val colors = MaskanTheme.colors
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(MaskanDimens.cornerRadius))
            .background(colors.fieldBackground)
            .padding(MaskanDimens.cardPadding),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(text = "Flat ${index + 1}", style = MaskanType.cardTitle, color = colors.textPrimary)
            if (onRemove != null) {
                Text(text = "Remove", style = MaskanType.secondary, color = colors.danger, modifier = Modifier.clickable(onClick = onRemove))
            }
        }
        if (showUnitName) {
            AppTextField(
                value = flat.unitName,
                onValueChange = { flat.unitName = it },
                label = if (requireUnitName) "Unit name" else "Unit name (optional)",
                placeholder = "e.g. Flat 2B",
            )
        }
        AppTextField(
            value = flat.monthlyRent,
            onValueChange = { flat.monthlyRent = it.filter { c -> c.isDigit() || c == '.' } },
            label = "Monthly rent",
            placeholder = "e.g. 2500",
            keyboardType = KeyboardType.Number,
        )
        Column {
            Text(text = "Property ID (tenant login code)", style = MaskanType.fieldLabel, color = colors.textSecondary)
            AppTextField(
                value = flat.manualCode ?: code,
                onValueChange = { flat.manualCode = it.uppercase().filter { c -> c.isLetterOrDigit() } },
                label = "",
                modifier = Modifier.padding(top = 6.dp),
            )
            if (flat.manualCode != null) {
                Text(
                    text = "Reset to suggested",
                    style = MaskanType.secondary,
                    color = colors.gradientStart,
                    modifier = Modifier.padding(top = 4.dp).clickable { flat.manualCode = null },
                )
            }
        }
    }
}
