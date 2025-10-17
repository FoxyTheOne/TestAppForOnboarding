package com.foxynest.testappforonboarding.presentation

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.foxynest.testappforonboarding.di.DispatcherIO
import com.foxynest.testappforonboarding.domain.entities.AnswerNames
import com.foxynest.testappforonboarding.domain.entities.ErrorType
import com.foxynest.testappforonboarding.domain.entities.UserPresentation
import com.foxynest.testappforonboarding.domain.use_cases.authUseCases.login.IGetAuthStateFlowUC
import com.foxynest.testappforonboarding.domain.use_cases.authUseCases.login.IGetCurrentUserUC
import com.foxynest.testappforonboarding.domain.use_cases.authUseCases.login.IUpdateProfileUC
import com.foxynest.testappforonboarding.domain.use_cases.onboardingUseCases.IOnboardingUC
import com.foxynest.testappforonboarding.other.MyEvent
import com.foxynest.testappforonboarding.other.Resource
import com.foxynest.testappforonboarding.other.Status
import com.foxynest.testappforonboarding.other.constants.CommonConstants
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
    private val _userNameLiveData = MutableLiveData<String>()
    val userNameLiveData: LiveData<String> = _userNameLiveData
    private val _emailLiveData = MutableLiveData<String>()
    val emailLiveData: LiveData<String> = _emailLiveData
    private val _genderLiveData = MutableLiveData<String>()
    val genderLiveData: LiveData<String> = _genderLiveData
    private val _isItABoyLiveData = MutableLiveData<String>()
    val isItABoyLiveData: LiveData<String> = _isItABoyLiveData
    private val _childNameLiveData = MutableLiveData<String>()
    val childNameLiveData: LiveData<String> = _childNameLiveData
    private val _childBirthDateLiveData = MutableLiveData<String>()
    val childBirthDateLiveData: LiveData<String> = _childBirthDateLiveData
    private val _hobbyLiveData = MutableLiveData<String>()
    val hobbyLiveData: LiveData<String> = _hobbyLiveData

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

    // ONBOARDING - CLEAN
    fun cleanNotReplaceableAnswers() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Указываем все ответы, которые находятся на необязательных фрагментах. Которые могут отобразиться, а могут нет, в зависимости от ответов. Нужно сохранить дефолтное состояние
                onboardingUC.saveIsItABoy("Нет")
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
                onboardingUC.saveIsItABoy("Нет")
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
                onboardingUC.saveBirthDate("")
                onboardingUC.saveHobbyAnswer("")
                saveAnswer(AnswerNames.CHILD_BIRTH_DATE_ANSWER.toString(), "")
                saveAnswer(AnswerNames.CHILD_HOBBY_ANSWER.toString(), "")
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

    // ONBOARDING - SAVE
    fun saveUserNameAnswer(userNameAnswer: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val trimmedUserNameAnswer = userNameAnswer.trim()
            try {
                onboardingUC.saveUserName(trimmedUserNameAnswer) // Сохраняем в data store
                saveAnswer(AnswerNames.USER_NAME_ANSWER.toString(), trimmedUserNameAnswer)
            } catch (e: IOException) {
                Log.e(TAG, "Failed to save user name", e)
                _errorMessageLiveData.postValue(
                    MyEvent(
                        Resource.error(
                            "Не удалось сохранить ваше имя на вашем телефоне",
                            ErrorType.FAILED_TO_SAVE_USER_NAME_LOCAL
                        )
                    )
                )
            } catch (e: Exception) {
                Log.e(TAG, "Unexpected error saving user name", e)
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

    fun saveEmailAnswer(emailAnswer: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val trimmedEmailAnswer = emailAnswer.trim()
            try {
                onboardingUC.saveEmail(trimmedEmailAnswer) // Сохраняем в data store
                saveAnswer(AnswerNames.EMAIL_ANSWER.toString(), trimmedEmailAnswer)
            } catch (e: IOException) {
                Log.e(TAG, "Failed to save email", e)
                _errorMessageLiveData.postValue(
                    MyEvent(
                        Resource.error(
                            "Не удалось сохранить email на вашем телефоне",
                            ErrorType.FAILED_TO_SAVE_EMAIL_LOCAL
                        )
                    )
                )
            } catch (e: Exception) {
                Log.e(TAG, "Unexpected error saving email", e)
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

    fun saveGenderAnswer(genderAnswer: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val trimmedGenderAnswer = genderAnswer.trim()
            try {
                onboardingUC.saveGender(trimmedGenderAnswer) // Сохраняем в data store
                saveAnswer(AnswerNames.CHILD_GENDER_ANSWER.toString(), trimmedGenderAnswer)
            } catch (e: IOException) {
                Log.e(TAG, "Failed to save gender", e)
                _errorMessageLiveData.postValue(
                    MyEvent(
                        Resource.error(
                            "Не удалось сохранить пол ребенка на вашем телефоне",
                            ErrorType.FAILED_TO_SAVE_GENDER_LOCAL
                        )
                    )
                )
            } catch (e: Exception) {
                Log.e(TAG, "Unexpected error saving gender", e)
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

    fun saveChildNameAnswer(nameAnswer: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val trimmedNameAnswer = nameAnswer.trim()
            try {
                onboardingUC.saveName(trimmedNameAnswer)
                saveAnswer(AnswerNames.CHILD_NAME_ANSWER.toString(), trimmedNameAnswer)
            } catch (e: IOException) {
                Log.e(TAG, "Failed to save child name", e)
                _errorMessageLiveData.postValue(
                    MyEvent(
                        Resource.error(
                            "Не удалось сохранить имя ребенка на вашем телефоне",
                            ErrorType.FAILED_TO_SAVE_CHILD_NAME_LOCAL
                        )
                    )
                )
            } catch (e: Exception) {
                Log.e(TAG, "Unexpected error saving child name", e)
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

    fun saveIsItABoyAnswer(answer: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val trimmedAnswer = answer.trim()
            try {
                onboardingUC.saveIsItABoy(trimmedAnswer)
                saveAnswer(AnswerNames.IS_IT_A_BOY_ANSWER.toString(), trimmedAnswer)
            } catch (e: IOException) {
                Log.e(TAG, "Failed to save is it a boy", e)
                _errorMessageLiveData.postValue(
                    MyEvent(
                        Resource.error(
                            "Не удалось сохранить is it s boy на вашем телефоне",
                            ErrorType.FAILED_TO_SAVE_IS_IT_A_BOY_LOCAL
                        )
                    )
                )
            } catch (e: Exception) {
                Log.e(TAG, "Unexpected error saving is it a boy", e)
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

    fun saveBirthDateAnswer(birthDate: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val trimmedBirthDate = birthDate.trim()
            try {
                onboardingUC.saveBirthDate(trimmedBirthDate)
                saveAnswer(AnswerNames.CHILD_BIRTH_DATE_ANSWER.toString(), trimmedBirthDate)
            } catch (e: IOException) {
                Log.e(TAG, "Failed to save birth date", e)
                _errorMessageLiveData.postValue(
                    MyEvent(
                        Resource.error(
                            "Не удалось сохранить день рождения ребенка на вашем телефоне",
                            ErrorType.FAILED_TO_SAVE_BIRTH_DATE_LOCAL
                        )
                    )
                )
            } catch (e: Exception) {
                Log.e(TAG, "Unexpected error saving birth date", e)
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

    fun saveHobbyAnswer(hobby: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val trimmedHobbyAnswer = hobby.trim()
            try {
                onboardingUC.saveHobbyAnswer(trimmedHobbyAnswer)
                saveAnswer(AnswerNames.CHILD_HOBBY_ANSWER.toString(), trimmedHobbyAnswer)
            } catch (e: IOException) {
                Log.e(TAG, "Failed to save hobby", e)
                _errorMessageLiveData.postValue(
                    MyEvent(
                        Resource.error(
                            "Не удалось сохранить хобби на вашем телефоне",
                            ErrorType.FAILED_TO_SAVE_HOBBY_LOCAL
                        )
                    )
                )
            } catch (e: Exception) {
                Log.e(TAG, "Unexpected error saving hobby", e)
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

    // ONBOARDING - GET
    // Запустить метод для получения этих данных можно, когда открывается PAYWALL FRAGMENT - в этот момент опрос уже точно пройден
    fun getAndUseAnswersFromOnboarding(forceUpdate: Boolean = false) {

        // Теперь мы вызываем опросник не только при первом запуске, но и если нажали кнопку "обновить поля"
        // Значит, мы можем находиться здесь и для обновления существующих полей. Узнаем, залогинен ли человек, чтобы знать, нужно ли сохранять данные на сервер или использовать их через Data store
        viewModelScope.launch(Dispatchers.IO) {
            val currentUser = getCurrentUserUC.execute().data

            // ЗАГРУЖАЕМ ДАННЫЕ ИЗ DATASTORE СРАЗУ (синхронно)
            loadDataFromDatastore()

//            getAndUseUserNameAnswer()
//            getAndUseEmailAnswer()
//            getAndUseGenderAnswer()
//            getAndUseChildNameAnswer()
//            getAndUseIsItABoyAnswer()
//            getAndUseBirthDateAnswer()
//            getAndHobbyAnswer()

            if (currentUser != null) {
                val updatedUser = createUserFromAnswers(currentUser.uid)
                _profileUpdatedLiveData.postValue(updateProfileUC.execute(updatedUser))
            }
        }
    }

    private suspend fun loadDataFromDatastore() {
        try {
            // Загружаем все данные параллельно для скорости
            val userName = withContext(Dispatchers.IO) {
                onboardingUC.getUserNameFlow().first()
            }
            val email = withContext(Dispatchers.IO) {
                onboardingUC.getEmailFlow().first()
            }
            val gender = withContext(Dispatchers.IO) {
                onboardingUC.getGenderFlow().first()
            }
            val childName = withContext(Dispatchers.IO) {
                onboardingUC.getNameFlow().first()
            }
            val isItABoy = withContext(Dispatchers.IO) {
                onboardingUC.getIsItABoyFlow().first()
            }
            val birthDate = withContext(Dispatchers.IO) {
                onboardingUC.getBirthDateFlow().first()
            }
            val hobby = withContext(Dispatchers.IO) {
                onboardingUC.getHobbyFlow().first()
            }

            // Обновляем LiveData на главном потоке
            withContext(Dispatchers.Main) {
                userName?.let { _userNameLiveData.value = it }
                email?.let { _emailLiveData.value = it }
                gender?.let { _genderLiveData.value = it }
                childName?.let { _childNameLiveData.value = it }
                isItABoy?.let { _isItABoyLiveData.value = it }
                birthDate?.let { _childBirthDateLiveData.value = it }
                hobby?.let { _hobbyLiveData.value = it }
            }

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

    fun retryDataSave() {
        viewModelScope.launch(Dispatchers.IO) {
            _retrySaveState.postValue(Resource.loading(null))

            // Получаем текущие данные из ViewModel и пытаемся сохранить снова
            val currentUser = getCurrentUserUC.execute().data
            if (currentUser != null) {
                // Повторно собираем данные для сохранения
                val updatedUser = createUserFromAnswers(currentUser.uid)
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

    private fun createUserFromAnswers(currentUserUid: String): UserPresentation {
        return UserPresentation(
            uid = currentUserUid,
            email = answers[AnswerNames.EMAIL_ANSWER.toString()] ?: "",
            isEmailVerified = answers[AnswerNames.IS_EMAIL_VERIFIED_ANSWER.toString()].toBoolean(),
            userName = answers[AnswerNames.USER_NAME_ANSWER.toString()] ?: "",
            childGender = answers[AnswerNames.CHILD_GENDER_ANSWER.toString()] ?: "",
            isItABoy = answers[AnswerNames.IS_IT_A_BOY_ANSWER.toString()] ?: "",
            childName = answers[AnswerNames.CHILD_NAME_ANSWER.toString()] ?: "",
            childHobby = answers[AnswerNames.CHILD_HOBBY_ANSWER.toString()] ?: "",
            childBirthDate = answers[AnswerNames.CHILD_BIRTH_DATE_ANSWER.toString()] ?: ""
        )
    }

//    private fun getAndUseUserNameAnswer() {
//        viewModelScope.launch(Dispatchers.IO) {
//            try {
//                onboardingUC.getUserNameFlow()
//                    .take(1) // Берем только первое значение
//                    .collect { userName ->
//                        userName?.let {
//                            _userNameLiveData.postValue(it)
//                        }
//                    }
//            } catch (e: IOException) {
//                e.printStackTrace()
//                _errorMessageLiveData.postValue(
//                    MyEvent(
//                        Resource.error(
//                            CommonConstants.FAILED_TO_LOAD_LOCAL,
//                            null
//                        )
//                    )
//                )
//            }
//        }
//    }
//
//    private fun getAndUseEmailAnswer() {
//        viewModelScope.launch(Dispatchers.IO) {
//            try {
//                onboardingUC.getEmailFlow()
//                    .take(1)
//                    .collect { email ->
//                        email?.let {
//                            _emailLiveData.postValue(it)
//                        }
//                    }
//            } catch (e: IOException) {
//                e.printStackTrace()
//                _errorMessageLiveData.postValue(
//                    MyEvent(
//                        Resource.error(
//                            CommonConstants.FAILED_TO_LOAD_LOCAL,
//                            null
//                        )
//                    )
//                )
//            }
//        }
//    }
//
//    private fun getAndUseGenderAnswer() {
//        viewModelScope.launch(Dispatchers.IO) {
//            try {
//                onboardingUC.getGenderFlow()
//                    .take(1)
//                    .collect { gender ->
//                        gender?.let { notNullGender ->
//                            _genderLiveData.postValue(notNullGender)
//                        }
//                    }
//            } catch (e: IOException) {
//                e.printStackTrace()
//                _errorMessageLiveData.postValue(
//                    MyEvent(
//                        Resource.error(
//                            CommonConstants.FAILED_TO_LOAD_LOCAL,
//                            null
//                        )
//                    )
//                )
//            }
//        }
//    }
//
//    private fun getAndUseChildNameAnswer() {
//        viewModelScope.launch(Dispatchers.IO) {
//            try {
//                onboardingUC.getNameFlow()
//                    .take(1)
//                    .collect { gender ->
//                        gender?.let { notNullGender ->
//                            _childNameLiveData.postValue(notNullGender)
//                        }
//                    }
//            } catch (e: IOException) {
//                e.printStackTrace()
//                _errorMessageLiveData.postValue(
//                    MyEvent(
//                        Resource.error(
//                            CommonConstants.FAILED_TO_LOAD_LOCAL,
//                            null
//                        )
//                    )
//                )
//            }
//        }
//    }
//
//    private fun getAndUseIsItABoyAnswer() {
//        viewModelScope.launch(Dispatchers.IO) {
//            try {
//                onboardingUC.getIsItABoyFlow()
//                    .take(1)
//                    .collect { gender ->
//                        gender?.let { notNullGender ->
//                            _isItABoyLiveData.postValue(notNullGender)
//                        }
//                    }
//            } catch (e: IOException) {
//                e.printStackTrace()
//                _errorMessageLiveData.postValue(
//                    MyEvent(
//                        Resource.error(
//                            CommonConstants.FAILED_TO_LOAD_LOCAL,
//                            null
//                        )
//                    )
//                )
//            }
//        }
//    }
//
//    private fun getAndUseBirthDateAnswer() {
//        viewModelScope.launch(Dispatchers.IO) {
//            try {
//                onboardingUC.getBirthDateFlow()
//                    .take(1)
//                    .collect { birthDate ->
//                        birthDate?.let {
//                            _childBirthDateLiveData.postValue(it)
//                        }
//                    }
//            } catch (e: IOException) {
//                e.printStackTrace()
//                _errorMessageLiveData.postValue(
//                    MyEvent(
//                        Resource.error(
//                            CommonConstants.FAILED_TO_LOAD_LOCAL,
//                            null
//                        )
//                    )
//                )
//            }
//        }
//    }
//
//    private fun getAndHobbyAnswer() {
//        viewModelScope.launch(Dispatchers.IO) {
//            try {
//                onboardingUC.getHobbyFlow()
//                    .take(1)
//                    .collect { gender ->
//                        gender?.let { notNullGender ->
//                            _hobbyLiveData.postValue(notNullGender)
//                        }
//                    }
//            } catch (e: IOException) {
//                e.printStackTrace()
//                _errorMessageLiveData.postValue(
//                    MyEvent(
//                        Resource.error(
//                            CommonConstants.FAILED_TO_LOAD_LOCAL,
//                            null
//                        )
//                    )
//                )
//            }
//        }
//    }

    private fun saveAnswer(questionName: String, answer: String) {
        _answers[questionName] = answer
        Log.d(
            TAG,
            "Проверка: Размер ${_answers.size}, Пол: ${_answers[AnswerNames.CHILD_GENDER_ANSWER.toString()]}"
        )
    }

    fun checkLoggedInStateGetUser() {
        viewModelScope.launch {
            _checkedUserInfo.value = getCurrentUserUC.execute()
        }
    }

    fun clearUserDataOnLogout() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Очищаем статус онбординга при ВЫХОДЕ ИЗ АККАУНТА
//                val recourseResult = onboardingUC.saveOnboardingState(false)
//                if (recourseResult.status == Status.SUCCESS) {
                onboardingUC.saveOnboardingState(false)

                // Очищаем LiveData
                withContext(Dispatchers.Main) {
                    _userNameLiveData.postValue("")
                    _emailLiveData.postValue("")
                    _genderLiveData.postValue("")
                    _isItABoyLiveData.postValue("")
                    _childNameLiveData.postValue("")
                    _childBirthDateLiveData.postValue("")
                    _hobbyLiveData.postValue("")
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
}