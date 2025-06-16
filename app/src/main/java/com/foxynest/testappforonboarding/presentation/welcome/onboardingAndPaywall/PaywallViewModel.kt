package com.foxynest.testappforonboarding.presentation.welcome.onboardingAndPaywall

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.foxynest.testappforonboarding.domain.onboardingAndPaywallUseCase.IOnboardingUseCase
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
    private val onboardingInteractor: IOnboardingUseCase
) : ViewModel() {
    companion object {
        private const val TAG = "PaywallViewModel"
    }

    // If smth went wrong
    private val _errorMessageLiveData =
        MutableLiveData<MyEvent<Resource<Boolean>>>() // It must be private, so that other classes can't change it
    val errorMessageLiveData: LiveData<MyEvent<Resource<Boolean>>> =
        _errorMessageLiveData // And another LiveData, that equals to previous, so that classes can't change it

    fun saveOnboardingState(onboardingState: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                onboardingInteractor.saveOnboardingState(onboardingState)
            } catch (e: IOException) {
                e.printStackTrace()
                _errorMessageLiveData.postValue(
                    MyEvent(
                        Resource.error(
                            CommonConstants.FAILED_TO_SAVE_LOCAL,
                            null
                        )
                    )
                )
                // TODO Нужна обработка этой ошибки сохранения. Возможно всплывающее окно, или из paywall или из activity. Например "проблема с подключением к интернету, мы не смогли сохранить результаты вашего опроса в ваш аккаунт. Ответы сохранены только в локальном хранилище и будут удалены при очистке кеша программы. Пожалуйста, проверьте ваше подключение к интернету а затем воспользуйтесь кнопкой "синхронизация" в профиле" и сделать кнопку "синхронизация" для сохранения ответов из локального хранилища в облачное
            }
        }
    }
}