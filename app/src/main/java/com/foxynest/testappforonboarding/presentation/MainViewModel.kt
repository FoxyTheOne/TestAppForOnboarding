package com.foxynest.testappforonboarding.presentation

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.foxynest.testappforonboarding.di.DispatcherIO
import com.foxynest.testappforonboarding.domain.entities.onboarding.AnswerNames
import com.foxynest.testappforonboarding.domain.entities.ErrorType
import com.foxynest.testappforonboarding.domain.entities.onboarding.OnboardingAnswers
import com.foxynest.testappforonboarding.domain.entities.user.UserPresentation
import com.foxynest.testappforonboarding.domain.use_cases.authUseCases.login.IGetAuthStateFlowUC
import com.foxynest.testappforonboarding.domain.use_cases.authUseCases.login.IGetCurrentUserUC
import com.foxynest.testappforonboarding.domain.use_cases.authUseCases.login.IUpdateProfileUC
import com.foxynest.testappforonboarding.domain.use_cases.onboardingUseCases.IOnboardingUC
import com.foxynest.testappforonboarding.other.MyEvent
import com.foxynest.testappforonboarding.other.Resource
import com.foxynest.testappforonboarding.other.Status
import com.foxynest.testappforonboarding.other.constants.CommonConstants
import com.foxynest.testappforonboarding.other.extensions.createUserFromAnswersInViewModel
import com.foxynest.testappforonboarding.other.extensions.isValidToSaveToFirebase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import javax.inject.Inject

// TODO можно ли что-то вынести отсюда в другие классы
@HiltViewModel
class MainViewModel @Inject constructor(
    private val onboardingUC: IOnboardingUC,
    private val updateProfileUC: IUpdateProfileUC,
    private val getCurrentUserUC: IGetCurrentUserUC,
    private val getAuthStateFlowUC: IGetAuthStateFlowUC,
    @DispatcherIO private val dispatcher: CoroutineDispatcher = Dispatchers.IO // TODO расставить dispatcher или удалить тесты
) : ViewModel() {
    companion object {
        private const val TAG = "MainViewModel"
    }

    // ONBOARDING
    // В MainViewModel - ВМЕСТО 7 LiveData используем одну:
    private val _onboardingAnswersLiveData = MutableLiveData<OnboardingAnswers>(OnboardingAnswers())
    val onboardingAnswersLiveData: LiveData<OnboardingAnswers> = _onboardingAnswersLiveData

    // Для временного хранения во время заполнения опроса (нужно при возвращении назад по страницам в онбординге)
    private val _answers = mutableMapOf<String, String>()
    val answers: Map<String, String> get() = _answers

    private val _profileUpdatedLiveData = MutableLiveData<Resource<ErrorType>>()
    val profileUpdatedLiveData: LiveData<Resource<ErrorType>> = _profileUpdatedLiveData

    // Разовая проверка состояния в firebase
    private val _checkedUserInfo =
        MutableStateFlow<Resource<UserPresentation?>>(Resource.init(null))
    val checkedUserInfo: StateFlow<Resource<UserPresentation?>> = _checkedUserInfo

    // Использовать только для проверки состояния auth. User мы получаем другим методом
    private val _authStateFlow = getAuthStateFlowUC.execute()
        .map { firebaseUser ->
            firebaseUser?.let {
                UserPresentation(
                    uid = firebaseUser.uid,
                    email = firebaseUser.email ?: "",
                    isEmailVerified = firebaseUser.isEmailVerified,
                    userName = firebaseUser.displayName ?: ""
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), null)
    val authStateFlow: StateFlow<UserPresentation?> = _authStateFlow

    private val _errorMessageLiveData =
        MutableLiveData<MyEvent<Resource<ErrorType>>>()
    val errorMessageLiveData: LiveData<MyEvent<Resource<ErrorType>>> =
        _errorMessageLiveData

    // Добавляем отдельный LiveData для состояния повторного сохранения
    private val _retrySaveState = MutableLiveData<Resource<ErrorType>>()
    val retrySaveState: LiveData<Resource<ErrorType>> = _retrySaveState

    fun checkLoggedInStateGetUser() {
        viewModelScope.launch {
            _checkedUserInfo.value = getCurrentUserUC.execute()
        }
    }

    // ONBOARDING - CLEAN
    fun cleanNotReplaceableAnswers() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Указываем все ответы, которые находятся на необязательных фрагментах. Которые могут отобразиться, а могут нет, в зависимости от ответов. Нужно сохранить дефолтное состояние
                onboardingUC.saveIsItABoy("Нет")
                saveAnswerToMainViewModel(AnswerNames.IS_IT_A_BOY_ANSWER.toString(), "Нет")
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
                onboardingUC.saveIsItABoy("Нет")
                saveAnswerToMainViewModel(AnswerNames.IS_IT_A_BOY_ANSWER.toString(), "Нет")
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
                onboardingUC.saveBirthDate("")
                onboardingUC.saveHobbyAnswer("")
                saveAnswerToMainViewModel(AnswerNames.CHILD_BIRTH_DATE_ANSWER.toString(), "")
                saveAnswerToMainViewModel(AnswerNames.CHILD_HOBBY_ANSWER.toString(), "")
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

    fun clearUserDataOnLogout() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Очищаем статус онбординга при ВЫХОДЕ ИЗ АККАУНТА
//                val recourseResult = onboardingUC.saveOnboardingState(false)
//                if (recourseResult.status == Status.SUCCESS) {
                onboardingUC.saveOnboardingState(false)

                val answers = OnboardingAnswers(
                    email = "",
                    userName = "",
                    childGender = "",
                    isItABoy = "",
                    childName = "",
                    childBirthDate = "",
                    childHobby = ""
                )

                // Очищаем LiveData - обновляем LiveData на главном потоке
                withContext(Dispatchers.Main) {
                    _onboardingAnswersLiveData.postValue(answers)
                }

                // Очищаем временные ответы
                _answers.clear()

                // Очищаем проверенного пользователя
                _checkedUserInfo.value = Resource.init(null)
//                }
            } catch (e: Exception) {
                Log.e(TAG, "Error clearing user data on logout", e)
                _errorMessageLiveData.postValue(
                    MyEvent(
                        Resource.error(
                            "Ошибка при очистке данных",
                            ErrorType.FAILED_TO_CLEAR_DATA
                        )
                    )
                )
            }
        }
    }

    // ONBOARDING - SAVE
    fun saveUserNameAnswer(userNameAnswer: String) {
        saveOperation(
            userNameAnswer,
            onboardingUC::saveUserName,
            AnswerNames.USER_NAME_ANSWER,
            ErrorType.FAILED_TO_SAVE_USER_NAME_LOCAL
        )
        updateOnboardingAnswers { it.copy(userName = userNameAnswer.trim()) }
    }

    fun saveEmailAnswer(emailAnswer: String) {
        saveOperation(
            emailAnswer,
            onboardingUC::saveEmail,
            AnswerNames.EMAIL_ANSWER,
            ErrorType.FAILED_TO_SAVE_EMAIL_LOCAL
        )
        updateOnboardingAnswers { it.copy(email = emailAnswer.trim()) }
    }

    fun saveGenderAnswer(genderAnswer: String) {
        saveOperation(
            genderAnswer,
            onboardingUC::saveGender,
            AnswerNames.CHILD_GENDER_ANSWER,
            ErrorType.FAILED_TO_SAVE_GENDER_LOCAL
        )
        updateOnboardingAnswers { it.copy(childGender = genderAnswer.trim()) }
    }

    fun saveChildNameAnswer(nameAnswer: String) {
        saveOperation(
            nameAnswer,
            onboardingUC::saveName,
            AnswerNames.CHILD_NAME_ANSWER,
            ErrorType.FAILED_TO_SAVE_CHILD_NAME_LOCAL
        )
        updateOnboardingAnswers { it.copy(childName = nameAnswer.trim()) }
    }

    fun saveIsItABoyAnswer(answer: String) {
        saveOperation(
            answer,
            onboardingUC::saveIsItABoy,
            AnswerNames.IS_IT_A_BOY_ANSWER,
            ErrorType.FAILED_TO_SAVE_IS_IT_A_BOY_LOCAL
        )
        updateOnboardingAnswers { it.copy(isItABoy = answer.trim()) }
    }

    fun saveBirthDateAnswer(birthDate: String) {
        saveOperation(
            birthDate,
            onboardingUC::saveBirthDate,
            AnswerNames.CHILD_BIRTH_DATE_ANSWER,
            ErrorType.FAILED_TO_SAVE_BIRTH_DATE_LOCAL
        )
        updateOnboardingAnswers { it.copy(childBirthDate = birthDate.trim()) }
    }

    fun saveHobbyAnswer(hobby: String) {
        saveOperation(
            hobby,
            onboardingUC::saveHobbyAnswer,
            AnswerNames.CHILD_HOBBY_ANSWER,
            ErrorType.FAILED_TO_SAVE_HOBBY_LOCAL
        )
        updateOnboardingAnswers { it.copy(childHobby = hobby.trim()) }
    }

    fun retryDataSave() {
        viewModelScope.launch(Dispatchers.IO) {
            _retrySaveState.postValue(Resource.loading(null))

            // Получаем текущие данные из ViewModel и пытаемся сохранить снова
            val currentUser = getCurrentUserUC.execute().data
            if (currentUser != null) {
                // Повторно собираем данные для сохранения
                val updatedUser = answers.createUserFromAnswersInViewModel(currentUser.uid)
                val result = updateProfileUC.execute(updatedUser)

                // Отправляем результат в отдельный LiveData для повторных попыток
                _retrySaveState.postValue(result)

                // Также обновляем основной LiveData для согласованности
                if (result.status == Status.SUCCESS) {
                    _profileUpdatedLiveData.postValue(result)
                }
            } else {
                _retrySaveState.postValue(Resource.error("User not authenticated", null))
            }
        }
    }

    // ONBOARDING - GET FROM DATA AND SAVE TO FIREBASE
    // Запустить метод для получения этих данных можно, когда открывается PAYWALL FRAGMENT - в этот момент опрос уже точно пройден
    fun getAnswersFromOnboardingSaveToFirebaseAndUse() {
        viewModelScope.launch(Dispatchers.IO) {
            // Проверим, осуществлён ли вход
            val currentUser = getCurrentUserUC.execute().data

            // ЗАГРУЖАЕМ ДАННЫЕ ИЗ DATASTORE ВСЕ СРАЗУ (синхронно) и кладём их в LiveData
            loadDataFromDatastore()

            // Сохранение в firebase только если осуществлён ли вход, чтобы обновить поля
            if (currentUser != null) {
                // Создаём UserPresentation на основе пройденного опроса
                val updatedUser = answers.createUserFromAnswersInViewModel(currentUser.uid)

                // Проверяем, что данные не пустые перед сохранением в Firebase
                val hasDataToSave = updatedUser.isValidToSaveToFirebase()

                if (hasDataToSave) {
                    // Если всё хорошо - сохранение данных в firestore и в _profileUpdatedLiveData
                    _profileUpdatedLiveData.postValue(updateProfileUC.execute(updatedUser))
                } else {
                    Log.w(TAG, "No data to save to Firebase - skipping update")
                }
            }
        }
    }

    // PRIVATE METHODS
    private fun saveOperation(
        answerToSave: String,
        saveToDataStoreAction: suspend (String) -> Unit,
        answerKey: AnswerNames,
        errorType: ErrorType
    ) {
        viewModelScope.launch(dispatcher) {
            val trimmedAnswer = answerToSave.trim()
            try {
                saveToDataStoreAction(trimmedAnswer)
                saveAnswerToMainViewModel(answerKey.toString(), trimmedAnswer)
            } catch (e: IOException) {
                Log.e(TAG, "Failed to save ${answerKey.name}", e)
                _errorMessageLiveData.postValue(
                    MyEvent(
                        Resource.error(
                            "Не удалось сохранить некоторые данные на вашем телефоне",
                            errorType
                        )
                    )
                )
            } catch (e: Exception) {
                Log.e(TAG, "Unexpected error saving ${answerKey.name}", e)
                _errorMessageLiveData.postValue(
                    MyEvent(
                        Resource.error(
                            "Произошла непредвиденная ошибка",
                            ErrorType.UNEXPECTED_ERROR
                        )
                    )
                )
            }
        }
    }

    private fun saveAnswerToMainViewModel(questionName: String, answer: String) {
        _answers[questionName] = answer
        Log.d(
            TAG,
            "Проверка: Размер ${_answers.size}, Пол: ${_answers[AnswerNames.CHILD_GENDER_ANSWER.toString()]}"
        )
    }

    private suspend fun loadDataFromDatastore() {
        try {
            val email = onboardingUC.getEmailFlow().first() ?: ""
            val userName = onboardingUC.getUserNameFlow().first() ?: ""
            val childGender = onboardingUC.getGenderFlow().first() ?: ""
            val isItABoy = onboardingUC.getIsItABoyFlow().first() ?: ""
            val childName = onboardingUC.getNameFlow().first() ?: ""
            val birthDate = onboardingUC.getBirthDateFlow().first() ?: ""
            val childHobby = onboardingUC.getHobbyFlow().first() ?: ""

            val answers = OnboardingAnswers(
                email = email,
                userName = userName,
                childGender = childGender,
                isItABoy = isItABoy,
                childName = childName,
                childBirthDate = birthDate,
                childHobby = childHobby
            )

            // Обновляем LiveData на главном потоке
            withContext(Dispatchers.Main) {
                _onboardingAnswersLiveData.postValue(answers)
            }

            // Также обновляем временные ответы для совместимости
            _answers.clear()
            _answers[AnswerNames.EMAIL_ANSWER.toString()] = email
            _answers[AnswerNames.USER_NAME_ANSWER.toString()] = userName
            _answers[AnswerNames.CHILD_GENDER_ANSWER.toString()] = childGender
            _answers[AnswerNames.IS_IT_A_BOY_ANSWER.toString()] = isItABoy
            _answers[AnswerNames.CHILD_NAME_ANSWER.toString()] = childName
            _answers[AnswerNames.CHILD_BIRTH_DATE_ANSWER.toString()] = birthDate
            _answers[AnswerNames.CHILD_HOBBY_ANSWER.toString()] = childHobby

        } catch (e: Exception) {
            Log.e(TAG, "Error loading data from DataStore", e)
            _errorMessageLiveData.postValue(
                MyEvent(
                    Resource.error(
                        CommonConstants.FAILED_TO_LOAD_LOCAL,
                        ErrorType.FAILED_TO_LOAD_LOCAL_DATA
                    )
                )
            )
        }
    }

    private fun updateOnboardingAnswers(update: (OnboardingAnswers) -> OnboardingAnswers) {
        val current = _onboardingAnswersLiveData.value ?: OnboardingAnswers()
        _onboardingAnswersLiveData.value = update(current)
    }
}