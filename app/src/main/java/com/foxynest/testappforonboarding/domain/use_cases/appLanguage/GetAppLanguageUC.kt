package com.foxynest.testappforonboarding.domain.use_cases.appLanguage

import com.foxynest.testappforonboarding.domain.entities.language.LanguageEnum
import com.foxynest.testappforonboarding.domain.iRepository.IAppLanguageRepository
import javax.inject.Inject

/**
 * Domain layer, UseCase. Бизнес-логика, Kotlin. Работает только с Repository.
 *
 * Interactor ответственен за обеспечение данными отдельные экраны (для каждого экрана - отдельный Interactor).
 * При работе с model, здесь происходит преобразование local -> presentation, т.е.
 * преобразование моделей в модели нижнего уровня перед тем, как нижний уровень сможет их использовать.
 */
class GetAppLanguageUC @Inject constructor(
    private val appLanguageRepository: IAppLanguageRepository
) : IGetAppLanguageUC {
    override suspend fun invoke(): LanguageEnum = appLanguageRepository.getAppLanguageFlow()
}