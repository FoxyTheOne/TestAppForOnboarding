package com.foxynest.testappforonboarding.presentation.welcome.auth.login

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
import com.foxynest.testappforonboarding.databinding.FragmentLogInBinding
import com.foxynest.testappforonboarding.other.constants.CommonConstants.PROFILE_DESTINATION
import com.foxynest.testappforonboarding.presentation.welcome.BaseWelcomeFragmentAbstract
import com.foxynest.testappforonboarding.presentation.welcome.auth.registration.RegistrationFormState
import com.foxynest.testappforonboarding.presentation.welcome.auth.registration.RegistrationViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class LoginFragment : BaseWelcomeFragmentAbstract() {
    companion object {
        private const val TAG = "LoginFragment"
    }

    // VIEW BINDING -> 1. Объявляем переменную. This property is only valid between onCreateView and onDestroyView
    private var binding: FragmentLogInBinding? = null

    private val loginViewModel by viewModels<LoginViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // VIEW BINDING -> 2. Инициализация
        binding = FragmentLogInBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initListeners()
        observeViewModel()
    }

    private fun initListeners() {
        binding?.textFieldMail?.editText?.addTextChangedListener {
            loginViewModel.onEvent(LoginFormEvent.EmailChanged(it.toString()))
        }

        binding?.textFieldPassword?.editText?.addTextChangedListener {
            loginViewModel.onEvent(LoginFormEvent.PasswordChanged(it.toString()))
        }

        binding?.buttonLogin?.setOnClickListener{
            loginViewModel.onEvent(LoginFormEvent.Submit)
        }

        binding?.buttonBack?.setOnClickListener {
            val argument = arguments?.getString("previous_destination")

            if (argument == PROFILE_DESTINATION) {

                if (findNavController().currentDestination?.id == R.id.navigation_login) {
                    findNavController().navigate(R.id.action_loginFragment_to_navigation_profile)
                }

            } else {

                if (findNavController().currentDestination?.id == R.id.navigation_login) {
                    findNavController().navigate(R.id.action_loginFragment_to_welcomeFragment)
                }

            }
        }
    }

    private fun observeViewModel() {
        // Наблюдение за состоянием формы (ошибки)
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                loginViewModel.state.collect { state ->
                    updateErrorStates(state)
                }
            }
        }

        // Наблюдаем за событиями валидации
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                loginViewModel.checkRegistrationEvents.collect { event ->
                    when (event) {
                        // Успешное заполнение
                        is LoginViewModel.CheckRegistrationEvent.SuccessCheckRegistration -> {
                            showLoginSuccess()
                        }
                    }
                }
            }
        }
    }

    private fun updateErrorStates(state: LoginFormState) {
        // Обновление ошибок для полей
        binding?.textFieldMail?.error = state.emailError
        binding?.textFieldPassword?.error = state.passwordError
    }

    private fun showLoginSuccess() {
        // TODO Действия после успешного входа
        // TODO String resources instead of text
        Toast.makeText(
            requireContext(),
            "Account exists",
            Toast.LENGTH_LONG
        ).show()
    }

    // VIEW BINDING -> 3. onDestroyView()
    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }
}