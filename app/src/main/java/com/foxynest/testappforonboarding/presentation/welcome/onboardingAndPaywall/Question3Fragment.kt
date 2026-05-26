package com.foxynest.testappforonboarding.presentation.welcome.onboardingAndPaywall

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import com.foxynest.testappforonboarding.R
import com.foxynest.testappforonboarding.databinding.FragmentQuestion3Binding
import com.foxynest.testappforonboarding.other.Status
import com.foxynest.testappforonboarding.other.extensions.calculateChildAge
import com.foxynest.testappforonboarding.other.extensions.convertDateMillisToMySaveFormat
import com.foxynest.testappforonboarding.other.extensions.safeNavigate
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.MaterialDatePicker
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@AndroidEntryPoint
class Question3Fragment : BaseOnboardingFragment() {
    companion object {
        private const val TAG = "Question3Fragment"
    }

    // VIEW BINDING -> 1. Объявляем переменную. This property is only valid between onCreateView and onDestroyView
    private var binding: FragmentQuestion3Binding? = null

    private var selectedDateInMillis: Long? = null
    override val currentFragmentId: Int = R.id.navigation_question3

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

        // Добавим логирование для отладки
        Log.d(TAG, "Question3Fragment onViewCreated")
        Log.d(TAG, "isOnboardingStarted: ${mainViewModel.isOnboardingStarted()}")
        Log.d(TAG, "isOnboardingCancelled: ${mainViewModel.isOnboardingCancelled()}")
        Log.d(TAG, "isOnboardingCompleted: ${mainViewModel.isOnboardingCompleted()}")
        Log.d(TAG, "sourceFragment: $sourceFragment")

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

        // SETUP BUTTONS
        // Добавляем обработчик кнопки отмены
        binding?.buttonCancel?.setOnClickListener {
            showCancelConfirmationDialog()
        }

        binding?.buttonNext?.setOnClickListener {
            val birthDateText = selectedDateInMillis?.convertDateMillisToMySaveFormat() ?: ""
            val ageText = binding?.textChildAgeValue?.text?.toString() ?: "0"

            if (validateDate(birthDateText, ageText)) {
                // Все проверки на null должны были быть в areFieldsValid()
                mainViewModel.updateBirthDate(birthDateText)

                val args = Bundle().apply {
                    putString("source_fragment", sourceFragment)
                }
                safeNavigate(
                    R.id.navigation_question3,
                    R.id.action_navigation_question3_to_navigation_question4,
                    args
                )
            }
        }
    }

    private fun validateDate(
        birthDateText: String?, // в формате MM/dd/yyyy
        ageText: String?
    ): Boolean {
        var isValid = true

        // Проверка даты рождения
        if (birthDateText.isNullOrBlank() || selectedDateInMillis == null) {
            binding?.textFieldBirthDate?.error = getString(R.string.error_birth_date_not_selected)
            isValid = false
        } else {
            binding?.textFieldBirthDate?.error = null
        }

        // Проверка возраста (должен быть больше 0)
        val age = ageText?.toIntOrNull() ?: 0
        if (age < 0) {
            if (isValid) { // Показываем ошибку возраста только если дата валидна
                binding?.textFieldBirthDate?.error = getString(R.string.error_birth_date_not_valid)
            }
            isValid = false
        }

        return isValid
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
            .setTitleText(getString(R.string.text_select_date_hint))
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

//    private fun onDateSelected(selectedDateMillis: Long) {
//        // Форматируем для отображения (день.месяц.год)
//        val displayFormatter = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
//        val displayDate = displayFormatter.format(Date(selectedDateMillis))
//        binding?.editTextBirthDate?.setText(displayDate)
//
//        // Сразу рассчитываем и отображаем возраст
//        calculateAndDisplayAge(Date(selectedDateMillis))
//    }

    private fun onDateSelected(selectedDateMillis: Long) {
        // Форматируем для отображения (день.месяц.год)
        val date = Date(selectedDateMillis)
        val displayFormatter = android.text.format.DateFormat.getDateFormat(requireContext())
        val displayDate = displayFormatter.format(date)
        binding?.editTextBirthDate?.setText(displayDate)

        // Сразу рассчитываем и отображаем возраст
        calculateAndDisplayAge(date)
    }

    private fun calculateAndDisplayAge(birthDate: Date): Int {
        var age = 0
        val resultAge = birthDate.calculateChildAge()

        when (resultAge.status) {
            Status.SUCCESS -> {
                age = resultAge.data ?: 0
            }

            Status.ERROR -> {
                age = 0
            } // TODO toast?
            else -> {}// nothing
        }

        // Сохраняем только число для обработки
        binding?.textChildAgeValue?.text = age.toString()

        // Очищаем ошибку, если возраст стал валидным
        if (age >= 0) {
            binding?.textFieldBirthDate?.error = null
            binding?.textFieldBirthDate?.isErrorEnabled = false
        }

        return age
    }

    private fun hasNavigatedToNext(): Boolean {
        return findNavController().currentDestination?.id == R.id.navigation_paywall
    }

    // VIEW BINDING -> 3. onDestroyView()
    override fun onDestroyView() {
        // cancelOnboarding() уже вызывается при нажатии кнопки отмены через диалог и не должен вызываться повторно в onDestroyView()
        binding = null
        super.onDestroyView()
    }
}