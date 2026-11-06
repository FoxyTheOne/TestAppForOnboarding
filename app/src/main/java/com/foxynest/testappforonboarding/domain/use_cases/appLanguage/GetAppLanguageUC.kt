package com.foxynest.testappforonboarding.domain.use_cases.appLanguage

import com.foxynest.testappforonboarding.domain.entities.language.LanguageEnum
import com.foxynest.testappforonboarding.domain.iRepository.IAppLanguageRepository
import javax.inject.Inject

/** Язык, выбранный в приложении (для переключателя в профиле). */
class GetAppLanguageUC @Inject constructor(
    private val appLanguageRepository: IAppLanguageRepository
) : IGetAppLanguageUC {
    override suspend fun invoke(): LanguageEnum = appLanguageRepository.getAppLanguageFlow()
}