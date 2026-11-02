package com.foxynest.testappforonboarding.presentation.dialogs

import android.app.Dialog
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import com.foxynest.testappforonboarding.R
import com.foxynest.testappforonboarding.databinding.DialogDeleteAccountBinding
import com.foxynest.testappforonboarding.domain.entities.ErrorType
import com.foxynest.testappforonboarding.other.Status
import com.foxynest.testappforonboarding.presentation.MainViewModel
import com.foxynest.testappforonboarding.presentation.common.toErrorMessage
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/**
 * Подтверждение удаления аккаунта паролем. Результат (успех) обрабатывает ProfileFragment через deleteAccountState.
 */
class DeleteAccountDialog : DialogFragment() {
    companion object {
        const val TAG = "DeleteAccountDialog"
    }

    private val mainViewModel: MainViewModel by activityViewModels()
    private var binding: DialogDeleteAccountBinding? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) mainViewModel.resetDeleteAccountState()

        mainViewModel.deleteAccountState.observe(this) { state ->
            when (state.status) {
                Status.LOADING -> setLoading(true)
                Status.ERROR -> {
                    setLoading(false)
                    val message = state.data.toErrorMessage(resources)
                        ?: getString(R.string.account_deletion_failed_error)
                    binding?.textInputLayoutPassword?.error = message
                    if (state.data != ErrorType.WRONG_PASSWORD_ERROR && state.data != ErrorType.PASSWORD_EMPTY_ERROR) {
                        binding?.textMessage?.text = message
                    }
                }

                Status.SUCCESS -> dismiss()
                Status.INIT -> setLoading(false)
            }
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialogBinding = DialogDeleteAccountBinding.inflate(layoutInflater)
        binding = dialogBinding
        dialogBinding.textInputEditTextPassword.doAfterTextChanged {
            dialogBinding.textInputLayoutPassword.error = null
        }

        return MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.delete_account_title)
            .setView(dialogBinding.root)
            .setPositiveButton(R.string.delete_account_confirm, null)
            .setNegativeButton(R.string.cancel, null)
            .create()
    }

    override fun onStart() {
        super.onStart()
        val alertDialog = dialog as? AlertDialog ?: return
        alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            mainViewModel.deleteAccount(
                binding?.textInputEditTextPassword?.text?.toString().orEmpty()
            )
        }
        alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE).setOnClickListener {
            mainViewModel.resetDeleteAccountState()
            dismiss()
        }
    }

    private fun setLoading(isLoading: Boolean) {
        binding?.progressBar?.visibility = if (isLoading) View.VISIBLE else View.GONE
        (dialog as? AlertDialog)?.let {
            it.getButton(AlertDialog.BUTTON_POSITIVE)?.isEnabled = !isLoading
            it.getButton(AlertDialog.BUTTON_NEGATIVE)?.isEnabled = !isLoading
        }
        isCancelable = !isLoading
    }

    override fun onDestroyView() {
        binding = null
        super.onDestroyView()
    }
}