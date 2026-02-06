package com.foxynest.testappforonboarding.presentation

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.foxynest.testappforonboarding.R
import com.foxynest.testappforonboarding.di.DispatcherIO
import com.foxynest.testappforonboarding.domain.entities.ErrorType
import com.foxynest.testappforonboarding.domain.entities.onboarding.AnswerMapping
import com.foxynest.testappforonboarding.domain.entities.onboarding.AnswerNames
import com.foxynest.testappforonboarding.domain.entities.onboarding.OnboardingAnswers
import com.foxynest.testappforonboarding.domain.entities.user.UserPresentation
import com.foxynest.testappforonboarding.domain.use_cases.authUseCases.login.IGetAuthStateFlowUC
import com.foxynest.testappforonboarding.domain.use_cases.authUseCases.login.IGetCurrentUserUC
import com.foxynest.testappforonboarding.domain.use_cases.authUseCases.login.ILoginValidateEmailUC
import com.foxynest.testappforonboarding.domain.use_cases.onboardingUseCases.IOnboardingUC
import com.foxynest.testappforonboarding.domain.use_cases.updatingDataUseCases.IUpdateEmailUC
import com.foxynest.testappforonboarding.domain.use_cases.updatingDataUseCases.IUpdateProfileUC
import com.foxynest.testappforonboarding.other.MyEvent
import com.foxynest.testappforonboarding.other.Resource
import com.foxynest.testappforonboarding.other.Status
import com.foxynest.testappforonboarding.other.constants.CommonConstants
import com.foxynest.testappforonboarding.other.extensions.createUpdatedUserFromAnswersInViewModel
import com.foxynest.testappforonboarding.other.extensions.isUserDataValid
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
    private val loginValidateEmailUC: ILoginValidateEmailUC,
    private val updateEmailUC: IUpdateEmailUC,
    @DispatcherIO private val dispatcher: CoroutineDispatcher = Dispatchers.IO // TODO расставить dispatcher или удалить тесты
) : ViewModel() {
    companion object {
        private const val TAG = "MainViewModel"
    }

    // TODO проверить верность получения данных
    private val answerMappings = listOf(
        AnswerMapping(
            answerName = AnswerNames.USER_NAME_ANSWER,
            getter = { onboardingUC.getUserNameFlow().first() },
            setter = onboardingUC::saveUserName,
            defaultValue = ""
        ),
        AnswerMapping(
            answerName = AnswerNames.CHILD_GENDER_ANSWER,
            getter = { onboardingUC.getGenderFlow().first() },
            setter = onboardingUC::saveGender,
            defaultValue = ""
        ),
        AnswerMapping(
            answerName = AnswerNames.IS_IT_A_BOY_ANSWER,
            getter = { onboardingUC.getIsItABoyFlow().first() },
            setter = onboardingUC::saveIsItABoy,
            defaultValue = "Нет"
        ),
        AnswerMapping(
            answerName = AnswerNames.CHILD_NAME_ANSWER,
            getter = { onboardingUC.getNameFlow().first() },
            setter = onboardingUC::saveName,
            defaultValue = ""
        ),
        AnswerMapping(
            answerName = AnswerNames.CHILD_BIRTH_DATE_ANSWER,
            getter = { onboardingUC.getBirthDateFlow().first() },
            setter = onboardingUC::saveBirthDate,
            defaultValue = ""
        ),
        AnswerMapping(
            answerName = AnswerNames.CHILD_HOBBY_ANSWER,
            getter = { onboardingUC.getHobbyFlow().first() },
            setter = onboardingUC::saveHobbyAnswer,
            defaultValue = ""
        )
    )

    // ONBOARDING
    // В MainViewModel - ВМЕСТО 7 LiveData используем одну:
    private val _onboardingAnswersLiveData = MutableLiveData<OnboardingAnswers>(OnboardingAnswers())
    val onboardingAnswersLiveData: LiveData<OnboardingAnswers> = _onboardingAnswersLiveData

    // Добавим временное хранилище для резервного копирования данных (нужно при отмене онбординга)
    private var _onboardingBackup: OnboardingAnswers? = null
    private var _isOnboardingStarted = false
    private var _isOnboardingCompleted = false
    private var _isOnboardingCancelled = false

    // Для временного хранения во время заполнения опроса (нужно при возвращении назад по страницам в онбординге)
    private val _answers = mutableMapOf<String, String>()
    val answers: Map<String, String> get() = _answers

    private val _profileUpdatedLiveData = MutableLiveData<Resource<ErrorType>>()
    val profileUpdatedLiveData: LiveData<Resource<ErrorType>> = _profileUpdatedLiveData

    // Изменение email в профиле по кнопке "изменить"
    private val _authEmailUpdateState = MutableLiveData<Resource<ErrorType>>()
    val authEmailUpdateState: LiveData<Resource<ErrorType>> = _authEmailUpdateState

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
                    authEmail = firebaseUser.email ?: "",
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

    // Создадим отдельную LiveData для управления загрузкой данных
    private val _onboardingDataLoaded = MutableLiveData<Boolean>(false)
    val onboardingDataLoaded: LiveData<Boolean> = _onboardingDataLoaded

    // Инициализация ViewModel с загрузкой данных
    init {
        loadOnboardingDataOnStart()
    }

    /**
     * Подготовка к онбордингу - сохраняем текущие данные в бэкап
     * и сбрасываем все до значений по умолчанию
     */
    fun prepareForOnboarding() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Сбрасываем ВСЕ флаги перед началом нового онбординга
                _isOnboardingStarted = true
                _isOnboardingCompleted = false
                _isOnboardingCancelled = false

                // 1. Сохраняем текущие данные в бэкап
                val currentAnswers = getCurrentAnswersForBackup()
                _onboardingBackup = currentAnswers

                Log.d(TAG, "Onboarding prepared: data backed up. Backup is null: ${_onboardingBackup == null}")

                // 2. Сбрасываем все ответы на значения по умолчанию
                resetAllAnswersToDefault()

                // 3. Очищаем временные ответы в ViewModel
                _answers.clear()

                // 4. Обновляем LiveData с пустыми значениями
                val emptyAnswers = OnboardingAnswers()
                withContext(Dispatchers.Main) {
                    _onboardingAnswersLiveData.postValue(emptyAnswers)
                }

                Log.d(TAG, "Onboarding prepared: data backed up and reset")

            } catch (e: Exception) {
                Log.e(TAG, "Error preparing for onboarding", e)
                _errorMessageLiveData.postValue(
                    MyEvent(
                        Resource.error(
                            "Ошибка при подготовке опроса",
                            ErrorType.UNEXPECTED_ERROR
                        )
                    )
                )
            }
        }
    }

    /**
     * Отмена онбординга - восстанавливаем данные из бэкапа
     */
    fun cancelOnboarding() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                Log.d(TAG, "cancelOnboarding called. Backup is null: ${_onboardingBackup == null}")

                _isOnboardingCancelled = true
                _isOnboardingCompleted = false
                _isOnboardingStarted = false  // Сбрасываем флаг запуска!

                _onboardingBackup?.let { backup ->
                    Log.d(TAG, "Restoring from backup: $backup")
                    // Восстанавливаем данные из бэкапа
                    restoreFromBackup(backup)
                    _onboardingBackup = null // Очищаем бэкап
                    Log.d(TAG, "Onboarding cancelled: data restored from backup")
                } ?: run {
                    Log.d(TAG, "Backup is null, resetting to defaults")
                    // Если бэкапа нет, просто очищаем
                    resetAllAnswersToDefault()

                    // НАДО ОБНОВИТЬ LiveData даже при очистке
                    val emptyAnswers = OnboardingAnswers()
                    withContext(Dispatchers.Main) {
                        _onboardingAnswersLiveData.postValue(emptyAnswers)
                    }
                    Log.d(TAG, "Onboarding cancelled: data reset to defaults (no backup)")
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error cancelling onboarding", e)
                _errorMessageLiveData.postValue(
                    MyEvent(
                        Resource.error(
                            "Ошибка при отмене опроса",
                            ErrorType.UNEXPECTED_ERROR
                        )
                    )
                )
            }
        }
    }

    /**
     * Завершение онбординга - очищаем бэкап
     */
    fun completeOnboarding() {
        _isOnboardingCompleted = true
        _isOnboardingStarted = false  // Сбрасываем флаг запуска
        _isOnboardingCancelled = false
        _onboardingBackup = null
        Log.d(TAG, "Onboarding completed: backup cleared")
    }

    /**
     * ONBOARDING - CLEAN
     */

    fun clearUserDataOnLogout() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // 1. Очищаем статус онбординга при ВЫХОДЕ ИЗ АККАУНТА
//                val recourseResult = onboardingUC.saveOnboardingState(false)
//                if (recourseResult.status == Status.SUCCESS) {
                onboardingUC.saveOnboardingState(false)

                // 2. Очищаем LiveData - обновляем LiveData на главном потоке
                val emptyAnswers = OnboardingAnswers()
                withContext(Dispatchers.Main) {
                    _onboardingAnswersLiveData.postValue(emptyAnswers)
                }

                // 3. Очищаем временные ответы
                _answers.clear()

                // 4. Очищаем DataStore через UC
                onboardingUC.clearAllOnboardingData()

                // 5. Очищаем состояние пользователя
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

    /**
     * ONBOARDING - FLAGS
     */
    fun isOnboardingStarted(): Boolean = _isOnboardingStarted
    fun isOnboardingCompleted(): Boolean = _isOnboardingCompleted
    fun isOnboardingCancelled(): Boolean = _isOnboardingCancelled

    /**
     * ONBOARDING - SAVE
     */
    fun saveUserNameAnswer(userNameAnswer: String) {
        saveOperation(
            userNameAnswer,
            onboardingUC::saveUserName,
            AnswerNames.USER_NAME_ANSWER,
            ErrorType.FAILED_TO_SAVE_USER_NAME_LOCAL
        )
        updateOnboardingAnswers { it.copy(userName = userNameAnswer.trim()) }
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
                val updatedUser = answers.createUpdatedUserFromAnswersInViewModel(currentUser)
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

    fun updateAuthEmail(newAuthEmail: String, password: String) {
        viewModelScope.launch {
            // Валидация email
            val trimmedLowercaseEmail = newAuthEmail.lowercase().trim()
            val validationResult = loginValidateEmailUC.execute(trimmedLowercaseEmail)

            when (validationResult.successful) {
                true -> {
                    _authEmailUpdateState.value = Resource.loading(null)

                    val result = updateEmailUC.updateAuthEmail(trimmedLowercaseEmail, password)
                    _authEmailUpdateState.value = result
                }

                false -> {
                    _authEmailUpdateState.value = Resource.error(
                        "Не верный адрес", validationResult.errorMessage
                    )
                }
            }
        }
    }

    /**
     * ONBOARDING - GET FROM DATA AND SAVE TO FIREBASE
     */
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
                val updatedUser = answers.createUpdatedUserFromAnswersInViewModel(currentUser)

                // Проверяем, что данные не пустые перед сохранением в Firebase
                val hasDataToSave = updatedUser.isUserDataValid()

                if (hasDataToSave) {
                    // Если всё хорошо - сохранение данных в firestore и в _profileUpdatedLiveData
                    _profileUpdatedLiveData.postValue(updateProfileUC.execute(updatedUser))
                } else {
                    Log.w(TAG, "No data to save to Firebase - skipping update")
                }
            }
        }
    }

    /**
     * OTHER
     */
    fun checkLoggedInStateGetUser() {
        viewModelScope.launch {
            _checkedUserInfo.value = getCurrentUserUC.execute()
        }
    }

    /**
     * PRIVATE METHODS
     */
    private suspend fun getCurrentAnswersForBackup(): OnboardingAnswers {
        Log.d(TAG, "getCurrentAnswersForBackup called")

        // TODO проверить логику
        // Проверяем, авторизован ли пользователь
        val currentUser = getCurrentUserUC.execute().data
        Log.d(TAG, "Current user is null: ${currentUser == null}")

        return if (currentUser != null) {
            Log.d(TAG, "Creating backup from Firebase user")
            // Если пользователь авторизован - берем данные из Firebase
            OnboardingAnswers(
                userName = currentUser.userName,
                childGender = currentUser.childGender,
                isItABoy = currentUser.isItABoy,
                childName = currentUser.childName,
                childBirthDate = currentUser.childBirthDate,
                childHobby = currentUser.childHobby
            )
        } else {
            Log.d(TAG, "Creating backup from DataStore")
            // Если не авторизован - собираем из DataStore
            val answersMap = mutableMapOf<String, String>()

            // Используем маппинги
            answerMappings.forEach { mapping ->
                val value = mapping.getter() ?: mapping.defaultValue
                answersMap[mapping.answerName.toString()] = value
                Log.d(TAG, "${mapping.answerName}: $value")
            }

            // Также загружаем из существующих LiveData для надежности
            val currentLiveData = _onboardingAnswersLiveData.value
            if (currentLiveData != null && currentLiveData != OnboardingAnswers()) {
                // Если в LiveData уже есть данные, используем их
                Log.d(TAG, "Using data from LiveData")
                currentLiveData
            } else {
                Log.d(TAG, "Creating from answersMap")
                answersMap.createOnboardingAnswersFromMap()
            }
        }
    }

    private suspend fun resetAllAnswersToDefault() {
        try {
            answerMappings.forEach { mapping ->
                mapping.setter(mapping.defaultValue)
            }
        } catch (e: IOException) {
            e.printStackTrace()
            throw e
        }
    }

    private suspend fun restoreFromBackup(backup: OnboardingAnswers) {
        try {
            // Сначала сохраняем в DataStore
            onboardingUC.saveUserName(backup.userName)
            onboardingUC.saveGender(backup.childGender)
            onboardingUC.saveIsItABoy(backup.isItABoy)
            onboardingUC.saveName(backup.childName)
            onboardingUC.saveBirthDate(backup.childBirthDate)
            onboardingUC.saveHobbyAnswer(backup.childHobby)

            // Обновляем временные ответы для совместимости
            updateAnswersMapFromBackup(backup)

            // ВАЖНО: Обновляем LiveData на главном потоке
            withContext(Dispatchers.Main) {
                _onboardingAnswersLiveData.postValue(backup)
            }

            Log.d(TAG, "Data restored from backup: $backup")
        } catch (e: IOException) {
            e.printStackTrace()
            throw e
        }
    }

    private fun updateAnswersMapFromBackup(backup: OnboardingAnswers) {
        _answers.clear()
        _answers[AnswerNames.USER_NAME_ANSWER.toString()] = backup.userName
        _answers[AnswerNames.CHILD_GENDER_ANSWER.toString()] = backup.childGender
        _answers[AnswerNames.IS_IT_A_BOY_ANSWER.toString()] = backup.isItABoy
        _answers[AnswerNames.CHILD_NAME_ANSWER.toString()] = backup.childName
        _answers[AnswerNames.CHILD_BIRTH_DATE_ANSWER.toString()] = backup.childBirthDate
        _answers[AnswerNames.CHILD_HOBBY_ANSWER.toString()] = backup.childHobby
    }

    // Добавим extension функцию для конвертации Map в OnboardingAnswers
    private fun Map<String, String>.createOnboardingAnswersFromMap(): OnboardingAnswers {
        return OnboardingAnswers(
            userName = this[AnswerNames.USER_NAME_ANSWER.toString()] ?: "",
            childGender = this[AnswerNames.CHILD_GENDER_ANSWER.toString()] ?: "",
            isItABoy = this[AnswerNames.IS_IT_A_BOY_ANSWER.toString()] ?: "",
            childName = this[AnswerNames.CHILD_NAME_ANSWER.toString()] ?: "",
            childBirthDate = this[AnswerNames.CHILD_BIRTH_DATE_ANSWER.toString()] ?: "",
            childHobby = this[AnswerNames.CHILD_HOBBY_ANSWER.toString()] ?: ""
        )
    }

    // Изменяем метод загрузки данных при старте
    private fun loadOnboardingDataOnStart() {
        viewModelScope.launch(dispatcher) {
            // Загружаем данные только если пользователь не зарегистрирован
            // Это предотвращает перезапись данных из Firebase
            val currentUser = getCurrentUserUC.execute().data

            if (currentUser == null) {
                loadDataFromDatastore()
            }
            _onboardingDataLoaded.postValue(true)
        }
    }

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
            val userName = onboardingUC.getUserNameFlow().first() ?: ""
            val childGender = onboardingUC.getGenderFlow().first() ?: ""
            val isItABoy = onboardingUC.getIsItABoyFlow().first() ?: ""
            val childName = onboardingUC.getNameFlow().first() ?: ""
            val birthDate = onboardingUC.getBirthDateFlow().first() ?: ""
            val childHobby = onboardingUC.getHobbyFlow().first() ?: ""

            // Проверяем, есть ли данные для загрузки
            val hasData = userName.isNotBlank() || childName.isNotBlank() || childHobby.isNotBlank()

            if (hasData) {
                val answers = OnboardingAnswers(
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
                _answers[AnswerNames.USER_NAME_ANSWER.toString()] = userName
                _answers[AnswerNames.CHILD_GENDER_ANSWER.toString()] = childGender
                _answers[AnswerNames.IS_IT_A_BOY_ANSWER.toString()] = isItABoy
                _answers[AnswerNames.CHILD_NAME_ANSWER.toString()] = childName
                _answers[AnswerNames.CHILD_BIRTH_DATE_ANSWER.toString()] = birthDate
                _answers[AnswerNames.CHILD_HOBBY_ANSWER.toString()] = childHobby
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

    private fun updateOnboardingAnswers(update: (OnboardingAnswers) -> OnboardingAnswers) {
        val current = _onboardingAnswersLiveData.value ?: OnboardingAnswers()
        _onboardingAnswersLiveData.value = update(current)
    }
}