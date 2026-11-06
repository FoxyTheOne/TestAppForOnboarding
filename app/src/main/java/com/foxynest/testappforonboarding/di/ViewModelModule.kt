package com.foxynest.testappforonboarding.di

import com.foxynest.testappforonboarding.data.repository.MigrateUserDataRepository
import com.foxynest.testappforonboarding.domain.iRepository.IMigrateUserDataRepository
import com.foxynest.testappforonboarding.domain.use_cases.authUseCases.account.DeleteAccountUC
import com.foxynest.testappforonboarding.domain.use_cases.authUseCases.account.IDeleteAccountUC
import com.foxynest.testappforonboarding.domain.use_cases.authUseCases.account.ISendPasswordResetUC
import com.foxynest.testappforonboarding.domain.use_cases.authUseCases.account.SendPasswordResetUC
import com.foxynest.testappforonboarding.domain.use_cases.authUseCases.login.ILoginValidateEmailUC
import com.foxynest.testappforonboarding.domain.use_cases.authUseCases.login.ILoginValidatePasswordUC
import com.foxynest.testappforonboarding.domain.use_cases.authUseCases.login.LoginValidateEmailUC
import com.foxynest.testappforonboarding.domain.use_cases.authUseCases.login.LoginValidatePasswordUC
import com.foxynest.testappforonboarding.domain.use_cases.authUseCases.register.IRegisterUC
import com.foxynest.testappforonboarding.domain.use_cases.authUseCases.register.IRegisterValidateEmailUC
import com.foxynest.testappforonboarding.domain.use_cases.authUseCases.register.IRegisterValidatePasswordUC
import com.foxynest.testappforonboarding.domain.use_cases.authUseCases.register.IRegisterValidateRepeatedPasswordUC
import com.foxynest.testappforonboarding.domain.use_cases.authUseCases.register.RegisterUC
import com.foxynest.testappforonboarding.domain.use_cases.authUseCases.register.RegisterValidateEmailUC
import com.foxynest.testappforonboarding.domain.use_cases.authUseCases.register.RegisterValidatePasswordUC
import com.foxynest.testappforonboarding.domain.use_cases.authUseCases.register.RegisterValidateRepeatedPasswordUC
import com.foxynest.testappforonboarding.domain.use_cases.migrate.IMigrateUserDataUC
import com.foxynest.testappforonboarding.domain.use_cases.migrate.MigrateUserDataUC
import com.foxynest.testappforonboarding.domain.use_cases.onboardingUseCases.IOnboardingUC
import com.foxynest.testappforonboarding.domain.use_cases.onboardingUseCases.OnboardingUC
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent

/**
 * Привязки, которые нужны только ViewModel-ям. `ViewModelComponent` – объекты отсюда создаются заново
 * для каждой ViewModel и живут столько же, сколько она. Use case-ы без состояния можно держать здесь –
 * если какой-то понадобится ещё и вне ViewModel (например, в BroadcastReceiver), его надо перенести в SingletonModule.
 */
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


    @Binds
    abstract fun bindsRegisterInteractor(
        registerUC: RegisterUC
    ): IRegisterUC

    @Binds
    abstract fun bindsDeleteAccountUC(impl: DeleteAccountUC): IDeleteAccountUC

    @Binds
    abstract fun bindsSendPasswordResetUC(impl: SendPasswordResetUC): ISendPasswordResetUC
}