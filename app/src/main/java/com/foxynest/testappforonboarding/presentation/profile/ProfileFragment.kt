package com.foxynest.testappforonboarding.presentation.profile

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.foxynest.testappforonboarding.R
import com.foxynest.testappforonboarding.databinding.FragmentProfileBinding
import com.foxynest.testappforonboarding.domain.entities.ErrorType
import com.foxynest.testappforonboarding.domain.entities.user.UserPresentation
import com.foxynest.testappforonboarding.other.Status
import com.foxynest.testappforonboarding.other.constants.CommonConstants.PROFILE_DESTINATION
import com.foxynest.testappforonboarding.other.extensions.calculateAge
import com.foxynest.testappforonboarding.other.extensions.createUserFromLocalDataInFragment
import com.foxynest.testappforonboarding.other.extensions.isUserDataValid
import com.foxynest.testappforonboarding.other.extensions.safeNavigate
import com.foxynest.testappforonboarding.other.extensions.toErrorMessage
import com.foxynest.testappforonboarding.presentation.MainViewModel
import com.foxynest.testappforonboarding.presentation.dialogs.ChangeAuthEmailDialog
import com.foxynest.testappforonboarding.presentation.dialogs.ChangeDisplayEmailDialog
import com.foxynest.testappforonboarding.presentation.dialogs.DataSaveErrorDialog
import com.foxynest.testappforonboarding.presentation.dialogs.simpleDialogs.ConfirmOnboardingRestartDialog
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


@AndroidEntryPoint
class ProfileFragment : Fragment() {
    companion object {
        private const val TAG = "ProfileFragment"
    }

    private var localFieldsObserver: Observer<Any>? = null
    private var dataSaveErrorDialog: DataSaveErrorDialog? = null
//    private var changeDisplayEmailDialog: ChangeDisplayEmailDialog? = null
    private var changeAuthEmailDialog: ChangeAuthEmailDialog? = null

    // VIEW BINDING -> 1. Объявляем переменную. This property is only valid between onCreateView and onDestroyView
    private var binding: FragmentProfileBinding? = null

    // 1.1. ViewModel. We bind our viewModel to the cycle of our activity, not fragment. So, we need to do this way:
    private lateinit var mainViewModel: MainViewModel
    private val profileViewModel by viewModels<ProfileViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // VIEW BINDING -> 2. Инициализация
        binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1.2. ViewModel. We bind our viewModel to the lifecycle of our activity, not fragment. We pass our activity as an owner of the lifecycle.
        // So, we need to do this way:
        mainViewModel = ViewModelProvider(requireActivity())[MainViewModel::class.java]

        // СНАЧАЛА подписываемся на observers
        subscribeOnObservers()

        // ПОТОМ инициализируем listeners
        initListeners()

        // И принудительно проверяем текущие данные
        checkCurrentData()
    }

    // Firebase будет держать user зарегистрированным даже после того, как он закроет программу и откроет заново. Поэтому нам нужно переписать метод onStart(), чтобы проверять, is user logged in in firebase or not
    override fun onStart() {
        super.onStart()
        mainViewModel.checkLoggedInStateGetUser()
    }

    private fun initListeners() {
        binding?.textGenderAnswerIsItABoy?.text = getString(R.string.no)

        binding?.buttonVerifyEmail?.setOnClickListener {
            profileViewModel.sendVerificationEmail()
        }

        binding?.buttonLogIn?.setOnClickListener {
            val direction = ProfileFragmentDirections.actionNavigationProfileToLoginFragment(
                PROFILE_DESTINATION
            )
            safeNavigate(R.id.navigation_profile, direction)
        }

        binding?.buttonRegisterAndLogin?.setOnClickListener {
            // Открываем по клику другой фрагмент, передаём туда user
            val user =
                mainViewModel.onboardingAnswersLiveData.createUserFromLocalDataInFragment(activity)

            val direction =
                ProfileFragmentDirections.actionNavigationProfileToRegisterFragment(user)
            safeNavigate(R.id.navigation_profile, direction)
        }

        binding?.buttonLogOut?.setOnClickListener {
            // 1. Выйти из аккаунта
            profileViewModel.logout()
            // 2. Обновить профиль - обновляется в logoutStateFlow
        }

        binding?.buttonRefillFields?.setOnClickListener {
            showConfirmOnboardingRestartDialog()
        }

//        binding?.buttonChangeDisplayEmail?.setOnClickListener {
//            showChangeDisplayEmailDialog()
//        }

        binding?.buttonChangeAuthEmail?.setOnClickListener {
            showChangeAuthEmailDialog()
        }
    }

    private fun subscribeOnObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            mainViewModel.checkedUserInfo
                .flowWithLifecycle(viewLifecycleOwner.lifecycle, Lifecycle.State.STARTED)
                .collect { resourceUser ->
                    val user = resourceUser.data
                    if (user != null) {
                        removeLocalObservers()
                        // Заполняем поля исходя из данных на сервере
                        fillFieldsInProfile(user)
                        showLogOutButtons(true)
                    } else {
                        // Локальные данные
                        setupLocalObservers()
                        // Принудительно обновляем из локальных данных
                        val localUser =
                            mainViewModel.onboardingAnswersLiveData.createUserFromLocalDataInFragment(
                                activity
                            )
                        if (localUser.isUserDataValid()) {
                            fillFieldsInProfile(localUser)
                        }
                        showLogOutButtons(false)
                    }
                }
        }

        // Подписка на состояние AuthState
        viewLifecycleOwner.lifecycleScope.launch {
            mainViewModel.authStateFlow
                .flowWithLifecycle(viewLifecycleOwner.lifecycle, Lifecycle.State.STARTED)
                .collect { user ->

                    if (user != null) {
                        // Пользователь авторизован:
                        // 1. Обновляем данные профиля в UI
                        mainViewModel.checkLoggedInStateGetUser() // Получаем user и обновляем данные в checkedUserInfo LiveData
                        // 2. Убираем кнопки для авторизации и регистрации, text_warning, text_warning_data, показываем кнопки "log out" и "обновить поля"
                        showLogOutButtons(true)
                    } else {
                        // Пользователь не авторизован:
                        // 1. Показываем данные профиля в UI исходя из пройденного онбординга (data store)
//                        fillFieldsInProfile(createUserFromLocalData())
                        val localUser =
                            mainViewModel.onboardingAnswersLiveData.createUserFromLocalDataInFragment(
                                activity
                            )
                        if (localUser.isUserDataValid()) {
                            fillFieldsInProfile(localUser)
                        }
                        // 2. Показываем кнопки для авторизации и регистрации, text_warning, text_warning_data, убираем кнопки "log out" и "обновить поля"
                        showLogOutButtons(false)
                    }

                }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            profileViewModel.isVerificationButtonEnabled
                .flowWithLifecycle(viewLifecycleOwner.lifecycle, Lifecycle.State.STARTED)
                .collect { isEnabled ->
                    binding?.buttonVerifyEmail?.isEnabled = isEnabled

                    context?.let {
                        val colorGray = ContextCompat.getColor(it, R.color.gray)
                        val colorWhite = ContextCompat.getColor(it, R.color.white)

                        when (isEnabled) {
                            false -> {
                                binding?.buttonVerifyEmail?.setTextColor(colorGray) ?: "#3D3C3C"
                                binding?.textEmailNotVerified?.text =
                                    getString(R.string.profile_email_verification_send)
                            }

                            true -> {
                                binding?.buttonVerifyEmail?.setTextColor(colorWhite)
                                    ?: "#FFFFFFFF"
                                binding?.textEmailNotVerified?.text =
                                    getString(R.string.profile_email_not_verified)
                            }
                        }
                    }
                }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            profileViewModel.isEmailVerificationSent
                .flowWithLifecycle(viewLifecycleOwner.lifecycle, Lifecycle.State.STARTED)
                .collect { isSent ->
                    if (isSent) {
                        Toast.makeText(
                            requireContext(),
                            getString(R.string.profile_email_verification_send),
                            Toast.LENGTH_SHORT
                        )
                            .show()
                    }
                }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            profileViewModel.isEmailVerified
                .flowWithLifecycle(viewLifecycleOwner.lifecycle, Lifecycle.State.STARTED)
                .collect { isVerified ->
                    if (isVerified) {
                        binding?.buttonVerifyEmail?.visibility = View.GONE
                        binding?.textEmailNotVerified?.visibility = View.GONE
//                    Toast.makeText(
//                        requireContext(),
//                        "Email verified successfully",
//                        Toast.LENGTH_SHORT
//                    ).show()
                    }
                }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            profileViewModel.logoutStateFlow
                .flowWithLifecycle(viewLifecycleOwner.lifecycle, Lifecycle.State.STARTED)
                .collect { resourceResult ->
                    val messageToShow = resourceResult.data.toErrorMessage(resources)

                    when (resourceResult.status) {
                        Status.SUCCESS -> {
                            // Вышли из аккаунта, провряем состояние user и обновляем UI
                            mainViewModel.checkLoggedInStateGetUser()

                            // Очистка данных (ВКЛЮЧАЯ сброс онбординга)
                            mainViewModel.clearUserDataOnLogout()

                            // И возвращаемся на WelcomeFragment
                            safeNavigate(
                                R.id.navigation_profile,
                                R.id.action_navigation_profile_to_navigation_welcome2
                            )
                        }

                        Status.ERROR -> {
                            Toast.makeText(activity, messageToShow, Toast.LENGTH_LONG).show()
                        }

                        else -> {
                            Log.d(TAG, "resource.status = ${resourceResult.status} ")
                        }
                    }
                }
        }

        mainViewModel.profileUpdatedLiveData.observe(viewLifecycleOwner) { resourceResult ->
            val messageToShow = resourceResult.data.toErrorMessage(resources)

            when (resourceResult.status) {
                Status.SUCCESS -> {
                    Log.d(TAG, "Profile updated")
                    mainViewModel.checkLoggedInStateGetUser() // To automatically update our image view and our edit text
//                    Toast.makeText(
//                        requireContext(),
//                        R.string.profile_toast_data_updated,
//                        Toast.LENGTH_SHORT
//                    ).show()
                    // Закрываем диалог если открыт
                    dataSaveErrorDialog?.dismiss()
                    dataSaveErrorDialog = null
                }

                Status.ERROR -> {
                    Log.d(TAG, "Error while saving data: $messageToShow")
                    // Показываем диалог только если это первая ошибка
                    if (dataSaveErrorDialog == null) {
                        showDataSaveErrorDialog(messageToShow)
                    }
                }

                else -> {
                    Log.d(TAG, "resource.status = ${resourceResult.status} ")
                }
            }
        }

        // Новая подписка на состояние повторного сохранения
        mainViewModel.retrySaveState.observe(viewLifecycleOwner) { resourceResult ->
            val messageToShow = resourceResult.data.toErrorMessage(resources)

            when (resourceResult.status) {
                Status.LOADING -> {
                    // Показываем индикатор загрузки в диалоге
                    dataSaveErrorDialog?.showLoading()
                }

                Status.SUCCESS -> {
                    Log.d(TAG, "Retry save successful")
                    Toast.makeText(
                        requireContext(),
                        R.string.profile_toast_data_updated,
                        Toast.LENGTH_SHORT
                    ).show()
                    // Закрываем диалог если открыт
                    dataSaveErrorDialog?.dismiss()
                    dataSaveErrorDialog = null
                }

                Status.ERROR -> {
                    Log.d(TAG, "Retry save failed: $messageToShow")
                    // Показываем ошибку в том же диалоге
                    dataSaveErrorDialog?.showError(messageToShow)
                }

                else -> {
                    // ignore
                }
            }
        }

//        mainViewModel.displayEmailUpdateState.observe(viewLifecycleOwner) { resourceResult ->
//            val messageToShow = resourceResult.data.toErrorMessage(resources)
//
//            when (resourceResult.status) {
//                Status.LOADING -> {
//                    // Прогресс уже показывается в диалоге через showLoading()
//                }
//
//                Status.SUCCESS -> {
//                    this.changeDisplayEmailDialog?.showSuccess()
//                    this.changeDisplayEmailDialog = null
//
//                    Toast.makeText(
//                        requireContext(),
//                        getString(R.string.display_email_updated_successfully),
//                        Toast.LENGTH_SHORT
//                    ).show()
//                    // Данные автоматически обновятся через checkedUserInfo observer
//                }
//
//                Status.ERROR -> {
//                    // Показываем ошибку от сервера в диалоге
//                    this.changeDisplayEmailDialog?.showError(
//                        messageToShow ?: getString(R.string.unexpected_error)
//                    )
//                    // Не обнуляем dialog, чтобы пользователь мог попробовать снова
//                }
//
//                else -> {
//                    // ignore
//                }
//            }
//        }

//        mainViewModel.authEmailUpdateState.observe(viewLifecycleOwner) { resourceResult ->
//            val message = resourceResult.data.toErrorMessage(resources)
//
//            when (resourceResult.status) {
//                Status.LOADING -> {
//                    // Прогресс уже показывается в диалоге через showLoading()
//                }
//
//                Status.SUCCESS -> {
//                    changeAuthEmailDialog?.showSuccess()
//                    changeAuthEmailDialog = null
//
//                    Toast.makeText(
//                        requireContext(),
//                        getString(R.string.auth_email_updated_successfully),
//                        Toast.LENGTH_SHORT
//                    ).show()
//
//                    // Показываем сообщение о необходимости верификации
//                    Toast.makeText(
//                        requireContext(),
//                        getString(R.string.profile_email_verification_send),
//                        Toast.LENGTH_LONG
//                    ).show()
//
//                    // Принудительно обновляем данные пользователя
//                    mainViewModel.checkLoggedInStateGetUser()
//                }
//
//                Status.ERROR -> {
////                    changeAuthEmailDialog?.showError(
////                        message ?: getString(R.string.unexpected_error)
////                    )
//                    // Определяем, это ошибка пароля или email
//                    val isPasswordError = resourceResult.message?.contains("password") ?: false
//
//                    changeAuthEmailDialog?.showError(
//                        message ?: getString(R.string.unexpected_error),
//                        isPasswordError
//                    )
//                }
//
//                else -> {
//                    // ignore
//                }
//            }
//        }

//        mainViewModel.authEmailUpdateState.observe(viewLifecycleOwner) { resourceResult ->
//            val message = resourceResult.data.toErrorMessage(resources)
//
//            // Детальное логирование
//            Log.d(
//                "AuthEmailUpdate",
//                "Status: ${resourceResult.status}, Message: $message, ErrorType: ${resourceResult.data}, RawMessage: ${resourceResult.message}"
//            )
//
//            when (resourceResult.status) {
//                Status.LOADING -> {
//                    // Прогресс уже показывается в диалоге через showLoading()
//                }
//
//                Status.SUCCESS -> {
//                    changeAuthEmailDialog?.showSuccess()
//                    changeAuthEmailDialog = null
//
//                    Toast.makeText(
//                        requireContext(),
//                        getString(R.string.auth_email_updated_successfully),
//                        Toast.LENGTH_SHORT
//                    ).show()
//
//                    // Показываем сообщение о необходимости верификации
//                    Toast.makeText(
//                        requireContext(),
//                        getString(R.string.profile_email_verification_send),
//                        Toast.LENGTH_LONG
//                    ).show()
//
//                    // Принудительно обновляем данные пользователя
//                    mainViewModel.checkLoggedInStateGetUser()
//                }
//
//                Status.ERROR -> {
//                    // Улучшенная логика определения типа ошибки
////                    val isPasswordError = when (resourceResult.data) {
////                        ErrorType.WRONG_PASSWORD_WHILE_CHANGING_EMAIL_ERROR,
////                        ErrorType.AUTH_DATA_ERROR -> true
////                        else -> false
////                    }
//                    val isPasswordError = resourceResult.message?.contains("password") ?: false
//
//                    Log.d(
//                        "AuthEmailUpdate",
//                        "IsPasswordError: $isPasswordError, ErrorType: ${resourceResult.data}"
//                    )
//
//                    changeAuthEmailDialog?.showError(
//                        message ?: getString(R.string.unexpected_error),
//                        isPasswordError
//                    )
//                }
//
//                else -> {}
//            }
//        }

//        mainViewModel.authEmailUpdateState.observe(viewLifecycleOwner) { resourceResult ->
//            val message = resourceResult.data.toErrorMessage(resources)
//
//            Log.d(
//                "AuthEmailUpdate",
//                "Status: ${resourceResult.status}, Message: $message, ErrorType: ${resourceResult.data}, RawMessage: ${resourceResult.message}"
//            )
//
//            when (resourceResult.status) {
//                Status.LOADING -> {
//                    // Прогресс уже показывается в диалоге через showLoading()
//                }
//
//                Status.SUCCESS -> {
//                    changeAuthEmailDialog?.showSuccess()
//                    changeAuthEmailDialog = null
//
//                    Toast.makeText(
//                        requireContext(),
//                        getString(R.string.auth_email_updated_successfully_please_verify),
//                        Toast.LENGTH_SHORT
//                    ).show()
//
//                    // ОБНОВЛЕННОЕ сообщение - объясняем процесс
//                    Toast.makeText(
//                        requireContext(),
//                        getString(R.string.verification_email_sent_explanation),
//                        Toast.LENGTH_LONG
//                    ).show()
//
//                    mainViewModel.checkLoggedInStateGetUser()
//                }
//
//                Status.ERROR -> {
//                    val isPasswordError = resourceResult.message?.contains("password") ?: false
//
//                    // Если это ошибка верификации, это не ошибка пароля
//                    val isVerificationNeeded =
//                        resourceResult.data == ErrorType.EMAIL_VERIFICATION_NEEDED
//
//                    Log.d(
//                        "AuthEmailUpdate",
//                        "IsPasswordError: $isPasswordError, ErrorType: ${resourceResult.data}"
//                    )
//
//                    changeAuthEmailDialog?.showError(
//                        message ?: getString(R.string.unexpected_error),
//                        isPasswordError && !isVerificationNeeded // Не показываем как ошибку пароля если нужна верификация
//                    )
//
//                    // Если требуется верификация, закрываем диалог с задержкой чтобы пользователь успел прочитать
//                    if (isVerificationNeeded) {
//                        Handler(Looper.getMainLooper()).postDelayed({
//                            changeAuthEmailDialog?.showSuccess()
//                        }, 3000) // 3 секунды на чтение сообщения
//                    }
//                }
//
//                else -> {}
//            }
//        }

//        mainViewModel.authEmailUpdateState.observe(viewLifecycleOwner) { resourceResult ->
//            val message = resourceResult.data.toErrorMessage(resources)
//
//            Log.d("AuthEmailUpdate", "Status: ${resourceResult.status}, Message: $message, ErrorType: ${resourceResult.data}, RawMessage: ${resourceResult.message}")
//
//            when (resourceResult.status) {
//                Status.LOADING -> {
//                    // Прогресс уже показывается в диалоге через showLoading()
//                }
//                Status.SUCCESS -> {
//                    changeAuthEmailDialog?.showSuccess()
//                    changeAuthEmailDialog = null
//
//                    Toast.makeText(
//                        requireContext(),
//                        getString(R.string.auth_email_updated_successfully),
//                        Toast.LENGTH_SHORT
//                    ).show()
//
//                    Toast.makeText(
//                        requireContext(),
//                        getString(R.string.verification_email_sent_explanation),
//                        Toast.LENGTH_LONG
//                    ).show()
//
//                    mainViewModel.checkLoggedInStateGetUser()
//                }
//                Status.ERROR -> {
//                    val isPasswordError = resourceResult.message?.contains("password") ?: false
//
//                    val isVerificationNeeded = resourceResult.data == ErrorType.EMAIL_VERIFICATION_NEEDED
//                    val isCurrentEmailNotVerified = resourceResult.data == ErrorType.CURRENT_EMAIL_NOT_VERIFIED
//
//                    Log.d("AuthEmailUpdate", "IsPasswordError: $isPasswordError, ErrorType: ${resourceResult.data}")
//
//                    // Для ошибки непроверенного текущего email показываем особое поведение
//                    if (isCurrentEmailNotVerified) {
//                        changeAuthEmailDialog?.showError(
//                            message ?: getString(R.string.unexpected_error),
//                            false // Не показываем как ошибку пароля
//                        )
//
//                        // Закрываем диалог через 3 секунды чтобы пользователь успел прочитать
//                        Handler(Looper.getMainLooper()).postDelayed({
//                            changeAuthEmailDialog?.dismiss()
//                            // Показываем кнопку верификации текущего email
//                            binding?.buttonVerifyEmail?.visibility = View.VISIBLE
//                        }, 3000)
//                    } else {
//                        changeAuthEmailDialog?.showError(
//                            message ?: getString(R.string.unexpected_error),
//                            isPasswordError && !isVerificationNeeded
//                        )
//
//                        if (isVerificationNeeded) {
//                            Handler(Looper.getMainLooper()).postDelayed({
//                                changeAuthEmailDialog?.showSuccess()
//                            }, 3000)
//                        }
//                    }
//                }
//                else -> {}
//            }
//        }

//        mainViewModel.authEmailUpdateState.observe(viewLifecycleOwner) { resourceResult ->
//            val message = resourceResult.data.toErrorMessage(resources)
//
//            Log.d(
//                "AuthEmailUpdate",
//                "Status: ${resourceResult.status}, Message: $message, ErrorType: ${resourceResult.data}, RawMessage: ${resourceResult.message}"
//            )
//
//            when (resourceResult.status) {
//                Status.LOADING -> {
//                    // Прогресс уже показывается в диалоге через showLoading()
//                }
//
//                Status.SUCCESS -> {
//                    changeAuthEmailDialog?.showSuccess()
//                    changeAuthEmailDialog = null
//
//                    Toast.makeText(
//                        requireContext(),
//                        getString(R.string.auth_email_updated_successfully),
//                        Toast.LENGTH_SHORT
//                    ).show()
//
//                    Toast.makeText(
//                        requireContext(),
//                        getString(R.string.verification_email_sent_explanation),
//                        Toast.LENGTH_LONG
//                    ).show()
//
//                    mainViewModel.checkLoggedInStateGetUser()
//                }
//
//                Status.ERROR -> {
//                    // Улучшенная логика определения типа ошибки
//                    val errorType = resourceResult.data
//                    val isPasswordError = resourceResult.message?.contains("password") ?: false
//
//                    val isCurrentEmailNotVerified =
//                        errorType == ErrorType.CURRENT_EMAIL_NOT_VERIFIED
//                    val isVerificationNeeded = errorType == ErrorType.EMAIL_VERIFICATION_NEEDED
//
//                    Log.d(
//                        "AuthEmailUpdate",
//                        "ErrorType: $errorType, IsPasswordError: $isPasswordError, IsCurrentEmailNotVerified: $isCurrentEmailNotVerified"
//                    )
//
//                    when {
//                        isPasswordError -> {
//                            // Ошибка пароля - показываем в диалоге и НЕ закрываем его
//                            changeAuthEmailDialog?.showError(
//                                message ?: getString(R.string.unexpected_error),
//                                true // Показываем как ошибку пароля
//                            )
//                        }
//
//                        isCurrentEmailNotVerified -> {
//                            // Ошибка непроверенного текущего email - показываем и закрываем диалог
//                            changeAuthEmailDialog?.showError(
//                                message ?: getString(R.string.unexpected_error),
//                                false
//                            )
//
//                            // Закрываем диалог через 4 секунды чтобы пользователь успел прочитать
//                            Handler(Looper.getMainLooper()).postDelayed({
//                                changeAuthEmailDialog?.dismiss()
//                                // Показываем кнопку верификации текущего email
//                                binding?.buttonVerifyEmail?.visibility = View.VISIBLE
//                                // Показываем дополнительное объяснение
//                                Toast.makeText(
//                                    requireContext(),
//                                    "Please verify your current email first, then try again",
//                                    Toast.LENGTH_LONG
//                                ).show()
//                            }, 4000)
//                        }
//
//                        isVerificationNeeded -> {
//                            // Нужна верификация нового email - это успешный случай
//                            changeAuthEmailDialog?.showError(
//                                message ?: getString(R.string.unexpected_error),
//                                false
//                            )
//
//                            Handler(Looper.getMainLooper()).postDelayed({
//                                changeAuthEmailDialog?.showSuccess()
//                            }, 3000)
//                        }
//
//                        else -> {
//                            // Любая другая ошибка
//                            changeAuthEmailDialog?.showError(
//                                message ?: getString(R.string.unexpected_error),
//                                false
//                            )
//                        }
//                    }
//                }
//
//                else -> {}
//            }
//        }

        mainViewModel.authEmailUpdateState.observe(viewLifecycleOwner) { resourceResult ->
            val message = resourceResult.data.toErrorMessage(resources)

            Log.d(
                "AuthEmailUpdate",
                "Status: ${resourceResult.status}, Message: $message, ErrorType: ${resourceResult.data}"
            )

            when (resourceResult.status) {
                Status.LOADING -> {
                    // Прогресс уже показывается
                }

                Status.SUCCESS -> {
                    changeAuthEmailDialog?.showSuccess()
                    changeAuthEmailDialog = null

                    Toast.makeText(
                        requireContext(),
                        getString(R.string.auth_email_updated_successfully),
                        Toast.LENGTH_SHORT
                    ).show()

                    Toast.makeText(
                        requireContext(),
                        getString(R.string.verification_email_sent_explanation),
                        Toast.LENGTH_LONG
                    ).show()

                    mainViewModel.checkLoggedInStateGetUser()
                }

                Status.ERROR -> {
                    val errorType = resourceResult.data
                    val isPasswordError = resourceResult.message?.contains("password") ?: false

                    val isCurrentEmailNotVerified =
                        errorType == ErrorType.CURRENT_EMAIL_NOT_VERIFIED

                    Log.d(
                        "AuthEmailUpdate",
                        "ErrorType: $errorType, IsPasswordError: $isPasswordError"
                    )

                    if (isCurrentEmailNotVerified) {
                        // Для ошибки верификации показываем специальное сообщение с действиями
                        changeAuthEmailDialog?.showVerificationError(
                            message ?: "Email not verified"
                        ) { action ->
                            when (action) {
                                "send_verification" -> {
                                    // Отправляем верификацию на текущий email
                                    profileViewModel.sendVerificationEmail()
                                    changeAuthEmailDialog?.dismiss()
                                    Toast.makeText(
                                        requireContext(),
                                        "Verification email sent to your current email address",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }

                                "cancel" -> {
                                    changeAuthEmailDialog?.dismiss()
                                }
                            }
                        }
                    } else {
                        // Для других ошибок используем стандартный showError
                        changeAuthEmailDialog?.showError(
                            message ?: getString(R.string.unexpected_error),
                            isPasswordError
                        )
                    }
                }

                else -> {}
            }
        }
    }


    private fun removeLocalObservers() {
        // Удаляем наблюдатель локальных полей
        localFieldsObserver?.let { observer ->
            mainViewModel.onboardingAnswersLiveData.removeObserver(observer)
            // Удалить остальные наблюдатели, если есть
        }
        localFieldsObserver = null
    }

    private fun setupLocalObservers() {
        removeLocalObservers()

        localFieldsObserver = Observer<Any> {
            fillFieldsInProfile(
                mainViewModel.onboardingAnswersLiveData.createUserFromLocalDataInFragment(
                    activity
                )
            )
            showLogOutButtons(false)
        }

        // Подписываемся на все необходимые LiveData
        mainViewModel.onboardingAnswersLiveData.observe(viewLifecycleOwner, localFieldsObserver!!)
    }

    private fun checkCurrentData() {
        // Проверяем, есть ли уже данные в LiveData
        val currentUser = mainViewModel.checkedUserInfo.value?.data
        if (currentUser != null) {
            fillFieldsInProfile(currentUser)
            showLogOutButtons(true)
        } else {
            // Если данных нет, создаем из локальных LiveData
            val localUser =
                mainViewModel.onboardingAnswersLiveData.createUserFromLocalDataInFragment(activity)
            if (localUser.isUserDataValid()) {
                fillFieldsInProfile(localUser)
            }
            showLogOutButtons(false)
        }
    }

    private fun fillFieldsInProfile(user: UserPresentation) {
        // Проверяем, чтобы userName не был равен "null"
        if (user.userName != "null") {
            binding?.textUserNameAnswer?.text = user.userName
            binding?.textGenderAnswer?.text = user.childGender
            binding?.textGenderAnswerIsItABoy?.text = user.isItABoy
            binding?.textChildNameAnswer?.text = user.childName
            binding?.textHobbyAnswer?.text = user.childHobby
            // ВЫЧИСЛЯЕМ возраст из даты рождения
            val age = calculateAgeFromBirthDate(user.childBirthDate)
            binding?.textChildAgeAnswer?.text = if (age >= 0) age.toString() else ""
            // ФОРМАТИРУЕМ дату для красивого отображения
            binding?.textBirthDateAnswer?.text = profileViewModel.formatBirthDateForDisplay(
                user.childBirthDate,
                getString(R.string.localization)
            )
//            // Email для профиля (изменяемый)
//            binding?.textDisplayEmailAnswer?.text = user.displayEmail
            // Email для входа (НЕ изменяемый)
            binding?.textAuthEmailAnswer?.text = user.authEmail

//            // Показываем/скрываем почту и кнопку верификации в зависимости от статуса email
//            if (user.authEmail.isEmpty() || user.authEmail.isBlank() || !user.isEmailVerified) {
//                binding?.textAuthEmailTitle?.visibility = View.VISIBLE
//                binding?.linearAuthEmailAnswer?.visibility = View.VISIBLE
//                binding?.linearEmailVerification?.visibility = View.VISIBLE
////                binding?.buttonVerifyEmail?.visibility = View.VISIBLE
////                binding?.textEmailNotVerified?.visibility = View.VISIBLE
//            } else {
//                binding?.textAuthEmailTitle?.visibility = View.GONE
//                binding?.linearAuthEmailAnswer?.visibility = View.GONE
//                binding?.linearEmailVerification?.visibility = View.GONE
////                binding?.buttonVerifyEmail?.visibility = View.GONE
////                binding?.textEmailNotVerified?.visibility = View.GONE
//            }
        }
    }

    private fun calculateAgeFromBirthDate(birthDateString: String): Int {
        if (birthDateString.isBlank()) return 0
        var birthDate: Date? = null

        try {
            // Парсим из формата хранения (yyyy-MM-dd)
            val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            birthDate = formatter.parse(birthDateString)
        } catch (e: ParseException) {
            Log.d(TAG, "Ошибка форматирования даты ${e.printStackTrace()}")
        }

        val resultAge = birthDate?.calculateAge()

        return when (resultAge?.status) {
            Status.SUCCESS -> {
                val age: Int = resultAge.data ?: 0
                age
            }

            Status.ERROR -> {
                0
            } // TODO toast?
            else -> {
                0
            } // nothing
        }
    }

    private fun showLogOutButtons(isUserLoggedIn: Boolean) {
        if (isUserLoggedIn) {
            // Убираем кнопки для авторизации и регистрации, text_warning, text_warning_data
            binding?.buttonLogIn?.visibility = View.GONE
            binding?.buttonRegisterAndLogin?.visibility = View.GONE
            binding?.textWarning?.visibility = View.GONE
            binding?.textWarningData?.visibility = View.GONE
            // Показываем кнопки "log out" и "обновить поля", button_verify_email
            binding?.buttonLogOut?.visibility = View.VISIBLE
//            binding?.buttonUpdateFields?.visibility = View.VISIBLE
            // Показываем поля с почтой
            binding?.textAuthEmailTitle?.visibility = View.VISIBLE
            binding?.linearAuthEmailAnswer?.visibility = View.VISIBLE
            binding?.linearEmailVerification?.visibility = View.VISIBLE
        } else {
            // Показываем кнопки для авторизации и регистрации, text_warning, text_warning_data
            binding?.buttonLogIn?.visibility = View.VISIBLE
            binding?.buttonRegisterAndLogin?.visibility = View.VISIBLE
            binding?.textWarning?.visibility = View.VISIBLE
            binding?.textWarningData?.visibility = View.VISIBLE
            // Убираем кнопки "log out" и "обновить поля", button_verify_email
            binding?.buttonLogOut?.visibility = View.GONE
//            binding?.buttonUpdateFields?.visibility = View.GONE
            // Показываем поля с почтой
            binding?.textAuthEmailTitle?.visibility = View.GONE
            binding?.linearAuthEmailAnswer?.visibility = View.GONE
            binding?.linearEmailVerification?.visibility = View.GONE
        }
    }

//    private fun showChangeDisplayEmailDialog() {
//        val currentEmail = binding?.textDisplayEmailAnswer?.text?.toString() ?: ""
//
//        this.changeDisplayEmailDialog = ChangeDisplayEmailDialog.newInstance(currentEmail)
//        this.changeDisplayEmailDialog
//            ?.setOnSaveClickListener { newEmail ->
////            changeDisplayEmailDialog?.showLoading()
//                mainViewModel.updateDisplayEmail(newEmail)
//            }
//            ?.show(parentFragmentManager, ChangeDisplayEmailDialog.TAG)
//    }

    private fun showChangeAuthEmailDialog() {
        val currentEmail = binding?.textAuthEmailAnswer?.text?.toString() ?: ""

        changeAuthEmailDialog = ChangeAuthEmailDialog.newInstance(currentEmail)
        changeAuthEmailDialog
            ?.setOnSaveClickListener { newEmail, password ->
//            changeAuthEmailDialog?.showLoading()
                mainViewModel.updateAuthEmail(newEmail, password)
            }
            ?.show(parentFragmentManager, ChangeAuthEmailDialog.TAG)
    }

    private fun showConfirmOnboardingRestartDialog() {
        ConfirmOnboardingRestartDialog.newInstance()
            .setListeners(
                positiveAction = {
                    // Пользователь подтвердил - запускаем заново онбординг, проходим его и в конце сохраняем результаты на сервер
                    safeNavigate(R.id.navigation_profile, R.id.navigation_question1)
                },
                negativeAction = {
                    // Пользователь отменил - ничего не делаем
                }
            )
            .show(parentFragmentManager, ConfirmOnboardingRestartDialog.TAG)
    }

    private fun showDataSaveErrorDialog(errorMessage: String? = null) {
        dataSaveErrorDialog = DataSaveErrorDialog.newInstance()
            .setListeners(
                retryAction = {
                    // Пользователь хочет повторить сохранение
                    mainViewModel.retryDataSave()
                },
                cancelAction = {
                    // Пользователь отменил
                    Toast.makeText(
                        requireContext(),
                        R.string.profile_toast_data_updating_error,
                        Toast.LENGTH_LONG
                    ).show()
                    dataSaveErrorDialog = null
                }
            )

        // Показываем диалог с первоначальным сообщением об ошибке
        dataSaveErrorDialog?.show(parentFragmentManager, DataSaveErrorDialog.TAG)

        // Устанавливаем кастомное сообщение если есть
        if (!errorMessage.isNullOrEmpty()) {
            // Ждем пока диалог создастся и затем обновляем сообщение
            dataSaveErrorDialog?.dialog?.setOnShowListener {
                dataSaveErrorDialog?.showError(errorMessage)
            }
        }
    }

    override fun onResume() {
        super.onResume()

        // При возвращении на фрагмент проверяем данные
        checkCurrentData()

        // При возвращении на экран проверяем статус верификации
        profileViewModel.checkEmailVerificationStatus()
    }

    // VIEW BINDING -> 3. onDestroyView()
    override fun onDestroyView() {
//        changeDisplayEmailDialog?.dismiss()
//        changeDisplayEmailDialog = null
        changeAuthEmailDialog?.dismiss()
        changeAuthEmailDialog = null
        binding = null
        super.onDestroyView()
    }
}