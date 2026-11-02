package com.foxynest.testappforonboarding.presentation.dialogs.simpleDialogs

import androidx.fragment.app.FragmentManager
import androidx.lifecycle.LifecycleOwner
import com.foxynest.testappforonboarding.R

class QuitDialog : BaseDialogFragment() {
    companion object {
        const val TAG = "QuitDialog"

        fun setResultListener(
            fragmentManager: FragmentManager,
            lifecycleOwner: LifecycleOwner,
            onResult: (confirmed: Boolean) -> Unit
        ) = BaseDialogFragment.setResultListener(TAG, fragmentManager, lifecycleOwner, onResult)
    }

    override val requestKey: String = TAG

    override fun getTitle(): String = getString(R.string.confirm_dialog_title)

    override fun getMessage(): String = getString(R.string.quit_confirm)

    override fun getPositiveButtonText(): String = getString(R.string.yes)

    override fun getNegativeButtonText(): String = getString(R.string.no)
}