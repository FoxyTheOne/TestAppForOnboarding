package com.foxynest.testappforonboarding.presentation.dialogs

import android.app.Dialog
import android.os.Bundle
import android.view.View
import androidx.fragment.app.DialogFragment
import com.foxynest.testappforonboarding.R
import com.foxynest.testappforonboarding.databinding.DialogDataSaveErrorBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class DataSaveErrorDialog : DialogFragment() {
    companion object {
        const val TAG = "DataSaveErrorDialog"

        fun newInstance(): DataSaveErrorDialog {
            return DataSaveErrorDialog()
        }
    }

    private var onRetryClick: (() -> Unit)? = null
    private var onCancelClick: (() -> Unit)? = null

    private lateinit var binding: DialogDataSaveErrorBinding

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        binding = DialogDataSaveErrorBinding.inflate(layoutInflater)

        val dialogBuilder = MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.data_save_error_title))
            .setView(binding.root)
            .setCancelable(false)

        // Создаем диалог
        val dialog = dialogBuilder.create()

        // Устанавливаем кастомные обработчики для кнопок
        dialog.setButton(Dialog.BUTTON_POSITIVE, getString(R.string.retry)) { _, _ ->
            // Пустая лямбда - обрабатываем в onStart
        }

        dialog.setButton(Dialog.BUTTON_NEGATIVE, getString(R.string.cancel)) { _, _ ->
            onCancelClick?.invoke()
        }

        return dialog
    }

    override fun onStart() {
        super.onStart()

        // Находим кнопку "Повторить" и устанавливаем обработчик
        val positiveButton = (dialog as? androidx.appcompat.app.AlertDialog)
            ?.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE)

        positiveButton?.setOnClickListener {
            onRetryClick?.invoke()
        }

        // Инициализируем UI
        binding.textMessage.text = getString(R.string.data_save_error_message)
        binding.progressBar.visibility = View.GONE
    }

    fun setListeners(
        retryAction: (() -> Unit)? = null,
        cancelAction: (() -> Unit)? = null
    ): DataSaveErrorDialog {
        this.onRetryClick = retryAction
        this.onCancelClick = cancelAction
        return this
    }

    fun showLoading() {
        binding.textMessage.text = getString(R.string.data_save_loading_message)
        binding.progressBar.visibility = View.VISIBLE

        val positiveButton = (dialog as? androidx.appcompat.app.AlertDialog)
            ?.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE)
        val negativeButton = (dialog as? androidx.appcompat.app.AlertDialog)
            ?.getButton(androidx.appcompat.app.AlertDialog.BUTTON_NEGATIVE)

        positiveButton?.isEnabled = false
        negativeButton?.isEnabled = false
    }

    fun showError(message: String? = null) {
        binding.textMessage.text = message ?: getString(R.string.data_save_error_message)
        binding.progressBar.visibility = View.GONE

        val positiveButton = (dialog as? androidx.appcompat.app.AlertDialog)
            ?.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE)
        val negativeButton = (dialog as? androidx.appcompat.app.AlertDialog)
            ?.getButton(androidx.appcompat.app.AlertDialog.BUTTON_NEGATIVE)

        positiveButton?.isEnabled = true
        negativeButton?.isEnabled = true
    }
}