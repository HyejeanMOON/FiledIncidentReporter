package com.hyejeanmoon.fieldincidentreporter.data.di

import android.content.Context
import androidx.room.Room
import com.hyejeanmoon.fieldincidentreporter.data.local.IncidentDao
import com.hyejeanmoon.fieldincidentreporter.data.local.IncidentDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object IncidentDatabaseModule {
    @Provides
    @Singleton
    fun provideIncidentDatabase(@ApplicationContext context: Context): IncidentDatabase =
        Room.databaseBuilder(context, IncidentDatabase::class.java, "incident-cache.db")
            // The database only caches server data, so a schema change can rebuild it.
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()

    @Provides
    fun provideIncidentDao(database: IncidentDatabase): IncidentDao = database.incidents()
}
