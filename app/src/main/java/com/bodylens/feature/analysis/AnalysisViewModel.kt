package com.bodylens.feature.analysis

import androidx.lifecycle.ViewModel
import com.bodylens.core.common.AnalysisResultBus
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class AnalysisViewModel @Inject constructor(
    resultBus: AnalysisResultBus,
) : ViewModel() {
    val result = resultBus.latest
}
