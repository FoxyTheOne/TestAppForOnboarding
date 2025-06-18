package com.foxynest.testappforonboarding.di

import com.foxynest.testappforonboarding.data.repository.OnboardingRepository
import com.foxynest.testappforonboarding.domain.iRepository.IOnboardingRepository
import com.foxynest.testappforonboarding.domain.use_cases.authUseCases.IValidateEmailUC
import com.foxynest.testappforonboarding.domain.use_cases.authUseCases.IValidatePasswordUC
import com.foxynest.testappforonboarding.domain.use_cases.authUseCases.IValidateRepeatedPasswordUC
import com.foxynest.testappforonboarding.domain.use_cases.authUseCases.ValidateEmailUC
import com.foxynest.testappforonboarding.domain.use_cases.authUseCases.ValidatePasswordUC
import com.foxynest.testappforonboarding.domain.use_cases.authUseCases.ValidateRepeatedPasswordUC
import com.foxynest.testappforonboarding.domain.use_cases.onboardingUseCases.IOnboardingUC
import com.foxynest.testappforonboarding.domain.use_cases.onboardingUseCases.OnboardingUC
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent

@Module
@InstallIn(ViewModelComponent::class)
abstract class ViewModelModule {
    @Binds
    abstract fun bindsOnboardingInteractor(
        onboardingInteractor: OnboardingUC
    ): IOnboardingUC

    @Binds
    abstract fun bindsValidateEmailInteractor(
        validateEmailInteractor: ValidateEmailUC
    ): IValidateEmailUC

    @Binds
    abstract fun bindsValidatePasswordInteractor(
        validatePasswordInteractor: ValidatePasswordUC
    ): IValidatePasswordUC

    @Binds
    abstract fun bindsValidateRepeatedPasswordInteractor(
        validateRepeatedPasswordInteractor: ValidateRepeatedPasswordUC
    ): IValidateRepeatedPasswordUC

//    @Binds
//    abstract fun bindsValidateTermsInteractor(
//        validateTermsInteractor: ValidateTermsUseCase
//    ): IValidateTermsUseCase

    @Binds
    abstract fun bindsOnboardingRepository(
        onboardingRepository: OnboardingRepository
    ): IOnboardingRepository
}