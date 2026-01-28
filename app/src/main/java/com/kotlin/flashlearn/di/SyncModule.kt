package com.kotlin.flashlearn.di

import com.kotlin.flashlearn.data.sync.SyncRepository
import com.kotlin.flashlearn.data.sync.SyncRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class SyncModule {

    @Binds
    @Singleton
    abstract fun bindSyncRepository(
        impl: SyncRepositoryImpl
    ): SyncRepository
}
