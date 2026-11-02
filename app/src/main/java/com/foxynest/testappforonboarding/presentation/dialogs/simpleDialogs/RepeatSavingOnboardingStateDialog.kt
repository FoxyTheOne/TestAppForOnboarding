package com.foxynest.testappforonboarding.presentation.dialogs.simpleDialogs

import androidx.fragment.app.FragmentManager
import androidx.lifecycle.LifecycleOwner
import com.foxynest.testappforonboarding.R

class RepeatSavingOnboardingStateDialog : BaseDialogFragment() {
    companion object {
        const val TAG = "RepeatSavingOnboardingStateDialog"

        fun setResultListener(
            fragmentManager: FragmentManager,
            lifecycleOwner: LifecycleOwner,
            onResult: (confirmed: Boolean) -> Unit
        ) = BaseDialogFragment.setResultListener(TAG, fragmentManager, lifecycleOwner, onResult)
    }

    override val requestKey: String = TAG

    override fun getTitle(): String = getString(R.string.local_onboarding_saving_error_title)

    override fun getMessage(): String = getString(R.string.local_onboarding_saving_error_text)

    override fun getPositiveButtonText(): String = getString(R.string.yes)

    override fun getNegativeButtonText(): String = getString(R.string.no)
}