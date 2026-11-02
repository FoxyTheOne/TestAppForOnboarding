package com.foxynest.testappforonboarding.domain.use_cases.appLanguage

import com.foxynest.testappforonboarding.domain.entities.language.LanguageEnum

interface ITakeLanguageForMigrationUC {
    suspend operator fun invoke(): LanguageEnum?
}