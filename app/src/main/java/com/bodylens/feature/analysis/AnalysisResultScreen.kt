package com.bodylens.feature.analysis

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.bodylens.domain.model.BodyComposition
import com.bodylens.domain.model.BodyMeasurements

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalysisResultScreen(
    onDone: () -> Unit,
    viewModel: AnalysisViewModel = hiltViewModel(),
) {
    val result by viewModel.result.collectAsState()
    Scaffold(topBar = { TopAppBar(title = { Text("Your result") }) }) { padding ->
        Column(
            Modifier.padding(padding).padding(24.dp).fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            val r = result
            if (r == null) {
                Text("No analysis available.")
            } else {
                r.composition?.let { CompositionCard(it) }
                MeasurementsCard(r.measurements)
                Text(
                    "This is an on-device estimate for tracking trends over time, not a medical " +
                        "measurement. Keep lighting, pose and distance consistent between sessions.",
                    style = MaterialTheme.typography.bodySmall,
                )
                Spacer(Modifier.weight(1f))
                Button(onClick = onDone, modifier = Modifier.fillMaxWidth()) { Text("Save & done") }
            }
        }
    }
}

@Composable
private fun CompositionCard(c: BodyComposition) {
    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Estimated body fat", style = MaterialTheme.typography.labelLarge)
            Text(
                "${format(c.lowerBound)}%  –  ${format(c.upperBound)}%",
                style = MaterialTheme.typography.headlineMedium,
            )
            Text(
                "Midpoint ${format(c.bodyFatPercent)}% · ${c.category.name.lowercase().replace('_', ' ')}",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun MeasurementsCard(m: BodyMeasurements) {
    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Estimated measurements", style = MaterialTheme.typography.labelLarge)
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) { Text("Waist"); Text(cm(m.waistCm)) }
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) { Text("Hip"); Text(cm(m.hipCm)) }
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) { Text("Neck"); Text(cm(m.neckCm)) }
        }
    }
}

private fun format(v: Double) = ((v * 10).toInt() / 10.0).toString()
private fun cm(v: Double?) = v?.let { "${format(it)} cm" } ?: "—"
