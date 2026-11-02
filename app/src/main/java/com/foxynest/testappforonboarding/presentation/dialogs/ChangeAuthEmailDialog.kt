package com.foxynest.testappforonboarding.presentation.dialogs

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.AlertDialog
import androidx.core.os.bundleOf
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import com.foxynest.testappforonboarding.R
import com.foxynest.testappforonboarding.databinding.DialogChangeAuthEmailBinding
import com.foxynest.testappforonboarding.domain.entities.ErrorType
import com.foxynest.testappforonboarding.other.Status
import com.foxynest.testappforonboarding.other.utils.EmailUtils
import com.foxynest.testappforonboarding.presentation.MainViewModel
import com.foxynest.testappforonboarding.presentation.common.toErrorMessage
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/**
 * Смена почты для входа. Диалог сам работает с MainViewModel, поэтому переживает пересоздание экрана.
 * После успешной отправки письма показывает, что будет дальше: вход заново с новой почтой.
 */
class ChangeAuthEmailDialog : DialogFragment() {
    companion object {
        const val TAG = "ChangeAuthEmailDialog"
        private const val CURRENT_EMAIL_KEY = "current_email"
        private const val NEW_EMAIL_KEY = "new_email"

        fun newInstance(currentEmail: String) = ChangeAuthEmailDialog().apply {
            arguments = bundleOf(CURRENT_EMAIL_KEY to currentEmail)
        }
    }

    private val mainViewModel: MainViewModel by activityViewModels()
    private var binding: DialogChangeAuthEmailBinding? = null
    private var requestedEmail: String = ""

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        if (savedInstanceState == null) mainViewModel.resetAuthEmailUpdateState()
        requestedEmail = savedInstanceState?.getString(NEW_EMAIL_KEY).orEmpty()

        val dialogBinding = DialogChangeAuthEmailBinding.inflate(layoutInflater)
        binding = dialogBinding
        if (savedInstanceState == null) {
            dialogBinding.textInputEditTextNewEmail.setText(
                arguments?.getString(CURRENT_EMAIL_KEY).orEmpty()
            )
        }
        dialogBinding.textInputEditTextNewEmail.doAfterTextChanged {
            dialogBinding.textInputLayoutNewEmail.error = null
        }
        dialogBinding.textInputEditTextPassword.doAfterTextChanged {
            dialogBinding.textInputLayoutPassword.error = null
        }

        return MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.change_auth_email_title))
            .setView(dialogBinding.root)
            // Обработчики ставятся в onStart, чтобы кнопки не закрывали диалог при ошибке
            .setPositiveButton(getString(R.string.save), null)
            .setNegativeButton(getString(R.string.cancel), null)
            .create()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(NEW_EMAIL_KEY, requestedEmail)
    }

    override fun onStart() {
        super.onStart()
        val alertDialog = dialog as? AlertDialog ?: return
        alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener { onSaveClicked() }
        alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE).setOnClickListener {
            mainViewModel.resetAuthEmailUpdateState()
            dismiss()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Подписка один раз; события приходят после onStart, когда кнопки диалога уже созданы
        mainViewModel.authEmailUpdateState.observe(this) { state ->
            when (state.status) {
                Status.LOADING -> setLoading(true)
                Status.ERROR -> {
                    setLoading(false)
                    val message =
                        state.data.toErrorMessage(resources) ?: getString(R.string.unexpected_error)
                    if (state.data == ErrorType.WRONG_PASSWORD_WHILE_CHANGING_EMAIL_ERROR) {
                        binding?.textInputLayoutPassword?.error = message
                    } else {
                        binding?.textInputLayoutNewEmail?.error = message
                    }
                }

                Status.SUCCESS -> {
                    mainViewModel.resetAuthEmailUpdateState()
                    showWhatHappensNext()
                    dismiss()
                }

                Status.INIT -> setLoading(false)
            }
        }
    }

    private fun onSaveClicked() {
        val currentBinding = binding ?: return
        val newEmail = currentBinding.textInputEditTextNewEmail.text.toString().trim()
        val password = currentBinding.textInputEditTextPassword.text.toString()

        currentBinding.textInputLayoutNewEmail.error = when {
            newEmail.isBlank() -> getString(R.string.email_empty_error)
            !EmailUtils.emailMatches(newEmail) -> getString(R.string.email_format_error)
            else -> null
        }
        currentBinding.textInputLayoutPassword.error =
            if (password.isBlank()) getString(R.string.password_empty_error) else null

        if (currentBinding.textInputLayoutNewEmail.error == null && currentBinding.textInputLayoutPassword.error == null) {
            requestedEmail = newEmail
            mainViewModel.updateAuthEmail(newEmail, password)
        }
    }

    private fun showWhatHappensNext() {
        // Обычный AlertDialog: он только информирует, и потерять его при пересоздании экрана не страшно
        MaterialAlertDialogBuilder(requireActivity())
            .setTitle(R.string.email_change_sent_title)
            .setMessage(getString(R.string.email_change_sent_message, requestedEmail))
            .setPositiveButton(R.string.ok, null)
            .show()
    }

    private fun setLoading(isLoading: Boolean) {
        binding?.progressBar?.visibility = if (isLoading) View.VISIBLE else View.GONE
        (dialog as? AlertDialog)?.let {
            it.getButton(AlertDialog.BUTTON_POSITIVE)?.isEnabled = !isLoading
            it.getButton(AlertDialog.BUTTON_NEGATIVE)?.isEnabled = !isLoading
        }
        if (isLoading) {
            val imm =
                requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(binding?.root?.windowToken, 0)
        }
    }

    override fun onDestroyView() {
        binding = null
        super.onDestroyView()
    }
}