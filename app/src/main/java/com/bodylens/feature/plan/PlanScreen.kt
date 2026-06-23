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
import com.bodylens.domain.model.ExercisePlan
import com.bodylens.domain.model.MuscleVolume
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
                item { PlanHeader(p) }
                items(p.days) { DayCard(it) }
                if (p.weeklyVolume.isNotEmpty()) {
                    item { WeeklyVolumeCard(p.weeklyVolume) }
                }
            }
        }
    }
}

private fun titleCase(value: String) =
    value.lowercase().replace('_', ' ').replaceFirstChar(Char::uppercase)

@Composable
private fun PlanHeader(plan: ExercisePlan) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            "${titleCase(plan.goal.name)} · ${plan.daysPerWeek} days/week",
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            "Emphasis: ${titleCase(plan.emphasis.name)} · Cardio: ${plan.cardioZone2Minutes} min/week (Zone 2)",
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun WeeklyVolumeCard(volume: List<MuscleVolume>) {
    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Weekly volume (sets per muscle)", style = MaterialTheme.typography.titleMedium)
            volume.forEach { v ->
                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                    Text(titleCase(v.muscle.name), style = MaterialTheme.typography.bodyMedium)
                    Text(
                        "${v.plannedSets} / ${v.targetSets}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (v.meetsTarget) MaterialTheme.colorScheme.onSurface
                        else MaterialTheme.colorScheme.error,
                    )
                }
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
