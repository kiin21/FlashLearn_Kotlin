package com.kotlin.flashlearn.presentation.navigation

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navArgument
import com.kotlin.flashlearn.domain.repository.AuthRepository
import com.kotlin.flashlearn.presentation.dailyword_archive.DailyWordArchiveScreen
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
import com.kotlin.flashlearn.presentation.topic.AddWordScreen
import com.kotlin.flashlearn.presentation.topic.CardDetailScreen
import com.kotlin.flashlearn.presentation.topic.CardDetailViewModel
import com.kotlin.flashlearn.presentation.topic.TopicDetailScreen
import com.kotlin.flashlearn.presentation.topic.TopicDetailViewModel
import com.kotlin.flashlearn.presentation.topic.TopicScreen
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

/**
 * Navigation host for the app.
 * Extracted from MainActivity for cleaner separation.
 * Uses type-safe routes and Hilt for ViewModel injection.
 */
@Composable
fun FlashlearnNavHost(
    navController: NavHostController,
    authRepository: AuthRepository,
    userRepository: com.kotlin.flashlearn.domain.repository.UserRepository,
    languageManager: com.kotlin.flashlearn.util.LanguageManager,
    themeManager: com.kotlin.flashlearn.util.ThemeManager,
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

            // Check if already signed in and restore session (checks for linked accounts)
            LaunchedEffect(key1 = Unit) {
                val userData = authRepository.restoreSession()
                if (userData != null) {
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

                        is SignInUiEvent.NavigateToRegister -> {
                            navController.navigate(Route.Register.route)
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
                },
                onUsernameChange = viewModel::onUsernameChange,
                onPasswordChange = viewModel::onPasswordChange,
                onLoginClick = viewModel::signInWithUsername,
                onRegisterClick = viewModel::navigateToRegister,
                onForgotPasswordClick = {
                    navController.navigate(Route.ForgotPassword.route)
                }
            )
        }

        composable(Route.ForgotPassword.route) {
            com.kotlin.flashlearn.presentation.sign_in.ForgotPasswordScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Route.ResetPassword.route,
            deepLinks = listOf(
                androidx.navigation.navDeepLink {
                    uriPattern = "flashlearn://reset-password?token={token}"
                }
            ),
            arguments = listOf(
                navArgument("token") { type = NavType.StringType }
            )
        ) {
            val viewModel =
                hiltViewModel<com.kotlin.flashlearn.presentation.reset_password.ResetPasswordViewModel>()
            val state by viewModel.state.collectAsStateWithLifecycle()

            // Handle one-time events
            LaunchedEffect(key1 = Unit) {
                viewModel.uiEvent.collectLatest { event ->
                    when (event) {
                        is com.kotlin.flashlearn.presentation.reset_password.ResetPasswordUiEvent.NavigateToSignIn -> {
                            navController.navigate(Route.SignIn.route) {
                                popUpTo(Route.ResetPassword.route) { inclusive = true }
                            }
                        }

                        is com.kotlin.flashlearn.presentation.reset_password.ResetPasswordUiEvent.ShowSuccess -> {
                            Toast.makeText(
                                context,
                                "Password reset successfully",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                }
            }

            com.kotlin.flashlearn.presentation.reset_password.ResetPasswordScreen(
                state = state,
                onPasswordChange = viewModel::onPasswordChange,
                onConfirmPasswordChange = viewModel::onConfirmPasswordChange,
                onSubmit = viewModel::onSubmit
            )
        }

        // Register Screen
        composable(Route.Register.route) {
            val viewModel =
                hiltViewModel<com.kotlin.flashlearn.presentation.register.RegisterViewModel>()
            val state by viewModel.state.collectAsStateWithLifecycle()

            LaunchedEffect(key1 = Unit) {
                viewModel.uiEvent.collectLatest { event ->
                    when (event) {
                        is com.kotlin.flashlearn.presentation.register.RegisterUiEvent.NavigateToOnboarding -> {
                            navController.navigate(Route.Onboarding.route) {
                                popUpTo(Route.Register.route) { inclusive = true }
                            }
                        }

                        is com.kotlin.flashlearn.presentation.register.RegisterUiEvent.NavigateToSignIn -> {
                            navController.popBackStack()
                        }

                        is com.kotlin.flashlearn.presentation.register.RegisterUiEvent.ShowError -> {
                            Toast.makeText(context, event.message, Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }

            com.kotlin.flashlearn.presentation.register.RegisterScreen(
                state = state,
                onUsernameChange = viewModel::onUsernameChange,
                onPasswordChange = viewModel::onPasswordChange,
                onConfirmPasswordChange = viewModel::onConfirmPasswordChange,
                onRegisterClick = viewModel::register,
                onBackClick = { navController.popBackStack() }
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
            val userSession by authRepository.sessionState.collectAsStateWithLifecycle()
            
            // Map UserData to User domain model partially for UI
            val user = userSession?.let {
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
                onNavigateToCommunity = {
                    navController.navigate(Route.Community.route)
                },
                onNavigateToLearningSession = { topicId ->
                    navController.navigate(Route.LearningSession.createRoute(topicId, "home"))
                },
                onNavigateToTopicDetail = { topicId ->
                    navController.navigate(Route.TopicDetail.createRoute(topicId))
                },
                onNavigateToDailyWordArchive = {
                    navController.navigate(Route.DailyWordArchive.route)
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
                onNavigateToCommunity = {
                    navController.navigate(Route.Community.route)
                },
                onNavigateToTopicDetail = { topicId ->
                    navController.navigate(
                        Route.TopicDetail.createRoute(topicId)
                    )
                },
                onNavigateToAddTopic = {
                    navController.navigate(Route.AddWord.createRoute(null))
                }
            )
        }

        composable(
            route = Route.TopicDetail.route,
            arguments = listOf(
                navArgument("topicId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val viewModel = hiltViewModel<TopicDetailViewModel>(backStackEntry)
            val state by viewModel.state.collectAsStateWithLifecycle()
            val topicId = backStackEntry.arguments?.getString("topicId").orEmpty()

            // Refresh cards when returning from AddWordScreen
            val currentBackStackEntry by navController.currentBackStackEntryAsState()
            LaunchedEffect(currentBackStackEntry) {
                // Only refresh if we're on this screen (not navigating away)
                if (currentBackStackEntry?.destination?.route == Route.TopicDetail.route) {
                    viewModel.refreshCards()
                }
            }

            TopicDetailScreen(
                topicId = topicId,
                state = state,
                onBack = { navController.popBackStack() },
                onNavigateToCardDetail = { cardId ->
                    navController.navigate(Route.CardDetail.createRoute(cardId))
                },
                onAddCard = {
                    navController.navigate(Route.AddWord.createRoute(topicId))
                },
                onAddTopic = {
                    navController.navigate(Route.AddWord.createRoute(null))
                },
                onStudyNow = {
                    navController.navigate(
                        Route.LearningSession.createRoute(
                            topicId,
                            returnTo = "topic"
                        )
                    )
                },
                onTakeQuiz = { config ->
                    val encodedIds = URLEncoder.encode(
                        config.selectedFlashcardIds.joinToString(","),
                        StandardCharsets.UTF_8.toString()
                    )
                    navController.navigate(
                        Route.QuizSession.createRoute(
                            topicId = topicId,
                            mode = config.mode,
                            count = config.questionCount,
                            selectedIds = encodedIds
                        )
                    )
                },
                onToggleSelectionMode = viewModel::toggleSelectionMode,
                onToggleCardSelection = viewModel::toggleCardSelection,
                onSelectAll = viewModel::selectAllCards,
                onDeleteSelected = viewModel::deleteSelectedCards,
                onDeleteTopic = {
                    viewModel.deleteTopic {
                        navController.popBackStack()
                    }
                },
                onUpdateTopic = viewModel::updateTopic,
                onRegenerateImage = viewModel::regenerateImage,
                onTogglePublic = viewModel::togglePublicStatus,
                onSaveToMyTopics = viewModel::saveToMyTopics,
                onClearMessages = viewModel::clearMessages,
                onSearchQueryChange = viewModel::updateSearchQuery,
                onUpdateFlashcard = viewModel::updateFlashcard,
                onDeleteFlashcard = viewModel::deleteFlashcard,
                onOpenFilter = viewModel::openFilterSheet,
                onLevelToggle = viewModel::toggleLevelFilter,
                onApplyFilters = viewModel::applyFilters,
                onClearFilters = viewModel::clearFilters,
                onDismissFilter = viewModel::dismissFilterSheet,
                onResetProgress = viewModel::resetTopicProgress
            )
        }

        // Card Detail Screen
        composable(
            route = Route.CardDetail.route,
            arguments = listOf(
                navArgument("cardId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val viewModel = hiltViewModel<CardDetailViewModel>()
            val state by viewModel.state.collectAsStateWithLifecycle()

            CardDetailScreen(
                state = state,
                onFlip = { viewModel.flip() },
                onBack = { navController.popBackStack() },
                onRegenerateImage = { viewModel.regenerateImage() }
            )
        }

        // Add Word Screen
        composable(
            route = Route.AddWord.route,
            arguments = listOf(
                navArgument("topicId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val topicId = backStackEntry.arguments?.getString("topicId")

            AddWordScreen(
                topicId = topicId,
                onBack = { navController.popBackStack() },
                onWordAdded = {
                    // Refresh the topic detail screen after adding words
                    navController.popBackStack()
                }
            )
        }

        composable(
            route = Route.LearningSession.route,
            arguments = listOf(
                navArgument("topicId") { type = NavType.StringType },
                navArgument("returnTo") {
                    type = NavType.StringType
                    defaultValue = "home"
                }
            )
        ) { backStackEntry ->
            val topicId = backStackEntry.arguments?.getString("topicId").orEmpty()
            val returnTo = backStackEntry.arguments?.getString("returnTo") ?: "home"

            val viewModel = hiltViewModel<LearningSessionViewModel>()
            val state by viewModel.state.collectAsStateWithLifecycle()

            // Handle one-time events
            LaunchedEffect(key1 = Unit) {
                viewModel.uiEvent.collectLatest { event ->
                    when (event) {
                        is LearningSessionUiEvent.NavigateToHome -> {
                            navController.popBackStack()
                        }

                        is LearningSessionUiEvent.SessionComplete -> {
                            navController.navigate(
                                Route.SessionComplete.createRoute(
                                    topicId,
                                    returnTo
                                )
                            )
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
                onSwipeRight = { viewModel.onSwipeRight() },
                onSwipeLeft = { viewModel.onSwipeLeft() },
                onUndo = { viewModel.onUndo() },
                onExit = { viewModel.exitSession() }
            )
        }

        composable(
            route = Route.SessionComplete.route,
            arguments = listOf(
                navArgument("topicId") { type = NavType.StringType },
                navArgument("returnTo") {
                    type = NavType.StringType
                    defaultValue = "home"
                }
            )
        ) { backStackEntry ->
            val topicId = backStackEntry.arguments?.getString("topicId").orEmpty()
            val returnTo = backStackEntry.arguments?.getString("returnTo") ?: "home"

            val previousBackStackEntry = navController.previousBackStackEntry
            val learningVm =
                previousBackStackEntry?.let { hiltViewModel<LearningSessionViewModel>(it) }

            val state = learningVm?.state?.collectAsStateWithLifecycle()?.value
            val streakResult by (learningVm?.streakResult?.collectAsStateWithLifecycle()
                ?: remember { mutableStateOf(null) })

            SessionCompleteScreen(
                masteredCount = state?.masteredCardCount ?: 0,
                totalCount = state?.initialCardCount ?: 0,
                streakResult = streakResult,
                onBackToHome = {
                    if (returnTo == "topic") {
                        navController.navigate(Route.TopicDetail.createRoute(topicId)) {
                            popUpTo(Route.TopicDetail.createRoute(topicId)) { inclusive = false }
                            launchSingleTop = true
                        }
                    } else {
                        navController.navigate(Route.Home.route) {
                            popUpTo(Route.Home.route) { inclusive = true }
                        }
                    }
                }
            )
        }

        composable(Route.Community.route) {
            com.kotlin.flashlearn.presentation.community.CommunityScreen(
                onNavigateToHome = {
                    navController.navigate(Route.Home.route) {
                        popUpTo(Route.Home.route) { inclusive = true }
                    }
                },
                onNavigateToTopic = {
                    navController.navigate(Route.Topic.route)
                },
                onNavigateToProfile = {
                    navController.navigate(Route.Profile.route)
                },
                onNavigateToTopicDetail = { topicId ->
                    navController.navigate(Route.TopicDetail.createRoute(topicId))
                },
                onNavigateToUserProfile = { userId ->
                    navController.navigate(Route.UserProfile.createRoute(userId))
                }
            )
        }

        // User Public Profile Screen
        composable(
            route = Route.UserProfile.route,
            arguments = listOf(
                navArgument("userId") { type = NavType.StringType }
            )
        ) {
            val viewModel =
                hiltViewModel<com.kotlin.flashlearn.presentation.community.UserPublicProfileViewModel>()
            val state by viewModel.state.collectAsStateWithLifecycle()

            com.kotlin.flashlearn.presentation.community.UserPublicProfileScreen(
                state = state,
                onBack = { navController.popBackStack() },
                onTopicClick = { topicId ->
                    navController.navigate(Route.TopicDetail.createRoute(topicId))
                },
                onUpvoteClick = viewModel::toggleUpvote,
                onRetry = viewModel::retry
            )
        }

        composable(Route.Profile.route) {
            val scope = rememberCoroutineScope()
            var isLinkingInProgress by remember { mutableStateOf(false) }
            // Using LinkedAccount objects now
            var linkedAccounts by remember {
                mutableStateOf<List<com.kotlin.flashlearn.domain.model.LinkedAccount>>(
                    emptyList()
                )
            }
            val currentUserData by authRepository.sessionState.collectAsStateWithLifecycle()
            val currentUserId = currentUserData?.userId

            // Fetch linked providers AND sync full user data from Firestore
            LaunchedEffect(key1 = currentUserId) {
                currentUserId?.let { userId ->
                    scope.launch {
                        val user = userRepository.getUser(userId)
                        if (user != null) {
                            // Sync session state from Firestore
                            val updatedUserData = com.kotlin.flashlearn.domain.model.UserData(
                                userId = user.userId,
                                username = user.displayName ?: user.loginUsername,
                                profilePictureUrl = user.photoUrl,
                                email = user.email
                            )
                            authRepository.setCurrentUser(updatedUserData)
                            linkedAccounts = user.linkedGoogleAccounts
                        }
                    }
                }
            }

            val linkGoogleLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.StartIntentSenderForResult(),
                onResult = { result ->
                    if (result.resultCode == Activity.RESULT_OK) {
                        result.data?.let { intent ->
                            scope.launch {
                                isLinkingInProgress = true
                                authRepository.linkGoogleAccountWithIntent(intent).fold(
                                    onSuccess = {
                                        Toast.makeText(
                                            context,
                                            "Google account linked successfully!",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        // Refresh state happens automatically via Flow update in repository
                                        currentUserId?.let { userId ->
                                            val user = userRepository.getUser(userId)
                                            linkedAccounts =
                                                user?.linkedGoogleAccounts ?: emptyList()
                                        }
                                        isLinkingInProgress = false
                                    },
                                    onFailure = { error ->
                                        Toast.makeText(
                                            context,
                                            "Linking failed: ${error.message}",
                                            Toast.LENGTH_LONG
                                        ).show()
                                        isLinkingInProgress = false
                                    }
                                )
                            }
                        }
                    } else {
                        isLinkingInProgress = false
                    }
                }
            )

            ProfileScreen(
                userData = currentUserData,
                linkedAccounts = linkedAccounts, // Pass the list
                isLinkingInProgress = isLinkingInProgress,
                onLinkGoogleAccount = {
                    scope.launch {
                        isLinkingInProgress = true
                        authRepository.linkGoogleAccount().fold(
                            onSuccess = { intentSender ->
                                intentSender?.let {
                                    linkGoogleLauncher.launch(
                                        IntentSenderRequest.Builder(it).build()
                                    )
                                } ?: run {
                                    Toast.makeText(context, "Linking failed", Toast.LENGTH_SHORT)
                                        .show()
                                    isLinkingInProgress = false
                                }
                            },
                            onFailure = { error ->
                                Toast.makeText(
                                    context,
                                    "Linking failed: ${error.message}",
                                    Toast.LENGTH_LONG
                                ).show()
                                isLinkingInProgress = false
                            }
                        )
                    }
                },
                onUnlinkGoogleAccount = { googleId -> // Accepts ID now
                    scope.launch {
                        authRepository.unlinkGoogleAccount(googleId).fold(
                            onSuccess = {
                                Toast.makeText(
                                    context,
                                    "Account unlinked successfully",
                                    Toast.LENGTH_SHORT
                                ).show()
                                // Refresh state happens automatically via Flow update in repository
                                currentUserId?.let { userId ->
                                    val user = userRepository.getUser(userId)
                                    linkedAccounts = user?.linkedGoogleAccounts ?: emptyList()
                                }
                            },
                            onFailure = { error ->
                                Toast.makeText(
                                    context,
                                    "Unlinking failed: ${error.message}",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        )
                    }
                },
                onUpdateEmail = { newEmail ->
                    scope.launch {
                        currentUserId?.let { userId ->
                            try {
                                userRepository.updateEmail(userId, newEmail)
                                // Refresh user data globally
                                val updatedUser = currentUserData?.copy(email = newEmail)
                                authRepository.setCurrentUser(updatedUser)
                                Toast.makeText(context, "Primary email updated", Toast.LENGTH_SHORT)
                                    .show()
                            } catch (e: Exception) {
                                Toast.makeText(
                                    context,
                                    "Failed to update email: ${e.message}",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }
                },
                onUpdateProfilePicture = { uri ->
                    scope.launch {
                        currentUserId?.let { userId ->
                            try {
                                Toast.makeText(context, "Uploading image...", Toast.LENGTH_SHORT)
                                    .show()
                                val downloadUrl =
                                    userRepository.uploadProfilePicture(userId, uri.toString())

                                // Refresh user data locally and globally
                                val updatedUser = currentUserData?.copy(profilePictureUrl = downloadUrl)
                                authRepository.setCurrentUser(updatedUser)
                                
                                Toast.makeText(
                                    context,
                                    "Profile picture updated",
                                    Toast.LENGTH_SHORT
                                ).show()
                            } catch (e: Exception) {
                                Toast.makeText(
                                    context,
                                    "Upload failed: ${e.message}",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    }
                },
                onChangePassword = { oldPassword, newPassword, onResult ->
                    scope.launch {
                        authRepository.changePassword(oldPassword, newPassword).fold(
                            onSuccess = {
                                Toast.makeText(
                                    context,
                                    "Password changed successfully",
                                    Toast.LENGTH_SHORT
                                ).show()
                                onResult(true, null)
                            },
                            onFailure = { error ->
                                onResult(false, error.message)
                            }
                        )
                    }
                },
                onSignOut = {
                    scope.launch {
                        authRepository.signOut()
                        Toast.makeText(context, "Signed out", Toast.LENGTH_SHORT).show()
                        navController.navigate(Route.SignIn.route) {
                            popUpTo(Route.Home.route) { inclusive = true }
                        }
                    }
                },
                onNavigateToHome = {
                    navController.navigate(Route.Home.route)
                },
                onNavigateToTopic = {
                    navController.navigate(Route.Topic.route)
                },
                onNavigateToCommunity = {
                    navController.navigate(Route.Community.route)
                },
                onLanguageChange = { languageCode ->
                    languageManager.setLanguage(languageCode)
                },
                themeManager = themeManager
            )
        }

        composable(
            route = Route.QuizSession.route,
            arguments = listOf(
                navArgument("topicId") { type = NavType.StringType },
                navArgument("mode") {
                    type = NavType.StringType
                    defaultValue = com.kotlin.flashlearn.domain.model.QuizMode.SPRINT.name
                },
                navArgument("count") {
                    type = NavType.IntType
                    defaultValue = 10
                }
            )
        ) {
            com.kotlin.flashlearn.presentation.quiz.QuizScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToSummary = { topicId ->
                    navController.navigate(Route.QuizSummary.createRoute(topicId))
                }
            )
        }

        composable(
            route = Route.QuizSummary.route,
            arguments = listOf(navArgument("topicId") { type = NavType.StringType })
        ) { backStackEntry ->
            // Reuse QuizViewModel scoped to quiz session for summary data
            val quizBackStackEntry = navController.previousBackStackEntry
            val viewModel = hiltViewModel<com.kotlin.flashlearn.presentation.quiz.QuizViewModel>(
                quizBackStackEntry!!
            )
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            val topicId = backStackEntry.arguments?.getString("topicId").orEmpty()

            com.kotlin.flashlearn.presentation.quiz.QuizSummaryScreen(
                results = state.results,
                onBackToTopic = {
                    navController.navigate(Route.TopicDetail.createRoute(topicId)) {
                        popUpTo(Route.TopicDetail.createRoute(topicId)) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }
        composable(Route.DailyWordArchive.route) {
            DailyWordArchiveScreen(
                onBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}
