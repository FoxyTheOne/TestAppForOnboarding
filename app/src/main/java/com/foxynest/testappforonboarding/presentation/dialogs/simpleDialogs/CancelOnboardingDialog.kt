package com.foxynest.testappforonboarding.presentation.dialogs.simpleDialogs

import com.foxynest.testappforonboarding.R

class CancelOnboardingDialog : BaseDialogFragment() {
    companion object {
        const val TAG = "CancelOnboardingDialog"

        fun newInstance(): CancelOnboardingDialog {
            return CancelOnboardingDialog()
        }
    }

    override fun getTitle(): String = getString(R.string.confirm_dialog_title)

    override fun getMessage(): String = getString(R.string.are_you_sure_confirm2)

    override fun getPositiveButtonText(): String = getString(R.string.yes)

    override fun getNegativeButtonText(): String = getString(R.string.no)
}