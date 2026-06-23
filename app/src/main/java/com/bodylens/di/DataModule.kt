package com.bodylens.di

import android.content.Context
import androidx.room.Room
import com.bodylens.core.common.IoDispatcher
import com.bodylens.data.catalog.ExerciseCatalogLoader
import com.bodylens.data.local.db.BodyLensDatabase
import com.bodylens.data.local.db.ProgressDao
import com.bodylens.data.ml.BodyAnalyzer
import com.bodylens.data.ml.MediaPipeBodyAnalyzer
import com.bodylens.data.ml.SilhouetteMeasurer
import com.bodylens.domain.service.ExercisePlanner
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataModule {

    @Provides @Singleton
    fun database(context: Context): BodyLensDatabase =
        Room.databaseBuilder(context, BodyLensDatabase::class.java, "bodylens.db").build()

    @Provides fun progressDao(db: BodyLensDatabase): ProgressDao = db.progressDao()

    @Provides @Singleton fun silhouetteMeasurer() = SilhouetteMeasurer()

    @Provides @Singleton fun exercisePlanner() = ExercisePlanner()

    @Provides @Singleton
    fun exerciseCatalogLoader(context: Context) = ExerciseCatalogLoader(context)

    @Provides @Singleton
    fun bodyAnalyzer(
        context: Context,
        measurer: SilhouetteMeasurer,
        @IoDispatcher dispatcher: CoroutineDispatcher,
    ): BodyAnalyzer = MediaPipeBodyAnalyzer(context, measurer, dispatcher)
}
