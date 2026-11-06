package com.foxynest.testappforonboarding.presentation.splash

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.foxynest.testappforonboarding.R
import com.foxynest.testappforonboarding.other.extensions.safeNavigate
import dagger.hilt.android.AndroidEntryPoint

/** Стартовый экран: пока идёт миграция данных и проверка входа, показывает заставку, потом решает, куда перейти. */
@AndroidEntryPoint
class SplashFragment : Fragment() {
    private val splashViewModel by viewModels<SplashViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_splash, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        splashViewModel.performStartupChecksAndNavigate()

        subscribeOnObservers()
    }

    private fun subscribeOnObservers() {
        splashViewModel.navigateToNextLiveData.observe(viewLifecycleOwner) { targetDestination ->
            navigateToNext(targetDestination)
        }
    }

    private fun navigateToNext(targetDestination: Int) {
        safeNavigate(R.id.splashFragment, targetDestination)
    }
}