package com.kotlin.flashlearn.presentation.navigation

import com.kotlin.flashlearn.domain.model.QuizMode

/**
 * Type-safe navigation routes.
 * Using sealed class for compile-time safety and better IDE support.
 */
sealed class Route(val route: String) {
    data object SignIn : Route("sign_in")
    data object Register : Route("register")
    data object Onboarding : Route("onboarding")
    data object Home : Route("home")
    data object Topic : Route("topic")
    data object Profile : Route("profile")

    data object TopicDetail : Route("topic_detail/{topicId}") {
        fun createRoute(topicId: String) = "topic_detail/$topicId"
    }

    data object CardDetail : Route("card_detail/{cardId}") {
        fun createRoute(cardId: String) = "card_detail/$cardId"
    }

    data object LearningSession : Route("learning_session/{topicId}?returnTo={returnTo}") {
        fun createRoute(topicId: String, returnTo: String) = "learning_session/$topicId?returnTo=$returnTo"
    }
    
    data object SessionComplete : Route("session_complete/{topicId}?returnTo={returnTo}"){
        fun createRoute(topicId: String, returnTo: String) = "session_complete/$topicId?returnTo=$returnTo"
    }
    data object Community : Route("community")
    
    data object UserProfile : Route("user_profile/{userId}") {
        fun createRoute(userId: String) = "user_profile/$userId"
    }
    
    data object AddWord : Route("add_word/{topicId}") {
        fun createRoute(topicId: String?) = "add_word/${topicId ?: "new"}"
    }

    data object QuizSession : Route("quiz_session/{topicId}?mode={mode}&count={count}&selectedIds={selectedIds}") {
        fun createRoute(
            topicId: String, 
            mode: QuizMode = QuizMode.SPRINT, 
            count: Int = 10,
            selectedIds: String = ""
        ) = "quiz_session/$topicId?mode=${mode.name}&count=$count&selectedIds=$selectedIds"
    }

    data object QuizSummary : Route("quiz_summary/{topicId}") {
        fun createRoute(topicId: String) = "quiz_summary/$topicId"
    }
}
