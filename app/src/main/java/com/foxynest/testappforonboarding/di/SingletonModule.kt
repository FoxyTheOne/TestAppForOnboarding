package com.foxynest.testappforonboarding.di

import android.content.Context
import android.content.res.Resources
import com.foxynest.testappforonboarding.data.dataSource.datastorePreferences.AppDatastorePreferences
import com.foxynest.testappforonboarding.data.dataSource.datastorePreferences.IAppDatastorePreferences
import com.foxynest.testappforonboarding.data.repository.AuthRepository
import com.foxynest.testappforonboarding.domain.iRepository.IAuthRepository
import com.foxynest.testappforonboarding.domain.use_cases.authUseCases.email.CheckEmailVerifiedUC
import com.foxynest.testappforonboarding.domain.use_cases.authUseCases.email.ICheckEmailVerifiedUC
import com.foxynest.testappforonboarding.domain.use_cases.authUseCases.email.ISendEmailVerificationUC
import com.foxynest.testappforonboarding.domain.use_cases.authUseCases.email.SendEmailVerificationUC
import com.foxynest.testappforonboarding.domain.use_cases.authUseCases.login.GetAuthStateFlowUC
import com.foxynest.testappforonboarding.domain.use_cases.authUseCases.login.GetCurrentUserUC
import com.foxynest.testappforonboarding.domain.use_cases.authUseCases.login.IGetAuthStateFlowUC
import com.foxynest.testappforonboarding.domain.use_cases.authUseCases.login.IGetCurrentUserUC
import com.foxynest.testappforonboarding.domain.use_cases.authUseCases.login.ILoginUC
import com.foxynest.testappforonboarding.domain.use_cases.authUseCases.login.ILogoutUC
import com.foxynest.testappforonboarding.domain.use_cases.authUseCases.login.IUpdateProfileUC
import com.foxynest.testappforonboarding.domain.use_cases.authUseCases.login.LoginUC
import com.foxynest.testappforonboarding.domain.use_cases.authUseCases.login.LogoutUC
import com.foxynest.testappforonboarding.domain.use_cases.authUseCases.login.UpdateProfileUC
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
        getCurrentLogInStateInteractor: GetCurrentUserUC
    ): IGetCurrentUserUC

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
        updateProfileInteractor: UpdateProfileUC
    ): IUpdateProfileUC
}