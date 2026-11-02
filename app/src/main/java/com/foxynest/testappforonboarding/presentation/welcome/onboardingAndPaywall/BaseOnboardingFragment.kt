package com.foxynest.testappforonboarding.presentation.welcome.onboardingAndPaywall

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.foxynest.testappforonboarding.R
import com.foxynest.testappforonboarding.other.extensions.safeNavigate
import com.foxynest.testappforonboarding.presentation.MainViewModel
import com.foxynest.testappforonboarding.presentation.dialogs.simpleDialogs.CancelOnboardingDialog

/**
 * Базовый фрагмент для расширения фрагментами с онбордингом.
 */
abstract class BaseOnboardingFragment : Fragment() {
    companion object {
        const val SOURCE_FRAGMENT_KEY = "source_fragment"
    }

    // ViewModel привязана к Activity: общая для всех экранов опроса
    protected val mainViewModel: MainViewModel by activityViewModels()
    protected abstract val currentFragmentId: Int

    // Добавляем переменную для хранения source_fragment
    protected val sourceFragment: String?
        get() = arguments?.getString(SOURCE_FRAGMENT_KEY)

    protected fun sourceArgs(): Bundle = Bundle().apply {
        putString(SOURCE_FRAGMENT_KEY, sourceFragment)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Результат диалога приходит и после пересоздания экрана (Fragment Result API)
        CancelOnboardingDialog.setResultListener(
            childFragmentManager,
            viewLifecycleOwner
        ) { confirmed ->
            if (confirmed) {
                mainViewModel.cancelOnboarding()
                navigateBackToPrevious()
            }
        }
    }

    // Общий метод для отображения диалога подтверждения отмены
    protected fun showCancelConfirmationDialog() {
        if (childFragmentManager.findFragmentByTag(CancelOnboardingDialog.TAG) == null) {
            CancelOnboardingDialog().show(childFragmentManager, CancelOnboardingDialog.TAG)
        }
    }

    // Общий метод для навигации назад
    private fun navigateBackToPrevious() {
        when (sourceFragment) {
            "welcome" -> safeNavigate(currentFragmentId, R.id.action_global_navigation_welcome)
            else -> safeNavigate(currentFragmentId, R.id.action_global_navigation_profile)
        }
    }
}