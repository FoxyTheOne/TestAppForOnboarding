package com.foxynest.testappforonboarding.presentation.dialogs.simpleDialogs

import androidx.fragment.app.FragmentManager
import androidx.lifecycle.LifecycleOwner
import com.foxynest.testappforonboarding.R

/** «Прервать опрос?» Результат слушает BaseOnboardingFragment: при «да» черновик удаляется, сохранённая анкета остаётся. */
class CancelOnboardingDialog : BaseDialogFragment() {
    companion object {
        const val TAG = "CancelOnboardingDialog"

        fun setResultListener(
            fragmentManager: FragmentManager,
            lifecycleOwner: LifecycleOwner,
            onResult: (confirmed: Boolean) -> Unit
        ) = BaseDialogFragment.setResultListener(TAG, fragmentManager, lifecycleOwner, onResult)
    }

    override val requestKey: String = TAG

    override fun getTitle(): String = getString(R.string.confirm_dialog_title)

    override fun getMessage(): String = getString(R.string.are_you_sure_confirm2)

    override fun getPositiveButtonText(): String = getString(R.string.yes)

    override fun getNegativeButtonText(): String = getString(R.string.no)
}