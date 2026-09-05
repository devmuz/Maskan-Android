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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Group
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
import com.maskan.mobileapp.ui.components.SegmentedControl
import com.maskan.mobileapp.ui.components.StatusBadge
import com.maskan.mobileapp.ui.landlord.LandlordViewModel
import com.maskan.mobileapp.ui.landlord.LocalLandlordContentBottomInset
import com.maskan.mobileapp.ui.theme.MaskanDimens
import com.maskan.mobileapp.ui.theme.MaskanTheme
import com.maskan.mobileapp.ui.theme.MaskanType
import kotlinx.coroutines.launch

private enum class PropertyFilter { ACTIVE, OLD }

/** Free-tier owned-building cap (feature-iap.md) — co-owned buildings never count against this. */
private const val FREE_PROPERTY_LIMIT = 1

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PropertiesScreen(
    viewModel: LandlordViewModel,
    onPropertyClick: (String) -> Unit,
    onArchivedPropertyClick: (String) -> Unit,
    onBuildingClick: (String) -> Unit,
    onAddClick: () -> Unit,
    onUpgradeRequired: () -> Unit,
) {
    val colors = MaskanTheme.colors
    val properties by viewModel.properties.collectAsStateWithLifecycle()
    val archivedProperties by viewModel.archivedProperties.collectAsStateWithLifecycle()
    val landlord by viewModel.landlord.collectAsStateWithLifecycle()
    val uploadingIds by viewModel.uploadingPhotoPropertyIds.collectAsStateWithLifecycle()

    var filter by remember { mutableStateOf(PropertyFilter.ACTIVE) }
    val groups = remember(properties) { groupProperties(properties) }
    val archivedGroups = remember(archivedProperties) { groupProperties(archivedProperties) }
    val currencyCode = { flats: List<com.maskan.mobileapp.data.model.Property> ->
        flats.firstOrNull()?.currency ?: landlord?.currencyCode ?: "USD"
    }
    // Only buildings this landlord primarily owns count against the free limit — co-owned
    // buildings never do (feature-iap.md).
    val ownedBuildingCount = remember(groups, viewModel.landlordUid) {
        groups.count { it.flats.first().landlordId == viewModel.landlordUid }
    }
    val isPro = landlord?.isPro == true
    val gatedAddClick = {
        if (!isPro && ownedBuildingCount >= FREE_PROPERTY_LIMIT) onUpgradeRequired() else onAddClick()
    }

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
        modifier = Modifier.fillMaxSize().background(colors.background).statusBarsPadding(),
    ) {
        if (properties.isEmpty() && archivedGroups.isEmpty() && filter == PropertyFilter.ACTIVE) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = MaskanDimens.screenHPadding,
                    end = MaskanDimens.screenHPadding,
                    top = 16.dp,
                    bottom = LocalLandlordContentBottomInset.current,
                ),
                verticalArrangement = Arrangement.spacedBy(MaskanDimens.sectionSpacing),
            ) {
                item { PropertiesHeader(gatedAddClick) }
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
                contentPadding = PaddingValues(
                    start = MaskanDimens.screenHPadding,
                    end = MaskanDimens.screenHPadding,
                    top = 16.dp,
                    bottom = LocalLandlordContentBottomInset.current,
                ),
                verticalArrangement = Arrangement.spacedBy(MaskanDimens.sectionSpacing),
            ) {
                item { PropertiesHeader(gatedAddClick) }
                if (archivedGroups.isNotEmpty()) {
                    item {
                        SegmentedControl(
                            options = listOf(PropertyFilter.ACTIVE, PropertyFilter.OLD),
                            selected = filter,
                            onSelect = { filter = it },
                            label = { if (it == PropertyFilter.ACTIVE) "Active" else "Old" },
                        )
                    }
                }
                if (filter == PropertyFilter.ACTIVE) {
                    if (groups.isEmpty()) {
                        item {
                            EmptyState(
                                icon = Icons.Filled.Apartment,
                                title = "No active properties",
                                message = "Add your first property to start managing rent and bills.",
                            )
                        }
                    }
                    items(groups, key = { it.key }) { group ->
                        PropertyGroupCard(
                            group = group,
                            isUploadingPhoto = group.flats.any { it.id in uploadingIds },
                            currencyCode = currencyCode(group.flats),
                            onFlatClick = onPropertyClick,
                            onManageCoOwners = { onBuildingClick(group.flats.first().id) },
                        )
                    }
                } else {
                    if (archivedGroups.isEmpty()) {
                        item {
                            EmptyState(
                                icon = Icons.Filled.Apartment,
                                title = "No old properties",
                                message = "Properties you delete appear here, kept for bill and payment history.",
                            )
                        }
                    }
                    items(archivedGroups, key = { it.key }) { group ->
                        ArchivedPropertyGroupCard(
                            group = group,
                            currencyCode = currencyCode(group.flats),
                            onFlatClick = onArchivedPropertyClick,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PropertiesHeader(onAddClick: () -> Unit) {
    val colors = MaskanTheme.colors
    Row(
        modifier = Modifier.fillMaxWidth(),
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
    onManageCoOwners: () -> Unit,
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
            Column(modifier = Modifier.weight(1f).clickable(onClick = onManageCoOwners)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(text = group.buildingName, style = MaskanType.cardTitle, color = colors.textPrimary)
                    Icon(Icons.Filled.Group, contentDescription = "Manage co-owners", tint = colors.textTertiary, modifier = Modifier.size(16.dp))
                }
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

/** Old (soft-deleted) building group — read-only, tappable flat cards to ArchivedPropertyDetailScreen. */
@Composable
private fun ArchivedPropertyGroupCard(
    group: PropertyGroup,
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
        Column {
            Text(text = group.buildingName, style = MaskanType.cardTitle, color = colors.textPrimary)
            Text(text = group.address, style = MaskanType.secondary, color = colors.textSecondary)
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            group.flats.forEach { flat ->
                MaskanCard(
                    modifier = Modifier.fillMaxWidth().clickable { onFlatClick(flat.id) },
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
                        StatusBadge(text = "Deleted", color = colors.textTertiary)
                    }
                }
            }
        }
    }
}
