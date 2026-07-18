package com.foxynest.testappforonboarding.domain.use_cases.appLanguage

import com.foxynest.testappforonboarding.domain.entities.language.LanguageEnum

interface ISetAppLanguageUC {
    suspend operator fun invoke(languageCode: LanguageEnum)
}