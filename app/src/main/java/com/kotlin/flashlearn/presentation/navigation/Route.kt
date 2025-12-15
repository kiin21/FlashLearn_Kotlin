package com.kotlin.flashlearn.presentation.navigation

/**
 * Type-safe navigation routes.
 * Using sealed class for compile-time safety and better IDE support.
 */
sealed class Route(val route: String) {
    data object SignIn : Route("sign_in")
    data object Profile : Route("profile")
}
