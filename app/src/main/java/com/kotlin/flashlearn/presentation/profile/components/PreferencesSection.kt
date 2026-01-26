package com.kotlin.flashlearn.presentation.profile.components

import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LockReset
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kotlin.flashlearn.R
import com.kotlin.flashlearn.domain.model.LinkedAccount
import com.kotlin.flashlearn.ui.theme.FlashRed
import com.kotlin.flashlearn.ui.theme.FlashRedLight
import com.kotlin.flashlearn.util.LanguageManager

private data class Language(
    val code: String,
    @StringRes val nameResId: Int,
    val flag: String
)

@Composable
fun PreferencesSection(
    linkedAccounts: List<LinkedAccount>,
    isLinkingInProgress: Boolean,
    onChangePassword: () -> Unit,
    onLanguageChange: (String) -> Unit,
    onLinkGoogleAccount: () -> Unit,
    onUnlinkAccount: (String) -> Unit,
    onSignOut: () -> Unit,
    onDeleteAccount: () -> Unit,
    isDarkMode: Boolean = false,
    onToggleDarkMode: (Boolean) -> Unit = {}
) {
    val languages = remember {
        listOf(
            Language("en", R.string.english, "🇺🇸"),
            Language("vi", R.string.vietnamese, "🇻🇳")
        )
    }
    
    val context = LocalContext.current
    val languageManager = remember {
        // In a real app we'd inject this, but for now we can grab it from Activity/App or Hilt
        // Since we are in a composable, using the one passed to NavHost or grabbing from Hilt entry point is better.
        // For simplicity assuming it's available via composition local or Hilt.
        // As a workaround, we'll rely on the fact that selectedLanguage state is managed below.
        LanguageManager(context)
    }

    // Initialize state based on current language
    var selectedLanguage by remember {
        val currentLangCode = languageManager.getLanguage()
        mutableStateOf(languages.find { it.code == currentLangCode } ?: languages.first())
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        
        // --- General Section ---
        ProfileSectionHeader(stringResource(R.string.general), MaterialTheme.colorScheme.primary)
        
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            ),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                // Dark Mode
                ProfileRowSwitch(
                    icon = Icons.Default.NightsStay,
                    text = stringResource(R.string.dark_mode),
                    isChecked = isDarkMode,
                    onCheckedChange = { onToggleDarkMode(it) },
                    iconTint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                // Language Selection
                ExpandableProfileSection(
                    icon = Icons.Default.Language,
                    title = stringResource(R.string.language),
                    iconTint = MaterialTheme.colorScheme.onSurfaceVariant
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        languages.forEachIndexed { index, language ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onLanguageChange(language.code) }
                                    .padding(vertical = 12.dp, horizontal = 16.dp), // Added horizontal padding for expanded items
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${language.flag}  ${stringResource(language.nameResId)}",
                                    style = MaterialTheme.typography.bodyLarge,
                                    modifier = Modifier.weight(1f)
                                )
                                
                                if (language == selectedLanguage) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                            if (index < languages.size - 1) {
                                HorizontalDivider(modifier = Modifier.padding(start = 56.dp, end = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                            }
                        }
                    }
                }
            }
        }

        // --- Account Section ---
        ProfileSectionHeader(stringResource(R.string.account), MaterialTheme.colorScheme.primary)
        
        Card(
             colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            ),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
             Column(modifier = Modifier.padding(vertical = 8.dp)) {
                 // Linked Accounts
                LinkedAccountsSection(
                    linkedAccounts = linkedAccounts,
                    onLinkGoogleAccount = onLinkGoogleAccount,
                    onUnlinkAccount = onUnlinkAccount,
                    isLinkingInProgress = isLinkingInProgress
                )

                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                // Change Password
                ProfileRowNavigation(
                    icon = Icons.Default.LockReset,
                    text = stringResource(R.string.change_password),
                    onClick = onChangePassword,
                    iconTint = MaterialTheme.colorScheme.onSurfaceVariant
                )
             }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // --- Actions Section ---
        // --- Actions Section ---
        Button(
            onClick = onSignOut,
            modifier = Modifier
                .fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurface
            ),
            shape = RoundedCornerShape(12.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 12.dp),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
        ) {
            Text(
                text = stringResource(R.string.log_out),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold
            )
        }
        
        // --- Danger Zone ---
        ProfileSectionHeader(stringResource(R.string.danger_zone), FlashRed)
        
        androidx.compose.material3.OutlinedButton(
            onClick = onDeleteAccount,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = FlashRedLight.copy(alpha = 0.05f),
                contentColor = FlashRed
            ),
            border = BorderStroke(1.dp, FlashRed.copy(alpha = 0.5f)),
            shape = RoundedCornerShape(12.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 12.dp)
        ) {
             Icon(
                imageVector = Icons.Default.DeleteOutline,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.delete_account),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold
            )
        }
        
        Spacer(modifier = Modifier.height(32.dp))
    }
}
