package com.kotlin.flashlearn.presentation.navigation

/**
 * Type-safe navigation routes.
 * Using sealed class for compile-time safety and better IDE support.
 */
sealed class Route(val route: String) {
    data object SignIn : Route("sign_in")
    data object Onboarding : Route("onboarding")
    data object Home : Route("home")
    data object Topic : Route("topic")
    data object Profile : Route("profile")

    data object TopicDetail : Route("topic_detail/{topicId}") {
        fun createRoute(topicId: String) = "topic_detail/$topicId"
    }
    
    data object LearningSession : Route("learning_session/{topicId}") {
        fun createRoute(topicId: String) = "learning_session/$topicId"
    }
    
    data object SessionComplete : Route("session_complete")
    data object Community : Route("community")
}
