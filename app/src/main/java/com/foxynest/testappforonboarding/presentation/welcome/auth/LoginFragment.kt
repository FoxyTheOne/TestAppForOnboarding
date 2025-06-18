package com.foxynest.testappforonboarding.presentation.welcome.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.foxynest.testappforonboarding.R
import com.foxynest.testappforonboarding.databinding.FragmentLogInBinding
import com.foxynest.testappforonboarding.other.constants.CommonConstants.PROFILE_DESTINATION
import com.foxynest.testappforonboarding.presentation.welcome.BaseWelcomeFragmentAbstract
import com.foxynest.testappforonboarding.presentation.welcome.auth.registration.RegistrationViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class LoginFragment : BaseWelcomeFragmentAbstract() {
    companion object {
        private const val TAG = "LoginFragment"
    }

    // VIEW BINDING -> 1. Объявляем переменную. This property is only valid between onCreateView and onDestroyView
    private var binding: FragmentLogInBinding? = null

    private val registrationViewModel by viewModels<RegistrationViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // VIEW BINDING -> 2. Инициализация
        binding = FragmentLogInBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initListeners()
    }

    private fun initListeners() {
        binding?.buttonBack?.setOnClickListener {
            val argument = arguments?.getString("previous_destination")

            if (argument == PROFILE_DESTINATION) {

                if (findNavController().currentDestination?.id == R.id.navigation_login) {
                    findNavController().navigate(R.id.action_loginFragment_to_navigation_profile)
                }

            } else {

                if (findNavController().currentDestination?.id == R.id.navigation_login) {
                    findNavController().navigate(R.id.action_loginFragment_to_welcomeFragment)
                }

            }
        }
    }

    // VIEW BINDING -> 3. onDestroyView()
    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }
}