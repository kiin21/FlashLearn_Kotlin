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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.kotlin.flashlearn.domain.model.UserData
import com.kotlin.flashlearn.presentation.components.BottomNavBar
import com.kotlin.flashlearn.presentation.navigation.Route
import com.kotlin.flashlearn.ui.theme.FlashLightGrey
import com.kotlin.flashlearn.ui.theme.FlashRed
import com.kotlin.flashlearn.ui.theme.FlashRedLight
import kotlinx.coroutines.launch
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts

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
    isLinkingInProgress: Boolean = false,
    modifier: Modifier = Modifier
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var showUnlinkDialog by remember { mutableStateOf<String?>(null) } // Store account ID to unlink
    var showDeleteAccountDialog by remember { mutableStateOf(false) }
    var showChangePasswordDialog by remember { mutableStateOf(false) }
    
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
    
    val showNotImplementedMessage: (String) -> Unit = { featureName ->
        scope.launch {
            snackbarHostState.showSnackbar("$featureName is coming soon!")
        }
    }

    if (showUnlinkDialog != null) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showUnlinkDialog = null },
            title = { Text("Unlink Account") },
            text = { Text("Are you sure you want to unlink this Google account?") },
            confirmButton = {
                TextButton(onClick = {
                    val accountId = showUnlinkDialog
                    showUnlinkDialog = null
                    accountId?.let { onUnlinkGoogleAccount(it) }
                }) {
                    Text("Unlink", color = FlashRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { showUnlinkDialog = null }) {
                    Text("Cancel")
                }
            }
        )
    }
    
    if (showDeleteAccountDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showDeleteAccountDialog = false },
            title = { Text("Delete Account") },
            text = { Text("Are you sure you want to delete your account? This action cannot be undone and all your data will be permanently deleted.") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteAccountDialog = false
                    onDeleteAccount()
                }) {
                    Text("Delete", color = FlashRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAccountDialog = false }) {
                    Text("Cancel")
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
                text = "Profile",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(top = 16.dp, bottom = 24.dp)
            )

            UserInfoSection(
                userData = userData,
                availableEmails = availableEmails,
                onEditProfilePicture = { 
                    imagePickerLauncher.launch(
                        androidx.activity.result.PickVisualMediaRequest(
                            ActivityResultContracts.PickVisualMedia.ImageOnly
                        )
                    )
                },
                onUpdateEmail = onUpdateEmail
            )
            Spacer(modifier = Modifier.height(32.dp))

            PreferencesSection(
                onChangePassword = { showChangePasswordDialog = true }
            )
            Spacer(modifier = Modifier.height(32.dp))

            LinkedAccountsSection(
                linkedAccounts = linkedAccounts,
                onLinkGoogleAccount = onLinkGoogleAccount,
                onUnlinkAccount = { accountId -> showUnlinkDialog = accountId },
                isLinkingInProgress = isLinkingInProgress
            )
            Spacer(modifier = Modifier.height(32.dp))

            AccountSection(
                onSignOut = onSignOut,
                onDeleteAccount = { showDeleteAccountDialog = true }
            )
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun UserInfoSection(
    userData: UserData?,
    availableEmails: List<String> = emptyList(),
    onEditProfilePicture: () -> Unit = {},
    onUpdateEmail: (String) -> Unit = {}
) {
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box {
            AsyncImage(
                model = userData?.profilePictureUrl,
                contentDescription = "Profile picture",
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
                    .border(2.dp, MaterialTheme.colorScheme.background, CircleShape)
                    .align(Alignment.BottomEnd)
                    .clickable { onEditProfilePicture() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Edit Profile Picture",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = userData?.username ?: "Guest",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        
        Box {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clickable(enabled = availableEmails.size > 1) { expanded = true }
                    .padding(4.dp)
            ) {
                Text(
                    text = userData?.email ?: "No email",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (availableEmails.size > 1) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Change email",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
            
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                availableEmails.forEach { email ->
                    DropdownMenuItem(
                        text = { Text(text = email) },
                        onClick = {
                            onUpdateEmail(email)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun PreferencesSection(
    onChangePassword: () -> Unit = {}
) {
    var notificationsEnabled by remember { mutableStateOf(true) }
    var darkModeEnabled by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "PREFERENCES",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 8.dp, bottom = 8.dp)
        )
        
        ProfileRowSwitch(
            icon = Icons.Default.Notifications,
            text = "Notifications",
            isChecked = notificationsEnabled,
            onCheckedChange = { notificationsEnabled = it }
        )
        Spacer(modifier = Modifier.height(12.dp))
        
        ProfileRowSwitch(
            icon = Icons.Default.NightsStay,
            text = "Dark Mode",
            isChecked = darkModeEnabled,
            onCheckedChange = { darkModeEnabled = it }
        )
        Spacer(modifier = Modifier.height(12.dp))
        
        ProfileRowNavigation(
            icon = Icons.Default.LockReset,
            text = "Change Password",
            onClick = onChangePassword
        )
    }
}

@Composable
private fun LinkedAccountsSection(
    linkedAccounts: List<com.kotlin.flashlearn.domain.model.LinkedAccount>,
    onLinkGoogleAccount: () -> Unit,
    onUnlinkAccount: (String) -> Unit,
    isLinkingInProgress: Boolean
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "LINKED ACCOUNTS",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 8.dp, bottom = 8.dp)
        )

        // Add specific "Link Google" button always visible to allow adding MORE accounts
        ProfileRowNavigation(
            icon = Icons.Default.Link,
            text = if (isLinkingInProgress) "Linking..." else "Link Google Account",
            onClick = onLinkGoogleAccount
        )
        Spacer(modifier = Modifier.height(8.dp))

        // List existing linked accounts
        linkedAccounts.forEach { account ->
            LinkedAccountRow(
                email = account.email,
                onUnlink = { onUnlinkAccount(account.accountId) }
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun LinkedAccountRow(
    email: String,
    onUnlink: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { }, // Non-clickable card, but row elements are
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = Color(0xFF4CAF50),
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                 Text(
                    text = "Google",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = email,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            androidx.compose.material3.IconButton(onClick = onUnlink) {
                Icon(
                    imageVector = Icons.Default.DeleteOutline,
                    contentDescription = "Unlink",
                    tint = FlashRed
                )
            }
        }
    }
}

@Composable
private fun AccountSection(
    onSignOut: () -> Unit,
    onDeleteAccount: () -> Unit = {}
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "ACCOUNT",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 8.dp, bottom = 8.dp)
        )
        
        ProfileRowNavigation(
            icon = Icons.AutoMirrored.Filled.ExitToApp,
            text = "Log Out",
            onClick = onSignOut
        )
        
        Spacer(modifier = Modifier.height(12.dp))

        ProfileRowNavigation(
            icon = Icons.Default.DeleteOutline,
            text = "Delete Account",
            textColor = FlashRed,
            iconTint = FlashRed,
            cardContainerColor = FlashRedLight.copy(alpha = 0.3f),
            iconBackgroundColor = Color.Transparent,
            onClick = onDeleteAccount
        )
    }
}

@Composable
private fun ProfileRowNavigation(
    icon: ImageVector,
    text: String,
    textColor: Color = MaterialTheme.colorScheme.onSurface,
    iconTint: Color = MaterialTheme.colorScheme.onSurface,
    cardContainerColor: Color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
    iconBackgroundColor: Color = MaterialTheme.colorScheme.surfaceVariant, // Slightly darker than card
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardContainerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp) // Slightly bigger touch target
                    .background(
                        if (iconBackgroundColor == Color.Transparent) Color.Transparent else iconBackgroundColor, 
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon, 
                    contentDescription = text, 
                    tint = iconTint,
                    modifier = Modifier.size(20.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = textColor,
                modifier = Modifier.weight(1f)
            )
            
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
        }
    }
}

@Composable
private fun ProfileRowInfo(
    icon: ImageVector,
    text: String,
    textColor: Color = MaterialTheme.colorScheme.onSurface,
    iconTint: Color = MaterialTheme.colorScheme.onSurface
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = text,
                    tint = iconTint,
                    modifier = Modifier.size(20.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = textColor
            )
        }
    }
}

@Composable
private fun ProfileRowSwitch(
    icon: ImageVector,
    text: String,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!isChecked) },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon, 
                    contentDescription = text, 
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(20.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Switch(
                checked = isChecked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = FlashRed,
                    uncheckedThumbColor = Color.White,
                    uncheckedTrackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                    uncheckedBorderColor = Color.Transparent
                ),
                modifier = Modifier.height(24.dp)
            )
        }
    }
}

@Composable
private fun ChangePasswordDialog(
    onDismiss: () -> Unit,
    onSubmit: (oldPassword: String, newPassword: String, onResult: (success: Boolean, error: String?) -> Unit) -> Unit
) {
    var oldPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var passwordVisible by remember { mutableStateOf(false) }

    androidx.compose.material3.AlertDialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        title = { Text("Change Password") },
        text = {
            Column {
                if (errorMessage != null) {
                    Text(
                        text = errorMessage!!,
                        color = FlashRed,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
                
                androidx.compose.material3.OutlinedTextField(
                    value = oldPassword,
                    onValueChange = { oldPassword = it; errorMessage = null },
                    label = { Text("Current Password") },
                    singleLine = true,
                    enabled = !isLoading,
                    visualTransformation = if (passwordVisible) 
                        androidx.compose.ui.text.input.VisualTransformation.None 
                    else 
                        androidx.compose.ui.text.input.PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = if (passwordVisible) "Hide password" else "Show password"
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                androidx.compose.material3.OutlinedTextField(
                    value = newPassword,
                    onValueChange = { newPassword = it; errorMessage = null },
                    label = { Text("New Password") },
                    singleLine = true,
                    enabled = !isLoading,
                    visualTransformation = if (passwordVisible) 
                        androidx.compose.ui.text.input.VisualTransformation.None 
                    else 
                        androidx.compose.ui.text.input.PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = if (passwordVisible) "Hide password" else "Show password"
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                androidx.compose.material3.OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it; errorMessage = null },
                    label = { Text("Confirm New Password") },
                    singleLine = true,
                    enabled = !isLoading,
                    visualTransformation = if (passwordVisible) 
                        androidx.compose.ui.text.input.VisualTransformation.None 
                    else 
                        androidx.compose.ui.text.input.PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = if (passwordVisible) "Hide password" else "Show password"
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    when {
                        oldPassword.isBlank() -> errorMessage = "Please enter current password"
                        newPassword.isBlank() -> errorMessage = "Please enter new password"
                        newPassword != confirmPassword -> errorMessage = "Passwords do not match"
                        newPassword.length < 8 -> errorMessage = "Password must be at least 8 characters"
                        else -> {
                            isLoading = true
                            errorMessage = null
                            onSubmit(oldPassword, newPassword) { success, error ->
                                isLoading = false
                                if (!success) {
                                    errorMessage = error ?: "Failed to change password"
                                }
                            }
                        }
                    }
                },
                enabled = !isLoading
            ) {
                if (isLoading) {
                    androidx.compose.material3.CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Change", color = MaterialTheme.colorScheme.primary)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isLoading) {
                Text("Cancel")
            }
        }
    )
}




