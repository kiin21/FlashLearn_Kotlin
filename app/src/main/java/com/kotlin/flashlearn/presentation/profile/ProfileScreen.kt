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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    userData: UserData?,
    onSignOut: () -> Unit,
    onNavigateToHome: () -> Unit,
    onNavigateToTopic: () -> Unit,
    onNavigateToCommunity: () -> Unit = {},
    isGoogleLinked: Boolean = false,
    onLinkGoogleAccount: () -> Unit = {},
    isLinkingInProgress: Boolean = false,
    modifier: Modifier = Modifier
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    
    val showNotImplementedMessage: (String) -> Unit = { featureName ->
        scope.launch {
            snackbarHostState.showSnackbar("$featureName is coming soon!")
        }
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
                onEditProfilePicture = { showNotImplementedMessage("Edit profile picture") }
            )
            Spacer(modifier = Modifier.height(32.dp))

            PreferencesSection(
                onChangePassword = { showNotImplementedMessage("Change password") }
            )
            Spacer(modifier = Modifier.height(32.dp))

            AccountSection(
                onSignOut = onSignOut,
                onDeleteAccount = { showNotImplementedMessage("Delete account") },
                isGoogleLinked = isGoogleLinked,
                onLinkGoogleAccount = onLinkGoogleAccount,
                isLinkingInProgress = isLinkingInProgress
            )
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}



@Composable
private fun UserInfoSection(
    userData: UserData?,
    onEditProfilePicture: () -> Unit = {}
) {
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
        Text(
            text = userData?.email ?: "No email",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
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
private fun AccountSection(
    onSignOut: () -> Unit,
    onDeleteAccount: () -> Unit = {},
    isGoogleLinked: Boolean = false,
    onLinkGoogleAccount: () -> Unit = {},
    isLinkingInProgress: Boolean = false
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "ACCOUNT",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 8.dp, bottom = 8.dp)
        )
        
        // Link Google Account
        if (isGoogleLinked) {
            ProfileRowInfo(
                icon = Icons.Default.Check,
                text = "Google Account Linked",
                iconTint = Color(0xFF4CAF50),
                textColor = Color(0xFF4CAF50)
            )
        } else {
            ProfileRowNavigation(
                icon = Icons.Default.Link,
                text = if (isLinkingInProgress) "Linking..." else "Link Google Account",
                onClick = onLinkGoogleAccount
            )
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
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
