package com.foxynest.testappforonboarding.presentation

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.foxynest.testappforonboarding.domain.onboardingAndPaywallUseCase.IOnboardingUseCase
import com.foxynest.testappforonboarding.domain.entities.AnswerNames
import com.foxynest.testappforonboarding.other.MyEvent
import com.foxynest.testappforonboarding.other.Resource
import com.foxynest.testappforonboarding.other.constants.CommonConstants
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.IOException
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val onboardingInteractor: IOnboardingUseCase
) : ViewModel() {
    companion object {
        private const val TAG = "MainViewModel"
    }

    private val _genderLiveData = MutableLiveData<String>()
    val genderLiveData: LiveData<String> = _genderLiveData

    private val _nameLiveData = MutableLiveData<String>()
    val nameLiveData: LiveData<String> = _nameLiveData

    private val _isItABoyLiveData = MutableLiveData<String>()
    val isItABoyLiveData: LiveData<String> = _isItABoyLiveData

    private val _hobbyLiveData = MutableLiveData<String>()
    val hobbyLiveData: LiveData<String> = _hobbyLiveData

    // If smth went wrong
    private val _errorMessageLiveData =
        MutableLiveData<MyEvent<Resource<Boolean>>>() // It must be private, so that other classes can't change it
    val errorMessageLiveData: LiveData<MyEvent<Resource<Boolean>>> =
        _errorMessageLiveData // And another LiveData, that equals to previous, so that classes can't change it

    // Для временного хранения во время заполнения опроса (нужно при возвращении назад по страницам в онбординге)
    private val _answers = mutableMapOf<String, String>()
    val answers: Map<String, String> get() = _answers

//        private val _text = MutableLiveData<String>().apply {
//        value = "This is Profile Fragment"
//    }
//    val text: LiveData<String> = _text

    fun cleanNotReplaceableAnswers() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Указываем все ответы, которые находятся на необязательных фрагментах. Которые могут отобразиться, а могут нет, в зависимости от ответов. Нужно сохранить дефолтное состояние
                onboardingInteractor.saveIsItABoy("Нет")
                saveAnswer(AnswerNames.IS_IT_A_BOY_ANSWER.toString(), "Нет")
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
            }
        }
    }

    fun cleanFragment2AnswersBeforeBackButton() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Указываем все ответы, которые находятся на втором фрагменте
                onboardingInteractor.saveIsItABoy("Нет")
                saveAnswer(AnswerNames.IS_IT_A_BOY_ANSWER.toString(), "Нет")
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
            }
        }
    }

    fun cleanFragment3AnswersBeforeBackButton() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Указываем все ответы, которые находятся на третьем фрагменте
                onboardingInteractor.saveHobbyAnswer("")
                saveAnswer(AnswerNames.HOBBY_ANSWER.toString(), "")
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
            }
        }
    }

    fun saveGenderAnswer(genderAnswer: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                onboardingInteractor.saveGender(genderAnswer)
                saveAnswer(AnswerNames.GENDER_ANSWER.toString(), genderAnswer)
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
            }
        }
    }

    fun saveNameAnswer(nameAnswer: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                onboardingInteractor.saveName(nameAnswer)
                saveAnswer(AnswerNames.NAME_ANSWER.toString(), nameAnswer)
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
            }
        }
    }

    fun saveIsItABoyAnswer(answer: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                onboardingInteractor.saveIsItABoy(answer)
                saveAnswer(AnswerNames.IS_IT_A_BOY_ANSWER.toString(), answer)
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
            }
        }
    }

    fun saveHobbyAnswer(hobby: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                onboardingInteractor.saveHobbyAnswer(hobby)
                saveAnswer(AnswerNames.HOBBY_ANSWER.toString(), hobby)
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
            }
        }
    }

    // Запустить метод для получения этих данных можно, когда открывается paywallfragment - в этот момент опрос уже точно пройден
    fun getAndUseAnswersFromOnboardind() {
        getAndUseGenderAnswer()
        getAndUseNameAnswer()
        getAndUseIsItABoyAnswer()
        getAndHobbyAnswer()
    }

    private fun getAndUseGenderAnswer() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                onboardingInteractor.getGenderFlow().collect { gender ->
                    gender?.let { notNullGender ->
                        _genderLiveData.postValue(notNullGender)
//                        _answers["GENDER"] = notNullGender
                    }
                }
            } catch (e: IOException) {
                e.printStackTrace()
                _errorMessageLiveData.postValue(
                    MyEvent(
                        Resource.error(
                            CommonConstants.FAILED_TO_LOAD_LOCAL,
                            null
                        )
                    )
                )
            }
        }
    }

    private fun getAndUseNameAnswer() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                onboardingInteractor.getNameFlow().collect { gender ->
                    gender?.let { notNullGender ->
                        _nameLiveData.postValue(notNullGender)
                    }
                }
            } catch (e: IOException) {
                e.printStackTrace()
                _errorMessageLiveData.postValue(
                    MyEvent(
                        Resource.error(
                            CommonConstants.FAILED_TO_LOAD_LOCAL,
                            null
                        )
                    )
                )
            }
        }
    }

    private fun getAndUseIsItABoyAnswer() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                onboardingInteractor.getIsItABoyFlow().collect { gender ->
                    gender?.let { notNullGender ->
                        _isItABoyLiveData.postValue(notNullGender)
                    }
                }
            } catch (e: IOException) {
                e.printStackTrace()
                _errorMessageLiveData.postValue(
                    MyEvent(
                        Resource.error(
                            CommonConstants.FAILED_TO_LOAD_LOCAL,
                            null
                        )
                    )
                )
            }
        }
    }

    private fun getAndHobbyAnswer() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                onboardingInteractor.getHobbyFlow().collect { gender ->
                    gender?.let { notNullGender ->
                        _hobbyLiveData.postValue(notNullGender)
                    }
                }
            } catch (e: IOException) {
                e.printStackTrace()
                _errorMessageLiveData.postValue(
                    MyEvent(
                        Resource.error(
                            CommonConstants.FAILED_TO_LOAD_LOCAL,
                            null
                        )
                    )
                )
            }
        }
    }

    private fun saveAnswer(questionName: String, answer: String) {
        _answers[questionName] = answer
        Log.d(
            TAG,
            "Проверка: Размер ${_answers.size}, Пол: ${_answers[AnswerNames.GENDER_ANSWER.toString()]}"
        )
    }
}