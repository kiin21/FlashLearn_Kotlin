package com.kotlin.flashlearn.presentation.topic

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopicDetailScreen(
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("B1 Environment") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { /* Add new card */ },
                containerColor = Color.Red
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
        ) {
            Text(
                text = "Essential vocabulary for nature",
                color = Color.Gray
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { /* Study */ },
                colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
            ) {
                Text("Study Now")
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Cards in this topic (3)",
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            CardItem("Biodiversity", "noun")
            CardItem("Sustainable", "adj")
            CardItem("Migrate", "verb")
        }
    }
}

@Composable
fun CardItem(word: String, type: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(word, fontWeight = FontWeight.Bold)
                Text(type, fontSize = 12.sp, color = Color.Gray)
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null)
        }
    }
}
