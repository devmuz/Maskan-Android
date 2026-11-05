package com.maskan.mobileapp.ui.landlord.properties

import android.net.Uri
import androidx.compose.foundation.layout.imePadding
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material3.Icon
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.maskan.mobileapp.data.repository.PropertyRepository
import com.maskan.mobileapp.data.repository.SharedBuildingEdits
import com.maskan.mobileapp.ui.components.AppTextField
import com.maskan.mobileapp.ui.components.GradientButton
import com.maskan.mobileapp.ui.components.PropertyPhoto
import com.maskan.mobileapp.ui.landlord.LandlordViewModel
import com.maskan.mobileapp.ui.theme.MaskanDimens
import com.maskan.mobileapp.ui.theme.MaskanTheme
import com.maskan.mobileapp.ui.theme.MaskanType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Building-level fields apply to every flat sharing the building; unit name
 * and rent only update this one flat (05-landlord-properties.md). Type and
 * Property ID aren't editable after creation.
 */
@Composable
fun EditPropertyScreen(viewModel: LandlordViewModel, propertyId: String, onDone: () -> Unit, onCancel: () -> Unit) {
    val colors = MaskanTheme.colors
    val scope = rememberCoroutineScope()
    val properties by viewModel.properties.collectAsStateWithLifecycle()
    val property = properties.find { it.id == propertyId }

    var buildingName by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var unit by remember { mutableStateOf("") }
    var rent by remember { mutableStateOf("") }
    var electricity by remember { mutableStateOf("") }
    var houseTax by remember { mutableStateOf("") }
    var waterTax by remember { mutableStateOf("") }
    var isSaving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var initialized by remember { mutableStateOf(false) }
    val context = LocalContext.current
    var newPhotoUri by remember { mutableStateOf<Uri?>(null) }
    var photoRemoved by remember { mutableStateOf(false) }
    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            newPhotoUri = uri
            photoRemoved = false
        }
    }

    LaunchedEffect(property?.id) {
        if (property != null && !initialized) {
            buildingName = property.displayBuildingName
            address = property.address
            unit = property.unit
            rent = if (property.monthlyRent > 0) property.monthlyRent.toString() else ""
            electricity = property.electricityAccountNumber.orEmpty()
            houseTax = property.houseTaxNumber.orEmpty()
            waterTax = property.waterTaxNumber.orEmpty()
            initialized = true
        }
    }

    if (property == null) {
        Box(modifier = Modifier.fillMaxSize().background(colors.background), contentAlignment = Alignment.Center) {
            Text(text = "Property not found", style = MaskanType.body, color = colors.textSecondary)
        }
        return
    }

    val siblingIds = remember(properties, property.groupKey) {
        properties.filter { it.groupKey == property.groupKey && it.id != property.id }.map { it.id }
    }

    Column(modifier = Modifier.fillMaxSize().background(colors.background).statusBarsPadding().navigationBarsPadding().imePadding()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = "Cancel", style = MaskanType.body, color = colors.gradientStart, modifier = Modifier.clickable(onClick = onCancel))
            Text(text = "Edit Property", style = MaskanType.cardTitle, color = colors.textPrimary)
            Box(modifier = Modifier.width(56.dp))
        }

        Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = MaskanDimens.screenHPadding),
            verticalArrangement = Arrangement.spacedBy(MaskanDimens.sectionSpacing),
        ) {
            if (property.propertyType?.supportsMultipleUnits == true) {
                Text(
                    text = "Building details apply to every flat in this building; unit name and rent apply to this flat only.",
                    style = MaskanType.secondary,
                    color = colors.textSecondary,
                )
            }
            val currentPhotoUrl = property.photoUrl?.takeIf { it.isNotBlank() && !photoRemoved }
            val hasPhoto = newPhotoUri != null || currentPhotoUrl != null
            Column {
                Text(
                    text = if (property.propertyType?.supportsMultipleUnits == true) "Photo (shown for every flat)" else "Photo (optional)",
                    style = MaskanType.fieldLabel,
                    color = colors.textSecondary,
                    modifier = Modifier.padding(bottom = 6.dp),
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(2.2f)
                        .clip(RoundedCornerShape(MaskanDimens.cornerRadius))
                        .background(colors.fieldBackground)
                        .clickable(enabled = !isSaving) { photoPicker.launch("image/*") },
                    contentAlignment = Alignment.Center,
                ) {
                    when {
                        newPhotoUri != null -> AsyncImage(model = newPhotoUri, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                        currentPhotoUrl != null -> PropertyPhoto(url = currentPhotoUrl, modifier = Modifier.fillMaxSize())
                        else -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Filled.AddPhotoAlternate, contentDescription = null, tint = colors.gradientStart)
                            Text(text = "Add a photo of the building", style = MaskanType.secondary, color = colors.textSecondary)
                        }
                    }
                }
                if (hasPhoto) {
                    Row(modifier = Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                        Text(
                            text = "Change photo",
                            style = MaskanType.bodyMedium,
                            color = colors.gradientStart,
                            modifier = Modifier.clickable(enabled = !isSaving) { photoPicker.launch("image/*") },
                        )
                        Text(
                            text = "Remove photo",
                            style = MaskanType.bodyMedium,
                            color = colors.danger,
                            modifier = Modifier.clickable(enabled = !isSaving) {
                                newPhotoUri = null
                                photoRemoved = property.photoUrl != null
                            },
                        )
                    }
                }
            }

            AppTextField(value = buildingName, onValueChange = { buildingName = it }, label = if (property.propertyType?.supportsMultipleUnits == true) "Building name" else "Property name", placeholder = "e.g. Willow Apartments")
            AppTextField(value = address, onValueChange = { address = it }, label = "Address", placeholder = "Street, area, city")
            if (property.propertyType?.supportsMultipleUnits == true) {
                AppTextField(value = unit, onValueChange = { unit = it }, label = "Unit name", placeholder = "e.g. Flat 2B")
            }
            AppTextField(value = rent, onValueChange = { rent = it.filter { c -> c.isDigit() || c == '.' } }, label = "Monthly rent", placeholder = "e.g. 2500", keyboardType = KeyboardType.Number)
            AppTextField(value = electricity, onValueChange = { electricity = it }, label = "Electricity account number", placeholder = "e.g. 100234567890", keyboardType = KeyboardType.Number)
            AppTextField(value = houseTax, onValueChange = { houseTax = it }, label = "House tax number", placeholder = "e.g. HT-889021")
            AppTextField(value = waterTax, onValueChange = { waterTax = it }, label = "Water tax number", placeholder = "e.g. WT-102938")

            error?.let { Text(text = it, style = MaskanType.secondary, color = colors.danger) }

            GradientButton(
                text = "Save Changes",
                isLoading = isSaving,
                loadingText = "Saving…",
                enabled = buildingName.isNotBlank() && address.isNotBlank() && (rent.toDoubleOrNull() ?: 0.0) > 0,
                modifier = Modifier.padding(bottom = 32.dp),
                onClick = {
                    isSaving = true
                    error = null
                    scope.launch {
                        try {
                            val pickedUri = newPhotoUri
                            val photoEdit = when {
                                pickedUri != null -> PropertyRepository.PhotoEdit.Replace(
                                    withContext(Dispatchers.IO) { compressImageForUpload(context, pickedUri) }
                                        ?: error("Couldn't read the selected photo."),
                                )
                                photoRemoved -> PropertyRepository.PhotoEdit.Remove
                                else -> PropertyRepository.PhotoEdit.Keep
                            }
                            viewModel.propertyRepository.editProperty(
                                property = property,
                                siblingIds = siblingIds,
                                shared = SharedBuildingEdits(
                                    buildingName = buildingName,
                                    address = address,
                                    electricityAccountNumber = electricity.ifBlank { null },
                                    houseTaxNumber = houseTax.ifBlank { null },
                                    waterTaxNumber = waterTax.ifBlank { null },
                                ),
                                unit = unit,
                                monthlyRent = rent.toDoubleOrNull() ?: property.monthlyRent,
                                photo = photoEdit,
                            )
                            onDone()
                        } catch (t: Throwable) {
                            error = t.message ?: "Couldn't save changes."
                        } finally {
                            isSaving = false
                        }
                    }
                },
            )
        }
    }
}
