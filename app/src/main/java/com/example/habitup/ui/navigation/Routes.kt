package com.example.habitup.ui.navigation

import kotlinx.serialization.Serializable

@Serializable data object Splash
@Serializable data object Login
@Serializable data object Today
@Serializable data class HabitForm(val habitId: String? = null)
@Serializable data class HabitDetail(val habitId: String)
@Serializable data object Settings
