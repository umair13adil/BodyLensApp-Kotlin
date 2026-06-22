package com.bodylens.data.repository

import com.bodylens.data.local.db.ProgressDao
import com.bodylens.data.mapper.toDomain
import com.bodylens.data.mapper.toEntity
import com.bodylens.domain.model.ProgressEntry
import com.bodylens.domain.repository.ProgressRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class ProgressRepositoryImpl @Inject constructor(
    private val dao: ProgressDao,
) : ProgressRepository {
    override fun observeHistory(): Flow<List<ProgressEntry>> =
        dao.observeAll().map { list -> list.map { it.toDomain() } }

    override suspend fun save(entry: ProgressEntry): Long = dao.insert(entry.toEntity())

    override suspend fun deleteAll() = dao.deleteAll()
}
