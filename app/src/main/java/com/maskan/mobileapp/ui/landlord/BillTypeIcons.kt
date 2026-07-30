package com.maskan.mobileapp.ui.landlord

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Handyman
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.RequestQuote
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.Plumbing
import androidx.compose.ui.graphics.vector.ImageVector
import com.maskan.mobileapp.data.model.BillType

fun billTypeIcon(type: BillType): ImageVector = when (type) {
    BillType.RENT -> Icons.Filled.Home
    BillType.ELECTRICITY -> Icons.Filled.Bolt
    BillType.WATER -> Icons.Filled.WaterDrop
    BillType.HOUSE_TAX -> Icons.Filled.RequestQuote
    BillType.SEWERAGE -> Icons.Filled.Plumbing
    BillType.MAINTENANCE -> Icons.Filled.Handyman
    BillType.OTHER -> Icons.Filled.Receipt
}
