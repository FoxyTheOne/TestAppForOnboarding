package com.foxynest.testappforonboarding.presentation.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.foxynest.testappforonboarding.databinding.FragmentAuthBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AuthFragment : Fragment() {
    companion object {
        private const val TAG = "AuthFragment"
    }

    // VIEW BINDING -> 1. Объявляем переменную. This property is only valid between onCreateView and onDestroyView
    private var binding: FragmentAuthBinding? = null

    private val authViewModel by viewModels<AuthViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // VIEW BINDING -> 2. Инициализация
        binding = FragmentAuthBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

//        initListeners()
    }

    // VIEW BINDING -> 3. onDestroyView()
    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }
}