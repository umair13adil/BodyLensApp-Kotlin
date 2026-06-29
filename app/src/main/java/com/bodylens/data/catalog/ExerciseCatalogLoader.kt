package com.bodylens.data.catalog

import android.content.Context
import com.bodylens.domain.model.Exercise
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json

/**
 * Loads the bundled free-exercise-db catalog from assets and caches it in memory.
 *
 * The catalog is static, read-only reference data, so it is parsed once on first
 * access and kept in memory rather than seeded into Room — this keeps it fully
 * offline with no database migration. Parsing ~1320 entries is a one-off cost.
 */
class ExerciseCatalogLoader(private val context: Context) {

    private val json = Json { ignoreUnknownKeys = true }
    private val mutex = Mutex()
    @Volatile private var cache: List<Exercise>? = null

    suspend fun catalog(): List<Exercise> {
        cache?.let { return it }
        return mutex.withLock {
            cache ?: load().also { cache = it }
        }
    }

    private fun load(): List<Exercise> {
        val raw = context.assets.open(ASSET_NAME).bufferedReader().use { it.readText() }
        return json.decodeFromString<List<ExerciseDto>>(raw).map { it.toDomain() }
    }

    private companion object {
        const val ASSET_NAME = "exercises.json"
    }
}