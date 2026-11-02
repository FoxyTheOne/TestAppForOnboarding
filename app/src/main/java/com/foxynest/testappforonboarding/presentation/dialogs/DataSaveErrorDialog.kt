package com.foxynest.testappforonboarding.presentation.dialogs

import android.app.Dialog
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import com.foxynest.testappforonboarding.R
import com.foxynest.testappforonboarding.databinding.DialogDataSaveErrorBinding
import com.foxynest.testappforonboarding.other.Status
import com.foxynest.testappforonboarding.presentation.MainViewModel
import com.foxynest.testappforonboarding.presentation.common.toErrorMessage
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/**
 * Повтор сохранения профиля в Firebase. Диалог сам работает с MainViewModel (общая для Activity),
 * поэтому после пересоздания экрана кнопки и состояние загрузки продолжают работать.
 */
class DataSaveErrorDialog : DialogFragment() {
    companion object {
        const val TAG = "DataSaveErrorDialog"
    }

    private val mainViewModel: MainViewModel by activityViewModels()
    private var binding: DialogDataSaveErrorBinding? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        if (savedInstanceState == null) mainViewModel.resetRetrySaveState()
        isCancelable = false

        val dialogBinding = DialogDataSaveErrorBinding.inflate(layoutInflater)
        binding = dialogBinding
        dialogBinding.textMessage.text = getString(R.string.data_save_error_message)

        return MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.data_save_error_title))
            .setView(dialogBinding.root)
            // Обработчик ставится в onStart, чтобы «Повторить» не закрывал диалог
            .setPositiveButton(getString(R.string.retry), null)
            .setNegativeButton(getString(R.string.cancel)) { _, _ ->
                mainViewModel.resetRetrySaveState()
                Toast.makeText(
                    requireContext(),
                    R.string.profile_toast_data_updating_error,
                    Toast.LENGTH_LONG
                ).show()
            }
            .create()
    }

    override fun onStart() {
        super.onStart()
        (dialog as? AlertDialog)?.getButton(AlertDialog.BUTTON_POSITIVE)?.setOnClickListener {
            mainViewModel.retryDataSave()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Подписка один раз; события приходят после onStart, когда кнопки диалога уже созданы
        mainViewModel.retrySaveState.observe(this) { state ->
            when (state.status) {
                Status.LOADING -> render(
                    isLoading = true,
                    message = getString(R.string.data_save_loading_message)
                )

                Status.ERROR -> render(
                    isLoading = false,
                    message = state.data.toErrorMessage(resources)
                        ?: getString(R.string.data_save_error_message)
                )

                Status.SUCCESS -> {
                    Toast.makeText(
                        requireContext(),
                        R.string.profile_toast_data_updated,
                        Toast.LENGTH_SHORT
                    ).show()
                    mainViewModel.resetRetrySaveState()
                    dismiss()
                }

                Status.INIT -> render(
                    isLoading = false,
                    message = getString(R.string.data_save_error_message)
                )
            }
        }
    }

    private fun render(isLoading: Boolean, message: String) {
        binding?.textMessage?.text = message
        binding?.progressBar?.visibility = if (isLoading) View.VISIBLE else View.GONE
        (dialog as? AlertDialog)?.let {
            it.getButton(AlertDialog.BUTTON_POSITIVE)?.isEnabled = !isLoading
            it.getButton(AlertDialog.BUTTON_NEGATIVE)?.isEnabled = !isLoading
        }
    }

    override fun onDestroyView() {
        binding = null
        super.onDestroyView()
    }
}