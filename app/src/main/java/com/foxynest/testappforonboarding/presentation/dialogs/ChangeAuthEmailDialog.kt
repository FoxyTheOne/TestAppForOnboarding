package com.foxynest.testappforonboarding.presentation.dialogs

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import com.foxynest.testappforonboarding.R
import com.foxynest.testappforonboarding.databinding.DialogChangeAuthEmailBinding
import com.foxynest.testappforonboarding.other.utils.EmailUtils
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar

class ChangeAuthEmailDialog : DialogFragment() {
    companion object {
        const val TAG = "ChangeAuthEmailDialog"
        private const val CURRENT_EMAIL_KEY = "current_email"

        fun newInstance(currentEmail: String): ChangeAuthEmailDialog {
            return ChangeAuthEmailDialog().apply {
                arguments = Bundle().apply {
                    putString(CURRENT_EMAIL_KEY, currentEmail)
                }
            }
        }
    }

    private var onSaveClickListener: ((String, String) -> Unit)? = null
    private lateinit var binding: DialogChangeAuthEmailBinding
    private var isLoading = false
    private var positiveButton: Button? = null
    private var negativeButton: Button? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        binding = DialogChangeAuthEmailBinding.inflate(layoutInflater)

        val currentEmail = arguments?.getString(CURRENT_EMAIL_KEY) ?: ""
        binding.textInputEditTextNewEmail.setText(currentEmail)

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.change_auth_email_title))
            .setView(binding.root)
            .setPositiveButton(getString(R.string.save), null)
            .setNegativeButton(getString(R.string.cancel), null)
            .create()

        dialog.setOnShowListener { dialogInterface ->
            val alertDialog = dialogInterface as AlertDialog
            positiveButton = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE)
            negativeButton = alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE)

            positiveButton?.setOnClickListener {
                val newEmail = binding.textInputEditTextNewEmail.text.toString().trim()
                val password = binding.textInputEditTextPassword.text.toString()

                if (validateForm(newEmail, password)) {
                    showLoading()
                    onSaveClickListener?.invoke(newEmail, password)
                } else {
                    // При ошибке валидации показываем сообщение в Toast для привлечения внимания
                    showValidationErrorToast()
                }
            }

            negativeButton?.setOnClickListener {
                if (!isLoading) {
                    alertDialog.dismiss()
                }
            }

            setupErrorClearingListeners()
        }

        return dialog
    }

    private fun showValidationErrorToast() {
        // Показываем краткое сообщение о необходимости исправить ошибки
        if (binding.textInputLayoutNewEmail.error != null) {
            Toast.makeText(
                requireContext(),
                binding.textInputLayoutNewEmail.error,
                Toast.LENGTH_SHORT
            ).show()
        } else if (binding.textInputLayoutPassword.error != null) {
            Toast.makeText(
                requireContext(),
                binding.textInputLayoutPassword.error,
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun setupErrorClearingListeners() {
        // Очищаем ошибки только для конкретного поля при изменении
        binding.textInputEditTextNewEmail.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                binding.textInputLayoutNewEmail.error = null
            }
        })

        binding.textInputEditTextPassword.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                binding.textInputLayoutPassword.error = null
            }
        })
    }

    private fun showLoading() {
        isLoading = true
        binding.progressBar.visibility = View.VISIBLE
        positiveButton?.isEnabled = false
        negativeButton?.isEnabled = false

        val colorGray = ContextCompat.getColor(requireContext(), R.color.gray)
        positiveButton?.setTextColor(colorGray)
        negativeButton?.setTextColor(colorGray)

        // Убираем клавиатуру, но сохраняем текущий фокус
        val currentFocus = dialog?.currentFocus
        val inputMethodManager =
            requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.hideSoftInputFromWindow(
            currentFocus?.windowToken ?: binding.textInputEditTextNewEmail.windowToken,
            0
        )
    }

    private fun validateForm(newEmail: String, password: String): Boolean {
        var isValid = true

        if (newEmail.isBlank()) {
            binding.textInputLayoutNewEmail.error = getString(R.string.email_empty_error)
            isValid = false
        } else if (!EmailUtils.emailMatches(newEmail)) {
            binding.textInputLayoutNewEmail.error = getString(R.string.email_format_error)
            isValid = false
        } else {
            binding.textInputLayoutNewEmail.error = null
        }

        if (password.isBlank()) {
            binding.textInputLayoutPassword.error = getString(R.string.password_empty_error)
            isValid = false
        } else {
            binding.textInputLayoutPassword.error = null
        }

        return isValid
    }

    fun showError(errorMessage: String, isPasswordError: Boolean = false) {
        isLoading = false
        binding.progressBar.visibility = View.GONE

        if (isPasswordError) {
            binding.textInputLayoutPassword.error = errorMessage
        } else {
            binding.textInputLayoutNewEmail.error = errorMessage
        }

        positiveButton?.isEnabled = true
        negativeButton?.isEnabled = true

        val colorPrimary = ContextCompat.getColor(requireContext(), R.color.purple_200)
        positiveButton?.setTextColor(colorPrimary)
        negativeButton?.setTextColor(colorPrimary)

        // Используем Snackbar для более красивого отображения ошибки
        val snackbar = Snackbar.make(binding.root, errorMessage, Snackbar.LENGTH_LONG)
        snackbar.setAction("OK") { snackbar.dismiss() }
        snackbar.show()
    }

    fun showSuccess() {
        isLoading = false
        binding.progressBar.visibility = View.GONE
        dismiss()
    }

    fun setOnSaveClickListener(listener: (String, String) -> Unit): ChangeAuthEmailDialog {
        this.onSaveClickListener = listener
        return this
    }

    fun showVerificationError(errorMessage: String, onAction: (String) -> Unit) {
        isLoading = false
        binding.progressBar.visibility = View.GONE

        // Показываем специальное сообщение с кнопками действий
        binding.textInputLayoutNewEmail.error = errorMessage

        // Меняем текст кнопок для специального случая
        positiveButton?.text = "Send Verification"
        negativeButton?.text = "Cancel"

        // Сохраняем оригинальные обработчики и устанавливаем новые
        positiveButton?.setOnClickListener {
            onAction("send_verification")
        }

        negativeButton?.setOnClickListener {
            onAction("cancel")
        }

        positiveButton?.isEnabled = true
        negativeButton?.isEnabled = true

        val colorPrimary = ContextCompat.getColor(requireContext(), R.color.purple_200)
        positiveButton?.setTextColor(colorPrimary)
        negativeButton?.setTextColor(colorPrimary)
    }
}