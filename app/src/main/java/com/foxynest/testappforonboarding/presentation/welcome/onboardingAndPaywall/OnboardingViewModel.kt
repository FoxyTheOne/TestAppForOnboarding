package com.foxynest.testappforonboarding.presentation.welcome.onboardingAndPaywall

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.foxynest.testappforonboarding.domain.use_cases.onboardingUseCases.IOnboardingUC
import com.foxynest.testappforonboarding.other.MyEvent
import com.foxynest.testappforonboarding.other.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

// TODO возможно не нужна
@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val onboardingInteractor: IOnboardingUC
) : ViewModel() {
    companion object {
        private const val TAG = "OnboardingViewModel"
    }

    // If smth went wrong
    private val _errorMessageLiveData =
        MutableLiveData<MyEvent<Resource<Boolean>>>() // It must be private, so that other classes can't change it
    val errorMessageLiveData: LiveData<MyEvent<Resource<Boolean>>> =
        _errorMessageLiveData // And another LiveData, that equals to previous, so that classes can't change it

//    fun saveGenderAnswer(genderAnswer: String) {
//        viewModelScope.launch(Dispatchers.IO) {
//            try {
//                onboardingInteractor.saveGender(genderAnswer)
////                saveAnswer(AnswerNames.GENDER_ANSWER.toString(), genderAnswer)
//            } catch (e: IOException) {
//                e.printStackTrace()
//                _errorMessageLiveData.postValue(
//                    MyEvent(
//                        Resource.error(
//                            FAILED_TO_SAVE_LOCAL,
//                            null
//                        )
//                    )
//                )
//            }
//        }
//    }

//    fun saveNameAnswer(nameAnswer: String) {
//        viewModelScope.launch(Dispatchers.IO) {
//            try {
//                onboardingInteractor.saveName(nameAnswer)
////                saveAnswer(AnswerNames.NAME_ANSWER.toString(), nameAnswer)
//            } catch (e: IOException) {
//                e.printStackTrace()
//                _errorMessageLiveData.postValue(
//                    MyEvent(
//                        Resource.error(
//                            FAILED_TO_SAVE_LOCAL,
//                            null
//                        )
//                    )
//                )
//            }
//        }
//    }

//    fun saveIsItABoyAnswer(answer: String) {
//        viewModelScope.launch(Dispatchers.IO) {
//            try {
//                onboardingInteractor.saveIsItABoy(answer)
////                saveAnswer(AnswerNames.IS_IT_A_BOY_ANSWER.toString(), answer)
//            } catch (e: IOException) {
//                e.printStackTrace()
//                _errorMessageLiveData.postValue(
//                    MyEvent(
//                        Resource.error(
//                            FAILED_TO_SAVE_LOCAL,
//                            null
//                        )
//                    )
//                )
//            }
//        }
//    }

//    fun saveHobbyAnswer(hobby: String) {
//        viewModelScope.launch(Dispatchers.IO) {
//            try {
//                onboardingInteractor.saveHobbyAnswer(hobby)
////                saveAnswer(AnswerNames.HOBBY_ANSWER.toString(), hobby)
//            } catch (e: IOException) {
//                e.printStackTrace()
//                _errorMessageLiveData.postValue(
//                    MyEvent(
//                        Resource.error(
//                            FAILED_TO_SAVE_LOCAL,
//                            null
//                        )
//                    )
//                )
//            }
//        }
//    }

//    fun cleanNotReplaceableAnswers() {
//        viewModelScope.launch(Dispatchers.IO) {
//            try {
//                // Указываем все ответы, которые находятся на необязательных фрагментах. Которые могут отобразиться, а могут нет, в зависимости от ответов. Нужно сохранить дефолтное состояние
//                onboardingInteractor.saveIsItABoy("Нет")
////                saveAnswer(AnswerNames.IS_IT_A_BOY_ANSWER.toString(), "Нет")
//            } catch (e: IOException) {
//                e.printStackTrace()
//                _errorMessageLiveData.postValue(
//                    MyEvent(
//                        Resource.error(
//                            FAILED_TO_SAVE_LOCAL,
//                            null
//                        )
//                    )
//                )
//            }
//        }
//    }

//    fun cleanFragment2AnswersBeforeBackButton() {
//        viewModelScope.launch(Dispatchers.IO) {
//            try {
//                // Указываем все ответы, которые находятся на втором фрагменте
//                onboardingInteractor.saveIsItABoy("Нет")
////                saveAnswer(AnswerNames.IS_IT_A_BOY_ANSWER.toString(), "Нет")
//            } catch (e: IOException) {
//                e.printStackTrace()
//                _errorMessageLiveData.postValue(
//                    MyEvent(
//                        Resource.error(
//                            FAILED_TO_SAVE_LOCAL,
//                            null
//                        )
//                    )
//                )
//            }
//        }
//    }

//    fun cleanFragment3AnswersBeforeBackButton() {
//        viewModelScope.launch(Dispatchers.IO) {
//            try {
//                // Указываем все ответы, которые находятся на третьем фрагменте
//                onboardingInteractor.saveHobbyAnswer("")
////                saveAnswer(AnswerNames.HOBBY_ANSWER.toString(), "")
//            } catch (e: IOException) {
//                e.printStackTrace()
//                _errorMessageLiveData.postValue(
//                    MyEvent(
//                        Resource.error(
//                            FAILED_TO_SAVE_LOCAL,
//                            null
//                        )
//                    )
//                )
//            }
//        }
//    }
}