package com.foxynest.testappforonboarding.presentation

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
import com.foxynest.testappforonboarding.other.extensions.hasValidData
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

    // TODO проверить верность получения данных
//    private val answerMappings = listOf(
//        AnswerMapping(
//            answerName = AnswerNames.USER_NAME_ANSWER,
//            getter = { onboardingUC.getUserNameFlow().first() },
//            setter = onboardingUC::saveUserName,
//            defaultValue = ""
//        ),
//        AnswerMapping(
//            answerName = AnswerNames.CHILD_GENDER_ANSWER,
//            getter = { onboardingUC.getGenderFlow().first() },
//            setter = onboardingUC::saveGender,
//            defaultValue = ""
//        ),
//        AnswerMapping(
//            answerName = AnswerNames.IS_IT_A_BOY_ANSWER,
//            getter = { onboardingUC.getIsItABoyFlow().first() },
//            setter = onboardingUC::saveIsItABoy,
//            defaultValue = "Нет"
//        ),
//        AnswerMapping(
//            answerName = AnswerNames.CHILD_NAME_ANSWER,
//            getter = { onboardingUC.getNameFlow().first() },
//            setter = onboardingUC::saveName,
//            defaultValue = ""
//        ),
//        AnswerMapping(
//            answerName = AnswerNames.CHILD_BIRTH_DATE_ANSWER,
//            getter = { onboardingUC.getBirthDateFlow().first() },
//            setter = onboardingUC::saveBirthDate,
//            defaultValue = ""
//        ),
////        AnswerMapping(
////            answerName = AnswerNames.CHILD_HOBBY_ANSWER,
////            getter = { onboardingUC.getHobbyFlow().first() },
////            setter = onboardingUC::saveHobbyAnswer,
////            defaultValue = ""
////        )
//        AnswerMapping(
//            answerName = AnswerNames.CHILD_HOBBIES_ANSWER,
//            getter = { onboardingUC.getHobbiesFlow().first() ?: "" },
//            setter = onboardingUC::saveHobbies,
//            defaultValue = ""
//        )
//    )

    // ONBOARDING
    // В MainViewModel - ВМЕСТО 7 LiveData используем одну:
    // ЕДИНСТВЕННАЯ LiveData для всех ответов
    private val _onboardingAnswersLiveData = MutableLiveData<OnboardingAnswers>(OnboardingAnswers())
    val onboardingAnswersLiveData: LiveData<OnboardingAnswers> = _onboardingAnswersLiveData

//    // Добавим временное хранилище для резервного копирования данных (нужно при отмене онбординга)
//    private var _onboardingBackup: OnboardingAnswers? = null
//    private var _isOnboardingStarted = false
//    private var _isOnboardingCompleted = false
//    private var _isOnboardingCancelled = false

    // Бэкап для отмены онбординга
    private var onboardingBackup: OnboardingAnswers? = null
    private var isOnboardingStarted = false
    private var isOnboardingCompleted = false
    private var isOnboardingCancelled = false

//    // Для временного хранения во время заполнения опроса (нужно при возвращении назад по страницам в онбординге)
//    private val _answers = mutableMapOf<String, String>()
//    val answers: Map<String, String> get() = _answers

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
//        loadOnboardingDataOnStart()
        loadOnboardingData()
    }

    /**
     * ONBOARDING - FLAGS
     */
    fun isOnboardingStarted(): Boolean = isOnboardingStarted
    fun isOnboardingCompleted(): Boolean = isOnboardingCompleted
    fun isOnboardingCancelled(): Boolean = isOnboardingCancelled
//    fun isOnboardingStarted(): Boolean = _isOnboardingStarted
//    fun isOnboardingCompleted(): Boolean = _isOnboardingCompleted
//    fun isOnboardingCancelled(): Boolean = _isOnboardingCancelled

    /**
     * Подготовка к онбордингу - сохраняем текущие данные в бэкап
     * и сбрасываем все до значений по умолчанию
     */
    fun prepareForOnboarding() {
        viewModelScope.launch {
            Log.d(TAG, "=== PREPARE FOR ONBOARDING ===")

            isOnboardingStarted = true
            isOnboardingCompleted = false
            isOnboardingCancelled = false

            // 1. Берём ТЕКУЩИЕ данные (которые уже сохранены в DataStore)
            val currentAnswers = _onboardingAnswersLiveData.value ?: OnboardingAnswers()

            // 2. Сохраняем бэкап, если текущие данные НЕ пустые
            onboardingBackup = if (currentAnswers.hasValidData()) currentAnswers else null

            // 3. Сбрасываем ответы (сохраняем пустой объект)
            onboardingUC.saveOnboardingAnswers(OnboardingAnswers())
        }
    }
//    fun prepareForOnboarding() {
//        viewModelScope.launch(Dispatchers.IO) {
//            try {
//                Log.d(TAG, "=== PREPARE FOR ONBOARDING ===")
//
//                // Сбрасываем ВСЕ флаги перед началом нового онбординга
//                _isOnboardingStarted = true
//                _isOnboardingCompleted = false
//                _isOnboardingCancelled = false
//
////                // 1. Сохраняем текущие данные в бэкап, только если есть что сохранять
////                val currentAnswers = getCurrentAnswersForBackup()
////                if (currentAnswers != null) {
////                    _onboardingBackup = currentAnswers
////                    Log.d(TAG, "Backup created: ${_onboardingBackup != null}")
////                    Log.d(TAG, "Backup content: $_onboardingBackup")
////                } else {
////                    Log.d(TAG, "Backup is not created: ${_onboardingBackup != null}")
////                }
//
//                // 1. Сохраняем текущие данные в бэкап, только если есть что сохранять
//                val currentAnswers = getCurrentAnswersForBackup()
//                _onboardingBackup = currentAnswers   // <- всегда присваиваем, даже если null
//                if (currentAnswers != null) {
//                    Log.d(TAG, "Backup created: $_onboardingBackup")
//                } else {
//                    Log.d(TAG, "Backup is null (no data to backup)")
//                }
//
//
//
////                В fun cancelOnboarding() , если _onboardingBackup = null, то выполняется run-блок.
////                В этом блоке мы не восстанавливаем никакие данные, а просто вызываем loadDataFromDatastore().
////                Что делает loadDataFromDatastore()? Она загружает текущие данные из DataStore и обновляет _onboardingAnswersLiveData и _answers.
////                То есть она отображает актуальное состояние, которое уже есть в DataStore. После отмены онбординга это именно то, что нам нужно:
////                ничего не менять, просто показать текущие данные.
//
//
//
//                // 2. Сбрасываем все ответы на значения по умолчанию
//                resetAllAnswersToDefault()
//
//                // 3. Очищаем временные ответы в ViewModel
//                _answers.clear()
//
//                // 4. Обновляем LiveData с пустыми значениями
//                val emptyAnswers = OnboardingAnswers()
//                withContext(Dispatchers.Main) {
//                    _onboardingAnswersLiveData.postValue(emptyAnswers)
//                }
//
//                Log.d(TAG, "Onboarding prepared: data backed up and reset")
//
//            } catch (e: Exception) {
//                Log.e(TAG, "Error preparing for onboarding", e)
//                _errorMessageLiveData.postValue(
//                    MyEvent(
//                        Resource.error(
//                            "Ошибка при подготовке опроса",
//                            ErrorType.UNEXPECTED_ERROR
//                        )
//                    )
//                )
//            }
//        }
//    }

    /**
     * Отмена онбординга - восстанавливаем данные из бэкапа
     */
    fun cancelOnboarding() {
        viewModelScope.launch {
            Log.d(TAG, "=== CANCEL ONBOARDING ===")

            isOnboardingCancelled = true
            isOnboardingCompleted = false
            isOnboardingStarted = false

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
        }
    }
//    fun cancelOnboarding() {
//        viewModelScope.launch(Dispatchers.IO) {
//            try {
//                Log.d(TAG, "=== CANCEL ONBOARDING ===")
//                Log.d(TAG, "Backup is null: ${_onboardingBackup == null}")
//
//                _isOnboardingCancelled = true
//                _isOnboardingCompleted = false
//                _isOnboardingStarted = false  // Сбрасываем флаг запуска!
//
//                _onboardingBackup?.let { backup ->
//                    Log.d(TAG, "Restoring from backup: $backup")
//                    // Восстанавливаем данные из бэкапа только если они есть
//                    restoreFromBackup(backup)
////                    _onboardingBackup = null // Очищаем бэкап - на всякий случай не буду очищать
//                    Log.d(TAG, "Onboarding cancelled: data restored from backup")
//                } ?: run {
//                    Log.d(TAG, "Backup is null or empty - nothing to restore")
//                    // Если бэкапа нет, НЕ очищаем данные!
//                    // Просто оставляем как есть
//
//                    // Но нам нужно обновить LiveData из текущих данных в DataStore
//                    loadDataFromDatastore()
//
////                    val emptyAnswers = OnboardingAnswers()
////                    withContext(Dispatchers.Main) {
////                        _onboardingAnswersLiveData.postValue(emptyAnswers)
////                    }
////                    Log.d(TAG, "Onboarding cancelled: data reset to defaults (no backup)")
//                }
//
//            } catch (e: Exception) {
//                Log.e(TAG, "Error cancelling onboarding", e)
//                _errorMessageLiveData.postValue(
//                    MyEvent(
//                        Resource.error(
//                            "Ошибка при отмене опроса",
//                            ErrorType.UNEXPECTED_ERROR
//                        )
//                    )
//                )
//            }
//        }
//    }

    /**
     * Завершение онбординга - очищаем бэкап
     */
    fun completeOnboarding() {
        isOnboardingCompleted = true
        isOnboardingStarted = false
        isOnboardingCancelled = false
        onboardingBackup = null
        Log.d(TAG, "Onboarding completed: backup cleared")
    }
//    fun completeOnboarding() {
//        _isOnboardingCompleted = true
//        _isOnboardingStarted = false  // Сбрасываем флаг запуска
//        _isOnboardingCancelled = false
//        _onboardingBackup = null
//        Log.d(TAG, "Onboarding completed: backup cleared")
//    }

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
                Log.e(TAG, "Error clearing user data", e)
            }
        }
    }


//    fun clearUserDataOnLogout() {
//        viewModelScope.launch(Dispatchers.IO) {
//            try {
//                // 1. Очищаем статус онбординга при ВЫХОДЕ ИЗ АККАУНТА
////                val recourseResult = onboardingUC.saveOnboardingState(false)
////                if (recourseResult.status == Status.SUCCESS) {
//                onboardingUC.saveOnboardingState(false)
//
//                // 2. Очищаем LiveData - обновляем LiveData на главном потоке
//                val emptyAnswers = OnboardingAnswers()
//                withContext(Dispatchers.Main) {
//                    _onboardingAnswersLiveData.postValue(emptyAnswers)
//                }
//
//                // 3. Очищаем временные ответы
//                _answers.clear()
//
//                // 4. Очищаем DataStore через UC
//                onboardingUC.clearAllOnboardingData()
//
//                // 5. Очищаем состояние пользователя
//                _checkedUserInfo.value = Resource.init(null)
////                }
//            } catch (e: Exception) {
//                Log.e(TAG, "Error clearing user data on logout", e)
//                _errorMessageLiveData.postValue(
//                    MyEvent(
//                        Resource.error(
//                            "Ошибка при очистке данных",
//                            ErrorType.FAILED_TO_CLEAR_DATA
//                        )
//                    )
//                )
//            }
//        }
//    }

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
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save onboarding answers", e)
            _errorMessageLiveData.postValue(
                MyEvent(
                    Resource.error(
                        "Не удалось сохранить данные",
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

    fun updateIsItABoy(answer: String) {
        updateOnboardingAnswers { it.copy(isItABoy = answer) }
    }

    fun updateChildName(name: String) {
        updateOnboardingAnswers { it.copy(childName = name) }
    }

    fun updateBirthDate(birthDate: String) {
        updateOnboardingAnswers { it.copy(childBirthDate = birthDate) }
    }

    fun updateChildHobbies(hobbies: String) {
        updateOnboardingAnswers { it.copy(childHobbies = hobbies) }
    }

//    fun saveUserNameAnswer(userNameAnswer: String) {
//        saveOperation(
//            userNameAnswer,
//            onboardingUC::saveUserName,
//            AnswerNames.USER_NAME_ANSWER,
//            ErrorType.FAILED_TO_SAVE_USER_NAME_LOCAL
//        )
//        updateOnboardingAnswers { it.copy(userName = userNameAnswer.trim()) }
//    }
//
//    fun saveGenderAnswer(genderAnswer: String) {
//        saveOperation(
//            genderAnswer,
//            onboardingUC::saveGender,
//            AnswerNames.CHILD_GENDER_ANSWER,
//            ErrorType.FAILED_TO_SAVE_GENDER_LOCAL
//        )
//        updateOnboardingAnswers { it.copy(childGender = genderAnswer.trim()) }
//    }
//
//    fun saveChildNameAnswer(nameAnswer: String) {
//        saveOperation(
//            nameAnswer,
//            onboardingUC::saveName,
//            AnswerNames.CHILD_NAME_ANSWER,
//            ErrorType.FAILED_TO_SAVE_CHILD_NAME_LOCAL
//        )
//        updateOnboardingAnswers { it.copy(childName = nameAnswer.trim()) }
//    }
//
//    fun saveIsItABoyAnswer(answer: String) {
//        saveOperation(
//            answer,
//            onboardingUC::saveIsItABoy,
//            AnswerNames.IS_IT_A_BOY_ANSWER,
//            ErrorType.FAILED_TO_SAVE_IS_IT_A_BOY_LOCAL
//        )
//        updateOnboardingAnswers { it.copy(isItABoy = answer.trim()) }
//    }
//
//    fun saveBirthDateAnswer(birthDate: String) {
//        saveOperation(
//            birthDate,
//            onboardingUC::saveBirthDate,
//            AnswerNames.CHILD_BIRTH_DATE_ANSWER,
//            ErrorType.FAILED_TO_SAVE_BIRTH_DATE_LOCAL
//        )
//        updateOnboardingAnswers { it.copy(childBirthDate = birthDate.trim()) }
//    }
//
////    fun saveHobbyAnswer(hobby: String) {
////        saveOperation(
////            hobby,
////            onboardingUC::saveHobbyAnswer,
////            AnswerNames.CHILD_HOBBY_ANSWER,
////            ErrorType.FAILED_TO_SAVE_HOBBY_LOCAL
////        )
////        updateOnboardingAnswers { it.copy(childHobby = hobby.trim()) }
////    }
//
//    fun saveHobbiesAnswer(hobbies: String) {
//        saveOperation(
//            hobbies,
//            onboardingUC::saveHobbies,
//            AnswerNames.CHILD_HOBBIES_ANSWER,
//            ErrorType.FAILED_TO_SAVE_HOBBIES_LOCAL
//        )
//        updateOnboardingAnswers { it.copy(childHobbies = hobbies) }
//    }

    fun retryDataSave() {
        viewModelScope.launch(Dispatchers.IO) {
            _retrySaveState.postValue(Resource.loading(null))

            // Получаем текущие данные из ViewModel и пытаемся сохранить снова
            val currentUser = getCurrentUserUC.execute().data
            if (currentUser != null) {
                // Повторно собираем данные для сохранения
//                val updatedUser = answers.createUpdatedUserFromAnswersInViewModel(currentUser)

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

//    private suspend fun resetAllAnswersToDefault() {
//        try {
//            answerMappings.forEach { mapping ->
//                mapping.setter(mapping.defaultValue)
//            }
//        } catch (e: IOException) {
//            e.printStackTrace()
//            throw e
//        }
//    }

//    private suspend fun restoreFromBackup(backup: OnboardingAnswers) {
//        try {
//            // Восстанавливаем только если есть что восстанавливать
//            if (backup.hasValidData()) {
//                Log.d(TAG, "Restoring valid data from backup")
//
//                // Сохраняем в DataStore только непустые значения
//                if (backup.userName.isNotBlank()) {
//                    onboardingUC.saveUserName(backup.userName)
//                }
//                if (backup.childGender.isNotBlank()) {
//                    onboardingUC.saveGender(backup.childGender)
//                }
//                if (backup.isItABoy.isNotBlank() && backup.isItABoy != "Нет") {
//                    onboardingUC.saveIsItABoy(backup.isItABoy)
//                }
//                if (backup.childName.isNotBlank()) {
//                    onboardingUC.saveName(backup.childName)
//                }
//                if (backup.childBirthDate.isNotBlank()) {
//                    onboardingUC.saveBirthDate(backup.childBirthDate)
//                }
//                if (backup.childHobbies.isNotBlank()) {
//                    onboardingUC.saveHobbies(backup.childHobbies)
//                }
//
//                // Обновляем временные ответы для совместимости
//                updateAnswersMapFromBackup(backup)
//
//                // Обновляем LiveData на главном потоке
//                withContext(Dispatchers.Main) {
//                    _onboardingAnswersLiveData.postValue(backup)
//                }
//
//                Log.d(TAG, "Data restored from backup: $backup")
//            } else {
//                Log.d(TAG, "Backup has no valid data, skipping restore")
//                // Загружаем текущие данные из DataStore
//                loadDataFromDatastore()
//            }
//        } catch (e: IOException) {
//            e.printStackTrace()
//            throw e
//        }
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
    private fun loadOnboardingData() {
        viewModelScope.launch {
            onboardingUC.getOnboardingAnswersFlow().collect { answers ->
                _onboardingAnswersLiveData.postValue(answers)
                Log.d(TAG, "Loaded onboarding answers: $answers")
            }
        }
    }

//    private fun saveOperation(
//        answerToSave: String,
//        saveToDataStoreAction: suspend (String) -> Unit,
//        answerKey: AnswerNames,
//        errorType: ErrorType
//    ) {
//        viewModelScope.launch(dispatcher) {
//            val trimmedAnswer = answerToSave.trim()
//
//            // НЕ сохраняем пустые строки!
//            if (trimmedAnswer.isEmpty()) {
//                Log.d(TAG, "Empty answer for ${answerKey.name} - skipping save")
//                return@launch
//            }
//
//            try {
//                saveToDataStoreAction(trimmedAnswer)
//                saveAnswerToMainViewModel(answerKey.toString(), trimmedAnswer)
//                Log.d(TAG, "Saved ${answerKey.name}: $trimmedAnswer")
//            } catch (e: IOException) {
//                Log.e(TAG, "Failed to save ${answerKey.name}", e)
//                _errorMessageLiveData.postValue(
//                    MyEvent(
//                        Resource.error(
//                            "Не удалось сохранить некоторые данные на вашем телефоне",
//                            errorType
//                        )
//                    )
//                )
//            } catch (e: Exception) {
//                Log.e(TAG, "Unexpected error saving ${answerKey.name}", e)
//                _errorMessageLiveData.postValue(
//                    MyEvent(
//                        Resource.error(
//                            "Произошла непредвиденная ошибка",
//                            ErrorType.UNEXPECTED_ERROR
//                        )
//                    )
//                )
//            }
//        }
//    }

//    private fun saveAnswerToMainViewModel(questionName: String, answer: String) {
//        _answers[questionName] = answer
//        Log.d(
//            TAG,
//            "Проверка: Размер ${_answers.size}, Пол: ${_answers[AnswerNames.CHILD_GENDER_ANSWER.toString()]}"
//        )
//    }

//    private suspend fun loadDataFromDatastore() {
//        try {
//            val userName = onboardingUC.getUserNameFlow().first() ?: ""
//            val childGender = onboardingUC.getGenderFlow().first() ?: ""
//            val isItABoy = onboardingUC.getIsItABoyFlow().first() ?: ""
//            val childName = onboardingUC.getNameFlow().first() ?: ""
//            val birthDate = onboardingUC.getBirthDateFlow().first() ?: ""
//            val childHobbies = onboardingUC.getHobbiesFlow().first() ?: ""
//
//             // МИГРАЦИЯ!!!! Если hobbies пусты, пробуем прочитать старое поле hobby
//                    if (childHobbies.isBlank()) {
//                        val oldHobby = onboardingUC.getHobbyFlow().first() ?: ""
//                        if (oldHobby.isNotBlank()) {
//                            childHobbies = oldHobby
//                            // Опционально: сохраняем в новый ключ
//                            onboardingUC.saveHobbies(oldHobby)
//                        }
//                    }
//
//
//            // Проверяем, есть ли данные для загрузки
//            val hasData = userName.isNotBlank() || childName.isNotBlank() || childHobbies.isNotBlank()
//
//            if (hasData) {
//                val answers = OnboardingAnswers(
//                    userName = userName,
//                    childGender = childGender,
//                    isItABoy = isItABoy,
//                    childName = childName,
//                    childBirthDate = birthDate,
//                    childHobbies = childHobbies
//                )
//
//                // Обновляем LiveData на главном потоке
//                withContext(Dispatchers.Main) {
//                    _onboardingAnswersLiveData.postValue(answers)
//                }
//
//                // Также обновляем временные ответы для совместимости
//                _answers.clear()
//                _answers[AnswerNames.USER_NAME_ANSWER.toString()] = userName
//                _answers[AnswerNames.CHILD_GENDER_ANSWER.toString()] = childGender
//                _answers[AnswerNames.IS_IT_A_BOY_ANSWER.toString()] = isItABoy
//                _answers[AnswerNames.CHILD_NAME_ANSWER.toString()] = childName
//                _answers[AnswerNames.CHILD_BIRTH_DATE_ANSWER.toString()] = birthDate
//                _answers[AnswerNames.CHILD_HOBBIES_ANSWER.toString()] = childHobbies
//            }
//
//        } catch (e: Exception) {
//            Log.e(TAG, "Error loading data from DataStore", e)
//            _errorMessageLiveData.postValue(
//                MyEvent(
//                    Resource.error(
//                        CommonConstants.FAILED_TO_LOAD_LOCAL,
//                        ErrorType.FAILED_TO_LOAD_LOCAL_DATA
//                    )
//                )
//            )
//        }
//    }

//    private fun updateOnboardingAnswers(update: (OnboardingAnswers) -> OnboardingAnswers) {
//        val current = _onboardingAnswersLiveData.value ?: OnboardingAnswers()
//        _onboardingAnswersLiveData.value = update(current)
//    }
}