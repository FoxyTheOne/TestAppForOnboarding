package com.foxynest.testappforonboarding.data.dataSource.sharedPreferences

interface IAppSharedPreference {
    fun saveOnboardingState(isCompleted: Boolean)
    fun isOnboardingCompleted(): Boolean
}