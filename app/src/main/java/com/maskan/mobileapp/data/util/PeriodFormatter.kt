package com.maskan.mobileapp.data.util

import com.maskan.mobileapp.data.model.Frequency
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Date
import java.util.Locale

/**
 * Period format (02-data-models.md / Bill.swift's BillingService): "yyyy-MM"
 * for monthly bills, "yyyy" for annual, "yyyy-MM-dd" for one-time — derived
 * from a reference date and frequency at creation time, not recomputed
 * later.
 */
object PeriodFormatter {
    private val monthlyFormatter = DateTimeFormatter.ofPattern("yyyy-MM")
    private val oneTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    private val oneTimeDisplayFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy")

    fun periodFor(referenceDate: LocalDate, frequency: Frequency): String =
        when (frequency) {
            Frequency.MONTHLY -> referenceDate.format(monthlyFormatter)
            Frequency.ANNUAL -> referenceDate.year.toString()
            Frequency.ONE_TIME -> referenceDate.format(oneTimeFormatter)
        }

    fun periodForNow(frequency: Frequency): String = periodFor(LocalDate.now(), frequency)

    /**
     * A 4-character period is annual (show as-is); a 10-character period is
     * one-time ("yyyy-MM-dd"); otherwise parse as year-month.
     */
    fun displayLabel(period: String): String {
        if (period.length == 4) return period
        if (period.length == 10) {
            return runCatching {
                LocalDate.parse(period, oneTimeFormatter).format(oneTimeDisplayFormatter)
            }.getOrDefault(period)
        }
        return runCatching {
            val yearMonth = YearMonth.parse(period, DateTimeFormatter.ofPattern("yyyy-MM"))
            "${yearMonth.month.getDisplayName(TextStyle.FULL, Locale.getDefault())} ${yearMonth.year}"
        }.getOrDefault(period)
    }

    fun currentMonthRentPeriod(): String = periodForNow(Frequency.MONTHLY)
}

fun Date.toLocalDate(): LocalDate =
    this.toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate()

fun LocalDate.toDate(): Date =
    Date.from(this.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant())

fun startOfToday(): Date = LocalDate.now().toDate()
