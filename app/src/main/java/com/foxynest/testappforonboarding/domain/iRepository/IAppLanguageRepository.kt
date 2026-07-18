package com.foxynest.testappforonboarding.domain.iRepository

import com.foxynest.testappforonboarding.domain.entities.language.LanguageEnum
import kotlinx.coroutines.flow.first

interface IAppLanguageRepository {
    suspend fun getAppLanguageFlow(): LanguageEnum
    suspend fun saveAppLanguage(languageCode: LanguageEnum)
}