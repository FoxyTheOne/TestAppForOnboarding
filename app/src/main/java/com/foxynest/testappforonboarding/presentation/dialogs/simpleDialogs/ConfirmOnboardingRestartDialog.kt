package com.foxynest.testappforonboarding.presentation.dialogs.simpleDialogs

import com.foxynest.testappforonboarding.R

class ConfirmOnboardingRestartDialog : BaseDialogFragment() {
    companion object {
        const val TAG = "ConfirmSurveyRestartDialog"

        fun newInstance(): ConfirmOnboardingRestartDialog {
            return ConfirmOnboardingRestartDialog()
        }
    }

    override fun getTitle(): String = getString(R.string.confirm_dialog_title)

    override fun getMessage(): String = getString(R.string.are_you_sure_confirm)

    override fun getPositiveButtonText(): String = getString(R.string.dialog_yes)

    override fun getNegativeButtonText(): String = getString(R.string.dialog_no)
}