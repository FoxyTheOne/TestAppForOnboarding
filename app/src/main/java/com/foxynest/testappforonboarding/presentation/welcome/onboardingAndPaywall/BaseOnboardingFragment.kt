package com.foxynest.testappforonboarding.presentation.welcome.onboardingAndPaywall

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.foxynest.testappforonboarding.R
import com.foxynest.testappforonboarding.other.extensions.safeNavigate
import com.foxynest.testappforonboarding.presentation.MainViewModel
import com.foxynest.testappforonboarding.presentation.dialogs.simpleDialogs.CancelOnboardingDialog
import dagger.hilt.android.AndroidEntryPoint

/**
 * Базовый фрагмент для расширения фрагментами с онбордингом.
 */
@AndroidEntryPoint
abstract class BaseOnboardingFragment : Fragment() {
    companion object {
        private const val TAG = "BaseOnboardingFragmentAbstract"
    }

    // 1.1. ViewModel. We bind our viewModel to the cycle of our activity, not fragment. So, we need to do this way:
    protected lateinit var mainViewModel: MainViewModel
    protected lateinit var onboardingViewModel: OnboardingViewModel
    protected var isCancelled = false
    protected abstract val currentFragmentId: Int

    // Добавляем переменную для хранения source_fragment
    protected var sourceFragment: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Получаем аргументы в onCreate
        arguments?.let { bundle ->
            sourceFragment = bundle.getString("source_fragment")
            Log.d(TAG, "Received source_fragment: $sourceFragment")
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1.2. ViewModel. We bind our viewModel to the lifecycle of our activity, not fragment. We pass our activity as an owner of the lifecycle.
        // So, we need to do this way:
        mainViewModel = ViewModelProvider(requireActivity())[MainViewModel::class.java]
        onboardingViewModel = ViewModelProvider(requireActivity())[OnboardingViewModel::class.java]
    }

    // Общий метод для отображения диалога подтверждения отмены
    protected fun showCancelConfirmationDialog() {
        CancelOnboardingDialog.newInstance()
            .setListeners(
                positiveAction = {
                    // Подтверждение отмены
                    isCancelled = true
                    mainViewModel.cancelOnboarding()

                    // Возвращаемся на предыдущий фрагмент
                    navigateBackToPrevious()
                },
                negativeAction = {
                    // Пользователь передумал - ничего не делаем
                }
            )
            .show(parentFragmentManager, "CancelOnboardingDialog")
    }

    // Общий метод для навигации назад
    protected fun navigateBackToPrevious() {
        Log.d("BaseOnboardingFragment", "navigateBackToPrevious called")
        Log.d("BaseOnboardingFragment", "currentFragmentId: $currentFragmentId")
        Log.d(
            "BaseOnboardingFragment",
            "source_fragment: ${arguments?.getString("source_fragment")}"
        )

        // Проверяем, откуда пришли в онбординг
        val previousDestination = arguments?.getString("source_fragment")

        // Используем правильный currentId для safeNavigate
        val currentId = currentFragmentId

        Log.d("BaseOnboardingFragment", "Navigating from $currentId to $previousDestination")

        when (previousDestination) {
            "profile" -> {
                // Возвращаемся в профиль
                safeNavigate(currentId, R.id.action_global_navigation_profile)
            }

            "welcome" -> {
                // Возвращаемся на Welcome
                safeNavigate(currentId, R.id.action_global_navigation_welcome)
            }

            else -> {
                // По умолчанию возвращаемся в профиль
                safeNavigate(currentId, R.id.action_global_navigation_profile)
            }
        }
    }
}