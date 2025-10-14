package com.foxynest.testappforonboarding.presentation.welcome.auth.registration

import android.os.Build.VERSION.SDK_INT
import android.os.Bundle
import android.os.Parcelable
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.foxynest.testappforonboarding.R
import com.foxynest.testappforonboarding.databinding.FragmentRegisterBinding
import com.foxynest.testappforonboarding.domain.entities.UserPresentation
import com.foxynest.testappforonboarding.other.Status
import com.foxynest.testappforonboarding.other.extensions.toErrorMessage
import com.foxynest.testappforonboarding.presentation.MainViewModel
import com.foxynest.testappforonboarding.presentation.welcome.BaseWelcomeFragmentAbstract
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

@AndroidEntryPoint
class RegistrationFragment : BaseWelcomeFragmentAbstract() {
    companion object {
        private const val TAG = "RegisterFragment"
    }

    // VIEW BINDING -> 1. Объявляем переменную. This property is only valid between onCreateView and onDestroyView
    private var binding: FragmentRegisterBinding? = null

    // 1.1. ViewModel. We bind our viewModel to the cycle of our activity, not fragment. So, we need to do this way:
    private lateinit var mainViewModel: MainViewModel
    private val registrationViewModel by viewModels<RegistrationViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // VIEW BINDING -> 2. Инициализация
        binding = FragmentRegisterBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        var user = UserPresentation(
            uid = "",
            email = "",
            isEmailVerified = false,
            userName = "",
            childGender = "",
            isItABoy = "",
            childName = "",
            childHobby = "",
            childAge = 0,
            childBirthDate = ""
        )
        if (arguments != null) {
            arguments?.parcelable<UserPresentation>("user_presentation")
                ?.let { user = it }
        }
        registrationViewModel.setUser(user)

        // 1.2. ViewModel. We bind our viewModel to the lifecycle of our activity, not fragment. We pass our activity as an owner of the lifecycle.
        // So, we need to do this way:
        mainViewModel = ViewModelProvider(requireActivity())[MainViewModel::class.java]

        initListeners()
        observeViewModel()
    }

    // Bundle
    private inline fun <reified T : Parcelable> Bundle.parcelable(key: String): T? = when {
        SDK_INT >= 33 -> getParcelable(key, T::class.java)
        else -> @Suppress("DEPRECATION") getParcelable(key) as? T
    }

    private fun initListeners() {
        binding?.textFieldMail?.editText?.addTextChangedListener {
            // Когда меняется текст, отправляем его в метод ViewModel, где сделается копия и запишется в mutableStateOf(RegistrationFormState()) для последующего submit(), где мы уже будем проверять правильность этих данных
            registrationViewModel.onEvent(RegistrationFormEvent.EmailChanged(it.toString()))
        }

        binding?.textFieldPassword?.editText?.addTextChangedListener {
            registrationViewModel.onEvent(RegistrationFormEvent.PasswordChanged(it.toString()))
        }

        binding?.textFieldPassword2?.editText?.addTextChangedListener {
            registrationViewModel.onEvent(RegistrationFormEvent.RepeatedPasswordChanged(it.toString()))
        }

        binding?.buttonRegisterAndLogin?.setOnClickListener {
            registrationViewModel.onEvent(RegistrationFormEvent.Submit)
        }

        binding?.buttonBack?.setOnClickListener {
            if (findNavController().currentDestination?.id == R.id.navigation_register) {
                findNavController().navigate(R.id.action_registerFragment_to_navigation_profile)
            }
        }
    }

    private fun observeViewModel() {
        // Наблюдение за состоянием формы (ошибки)
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                registrationViewModel.state.collect { state ->
                    updateErrorStates(state)
                }
            }
        }

//        // Наблюдаем за событиями валидации
//        viewLifecycleOwner.lifecycleScope.launch {
//            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
//                registrationViewModel.validationEvents.collect { event ->
//                    when (event) {
//                        // Успешное заполнение
//                        is RegistrationViewModel.ValidationEvent.SuccessValidation -> {
//                            showRegistrationSuccess()
//                        }
//                    }
//                }
//            }
//        }

        // Наблюдаем за событиями регистрации
        viewLifecycleOwner.lifecycleScope.launch {
            registrationViewModel.registrationStateFlow.onEach { resource ->
                when (resource.status) {
                    Status.SUCCESS -> {
                        // Вызываем сообщение
                        Toast.makeText(
                            activity,
                            getString(R.string.registered_text),
                            Toast.LENGTH_LONG
                        ).show()

                        // Обновляем состояние user в MainViewModel
                        mainViewModel.checkLoggedInStateGetUser()

                        // Переходим в профиль
                        if (findNavController().currentDestination?.id == R.id.navigation_register) {
                            findNavController().navigate(R.id.action_registerFragment_to_navigation_profile)
                        }
                    }

                    Status.ERROR -> {
                        // Неудачная регистрация - например, такой email уже зарегистрирован
                        val toastMessage = resource.data.toErrorMessage(resources)
//                        Toast.makeText(activity, resource.message, Toast.LENGTH_LONG).show()
                        Toast.makeText(activity, toastMessage, Toast.LENGTH_LONG).show()
                    }

                    else -> {
                        Log.d(TAG, "resource.status = ${resource.status} ")
                    }
                }
            }
        }
    }

    private fun updateErrorStates(state: RegistrationFormState) {
        // Обновление ошибок для полей
        binding?.textFieldMail?.error = state.emailError
        binding?.textFieldPassword?.error = state.passwordError
        binding?.textFieldPassword2?.error = state.repeatedPasswordError
    }

//    private fun showRegistrationSuccess() {
//        // Действия после успешной регистрации
//        Toast.makeText(
//            requireContext(),
//            "Registration successful",
//            Toast.LENGTH_LONG
//        ).show()
//    }

    // VIEW BINDING -> 3. onDestroyView()
    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }
}