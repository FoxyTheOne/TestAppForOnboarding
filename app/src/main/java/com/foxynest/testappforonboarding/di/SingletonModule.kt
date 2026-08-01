package com.foxynest.testappforonboarding.di

import android.content.Context
import androidx.work.WorkManager
import com.foxynest.testappforonboarding.data.dataSource.datastorePreferences.AppDatastorePreferences
import com.foxynest.testappforonboarding.data.dataSource.datastorePreferences.IAppDatastorePreferences
import com.foxynest.testappforonboarding.data.repository.AppLanguageRepository
import com.foxynest.testappforonboarding.data.repository.AuthRepository
import com.foxynest.testappforonboarding.data.repository.BirthdayNotificationRepository
import com.foxynest.testappforonboarding.data.repository.OnboardingRepository
import com.foxynest.testappforonboarding.domain.iRepository.IAppLanguageRepository
import com.foxynest.testappforonboarding.domain.iRepository.IAuthRepository
import com.foxynest.testappforonboarding.domain.iRepository.IBirthdayNotificationRepository
import com.foxynest.testappforonboarding.domain.iRepository.IOnboardingRepository
import com.foxynest.testappforonboarding.domain.use_cases.appLanguage.GetAppLanguageUC
import com.foxynest.testappforonboarding.domain.use_cases.appLanguage.IGetAppLanguageUC
import com.foxynest.testappforonboarding.domain.use_cases.appLanguage.ISetAppLanguageUC
import com.foxynest.testappforonboarding.domain.use_cases.appLanguage.SetAppLanguageUC
import com.foxynest.testappforonboarding.domain.use_cases.authUseCases.GetCurrentUserPresentationUC
import com.foxynest.testappforonboarding.domain.use_cases.authUseCases.IGetCurrentUserPresentationUC
import com.foxynest.testappforonboarding.domain.use_cases.authUseCases.email.CheckEmailVerifiedUC
import com.foxynest.testappforonboarding.domain.use_cases.authUseCases.email.ICheckEmailVerifiedUC
import com.foxynest.testappforonboarding.domain.use_cases.authUseCases.email.ISendEmailVerificationUC
import com.foxynest.testappforonboarding.domain.use_cases.authUseCases.email.SendEmailVerificationUC
import com.foxynest.testappforonboarding.domain.use_cases.authUseCases.login.GetAuthStateFlowUC
import com.foxynest.testappforonboarding.domain.use_cases.authUseCases.login.GetCurrentFirebaseUserUC
import com.foxynest.testappforonboarding.domain.use_cases.authUseCases.login.IGetAuthStateFlowUC
import com.foxynest.testappforonboarding.domain.use_cases.authUseCases.login.IGetCurrentFirebaseUserUC
import com.foxynest.testappforonboarding.domain.use_cases.authUseCases.login.ILoginUC
import com.foxynest.testappforonboarding.domain.use_cases.authUseCases.login.ILogoutUC
import com.foxynest.testappforonboarding.domain.use_cases.updatingDataUseCases.IUpdateFirebaseProfileUC
import com.foxynest.testappforonboarding.domain.use_cases.authUseCases.login.LoginUC
import com.foxynest.testappforonboarding.domain.use_cases.authUseCases.login.LogoutUC
import com.foxynest.testappforonboarding.domain.use_cases.updatingDataUseCases.IUpdateEmailUC
import com.foxynest.testappforonboarding.domain.use_cases.updatingDataUseCases.UpdateEmailUC
import com.foxynest.testappforonboarding.domain.use_cases.updatingDataUseCases.UpdateFirebaseProfileUC
import com.foxynest.testappforonboarding.other.timeProvider.ITimeProvider
import com.foxynest.testappforonboarding.other.timeProvider.SystemTimeProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Файл с инструкциями для Hilt
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class SingletonModule {

    companion object {
        @Provides
        @Singleton
        fun provideFirebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance()

        @Provides
        @Singleton
        fun provideFirebaseFirestore(): FirebaseFirestore = FirebaseFirestore.getInstance()

        @Provides
        @Singleton
        fun provideWorkManager(@ApplicationContext context: Context): WorkManager {
            return WorkManager.getInstance(context)
        }

        @Provides
        fun provideTimeProvider(): ITimeProvider = SystemTimeProvider()
    }

    @Binds
    @Singleton
    abstract fun bindsDatastorePreference(
        appDatastorePreference: AppDatastorePreferences
    ): IAppDatastorePreferences

    @Binds
    @Singleton
    abstract fun bindsAuthRepository(authRepository: AuthRepository): IAuthRepository

    // Use Cases, связанные с Auth Repository:
    @Binds
    abstract fun bindsGetAuthStateFlowInteractor(
        getAuthStateFlowInteractor: GetAuthStateFlowUC
    ): IGetAuthStateFlowUC

    @Binds
    abstract fun bindsGetCurrentLogInStateInteractor(
        getCurrentLogInStateInteractor: GetCurrentFirebaseUserUC
    ): IGetCurrentFirebaseUserUC

    @Binds
    abstract fun bindsSendEmailVerificationInteractor(
        sendEmailVerificationInteractor: SendEmailVerificationUC
    ): ISendEmailVerificationUC

    @Binds
    abstract fun bindsCheckEmailVerifiedInteractor(
        checkEmailVerifiedInteractor: CheckEmailVerifiedUC
    ): ICheckEmailVerifiedUC

    @Binds
    abstract fun bindsLoginUCInteractor(
        loginUCInteractor: LoginUC
    ): ILoginUC

    @Binds
    abstract fun bindsLogoutUCInteractor(
        logoutUCInteractor: LogoutUC
    ): ILogoutUC

    @Binds
    abstract fun bindsUpdateProfileInteractor(
        updateProfileInteractor: UpdateFirebaseProfileUC
    ): IUpdateFirebaseProfileUC

    @Binds
    abstract fun bindsUpdateEmailUCInteractor(
        updateEmailUC: UpdateEmailUC
    ): IUpdateEmailUC

    @Binds
    abstract fun bindsGetCurrentUserPresentationInteractor(
        getCurrentUserPresentationUC: GetCurrentUserPresentationUC
    ): IGetCurrentUserPresentationUC

    // Language
    @Binds
    abstract fun bindsSetAppLanguageInteractor(
        setAppLanguageUC: SetAppLanguageUC
    ): ISetAppLanguageUC

    @Binds
    abstract fun bindsGetAppLanguageInteractor(
        getAppLanguageUC: GetAppLanguageUC
    ): IGetAppLanguageUC

    @Binds
    abstract fun bindsAppLanguageRepository(
        appLanguageRepository: AppLanguageRepository
    ): IAppLanguageRepository

    @Binds
    @Singleton
    abstract fun bindsBirthdayNotificationManager(
        birthdayNotificationManager: BirthdayNotificationRepository
    ): IBirthdayNotificationRepository

    @Binds
    @Singleton
    abstract fun bindsOnboardingRepository(
        onboardingRepository: OnboardingRepository
    ): IOnboardingRepository
}