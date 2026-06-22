package com.bodylens.feature.progress

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bodylens.domain.model.ProgressEntry
import com.bodylens.domain.usecase.GetProgressHistoryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class ProgressViewModel @Inject constructor(
    history: GetProgressHistoryUseCase,
) : ViewModel() {
    val entries = history().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList<ProgressEntry>())
}
