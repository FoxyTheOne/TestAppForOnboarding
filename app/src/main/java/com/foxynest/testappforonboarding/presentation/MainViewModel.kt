package com.foxynest.testappforonboarding.presentation

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.foxynest.testappforonboarding.domain.use_cases.onboardingUseCases.IOnboardingUC
import com.foxynest.testappforonboarding.domain.entities.AnswerNames
import com.foxynest.testappforonboarding.domain.entities.UserPresentation
import com.foxynest.testappforonboarding.domain.use_cases.authUseCases.login.GetAuthStateFlowUC
import com.foxynest.testappforonboarding.domain.use_cases.authUseCases.login.GetCurrentUserUC
import com.foxynest.testappforonboarding.domain.use_cases.authUseCases.login.IUpdateProfileUC
import com.foxynest.testappforonboarding.other.MyEvent
import com.foxynest.testappforonboarding.other.Resource
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
    private val _emailLiveData = MutableLiveData<String>() // TODO
    val emailLiveData: LiveData<String> = _emailLiveData
    private val _userNameLiveData = MutableLiveData<String>() // TODO
    val userNameLiveData: LiveData<String> = _userNameLiveData
    private val _genderLiveData = MutableLiveData<String>()
    val genderLiveData: LiveData<String> = _genderLiveData
    private val _isItABoyLiveData = MutableLiveData<String>()
    val isItABoyLiveData: LiveData<String> = _isItABoyLiveData
    private val _childNameLiveData = MutableLiveData<String>()
    val childNameLiveData: LiveData<String> = _childNameLiveData
    private val _hobbyLiveData = MutableLiveData<String>()
    val hobbyLiveData: LiveData<String> = _hobbyLiveData
    private val _childAgeLiveData = MutableLiveData<Int>() // TODO
    val childAgeLiveData: LiveData<Int> = _childAgeLiveData
    private val _childBirthDateLiveData = MutableLiveData<String>() // TODO
    val childBirthDateLiveData: LiveData<String> = _childBirthDateLiveData

    // Для временного хранения во время заполнения опроса (нужно при возвращении назад по страницам в онбординге)
    private val _answers = mutableMapOf<String, String>()
    val answers: Map<String, String> get() = _answers

    private val _profileUpdatedLiveData = MutableLiveData<Resource<Unit>>()
    val profileUpdatedLiveData: LiveData<Resource<Unit>> = _profileUpdatedLiveData

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
        MutableLiveData<MyEvent<Resource<Boolean>>>() // It must be private, so that other classes can't change it
    val errorMessageLiveData: LiveData<MyEvent<Resource<Boolean>>> =
        _errorMessageLiveData // And another LiveData, that equals to previous, so that classes can't change it

//        private val _text = MutableLiveData<String>().apply {
//        value = "This is Profile Fragment"
//    }
//    val text: LiveData<String> = _text

    // ONBOARDING
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
                onboardingUC.saveHobbyAnswer("")
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

    fun saveGenderAnswer(genderAnswer: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                onboardingUC.saveGender(genderAnswer) // Сохраняем в data store
                saveAnswer(AnswerNames.CHILD_GENDER_ANSWER.toString(), genderAnswer)
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
                onboardingUC.saveName(nameAnswer)
                saveAnswer(AnswerNames.CHILD_NAME_ANSWER.toString(), nameAnswer)
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
                onboardingUC.saveIsItABoy(answer)
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
                onboardingUC.saveHobbyAnswer(hobby)
                saveAnswer(AnswerNames.CHILD_HOBBY_ANSWER.toString(), hobby)
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

        // Теперь мы вызываем опросник не только при первом запуске, но и если нажали кнопку "обновить поля"
        // Значит, мы можем находиться здесь и для обновления существующих полей. Узнаем, залогинен ли человек, чтобы знать, нужно ли сохранять данные на сервер или использовать их через Data store
        viewModelScope.launch(Dispatchers.IO) {
            val currentUser = getCurrentUserUC.execute().data

            getAndUseGenderAnswer()
            getAndUseNameAnswer()
            getAndUseIsItABoyAnswer()
            getAndHobbyAnswer()

            if (currentUser != null) {
                val updatedUser = UserPresentation(
                    uid = currentUser.uid,
                    email = answers[AnswerNames.EMAIL_ANSWER.toString()] ?: "",
                    isEmailVerified = answers[AnswerNames.IS_EMAIL_VERIFIED_ANSWER.toString()].toBoolean(),
                    userName = answers[AnswerNames.USER_NAME_ANSWER.toString()] ?: "",
                    childGender = answers[AnswerNames.CHILD_GENDER_ANSWER.toString()] ?: "",
                    isItABoy = answers[AnswerNames.IS_IT_A_BOY_ANSWER.toString()] ?: "",
                    childName = answers[AnswerNames.CHILD_NAME_ANSWER.toString()] ?: "",
                    childHobby = answers[AnswerNames.CHILD_HOBBY_ANSWER.toString()] ?: "",
                    childAge = answers[AnswerNames.CHILD_AGE_ANSWER.toString()]?.toInt() ?: 0,
                    childBirthDate = answers[AnswerNames.CHILD_BIRTH_DATE_ANSWER.toString()] ?: ""
                )

                _profileUpdatedLiveData.postValue(updateProfileUC.execute(updatedUser))
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

    private fun getAndUseNameAnswer() {
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