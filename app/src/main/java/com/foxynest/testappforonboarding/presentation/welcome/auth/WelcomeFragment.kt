package com.foxynest.testappforonboarding.presentation.welcome.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.foxynest.testappforonboarding.R
import com.foxynest.testappforonboarding.databinding.FragmentWelcomeBinding
import com.foxynest.testappforonboarding.presentation.welcome.BaseWelcomeFragmentAbstract
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class WelcomeFragment : BaseWelcomeFragmentAbstract() {
    companion object {
        private const val TAG = "AuthFragment"
    }

    // VIEW BINDING -> 1. Объявляем переменную. This property is only valid between onCreateView and onDestroyView
    private var binding: FragmentWelcomeBinding? = null

    private val authViewModel by viewModels<AuthViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // VIEW BINDING -> 2. Инициализация
        binding = FragmentWelcomeBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initListeners()
    }

    private fun initListeners() {
        binding?.buttonStartOnboarding?.setOnClickListener {
            if (findNavController().currentDestination?.id == R.id.navigation_welcome) {
                findNavController().navigate(R.id.action_welcomeFragment_to_navigation_question1)
            }
        }

        binding?.buttonStartLogin?.setOnClickListener {
            if (findNavController().currentDestination?.id == R.id.navigation_welcome) {
                findNavController().navigate(R.id.action_welcomeFragment_to_loginFragment)
            }
        }
    }

    // VIEW BINDING -> 3. onDestroyView()
    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }
}