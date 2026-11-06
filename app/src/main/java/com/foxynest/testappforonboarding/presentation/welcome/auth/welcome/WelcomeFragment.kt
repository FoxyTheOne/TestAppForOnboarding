package com.foxynest.testappforonboarding.presentation.welcome.auth.welcome

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import com.foxynest.testappforonboarding.R
import com.foxynest.testappforonboarding.databinding.FragmentWelcomeBinding
import com.foxynest.testappforonboarding.other.extensions.safeNavigate
import com.foxynest.testappforonboarding.presentation.welcome.BaseWelcomeFragmentAbstract
import dagger.hilt.android.AndroidEntryPoint

/** Приветствие при первом запуске: «Пройти опрос» или «Войти» (если аккаунт уже есть). */
@AndroidEntryPoint
class WelcomeFragment : BaseWelcomeFragmentAbstract() {
    companion object {
        private const val TAG = "AuthFragment"
    }

    // VIEW BINDING -> 1. Объявляем переменную. This property is only valid between onCreateView and onDestroyView
    private var _binding: FragmentWelcomeBinding? = null

    // Делаем binding internal для тестов в том же модуле
    internal val binding: FragmentWelcomeBinding?
        get() = _binding

//    private val registrationViewModel by viewModels<RegistrationViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // VIEW BINDING -> 2. Инициализация
        _binding = FragmentWelcomeBinding.inflate(inflater, container, false)
        return _binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initListeners()
    }

    private fun initListeners() {
        _binding?.buttonStartOnboarding?.setOnClickListener {
//            // Сбрасываем состояние онбординга перед запуском
//            mainViewModel.resetOnboardingState()

            // Создаем Bundle с аргументом
            val args = Bundle().apply {
                putString("source_fragment", "welcome")
            }

            // Используем safeNavigate с аргументами
            safeNavigate(
                R.id.navigation_welcome,
                R.id.action_welcomeFragment_to_navigation_question1,
                args
            )
        }

        _binding?.buttonStartLogin?.setOnClickListener {
            if (findNavController().currentDestination?.id == R.id.navigation_welcome) {
                findNavController().navigate(R.id.action_welcomeFragment_to_loginFragment)
            }
        }
    }

    // VIEW BINDING -> 3. onDestroyView()
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}