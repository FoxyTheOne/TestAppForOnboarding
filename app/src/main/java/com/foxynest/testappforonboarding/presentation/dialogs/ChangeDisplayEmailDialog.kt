package com.foxynest.testappforonboarding.presentation.dialogs

import androidx.appcompat.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import com.foxynest.testappforonboarding.R
import com.foxynest.testappforonboarding.databinding.DialogChangeEmailBinding
import com.foxynest.testappforonboarding.other.utils.EmailUtils
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class ChangeDisplayEmailDialog : DialogFragment() {
    companion object {
        const val TAG = "ChangeDisplayEmailDialog"
        private const val CURRENT_EMAIL_KEY = "current_email"

        fun newInstance(currentEmail: String): ChangeDisplayEmailDialog {
            return ChangeDisplayEmailDialog().apply {
                arguments = Bundle().apply {
                    putString(CURRENT_EMAIL_KEY, currentEmail)
                }
            }
        }
    }

    private var onSaveClickListener: ((String) -> Unit)? = null
    private lateinit var binding: DialogChangeEmailBinding
    private var isLoading = false
    private var positiveButton: Button? = null
    private var negativeButton: Button? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        binding = DialogChangeEmailBinding.inflate(layoutInflater)

        val currentEmail = arguments?.getString(CURRENT_EMAIL_KEY) ?: ""
        binding.textInputEditText.setText(currentEmail)

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.change_display_email_title))
            .setView(binding.root)
            .setPositiveButton(getString(R.string.save), null)
            .setNegativeButton(getString(R.string.cancel), null)
            .create()

        // Используем onShowListener для полного контроля
        dialog.setOnShowListener { dialogInterface ->
            val alertDialog = dialogInterface as AlertDialog
            positiveButton = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE)
            negativeButton = alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE)

            positiveButton?.setOnClickListener {
                val newEmail = binding.textInputEditText.text.toString().trim()
                if (validateEmail(newEmail)) {
                    showLoading()
                    onSaveClickListener?.invoke(newEmail)
                }
                // Не вызываем dismiss() - диалог останется открытым при ошибке валидации
            }

            negativeButton?.setOnClickListener {
                if (!isLoading) {
                    alertDialog.dismiss()
                }
            }

            // Добавляем слушатели для очистки ошибки при взаимодействии с полем
            setupErrorClearingListeners()
        }

        return dialog
    }

    private fun setupErrorClearingListeners() {
        // Очищаем ошибку при изменении текста
        binding.textInputEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                // Очищаем ошибку когда пользователь начинает вводить
                binding.textInputLayout.error = null
            }
        })

        // Очищаем ошибку при фокусе на поле (на случай, если пользователь просто кликнул в поле)
        binding.textInputEditText.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                binding.textInputLayout.error = null
            }
        }

        // Очищаем ошибку при клике на поле (дополнительная защита)
        binding.textInputEditText.setOnClickListener {
            binding.textInputLayout.error = null
        }
    }

    private fun showLoading() {
        isLoading = true
        binding.progressBar.visibility = View.VISIBLE
        positiveButton?.isEnabled = false
        negativeButton?.isEnabled = false

        // Меняем цвет текста на серый для визуального отключения
        val colorGray = ContextCompat.getColor(requireContext(), R.color.gray)
        positiveButton?.setTextColor(colorGray)
        negativeButton?.setTextColor(colorGray)

        // Убираем фокус и клавиатуру при загрузке
        binding.textInputEditText.clearFocus()
        val inputMethodManager = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.hideSoftInputFromWindow(binding.textInputEditText.windowToken, 0)
    }

    private fun validateEmail(email: String): Boolean {
        if (email.isBlank()) {
            binding.textInputLayout.error = getString(R.string.email_empty_error)
            return false
        } else if (!EmailUtils.emailMatches(email)) {
            binding.textInputLayout.error = getString(R.string.email_format_error)
            return false
        }
        binding.textInputLayout.error = null
        return true
    }

    fun showError(errorMessage: String) {
        isLoading = false
        binding.progressBar.visibility = View.GONE
        binding.textInputLayout.error = errorMessage

        positiveButton?.isEnabled = true
        negativeButton?.isEnabled = true

        // Восстанавливаем оригинальные цвета
        val colorPrimary = ContextCompat.getColor(requireContext(), R.color.purple_200)
        positiveButton?.setTextColor(colorPrimary)
        negativeButton?.setTextColor(colorPrimary)

        // Возвращаем фокус на поле ввода при ошибке
        binding.textInputEditText.requestFocus()
        binding.textInputEditText.selectAll() // Выделяем весь текст для удобства исправления

        val inputMethodManager = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.showSoftInput(binding.textInputEditText, InputMethodManager.SHOW_IMPLICIT)
    }

    fun showSuccess() {
        isLoading = false
        binding.progressBar.visibility = View.GONE
        dismiss()
    }

    fun setOnSaveClickListener(listener: (String) -> Unit): ChangeDisplayEmailDialog {
        this.onSaveClickListener = listener
        return this
    }
}