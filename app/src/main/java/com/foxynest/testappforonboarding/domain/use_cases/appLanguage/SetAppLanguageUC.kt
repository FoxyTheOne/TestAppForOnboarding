package com.foxynest.testappforonboarding.domain.use_cases.appLanguage

import com.foxynest.testappforonboarding.domain.entities.language.LanguageEnum
import com.foxynest.testappforonboarding.domain.iRepository.IAppLanguageRepository
import javax.inject.Inject

/** Запоминает выбранный язык – копия для фонового кода (уведомление о дне рождения). Применяет язык LanguageManager. */
class SetAppLanguageUC @Inject constructor(
    private val appLanguageRepository: IAppLanguageRepository
) : ISetAppLanguageUC {
    override suspend fun invoke(languageCode: LanguageEnum) = appLanguageRepository.saveAppLanguage(languageCode)
}