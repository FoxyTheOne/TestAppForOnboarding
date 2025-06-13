package com.foxynest.testappforonboarding.presentation.onboardingAndPaywall

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.foxynest.testappforonboarding.R
import com.foxynest.testappforonboarding.databinding.FragmentQuestion3Binding
import com.foxynest.testappforonboarding.domain.entities.AnswerNames
import com.foxynest.testappforonboarding.domain.entities.Gender
import com.foxynest.testappforonboarding.presentation.MainViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class Question3Fragment : BaseOnboardingFragmentAbstract() {
    companion object {
        private const val TAG = "Question3Fragment"
    }

    // VIEW BINDING -> 1. Объявляем переменную. This property is only valid between onCreateView and onDestroyView
    private var binding: FragmentQuestion3Binding? = null

    // 1.1. ViewModel. We bind our viewModel to the cycle of our activity, not fragment. So, we need to do this way:
    private lateinit var mainViewModel: MainViewModel
//    private val onboardingViewModel by viewModels<OnboardingViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // VIEW BINDING -> 2. Инициализация
        binding = FragmentQuestion3Binding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1.2. ViewModel. We bind our viewModel to the lifecycle of our activity, not fragment. We pass our activity as an owner of the lifecycle.
        // So, we need to do this way:
        mainViewModel = ViewModelProvider(requireActivity())[MainViewModel::class.java]

        initListeners()
    }

    private fun initListeners() {
        binding?.buttonFinish?.setOnClickListener {
            val textFieldCharHobbyAnswer = binding?.textFieldHobbyAnswer?.editText?.text

            if (!textFieldCharHobbyAnswer.isNullOrBlank()) {
                mainViewModel.saveHobbyAnswer(textFieldCharHobbyAnswer.toString())

                if (this.findNavController().currentDestination?.id == R.id.navigation_question3) {
                    this.findNavController()
                        .navigate(R.id.action_navigation_question3_to_paywallFragment)
                }
            } else {
                Toast.makeText(context, "Пожалуйста, заполните поле", Toast.LENGTH_SHORT)
                    .show()
            }
        }

        binding?.buttonBack?.setOnClickListener {
//            onboardingViewModel.cleanFragment3AnswersBeforeBackButton()
            mainViewModel.cleanFragment3AnswersBeforeBackButton()

            val savedGenderName = mainViewModel.answers[AnswerNames.GENDER_ANSWER.toString()]

            if (this.findNavController().currentDestination?.id == R.id.navigation_question3) {
                if (savedGenderName == Gender.BOY.genderName) {
                    this.findNavController()
                        .navigate(R.id.action_navigation_question3_to_navigation_question2)
                }

                if (savedGenderName == Gender.GIRL.genderName) {
                    this.findNavController()
                        .navigate(R.id.action_navigation_question3_to_navigation_question1)
                }

                Log.d(TAG, savedGenderName ?: "Не сохранено")
            }


        }
    }

    // VIEW BINDING -> 3. onDestroyView()
    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }
}