package com.bodylens.feature.plan

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.bodylens.domain.model.WorkoutDay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanScreen(
    onBack: () -> Unit,
    viewModel: PlanViewModel = hiltViewModel(),
) {
    val plan by viewModel.plan.collectAsState()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Your plan") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
                },
            )
        },
    ) { padding ->
        val p = plan
        if (p == null) {
            Box(Modifier.padding(padding).fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                Modifier.padding(padding).fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    Text(
                        "${p.goal.name.lowercase().replace('_', ' ').replaceFirstChar(Char::uppercase)} · ${p.daysPerWeek} days/week",
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
                items(p.days) { DayCard(it) }
            }
        }
    }
}

@Composable
private fun DayCard(day: WorkoutDay) {
    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(day.dayLabel, style = MaterialTheme.typography.titleMedium)
            day.prescriptions.forEach { rx ->
                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                    Text(rx.exercise.name, style = MaterialTheme.typography.bodyMedium)
                    Text("${rx.sets} × ${rx.reps}", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}
