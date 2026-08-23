package com.maskan.mobileapp.data.util

import java.text.NumberFormat
import java.util.Locale

/**
 * Amounts throughout the app format as `"<CURRENCY CODE> <amount>"` (e.g.
 * `"AED 1,200"`), never a currency symbol — symbols don't localize well and
 * are ambiguous (e.g. "$" for multiple currencies). This is a deliberate
 * departure from `NumberFormat`'s currency style (02-data-models.md).
 */
object AmountFormatter {
    private val numberFormat: NumberFormat = NumberFormat.getNumberInstance(Locale.US).apply {
        maximumFractionDigits = 0
        isGroupingUsed = true
    }

    fun format(amount: Double, currencyCode: String): String =
        "$currencyCode ${numberFormat.format(amount)}"
}
