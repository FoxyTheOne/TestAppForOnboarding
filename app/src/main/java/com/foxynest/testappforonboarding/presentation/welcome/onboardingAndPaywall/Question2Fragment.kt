package com.foxynest.testappforonboarding.presentation.welcome.onboardingAndPaywall

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.foxynest.testappforonboarding.R
import com.foxynest.testappforonboarding.databinding.FragmentQuestion2Binding
import com.foxynest.testappforonboarding.other.extensions.safeNavigate
import com.foxynest.testappforonboarding.presentation.MainViewModel
import com.foxynest.testappforonboarding.presentation.welcome.BaseWelcomeFragmentAbstract
import com.foxynest.testappforonboarding.presentation.welcome.onboardingAndPaywall.Question1Fragment.Companion
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class Question2Fragment : BaseOnboardingFragment() {
    companion object {
        private const val TAG = "Question2Fragment"
    }

    // VIEW BINDING -> 1. Объявляем переменную. This property is only valid between onCreateView and onDestroyView
    private var binding: FragmentQuestion2Binding? = null

    override val currentFragmentId: Int = R.id.navigation_question2

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // VIEW BINDING -> 2. Инициализация
        binding = FragmentQuestion2Binding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Добавим логирование для отладки
        Log.d(TAG, "Question2Fragment onViewCreated")
        Log.d(TAG, "isOnboardingStarted: ${mainViewModel.isOnboardingStarted()}")
        Log.d(TAG, "isOnboardingCancelled: ${mainViewModel.isOnboardingCancelled()}")
        Log.d(TAG, "isOnboardingCompleted: ${mainViewModel.isOnboardingCompleted()}")
        Log.d(TAG, "sourceFragment: $sourceFragment")

        initListeners()
    }

    private fun initListeners() {
        binding?.buttonYes?.setOnClickListener {
            saveAnswerAndGoFurther(binding?.buttonYes?.text.toString())
        }

        binding?.buttonNo?.setOnClickListener {
            saveAnswerAndGoFurther(binding?.buttonNo?.text.toString())
        }

        // Добавляем обработчик кнопки отмены
        binding?.buttonCancel?.setOnClickListener {
            showCancelConfirmationDialog()
        }
    }

    private fun saveAnswerAndGoFurther(answer: String) {
        mainViewModel.updateOnboardingAnswers { it.copy(isItABoy = answer) }
//        mainViewModel.saveIsItABoyAnswer(answer)

        // Передаем аргумент при навигации на Question3Fragment
        val args = Bundle().apply {
            putString("source_fragment", sourceFragment)
        }
        safeNavigate(R.id.navigation_question2,
            R.id.action_navigation_question2_to_navigation_question3,
            args)
    }

    private fun hasNavigatedToNext(): Boolean {
        return findNavController().currentDestination?.id in
                listOf(
                    R.id.navigation_question3,
                    R.id.navigation_paywall
                )
    }

    // VIEW BINDING -> 3. onDestroyView()
    override fun onDestroyView() {
        // Если фрагмент уничтожается без перехода к следующему вопросу,
        // отменяем онбординг
//        if (!isCancelled && !hasNavigatedToNext() && !mainViewModel.isOnboardingCompleted()) {
//            mainViewModel.cancelOnboarding()
//        }
        // Убираем лишний вызов cancelOnboarding()
        // cancelOnboarding() уже вызывается при нажатии кнопки отмены через диалог
        // и не должен вызываться повторно в onDestroyView()
        binding = null
        super.onDestroyView()
    }
}