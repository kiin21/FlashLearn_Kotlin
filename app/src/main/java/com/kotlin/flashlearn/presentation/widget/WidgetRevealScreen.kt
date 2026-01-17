package com.kotlin.flashlearn.presentation.widget

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.kotlin.flashlearn.domain.widget.WidgetState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WidgetRevealScreen(
    onCompleted: () -> Unit,
    onBack: () -> Unit,
    vm: WidgetRevealViewModel = hiltViewModel()
) {
    val state by vm.state.collectAsState()
    val busy by vm.busy.collectAsState()

    LaunchedEffect(state) {
        if (state is WidgetState.DoneToday) {
            onCompleted()
        }
    }

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
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            when (state) {
                is WidgetState.SignedOut -> {
                    Text("You need to log in to use Daily Widget.")
                }

                is WidgetState.Exhausted -> {
                    val s = state as WidgetState.Exhausted
                    Text("No new words today.")
                }

                is WidgetState.CardHidden -> {
                    val s = state as WidgetState.CardHidden
                    Text("Today's word", style = MaterialTheme.typography.labelLarge)
                    Text(
                        text = s.flashcard.word,
                        style = MaterialTheme.typography.headlineLarge
                    )

                    Button(
                        onClick = { vm.reveal() },
                        enabled = !busy,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Reveal")
                    }
                }

                is WidgetState.CardRevealed -> {
                    val s = state as WidgetState.CardRevealed
                    Text("Today's word", style = MaterialTheme.typography.labelLarge)
                    Text(
                        text = s.flashcard.word,
                        style = MaterialTheme.typography.headlineLarge
                    )
                    if (s.flashcard.ipa.isNotBlank()) {
                        Text("/${s.flashcard.ipa}/", style = MaterialTheme.typography.bodyMedium)
                    }
                    Divider()
                    if (s.flashcard.definition.isNotBlank()) {
                        Text("Definition", style = MaterialTheme.typography.labelMedium)
                        Text(s.flashcard.definition)
                    }
                    if (s.flashcard.exampleSentence.isNotBlank()) {
                        Spacer(Modifier.height(8.dp))
                        Text("Example", style = MaterialTheme.typography.labelMedium)
                        Text(s.flashcard.exampleSentence)
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedButton(
                            onClick = { vm.missed() },
                            enabled = !busy,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Missed")
                        }
                        Button(
                            onClick = { vm.gotIt() },
                            enabled = !busy,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Got it")
                        }
                    }
                }

                is WidgetState.DoneToday -> {
                    Text("Done today 🎉")
                }
            }
        }
    }
}