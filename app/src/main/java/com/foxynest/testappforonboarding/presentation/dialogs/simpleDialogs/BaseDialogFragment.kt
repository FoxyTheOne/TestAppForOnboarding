package com.foxynest.testappforonboarding.presentation.dialogs.simpleDialogs

import android.app.Dialog
import android.os.Bundle
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.setFragmentResult
import androidx.lifecycle.LifecycleOwner
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/**
 * Диалог «Да/Нет». Результат возвращается через Fragment Result API, а не через лямбды:
 * при пересоздании экрана (смена языка, тёмной темы) система восстанавливает диалог без лямбд,
 * и раньше кнопки просто переставали что-либо делать.
 *
 * Использование:
 * ```
 * // в onViewCreated / onCreate хоста
 * QuitDialog.setResultListener(childFragmentManager, viewLifecycleOwner) { confirmed -> ... }
 * // показ
 * QuitDialog().show(childFragmentManager, QuitDialog.TAG)
 * ```
 */
abstract class BaseDialogFragment : DialogFragment() {

    abstract fun getTitle(): String
    abstract fun getMessage(): String
    abstract fun getPositiveButtonText(): String
    abstract fun getNegativeButtonText(): String

    /** Ключ результата – у каждого диалога свой. */
    protected abstract val requestKey: String

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return MaterialAlertDialogBuilder(requireContext())
            .setTitle(getTitle())
            .setMessage(getMessage())
            .setPositiveButton(getPositiveButtonText()) { _, _ -> sendResult(true) }
            .setNegativeButton(getNegativeButtonText()) { _, _ -> sendResult(false) }
            .create()
    }

    private fun sendResult(confirmed: Boolean) {
        setFragmentResult(requestKey, bundleOf(RESULT_CONFIRMED to confirmed))
    }

    companion object {
        private const val RESULT_CONFIRMED = "confirmed"

        fun setResultListener(
            requestKey: String,
            fragmentManager: FragmentManager,
            lifecycleOwner: LifecycleOwner,
            onResult: (confirmed: Boolean) -> Unit
        ) {
            fragmentManager.setFragmentResultListener(requestKey, lifecycleOwner) { _, bundle ->
                onResult(bundle.getBoolean(RESULT_CONFIRMED))
            }
        }
    }
}