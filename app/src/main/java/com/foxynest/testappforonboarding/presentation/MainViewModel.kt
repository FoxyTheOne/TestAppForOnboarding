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
import com.foxynest.testappforonboarding.domain.use_cases.authUseCases.login.IGetCurrentFirebaseUserUC
import com.foxynest.testappforonboarding.domain.use_cases.authUseCases.login.ILoginValidateEmailUC
import com.foxynest.testappforonboarding.domain.use_cases.notification.IScheduleBirthdayNotificationUC
import com.foxynest.testappforonboarding.domain.use_cases.onboardingUseCases.IOnboardingUC
import com.foxynest.testappforonboarding.domain.use_cases.updatingDataUseCases.IUpdateEmailUC
import com.foxynest.testappforonboarding.domain.use_cases.updatingDataUseCases.IUpdateFirebaseProfileUC
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
    private val updateFirebaseProfileUC: IUpdateFirebaseProfileUC,
    private val getCurrentFirebaseUserUC: IGetCurrentFirebaseUserUC,
    private val getAuthStateFlowUC: IGetAuthStateFlowUC,
    private val loginValidateEmailUC: ILoginValidateEmailUC,
    private val updateEmailUC: IUpdateEmailUC,
    private val scheduleBirthdayNotificationUC: IScheduleBirthdayNotificationUC,
    @DispatcherIO private val dispatcher: CoroutineDispatcher = Dispatchers.IO // TODO расставить dispatcher или удалить тесты
) : ViewModel() {
    companion object {
        private const val TAG = "MainViewModel"
    }

    // ONBOARDING
    // ЕДИНАЯ LiveData для всех ответов
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
        refreshBirthdayNotification()
    }

    /**
     * ONBOARDING - CLEAN
     */
    fun clearUserDataOnLogout() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                onboardingUC.saveOnboardingState(false)
                onboardingUC.clearAllOnboardingData()
                _checkedUserInfo.value = Resource.init(null)
                // Clear notification
                refreshBirthdayNotificationWithParameters("", "")
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
                // Немедленная проверка. Чтобы не было конфликтов или повотрных уведомлений, делаем эту проверку один раз, после прохождения онбординга
                // TODO Если сделаем возможность изменять отдельные поля без онбординга, то там нужно дуюлировать эту немедленную проверку (например поля, от которых ничего не зависит - имя, дата рождения)
                scheduleBirthdayNotificationUC.checkAndNotifyIfBirthdayToday(updated.childName, updated.childBirthDate)
                refreshBirthdayNotificationWithParameters(updated.childName, updated.childBirthDate)
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
            val currentFirebaseUser = getCurrentFirebaseUserUC.execute().data
            if (currentFirebaseUser != null) {
                // Повторно собираем данные для сохранения
                val currentAnswers = _onboardingAnswersLiveData.value ?: OnboardingAnswers()
                val updatedUser = currentFirebaseUser.copy(
                    userName = currentAnswers.userName,
                    childGender = currentAnswers.childGender,
                    isItABoy = currentAnswers.isItABoy,
                    childName = currentAnswers.childName,
                    childBirthDate = currentAnswers.childBirthDate,
                    childHobbies = currentAnswers.childHobbies
                )

                val result = updateFirebaseProfileUC.execute(updatedUser)

                // Отправляем результат в отдельный LiveData для повторных попыток
                _retrySaveState.postValue(result)

                // Также обновляем основной LiveData для согласованности
                if (result.status == Status.SUCCESS) {
                    _profileUpdatedLiveData.postValue(result)
                    refreshBirthdayNotificationWithParameters(updatedUser.childName, updatedUser.childBirthDate)
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

            val currentFirebaseUser = getCurrentFirebaseUserUC.execute().data
            val currentAnswers = _onboardingAnswersLiveData.value ?: OnboardingAnswers()

            if (currentFirebaseUser != null) {
                val updatedUser = currentFirebaseUser.copy(
                    userName = currentAnswers.userName,
                    childGender = currentAnswers.childGender,
                    isItABoy = currentAnswers.isItABoy,
                    childName = currentAnswers.childName,
                    childBirthDate = currentAnswers.childBirthDate,
                    childHobbies = currentAnswers.childHobbies
                )

                if (updatedUser.isUserDataValid()) {
                    Log.d(TAG, "Saving valid user data to Firebase: $updatedUser")
                    _profileUpdatedLiveData.postValue(updateFirebaseProfileUC.execute(updatedUser))
                    refreshBirthdayNotificationWithParameters(updatedUser.childName, updatedUser.childBirthDate)
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

    /**
     * OTHER
     */
    fun checkLoggedInStateGetUser() {
        viewModelScope.launch {
            _checkedUserInfo.value = getCurrentFirebaseUserUC.execute()
        }
    }

    // Вызываем после сохранения онбординга
    // (в методах updateOnboardingAnswers, completeOnboarding, getAnswersFromOnboardingSaveToFirebaseAndUse)
    fun refreshBirthdayNotification() {
        viewModelScope.launch(Dispatchers.IO) {
            scheduleBirthdayNotificationUC.scheduleWithoutParameters()
        }
    }

    fun refreshBirthdayNotificationWithParameters(childName: String, childBirthDate: String) {
        viewModelScope.launch(Dispatchers.IO) {
            scheduleBirthdayNotificationUC.scheduleWithParameters(childName, childBirthDate)
        }
    }

    /**
     * PRIVATE METHODS
     */
    // Загрузка данных из DataStore при старте
    private fun loadOnboardingDataOnStart() {
        viewModelScope.launch {
            onboardingUC.getOnboardingAnswersFlow().collect { answers ->
                _onboardingAnswersLiveData.postValue(answers)
                Log.d(TAG, "Loaded onboarding answers: $answers")
                // После загрузки планируем уведомление (если данные валидны)
                refreshBirthdayNotification()
            }
        }
    }
}