package com.foxynest.testappforonboarding.presentation.dialogs.simpleDialogs

import com.foxynest.testappforonboarding.R

class RepeatSavingOnboardingStateDialog : BaseDialogFragment(){
    companion object {
        const val TAG = "RepeatSavingOnboardingStateDialog"

        fun newInstance(): RepeatSavingOnboardingStateDialog {
            return RepeatSavingOnboardingStateDialog()
        }
    }

    override fun getTitle(): String = getString(R.string.local_onboarding_saving_error_title)

    override fun getMessage(): String = getString(R.string.local_onboarding_saving_error_text)

    override fun getPositiveButtonText(): String = getString(R.string.yes)

    override fun getNegativeButtonText(): String = getString(R.string.no)
}