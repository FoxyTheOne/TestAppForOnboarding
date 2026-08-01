package com.foxynest.testappforonboarding.di

import com.foxynest.testappforonboarding.data.repository.MigrateUserDataRepository
import com.foxynest.testappforonboarding.domain.iRepository.IMigrateUserDataRepository
import com.foxynest.testappforonboarding.domain.use_cases.authUseCases.login.ILoginValidateEmailUC
import com.foxynest.testappforonboarding.domain.use_cases.authUseCases.login.ILoginValidatePasswordUC
import com.foxynest.testappforonboarding.domain.use_cases.authUseCases.login.LoginValidateEmailUC
import com.foxynest.testappforonboarding.domain.use_cases.authUseCases.login.LoginValidatePasswordUC
import com.foxynest.testappforonboarding.domain.use_cases.authUseCases.register.IRegisterValidateEmailUC
import com.foxynest.testappforonboarding.domain.use_cases.authUseCases.register.IRegisterValidatePasswordUC
import com.foxynest.testappforonboarding.domain.use_cases.authUseCases.register.IRegisterValidateRepeatedPasswordUC
import com.foxynest.testappforonboarding.domain.use_cases.authUseCases.register.RegisterValidateEmailUC
import com.foxynest.testappforonboarding.domain.use_cases.authUseCases.register.RegisterValidatePasswordUC
import com.foxynest.testappforonboarding.domain.use_cases.authUseCases.register.RegisterValidateRepeatedPasswordUC
import com.foxynest.testappforonboarding.domain.use_cases.migrate.IMigrateUserDataUC
import com.foxynest.testappforonboarding.domain.use_cases.migrate.MigrateUserDataUC
import com.foxynest.testappforonboarding.domain.use_cases.notification.IScheduleBirthdayNotificationUC
import com.foxynest.testappforonboarding.domain.use_cases.notification.ScheduleBirthdayNotificationUC
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
        validateEmailInteractor: RegisterValidateEmailUC
    ): IRegisterValidateEmailUC

    @Binds
    abstract fun bindsValidatePasswordInteractor(
        validatePasswordInteractor: RegisterValidatePasswordUC
    ): IRegisterValidatePasswordUC

    @Binds
    abstract fun bindsValidateRepeatedPasswordInteractor(
        validateRepeatedPasswordInteractor: RegisterValidateRepeatedPasswordUC
    ): IRegisterValidateRepeatedPasswordUC

//    @Binds
//    abstract fun bindsValidateTermsInteractor(
//        validateTermsInteractor: ValidateTermsUseCase
//    ): IValidateTermsUseCase

    @Binds
    abstract fun bindsLoginValidateEmailInteractor(
        loginValidateEmailInteractor: LoginValidateEmailUC
    ): ILoginValidateEmailUC

    @Binds
    abstract fun bindsLoginValidatePasswordInteractor(
        loginValidatePasswordInteractor: LoginValidatePasswordUC
    ): ILoginValidatePasswordUC

//    @Binds
//    abstract fun bindsOnboardingRepository(
//        onboardingRepository: OnboardingRepository
//    ): IOnboardingRepository

    @Binds
    abstract fun bindsMigrateUserDataInteractor(
        migrateUserDataUC: MigrateUserDataUC
    ): IMigrateUserDataUC

    @Binds
    abstract fun bindsMigrateUserDataRepository(
        migrateUserDataRepository: MigrateUserDataRepository
    ): IMigrateUserDataRepository


    // test
    @Binds
    abstract fun bindsScheduleBirthdayNotificationInteractor(
        scheduleBirthdayNotificationUC: ScheduleBirthdayNotificationUC
    ): IScheduleBirthdayNotificationUC
}