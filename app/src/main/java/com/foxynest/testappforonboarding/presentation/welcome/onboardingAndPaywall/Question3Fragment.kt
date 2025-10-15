package com.foxynest.testappforonboarding.presentation.welcome.onboardingAndPaywall

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.foxynest.testappforonboarding.R
import com.foxynest.testappforonboarding.databinding.FragmentQuestion3Binding
import com.foxynest.testappforonboarding.domain.entities.AnswerNames
import com.foxynest.testappforonboarding.domain.entities.Gender
import com.foxynest.testappforonboarding.presentation.MainViewModel
import com.foxynest.testappforonboarding.presentation.welcome.BaseWelcomeFragmentAbstract
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.MaterialDatePicker
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@AndroidEntryPoint
class Question3Fragment : BaseWelcomeFragmentAbstract() {
    companion object {
        private const val TAG = "Question3Fragment"
    }

    private var selectedDateInMillis: Long? = null

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
        // SETUP DATE INPUT
        // Показываем DatePicker при клике на иконку календаря
        binding?.textFieldBirthDate?.setEndIconOnClickListener {
            showDatePicker()
        }

        // Добавьте обработчик клика на само поле
        binding?.editTextBirthDate?.setOnClickListener {
            showDatePicker()
        }

        // SETUP FIELDS
        binding?.editTextFieldHobbyAnswer?.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                s?.let { editable ->
                    val text = editable.toString()
                    // Валидируем и сразу очищаем ошибку если поле заполнено
                    validateHobby(text)
                }
            }
        })

        // SETUP BUTTONS
        binding?.buttonFinish?.setOnClickListener {
            // Получаем дату в нужном формате для сохранения
            val birtDateText = selectedDateInMillis?.let { millis ->
                convertToSaveFormat(millis)
            }
            val ageText = binding?.textChildAgeValue?.text?.toString()
            val textFieldCharHobbyAnswer =
                binding?.textFieldHobbyAnswer?.editText?.text?.toString()?.trim()


            val areFieldsValid = validateAllFields(birtDateText, ageText, textFieldCharHobbyAnswer)

            if (areFieldsValid) {
                // Все проверки на null должны были быть в areFieldsValid()
                mainViewModel.saveHobbyAnswer(textFieldCharHobbyAnswer ?: "")
                mainViewModel.saveBirthDateAnswer(birtDateText ?: "0")
//                mainViewModel.saveChildAgeAnswer(ageText?.toInt() ?: 0)

                if (this.findNavController().currentDestination?.id == R.id.navigation_question3) {
                    this.findNavController()
                        .navigate(R.id.action_navigation_question3_to_paywallFragment)
                }
            }
        }

        binding?.buttonBack?.setOnClickListener {
            mainViewModel.cleanFragment3AnswersBeforeBackButton()

            val savedGenderName = mainViewModel.answers[AnswerNames.CHILD_GENDER_ANSWER.toString()]

            if (this.findNavController().currentDestination?.id == R.id.navigation_question3) {
                when (savedGenderName) {
                    Gender.BOY.genderName -> {
                        findNavController().navigate(R.id.action_navigation_question3_to_navigation_question2)
                    }

                    Gender.GIRL.genderName -> {
                        findNavController().navigate(R.id.action_navigation_question3_to_navigation_question1)
                    }
                }
                Log.d(TAG, savedGenderName ?: "Не сохранено")
            }
        }
    }

    private fun validateAllFields(
        birthDateText: String?, // в формате MM/dd/yyyy
        ageText: String?,
        textFieldCharHobbyAnswer: String?
    ): Boolean {
        var isValid = true

        // Проверка даты рождения
        if (birthDateText.isNullOrBlank() || selectedDateInMillis == null) {
            binding?.textFieldBirthDate?.error = "Выберите дату рождения"
            isValid = false
        } else {
            binding?.textFieldBirthDate?.error = null
        }

//        // Проверка возраста (должен быть больше 0)
//        if (ageText == "0" || ageText.isNullOrBlank()) {
//            if (isValid) { // Показываем ошибку возраста только если дата валидна
//                binding?.textFieldBirthDate?.error = "Проверьте дату рождения"
//            }
//            isValid = false
//        }

        // Проверка возраста (должен быть больше 0)
        val age = ageText?.toIntOrNull() ?: 0
        if (age <= 0) {
            if (isValid) { // Показываем ошибку возраста только если дата валидна
                binding?.textFieldBirthDate?.error = "Проверьте дату рождения"
            }
            isValid = false
        }

        // Проверка хобби
        if (!validateHobby(textFieldCharHobbyAnswer)) {
            isValid = false
        }

        return isValid
    }

    private fun validateHobby(hobbyAnswer: String?): Boolean {
        return if (hobbyAnswer.isNullOrBlank()) {
            binding?.textFieldHobbyAnswer?.error = "Введите хобби"
            false
        } else {
            // Очищаем ошибку, если поле заполнено
            binding?.textFieldHobbyAnswer?.error = null
//            binding?.textFieldHobbyAnswer?.isErrorEnabled = false
            true
        }
    }

    private fun showDatePicker() {
        // Устанавливаем ограничения даты (например, от 1900 года до сегодня)
        val constraintsBuilder = CalendarConstraints.Builder()
        val today = MaterialDatePicker.todayInUtcMilliseconds()
        val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))

        // Минимальная дата - 1 января 1900
        calendar.set(1900, 0, 1)
        val minDate = calendar.timeInMillis

        // Максимальная дата - сегодня
        val maxDate = today

        constraintsBuilder.setStart(minDate)
        constraintsBuilder.setEnd(maxDate)

        // Если уже выбрана дата, устанавливаем её как начальную
        if (selectedDateInMillis != null) {
            constraintsBuilder.setOpenAt(selectedDateInMillis!!)
        }

        val datePicker = MaterialDatePicker.Builder.datePicker()
            .setTitleText("Выберите дату рождения")
            .setSelection(selectedDateInMillis ?: today) // Текущая дата или ранее выбранная
            .setCalendarConstraints(constraintsBuilder.build())
//            .setTheme(R.style.ThemeOverlay_Material3_DatePicker)
            .build()

        datePicker.addOnPositiveButtonClickListener { selection ->
            selectedDateInMillis = selection
            onDateSelected(selection)
        }

        datePicker.show(childFragmentManager, "DATE_PICKER")
    }

    private fun onDateSelected(selectedDateMillis: Long) {
        // Форматируем для отображения (день.месяц.год)
        val displayFormatter = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
        val displayDate = displayFormatter.format(Date(selectedDateMillis))
        binding?.editTextBirthDate?.setText(displayDate)

        // Сразу рассчитываем и отображаем возраст
        calculateAndDisplayAge(Date(selectedDateMillis))
    }

//    private fun calculateAndDisplayAge(birthDate: Date): Int {
//        val today = Calendar.getInstance()
//        val birthCalendar = Calendar.getInstance().apply { time = birthDate }
//
//        var age = today.get(Calendar.YEAR) - birthCalendar.get(Calendar.YEAR)
//
//        // Более точная проверка дня рождения
//        val todayMonth = today.get(Calendar.MONTH)
//        val birthMonth = birthCalendar.get(Calendar.MONTH)
//
//        if (todayMonth < birthMonth) {
//            age--
//        } else if (todayMonth == birthMonth) {
//            val todayDay = today.get(Calendar.DAY_OF_MONTH)
//            val birthDay = birthCalendar.get(Calendar.DAY_OF_MONTH)
//            if (todayDay < birthDay) {
//                age--
//            }
//        }
//
//        val ageText = age.toString()
//        binding?.textChildAgeValue?.text = ageText
//
//        // Добавляем текст "лет/год/года" для лучшего UX
//        val ageTextWithLabel = when {
//            age % 10 == 1 && age % 100 != 11 -> "$age год"
//            age % 10 in 2..4 && age % 100 !in 12..14 -> "$age года"
//            else -> "$age лет"
//        }
//        binding?.textChildAgeValue?.text = ageTextWithLabel
//
//        // Очищаем ошибку, если возраст стал валидным
//        if (age > 0) {
//            binding?.textFieldBirthDate?.error = null
//            binding?.textFieldBirthDate?.isErrorEnabled = false
//        }
//
//        return age
//    }

    private fun calculateAndDisplayAge(birthDate: Date): Int {
        val today = Calendar.getInstance()
        val birthCalendar = Calendar.getInstance().apply { time = birthDate }

        var age = today.get(Calendar.YEAR) - birthCalendar.get(Calendar.YEAR)

        // Более точная проверка дня рождения
        val todayMonth = today.get(Calendar.MONTH)
        val birthMonth = birthCalendar.get(Calendar.MONTH)

        if (todayMonth < birthMonth) {
            age--
        } else if (todayMonth == birthMonth) {
            val todayDay = today.get(Calendar.DAY_OF_MONTH)
            val birthDay = birthCalendar.get(Calendar.DAY_OF_MONTH)
            if (todayDay < birthDay) {
                age--
            }
        }

        // Сохраняем только число для обработки
        binding?.textChildAgeValue?.text = age.toString()

        // Очищаем ошибку, если возраст стал валидным
        if (age > 0) {
            binding?.textFieldBirthDate?.error = null
            binding?.textFieldBirthDate?.isErrorEnabled = false
        }

        return age
    }

    private fun convertToSaveFormat(selectedDateMillis: Long): String {
        return try {
            val saveFormatter = SimpleDateFormat(
                "MM/dd/yyyy",
                Locale.US
            ) // Используем US локаль для гарантии формата
            saveFormatter.format(Date(selectedDateMillis))
        } catch (e: Exception) {
            ""
        }
    }

    // VIEW BINDING -> 3. onDestroyView()
    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }
}