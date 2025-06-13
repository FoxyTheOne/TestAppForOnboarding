package com.foxynest.testappforonboarding.data.dataSource.sharedPreferences

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/**
 * Заменён на Data store. Удалить, если не нужно
 */
class AppSharedPreference @Inject constructor(@ApplicationContext context: Context) :
    IAppSharedPreference {
    companion object {
        private const val TAG = "AppSharedPreference"

        // Константа - имя файла
        private const val PREFERENCE_NAME = "AppSharedPreference"

        // Ключи для данных
        private const val IS_ONBOARDING_COMPLETED = "is_onboarding_completed"
    }

    // У нас будет один общий файл, поэтому .getSharedPreferences()
    private val sharedPreference = context.getSharedPreferences(
        PREFERENCE_NAME,
        Context.MODE_PRIVATE
    )

    override fun saveOnboardingState(isCompleted: Boolean) {
        sharedPreference?.edit()?.putBoolean(
            IS_ONBOARDING_COMPLETED,
            isCompleted
        )?.apply()
    }

    override fun isOnboardingCompleted(): Boolean {
        return sharedPreference?.getBoolean(
            IS_ONBOARDING_COMPLETED,
            false
        ) ?: false
    }
}