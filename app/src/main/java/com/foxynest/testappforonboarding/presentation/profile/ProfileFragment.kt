package com.foxynest.testappforonboarding.presentation.profile

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.foxynest.testappforonboarding.R
import com.foxynest.testappforonboarding.databinding.FragmentProfileBinding
import com.foxynest.testappforonboarding.domain.entities.UserPresentation
import com.foxynest.testappforonboarding.other.Status
import com.foxynest.testappforonboarding.other.constants.CommonConstants.PROFILE_DESTINATION
import com.foxynest.testappforonboarding.other.extensions.toErrorMessage
import com.foxynest.testappforonboarding.presentation.MainViewModel
import com.foxynest.testappforonboarding.presentation.dialogs.simpleDialogs.ConfirmOnboardingRestartDialog
import com.foxynest.testappforonboarding.presentation.dialogs.DataSaveErrorDialog
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch


@AndroidEntryPoint
class ProfileFragment : Fragment() {
    companion object {
        private const val TAG = "ProfileFragment"
    }

    private var localFieldsObserver: Observer<Any>? = null
    private var dataSaveErrorDialog: DataSaveErrorDialog? = null

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

//        // Firebase: При инициализации Activity проверьте, вошел ли пользователь в систему
//        mainViewModel.checkLoggedInStateGetUser()

        // ???? На экране профиля под заполненныеми полями кнопки "Сохранить данные на сервер" и "Загрузить данные с сервера (обновить поля)"
        // ???? Но показывать их если поля user != user ? (через || , кроме uid) с подписью "Введенные данные отличаются от сохранённых на сервере, выберите действие:"
        // Не делала потому что при создании фрагмента профиля мы загружаем данные с сервера

        initListeners()
        subscribeOnObservers()
    }

    // Firebase будет держать user зарегистрированным даже после того, как он закроет программу и откроет заново. Поэтому нам нужно переписать метод onStart(), чтобы проверять, is user logged in in firebase or not
    override fun onStart() {
        super.onStart()
        mainViewModel.checkLoggedInStateGetUser()
    }

    private fun initListeners() {
        binding?.textGenderAnswerIsItABoy?.text = "Нет"

        binding?.buttonVerifyEmail?.setOnClickListener {
            profileViewModel.sendVerificationEmail()
        }

        binding?.buttonLogIn?.setOnClickListener {
            val direction = ProfileFragmentDirections.actionNavigationProfileToLoginFragment(
                PROFILE_DESTINATION
            )

            if (findNavController().currentDestination?.id == R.id.navigation_profile) {
                findNavController().navigate(direction)
            }
        }

        binding?.buttonRegisterAndLogin?.setOnClickListener {
            // Открываем по клику другой фрагмент, передаём туда user
            val user = UserPresentation(
                uid = "", // Только для передачи данных для регистрации!
                email = binding?.textEmailAnswer?.text.toString(),
                isEmailVerified = false, // Тут надо быть аккуратным, false - потому что ещё не зарегистрирован. Но на страницу регистрации может перейти и уже зарегистрированный пользователь. Не использовать эти данные для чего-либо, кроме регистрации!
                userName = binding?.textUserNameAnswer?.text.toString(),
                childGender = binding?.textGenderAnswer?.text.toString(),
                isItABoy = binding?.textGenderAnswerIsItABoy?.text.toString(),
                childName = binding?.textChildNameAnswer?.text.toString(),
                childHobby = binding?.textHobbyAnswer?.text.toString(),
                childAge = binding?.textChildAgeAnswer?.text.toString().toInt(),
                childBirthDate = binding?.textBirthDateAnswer?.text.toString()
            )
            val direction =
                ProfileFragmentDirections.actionNavigationProfileToRegisterFragment(user)
            if (findNavController().currentDestination?.id == R.id.navigation_profile) {
                findNavController().navigate(direction)
            }
        }

        binding?.buttonLogOut?.setOnClickListener {
            // 1. Выйти из аккаунта
            profileViewModel.logout()
            // 2. Обновить профиль - обновляется в logoutStateFlow
        }

        binding?.buttonRefillFields?.setOnClickListener {
            showConfirmOnboardingRestartDialog()
        }
    }

    private fun subscribeOnObservers() {
//        mainViewModel.genderLiveData.observe(viewLifecycleOwner) { gender ->
//            binding?.textGenderAnswer?.text = gender
//        }
//        mainViewModel.isItABoyLiveData.observe(viewLifecycleOwner) { isItABoy ->
//            binding?.textGenderAnswerIsItABoy?.text = isItABoy.toString()
//        }
//        mainViewModel.childNameLiveData.observe(viewLifecycleOwner) { name ->
//            binding?.textChildNameAnswer?.text = name
//        }
//        mainViewModel.hobbyLiveData.observe(viewLifecycleOwner) { hobby ->
//            binding?.textHobbyAnswer?.text = hobby
//        }

        // Ответ на разовый запрос, получаем User
//        viewLifecycleOwner.lifecycleScope.launch {
//            mainViewModel.checkedUserInfo.collect { resourceUser ->
//                resourceUser.data?.let { nonNullUser ->
//                    // Заполняем поля профиля исходя из данных на сервере
//                    fillFieldsInProfile(nonNullUser)
//                }
//            }
//        }

        viewLifecycleOwner.lifecycleScope.launch {
            mainViewModel.checkedUserInfo.collect { resourceUser ->
                val user = resourceUser.data
                if (user != null) {
                    // Удаляем наблюдатель локальных полей
                    localFieldsObserver?.let { observer ->
                        mainViewModel.genderLiveData.removeObserver(observer)
                        mainViewModel.isItABoyLiveData.removeObserver(observer)
                        mainViewModel.childNameLiveData.removeObserver(observer)
                        mainViewModel.hobbyLiveData.removeObserver(observer)
                        // !!! Удалить остальные наблюдатели, если есть
                    }
                    localFieldsObserver = null

                    // Заполняем поля профиля исходя из данных на сервере
                    fillFieldsInProfile(user)
                    showLogOutButtons(true)
                } else {
                    // Создаем наблюдатель только если его еще нет
                    if (localFieldsObserver == null) {
                        localFieldsObserver = Observer<Any> {
                            fillFieldsInProfile(
                                UserPresentation(
                                    "",
                                    mainViewModel.emailLiveData.value ?: "",
                                    false,
                                    mainViewModel.userNameLiveData.value ?: "",
                                    mainViewModel.genderLiveData.value ?: "",
                                    mainViewModel.isItABoyLiveData.value ?: "",
                                    mainViewModel.childNameLiveData.value ?: "",
                                    mainViewModel.hobbyLiveData.value ?: "",
                                    mainViewModel.childAgeLiveData.value ?: 0,
                                    mainViewModel.childBirthDateLiveData.value ?: ""
                                )
                            )
                            showLogOutButtons(false)
                        }

                        // Подписываемся на все необходимые LiveData
                        listOf(
                            mainViewModel.genderLiveData,
                            mainViewModel.isItABoyLiveData,
                            mainViewModel.childNameLiveData,
                            mainViewModel.hobbyLiveData
                            // !!! Остальные LiveData, если нужны
                        ).forEach { liveData ->
                            liveData.observe(viewLifecycleOwner, localFieldsObserver!!)
                        }
                    }
                }
            }
        }

        // Подписка на состояние AuthState
        mainViewModel.authStateFlow.onEach { user ->
            if (user != null) {
                // Пользователь авторизован:
                // 1. Обновляем данные профиля в UI
                mainViewModel.checkLoggedInStateGetUser() // Получаем user и обновляем данные в checkedUserInfo LiveData
                // 2. Убираем кнопки для авторизации и регистрации, text_warning, text_warning_data, показываем кнопки "log out" и "обновить поля"
                showLogOutButtons(true)
            } else {
                // Пользователь не авторизован:
                // 1. Показываем данные профиля в UI исходя из пройденного онбординга (data store)
                fillFieldsInProfile(
                    UserPresentation(
                        "",
                        mainViewModel.emailLiveData.value ?: "",
                        false,
                        mainViewModel.userNameLiveData.value ?: "",
                        mainViewModel.genderLiveData.value ?: "",
                        mainViewModel.isItABoyLiveData.value ?: "",
                        mainViewModel.childNameLiveData.value ?: "",
                        mainViewModel.hobbyLiveData.value ?: "",
                        mainViewModel.childAgeLiveData.value ?: 0,
                        mainViewModel.childBirthDateLiveData.value ?: ""
                    )
                )
                // 2. Показываем кнопки для авторизации и регистрации, text_warning, text_warning_data, убираем кнопки "log out" и "обновить поля"
                showLogOutButtons(false)
            }
        }.launchIn(lifecycleScope)

        viewLifecycleOwner.lifecycleScope.launch {
            profileViewModel.isVerificationButtonEnabled.collect { isEnabled ->
                binding?.buttonVerifyEmail?.isEnabled = isEnabled

                context?.let {
                    val colorGray = ContextCompat.getColor(it, R.color.gray)
                    val colorWhite = ContextCompat.getColor(it, R.color.white)

                    when (isEnabled) {
                        true -> binding?.buttonVerifyEmail?.setTextColor(colorGray) ?: "#3D3C3C"
                        false -> binding?.buttonVerifyEmail?.setTextColor(colorWhite) ?: "#FFFFFFFF"
                    }
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            profileViewModel.isEmailVerificationSent.collect { isSent ->
                if (isSent) {
                    Toast.makeText(requireContext(), "Verification email sent", Toast.LENGTH_SHORT)
                        .show()
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            profileViewModel.isEmailVerified.collect { isVerified ->
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
            profileViewModel.logoutStateFlow.collect { resourceResult ->
                val messageToShow = resourceResult.data.toErrorMessage(resources)

                when (resourceResult.status) {
                    Status.SUCCESS -> {
                        // Вышли из аккаунта, провряем состояние user и обновляем UI
                        mainViewModel.checkLoggedInStateGetUser()
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
                    Toast.makeText(requireContext(), R.string.profile_toast_data_updated, Toast.LENGTH_SHORT).show()
                    // Закрываем диалог если открыт
                    dataSaveErrorDialog?.dismiss()
                    dataSaveErrorDialog = null
                }

                Status.ERROR -> {
//                    Log.d(TAG, "Error while saving data: ${resourceResult.message}")
//                    showDataSaveErrorDialog()

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
                    Toast.makeText(requireContext(), R.string.profile_toast_data_updated, Toast.LENGTH_SHORT).show()
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
    }

    private fun fillFieldsInProfile(user: UserPresentation) {
        // Проверяем, чтобы userName не был равен "null"
        if (user.userName != "null") {
            binding?.textUserNameAnswer?.text = user.userName
            binding?.textGenderAnswer?.text = user.childGender
            binding?.textGenderAnswerIsItABoy?.text = user.isItABoy
            binding?.textChildNameAnswer?.text = user.childName
            binding?.textHobbyAnswer?.text = user.childHobby
            binding?.textChildAgeAnswer?.text = user.childAge.toString()
            binding?.textBirthDateAnswer?.text = user.childBirthDate
            binding?.textEmailAnswer?.text = user.email

            // Показываем/скрываем кнопку верификации в зависимости от статуса email
            if (user.email.isEmpty()
                || user.email.isBlank()
                || !user.isEmailVerified
            ) {
                binding?.buttonVerifyEmail?.visibility = View.VISIBLE
                binding?.textEmailNotVerified?.visibility = View.VISIBLE
            } else {
                binding?.buttonVerifyEmail?.visibility = View.GONE
                binding?.textEmailNotVerified?.visibility = View.GONE
            }
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
            binding?.linearEmailVerification?.visibility = View.GONE
        }
    }

    private fun showConfirmOnboardingRestartDialog() {
        ConfirmOnboardingRestartDialog.newInstance()
            .setListeners(
                positiveAction = {
                    // Пользователь подтвердил - запускаем заново онбординг, проходим его и в конце сохраняем результаты на сервер
                    if (findNavController().currentDestination?.id == R.id.navigation_profile) {
                        findNavController().navigate(R.id.navigation_question1)
                    }
                },
                negativeAction = {
                    // Пользователь отменил - ничего не делаем
                }
            )
            .show(parentFragmentManager, ConfirmOnboardingRestartDialog.TAG)
    }

//    private fun showDataSaveErrorDialog() {
//        DataSaveErrorDialog.newInstance()
//            .setListeners(
//                positiveAction = {
//                    // Пользователь хочет повторить сохранение
//                    mainViewModel.retryDataSave()
//                },
//                negativeAction = {
//                    // Пользователь отменил - показываем сообщение
//                    Toast.makeText(
//                        requireContext(),
//                        R.string.profile_toast_data_updating_error,
//                        Toast.LENGTH_LONG
//                    ).show()
//                }
//            )
//            .show(parentFragmentManager, DataSaveErrorDialog.TAG)
//    }

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
        // При возвращении на экран проверяем статус верификации
        profileViewModel.checkEmailVerificationStatus()
    }

    // VIEW BINDING -> 3. onDestroyView()
    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }
}