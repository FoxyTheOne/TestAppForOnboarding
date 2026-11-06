package com.foxynest.testappforonboarding.presentation.dialogs.simpleDialogs

import androidx.fragment.app.FragmentManager
import androidx.lifecycle.LifecycleOwner
import com.foxynest.testappforonboarding.R

/**
 * Объяснение перед системным запросом разрешения на уведомления: «чтобы поздравить с днём рождения».
 * Системное окно можно показать лишь пару раз – после двух отказов Android перестаёт его показывать,
 * поэтому сначала объясняем зачем, и только при «да» вызываем системный запрос (MainActivity).
 */
class NotificationPermissionDialog : BaseDialogFragment() {
    companion object {
        const val TAG = "NotificationPermissionDialog"

        fun setResultListener(
            fragmentManager: FragmentManager,
            lifecycleOwner: LifecycleOwner,
            onResult: (confirmed: Boolean) -> Unit
        ) = BaseDialogFragment.setResultListener(TAG, fragmentManager, lifecycleOwner, onResult)
    }

    override val requestKey: String = TAG

    override fun getTitle(): String = getString(R.string.notification_permission_title)

    override fun getMessage(): String = getString(R.string.notification_permission_message)

    override fun getPositiveButtonText(): String = getString(R.string.notification_permission_allow)

    override fun getNegativeButtonText(): String = getString(R.string.notification_permission_later)
}