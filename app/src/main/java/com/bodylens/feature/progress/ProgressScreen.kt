package com.bodylens.feature.progress

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.bodylens.domain.model.ProgressEntry
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgressScreen(
    onCapture: () -> Unit,
    onPlan: () -> Unit,
    onSettings: () -> Unit,
    viewModel: ProgressViewModel = hiltViewModel(),
) {
    val entries by viewModel.entries.collectAsState()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Progress") },
                actions = {
                    IconButton(onClick = onPlan) { Icon(Icons.Default.FitnessCenter, "Plan") }
                    IconButton(onClick = onSettings) { Icon(Icons.Default.Settings, "Settings") }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onCapture,
                icon = { Icon(Icons.Default.Add, null) },
                text = { Text("New capture") },
            )
        },
    ) { padding ->
        if (entries.isEmpty()) {
            Box(Modifier.padding(padding).fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                Text("No captures yet. Tap “New capture” to start your timeline.")
            }
        } else {
            LazyColumn(
                Modifier.padding(padding).fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(entries) { EntryRow(it) }
            }
        }
    }
}

@Composable
private fun EntryRow(entry: ProgressEntry) {
    ElevatedCard(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(16.dp).fillMaxWidth(), Arrangement.SpaceBetween) {
            Column {
                Text(dateFmt.format(Date(entry.createdAtEpochMs)), style = MaterialTheme.typography.titleMedium)
                Text(
                    "Waist ${entry.measurements.waistCm?.let { "${it.toInt()} cm" } ?: "—"}",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            entry.bodyFatPercent?.let {
                Text("${it}%", style = MaterialTheme.typography.titleLarge)
            }
        }
    }
}

private val dateFmt = SimpleDateFormat("d MMM yyyy", Locale.getDefault())
