package com.kotlin.flashlearn.presentation.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Topic
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.kotlin.flashlearn.ui.theme.FlashRed
import androidx.annotation.StringRes
import androidx.compose.ui.res.stringResource
import com.kotlin.flashlearn.R

enum class BottomNavItem(
    val route: String,
    val icon: ImageVector,
@StringRes val labelResId: Int
) {
    Home("home", Icons.Default.Home, R.string.nav_home),
    Topic("topic", Icons.Default.Topic, R.string.nav_topic),
    Community("community", Icons.Outlined.Forum, R.string.nav_community),
    Profile("profile", Icons.Default.Person, R.string.nav_profile)
}

@Composable
fun BottomNavBar(
    currentRoute: String,
    onNavigate: (String) -> Unit
) {
    NavigationBar(
        containerColor = Color.White
    ) {
        BottomNavItem.entries.forEach { item ->
            val isSelected = currentRoute == item.route
            val label = stringResource(item.labelResId)
            NavigationBarItem(
                selected = isSelected,
                onClick = { onNavigate(item.route) },
                icon = {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = label
                    )
                },
                label = { Text(text = label) },
                alwaysShowLabel = true,
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = FlashRed,
                    selectedTextColor = FlashRed,
                    indicatorColor = Color.Transparent,
                    unselectedIconColor = Color.Gray,
                    unselectedTextColor = Color.Gray
                )
            )
        }
    }
}
