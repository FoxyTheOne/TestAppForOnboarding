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
import com.foxynest.testappforonboarding.domain.entities.user.toUserPresentation
import com.foxynest.testappforonboarding.domain.use_cases.authUseCases.account.IDeleteAccountUC
import com.foxynest.testappforonboarding.domain.use_cases.authUseCases.login.IGetAuthStateFlowUC
import com.foxynest.testappforonboarding.domain.use_cases.authUseCases.login.IIsUserLoggedInUC
import com.foxynest.testappforonboarding.domain.use_cases.authUseCases.login.ILoginValidateEmailUC
import com.foxynest.testappforonboarding.domain.use_cases.authUseCases.login.ILogoutUC
import com.foxynest.testappforonboarding.domain.use_cases.authUseCases.login.IObserveCurrentUserUC
import com.foxynest.testappforonboarding.domain.use_cases.notification.IScheduleBirthdayNotificationUC
import com.foxynest.testappforonboarding.domain.use_cases.onboardingUseCases.IOnboardingUC
import com.foxynest.testappforonboarding.domain.use_cases.updatingDataUseCases.IUpdateEmailUC
import com.foxynest.testappforonboarding.domain.use_cases.updatingDataUseCases.IUpdateFirebaseProfileUC
import com.foxynest.testappforonboarding.other.MyEvent
import com.foxynest.testappforonboarding.other.Resource
import com.foxynest.testappforonboarding.other.Status
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.IOException
import javax.inject.Inject

/**
 * Общая ViewModel приложения: привязана к MainActivity, поэтому все фрагменты получают ОДИН и тот же объект
 * (`by activityViewModels()`). Через неё экраны делят анкету, профиль и события аккаунта.
 *
 * Что здесь:
 * - анкета: черновик опроса и подтверждённые ответы (локально, DataStore);
 * - профиль из Firestore ([checkedUserInfo]) и состояние входа ([authStateFlow]);
 * - выход, удаление аккаунта, смена почты, повтор сохранения в Firebase;
 * - планирование поздравления и момент, когда спросить разрешение на уведомления.
 *
 * Два вида данных для экранов (см. MyEvent):
 * - состояние (`...State`, `checkedUserInfo`) – экран показывает его снова после пересоздания;
 * - событие (`...Event`, MyEvent) – срабатывает один раз (тост, переход, диалог).
 *
 * При изменениях в Data Store (добавление, изменение полей) нужна миграция (MigrateUserDataRepository)!
 * Убедиться, что данные пользователей не пропадут (как зарегистрированных, так и нет).
 */
@HiltViewModel
class MainViewModel @Inject constructor(
    private val onboardingUC: IOnboardingUC,
    private val updateFirebaseProfileUC: IUpdateFirebaseProfileUC,
    observeCurrentUserUC: IObserveCurrentUserUC,
    getAuthStateFlowUC: IGetAuthStateFlowUC,
    private val isUserLoggedInUC: IIsUserLoggedInUC,
    private val loginValidateEmailUC: ILoginValidateEmailUC,
    private val updateEmailUC: IUpdateEmailUC,
    private val logoutUC: ILogoutUC,
    private val deleteAccountUC: IDeleteAccountUC,
    private val scheduleBirthdayNotificationUC: IScheduleBirthdayNotificationUC
) : ViewModel() {
    companion object {
        private const val TAG = "MainViewModel"
        private const val STOP_TIMEOUT_MILLIS = 5_000L
    }

    /**
     * Все операции записи локальных ответов идут строго по очереди.
     * Раньше «Готово» на последнем вопросе и сохранение в Firebase на Paywall запускались параллельно,
     * и в Firebase уходили ответы без последнего шага.
     */
    private val localDataMutex = Mutex()
    private val firebaseRepairMutex = Mutex()

    // Выход/удаление аккаунта инициированы пользователем: это не «принудительный выход», и профиль не надо «чинить»
    private var isAccountOperationInProgress = false

    // Подтверждённые ответы (профиль, главная, регистрация).
    // Не asLiveData(): фрагменты читают .value без подписки, а asLiveData без наблюдателей не собирает Flow
    private val _onboardingAnswersLiveData = MutableLiveData(OnboardingAnswers())
    val onboardingAnswersLiveData: LiveData<OnboardingAnswers> = _onboardingAnswersLiveData

    // Черновик текущего прохождения опроса
    private val _onboardingDraftLiveData = MutableLiveData(OnboardingAnswers())
    val onboardingDraftLiveData: LiveData<OnboardingAnswers> = _onboardingDraftLiveData

    /**
     * Профиль из Firestore через один snapshot-листенер (раньше – get() в onStart каждого экрана).
     * Листенер активен, пока на профиль подписан хотя бы один экран, и отключается через 5 секунд после ухода в фон.
     *
     * `stateIn` превращает Flow в StateFlow – «Flow с текущим значением» (`.value` можно прочитать в любой момент):
     * - `SharingStarted.WhileSubscribed(5000)` – работать, пока есть подписчики, и ещё 5 секунд после последнего.
     *   5 секунд – чтобы при повороте экрана (подписчик уходит и сразу возвращается) не переподключаться к базе;
     * - `Resource.init(null)` – значение до первого ответа.
     *
     * `onEach(::onRemoteUserChanged)` – на каждое изменение профиля перепланируем поздравление
     * (дату могли поменять на другом устройстве) или досылаем анкету, если документ на сервере неполный.
     */
    val checkedUserInfo: StateFlow<Resource<UserPresentation?>> = observeCurrentUserUC.execute()
        .onEach(::onRemoteUserChanged)
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            Resource.init(null)
        )

    // Только состояние авторизации, без запроса профиля
    val authStateFlow: StateFlow<UserPresentation?> = getAuthStateFlowUC.execute()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), null)

    // PROFILE
    private val _profileUpdatedEvent = MutableLiveData<MyEvent<Resource<ErrorType>>>()
    val profileUpdatedEvent: LiveData<MyEvent<Resource<ErrorType>>> = _profileUpdatedEvent

    // Изменение email в профиле. Состояние, а не событие: диалог переживает пересоздание экрана и читает его заново
    private val _authEmailUpdateState = MutableLiveData<Resource<ErrorType>>(Resource.init(null))
    val authEmailUpdateState: LiveData<Resource<ErrorType>> = _authEmailUpdateState

    private val _deleteAccountState = MutableLiveData<Resource<ErrorType>>(Resource.init(null))
    val deleteAccountState: LiveData<Resource<ErrorType>> = _deleteAccountState

    private val _logoutEvent = MutableLiveData<MyEvent<Resource<ErrorType>>>()
    val logoutEvent: LiveData<MyEvent<Resource<ErrorType>>> = _logoutEvent

    // Firebase сам завершил сессию (например, почта для входа изменена по ссылке из письма)
    private val _sessionEndedEvent = MutableLiveData<MyEvent<Unit>>()
    val sessionEndedEvent: LiveData<MyEvent<Unit>> = _sessionEndedEvent

    private val _askNotificationPermissionEvent = MutableLiveData<MyEvent<Unit>>()
    val askNotificationPermissionEvent: LiveData<MyEvent<Unit>> = _askNotificationPermissionEvent

    // ERROR
    private val _errorMessageLiveData = MutableLiveData<MyEvent<Resource<ErrorType>>>()
    val errorMessageLiveData: LiveData<MyEvent<Resource<ErrorType>>> = _errorMessageLiveData

    // Ошибка локального сохранения завершённого опроса (Paywall предлагает повторить)
    private val _finishOnboardingErrorLiveData = MutableLiveData<MyEvent<ErrorType>>()
    val finishOnboardingErrorLiveData: LiveData<MyEvent<ErrorType>> = _finishOnboardingErrorLiveData

    private val _retrySaveState = MutableLiveData<Resource<ErrorType>>(Resource.init(null))
    val retrySaveState: LiveData<Resource<ErrorType>> = _retrySaveState

    init {
        viewModelScope.launch {
            onboardingUC.getOnboardingAnswersFlow()
                .collect { _onboardingAnswersLiveData.value = it }
        }
        viewModelScope.launch {
            onboardingUC.getOnboardingDraftFlow().collect { _onboardingDraftLiveData.value = it }
        }
        observeSessionEnd(getAuthStateFlowUC)
    }

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
            // Лучший момент спросить разрешение: пользователь только что указал дату рождения
            askNotificationPermissionIfNeeded()
            saveAnswersToFirebaseIfLoggedIn(committed)?.let {
                _profileUpdatedEvent.value = MyEvent(it)
            }
        }
    }

    /**
     * ACCOUNT
     */
    fun logout() {
        viewModelScope.launch {
            isAccountOperationInProgress = true
            val result = logoutUC.execute()
            if (result.status != Status.SUCCESS) isAccountOperationInProgress = false
            _logoutEvent.value = MyEvent(result)
        }
    }

    fun resetDeleteAccountState() {
        _deleteAccountState.value = Resource.init(null)
    }

    fun deleteAccount(password: String) {
        viewModelScope.launch {
            _deleteAccountState.value = Resource.loading(null)
            isAccountOperationInProgress = true
            val result = deleteAccountUC.execute(password)
            if (result.status != Status.SUCCESS) isAccountOperationInProgress = false
            _deleteAccountState.value = result
        }
    }

    fun resetAuthEmailUpdateState() {
        _authEmailUpdateState.value = Resource.init(null)
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
                    Resource.error("Invalid email", validationResult.errorMessage)
            }
        }
    }

    fun retryDataSave() {
        viewModelScope.launch {
            _retrySaveState.value = Resource.loading(null)
            val answers =
                localDataMutex.withLock { onboardingUC.getOnboardingAnswersFlow().first() }
            val result = saveAnswersToFirebaseIfLoggedIn(answers)
                ?: Resource.error("User not authenticated", ErrorType.NOT_AUTHENTICATED_ERROR)
            _retrySaveState.value = result
        }
    }

    fun resetRetrySaveState() {
        _retrySaveState.value = Resource.init(null)
    }

    /**
     * STARTUP
     */
    fun onAppStarted() {
        viewModelScope.launch {
            if (isUserLoggedInUC.execute()) {
                // Для вошедшего пользователя данные придут из листенера профиля; пока – по сохранённым локально
                scheduleBirthdayNotificationUC.rearmFromSavedData(userIsInApp = true)
            } else {
                val answers = onboardingUC.getOnboardingAnswersFlow().first()
                scheduleBirthdayNotificationUC.update(
                    answers.childName,
                    answers.childBirthDate,
                    userIsInApp = true
                )
            }

            val hasBirthdayData =
                isUserLoggedInUC.execute() || onboardingUC.isOnboardingCompleted().first()
            if (hasBirthdayData) askNotificationPermissionIfNeeded()
        }
    }

    fun markNotificationPermissionAsked() {
        viewModelScope.launch { scheduleBirthdayNotificationUC.markNotificationPermissionAsked() }
    }

    /**
     * PRIVATE METHODS
     */
    private suspend fun askNotificationPermissionIfNeeded() {
        if (scheduleBirthdayNotificationUC.shouldAskNotificationPermission()) {
            _askNotificationPermissionEvent.value = MyEvent(Unit)
        }
    }

    /**
     * «Был пользователь – стал null» без нажатия «Выйти» значит, что сессию завершил Firebase
     * (почту для входа сменили по ссылке из письма, пароль сменили на другом устройстве, аккаунт удалили в консоли).
     * Тогда показываем пояснение, а не молча выкидываем человека из аккаунта.
     */
    private fun observeSessionEnd(getAuthStateFlowUC: IGetAuthStateFlowUC) {
        viewModelScope.launch {
            var previousUid: String? = null
            getAuthStateFlowUC.execute().collect { user ->
                if (previousUid != null && user == null) {
                    if (isAccountOperationInProgress) {
                        isAccountOperationInProgress = false
                    } else {
                        _sessionEndedEvent.value = MyEvent(Unit)
                    }
                }
                previousUid = user?.uid
            }
        }
    }

    private suspend fun onRemoteUserChanged(result: Resource<UserPresentation?>) {
        val user = result.data
        if (result.status != Status.SUCCESS || user == null || isAccountOperationInProgress) return

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

    /** Запись в DataStore строго по очереди (через [localDataMutex]); ошибка диска → тост вместо падения. */
    private fun launchLocalWrite(errorLog: String, block: suspend () -> Unit) {
        viewModelScope.launch {
            try {
                localDataMutex.withLock { block() }
            } catch (e: IOException) {
                Log.e(TAG, errorLog, e)
                _errorMessageLiveData.value = MyEvent(
                    Resource.error("Failed to save data", ErrorType.DATA_NOT_SAVED_ERROR)
                )
            }
        }
    }

    /** @return null, если пользователь не авторизован. Профиль из сети не читается – для записи нужен только вход. */
    private suspend fun saveAnswersToFirebaseIfLoggedIn(answers: OnboardingAnswers): Resource<ErrorType>? {
        if (!isUserLoggedInUC.execute()) return null
        // UpdateFirebaseProfileUC сам проверяет полноту данных и не даст перезаписать профиль пустыми полями
        return updateFirebaseProfileUC.execute(answers.toUserPresentation())
    }

    /**
     * Аккаунт создан, а документ профиля в Firestore пустой (например, связь пропала сразу после регистрации).
     * Если на устройстве есть полная анкета – досылаем её, чтобы данные не потерялись.
     * Листенер сам пришлёт обновлённый профиль после записи.
     */
    private suspend fun repairFirebaseProfileFromLocalData() {
        if (!firebaseRepairMutex.tryLock()) return
        try {
            val localAnswers = onboardingUC.getOnboardingAnswersFlow().first()
            if (!localAnswers.isValidData()) return
            Log.w(TAG, "Firestore profile is incomplete, uploading local answers")
            saveAnswersToFirebaseIfLoggedIn(localAnswers)
        } finally {
            firebaseRepairMutex.unlock()
        }
    }
}