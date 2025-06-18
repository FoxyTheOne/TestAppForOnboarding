package com.foxynest.testappforonboarding.presentation.welcome

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.foxynest.testappforonboarding.R
import com.foxynest.testappforonboarding.data.dataSource.datastorePreferences.IAppDatastorePreferences
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject


/**
 * Базовый фрагмент для расширения фрагментами с онбордингом.
 * Содержит логику перехода сразу на content, если в предыдущий раз пользователь полностью прошёл онбординг
 */
@AndroidEntryPoint
abstract class BaseWelcomeFragmentAbstract : Fragment() {
    companion object {
        private const val TAG = "BaseOnboardingFragmentAbstract"
    }

    // Воспользуемся global navigation actions, чотбы открывать сразу content, если выполнено условие
    @Inject
    lateinit var appPreferences: IAppDatastorePreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        lifecycleScope.launch {
            appPreferences.isOnboardingCompleted()
                .collect { isCompleted ->
                    Log.d(TAG, "Проверка, пройден ли онбординг = $isCompleted")
                    if (isCompleted) {
//                        // Срабатывает моментально, а нам это нужно для последующих запусков, после первого
//                        findNavController().navigate(R.id.action_global_navigation_home)

                        if (findNavController().currentDestination?.id == R.id.navigation_welcome) {
                            findNavController().navigate(R.id.action_global_navigation_home)
                        }
                    }
                }
        }

    }
}