package com.bodylens.core.common

import com.bodylens.domain.model.AnalysisResult
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * In-memory hand-off of the latest analysis between Capture and Result screens,
 * avoiding the need to serialise sensitive data through the navigation back stack.
 */
@Singleton
class AnalysisResultBus @Inject constructor() {
    val latest = MutableStateFlow<AnalysisResult?>(null)
}
