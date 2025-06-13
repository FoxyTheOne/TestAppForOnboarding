package com.foxynest.testappforonboarding.di

import com.foxynest.testappforonboarding.data.repository.OnboardingRepository
import com.foxynest.testappforonboarding.domain.iRepository.IOnboardingRepository
import com.foxynest.testappforonboarding.domain.onboardingAndPaywallUseCase.IOnboardingUseCase
import com.foxynest.testappforonboarding.domain.onboardingAndPaywallUseCase.OnboardingUseCase
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent

@Module
@InstallIn(ViewModelComponent::class)
abstract class ViewModelModule {
    @Binds
    abstract fun bindsOnboardingInteractor(
        onboardingInteractor: OnboardingUseCase
    ): IOnboardingUseCase

    @Binds
    abstract fun bindsOnboardingRepository(
        onboardingRepository: OnboardingRepository
    ): IOnboardingRepository
}