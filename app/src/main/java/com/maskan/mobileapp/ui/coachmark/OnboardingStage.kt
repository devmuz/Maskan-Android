package com.maskan.mobileapp.ui.coachmark

sealed class OnboardingStage {
    data object PointAtAddButton : OnboardingStage()
    data class PropertyTypeIntro(val step: Int) : OnboardingStage()
    data object Finished : OnboardingStage()
}
