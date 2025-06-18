package com.foxynest.testappforonboarding.presentation.welcome.auth.registration

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.foxynest.testappforonboarding.R
import com.foxynest.testappforonboarding.databinding.FragmentRegisterBinding
import com.foxynest.testappforonboarding.presentation.welcome.BaseWelcomeFragmentAbstract
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class RegistrationFragment : BaseWelcomeFragmentAbstract() {
    companion object {
        private const val TAG = "RegisterFragment"
    }

    // VIEW BINDING -> 1. Объявляем переменную. This property is only valid between onCreateView and onDestroyView
    private var binding: FragmentRegisterBinding? = null

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

        initListeners()
        observeViewModel()
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

        // Наблюдаем за событиями валидации
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                registrationViewModel.validationEvents.collect { event ->
                    when (event) {
                        // Успешное заполнение
                        is RegistrationViewModel.ValidationEvent.Success -> {
                            showRegistrationSuccess()
                        }
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

    private fun showRegistrationSuccess() {
        // Действия после успешной регистрации
        // TODO String resources instead of text
        Toast.makeText(
            requireContext(),
            "Registration successful",
            Toast.LENGTH_LONG
        ).show()
    }

    // VIEW BINDING -> 3. onDestroyView()
    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }
}