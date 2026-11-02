package com.foxynest.testappforonboarding.presentation.welcome.auth.registration

import android.os.Bundle
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
import com.foxynest.testappforonboarding.domain.entities.user.toUserPresentation
import com.foxynest.testappforonboarding.other.Status
import com.foxynest.testappforonboarding.presentation.MainViewModel
import com.foxynest.testappforonboarding.presentation.common.toErrorMessage
import com.foxynest.testappforonboarding.presentation.welcome.BaseWelcomeFragmentAbstract
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect
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

        // 1.2. ViewModel. We bind our viewModel to the lifecycle of our activity, not fragment. We pass our activity as an owner of the lifecycle.
        // So, we need to do this way:
        mainViewModel = ViewModelProvider(requireActivity())[MainViewModel::class.java]

        // Анкету берём из общей MainViewModel, а не из аргумента навигации:
        // так UserPresentation в domain не обязан быть Parcelable (зависимость от Android)
        mainViewModel.onboardingAnswersLiveData.observe(viewLifecycleOwner) { answers ->
            registrationViewModel.setUser(answers.toUserPresentation())
        }

        initListeners()
        observeViewModel()
        subscribeOnObservers()
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
    }

    private fun subscribeOnObservers() {
        // Наблюдаем за событиями регистрации
        viewLifecycleOwner.lifecycleScope.launch {
            registrationViewModel.registrationStateFlow.onEach { resource ->
                when (resource.status) {
                    Status.SUCCESS -> {
                        // Вызываем сообщение
                        Toast.makeText(
                            activity,
                            // Письмо для подтверждения почты уже отправлено (мягкая верификация)
                            getString(R.string.registered_verification_sent),
                            Toast.LENGTH_LONG
                        ).show()

                        // Переходим в профиль
                        if (findNavController().currentDestination?.id == R.id.navigation_register) {
                            findNavController().navigate(R.id.action_registerFragment_to_navigation_profile)
                        }
                    }

                    Status.ERROR -> {
                        // Неудачная регистрация - например, такой email уже зарегистрирован
                        val toastMessage = resource.data.toErrorMessage(resources)
                        Toast.makeText(activity, toastMessage, Toast.LENGTH_LONG).show()
                    }

                    else -> {
                        Log.d(TAG, "resource.status = ${resource.status} ")
                    }
                }
            }.collect()
        }
    }

    private fun updateErrorStates(state: RegistrationFormState) {
        // Обновление ошибок для полей
        binding?.textFieldMail?.error = state.emailError.toErrorMessage(resources)
        binding?.textFieldPassword?.error = state.passwordError.toErrorMessage(resources)
        binding?.textFieldPassword2?.error = state.repeatedPasswordError.toErrorMessage(resources)
    }

    // VIEW BINDING -> 3. onDestroyView()
    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }
}