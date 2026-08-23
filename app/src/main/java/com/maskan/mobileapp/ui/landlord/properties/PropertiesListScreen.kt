package com.maskan.mobileapp.ui.landlord.properties

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.maskan.mobileapp.data.util.AmountFormatter
import com.maskan.mobileapp.ui.components.EmptyState
import com.maskan.mobileapp.ui.components.MaskanCard
import com.maskan.mobileapp.ui.components.StatusBadge
import com.maskan.mobileapp.ui.landlord.LandlordViewModel
import com.maskan.mobileapp.ui.theme.MaskanDimens
import com.maskan.mobileapp.ui.theme.MaskanTheme
import com.maskan.mobileapp.ui.theme.MaskanType
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PropertiesScreen(viewModel: LandlordViewModel, onPropertyClick: (String) -> Unit, onAddClick: () -> Unit) {
    val colors = MaskanTheme.colors
    val properties by viewModel.properties.collectAsStateWithLifecycle()
    val landlord by viewModel.landlord.collectAsStateWithLifecycle()
    val uploadingIds by viewModel.uploadingPhotoPropertyIds.collectAsStateWithLifecycle()
    val groups = remember(properties) { groupProperties(properties) }

    val scope = rememberCoroutineScope()
    var isRefreshing by remember { mutableStateOf(false) }

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = {
            scope.launch {
                isRefreshing = true
                viewModel.propertyRepository.refresh(viewModel.landlordUid)
                isRefreshing = false
            }
        },
        modifier = Modifier.fillMaxSize().background(colors.background),
    ) {
        if (properties.isEmpty()) {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                item { PropertiesHeader(onAddClick) }
                item {
                    EmptyState(
                        icon = Icons.Filled.Apartment,
                        title = "No properties yet",
                        message = "Add your first property to start managing rent and bills.",
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = MaskanDimens.screenHPadding, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(MaskanDimens.sectionSpacing),
            ) {
                item { PropertiesHeader(onAddClick) }
                items(groups, key = { it.key }) { group ->
                    PropertyGroupCard(
                        group = group,
                        isUploadingPhoto = group.flats.any { it.id in uploadingIds },
                        currencyCode = group.flats.firstOrNull()?.currency ?: landlord?.currencyCode ?: "USD",
                        onFlatClick = onPropertyClick,
                    )
                }
            }
        }
    }
}

@Composable
private fun PropertiesHeader(onAddClick: () -> Unit) {
    val colors = MaskanTheme.colors
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = "Properties", style = MaskanType.screenTitle, color = colors.textPrimary)
        Box(
            modifier = Modifier.size(44.dp).background(colors.surface, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            IconButton(onClick = onAddClick) {
                Icon(Icons.Filled.Add, contentDescription = "Add Property", tint = colors.gradientStart)
            }
        }
    }
}

@Composable
private fun PropertyGroupCard(
    group: PropertyGroup,
    isUploadingPhoto: Boolean,
    currencyCode: String,
    onFlatClick: (String) -> Unit,
) {
    val colors = MaskanTheme.colors
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.titleBackground, RoundedCornerShape(MaskanDimens.cornerRadius))
            .padding(MaskanDimens.cardPadding),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
            Column {
                Text(text = group.buildingName, style = MaskanType.cardTitle, color = colors.textPrimary)
                Text(text = group.address, style = MaskanType.secondary, color = colors.textSecondary)
            }
            Column(horizontalAlignment = Alignment.End) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (isUploadingPhoto) "Uploading photo…" else "${group.occupiedCount}/${group.totalCount} occupied",
                        style = MaskanType.secondary,
                        color = colors.textSecondary,
                    )
                    Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = colors.textTertiary, modifier = Modifier.size(18.dp))
                }
                group.propertyType?.let {
                    Text(text = it.label, style = MaskanType.bodyMedium, color = colors.gradientStart)
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            group.flats.forEach { flat ->
                MaskanCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onFlatClick(flat.id) },
                    shadowRadius = MaskanDimens.rowShadowRadius,
                ) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text(text = flat.displayTitle, style = MaskanType.bodyMedium, color = colors.textPrimary)
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(text = "#", style = MaskanType.secondary, color = colors.gradientStart)
                                Text(text = flat.propertyIdCode, style = MaskanType.secondary, color = colors.gradientStart)
                            }
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            StatusBadge(
                                text = if (flat.occupied) "Occupied" else "Vacant",
                                color = if (flat.occupied) colors.success else colors.warning,
                            )
                            Text(
                                text = "${AmountFormatter.format(flat.monthlyRent, flat.currency ?: currencyCode)} / month",
                                style = MaskanType.secondary,
                                color = colors.textSecondary,
                                modifier = Modifier.padding(top = 4.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}
