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

        // ОЧИСТКА ОТВЕТОВ, которые не перезапишутся, перед запуском онбординга, чтобы не было конфликтов в собранных данных
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
            val userNameAnswer =
                binding?.textFieldUserNameAnswer?.editText?.text?.toString()?.trim()
//            val emailAnswer =
//                binding?.textFieldUserEmailAnswer?.editText?.text?.toString()?.trim()?.lowercase()
            val checkedRadioButtonId = binding?.radioGroupQuestion1?.checkedRadioButtonId
            val radioGenderAnswerBoy = binding?.genderAnswerBoy
            val radioGenderAnswerGirl = binding?.genderAnswerGirl
            val childNameAnswer =
                binding?.textFieldChildNameAnswer?.editText?.text?.toString()?.trim()

            val areFieldsValid = validateAllFields(
                userNameAnswer,
//                emailAnswer,
                checkedRadioButtonId,
                radioGenderAnswerBoy,
                radioGenderAnswerGirl,
                childNameAnswer
            )

            if (areFieldsValid) {
                // Все проверки, в том числе "checkedRadioButtonId == null" должны были быть в areFieldsValid()
                mainViewModel.saveUserNameAnswer(userNameAnswer ?: "")
//                mainViewModel.saveEmailAnswer(emailAnswer ?: "")
                checkedRadioButtonId?.let {
                    val selectedRadioButton: RadioButton? = binding?.root?.findViewById(it)
                    mainViewModel.saveGenderAnswer(selectedRadioButton?.text.toString())
                }
                mainViewModel.saveChildNameAnswer(childNameAnswer ?: "")

                if (this.findNavController().currentDestination?.id == R.id.navigation_question1) {
                    when {
                        radioGenderAnswerBoy?.isChecked == true -> this.findNavController()
                            .navigate(R.id.action_navigation_question1_to_question2Fragment)

                        radioGenderAnswerGirl?.isChecked == true -> this.findNavController()
                            .navigate(R.id.action_navigation_question1_to_navigation_question3)
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

//        binding?.editTextFieldUserEmailAnswer?.addTextChangedListener(object : TextWatcher {
//            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
//            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
//            override fun afterTextChanged(s: Editable?) {
//                s?.let { editable ->
//                    val text = editable.toString()
//                    validateEmail(text)
//                }
//            }
//        })

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

    private fun validateUserName(userNameAnswer: String?): Boolean {
        var isValid = true
        if (userNameAnswer.isNullOrBlank()) {
            binding?.textFieldUserNameAnswer?.error = getString(R.string.error_user_name_not_selected)
            isValid = false
        } else {
            binding?.textFieldUserNameAnswer?.error = null
        }
        return isValid
    }

//    private fun validateEmail(emailAnswer: String?): Boolean {
//        var isValid = true
//        if (emailAnswer.isNullOrBlank()) {
//            binding?.textFieldUserEmailAnswer?.error = getString(R.string.error_email_not_selected)
//            isValid = false
//        } else if (!EmailUtils.emailMatches(emailAnswer)) {
//            binding?.textFieldUserEmailAnswer?.error = getString(R.string.email_format_error)
//            isValid = false
//        } else {
//            binding?.textFieldUserEmailAnswer?.error = null
//        }
//        return isValid
//    }

    private fun validateChildName(childNameAnswer: String?): Boolean {
        var isValid = true
        if (childNameAnswer.isNullOrBlank()) {
            binding?.textFieldChildNameAnswer?.error = getString(R.string.error_child_name_not_selected)
            isValid = false
        } else {
            binding?.textFieldChildNameAnswer?.error = null
        }
        return isValid
    }

    private fun validateAllFields(
        userNameAnswer: String?,
//        emailAnswer: String?,
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

//        if (!validateEmail(emailAnswer)) {
//            isValid = false
//        }

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
        super.onDestroyView()
        binding = null
    }
}