package com.maskan.mobileapp.ui.coachmark

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.maskan.mobileapp.data.prefs.CoachmarkPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * One instance per landlord session, scoped at LandlordShellScreen (same pattern as
 * LandlordViewModel). DataStore reads are async — unlike iOS's synchronous UserDefaults
 * read — so [stage] starts at [OnboardingStage.Finished] and [initialStageResolved] flips
 * once the real "has seen onboarding" value has been read.
 */
class CoachMarkSequencer(private val prefs: CoachmarkPreferences) : ViewModel() {
    private val _stage = MutableStateFlow<OnboardingStage>(OnboardingStage.Finished)
    val stage: StateFlow<OnboardingStage> = _stage.asStateFlow()

    private val _initialStageResolved = MutableStateFlow(false)
    val initialStageResolved: StateFlow<Boolean> = _initialStageResolved.asStateFlow()

    init {
        viewModelScope.launch {
            val seen = prefs.hasSeenFlow.first()
            _stage.value = if (seen) OnboardingStage.Finished else OnboardingStage.PointAtAddButton
            _initialStageResolved.value = true
        }
    }

    fun tappedAddButton() {
        if (_stage.value == OnboardingStage.PointAtAddButton) {
            _stage.value = OnboardingStage.PropertyTypeIntro(0)
        }
    }

    fun advancePropertyTypeIntro() {
        val current = _stage.value as? OnboardingStage.PropertyTypeIntro ?: return
        if (current.step < 3) {
            _stage.value = OnboardingStage.PropertyTypeIntro(current.step + 1)
        } else {
            finish()
        }
    }

    fun stepBackPropertyTypeIntro() {
        val current = _stage.value as? OnboardingStage.PropertyTypeIntro ?: return
        if (current.step > 0) {
            _stage.value = OnboardingStage.PropertyTypeIntro(current.step - 1)
        }
    }

    fun skip() = finish()

    private fun finish() {
        _stage.value = OnboardingStage.Finished
        viewModelScope.launch { prefs.markSeen() }
    }

    class Factory(private val prefs: CoachmarkPreferences) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = CoachMarkSequencer(prefs) as T
    }
}
