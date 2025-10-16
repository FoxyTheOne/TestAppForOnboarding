package com.foxynest.testappforonboarding.presentation.welcome.onboardingAndPaywall

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.foxynest.testappforonboarding.R
import com.foxynest.testappforonboarding.databinding.FragmentQuestion2Binding
import com.foxynest.testappforonboarding.presentation.MainViewModel
import com.foxynest.testappforonboarding.presentation.welcome.BaseWelcomeFragmentAbstract
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class Question2Fragment : BaseWelcomeFragmentAbstract() {
    companion object {
        private const val TAG = "Question2Fragment"
    }

    // VIEW BINDING -> 1. Объявляем переменную. This property is only valid between onCreateView and onDestroyView
    private var binding: FragmentQuestion2Binding? = null

    // 1.1. ViewModel. We bind our viewModel to the cycle of our activity, not fragment. So, we need to do this way:
    private lateinit var mainViewModel: MainViewModel
//    private val onboardingViewModel by viewModels<OnboardingViewModel>()

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

        // 1.2. ViewModel. We bind our viewModel to the lifecycle of our activity, not fragment. We pass our activity as an owner of the lifecycle.
        // So, we need to do this way:
        mainViewModel = ViewModelProvider(requireActivity())[MainViewModel::class.java]

        initListeners()
    }

    private fun initListeners() {
        binding?.buttonYes?.setOnClickListener {
            saveAnswerAndGoFurther(binding?.buttonYes?.text.toString())
        }

        binding?.buttonNo?.setOnClickListener {
            saveAnswerAndGoFurther(binding?.buttonNo?.text.toString())
        }

        binding?.buttonBack?.setOnClickListener {
            mainViewModel.cleanFragment2AnswersBeforeBackButton()

            if (this.findNavController().currentDestination?.id == R.id.navigation_question2) {
                this.findNavController()
                    .navigate(R.id.action_navigation_question2_to_navigation_question1)
            }
        }
    }

    private fun saveAnswerAndGoFurther(answer: String) {
        mainViewModel.saveIsItABoyAnswer(answer)

        if (this.findNavController().currentDestination?.id == R.id.navigation_question2) {
            this.findNavController()
                .navigate(R.id.action_navigation_question2_to_navigation_question3)
        }
    }

    // VIEW BINDING -> 3. onDestroyView()
    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }
}