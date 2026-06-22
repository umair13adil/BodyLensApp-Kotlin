package com.bodylens.feature.onboarding

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.bodylens.domain.model.*

@Composable
fun OnboardingScreen(
    onFinished: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    Scaffold { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(24.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            when (state.step) {
                0 -> AgeGateStep(state, viewModel)
                else -> ProfileStep(state, viewModel, onFinished)
            }
        }
    }
}

@Composable
private fun AgeGateStep(state: OnboardingState, vm: OnboardingViewModel) {
    Text("Welcome to BodyLens", style = MaterialTheme.typography.headlineSmall)
    Text(
        "BodyLens analyses fitted-clothing body photos on your device to track your " +
            "composition over time. Photos never leave your phone and are never uploaded.",
        style = MaterialTheme.typography.bodyMedium,
    )
    CheckRow("I confirm I am 18 years or older.", state.ageConfirmed, vm::setAgeConfirmed)
    CheckRow(
        "I understand my photos are stored only on this device, and I can delete everything anytime.",
        state.consentGiven, vm::setConsent,
    )
    Spacer(Modifier.height(8.dp))
    Button(onClick = vm::next, enabled = state.canProceedFromGate, modifier = Modifier.fillMaxWidth()) {
        Text("Continue")
    }
}

@Composable
private fun ProfileStep(state: OnboardingState, vm: OnboardingViewModel, onFinished: () -> Unit) {
    Text("About you", style = MaterialTheme.typography.headlineSmall)

    Text("Sex (for the body-fat formula)", style = MaterialTheme.typography.labelLarge)
    SingleChoiceRow(Sex.entries, state.sex, { it.name }, vm::setSex)

    OutlinedTextField(
        value = state.heightCm,
        onValueChange = vm::setHeight,
        label = { Text("Height (cm)") },
        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.fillMaxWidth(),
    )

    Text("Goal", style = MaterialTheme.typography.labelLarge)
    SingleChoiceRow(FitnessGoal.entries, state.goal, { it.pretty() }, vm::setGoal)

    Text("Experience", style = MaterialTheme.typography.labelLarge)
    SingleChoiceRow(ExperienceLevel.entries, state.experience, { it.name.lowercase().replaceFirstChar(Char::uppercase) }, vm::setExperience)

    Text("Equipment", style = MaterialTheme.typography.labelLarge)
    SingleChoiceRow(Equipment.entries, state.equipment, { it.pretty() }, vm::setEquipment)

    Spacer(Modifier.height(8.dp))
    Button(
        onClick = { vm.finish(onFinished) },
        enabled = state.canFinish,
        modifier = Modifier.fillMaxWidth(),
    ) { Text("Start") }
}

@Composable
private fun CheckRow(text: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().toggleable(checked, role = Role.Checkbox) { onChange(it) },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = checked, onCheckedChange = null)
        Spacer(Modifier.width(12.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun <T> SingleChoiceRow(items: List<T>, selected: T?, label: (T) -> String, onSelect: (T) -> Unit) {
    FlowRowChips(items, selected, label, onSelect)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun <T> FlowRowChips(items: List<T>, selected: T?, label: (T) -> String, onSelect: (T) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items.forEach { item ->
            FilterChip(
                selected = item == selected,
                onClick = { onSelect(item) },
                label = { Text(label(item)) },
            )
        }
    }
}

private fun FitnessGoal.pretty() = name.lowercase().replace('_', ' ').replaceFirstChar(Char::uppercase)
private fun Equipment.pretty() = name.lowercase().replace('_', ' ').replaceFirstChar(Char::uppercase)
