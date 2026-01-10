package com.kotlin.flashlearn.presentation.profile.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kotlin.flashlearn.R
import com.kotlin.flashlearn.domain.model.LinkedAccount
import com.kotlin.flashlearn.ui.theme.FlashRed

@Composable
fun LinkedAccountsSection(
    linkedAccounts: List<LinkedAccount>,
    onLinkGoogleAccount: () -> Unit,
    onUnlinkAccount: (String) -> Unit,
    isLinkingInProgress: Boolean
) {
     ExpandableProfileSection(
        icon = Icons.Default.Link,
        title = stringResource(R.string.linked_accounts),
        subtitle = if (linkedAccounts.isNotEmpty()) "${linkedAccounts.size} connected" else null,
        iconTint = MaterialTheme.colorScheme.onSurfaceVariant
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            linkedAccounts.forEach { account ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp, horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Google Logo
                    Icon(
                        painter = painterResource(R.drawable.ic_google_logo),
                        contentDescription = "Google",
                        modifier = Modifier.size(24.dp),
                        tint = Color.Unspecified // Keep original colors
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "Google", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                        Text(text = account.email, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = { onUnlinkAccount(account.accountId) }) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = stringResource(R.string.delete),
                            tint = FlashRed
                        )
                    }
                }
                HorizontalDivider(modifier = Modifier.padding(start = 56.dp, end = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            }

            // Link new account button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onLinkGoogleAccount)
                    .padding(vertical = 12.dp, horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                 Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = if (isLinkingInProgress) stringResource(R.string.linking) else stringResource(R.string.link_google_account),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
