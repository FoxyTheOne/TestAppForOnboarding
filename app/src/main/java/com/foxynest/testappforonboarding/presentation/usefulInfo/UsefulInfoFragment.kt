package com.foxynest.testappforonboarding.presentation.usefulInfo

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.foxynest.testappforonboarding.databinding.FragmentUsefulInfoBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class UsefulInfoFragment : Fragment() {

    // VIEW BINDING -> 1. Объявляем переменную. This property is only valid between onCreateView and onDestroyView
    private var binding: FragmentUsefulInfoBinding? = null

    private val usefulInfoViewModel by viewModels<UsefulInfoViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // VIEW BINDING -> 2. Инициализация
        binding = FragmentUsefulInfoBinding.inflate(inflater, container, false)
        return binding?.root
    }

//    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
//        super.onViewCreated(view, savedInstanceState)
//
//        subscribeOnLiveData()
//    }

//    private fun subscribeOnLiveData() {
//    }

    // VIEW BINDING -> 3. onDestroyView()
    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }
}