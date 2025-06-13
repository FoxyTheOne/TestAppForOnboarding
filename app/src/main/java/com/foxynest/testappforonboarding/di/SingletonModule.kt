package com.foxynest.testappforonboarding.di

import com.foxynest.testappforonboarding.data.dataSource.datastorePreferences.AppDatastorePreferences
import com.foxynest.testappforonboarding.data.dataSource.datastorePreferences.IAppDatastorePreferences
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Файл с инструкциями для Hilt
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class SingletonModule {
    @Binds
    @Singleton
    abstract fun bindsDatastorePreference(
        appDatastorePreference: AppDatastorePreferences
    ): IAppDatastorePreferences
}