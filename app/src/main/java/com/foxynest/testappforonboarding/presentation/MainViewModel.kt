package com.foxynest.testappforonboarding.presentation

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.foxynest.testappforonboarding.domain.entities.ErrorType
import com.foxynest.testappforonboarding.domain.entities.onboarding.OnboardingAnswers
import com.foxynest.testappforonboarding.domain.entities.onboarding.isValidData
import com.foxynest.testappforonboarding.domain.entities.user.UserPresentation
import com.foxynest.testappforonboarding.domain.entities.user.isValidData
import com.foxynest.testappforonboarding.domain.use_cases.authUseCases.IGetCurrentUserPresentationUC
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
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.IOException
import javax.inject.Inject

/**
 * При изменениях в Data Store (добавление, изменение полей) нужна миграция (MigrateUserDataRepository)!
 * Убедиться, что данные пользователей не пропадут (как зарегистрированных, так и нет).
 */
@HiltViewModel
class MainViewModel @Inject constructor(
    private val onboardingUC: IOnboardingUC,
    private val updateFirebaseProfileUC: IUpdateFirebaseProfileUC,
    private val getCurrentFirebaseUserUC: IGetCurrentFirebaseUserUC,
    private val getCurrentUserPresentationUC: IGetCurrentUserPresentationUC,
    getAuthStateFlowUC: IGetAuthStateFlowUC,
    private val loginValidateEmailUC: ILoginValidateEmailUC,
    private val updateEmailUC: IUpdateEmailUC,
    private val scheduleBirthdayNotificationUC: IScheduleBirthdayNotificationUC
) : ViewModel() {
    companion object {
        private const val TAG = "MainViewModel"
    }

    /**
     * Все операции записи локальных ответов идут строго по очереди.
     * Раньше «Готово» на последнем вопросе и сохранение в Firebase на Paywall запускались параллельно,
     * и в Firebase уходили ответы без последнего шага.
     */
    private val localDataMutex = Mutex()
    private val firebaseRepairMutex = Mutex()

    // Подтверждённые ответы (профиль, главная, регистрация).
    // Не asLiveData(): фрагменты читают .value без подписки, а asLiveData без наблюдателей не собирает Flow
    private val _onboardingAnswersLiveData = MutableLiveData(OnboardingAnswers())
    val onboardingAnswersLiveData: LiveData<OnboardingAnswers> = _onboardingAnswersLiveData

    // Черновик текущего прохождения опроса
    private val _onboardingDraftLiveData = MutableLiveData(OnboardingAnswers())
    val onboardingDraftLiveData: LiveData<OnboardingAnswers> = _onboardingDraftLiveData

    init {
        viewModelScope.launch {
            onboardingUC.getOnboardingAnswersFlow()
                .collect { _onboardingAnswersLiveData.value = it }
        }
        viewModelScope.launch {
            onboardingUC.getOnboardingDraftFlow().collect { _onboardingDraftLiveData.value = it }
        }
    }

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
    val authStateFlow: StateFlow<UserPresentation?> = getAuthStateFlowUC.execute()
        .map { firebaseUser ->
            firebaseUser?.let {
                UserPresentation(
                    uid = it.uid,
                    authEmail = it.email ?: "",
                    isEmailVerified = it.isEmailVerified,
                    userName = it.displayName ?: ""
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    // ERROR
    private val _errorMessageLiveData = MutableLiveData<MyEvent<Resource<ErrorType>>>()
    val errorMessageLiveData: LiveData<MyEvent<Resource<ErrorType>>> = _errorMessageLiveData

    // Ошибка локального сохранения завершённого опроса (Paywall предлагает повторить)
    private val _finishOnboardingErrorLiveData = MutableLiveData<MyEvent<ErrorType>>()
    val finishOnboardingErrorLiveData: LiveData<MyEvent<ErrorType>> = _finishOnboardingErrorLiveData

    // Добавляем отдельный LiveData для состояния повторного сохранения
    private val _retrySaveState = MutableLiveData<Resource<ErrorType>>()
    val retrySaveState: LiveData<Resource<ErrorType>> = _retrySaveState

    /**
     * ONBOARDING
     * Подтверждённые ответы не трогаются до конца опроса: отмена, закрытие приложения или смерть процесса
     * посреди опроса больше не стирают данные пользователя.
     */
    fun prepareForOnboarding() = launchLocalWrite("Error preparing for onboarding") {
        onboardingUC.startOnboardingDraft()
    }

    fun cancelOnboarding() = launchLocalWrite("Error cancelling onboarding") {
        onboardingUC.cancelOnboardingDraft()
    }

    fun updateOnboardingAnswers(update: (OnboardingAnswers) -> OnboardingAnswers) =
        launchLocalWrite("Failed to save onboarding answers") {
            onboardingUC.updateOnboardingDraft(update)
        }

    fun updateIsItABoy(answer: String) = updateOnboardingAnswers { it.copy(isItABoy = answer) }

    fun updateBirthDate(birthDate: String) =
        updateOnboardingAnswers { it.copy(childBirthDate = birthDate) }

    fun updateChildHobbies(hobbies: String) =
        updateOnboardingAnswers { it.copy(childHobbies = hobbies) }

    /**
     * Завершение опроса одной операцией: атомарно сохраняем ответы + статус «пройден»,
     * планируем поздравление и отправляем данные в Firebase. Повторный вызов безопасен (черновика уже нет).
     */
    fun finishOnboarding() {
        viewModelScope.launch {
            val committed = try {
                localDataMutex.withLock { onboardingUC.commitOnboardingDraft() }
            } catch (e: IOException) {
                Log.e(TAG, "Failed to commit onboarding", e)
                _finishOnboardingErrorLiveData.value =
                    MyEvent(ErrorType.FAILED_TO_SAVE_ONBOARDING_STATE_LOCAL)
                return@launch
            } ?: return@launch

            // Пользователь только что заполнил анкету – если сегодня день рождения, поздравляем сразу
            scheduleBirthdayNotificationUC.update(
                committed.childName,
                committed.childBirthDate,
                userIsInApp = true
            )
            saveAnswersToFirebaseIfLoggedIn(committed)?.let { _profileUpdatedLiveData.value = it }
        }
    }

    /**
     * LOGOUT
     */
    fun clearUserDataOnLogout() = launchLocalWrite("Error clearing user data on logout") {
        onboardingUC.clearAllOnboardingData()
        _checkedUserInfo.value = Resource.init(null)
        scheduleBirthdayNotificationUC.cancel()
    }

    fun retryDataSave() {
        viewModelScope.launch {
            _retrySaveState.value = Resource.loading(null)
            val answers =
                localDataMutex.withLock { onboardingUC.getOnboardingAnswersFlow().first() }
            val result = saveAnswersToFirebaseIfLoggedIn(answers)
                ?: Resource.error("User not authenticated", ErrorType.NOT_AUTHENTICATED_ERROR)
            _retrySaveState.value = result
            if (result.status == Status.SUCCESS) {
                _profileUpdatedLiveData.value = result
            }
        }
    }

    fun updateAuthEmail(newAuthEmail: String, password: String) {
        viewModelScope.launch {
            val trimmedLowercaseEmail = newAuthEmail.lowercase().trim()
            val validationResult = loginValidateEmailUC.execute(trimmedLowercaseEmail)

            if (validationResult.successful) {
                _authEmailUpdateState.value = Resource.loading(null)
                _authEmailUpdateState.value =
                    updateEmailUC.updateAuthEmail(trimmedLowercaseEmail, password)
            } else {
                _authEmailUpdateState.value =
                    Resource.error("Не верный адрес", validationResult.errorMessage)
            }
        }
    }

    /**
     * OTHER
     */
    fun checkLoggedInStateGetUser() {
        viewModelScope.launch {
            val result = getCurrentFirebaseUserUC.execute()
            _checkedUserInfo.value = result

            val user = result.data
            if (result.status != Status.SUCCESS || user == null) return@launch

            if (user.isValidData()) {
                // Данные могли измениться на другом устройстве – перепланируем
                scheduleBirthdayNotificationUC.update(
                    user.childName,
                    user.childBirthDate,
                    userIsInApp = true
                )
            } else {
                repairFirebaseProfileFromLocalData()
            }
        }
    }

    fun planningBirthdayNotificationOnStart() {
        viewModelScope.launch {
            val user = getCurrentUserPresentationUC().data
            // Если данных нет (например, нет сети) – UC перепланирует по ранее сохранённым, а не сбросит уведомление
            scheduleBirthdayNotificationUC.update(
                user?.childName.orEmpty(),
                user?.childBirthDate.orEmpty(),
                userIsInApp = true
            )
        }
    }

    /**
     * PRIVATE METHODS
     */
    private fun launchLocalWrite(errorLog: String, block: suspend () -> Unit) {
        viewModelScope.launch {
            try {
                localDataMutex.withLock { block() }
            } catch (e: IOException) {
                Log.e(TAG, errorLog, e)
                _errorMessageLiveData.value = MyEvent(
                    Resource.error("Не удалось сохранить данные", ErrorType.DATA_NOT_SAVED_ERROR)
                )
            }
        }
    }

    /** @return null, если пользователь не авторизован */
    private suspend fun saveAnswersToFirebaseIfLoggedIn(answers: OnboardingAnswers): Resource<ErrorType>? {
        val currentFirebaseUser = getCurrentFirebaseUserUC.execute().data ?: return null
        val updatedUser = currentFirebaseUser.copy(
            userName = answers.userName,
            childGender = answers.childGender,
            isItABoy = answers.isItABoy,
            childName = answers.childName,
            childBirthDate = answers.childBirthDate,
            childHobbies = answers.childHobbies
        )
        // UpdateFirebaseProfileUC сам проверяет полноту данных и не даст перезаписать профиль пустыми полями
        return updateFirebaseProfileUC.execute(updatedUser)
    }

    /**
     * Аккаунт создан, а документ профиля в Firestore пустой (например, связь пропала сразу после регистрации).
     * Если на устройстве есть полная анкета – досылаем её, чтобы данные не потерялись.
     */
    private suspend fun repairFirebaseProfileFromLocalData() {
        if (!firebaseRepairMutex.tryLock()) return
        try {
            val localAnswers = onboardingUC.getOnboardingAnswersFlow().first()
            if (!localAnswers.isValidData()) return

            Log.w(TAG, "Firestore profile is incomplete, uploading local answers")
            val result = saveAnswersToFirebaseIfLoggedIn(localAnswers) ?: return
            if (result.status == Status.SUCCESS) {
                _checkedUserInfo.value = getCurrentFirebaseUserUC.execute()
            }
        } finally {
            firebaseRepairMutex.unlock()
        }
    }
}