package com.bodylens.feature.plan

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.draw.clip
import coil.compose.AsyncImage
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.bodylens.core.ui.components.MuscleMap
import com.bodylens.domain.model.ExercisePlan
import com.bodylens.domain.model.MuscleVolume
import com.bodylens.domain.model.WorkoutDay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanScreen(
    onBack: () -> Unit,
    onExerciseClick: (String) -> Unit = {},
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
                item { MusclesWorkedCard(p) }
                items(p.days) { DayCard(it, onExerciseClick) }
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
private fun MusclesWorkedCard(plan: ExercisePlan) {
    val rx = remember(plan) { plan.days.flatMap { it.prescriptions } }
    val primary = remember(rx) { rx.flatMap { it.exercise.primaryMuscles }.toSet() }
    val secondary = remember(rx) { rx.flatMap { it.exercise.secondaryMuscles }.toSet() }
    if (primary.isEmpty() && secondary.isEmpty()) return
    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Muscles worked", style = MaterialTheme.typography.titleMedium)
            MuscleMap(
                primaryMuscles = primary,
                secondaryMuscles = secondary,
                modifier = Modifier.fillMaxWidth(),
            )
        }
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
private fun DayCard(day: WorkoutDay, onExerciseClick: (String) -> Unit) {
    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(day.dayLabel, style = MaterialTheme.typography.titleMedium)
            day.prescriptions.forEach { rx -> ExerciseRow(rx, onExerciseClick) }
        }
    }
}

@Composable
private fun ExerciseRow(rx: com.bodylens.domain.model.ExercisePrescription, onExerciseClick: (String) -> Unit) {
    val ex = rx.exercise
    Row(
        Modifier
            .fillMaxWidth()
            .clickable { onExerciseClick(ex.id) },
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
    ) {
        ex.image?.let { path ->
            AsyncImage(
                model = "file:///android_asset/$path",
                contentDescription = ex.name,
                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                modifier = Modifier
                    .size(56.dp)
                    .clip(MaterialTheme.shapes.small)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
            )
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(ex.name, style = MaterialTheme.typography.bodyMedium)
            val tags = listOfNotNull(ex.bodyPart, ex.target)
                .map(::titleCase)
                .distinct()
                .joinToString(" · ")
            if (tags.isNotEmpty()) {
                Text(
                    tags,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Text("${rx.sets} × ${rx.reps}", style = MaterialTheme.typography.bodyMedium)
    }
}
