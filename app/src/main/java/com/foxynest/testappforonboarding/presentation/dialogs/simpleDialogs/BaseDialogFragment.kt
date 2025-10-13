package com.foxynest.testappforonboarding.presentation.dialogs.simpleDialogs

import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder

abstract class BaseDialogFragment : DialogFragment() {
    protected var onPositiveButtonClick: (() -> Unit)? = null
    protected var onNegativeButtonClick: (() -> Unit)? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return MaterialAlertDialogBuilder(requireContext())
            .setTitle(getTitle())
            .setMessage(getMessage())
            .setPositiveButton(getPositiveButtonText()) { _, _ ->
                onPositiveButtonClick?.invoke()
                dismiss()
            }
            .setNegativeButton(getNegativeButtonText()) { _, _ ->
                onNegativeButtonClick?.invoke()
                dismiss()
            }
            .create()
    }

    abstract fun getTitle(): String
    abstract fun getMessage(): String
    abstract fun getPositiveButtonText(): String
    abstract fun getNegativeButtonText(): String

    fun setListeners(
        positiveAction: (() -> Unit)? = null,
        negativeAction: (() -> Unit)? = null
    ): BaseDialogFragment {
        this.onPositiveButtonClick = positiveAction
        this.onNegativeButtonClick = negativeAction
        return this
    }
}