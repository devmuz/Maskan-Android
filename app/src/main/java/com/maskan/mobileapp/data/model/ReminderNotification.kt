package com.maskan.mobileapp.data.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

/**
 * `notifications` collection. Currently write-only from the landlord side
 * (a "send reminder" action) — no UI reads this back yet.
 */
data class ReminderNotification(
    @DocumentId
    val id: String = "",
    val tenantId: String = "",
    val type: String = "reminder",
    val message: String = "",
    @ServerTimestamp
    val sentAt: Date? = null,
    val read: Boolean = false,
)
