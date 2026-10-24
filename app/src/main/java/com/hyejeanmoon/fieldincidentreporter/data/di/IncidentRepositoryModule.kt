package com.hyejeanmoon.fieldincidentreporter.data.di

import com.hyejeanmoon.fieldincidentreporter.data.local.CapturedPhotoFiles
import com.hyejeanmoon.fieldincidentreporter.data.repository.OfflineFirstIncidentRepository
import com.hyejeanmoon.fieldincidentreporter.domain.repository.CapturedPhotoStore
import com.hyejeanmoon.fieldincidentreporter.domain.repository.IncidentRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal abstract class IncidentRepositoryModule {
    @Binds
    @Singleton
    abstract fun bindIncidentRepository(repository: OfflineFirstIncidentRepository): IncidentRepository

    @Binds
    abstract fun bindCapturedPhotoStore(store: CapturedPhotoFiles): CapturedPhotoStore
}
