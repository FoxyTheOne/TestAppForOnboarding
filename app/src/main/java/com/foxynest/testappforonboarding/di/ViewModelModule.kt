package com.foxynest.testappforonboarding.di

import com.foxynest.testappforonboarding.data.repository.OnboardingRepository
import com.foxynest.testappforonboarding.domain.iRepository.IOnboardingRepository
import com.foxynest.testappforonboarding.domain.use_cases.authUseCases.email.CheckEmailVerifiedUC
import com.foxynest.testappforonboarding.domain.use_cases.authUseCases.email.ICheckEmailVerifiedUC
import com.foxynest.testappforonboarding.domain.use_cases.authUseCases.email.ISendEmailVerificationUC
import com.foxynest.testappforonboarding.domain.use_cases.authUseCases.email.SendEmailVerificationUC
import com.foxynest.testappforonboarding.domain.use_cases.authUseCases.login.CheckRegisteredEmailAndPasswordUC
import com.foxynest.testappforonboarding.domain.use_cases.authUseCases.login.GetCurrentUserUC
import com.foxynest.testappforonboarding.domain.use_cases.authUseCases.login.ICheckRegisteredEmailAndPasswordUC
import com.foxynest.testappforonboarding.domain.use_cases.authUseCases.login.IGetCurrentUserUC
import com.foxynest.testappforonboarding.domain.use_cases.authUseCases.register.IValidateEmailUC
import com.foxynest.testappforonboarding.domain.use_cases.authUseCases.register.IValidatePasswordUC
import com.foxynest.testappforonboarding.domain.use_cases.authUseCases.register.IValidateRepeatedPasswordUC
import com.foxynest.testappforonboarding.domain.use_cases.authUseCases.register.ValidateEmailUC
import com.foxynest.testappforonboarding.domain.use_cases.authUseCases.register.ValidatePasswordUC
import com.foxynest.testappforonboarding.domain.use_cases.authUseCases.register.ValidateRepeatedPasswordUC
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
    abstract fun bindsCheckRegisteredEmailAndPasswordInteractor(
        checkRegisteredEmailAndPasswordInteractor: CheckRegisteredEmailAndPasswordUC
    ): ICheckRegisteredEmailAndPasswordUC

    @Binds
    abstract fun bindsOnboardingRepository(
        onboardingRepository: OnboardingRepository
    ): IOnboardingRepository
}