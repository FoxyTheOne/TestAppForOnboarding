package com.foxynest.testappforonboarding.di

import android.app.AlarmManager
import android.content.Context
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
import com.foxynest.testappforonboarding.domain.use_cases.appLanguage.ITakeLanguageForMigrationUC
import com.foxynest.testappforonboarding.domain.use_cases.appLanguage.SetAppLanguageUC
import com.foxynest.testappforonboarding.domain.use_cases.appLanguage.TakeLanguageForMigrationUC
import com.foxynest.testappforonboarding.domain.use_cases.authUseCases.email.CheckEmailVerifiedUC
import com.foxynest.testappforonboarding.domain.use_cases.authUseCases.email.ICheckEmailVerifiedUC
import com.foxynest.testappforonboarding.domain.use_cases.authUseCases.email.ISendEmailVerificationUC
import com.foxynest.testappforonboarding.domain.use_cases.authUseCases.email.SendEmailVerificationUC
import com.foxynest.testappforonboarding.domain.use_cases.authUseCases.login.GetAuthStateFlowUC
import com.foxynest.testappforonboarding.domain.use_cases.authUseCases.login.IGetAuthStateFlowUC
import com.foxynest.testappforonboarding.domain.use_cases.authUseCases.login.IIsUserLoggedInUC
import com.foxynest.testappforonboarding.domain.use_cases.authUseCases.login.ILoginUC
import com.foxynest.testappforonboarding.domain.use_cases.authUseCases.login.ILogoutUC
import com.foxynest.testappforonboarding.domain.use_cases.authUseCases.login.IObserveCurrentUserUC
import com.foxynest.testappforonboarding.domain.use_cases.authUseCases.login.IsUserLoggedInUC
import com.foxynest.testappforonboarding.domain.use_cases.authUseCases.login.LoginUC
import com.foxynest.testappforonboarding.domain.use_cases.authUseCases.login.LogoutUC
import com.foxynest.testappforonboarding.domain.use_cases.authUseCases.login.ObserveCurrentUserUC
import com.foxynest.testappforonboarding.domain.use_cases.notification.IScheduleBirthdayNotificationUC
import com.foxynest.testappforonboarding.domain.use_cases.notification.ScheduleBirthdayNotificationUC
import com.foxynest.testappforonboarding.domain.use_cases.updatingDataUseCases.IUpdateEmailUC
import com.foxynest.testappforonboarding.domain.use_cases.updatingDataUseCases.IUpdateFirebaseProfileUC
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
        fun provideTimeProvider(): ITimeProvider = SystemTimeProvider()

        @Provides
        @Singleton
        fun provideAlarmManager(@ApplicationContext context: Context): AlarmManager {
            return context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        }
    }

    // Data sources & repositories
    @Binds
    @Singleton
    abstract fun bindsDatastorePreference(impl: AppDatastorePreferences): IAppDatastorePreferences

    @Binds
    @Singleton
    abstract fun bindsAuthRepository(impl: AuthRepository): IAuthRepository

    @Binds
    @Singleton
    abstract fun bindsOnboardingRepository(impl: OnboardingRepository): IOnboardingRepository

    @Binds
    @Singleton
    abstract fun bindsBirthdayNotificationRepository(
        impl: BirthdayNotificationRepository
    ): IBirthdayNotificationRepository

    @Binds
    abstract fun bindsAppLanguageRepository(impl: AppLanguageRepository): IAppLanguageRepository

    // Auth
    @Binds
    abstract fun bindsGetAuthStateFlowUC(impl: GetAuthStateFlowUC): IGetAuthStateFlowUC

    @Binds
    abstract fun bindsObserveCurrentUserUC(impl: ObserveCurrentUserUC): IObserveCurrentUserUC

    @Binds
    abstract fun bindsIsUserLoggedInUC(impl: IsUserLoggedInUC): IIsUserLoggedInUC

    @Binds
    abstract fun bindsLoginUC(impl: LoginUC): ILoginUC

    @Binds
    abstract fun bindsLogoutUC(impl: LogoutUC): ILogoutUC

    // Email & profile
    @Binds
    abstract fun bindsSendEmailVerificationUC(impl: SendEmailVerificationUC): ISendEmailVerificationUC

    @Binds
    abstract fun bindsCheckEmailVerifiedUC(impl: CheckEmailVerifiedUC): ICheckEmailVerifiedUC

    @Binds
    abstract fun bindsUpdateProfileUC(impl: UpdateFirebaseProfileUC): IUpdateFirebaseProfileUC

    @Binds
    abstract fun bindsUpdateEmailUC(impl: UpdateEmailUC): IUpdateEmailUC

    // Language
    @Binds
    abstract fun bindsSetAppLanguageUC(impl: SetAppLanguageUC): ISetAppLanguageUC

    @Binds
    abstract fun bindsGetAppLanguageUC(impl: GetAppLanguageUC): IGetAppLanguageUC

    @Binds
    abstract fun bindsTakeLanguageForMigrationUC(impl: TakeLanguageForMigrationUC): ITakeLanguageForMigrationUC

    // В SingletonComponent, а не в ViewModelComponent: нужен ещё и BroadcastReceiver-у
    @Binds
    abstract fun bindsScheduleBirthdayNotificationUC(
        impl: ScheduleBirthdayNotificationUC
    ): IScheduleBirthdayNotificationUC
}