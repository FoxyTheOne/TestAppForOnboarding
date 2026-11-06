package com.foxynest.testappforonboarding.presentation.welcome.auth.login

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
import com.foxynest.testappforonboarding.databinding.FragmentLogInBinding
import com.foxynest.testappforonboarding.other.Status
import com.foxynest.testappforonboarding.other.constants.CommonConstants.PROFILE_DESTINATION
import com.foxynest.testappforonboarding.presentation.MainViewModel
import com.foxynest.testappforonboarding.presentation.common.toErrorMessage
import com.foxynest.testappforonboarding.presentation.welcome.BaseWelcomeFragmentAbstract
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

/**
 * Экран входа: почта + пароль, «Забыли пароль?».
 * Открывается с приветствия или из профиля; аргумент previous_destination говорит, куда возвращаться по «Назад».
 */
@AndroidEntryPoint
class LoginFragment : BaseWelcomeFragmentAbstract() {
    companion object {
        private const val TAG = "LoginFragment"
    }

    // VIEW BINDING -> 1. Объявляем переменную. This property is only valid between onCreateView and onDestroyView
    private var binding: FragmentLogInBinding? = null

    // 1.1. ViewModel. We bind our viewModel to the cycle of our activity, not fragment. So, we need to do this way:
    private lateinit var mainViewModel: MainViewModel
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

        // 1.2. ViewModel. We bind our viewModel to the lifecycle of our activity, not fragment. We pass our activity as an owner of the lifecycle.
        // So, we need to do this way:
        mainViewModel = ViewModelProvider(requireActivity())[MainViewModel::class.java]

        initListeners()
        observeViewModel()
        subscribeOnObservers()
        observePasswordReset()
    }

    private fun initListeners() {
        binding?.textFieldMail?.editText?.addTextChangedListener {
            loginViewModel.onEvent(LoginFormEvent.EmailChanged(it.toString()))
        }

        binding?.textFieldPassword?.editText?.addTextChangedListener {
            loginViewModel.onEvent(LoginFormEvent.PasswordChanged(it.toString()))
        }

        binding?.buttonLogin?.setOnClickListener {
            loginViewModel.onEvent(LoginFormEvent.Submit)
        }

        binding?.buttonForgotPassword?.setOnClickListener {
            loginViewModel.sendPasswordReset()
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
    }

    private fun subscribeOnObservers() {
        // Наблюдаем за событиями входа в аккаунт
        viewLifecycleOwner.lifecycleScope.launch {
            loginViewModel.loginStateFlow.onEach { resource ->
                when (resource.status) {
                    Status.SUCCESS -> {
                        // Вызываем сообщение
                        Toast.makeText(
                            activity,
                            getString(R.string.logged_in_text),
                            Toast.LENGTH_LONG
                        ).show()

                        // Переходим в профиль (данные профиля подтянет листенер в MainViewModel)
                        if (findNavController().currentDestination?.id == R.id.navigation_login) {
                            findNavController().navigate(R.id.action_loginFragment_to_navigation_profile)
                        }
                    }

                    Status.ERROR -> {
                        val messageToShow = resource.data.toErrorMessage(resources)
                        Toast.makeText(activity, messageToShow, Toast.LENGTH_LONG).show()
                    }

                    else -> {
                        Log.d(TAG, "resource.status = ${resource.status} ")
                    }
                }
            }.collect()
        }
    }

    private fun observePasswordReset() {
        loginViewModel.passwordResetEvent.observe(viewLifecycleOwner) { event ->
            val result = event.getContentIfNotHandled() ?: return@observe
            val message = when (result.status) {
                // Нейтральный текст: не раскрываем, есть ли аккаунт с такой почтой
                Status.SUCCESS -> getString(R.string.password_reset_sent)
                else -> result.data.toErrorMessage(resources)
                    ?: getString(R.string.password_reset_failed_error)
            }
            Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
        }
    }

    private fun updateErrorStates(state: LoginFormState) {
        // Обновление ошибок для полей
        binding?.textFieldMail?.error = state.emailError.toErrorMessage(resources)
        binding?.textFieldPassword?.error = state.passwordError.toErrorMessage(resources)
    }

    // VIEW BINDING -> 3. onDestroyView()
    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }
}