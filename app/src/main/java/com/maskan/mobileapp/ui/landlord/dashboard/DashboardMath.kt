package com.maskan.mobileapp.ui.landlord.dashboard

import com.maskan.mobileapp.data.model.Bill
import com.maskan.mobileapp.data.model.BillStatus
import com.maskan.mobileapp.data.model.PaidBy
import com.maskan.mobileapp.data.model.Payment
import com.maskan.mobileapp.data.util.toLocalDate
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

/**
 * `unpaidBills` is the shared base set both Overdue and Upcoming derive
 * from; the two ranges never overlap by construction (04-landlord-dashboard.md).
 */
fun unpaidBills(bills: List<Bill>): List<Bill> = bills.filter { it.status != BillStatus.PAID }

fun overdueBills(bills: List<Bill>, startOfToday: LocalDate): List<Bill> =
    unpaidBills(bills)
        .filter { it.dueDate != null && it.dueDate.toLocalDate().isBefore(startOfToday) }
        .sortedBy { it.dueDate }

fun upcomingBills(bills: List<Bill>, startOfToday: LocalDate): List<Bill> {
    val weekOut = startOfToday.plusDays(7)
    return unpaidBills(bills)
        .filter {
            it.dueDate != null &&
                !it.dueDate.toLocalDate().isBefore(startOfToday) &&
                !it.dueDate.toLocalDate().isAfter(weekOut)
        }
        .sortedBy { it.dueDate }
}

/** "Overdue" / "Due today" / "Due tomorrow" / "Due in Nd" (04-landlord-dashboard.md). */
fun dueStatusLabel(bill: Bill, startOfToday: LocalDate): String {
    val dueDate = bill.dueDate?.toLocalDate() ?: return ""
    val days = java.time.temporal.ChronoUnit.DAYS.between(startOfToday, dueDate)
    return when {
        days < 0 -> "Overdue"
        days == 0L -> "Due today"
        days == 1L -> "Due tomorrow"
        else -> "Due in ${days}d"
    }
}

data class MonthBucket(val label: String, val collected: Double, val expense: Double)

/**
 * Collected = sum of payments whose paidDate falls in the month and whose
 * linked bill has `paidBy` absent/"tenant" — a payment whose bill can't be
 * found is excluded entirely, never defaulted to tenant-paid.
 * Expense = sum of bill amounts (billed, not paid — a bill due in June but
 * paid in July still counts as June's expense) with `paidBy == "landlord"`
 * whose dueDate falls in the month. Classifying by bill `type` alone (rent
 * vs. non-rent) is wrong and has previously miscategorized non-rent
 * tenant-paid bills as expenses — `paidBy` is the only correct signal
 * (04-landlord-dashboard.md). Bucketing uses calendar month+year equality.
 */
fun chartBuckets(bills: List<Bill>, payments: List<Payment>, referenceMonth: YearMonth = YearMonth.now()): List<MonthBucket> {
    val months = (5 downTo 0).map { referenceMonth.minusMonths(it.toLong()) }
    val billsById = bills.associateBy { it.id }

    return months.map { month ->
        val collected = payments
            .filter { payment ->
                val paidMonth = payment.paidDate?.toLocalDate()?.let { YearMonth.from(it) }
                val bill = billsById[payment.billId] ?: return@filter false
                paidMonth == month && bill.paidBy == PaidBy.TENANT
            }
            .sumOf { it.amount }

        val expense = bills
            .filter { bill ->
                val dueMonth = bill.dueDate?.toLocalDate()?.let { YearMonth.from(it) }
                bill.paidBy == PaidBy.LANDLORD && dueMonth == month
            }
            .sumOf { it.amount }

        MonthBucket(
            label = month.month.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
            collected = collected,
            expense = expense,
        )
    }
}

fun propertyDisplayName(buildingName: String?, name: String, unit: String): String {
    val building = buildingName ?: name
    return if (unit.isNotBlank()) "$building – $unit" else building
}
