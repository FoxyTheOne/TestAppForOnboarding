package com.foxynest.testappforonboarding.presentation

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.foxynest.testappforonboarding.di.DispatcherIO
import com.foxynest.testappforonboarding.domain.entities.ErrorType
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
import com.foxynest.testappforonboarding.other.extensions.hasValidData
import com.foxynest.testappforonboarding.other.extensions.isUserDataValid
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.IOException
import javax.inject.Inject

/**
 * При изменениях в Data Store (добавление, изменение полей) нужно делать миграцию здесь (loadDataFromDatastore)! Убедиться, что данные пользователей не пропадут!!! (как зарегистрированных, так и нет)
 */
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

    // ONBOARDING
    // ЕДИНСТВЕННАЯ LiveData для всех ответов
    private val _onboardingAnswersLiveData = MutableLiveData<OnboardingAnswers>(OnboardingAnswers())
    val onboardingAnswersLiveData: LiveData<OnboardingAnswers> = _onboardingAnswersLiveData

    // Бэкап для отмены онбординга
    private var onboardingBackup: OnboardingAnswers? = null
    private var isOnboardingStarted = false
    private var isOnboardingCompleted = false
    private var isOnboardingCancelled = false

    // PROFILE
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

    // ERROR
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
     * ONBOARDING - FLAGS
     */
    fun isOnboardingStarted(): Boolean = isOnboardingStarted
    fun isOnboardingCompleted(): Boolean = isOnboardingCompleted
    fun isOnboardingCancelled(): Boolean = isOnboardingCancelled

    /**
     * Подготовка к онбордингу - сохраняем текущие данные в бэкап
     * и сбрасываем все до значений по умолчанию
     */
    fun prepareForOnboarding() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                Log.d(TAG, "=== PREPARE FOR ONBOARDING ===")

                // Сбрасываем ВСЕ флаги перед началом нового онбординга
                isOnboardingStarted = true
                isOnboardingCompleted = false
                isOnboardingCancelled = false

                // 1. Берём ТЕКУЩИЕ данные (которые уже сохранены в DataStore)
                val currentAnswers = _onboardingAnswersLiveData.value ?: OnboardingAnswers()

                // 2. Сохраняем бэкап, если текущие данные НЕ пустые
                onboardingBackup = if (currentAnswers.hasValidData()) currentAnswers else null

                // 3. Сбрасываем ответы (сохраняем пустой объект)
                onboardingUC.saveOnboardingAnswers(OnboardingAnswers())
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
                Log.d(TAG, "=== CANCEL ONBOARDING ===")

                isOnboardingCancelled = true
                isOnboardingCompleted = false
                isOnboardingStarted = false // Сбрасываем флаг запуска!

                // Восстанавливаем данные из бэкапа
                onboardingBackup?.let { backup ->
                    Log.d(TAG, "Restoring from backup: $backup")
                    onboardingUC.saveOnboardingAnswers(backup)
                    onboardingBackup = null
                }
                // Если бэкапа нет – ничего не восстанавливаем, значит данных и не было
//                ?: run {
//                Log.d(TAG, "No backup to restore")
//                // Если бэкапа нет, перезагружаем из DataStore
//                loadOnboardingData()
//            }
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
        isOnboardingCompleted = true
        isOnboardingStarted = false // Сбрасываем флаг запуска
        isOnboardingCancelled = false
        onboardingBackup = null
        Log.d(TAG, "Onboarding completed: backup cleared")
    }

    /**
     * ONBOARDING - CLEAN
     */
    fun clearUserDataOnLogout() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                onboardingUC.saveOnboardingState(false)
//                onboardingUC.saveOnboardingAnswers(OnboardingAnswers())  // очищаем
                onboardingUC.clearAllOnboardingData()
                _checkedUserInfo.value = Resource.init(null)
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
     * ONBOARDING - SAVE
     */
    // === ЕДИНСТВЕННЫЙ МЕТОД СОХРАНЕНИЯ ===
    fun updateOnboardingAnswers(update: (OnboardingAnswers) -> OnboardingAnswers) {
        val current = _onboardingAnswersLiveData.value ?: OnboardingAnswers()
        val updated = update(current)

        viewModelScope.launch {
            try {
                onboardingUC.saveOnboardingAnswers(updated)
                Log.d(TAG, "Saved onboarding answers: $updated")
            } catch (e: IOException) {
                Log.e(TAG, "Failed to save onboarding answers", e)
                _errorMessageLiveData.postValue(
                    MyEvent(
                        Resource.error(
                            "Не удалось сохранить данные",
                            ErrorType.DATA_NOT_SAVED_ERROR
                        )
                    )
                )
            } catch (e: Exception) {
                Log.e(TAG, "Unexpected error saving onboarding answers", e)
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

    // === УДОБНЫЕ МЕТОДЫ-ОБЁРТКИ (опционально) ===
    // Их можно вызывать из фрагментов для красоты
    fun updateUserName(userName: String) {
        updateOnboardingAnswers { it.copy(userName = userName) }
    }

    fun updateChildGender(gender: String) {
        updateOnboardingAnswers { it.copy(childGender = gender) }
    }

    fun updateChildName(name: String) {
        updateOnboardingAnswers { it.copy(childName = name) }
    }

    fun updateIsItABoy(answer: String) {
        updateOnboardingAnswers { it.copy(isItABoy = answer) }
    }

    fun updateBirthDate(birthDate: String) {
        updateOnboardingAnswers { it.copy(childBirthDate = birthDate) }
    }

    fun updateChildHobbies(hobbies: String) {
        updateOnboardingAnswers { it.copy(childHobbies = hobbies) }
    }

    fun retryDataSave() {
        viewModelScope.launch(Dispatchers.IO) {
            _retrySaveState.postValue(Resource.loading(null))

            // Получаем текущие данные из ViewModel и пытаемся сохранить снова
            val currentUser = getCurrentUserUC.execute().data
            if (currentUser != null) {
                // Повторно собираем данные для сохранения
                val currentAnswers = _onboardingAnswersLiveData.value ?: OnboardingAnswers()
                val updatedUser = currentUser.copy(
                    userName = currentAnswers.userName,
                    childGender = currentAnswers.childGender,
                    isItABoy = currentAnswers.isItABoy,
                    childName = currentAnswers.childName,
                    childBirthDate = currentAnswers.childBirthDate,
                    childHobbies = currentAnswers.childHobbies
                )

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
    fun getAnswersFromOnboardingSaveToFirebaseAndUse() {
        viewModelScope.launch(Dispatchers.IO) {
            Log.d(TAG, "=== SAVE TO FIREBASE ===")

            val currentUser = getCurrentUserUC.execute().data
            val currentAnswers = _onboardingAnswersLiveData.value ?: OnboardingAnswers()

            if (currentUser != null) {
                val updatedUser = currentUser.copy(
                    userName = currentAnswers.userName,
                    childGender = currentAnswers.childGender,
                    isItABoy = currentAnswers.isItABoy,
                    childName = currentAnswers.childName,
                    childBirthDate = currentAnswers.childBirthDate,
                    childHobbies = currentAnswers.childHobbies
                )

                if (updatedUser.isUserDataValid()) {
                    Log.d(TAG, "Saving valid user data to Firebase: $updatedUser")
                    _profileUpdatedLiveData.postValue(updateProfileUC.execute(updatedUser))
                } else {
                    Log.w(TAG, "No valid data to save to Firebase")
                    _errorMessageLiveData.postValue(
                        MyEvent(
                            Resource.error(
                                "Не все обязательные поля заполнены",
                                ErrorType.FIELD_EMPTY_ERROR
                            )
                        )
                    )
                }
            } else {
                Log.d(TAG, "User not logged in - skipping Firebase save")
            }
        }
    }

    // Запустить метод для получения этих данных можно, когда открывается PAYWALL FRAGMENT - в этот момент опрос уже точно пройден
//    fun getAnswersFromOnboardingSaveToFirebaseAndUse() {
//        viewModelScope.launch(Dispatchers.IO) {
//            Log.d(TAG, "=== SAVE TO FIREBASE ===")
//
//            // Проверим, осуществлён ли вход
//            val currentUser = getCurrentUserUC.execute().data
//
////            // ЗАГРУЖАЕМ ДАННЫЕ ИЗ DATASTORE ВСЕ СРАЗУ (синхронно) и кладём их в LiveData
////            loadDataFromDatastore()
//
//            // Сохранение в firebase только если осуществлён ли вход, чтобы обновить поля
//            if (currentUser != null) {
//                // Создаём UserPresentation на основе пройденного опроса
//                val updatedUser = answers.createUpdatedUserFromAnswersInViewModel(currentUser)
//
//                // Проверяем, что данные не пустые перед сохранением в Firebase
//                val hasDataToSave = updatedUser.isUserDataValid()
//
//                if (hasDataToSave) {
//                    // Если всё хорошо - сохранение данных в firestore и в _profileUpdatedLiveData
//                    Log.d(TAG, "Saving valid user data to Firebase: $updatedUser")
//                    _profileUpdatedLiveData.postValue(updateProfileUC.execute(updatedUser))
//                } else {
//                    Log.w(
//                        TAG,
//                        "No valid data to save to Firebase - skipping update. User: $updatedUser"
//                    )
//                    // Можно показать уведомление пользователю
//                    _errorMessageLiveData.postValue(
//                        MyEvent(
//                            Resource.error(
//                                "Не все обязательные поля заполнены",
//                                ErrorType.FIELD_EMPTY_ERROR
//                            )
//                        )
//                    )
//                }
//            } else {
//                Log.d(TAG, "User not logged in - skipping Firebase save")
//            }
//        }
//    }

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
//    private suspend fun getCurrentAnswersForBackup(): OnboardingAnswers? {
//        Log.d(TAG, "getCurrentAnswersForBackup called")
//
//        // TODO проверить логику
//        // Проверяем, авторизован ли пользователь
//        val currentUser = getCurrentUserUC.execute().data
//        Log.d(TAG, "Current user is null: ${currentUser == null}")
//
//        // Сохранять только не пустые данные
//        val backupAnswers = if (currentUser != null) {
//            // Если пользователь авторизован - берем данные из Firebase
//            // Проверяем, есть ли реальные данные
//            if (currentUser.isUserDataValid()) {
//                Log.d(TAG, "Creating backup from Firebase user")
//                OnboardingAnswers(
//                    userName = currentUser.userName,
//                    childGender = currentUser.childGender,
//                    isItABoy = currentUser.isItABoy,
//                    childName = currentUser.childName,
//                    childBirthDate = currentUser.childBirthDate,
//                    childHobbies = currentUser.childHobbies
//                )
//            } else {
//                Log.d(TAG, "Firebase user has no valid data, returning null")
//                null
//            }
//        } else {
//            // Если не авторизован - собираем из DataStore
//            Log.d(TAG, "Creating backup from DataStore")
//
//            // Собираем только непустые данные
//            val answersMap = mutableMapOf<String, String>()
//            var hasValidData = false
//
//            // Используем маппинги
//            answerMappings.forEach { mapping ->
//                val value = mapping.getter() ?: mapping.defaultValue
//                // Сохраняем только если значение непустое и не равно defaultValue для строковых полей
//                val shouldSave = when {
//                    // для IS_IT_A_BOY_ANSWER
//                    mapping.answerName == AnswerNames.IS_IT_A_BOY_ANSWER ->
//                        value != "Нет" && value.isNotBlank()
//                    // Для остальных ответов
//                    else -> value.isNotBlank()
//                }
//
//                // shouldSave = true значит значения не дефолтные
//                if (shouldSave) {
//                    answersMap[mapping.answerName.toString()] = value
//                    hasValidData = true
//                    Log.d(TAG, "${mapping.answerName}: $value - WILL SAVE")
//                } else {
//                    Log.d(TAG, "${mapping.answerName}: $value - SKIP")
//                }
//            }
//
//            if (hasValidData) {
//                Log.d(TAG, "Creating backup from answersMap")
//                answersMap.createOnboardingAnswersFromMap()
//            } else {
//                Log.d(TAG, "No valid data in DataStore for backup")
//                null
//            }
//
////            answerMappings.forEach { mapping ->
////                val value = mapping.getter() ?: mapping.defaultValue
////                answersMap[mapping.answerName.toString()] = value
////                Log.d(TAG, "${mapping.answerName}: $value")
////            }
////
////            // Также загружаем из существующих LiveData для надежности
////            val currentLiveData = _onboardingAnswersLiveData.value
////            if (currentLiveData != null && currentLiveData != OnboardingAnswers()) {
////                // Если в LiveData уже есть данные, используем их
////                Log.d(TAG, "Using data from LiveData")
////                currentLiveData
////            } else {
////                Log.d(TAG, "Creating from answersMap")
////                answersMap.createOnboardingAnswersFromMap()
////            }
//        }
//
//        Log.d(TAG, "Backup created: ${backupAnswers != null}")
//        return backupAnswers
//    }

    /**
     * Сюда добавляем новые ответы
     */
//    private fun updateAnswersMapFromBackup(backup: OnboardingAnswers) {
//        _answers.clear()
//        _answers[AnswerNames.USER_NAME_ANSWER.toString()] = backup.userName
//        _answers[AnswerNames.CHILD_GENDER_ANSWER.toString()] = backup.childGender
//        _answers[AnswerNames.IS_IT_A_BOY_ANSWER.toString()] = backup.isItABoy
//        _answers[AnswerNames.CHILD_NAME_ANSWER.toString()] = backup.childName
//        _answers[AnswerNames.CHILD_BIRTH_DATE_ANSWER.toString()] = backup.childBirthDate
//        _answers[AnswerNames.CHILD_HOBBIES_ANSWER.toString()] = backup.childHobbies
//    }

//    // Добавим extension функцию для конвертации Map в OnboardingAnswers
//    private fun Map<String, String>.createOnboardingAnswersFromMap(): OnboardingAnswers {
//        return OnboardingAnswers(
//            userName = this[AnswerNames.USER_NAME_ANSWER.toString()] ?: "",
//            childGender = this[AnswerNames.CHILD_GENDER_ANSWER.toString()] ?: "",
//            isItABoy = this[AnswerNames.IS_IT_A_BOY_ANSWER.toString()] ?: "",
//            childName = this[AnswerNames.CHILD_NAME_ANSWER.toString()] ?: "",
//            childBirthDate = this[AnswerNames.CHILD_BIRTH_DATE_ANSWER.toString()] ?: "",
//            childHobbies = this[AnswerNames.CHILD_HOBBIES_ANSWER.toString()] ?: ""
//        )
//    }

    // Изменяем метод загрузки данных при старте
//    private fun loadOnboardingDataOnStart() {
//        viewModelScope.launch(dispatcher) {
//            // Загружаем данные только если пользователь не зарегистрирован
//            // Это предотвращает перезапись данных из Firebase
//            val currentUser = getCurrentUserUC.execute().data
//
//            if (currentUser == null) {
//                loadDataFromDatastore()
//            }
//            _onboardingDataLoaded.postValue(true)
//        }
//    }
    // Загрузка данных из DataStore при старте
    private fun loadOnboardingDataOnStart() {
        viewModelScope.launch {
            onboardingUC.getOnboardingAnswersFlow().collect { answers ->
                _onboardingAnswersLiveData.postValue(answers)
                Log.d(TAG, "Loaded onboarding answers: $answers")
            }
        }
    }
}