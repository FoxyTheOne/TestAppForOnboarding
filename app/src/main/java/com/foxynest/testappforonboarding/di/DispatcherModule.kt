package com.foxynest.testappforonboarding.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import jakarta.inject.Qualifier
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class DispatcherIO

@Module
@InstallIn(SingletonComponent::class)
object DispatcherModule {
    @Provides
    @DispatcherIO
    fun provideIODispatcher(): CoroutineDispatcher = Dispatchers.IO
}