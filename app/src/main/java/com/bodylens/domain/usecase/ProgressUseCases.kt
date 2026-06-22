package com.bodylens.domain.usecase

import com.bodylens.domain.model.ProgressEntry
import com.bodylens.domain.repository.ProgressRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetProgressHistoryUseCase @Inject constructor(
    private val repository: ProgressRepository,
) {
    operator fun invoke(): Flow<List<ProgressEntry>> = repository.observeHistory()
}

class SaveProgressEntryUseCase @Inject constructor(
    private val repository: ProgressRepository,
) {
    suspend operator fun invoke(entry: ProgressEntry): Long = repository.save(entry)
}

/** Privacy control: wipe every photo + record the app holds. */
class DeleteAllDataUseCase @Inject constructor(
    private val progressRepository: ProgressRepository,
    private val userRepository: com.bodylens.domain.repository.UserRepository,
) {
    suspend operator fun invoke() {
        progressRepository.deleteAll()
        userRepository.clear()
    }
}
