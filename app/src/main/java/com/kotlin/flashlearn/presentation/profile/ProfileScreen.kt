package com.kotlin.flashlearn.presentation.profile

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kotlin.flashlearn.R
import com.kotlin.flashlearn.domain.model.UserData
import com.kotlin.flashlearn.presentation.components.BottomNavBar
import com.kotlin.flashlearn.presentation.navigation.Route
import com.kotlin.flashlearn.presentation.profile.components.ChangePasswordDialog
import com.kotlin.flashlearn.presentation.profile.components.PreferencesSection
import com.kotlin.flashlearn.presentation.profile.components.ProfilePictureDialog
import com.kotlin.flashlearn.presentation.profile.components.UserInfoSection
import com.kotlin.flashlearn.ui.theme.FlashRed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    userData: UserData?,
    onSignOut: () -> Unit,
    onNavigateToHome: () -> Unit,
    onNavigateToTopic: () -> Unit,
    onNavigateToCommunity: () -> Unit = {},
    linkedAccounts: List<com.kotlin.flashlearn.domain.model.LinkedAccount> = emptyList(),
    onLinkGoogleAccount: () -> Unit = {},
    onUnlinkGoogleAccount: (String) -> Unit = {},
    onUpdateEmail: (String) -> Unit = {},
    onUpdateProfilePicture: (android.net.Uri) -> Unit = {},
    onChangePassword: (oldPassword: String, newPassword: String, onResult: (success: Boolean, error: String?) -> Unit) -> Unit = { _, _, _ -> },
    onLanguageChange: (String) -> Unit = {},
    themeManager: com.kotlin.flashlearn.util.ThemeManager,
    isLinkingInProgress: Boolean = false,
    modifier: Modifier = Modifier
) {
    val snackbarHostState = remember { SnackbarHostState() }
    rememberCoroutineScope()
    var showUnlinkDialog by remember { mutableStateOf<String?>(null) } // Store account ID to unlink
    var showChangePasswordDialog by remember { mutableStateOf(false) }
    var showProfilePictureDialog by remember { mutableStateOf(false) }

    val themeMode by themeManager.themeMode.collectAsStateWithLifecycle()
    val isSystemDark = isSystemInDarkTheme()
    val isDarkMode = when (themeMode) {
        com.kotlin.flashlearn.util.ThemeManager.MODE_DARK -> true
        com.kotlin.flashlearn.util.ThemeManager.MODE_LIGHT -> false
        else -> isSystemDark
    }

    val onToggleDarkMode: (Boolean) -> Unit = { isDark ->
        themeManager.setThemeMode(
            if (isDark) com.kotlin.flashlearn.util.ThemeManager.MODE_DARK
            else com.kotlin.flashlearn.util.ThemeManager.MODE_LIGHT
        )
    }

    // Image picker launcher
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            onUpdateProfilePicture(uri)
        }
    }

    // Derive available emails from linked accounts + current email if not in list
    val currentEmail = userData?.email
    val availableEmails = remember(linkedAccounts, currentEmail) {
        val emails = linkedAccounts.map { it.email }.toMutableList()
        currentEmail?.let {
            if (!emails.contains(it) && it.isNotEmpty()) emails.add(it)
        }
        emails.distinct().sorted()
    }

    if (showUnlinkDialog != null) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showUnlinkDialog = null },
            title = { Text(stringResource(R.string.unlink_account)) },
            text = { Text(stringResource(R.string.unlink_account_confirm)) },
            confirmButton = {
                TextButton(onClick = {
                    val accountId = showUnlinkDialog
                    showUnlinkDialog = null
                    accountId?.let { onUnlinkGoogleAccount(it) }
                }) {
                    Text(stringResource(R.string.unlink), color = FlashRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { showUnlinkDialog = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    if (showChangePasswordDialog) {
        ChangePasswordDialog(
            onDismiss = { showChangePasswordDialog = false },
            onSubmit = { oldPassword, newPassword, onResult ->
                onChangePassword(oldPassword, newPassword) { success, error ->
                    onResult(success, error)
                    if (success) {
                        showChangePasswordDialog = false
                    }
                }
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            BottomNavBar(
                currentRoute = Route.Profile.route,
                onNavigate = { route ->
                    when (route) {
                        Route.Home.route -> onNavigateToHome()
                        Route.Topic.route -> onNavigateToTopic()
                        Route.Profile.route -> {}
                        "community" -> onNavigateToCommunity()
                    }
                }
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = stringResource(R.string.profile),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(top = 16.dp, bottom = 24.dp)
            )

            UserInfoSection(
                userData = userData,
                availableEmails = availableEmails,
                onEditProfilePicture = {
                    imagePickerLauncher.launch(
                        PickVisualMediaRequest(
                            ActivityResultContracts.PickVisualMedia.ImageOnly
                        )
                    )
                },
                onUpdateEmail = onUpdateEmail,
                onViewProfilePicture = { showProfilePictureDialog = true }
            )

            if (showProfilePictureDialog) {
                ProfilePictureDialog(
                    imageUrl = userData?.profilePictureUrl,
                    onDismiss = { showProfilePictureDialog = false }
                )
            }
            Spacer(modifier = Modifier.height(32.dp))

            PreferencesSection(
                linkedAccounts = linkedAccounts,
                isLinkingInProgress = isLinkingInProgress,
                onChangePassword = { showChangePasswordDialog = true },
                onLanguageChange = onLanguageChange,
                onLinkGoogleAccount = onLinkGoogleAccount,
                onUnlinkAccount = { accountId -> showUnlinkDialog = accountId },
                onSignOut = onSignOut,
                isDarkMode = isDarkMode,
                onToggleDarkMode = onToggleDarkMode
            )
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}




