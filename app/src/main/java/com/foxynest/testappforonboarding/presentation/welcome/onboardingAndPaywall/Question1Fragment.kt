package com.foxynest.testappforonboarding.presentation.welcome.onboardingAndPaywall

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.core.widget.doAfterTextChanged
import com.foxynest.testappforonboarding.R
import com.foxynest.testappforonboarding.databinding.FragmentQuestion1Binding
import com.foxynest.testappforonboarding.domain.entities.ErrorType
import com.foxynest.testappforonboarding.domain.entities.Gender
import com.foxynest.testappforonboarding.domain.entities.PersonNameRules
import com.foxynest.testappforonboarding.other.extensions.safeNavigate
import com.foxynest.testappforonboarding.presentation.common.applyPersonNameRules
import com.foxynest.testappforonboarding.presentation.common.toErrorMessage
import com.google.android.material.textfield.TextInputLayout
import dagger.hilt.android.AndroidEntryPoint

/**
 * Вопрос 1: имя родителя, имя ребёнка, пол. Здесь же начинается опрос – создаётся черновик (prepareForOnboarding).
 * Мальчик → вопрос 2, девочка → сразу вопрос 3.
 */
@AndroidEntryPoint
class Question1Fragment : BaseOnboardingFragment() {
    companion object {
        private const val TAG = "Question1Fragment"
    }

    // VIEW BINDING -> 1. Объявляем переменную. This property is only valid between onCreateView and onDestroyView
    private var binding: FragmentQuestion1Binding? = null

    override val currentFragmentId: Int = R.id.navigation_question1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Только при первом открытии экрана. Раньше подготовка запускалась в onViewCreated и повторялась
        // при пересоздании Activity (смена языка, тёмной темы), перетирая бэкап уже пустыми данными
        if (savedInstanceState == null) {
            // Подготавливаем данные для онбординга
            // Теперь готовимся всегда, когда заходим на первый фрагмент онбординга, независимо от предыдущего состояния
            mainViewModel.prepareForOnboarding()
        }
    }

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

        // Какие символы можно вводить и максимальная длина – правила из domain (PersonNameRules)
        binding?.editTextFieldUserNameAnswer?.applyPersonNameRules()
        binding?.editTextFieldChildNameAnswer?.applyPersonNameRules()

        binding?.buttonNext?.setOnClickListener {
            val checkedRadioButtonId = binding?.radioGroupQuestion1?.checkedRadioButtonId
            val radioGenderAnswerBoy = binding?.genderAnswerBoy
            val radioGenderAnswerGirl = binding?.genderAnswerGirl

            // Проверяем все поля сразу (а не до первой ошибки), чтобы подсветить каждое неправильное
            val userNameAnswer = validateName(
                binding?.textFieldUserNameAnswer,
                R.string.error_user_name_not_selected
            )
            val childNameAnswer = validateName(
                binding?.textFieldChildNameAnswer,
                R.string.error_child_name_not_selected
            )
            val isGenderSelected = validateGender(radioGenderAnswerBoy, radioGenderAnswerGirl)

            if (userNameAnswer != null && childNameAnswer != null && isGenderSelected) {
                val selectedGender = when (checkedRadioButtonId) {
                    R.id.gender_answer_boy -> Gender.BOY.name
                    R.id.gender_answer_girl -> Gender.GIRL.name
                    else -> null
                }

                mainViewModel.updateOnboardingAnswers { answers ->
                    answers.copy(
                        userName = userNameAnswer,
                        childGender = selectedGender ?: Gender.BOY.name,
                        childName = childNameAnswer,
                        // Ответ «мальчик ли это» имеет смысл только для мальчика – не оставляем старое значение
                        isItABoy = if (selectedGender == Gender.GIRL.name) "" else answers.isItABoy
                    )
                }

                when {
                    radioGenderAnswerBoy?.isChecked == true -> safeNavigate(
                        R.id.navigation_question1,
                        R.id.action_navigation_question1_to_question2Fragment,
                        sourceArgs()
                    )

                    radioGenderAnswerGirl?.isChecked == true -> safeNavigate(
                        R.id.navigation_question1,
                        R.id.action_navigation_question1_to_navigation_question3,
                        sourceArgs()
                    )
                }
            }
        }

        // Пока человек печатает, ошибку только убираем, а не показываем новую:
        // иначе «Анна-» посреди набора «Анна-Мария» сразу подсвечивалось бы красным
        binding?.editTextFieldUserNameAnswer?.doAfterTextChanged {
            binding?.textFieldUserNameAnswer?.error = null
        }
        binding?.editTextFieldChildNameAnswer?.doAfterTextChanged {
            binding?.textFieldChildNameAnswer?.error = null
        }

    }

    /**
     * Проверяет имя по правилам из domain и показывает ошибку под полем.
     * @return нормализованное имя («  Анна   Мария » → «Анна Мария») или null, если имя не подходит
     */
    private fun validateName(field: TextInputLayout?, @StringRes emptyError: Int): String? {
        val name = PersonNameRules.normalize(field?.editText?.text?.toString().orEmpty())
        val error = PersonNameRules.validate(name)
        field?.error = when (error) {
            null -> null
            ErrorType.FIELD_EMPTY_ERROR -> getString(emptyError)
            else -> error.toErrorMessage(resources)
        }
        return if (error == null) name else null
    }

    private fun validateGender(boy: RadioButton?, girl: RadioButton?): Boolean {
        val isSelected = boy?.isChecked == true || girl?.isChecked == true
        if (!isSelected) {
            Toast.makeText(
                context,
                getString(R.string.error_gender_not_selected),
                Toast.LENGTH_SHORT
            ).show()
        }
        return isSelected
    }

    // VIEW BINDING -> 3. onDestroyView()
    override fun onDestroyView() {
        // cancelOnboarding() уже вызывается при нажатии кнопки отмены через диалог и не должен вызываться повторно в onDestroyView()
        binding = null
        super.onDestroyView()
    }
}