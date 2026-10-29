package com.foxynest.testappforonboarding.presentation.welcome.onboardingAndPaywall

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.foxynest.testappforonboarding.R
import com.foxynest.testappforonboarding.databinding.FragmentQuestion3Binding
import com.foxynest.testappforonboarding.other.extensions.calculateAge
import com.foxynest.testappforonboarding.other.extensions.parseDateToLocalDate
import com.foxynest.testappforonboarding.other.extensions.safeNavigate
import com.foxynest.testappforonboarding.other.extensions.toMySaveFormatString
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.DateValidatorPointBackward
import com.google.android.material.datepicker.MaterialDatePicker
import dagger.hilt.android.AndroidEntryPoint
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

@AndroidEntryPoint
class Question3Fragment : BaseOnboardingFragment() {
    companion object {
        private const val SELECTED_DATE_KEY = "selected_date"
        private const val DATE_PICKER_TAG = "DATE_PICKER"
        private val MIN_BIRTH_DATE: LocalDate = LocalDate.of(1900, 1, 1)
    }

    // VIEW BINDING -> 1. Объявляем переменную. This property is only valid between onCreateView and onDestroyView
    private var binding: FragmentQuestion3Binding? = null

    private var selectedDate: LocalDate? = null
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

        // Выбранная дата переживает пересоздание экрана (смена языка/темы)
        savedInstanceState?.getString(SELECTED_DATE_KEY)?.parseDateToLocalDate()
            ?.let(::onDateSelected)

        // Диалог восстанавливается системой сам, но слушатель – нет
        (childFragmentManager.findFragmentByTag(DATE_PICKER_TAG) as? MaterialDatePicker<*>)
            ?.let { restorePickerListener(it) }

        initListeners()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        selectedDate?.let { outState.putString(SELECTED_DATE_KEY, it.toMySaveFormatString()) }
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
            val date = validateDateForButtonNext() ?: return@setOnClickListener
            mainViewModel.updateBirthDate(date.toMySaveFormatString())
            safeNavigate(
                R.id.navigation_question3,
                R.id.action_navigation_question3_to_navigation_question4,
                sourceArgs()
            )
        }
    }

    private fun validateDateForButtonNext(): LocalDate? {
        val date = selectedDate
        val error = when {
            date == null -> getString(R.string.error_birth_date_not_selected)
            // Раньше проверялся только возраст < 0, а он равен 0 для любой даты в ближайшие 12 месяцев
            date.isAfter(LocalDate.now()) || date.isBefore(MIN_BIRTH_DATE) ->
                getString(R.string.error_birth_date_not_valid)

            else -> null
        }
        binding?.textFieldBirthDate?.error = error
        return if (error == null) date else null
    }

    private fun showDatePicker() {
        if (childFragmentManager.findFragmentByTag(DATE_PICKER_TAG) != null) return

        // MaterialDatePicker работает с полночью UTC. Любые переводы через ZoneId.systemDefault()
        // сдвигают дату на день: в часовых поясах западнее UTC сохранялся вчерашний день
        val todayUtcMillis = LocalDate.now().toUtcMillis()
        val constraints = CalendarConstraints.Builder()
            .setStart(MIN_BIRTH_DATE.toUtcMillis())
            .setEnd(todayUtcMillis)
            // setEnd ограничивает только пролистывание месяцев; выбрать будущий день текущего месяца мешает валидатор
            .setValidator(DateValidatorPointBackward.before(todayUtcMillis + 1))
            .setOpenAt(selectedDate?.toUtcMillis() ?: todayUtcMillis)
            .build()

        val datePicker = MaterialDatePicker.Builder.datePicker()
            .setTitleText(getString(R.string.text_select_date_hint))
            .setSelection(selectedDate?.toUtcMillis() ?: todayUtcMillis)
            .setCalendarConstraints(constraints)
            .build()

        restorePickerListener(datePicker)
        datePicker.show(childFragmentManager, DATE_PICKER_TAG)
    }

    @Suppress("UNCHECKED_CAST")
    private fun restorePickerListener(picker: MaterialDatePicker<*>) {
        (picker as MaterialDatePicker<Long>).addOnPositiveButtonClickListener { selectionMillis ->
            onDateSelected(
                Instant.ofEpochMilli(selectionMillis).atZone(ZoneOffset.UTC).toLocalDate()
            )
        }
    }

    private fun onDateSelected(date: LocalDate) {
        selectedDate = date

        val displayDate = date.format(
            DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(Locale.getDefault())
        )
        binding?.editTextBirthDate?.setText(displayDate)
        binding?.textChildAgeValue?.text = (date.calculateAge().data ?: 0).toString()
        // Очищаем ошибку
        binding?.textFieldBirthDate?.error = null
        binding?.textFieldBirthDate?.isErrorEnabled = false
    }

    private fun LocalDate.toUtcMillis(): Long =
        atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

    // VIEW BINDING -> 3. onDestroyView()
    override fun onDestroyView() {
        binding = null
        super.onDestroyView()
    }
}