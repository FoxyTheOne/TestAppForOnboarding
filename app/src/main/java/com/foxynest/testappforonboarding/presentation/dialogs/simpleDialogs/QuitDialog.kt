package com.foxynest.testappforonboarding.presentation.dialogs.simpleDialogs

import com.foxynest.testappforonboarding.R

class QuitDialog: BaseDialogFragment() {
    companion object {
        const val TAG = "QuitDialog"

        fun newInstance(): QuitDialog {
            return QuitDialog()
        }
    }

    override fun getTitle(): String = getString(R.string.confirm_dialog_title)

    override fun getMessage(): String = getString(R.string.quit_confirm)

    override fun getPositiveButtonText(): String = getString(R.string.yes)

    override fun getNegativeButtonText(): String = getString(R.string.no)
}