package com.kotlin.flashlearn.presentation.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.outlined.Book
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kotlin.flashlearn.domain.model.User
import com.kotlin.flashlearn.presentation.components.BottomNavBar
import com.kotlin.flashlearn.ui.theme.FlashBlack
import com.kotlin.flashlearn.ui.theme.FlashDarkGrey
import com.kotlin.flashlearn.ui.theme.FlashGreen
import com.kotlin.flashlearn.ui.theme.FlashGrey
import com.kotlin.flashlearn.ui.theme.FlashLightGrey
import com.kotlin.flashlearn.ui.theme.FlashRed
import com.kotlin.flashlearn.ui.theme.FlashRedLight
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(
    userData: User?,
    onNavigateToProfile: () -> Unit, // Temporarily navigate to profile
    onNavigateToTopic: () -> Unit,
    onNavigateToLearningSession: (String) -> Unit = {} // Temporary for demo
) {
    Scaffold(
        bottomBar = {
            BottomNavBar(
                currentRoute = "home",
                onNavigate = { route ->
                    if (route == "profile") onNavigateToProfile()
                    if (route == "topic") onNavigateToTopic()
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            HeaderSection(userData)
            Spacer(modifier = Modifier.height(24.dp))

            // Exam Date Card
            ExamDateCard(userData)
            Spacer(modifier = Modifier.height(24.dp))

            // Daily Word
            DailyWordSection()
            Spacer(modifier = Modifier.height(24.dp))

            // Continue Learning
            ContinueLearningSection(
                onStartLearning = { onNavigateToLearningSession("env_science_101") }
            )
            Spacer(modifier = Modifier.height(24.dp))

            // Recommended
            RecommendedSection()
        }
    }
}

@Composable
fun HeaderSection(user: User?) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "Hello, ${user?.displayName?.split(" ")?.firstOrNull() ?: "Friend"}!",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Let's keep the streak alive.",
                style = MaterialTheme.typography.bodyMedium,
                color = FlashGrey
            )
        }
        
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFFFFF3E0))
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.LocalFireDepartment,
                contentDescription = null,
                tint = Color(0xFFFF9800),
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "${user?.streak ?: 0} DAYS",
                color = Color(0xFFFF9800),
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
fun ExamDateCard(user: User?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = FlashDarkGrey),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "VSTEP EXAM DATE",
                    color = FlashGrey,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                val date = user?.examDate?.let { Date(it) } ?: Date()
                val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.US)
                Text(
                    text = dateFormat.format(date), // Mock date logic
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .background(Color(0xFF333333), RoundedCornerShape(12.dp))
                    .padding(12.dp)
            ) {
                Text(
                    text = "14", // days left
                    color = FlashRed,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Days Left",
                    color = FlashGrey,
                    fontSize = 10.sp
                )
            }
        }
    }
}

@Composable
fun DailyWordSection() {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Daily Word",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "View Archive",
                color = FlashRed,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F9FE)), // Light blueish
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "B2 LEVEL",
                        color = FlashRed,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .background(FlashRedLight, RoundedCornerShape(4.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                    
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(FlashRedLight, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.VolumeUp,
                            contentDescription = "Pronounce",
                            tint = FlashRed
                        )
                    }
                }
                
                Text(
                    text = "Resilient",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = FlashBlack
                )
                Text(
                    text = "/rɪˈzɪl.jənt/",
                    fontSize = 14.sp,
                    color = FlashGrey,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                Text(
                    text = "Able to withstand or recover quickly from difficult conditions.",
                    color = FlashDarkGrey,
                    lineHeight = 20.sp
                )
            }
        }
    }
}

@Composable
fun ContinueLearningSection(
    onStartLearning: () -> Unit = {}
) {
    Column {
        Text(
            text = "Continue Learning",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(12.dp))
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = FlashLightGrey),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .background(FlashRedLight, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "75%",
                        color = FlashRed,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "B1 Environment",
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { 0.75f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = FlashRed,
                        trackColor = Color.LightGray
                    )
                }
                IconButton(onClick = onStartLearning) {
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "Continue"
                    )
                }
            }
        }
    }
}

@Composable
fun RecommendedSection() {
    Column {
        Text(
            text = "Recommended for You",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(12.dp))
        
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(3) { index ->
                RecommendedCard(index)
            }
        }
    }
}

@Composable
fun RecommendedCard(index: Int) {
    val titles = listOf("VSTEP C1 Vocab", "Listening Part 2", "Writing Task 1")
    val counts = listOf("20 words", "20 words", "20 words")
    
    Card(
        modifier = Modifier.width(140.dp),
        colors = CardDefaults.cardColors(containerColor = FlashLightGrey),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(Color(0xFFE8F5E9), RoundedCornerShape(8.dp)), // Light Green
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Book,
                    contentDescription = null,
                    tint = FlashGreen
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = titles.getOrElse(index) { "Collection" },
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = counts.getOrElse(index) { "20 words" },
                color = FlashGrey,
                fontSize = 12.sp
            )
        }
    }
}
