package com.kotlin.flashlearn.presentation.dailyword_archive

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyWordArchiveScreen(
    onBack: () -> Unit
) {
    val vm: DailyWordArchiveViewModel = hiltViewModel()
    val state by vm.state.collectAsState()

    val ctx = LocalContext.current

    var showFromPicker by remember { mutableStateOf(false) }
    var showToPicker by remember { mutableStateOf(false) }

    if (state.error != null) {
        LaunchedEffect(state.error) {
            Toast.makeText(ctx, state.error, Toast.LENGTH_LONG).show()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Daily Word Archive",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(onClick = { vm.clearFilters() }) {
                        Text("Clear", fontWeight = FontWeight.Bold)
                    }
                }
            )
        }
    ) { pv ->
        Column(
            modifier = Modifier
                .padding(pv)
                .padding(16.dp)
                .fillMaxSize()
        ) {
            OutlinedTextField(
                value = state.query,
                onValueChange = vm::setQuery,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Search word / meaning") }
            )

            Spacer(Modifier.height(12.dp))

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                AssistChip(
                    onClick = { showFromPicker = true },
                    label = { Text("From: ${state.fromDate ?: "--"}") }
                )
                AssistChip(
                    onClick = { showToPicker = true },
                    label = { Text("To: ${state.toDate ?: "--"}") }
                )
            }

            Spacer(Modifier.height(12.dp))

            if (state.isLoading) {
                LinearProgressIndicator(Modifier.fillMaxWidth())
                Spacer(Modifier.height(12.dp))
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(state.items) { item ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.large
                    ) {
                        Column(Modifier.padding(14.dp)) {
                            Text(item.dateKey, fontWeight = FontWeight.SemiBold)
                            Spacer(Modifier.height(6.dp))
                            Text(
                                item.word,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            if (!item.ipa.isNullOrBlank()) Text(
                                item.ipa ?: "",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (!item.meaning.isNullOrBlank()) Text(item.meaning ?: "")
                        }
                    }
                }
            }
        }
    }

    if (showFromPicker) {
        val pickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showFromPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    vm.setFromDateMillis(pickerState.selectedDateMillis)
                    showFromPicker = false
                }) { Text("Set") }
            },
            dismissButton = {
                TextButton(onClick = { showFromPicker = false }) { Text("Cancel") }
            }
        ) { DatePicker(state = pickerState) }
    }

    if (showToPicker) {
        val pickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showToPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    vm.setToDateMillis(pickerState.selectedDateMillis)
                    showToPicker = false
                }) { Text("Set") }
            },
            dismissButton = {
                TextButton(onClick = { showToPicker = false }) { Text("Cancel") }
            }
        ) { DatePicker(state = pickerState) }
    }
}
