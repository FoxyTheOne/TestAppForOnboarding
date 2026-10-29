package com.foxynest.testappforonboarding.presentation.welcome.onboardingAndPaywall

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.foxynest.testappforonboarding.R
import com.foxynest.testappforonboarding.databinding.FragmentQuestion2Binding
import com.foxynest.testappforonboarding.domain.entities.onboarding.YesNoAnswer
import com.foxynest.testappforonboarding.other.extensions.safeNavigate
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class Question2Fragment : BaseOnboardingFragment() {

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

        initListeners()
    }

    private fun initListeners() {
        binding?.buttonYes?.setOnClickListener { saveAnswerAndGoFurther(YesNoAnswer.YES) }
        binding?.buttonNo?.setOnClickListener { saveAnswerAndGoFurther(YesNoAnswer.NO) }

        // Добавляем обработчик кнопки отмены
        binding?.buttonCancel?.setOnClickListener {
            showCancelConfirmationDialog()
        }
    }

    private fun saveAnswerAndGoFurther(answer: YesNoAnswer) {
        mainViewModel.updateIsItABoy(answer.name)
        safeNavigate(
            R.id.navigation_question2,
            R.id.action_navigation_question2_to_navigation_question3,
            sourceArgs()
        )
    }

    // VIEW BINDING -> 3. onDestroyView()
    override fun onDestroyView() {
        binding = null
        super.onDestroyView()
    }
}