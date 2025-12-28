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

enum class BottomNavItem(
    val route: String,
    val icon: ImageVector,
    val label: String
) {
    Home("home", Icons.Default.Home, "Home"),
    Topic("topic", Icons.Default.Topic, "Topic"),
    Community("community", Icons.Outlined.Forum, "Community"),
    Profile("profile", Icons.Default.Person, "Profile")
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
            NavigationBarItem(
                selected = isSelected,
                onClick = { onNavigate(item.route) },
                icon = {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.label
                    )
                },
                label = { Text(text = item.label) },
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
