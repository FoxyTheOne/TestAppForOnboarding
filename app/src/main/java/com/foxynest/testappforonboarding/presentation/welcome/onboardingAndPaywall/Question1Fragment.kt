package com.foxynest.testappforonboarding.presentation.welcome.onboardingAndPaywall

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import android.widget.Toast
import androidx.navigation.fragment.findNavController
import com.foxynest.testappforonboarding.R
import com.foxynest.testappforonboarding.databinding.FragmentQuestion1Binding
import com.foxynest.testappforonboarding.other.extensions.safeNavigate
import dagger.hilt.android.AndroidEntryPoint


@AndroidEntryPoint
class Question1Fragment : BaseOnboardingFragment() {
    companion object {
        private const val TAG = "Question1Fragment"
    }

    // VIEW BINDING -> 1. Объявляем переменную. This property is only valid between onCreateView and onDestroyView
    private var binding: FragmentQuestion1Binding? = null

    override val currentFragmentId: Int = R.id.navigation_question1

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

        // Добавим логирование для отладки
        Log.d(TAG, "Question1Fragment onViewCreated")
        Log.d(TAG, "isOnboardingStarted: ${mainViewModel.isOnboardingStarted()}")
        Log.d(TAG, "isOnboardingCancelled: ${mainViewModel.isOnboardingCancelled()}")
        Log.d(TAG, "isOnboardingCompleted: ${mainViewModel.isOnboardingCompleted()}")
        Log.d(TAG, "sourceFragment: $sourceFragment")

        // Подготавливаем данные для онбординга
        // Теперь готовимся всегда, когда заходим на первый фрагмент онбординга, независимо от предыдущего состояния
        mainViewModel.prepareForOnboarding()

        initListeners()
    }

    private fun initListeners() {
        binding?.genderAnswerBoy?.setOnCheckedChangeListener { buttonView, isChecked ->
            Log.d(TAG, "answer1 is checked: $isChecked")
        }

        binding?.genderAnswerGirl?.setOnCheckedChangeListener { buttonView, isChecked ->
            Log.d(TAG, "answer2 is checked: $isChecked")
        }

        binding?.buttonCancel?.setOnClickListener {
            showCancelConfirmationDialog()
        }

        binding?.buttonNext?.setOnClickListener {
            val userNameAnswer =
                binding?.textFieldUserNameAnswer?.editText?.text?.toString()?.trim() ?: ""
            val checkedRadioButtonId = binding?.radioGroupQuestion1?.checkedRadioButtonId
            val radioGenderAnswerBoy = binding?.genderAnswerBoy
            val radioGenderAnswerGirl = binding?.genderAnswerGirl
            val childNameAnswer =
                binding?.textFieldChildNameAnswer?.editText?.text?.toString()?.trim() ?: ""

            val areFieldsValid = validateAllFields(
                userNameAnswer,
                checkedRadioButtonId,
                radioGenderAnswerBoy,
                radioGenderAnswerGirl,
                childNameAnswer
            )

            if (areFieldsValid) {
                // Если checkedRadioButtonId будет null, то валидация не будет успешна и сюда не зайдет. Но для метода findViewById() это нужно указать явно
                checkedRadioButtonId?.let {
                    val selectedRadioButton: RadioButton? = binding?.root?.findViewById(it)

                    // Используем единый метод обновления
                    mainViewModel.updateOnboardingAnswers { answers ->
                        answers.copy(
                            userName = userNameAnswer,
                            childGender = selectedRadioButton?.text.toString(),
                            childName = childNameAnswer
                        )
                    }

                    when {
                        radioGenderAnswerBoy?.isChecked == true -> {
                            // Передаем аргумент при навигации на Question2Fragment
                            val args = Bundle().apply {
                                putString("source_fragment", sourceFragment)
                            }
                            safeNavigate(
                                R.id.navigation_question1,
                                R.id.action_navigation_question1_to_question2Fragment,
                                args
                            )
                        }

                        radioGenderAnswerGirl?.isChecked == true -> {
                            // Передаем аргумент при навигации на Question3Fragment
                            val args = Bundle().apply {
                                putString("source_fragment", sourceFragment)
                            }
                            safeNavigate(
                                R.id.navigation_question1,
                                R.id.action_navigation_question1_to_navigation_question3,
                                args
                            )
                        }
                    }
                }

            }
        }

        binding?.editTextFieldUserNameAnswer?.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                s?.let { editable ->
                    val text = editable.toString()
                    validateUserName(text)
                }
            }
        })

        binding?.editTextFieldChildNameAnswer?.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                s?.let { editable ->
                    val text = editable.toString()
                    validateChildName(text)
                }
            }
        })
    }

    private fun hasNavigatedToNext(): Boolean {
        // Проверяем, был ли навигация к следующему фрагменту
        return findNavController().currentDestination?.id in
                listOf(
                    R.id.navigation_question2,
                    R.id.navigation_question3,
                    R.id.navigation_paywall
                )
    }

    private fun validateUserName(userNameAnswer: String?): Boolean {
        var isValid = true
        if (userNameAnswer.isNullOrBlank()) {
            binding?.textFieldUserNameAnswer?.error =
                getString(R.string.error_user_name_not_selected)
            isValid = false
        } else {
            binding?.textFieldUserNameAnswer?.error = null
        }
        return isValid
    }

    private fun validateChildName(childNameAnswer: String?): Boolean {
        var isValid = true
        if (childNameAnswer.isNullOrBlank()) {
            binding?.textFieldChildNameAnswer?.error =
                getString(R.string.error_child_name_not_selected)
            isValid = false
        } else {
            binding?.textFieldChildNameAnswer?.error = null
        }
        return isValid
    }

    private fun validateAllFields(
        userNameAnswer: String?,
        checkedRadioButtonId: Int?,
        radioGenderAnswerBoy: RadioButton?,
        radioGenderAnswerGirl: RadioButton?,
        childNameAnswer: String?
    ): Boolean {
        var isValid = true

        // Каждая функция в отдельном if для того, чтобы проверилась каждая из функций и если нужно, подписало причину error под текстовым полем
        if (!validateUserName(userNameAnswer)) {
            isValid = false
        }

        // Проверка RadioButton
        if (checkedRadioButtonId == null) {
            Toast.makeText(
                context,
                getString(R.string.error_gender_not_selected),
                Toast.LENGTH_SHORT
            )
                .show()
            isValid = false
        }
        if (radioGenderAnswerBoy?.isChecked == false && radioGenderAnswerGirl?.isChecked == false) {
            Toast.makeText(
                context,
                getString(R.string.error_gender_not_selected),
                Toast.LENGTH_SHORT
            )
                .show()
            isValid = false
        }

        if (!validateChildName(childNameAnswer)) {
            isValid = false
        }

        return isValid
    }

    // VIEW BINDING -> 3. onDestroyView()
    override fun onDestroyView() {
        // cancelOnboarding() уже вызывается при нажатии кнопки отмены через диалог и не должен вызываться повторно в onDestroyView()
        binding = null
        super.onDestroyView()
    }
}