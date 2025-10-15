package com.foxynest.testappforonboarding.presentation

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.foxynest.testappforonboarding.domain.use_cases.onboardingUseCases.IOnboardingUC
import com.foxynest.testappforonboarding.domain.entities.AnswerNames
import com.foxynest.testappforonboarding.domain.entities.ErrorType
import com.foxynest.testappforonboarding.domain.entities.UserPresentation
import com.foxynest.testappforonboarding.domain.use_cases.authUseCases.login.GetAuthStateFlowUC
import com.foxynest.testappforonboarding.domain.use_cases.authUseCases.login.GetCurrentUserUC
import com.foxynest.testappforonboarding.domain.use_cases.authUseCases.login.IUpdateProfileUC
import com.foxynest.testappforonboarding.other.MyEvent
import com.foxynest.testappforonboarding.other.Resource
import com.foxynest.testappforonboarding.other.Status
import com.foxynest.testappforonboarding.other.constants.CommonConstants
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.IOException
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val onboardingUC: IOnboardingUC,
    private val updateProfileUC: IUpdateProfileUC,
    private val getCurrentUserUC: GetCurrentUserUC,
    private val getAuthStateFlowUC: GetAuthStateFlowUC
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
//    private val _childAgeLiveData = MutableLiveData<Int>()
//    val childAgeLiveData: LiveData<Int> = _childAgeLiveData
    private val _hobbyLiveData = MutableLiveData<String>()
    val hobbyLiveData: LiveData<String> = _hobbyLiveData

    // Для временного хранения во время заполнения опроса (нужно при возвращении назад по страницам в онбординге)
    private val _answers = mutableMapOf<String, String>()
    val answers: Map<String, String> get() = _answers

    private val _profileUpdatedLiveData = MutableLiveData<Resource<ErrorType>>()
    val profileUpdatedLiveData: LiveData<Resource<ErrorType>> = _profileUpdatedLiveData

//    // REGISTER
//    // For a toast of registration state
//    private val _registrationStateFlow = MutableStateFlow<Resource<Unit>>(Resource.init(null))
//    val registrationStateFlow: StateFlow<Resource<Unit>> = _registrationStateFlow

    // Log in state, returns user
    // Для такого же метода/лайв даты в фрагменте/viewmodel профиля:
//    viewModel.userState.onEach { user ->
//        user?.let {
//            binding.apply {
//                etUsername.setText(it.username)
//                etChildName.setText(it.childName)
//                etChildAge.setText(it.childAge.toString())
//                etChildBirthDate.setText(it.childBirthDate)
//                etEmail.setText(it.email)
//
//                if (it.email.isNotEmpty() && !it.isEmailVerified) {
//                    btnVerifyEmail.visibility = View.VISIBLE
//                    tvEmailNotVerified.visibility = View.VISIBLE
//                } else {
//                    btnVerifyEmail.visibility = View.GONE
//                    tvEmailNotVerified.visibility = View.GONE
//                }
//            }
//        }
//    }.launchIn(viewLifecycleOwner.lifecycleScope)
    // И ИМЕТЬ В ВИДУ:
    // Status.INIT -> Do nothing,
    // Status.SUCCESS ->
    //    if (user == null) {tvLoggedIn.text = "You are not logged in"}
    //    else {
    //        tvLoggedIn.text = "You are logged in!"
    //        etUsername.setText(user.displayName)
    //        ivProfilePicture.setImageURI(user.photoUrl)
    //    }
    //    Status.ERROR, Status.LOADING

    //    binding.btnUpdateProfile.setOnClickListener {
//        val user = viewModel.userState.value?.copy(
//            username = binding.etUsername.text.toString(),
//            childName = binding.etChildName.text.toString(),
//            childAge = binding.etChildAge.text.toString().toIntOrNull() ?: 0,
//            childBirthDate = binding.etChildBirthDate.text.toString()
//        )
//        user?.let { viewModel.updateProfile(it) }
//    }
//
//    binding.btnVerifyEmail.setOnClickListener {
//        viewModel.sendVerificationEmail()
//    }
//
//    binding.btnLogout.setOnClickListener {
//        viewModel.logout()
//        findNavController().navigate(R.id.action_to_login)
//    }
    //
    // !!!В нашем случае (фрагмент решистрации или логина) после успешной регистрации или авторизации нужно перейти на страницу профиля!!!
    // In fragment:
    //         viewModel.registrationState.onEach { resource ->
    //            when (resource) {
    //                is Resource.Success -> findNavController().navigate(R.id.action_to_profile)
    //                is Resource.Error -> showError(resource.message)
    //                // Обработка других состояний
    //            }
    //        }.launchIn(viewLifecycleOwner.lifecycleScope)
    // Разовая проверка состояния в firebase
    private val _checkedUserInfo =
        MutableStateFlow<Resource<UserPresentation?>>(Resource.init(null))
    val checkedUserInfo: StateFlow<Resource<UserPresentation?>> = _checkedUserInfo

    // Подписка на изменение состояния в firebase
//    private val _userStateFlow = MutableStateFlow<Resource<User?>>(Resource.init(null))
//    val userStateFlow: StateFlow<Resource<User?>> = _userStateFlow

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

    // If smth went wrong
    private val _errorMessageLiveData =
        MutableLiveData<MyEvent<Resource<ErrorType>>>() // It must be private, so that other classes can't change it
    val errorMessageLiveData: LiveData<MyEvent<Resource<ErrorType>>> =
        _errorMessageLiveData // And another LiveData, that equals to previous, so that classes can't change it

    // Добавляем отдельный LiveData для состояния повторного сохранения
    private val _retrySaveState = MutableLiveData<Resource<ErrorType>>()
    val retrySaveState: LiveData<Resource<ErrorType>> = _retrySaveState

//        private val _text = MutableLiveData<String>().apply {
//        value = "This is Profile Fragment"
//    }
//    val text: LiveData<String> = _text

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
//                onboardingUC.saveChildAge(0)
                onboardingUC.saveHobbyAnswer("")
                saveAnswer(AnswerNames.CHILD_BIRTH_DATE_ANSWER.toString(), "")
//                saveAnswer(AnswerNames.CHILD_AGE_ANSWER.toString(), "0")
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

//    fun saveChildAgeAnswer(age: Int) {
//        viewModelScope.launch(Dispatchers.IO) {
//            try {
//                onboardingUC.saveChildAge(age)
//                saveAnswer(AnswerNames.CHILD_AGE_ANSWER.toString(), age.toString())
//            } catch (e: IOException) {
//                Log.e(TAG, "Failed to save child age", e)
//                _errorMessageLiveData.postValue(
//                    MyEvent(
//                        Resource.error(
//                            "Не удалось сохранить возраст ребенка на вашем телефоне",
//                            ErrorType.FAILED_TO_SAVE_CHILD_AGE_LOCAL
//                        )
//                    )
//                )
//            } catch (e: Exception) {
//                Log.e(TAG, "Unexpected error saving child age", e)
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
    fun getAndUseAnswersFromOnboardind() {

        // Теперь мы вызываем опросник не только при первом запуске, но и если нажали кнопку "обновить поля"
        // Значит, мы можем находиться здесь и для обновления существующих полей. Узнаем, залогинен ли человек, чтобы знать, нужно ли сохранять данные на сервер или использовать их через Data store
        viewModelScope.launch(Dispatchers.IO) {
            val currentUser = getCurrentUserUC.execute().data

            getAndUseUserNameAnswer()
            getAndUseEmailAnswer()
            getAndUseGenderAnswer()
            getAndUseChildNameAnswer()
            getAndUseIsItABoyAnswer()
            getAndUseBirthDateAnswer()
//            getAndUseChildAgeAnswer()
            getAndHobbyAnswer()

            if (currentUser != null) {
//                val updatedUser = UserPresentation(
//                    uid = currentUser.uid,
//                    email = answers[AnswerNames.EMAIL_ANSWER.toString()] ?: "",
//                    isEmailVerified = answers[AnswerNames.IS_EMAIL_VERIFIED_ANSWER.toString()].toBoolean(),
//                    userName = answers[AnswerNames.USER_NAME_ANSWER.toString()] ?: "",
//                    childGender = answers[AnswerNames.CHILD_GENDER_ANSWER.toString()] ?: "",
//                    isItABoy = answers[AnswerNames.IS_IT_A_BOY_ANSWER.toString()] ?: "",
//                    childName = answers[AnswerNames.CHILD_NAME_ANSWER.toString()] ?: "",
//                    childHobby = answers[AnswerNames.CHILD_HOBBY_ANSWER.toString()] ?: "",
//                    childAge = answers[AnswerNames.CHILD_AGE_ANSWER.toString()]?.toInt() ?: 0,
//                    childBirthDate = answers[AnswerNames.CHILD_BIRTH_DATE_ANSWER.toString()] ?: ""
//                )
                val updatedUser = createUserFromAnswers(currentUser.uid)

                _profileUpdatedLiveData.postValue(updateProfileUC.execute(updatedUser))
            }
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

                // Также обновляем основной LiveData для consistency
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
//            childAge = answers[AnswerNames.CHILD_AGE_ANSWER.toString()]?.toInt() ?: 0,
            childBirthDate = answers[AnswerNames.CHILD_BIRTH_DATE_ANSWER.toString()] ?: ""
        )
    }

    private fun getAndUseUserNameAnswer() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                onboardingUC.getUserNameFlow().collect { userName ->
                    userName?.let {
                        _userNameLiveData.postValue(it)
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

    private fun getAndUseEmailAnswer() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                onboardingUC.getEmailFlow().collect { email ->
                    email?.let {
                        _emailLiveData.postValue(it)
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

    private fun getAndUseGenderAnswer() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                onboardingUC.getGenderFlow().collect { gender ->
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

    private fun getAndUseChildNameAnswer() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                onboardingUC.getNameFlow().collect { gender ->
                    gender?.let { notNullGender ->
                        _childNameLiveData.postValue(notNullGender)
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
                onboardingUC.getIsItABoyFlow().collect { gender ->
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

    private fun getAndUseBirthDateAnswer() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                onboardingUC.getBirthDateFlow().collect { birthDate ->
                    birthDate?.let {
                        _childBirthDateLiveData.postValue(it)
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

//    private fun getAndUseChildAgeAnswer() {
//        viewModelScope.launch(Dispatchers.IO) {
//            try {
//                onboardingUC.getChildAgeFlow().collect { age ->
//                    age?.let {
//                        _childAgeLiveData.postValue(it)
//                    }
//                }
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

    private fun getAndHobbyAnswer() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                onboardingUC.getHobbyFlow().collect { gender ->
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
            "Проверка: Размер ${_answers.size}, Пол: ${_answers[AnswerNames.CHILD_GENDER_ANSWER.toString()]}"
        )
    }

    fun checkLoggedInStateGetUser() {
        viewModelScope.launch {
            _checkedUserInfo.value = getCurrentUserUC.execute()
        }
    }
}