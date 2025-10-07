package com.foxynest.testappforonboarding.presentation.welcome.onboardingAndPaywall

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.foxynest.testappforonboarding.R
import com.foxynest.testappforonboarding.databinding.FragmentQuestion1Binding
import com.foxynest.testappforonboarding.presentation.MainViewModel
import com.foxynest.testappforonboarding.presentation.welcome.BaseWelcomeFragmentAbstract
import dagger.hilt.android.AndroidEntryPoint


@AndroidEntryPoint
class Question1Fragment : BaseWelcomeFragmentAbstract() {
    companion object {
        private const val TAG = "Question1Fragment"
    }

    // VIEW BINDING -> 1. Объявляем переменную. This property is only valid between onCreateView and onDestroyView
    private var binding: FragmentQuestion1Binding? = null

    // 1.1. ViewModel. We bind our viewModel to the cycle of our activity, not fragment. So, we need to do this way:
    private lateinit var mainViewModel: MainViewModel
//    private val onboardingViewModel by viewModels<OnboardingViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // VIEW BINDING -> 2. Инициализация
        binding = FragmentQuestion1Binding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1.2. ViewModel. We bind our viewModel to the lifecycle of our activity, not fragment. We pass our activity as an owner of the lifecycle.
        // So, we need to do this way:
        mainViewModel = ViewModelProvider(requireActivity())[MainViewModel::class.java]

        // !!! Перед разветвлением нам нужно удалить те ответы, которые могут не перезаписаться
        // ОЧИСТКА ОТВЕТОВ первая, важна мне сейчас при тестировании, чтобы каждый запуск был как первый. Потом думаю можно удалить (вторая - при переходе на предыдущие фрагменты, используя кнопку назад)
        // P.S. А нужно ли удалять? Я вызываю заново опрос, если человек хочет изменить свои ответы
//        onboardingViewModel.cleanNotReplaceableAnswers()
        mainViewModel.cleanNotReplaceableAnswers()

        initListeners()
    }

    private fun initListeners() {
        binding?.genderAnswerBoy?.setOnCheckedChangeListener { buttonView, isChecked ->
            Log.d(TAG, "answer1 is checked: $isChecked")
        }

        binding?.genderAnswerGirl?.setOnCheckedChangeListener { buttonView, isChecked ->
            Log.d(TAG, "answer2 is checked: $isChecked")
        }

        binding?.buttonNext?.setOnClickListener {
            val radioGenderAnswerBoy = binding?.genderAnswerBoy
            val radioGenderAnswerGirl = binding?.genderAnswerGirl
            val textFieldCharNameAnswer = binding?.textFieldNameAnswer?.editText?.text
            val checkedRadioButtonId = binding?.radioGroupQuestion1?.checkedRadioButtonId

            if (checkedRadioButtonId != null && !textFieldCharNameAnswer.isNullOrBlank()) {
                if (radioGenderAnswerBoy?.isChecked == true || radioGenderAnswerGirl?.isChecked == true) {

                    val selectedRadioButton: RadioButton? =
                        binding?.root?.findViewById(checkedRadioButtonId)

                    mainViewModel.saveGenderAnswer(selectedRadioButton?.text.toString())
                    mainViewModel.saveNameAnswer(textFieldCharNameAnswer.toString())

                    if (this.findNavController().currentDestination?.id == R.id.navigation_question1) {
                        when {
                            radioGenderAnswerBoy?.isChecked == true -> this.findNavController()
                                .navigate(R.id.action_navigation_question1_to_question2Fragment)

                            radioGenderAnswerGirl?.isChecked == true -> this.findNavController()
                                .navigate(R.id.action_navigation_question1_to_navigation_question3)
                        }
                    }

                } else {
                    Toast.makeText(context, "Пожалуйста, выберите пол ребенка", Toast.LENGTH_SHORT)
                        .show()
                }
            } else {
                Toast.makeText(context, "Пожалуйста, заполните все поля", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // VIEW BINDING -> 3. onDestroyView()
    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }
}