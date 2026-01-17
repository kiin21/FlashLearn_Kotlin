package com.kotlin.flashlearn.presentation.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LockReset
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.annotation.StringRes
import androidx.compose.material3.HorizontalDivider
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.ui.draw.rotate
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Add
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.kotlin.flashlearn.domain.model.UserData
import com.kotlin.flashlearn.presentation.components.BottomNavBar
import com.kotlin.flashlearn.presentation.navigation.Route
import com.kotlin.flashlearn.ui.theme.FlashLightGrey
import com.kotlin.flashlearn.ui.theme.FlashRed
import com.kotlin.flashlearn.ui.theme.FlashRedLight
import java.util.Locale
import com.kotlin.flashlearn.util.LanguageManager
import com.kotlin.flashlearn.R
import kotlinx.coroutines.launch
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import com.kotlin.flashlearn.presentation.profile.components.ChangePasswordDialog
import com.kotlin.flashlearn.presentation.profile.components.PreferencesSection
import com.kotlin.flashlearn.presentation.profile.components.UserInfoSection
import com.kotlin.flashlearn.presentation.profile.components.ProfilePictureDialog
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
    onDeleteAccount: () -> Unit = {},
    onChangePassword: (oldPassword: String, newPassword: String, onResult: (success: Boolean, error: String?) -> Unit) -> Unit = { _, _, _ -> },
    onLanguageChange: (String) -> Unit = {},
    isLinkingInProgress: Boolean = false,
    modifier: Modifier = Modifier
) {
    val snackbarHostState = remember { SnackbarHostState() }
    rememberCoroutineScope()
    var showUnlinkDialog by remember { mutableStateOf<String?>(null) } // Store account ID to unlink
    var showDeleteAccountDialog by remember { mutableStateOf(false) }
    var showChangePasswordDialog by remember { mutableStateOf(false) }
    var showProfilePictureDialog by remember { mutableStateOf(false) }
    
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
    
    if (showDeleteAccountDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showDeleteAccountDialog = false },
            title = { Text(stringResource(R.string.delete_account_title)) },
            text = { Text(stringResource(R.string.delete_account_confirm)) },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteAccountDialog = false
                    onDeleteAccount()
                }) {
                    Text(stringResource(R.string.delete), color = FlashRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAccountDialog = false }) {
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
                onDeleteAccount = { showDeleteAccountDialog = true }
            )
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}




