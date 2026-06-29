package com.bodylens.feature.exercise

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.bodylens.domain.model.Exercise

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ExerciseDetailScreen(
    onBack: () -> Unit,
    viewModel: ExerciseDetailViewModel = hiltViewModel(),
) {
    val exercise by viewModel.exercise.collectAsState()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(exercise?.name?.let(::titleCase) ?: "Exercise") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
                },
            )
        },
    ) { padding ->
        val ex = exercise
        if (ex == null) {
            Box(Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                ex.image?.let { path ->
                    AsyncImage(
                        model = "file:///android_asset/$path",
                        contentDescription = ex.name,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(240.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                    )
                }

                Text(titleCase(ex.name), style = MaterialTheme.typography.headlineSmall)

                TagSection(ex)

                if (ex.instructions.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Instructions", style = MaterialTheme.typography.titleMedium)
                        ex.instructions.forEachIndexed { i, step ->
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text(
                                    "${i + 1}.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                                Text(step, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TagSection(ex: Exercise) {
    val tags = buildList {
        ex.bodyPart?.let { add("Body part" to titleCase(it)) }
        ex.target?.let { add("Target" to titleCase(it)) }
        add("Equipment" to titleCase(ex.requiredEquipment.name))
        add("Category" to titleCase(ex.category.name))
        ex.primaryMuscles.forEach { add("Primary" to titleCase(it.name)) }
        ex.secondaryMuscles.forEach { add("Secondary" to titleCase(it.name)) }
    }
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        tags.forEach { (group, value) ->
            AssistChip(
                onClick = {},
                label = { Text(value) },
                leadingIcon = {
                    Text(group, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                },
            )
        }
    }
}

private fun titleCase(value: String) =
    value.lowercase().replace('_', ' ').replaceFirstChar(Char::uppercase)
