package com.maskan.mobileapp.ui.landlord.properties

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.maskan.mobileapp.data.repository.FREE_CO_OWNER_LIMIT
import com.maskan.mobileapp.data.repository.MAX_CO_OWNERS
import com.maskan.mobileapp.ui.components.AppTextField
import com.maskan.mobileapp.ui.components.GradientButton
import com.maskan.mobileapp.ui.components.MaskanCard
import com.maskan.mobileapp.ui.components.StatusBadge
import com.maskan.mobileapp.ui.landlord.LandlordViewModel
import com.maskan.mobileapp.ui.theme.MaskanDimens
import com.maskan.mobileapp.ui.theme.MaskanTheme
import com.maskan.mobileapp.ui.theme.MaskanType
import kotlinx.coroutines.launch

/**
 * Building-level view (feature-properties.md's `BuildingDetailView`): all
 * flats in the building plus co-owner management. Reached by tapping a
 * building header on PropertiesScreen. [propertyId] is any one flat's id in
 * the building — the group is resolved client-side from it.
 */
@Composable
fun BuildingDetailScreen(viewModel: LandlordViewModel, propertyId: String, onBack: () -> Unit, onFlatClick: (String) -> Unit, onUpgradeRequired: () -> Unit) {
    val colors = MaskanTheme.colors
    val properties by viewModel.properties.collectAsStateWithLifecycle()
    val landlord by viewModel.landlord.collectAsStateWithLifecycle()
    val anchor = properties.find { it.id == propertyId }
    val scope = rememberCoroutineScope()
    // The person managing co-owners is always the signed-in landlord (only owners see the
    // Add control) — their own Pro status decides the cap (feature-iap.md).
    val coOwnerLimit = if (landlord?.isPro == true) MAX_CO_OWNERS else FREE_CO_OWNER_LIMIT

    if (anchor == null) {
        Box(modifier = Modifier.fillMaxSize().background(colors.background), contentAlignment = Alignment.Center) {
            Text(text = "Building not found", style = MaskanType.body, color = colors.textSecondary)
        }
        return
    }

    val group = remember(properties, anchor.groupKey) { groupProperties(properties).first { it.key == anchor.groupKey } }
    // Any sibling can stand in for "the property" for co-owner batch writes; coOwners is identical across flats.
    val representative = group.flats.first()
    val siblingIds = group.flats.map { it.id }
    val myUid = viewModel.landlordUid
    val isOwner = representative.landlordId == myUid
    val coOwnerUids = representative.coOwners.orEmpty()

    var ownerEmail by remember { mutableStateOf<String?>(null) }
    var coOwnerEmails by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    LaunchedEffect(representative.landlordId, coOwnerUids) {
        if (!isOwner) ownerEmail = viewModel.landlordRepository.getById(representative.landlordId)?.email
        coOwnerEmails = coOwnerUids.associateWith { uid -> viewModel.landlordRepository.getById(uid)?.email ?: uid }
    }

    var addEmail by remember { mutableStateOf("") }
    var isAdding by remember { mutableStateOf(false) }
    var actionError by remember { mutableStateOf<String?>(null) }
    var uidPendingRemoval by remember { mutableStateOf<String?>(null) }
    var showLeaveConfirm by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().background(colors.background).statusBarsPadding().navigationBarsPadding()) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = colors.textPrimary) }
        }

        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = MaskanDimens.screenHPadding),
            verticalArrangement = Arrangement.spacedBy(MaskanDimens.sectionSpacing),
        ) {
            Column {
                Text(text = group.buildingName, style = MaskanType.sectionTitle, color = colors.textPrimary)
                Text(text = group.address, style = MaskanType.secondary, color = colors.textSecondary, modifier = Modifier.padding(top = 2.dp))
            }

            Column {
                Text(text = "FLATS", style = MaskanType.overline, color = colors.textTertiary, modifier = Modifier.padding(bottom = 8.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    group.flats.forEach { flat ->
                        MaskanCard(modifier = Modifier.fillMaxWidth().clickable { onFlatClick(flat.id) }) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text(text = flat.displayTitle, style = MaskanType.bodyMedium, color = colors.textPrimary)
                                StatusBadge(text = if (flat.occupied) "Occupied" else "Vacant", color = if (flat.occupied) colors.success else colors.warning)
                            }
                        }
                    }
                }
            }

            Column {
                Text(text = "CO-OWNERS", style = MaskanType.overline, color = colors.textTertiary, modifier = Modifier.padding(bottom = 8.dp))

                if (!isOwner) {
                    MaskanCard(modifier = Modifier.fillMaxWidth()) {
                        Text(text = "Owner", style = MaskanType.secondary, color = colors.textSecondary)
                        Text(text = ownerEmail ?: "…", style = MaskanType.bodyMedium, color = colors.textPrimary, modifier = Modifier.padding(top = 2.dp))
                    }
                }

                if (coOwnerUids.isEmpty()) {
                    Text(text = "No co-owners yet.", style = MaskanType.secondary, color = colors.textSecondary, modifier = Modifier.padding(top = 8.dp))
                } else {
                    Column(modifier = Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        coOwnerUids.forEach { uid ->
                            MaskanCard(modifier = Modifier.fillMaxWidth()) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Text(text = coOwnerEmails[uid] ?: uid, style = MaskanType.bodyMedium, color = colors.textPrimary)
                                    if (isOwner) {
                                        IconButton(onClick = { uidPendingRemoval = uid }) {
                                            Icon(Icons.Filled.Close, contentDescription = "Remove co-owner", tint = colors.danger)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                if (isOwner) {
                    Column(modifier = Modifier.padding(top = 16.dp)) {
                        AppTextField(
                            value = addEmail,
                            onValueChange = { addEmail = it; actionError = null },
                            label = "Add co-owner by email",
                            placeholder = "landlord@example.com",
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Email,
                        )
                        Text(
                            text = "${coOwnerUids.size}/$coOwnerLimit co-owners" + if (landlord?.isPro != true) " (Free plan)" else "",
                            style = MaskanType.secondary,
                            color = colors.textTertiary,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                        if (coOwnerUids.size >= coOwnerLimit) {
                            GradientButton(
                                text = "Upgrade to add more co-owners",
                                onClick = onUpgradeRequired,
                                modifier = Modifier.padding(top = 12.dp),
                            )
                        } else {
                            GradientButton(
                                text = "Add",
                                isLoading = isAdding,
                                enabled = addEmail.isNotBlank(),
                                onClick = {
                                    val email = addEmail.trim()
                                    isAdding = true
                                    actionError = null
                                    scope.launch {
                                        try {
                                            val found = viewModel.landlordRepository.findByEmail(email)
                                            when {
                                                found == null -> actionError = "No Maskan account found for that email."
                                                found.id == myUid -> actionError = "You already own this building."
                                                coOwnerUids.contains(found.id) -> actionError = "Already a co-owner."
                                                else -> viewModel.propertyRepository.addCoOwner(representative, siblingIds, found.id, coOwnerLimit)
                                            }
                                            addEmail = ""
                                        } catch (t: Throwable) {
                                            actionError = t.message ?: "Couldn't add co-owner."
                                        } finally {
                                            isAdding = false
                                        }
                                    }
                                },
                                modifier = Modifier.padding(top = 12.dp),
                            )
                        }
                    }
                } else {
                    GradientButton(
                        text = "Leave Building",
                        onClick = { showLeaveConfirm = true },
                        brush = androidx.compose.ui.graphics.SolidColor(colors.danger),
                        contentColor = androidx.compose.ui.graphics.Color.White,
                        modifier = Modifier.padding(top = 16.dp),
                    )
                }

                actionError?.let {
                    Text(text = it, style = MaskanType.secondary, color = colors.danger, modifier = Modifier.padding(top = 8.dp))
                }
            }

            Box(modifier = Modifier.padding(bottom = 24.dp))
        }
    }

    uidPendingRemoval?.let { uid ->
        AlertDialog(
            onDismissRequest = { uidPendingRemoval = null },
            title = { Text("Remove this co-owner?") },
            text = { Text("${coOwnerEmails[uid] ?: uid} will lose management access to this building.") },
            confirmButton = {
                TextButton(onClick = {
                    uidPendingRemoval = null
                    scope.launch {
                        try {
                            viewModel.propertyRepository.removeCoOwner(representative, siblingIds, uid)
                        } catch (t: Throwable) {
                            actionError = t.message ?: "Couldn't remove co-owner."
                        }
                    }
                }) { Text("Remove", color = colors.danger) }
            },
            dismissButton = { TextButton(onClick = { uidPendingRemoval = null }) { Text("Cancel") } },
        )
    }

    if (showLeaveConfirm) {
        AlertDialog(
            onDismissRequest = { showLeaveConfirm = false },
            title = { Text("Leave this building?") },
            text = { Text("You'll lose management access to every flat in this building.") },
            confirmButton = {
                TextButton(onClick = {
                    showLeaveConfirm = false
                    scope.launch {
                        try {
                            viewModel.propertyRepository.removeCoOwner(representative, siblingIds, myUid)
                            onBack()
                        } catch (t: Throwable) {
                            actionError = t.message ?: "Couldn't leave this building."
                        }
                    }
                }) { Text("Leave", color = colors.danger) }
            },
            dismissButton = { TextButton(onClick = { showLeaveConfirm = false }) { Text("Cancel") } },
        )
    }
}
