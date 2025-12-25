package com.kotlin.flashlearn.presentation.navigation

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.kotlin.flashlearn.domain.repository.AuthRepository
import com.kotlin.flashlearn.presentation.home.HomeScreen
import com.kotlin.flashlearn.presentation.learning_session.LearningSessionScreen
import com.kotlin.flashlearn.presentation.learning_session.LearningSessionUiEvent
import com.kotlin.flashlearn.presentation.learning_session.LearningSessionViewModel
import com.kotlin.flashlearn.presentation.learning_session.SessionCompleteScreen
import com.kotlin.flashlearn.presentation.onboarding.OnboardingScreen
import com.kotlin.flashlearn.presentation.profile.ProfileScreen
import com.kotlin.flashlearn.presentation.sign_in.SignInScreen
import com.kotlin.flashlearn.presentation.sign_in.SignInUiEvent
import com.kotlin.flashlearn.presentation.sign_in.SignInViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.kotlin.flashlearn.presentation.topic.TopicDetailScreen
import com.kotlin.flashlearn.presentation.topic.TopicScreen
import com.kotlin.flashlearn.presentation.topic.TopicScreen

/**
 * Navigation host for the app.
 * Extracted from MainActivity for cleaner separation.
 * Uses type-safe routes and Hilt for ViewModel injection.
 */
@Composable
fun FlashlearnNavHost(
    navController: NavHostController,
    authRepository: AuthRepository,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    NavHost(
        navController = navController,
        startDestination = Route.SignIn.route,
        modifier = modifier
    ) {
        composable(Route.SignIn.route) {
            val viewModel = hiltViewModel<SignInViewModel>()
            val state by viewModel.state.collectAsStateWithLifecycle()
            val scope = rememberCoroutineScope()

            // Check if already signed in
            LaunchedEffect(key1 = Unit) {
                if (authRepository.getSignedInUser() != null) {
                    navController.navigate(Route.Home.route) {
                        popUpTo(Route.SignIn.route) { inclusive = true }
                    }
                }
            }

            // Handle one-time events
            LaunchedEffect(key1 = Unit) {
                viewModel.uiEvent.collectLatest { event ->
                    when (event) {
                        is SignInUiEvent.NavigateToOnboarding -> {
                            navController.navigate(Route.Onboarding.route) {
                                popUpTo(Route.SignIn.route) { inclusive = true }
                            }
                            viewModel.resetState()
                        }
                        is SignInUiEvent.NavigateToHome -> {
                            Toast.makeText(
                                context,
                                "Sign in successful",
                                Toast.LENGTH_SHORT
                            ).show()
                            navController.navigate(Route.Home.route) {
                                popUpTo(Route.SignIn.route) { inclusive = true }
                            }
                            viewModel.resetState()
                        }
                        is SignInUiEvent.ShowError -> {
                            Toast.makeText(
                                context,
                                event.message,
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                }
            }

            val launcher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.StartIntentSenderForResult(),
                onResult = { result ->
                    if (result.resultCode == Activity.RESULT_OK) {
                        result.data?.let { intent ->
                            viewModel.onSignInResult(intent)
                        }
                    }
                }
            )

            SignInScreen(
                state = state,
                onSignInClick = {
                    scope.launch {
                        viewModel.signIn()?.let { intentSender ->
                            launcher.launch(
                                IntentSenderRequest.Builder(intentSender).build()
                            )
                        }
                    }
                }
            )
        }
        
        composable(Route.Onboarding.route) {
            OnboardingScreen(
                onFinish = {
                    navController.navigate(Route.Home.route) {
                        popUpTo(Route.Onboarding.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Route.Home.route) {
            val userData = authRepository.getSignedInUser()
            // In a real app, we might want to fetch full user details from Firestore here
            // using a HomeViewModel, but passing basic auth data works for now.
            
            // Map UserData to User domain model partially for UI
            val user = userData?.let {
                com.kotlin.flashlearn.domain.model.User(
                    userId = it.userId,
                    displayName = it.username,
                    photoUrl = it.profilePictureUrl
                )
            }

            HomeScreen(
                userData = user,
                onNavigateToProfile = {
                    navController.navigate(Route.Profile.route)
                },
                onNavigateToTopic = {
                    navController.navigate(Route.Topic.route)
                },
                onNavigateToLearningSession = { topicId ->
                    navController.navigate(Route.LearningSession.createRoute(topicId))
                }
            )
        }

        composable(Route.Topic.route) {
            TopicScreen(
                onNavigateToHome = {
                    navController.navigate(Route.Home.route)
                },
                onNavigateToProfile = {
                    navController.navigate(Route.Profile.route)
                },
                onNavigateToTopicDetail = { topicId ->
                    navController.navigate(
                        Route.TopicDetail.createRoute(topicId)
                    )
                }
            )
        }

        composable(
            route = Route.TopicDetail.route,
            arguments = listOf(
                navArgument("topicId") { type = NavType.StringType }
            )
        ) {
            TopicDetailScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Route.LearningSession.route,
            arguments = listOf(
                navArgument("topicId") { type = NavType.StringType }
            )
        ) {
            val viewModel = hiltViewModel<LearningSessionViewModel>()
            val state by viewModel.state.collectAsStateWithLifecycle()
            val currentUserId = authRepository.getSignedInUser()?.userId ?: ""

            // Handle one-time events
            LaunchedEffect(key1 = Unit) {
                viewModel.uiEvent.collectLatest { event ->
                    when (event) {
                        is LearningSessionUiEvent.NavigateToHome -> {
                            navController.navigate(Route.Home.route) {
                                popUpTo(Route.LearningSession.route) { inclusive = true }
                            }
                        }
                        is LearningSessionUiEvent.SessionComplete -> {
                            navController.navigate(Route.SessionComplete.route)
                        }
                        is LearningSessionUiEvent.ShowError -> {
                            Toast.makeText(
                                context,
                                event.message,
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                }
            }

            LearningSessionScreen(
                state = state,
                onFlipCard = { viewModel.flipCard() },
                onGotIt = { viewModel.onGotIt(currentUserId) },
                onStudyAgain = { viewModel.onStudyAgain(currentUserId) },
                onExit = { viewModel.exitSession() }
            )
        }

        composable(Route.SessionComplete.route) {
            val previousBackStackEntry = navController.previousBackStackEntry
            val state = previousBackStackEntry?.let {
                hiltViewModel<LearningSessionViewModel>(it).state.collectAsStateWithLifecycle().value
            }

            SessionCompleteScreen(
                masteredCount = state?.masteredCardIds?.size ?: 0,
                totalCount = state?.flashcards?.size ?: 0,
                onBackToHome = {
                    navController.navigate(Route.Home.route) {
                        popUpTo(Route.Home.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Route.Profile.route) {
            val scope = rememberCoroutineScope()
            
            ProfileScreen(
                userData = authRepository.getSignedInUser(),
                onSignOut = {
                    scope.launch {
                        authRepository.signOut()
                        Toast.makeText(context, "Signed out", Toast.LENGTH_SHORT).show()
                        navController.navigate(Route.SignIn.route) {
                            popUpTo(Route.Home.route) { inclusive = true }
                        }
                    }
                }
            )
        }
    }
}
