package com.kotlin.flashlearn.di

import com.kotlin.flashlearn.data.repository.DailyWordRepositoryImpl
import com.kotlin.flashlearn.domain.repository.DailyWordRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DailyWordModule {

    @Binds
    @Singleton
    abstract fun bindDailyWordRepository(
        impl: DailyWordRepositoryImpl
    ): DailyWordRepository
}
