package com.maskan.mobileapp.ui.coachmark

import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot

/**
 * Compose has no equivalent to SwiftUI's `anchorPreference`/`PreferenceKey` bubbling, so
 * target composables register their root-relative bounds here imperatively, and the one
 * coachmark overlay (hosted in LandlordShellScreen) reads them back by id.
 */
class CoachMarkAnchorRegistry {
    private val anchors = mutableStateMapOf<String, Rect>()

    fun register(id: String, rect: Rect) {
        anchors[id] = rect
    }

    fun unregister(id: String) {
        anchors.remove(id)
    }

    operator fun get(id: String): Rect? = anchors[id]
}

fun Modifier.coachMarkAnchor(id: String, registry: CoachMarkAnchorRegistry): Modifier = composed {
    DisposableEffect(id) {
        onDispose { registry.unregister(id) }
    }
    onGloballyPositioned { coordinates ->
        val position = coordinates.positionInRoot()
        registry.register(
            id,
            Rect(
                left = position.x,
                top = position.y,
                right = position.x + coordinates.size.width,
                bottom = position.y + coordinates.size.height,
            ),
        )
    }
}
