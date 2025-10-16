package com.foxynest.testappforonboarding.presentation.welcome.onboardingAndPaywall

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.foxynest.testappforonboarding.domain.use_cases.onboardingUseCases.IOnboardingUC
import com.foxynest.testappforonboarding.other.MyEvent
import com.foxynest.testappforonboarding.other.Resource
import com.foxynest.testappforonboarding.other.constants.CommonConstants
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.IOException
import javax.inject.Inject

@HiltViewModel
class PaywallViewModel @Inject constructor(
    private val onboardingInteractor: IOnboardingUC
) : ViewModel() {
    companion object {
        private const val TAG = "PaywallViewModel"
    }

    private val _errorMessageLiveData =
        MutableLiveData<MyEvent<Resource<Boolean>>>()
    val errorMessageLiveData: LiveData<MyEvent<Resource<Boolean>>> =
        _errorMessageLiveData

    fun saveOnboardingState(onboardingState: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                //
                onboardingInteractor.saveOnboardingState(onboardingState)
            } catch (e: IOException) {
                e.printStackTrace()
                _errorMessageLiveData.postValue(
                    MyEvent(
                        Resource.error(
                            CommonConstants.FAILED_TO_SAVE_ONBOARDING_STATE_LOCAL,
                            null
                        )
                    )
                )
            }
        }
    }
}