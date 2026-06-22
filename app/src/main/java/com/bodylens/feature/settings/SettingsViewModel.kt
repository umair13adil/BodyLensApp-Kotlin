package com.bodylens.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bodylens.domain.usecase.DeleteAllDataUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val deleteAllData: DeleteAllDataUseCase,
) : ViewModel() {
    fun wipeEverything(onDone: () -> Unit) {
        viewModelScope.launch {
            deleteAllData()
            onDone()
        }
    }
}
