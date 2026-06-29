package com.bodylens.core.navigation

object Routes {
    const val ONBOARDING = "onboarding"
    const val HOME = "home"          // progress dashboard
    const val CAPTURE = "capture"
    const val ANALYSIS = "analysis"
    const val PLAN = "plan"
    const val SETTINGS = "settings"
    const val EXERCISE_DETAIL = "exercise/{exerciseId}"

    fun exerciseDetail(exerciseId: String) = "exercise/$exerciseId"
}
