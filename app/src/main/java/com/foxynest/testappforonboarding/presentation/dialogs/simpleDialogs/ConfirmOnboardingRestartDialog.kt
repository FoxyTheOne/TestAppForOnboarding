package com.foxynest.testappforonboarding.presentation.dialogs.simpleDialogs

class ConfirmOnboardingRestartDialog: BaseDialogFragment() {
    // TODO String resources
    override fun getTitle(): String = "Подтверждение"

    override fun getMessage(): String =
        "Вы уверены? Текущие ответы будут стёрты и перезаписаны."

    override fun getPositiveButtonText(): String = "Да"

    override fun getNegativeButtonText(): String = "Нет"

    companion object {
        const val TAG = "ConfirmSurveyRestartDialog"

        fun newInstance(): ConfirmOnboardingRestartDialog {
            return ConfirmOnboardingRestartDialog()
        }
    }
}