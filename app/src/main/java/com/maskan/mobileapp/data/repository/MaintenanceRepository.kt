package com.maskan.mobileapp.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Live-listens to a single public config doc (config/app) so the splash
 * screen can block the app for scheduled maintenance without a new build.
 * Same Firestore doc as iOS (Maskan-iOS's MaintenanceService) — one admin
 * toggle takes both platforms down together. Toggled from the Firebase
 * console / Admin SDK only; Firestore rules make this doc world-readable
 * but deny all client writes.
 */
data class MaintenanceState(
    /** Maps to the `scheduleMaintenance` field — whether to show the banner at all. */
    val isScheduled: Boolean,
    /** Maps to `message` — shown as the popup's subtitle. */
    val message: String,
    /** Maps to `appAccessible` — true lets the user dismiss and continue into the
     * app; false freezes them on the splash screen with no way past the popup. */
    val isAppAccessible: Boolean,
)

class MaintenanceRepository(private val firestore: FirebaseFirestore) {
    private val _state = MutableStateFlow<MaintenanceState?>(null)
    val state: StateFlow<MaintenanceState?> = _state

    private var listenerStarted = false

    fun startListening() {
        if (listenerStarted) return
        listenerStarted = true
        firestore.collection("config").document("app")
            .addSnapshotListener { snapshot, error ->
                val data = snapshot?.data
                if (error != null || data == null) {
                    // Missing doc / transient error: fail open rather than
                    // stranding every user on the splash screen.
                    _state.value = MaintenanceState(isScheduled = false, message = "", isAppAccessible = true)
                    return@addSnapshotListener
                }
                val rawMessage = data["message"] as? String
                    ?: "Due to scheduled maintenance app is disabled"
                _state.value = MaintenanceState(
                    isScheduled = data["scheduleMaintenance"] as? Boolean ?: false,
                    // A literal "\n" typed into the Firebase Console's plain
                    // text field stores as a two-character backslash-n, not
                    // an actual newline — unescape it so admins can still
                    // get line breaks in the message.
                    message = rawMessage.replace("\\n", "\n"),
                    isAppAccessible = data["appAccessible"] as? Boolean ?: false,
                )
            }
    }
}
