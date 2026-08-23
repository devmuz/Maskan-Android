package com.maskan.mobileapp.ui.landlord

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Handyman
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.RequestQuote
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.Plumbing
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.maskan.mobileapp.data.model.BillType
import com.maskan.mobileapp.ui.theme.MaskanTheme

fun billTypeIcon(type: BillType): ImageVector = when (type) {
    BillType.RENT -> Icons.Filled.Home
    BillType.ELECTRICITY -> Icons.Filled.Bolt
    BillType.WATER -> Icons.Filled.WaterDrop
    BillType.HOUSE_TAX -> Icons.Filled.RequestQuote
    BillType.SEWERAGE -> Icons.Filled.Plumbing
    BillType.MAINTENANCE -> Icons.Filled.Handyman
    BillType.OTHER -> Icons.Filled.Receipt
}

/** Distinct badge color per bill type, for lists that mix multiple types (e.g. tenant payment history). */
@Composable
fun billTypeColor(type: BillType): Color {
    val colors = MaskanTheme.colors
    return when (type) {
        BillType.RENT -> colors.indigoDeep
        BillType.ELECTRICITY -> colors.warning
        BillType.WATER -> Color(0xFF2E90E5)
        BillType.HOUSE_TAX -> colors.gold
        BillType.SEWERAGE -> Color(0xFF14B8A6)
        BillType.MAINTENANCE -> colors.gradientStart
        BillType.OTHER -> colors.textTertiary
    }
}
