package com.kotlin.flashlearn.presentation.widget

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.kotlin.flashlearn.domain.widget.WidgetState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WidgetCompletedScreen(
    onBackHome: () -> Unit,
    onBack: () -> Unit,
    vm: WidgetCompletedViewModel = hiltViewModel()
) {
    val state by vm.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Daily Word") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.Close, contentDescription = "Close Selection")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            when (state) {
                is WidgetState.DoneToday -> {
                    val s = state as WidgetState.DoneToday

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("🎉 Complete the Daily Word!", style = MaterialTheme.typography.headlineSmall)
                            Text(
                                "You have completed the widget today (${s.date}).",
                                style = MaterialTheme.typography.bodyMedium
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                StatCard(
                                    title = "Current streak",
                                    value = "${s.streakCurrent} days",
                                    icon = { Icon(Icons.Default.LocalFireDepartment, contentDescription = null) },
                                    modifier = Modifier.weight(1f)
                                )
                                StatCard(
                                    title = "Best streak",
                                    value = "${s.streakBest} days",
                                    icon = { Icon(Icons.Default.LocalFireDepartment, contentDescription = null) },
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            Button(
                                onClick = onBackHome,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Back to Home")
                            }

                            OutlinedButton(
                                onClick = { vm.refresh() },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Refresh")
                            }
                        }
                    }
                }

                is WidgetState.SignedOut -> {
                    Text("You need to log in to view your streak.")
                }

                is WidgetState.CardHidden,
                is WidgetState.CardRevealed,
                is WidgetState.Exhausted -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("Not finished today", style = MaterialTheme.typography.titleLarge)
                            Text(
                                "Complete the Daily Word to earn a streak.",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Button(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
                                Text("Back")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatCard(
    title: String,
    value: String,
    icon: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier, shape = RoundedCornerShape(16.dp)) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                icon()
                Text(title, style = MaterialTheme.typography.labelLarge)
            }
            Text(value, style = MaterialTheme.typography.headlineSmall)
        }
    }
}