package com.maskan.mobileapp.ui.landlord.properties

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PersonAdd
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.maskan.mobileapp.data.model.PaidBy
import com.maskan.mobileapp.data.repository.FREE_CO_OWNER_LIMIT
import com.maskan.mobileapp.data.repository.MAX_CO_OWNERS
import com.maskan.mobileapp.data.repository.isBuildingLocked
import com.maskan.mobileapp.data.util.AmountFormatter
import com.maskan.mobileapp.data.util.PropertyIdCodeGenerator
import com.maskan.mobileapp.ui.components.AppTextField
import com.maskan.mobileapp.ui.components.BillExpenseRow
import com.maskan.mobileapp.ui.components.GradientButton
import com.maskan.mobileapp.ui.components.MaskanCard
import com.maskan.mobileapp.ui.components.StatusBadge
import com.maskan.mobileapp.ui.landlord.LandlordViewModel
import com.maskan.mobileapp.ui.landlord.billTypeColor
import com.maskan.mobileapp.ui.landlord.billTypeIcon
import com.maskan.mobileapp.ui.theme.MaskanDimens
import com.maskan.mobileapp.ui.theme.MaskanTheme
import com.maskan.mobileapp.ui.theme.MaskanType
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Building-level view (ANDROID_PROPERTIES_FEATURE_SPEC.md §3): header,
 * units + Add Flat, Documents, Property Expenses, co-owner management, and
 * the whole-building delete/leave action. [propertyId] is any one flat's id
 * in the building — the group is resolved client-side from it.
 */
@Composable
fun BuildingDetailScreen(
    viewModel: LandlordViewModel,
    propertyId: String,
    onBack: () -> Unit,
    onFlatClick: (String) -> Unit,
    onEdit: (String) -> Unit,
    onUpgradeRequired: () -> Unit,
    onDocumentsClick: (String) -> Unit,
    onBillClick: (String) -> Unit,
) {
    val colors = MaskanTheme.colors
    val properties by viewModel.properties.collectAsStateWithLifecycle()
    val landlord by viewModel.landlord.collectAsStateWithLifecycle()
    val bills by viewModel.bills.collectAsStateWithLifecycle()
    val anchor = properties.find { it.id == propertyId }
    val scope = rememberCoroutineScope()
    val isPro = landlord?.isPro == true
    val coOwnerLimit = if (isPro) MAX_CO_OWNERS else FREE_CO_OWNER_LIMIT

    if (anchor == null) {
        Box(modifier = Modifier.fillMaxSize().background(colors.background), contentAlignment = Alignment.Center) {
            Text(text = "Building not found", style = MaskanType.body, color = colors.textSecondary)
        }
        return
    }

    val group = remember(properties, anchor.groupKey) { groupProperties(properties).first { it.key == anchor.groupKey } }
    // Any sibling can stand in for "the building" for shared-field batch writes; coOwners is identical across flats.
    val representative = group.flats.first()
    val siblingIds = group.flats.map { it.id }
    val myUid = viewModel.landlordUid
    val isOwner = representative.landlordId == myUid
    val coOwnerUids = representative.coOwners.orEmpty()
    val currencyCode = representative.currency ?: landlord?.currencyCode ?: "USD"
    val isLocked = remember(group, properties, isPro) {
        isBuildingLocked(group.key, representative.landlordId, myUid, isPro, properties)
    }
    val expenseBills = remember(bills, siblingIds) {
        bills.filter { it.propertyId in siblingIds && it.paidBy == PaidBy.LANDLORD }.sortedByDescending { it.dueDate?.time ?: 0L }
    }

    var ownerEmail by remember { mutableStateOf<String?>(null) }
    var coOwnerEmails by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    LaunchedEffect(representative.landlordId, coOwnerUids) {
        if (!isOwner) ownerEmail = viewModel.landlordRepository.getById(representative.landlordId)?.email
        coOwnerEmails = coOwnerUids.associateWith { uid -> viewModel.landlordRepository.getById(uid)?.email ?: uid }
    }

    var showAddCoOwnerForm by remember { mutableStateOf(false) }
    var addEmail by remember { mutableStateOf("") }
    var isAdding by remember { mutableStateOf(false) }
    var actionError by remember { mutableStateOf<String?>(null) }
    var uidPendingRemoval by remember { mutableStateOf<String?>(null) }
    var showLeaveConfirm by remember { mutableStateOf(false) }

    var showAddFlatForm by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var isDeleting by remember { mutableStateOf(false) }
    var deleteError by remember { mutableStateOf<String?>(null) }

    fun requestAddCoOwner() {
        if (isLocked) { onUpgradeRequired(); return }
        if (coOwnerUids.size >= coOwnerLimit) { onUpgradeRequired(); return }
        showAddCoOwnerForm = true
    }

    fun requestAddFlat() {
        if (isLocked) { onUpgradeRequired(); return }
        showAddFlatForm = true
    }

    Column(modifier = Modifier.fillMaxSize().background(colors.background).statusBarsPadding().navigationBarsPadding()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = colors.textPrimary) }
            if (isOwner) {
                IconButton(onClick = { if (isLocked) onUpgradeRequired() else onEdit(representative.id) }) {
                    Icon(Icons.Filled.Edit, contentDescription = "Edit", tint = colors.textPrimary)
                }
            }
        }

        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = MaskanDimens.screenHPadding),
            verticalArrangement = Arrangement.spacedBy(MaskanDimens.sectionSpacing),
        ) {
            if (!representative.photoUrl.isNullOrBlank()) {
                AsyncImage(
                    model = representative.photoUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxWidth().height(180.dp).clip(RoundedCornerShape(MaskanDimens.heroCornerRadius)),
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                )
            }

            Column {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                    Text(text = group.buildingName, style = MaskanType.sectionTitle, color = colors.textPrimary, modifier = Modifier.weight(1f))
                    StatusBadge(text = "${group.occupiedCount}/${group.totalCount} occupied", color = colors.success)
                }
                Text(text = group.address, style = MaskanType.secondary, color = colors.textSecondary, modifier = Modifier.padding(top = 2.dp))
                group.propertyType?.let {
                    Text(text = it.label, style = MaskanType.bodyMedium, color = colors.gradientStart, modifier = Modifier.padding(top = 2.dp))
                }
            }

            if (isLocked) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(colors.warning.copy(alpha = 0.12f), RoundedCornerShape(MaskanDimens.cornerRadius))
                        .clickable(onClick = onUpgradeRequired)
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Filled.Lock, contentDescription = null, tint = colors.warning, modifier = Modifier.size(18.dp))
                    Text(
                        text = "Locked on the free plan — you can view this property and remove it or its tenant, but editing needs Pro.",
                        style = MaskanType.secondary,
                        color = colors.textPrimary,
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
            }

            Column {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "UNITS", style = MaskanType.overline, color = colors.textTertiary)
                    if (group.propertyType?.supportsMultipleUnits == true) {
                        Text(
                            text = "+ Add Flat",
                            style = MaskanType.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = colors.gradientStart,
                            modifier = Modifier.clickable { requestAddFlat() },
                        )
                    }
                }
                Column(modifier = Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    group.flats.forEach { flat ->
                        MaskanCard(modifier = Modifier.fillMaxWidth().clickable { onFlatClick(flat.id) }) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Column {
                                    Text(text = flat.displayTitle, style = MaskanType.bodyMedium, color = colors.textPrimary)
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text(text = "#", style = MaskanType.secondary, color = colors.gradientStart)
                                        Text(text = flat.propertyIdCode, style = MaskanType.secondary, color = colors.gradientStart)
                                    }
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    StatusBadge(text = if (flat.occupied) "Occupied" else "Vacant", color = if (flat.occupied) colors.success else colors.warning)
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
                if (showAddFlatForm) {
                    AddFlatForm(
                        buildingName = group.buildingName,
                        address = group.address,
                        onCancel = { showAddFlatForm = false },
                        onSave = { unitName, rent, code ->
                            scope.launch {
                                try {
                                    viewModel.propertyRepository.addFlat(representative, unitName, rent, code)
                                    showAddFlatForm = false
                                } catch (t: Throwable) {
                                    actionError = t.message ?: "Couldn't add this flat."
                                }
                            }
                        },
                        checkCodeAvailable = { code -> viewModel.propertyRepository.isCodeAvailable(code) },
                    )
                }
            }

            MaskanCard(
                modifier = Modifier.fillMaxWidth().clickable { onDocumentsClick(representative.id) },
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Description, contentDescription = null, tint = colors.gradientStart)
                        Text(text = "Documents", style = MaskanType.bodyMedium, color = colors.textPrimary, modifier = Modifier.padding(start = 10.dp))
                    }
                    Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = colors.textTertiary)
                }
            }

            if (expenseBills.isNotEmpty()) {
                Column {
                    Text(text = "PROPERTY EXPENSES", style = MaskanType.overline, color = colors.textTertiary, modifier = Modifier.padding(bottom = 8.dp))
                    MaskanCard(modifier = Modifier.fillMaxWidth(), contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)) {
                        expenseBills.forEachIndexed { index, bill ->
                            val isPaid = bill.status.raw == "paid"
                            BillExpenseRow(
                                title = bill.type.label,
                                date = bill.dueDate,
                                amount = AmountFormatter.format(bill.amount, currencyCode),
                                statusLabel = if (isPaid) "Paid" else "Pending",
                                statusColor = if (isPaid) colors.success else colors.warning,
                                iconTint = billTypeColor(bill.type),
                                icon = billTypeIcon(bill.type),
                                onClick = { onBillClick(bill.id) },
                            )
                            if (index != expenseBills.lastIndex) {
                                androidx.compose.material3.HorizontalDivider(color = colors.border)
                            }
                        }
                    }
                }
            }

            Column {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "CO-OWNERS", style = MaskanType.overline, color = colors.textTertiary)
                    if (isOwner) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { requestAddCoOwner() },
                        ) {
                            Icon(Icons.Filled.PersonAdd, contentDescription = null, tint = colors.gradientStart, modifier = Modifier.size(16.dp))
                            Text(text = " Add", style = MaskanType.bodyMedium.copy(fontWeight = FontWeight.SemiBold), color = colors.gradientStart)
                        }
                    }
                }

                if (!isOwner) {
                    MaskanCard(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
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

                if (isOwner && showAddCoOwnerForm) {
                    Column(modifier = Modifier.padding(top = 16.dp)) {
                        AppTextField(
                            value = addEmail,
                            onValueChange = { addEmail = it; actionError = null },
                            label = "Add co-owner by email",
                            placeholder = "landlord@example.com",
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Email,
                        )
                        Text(
                            text = "${coOwnerUids.size}/$coOwnerLimit co-owners" + if (!isPro) " (Free plan)" else "",
                            style = MaskanType.secondary,
                            color = colors.textTertiary,
                            modifier = Modifier.padding(top = 4.dp),
                        )
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
                                            else -> {
                                                viewModel.propertyRepository.addCoOwner(representative, siblingIds, found.id, coOwnerLimit)
                                                showAddCoOwnerForm = false
                                            }
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

                actionError?.let {
                    Text(text = it, style = MaskanType.secondary, color = colors.danger, modifier = Modifier.padding(top = 8.dp))
                }
            }

            Column(modifier = Modifier.padding(bottom = 24.dp)) {
                if (isOwner) {
                    val allVacant = group.flats.all { !it.occupied }
                    GradientButton(
                        text = "Delete Property",
                        onClick = { showDeleteConfirm = true },
                        enabled = allVacant,
                        isLoading = isDeleting,
                        loadingText = "Deleting…",
                        brush = SolidColor(colors.danger.copy(alpha = if (allVacant) 1f else 0.3f)),
                        contentColor = Color.White,
                    )
                    if (!allVacant) {
                        Text(
                            text = "All units must be vacant before deleting the property.",
                            style = MaskanType.secondary,
                            color = colors.textSecondary,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }
                    deleteError?.let {
                        Text(text = it, style = MaskanType.secondary, color = colors.danger, modifier = Modifier.padding(top = 8.dp))
                    }
                } else {
                    GradientButton(
                        text = "Leave Building",
                        onClick = { showLeaveConfirm = true },
                        brush = SolidColor(colors.danger),
                        contentColor = Color.White,
                    )
                }
            }
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

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete this property?") },
            text = { Text("All ${group.totalCount} unit(s) and their Property IDs will be permanently removed. Bills, payments, and tenant history are kept.") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    isDeleting = true
                    scope.launch {
                        try {
                            viewModel.propertyRepository.deleteBuilding(group.flats)
                            onBack()
                        } catch (t: Throwable) {
                            deleteError = t.message ?: "Couldn't delete this property."
                        } finally {
                            isDeleting = false
                        }
                    }
                }) { Text("Delete", color = colors.danger) }
            },
            dismissButton = { TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") } },
        )
    }
}

/**
 * Add Flat (ANDROID_PROPERTIES_FEATURE_SPEC.md §6.3): single-flat inline
 * form, live-derived Property ID mirroring AddPropertyScreen's per-flat
 * entry (§6.4), scoped to one code session so the suggestion doesn't jump
 * around while typing.
 */
@Composable
private fun AddFlatForm(
    buildingName: String,
    address: String,
    onCancel: () -> Unit,
    onSave: (unitName: String, monthlyRent: Double, propertyIdCode: String) -> Unit,
    checkCodeAvailable: suspend (String) -> Boolean,
) {
    val colors = MaskanTheme.colors
    val scope = rememberCoroutineScope()
    val session = remember { PropertyIdCodeGenerator.Session() }
    var unitName by remember { mutableStateOf("") }
    var monthlyRent by remember { mutableStateOf("") }
    var manualCode by remember { mutableStateOf<String?>(null) }
    var isSaving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val code = manualCode ?: session.derive(buildingName + address)
    val rentValue = monthlyRent.toDoubleOrNull()
    val isValid = unitName.isNotBlank() && rentValue != null && rentValue > 0 && code.length >= 4

    MaskanCard(modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
        Text(text = "ADD FLAT", style = MaskanType.overline, color = colors.textTertiary)
        Column(modifier = Modifier.padding(top = 10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            AppTextField(value = unitName, onValueChange = { unitName = it }, label = "Unit name", placeholder = "e.g. Flat 3")
            AppTextField(value = monthlyRent, onValueChange = { monthlyRent = it }, label = "Monthly rent", placeholder = "0", keyboardType = KeyboardType.Number)
            AppTextField(
                value = code,
                onValueChange = { manualCode = it.uppercase().filter { c -> c.isLetterOrDigit() } },
                label = "Property ID",
                placeholder = "AUTO",
            )
            if (manualCode != null) {
                Text(
                    text = "Reset to suggested",
                    style = MaskanType.secondary,
                    color = colors.gradientStart,
                    modifier = Modifier.clickable { manualCode = null },
                )
            }
            error?.let { Text(text = it, style = MaskanType.secondary, color = colors.danger) }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GradientButton(
                    text = "Save",
                    isLoading = isSaving,
                    enabled = isValid,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        isSaving = true
                        error = null
                        scope.launch {
                            try {
                                if (!checkCodeAvailable(code)) {
                                    error = "Property ID \"$code\" is already in use."
                                    isSaving = false
                                    return@launch
                                }
                                onSave(unitName, rentValue ?: 0.0, code)
                            } catch (t: Throwable) {
                                error = t.message ?: "Couldn't add this flat."
                                isSaving = false
                            }
                        }
                    },
                )
                GradientButton(
                    text = "Cancel",
                    onClick = onCancel,
                    brush = SolidColor(colors.fieldBackground),
                    contentColor = colors.textPrimary,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}
