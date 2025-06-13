package com.foxynest.testappforonboarding.presentation.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import com.foxynest.testappforonboarding.R
import com.foxynest.testappforonboarding.databinding.FragmentHomeBinding
import com.foxynest.testappforonboarding.presentation.MainViewModel
import dagger.hilt.android.AndroidEntryPoint
import java.lang.StringBuilder

@AndroidEntryPoint
class HomeFragment : Fragment() {
    // VIEW BINDING -> 1. Объявляем переменную. This property is only valid between onCreateView and onDestroyView
    private var binding: FragmentHomeBinding? = null

    // 1.1. ViewModel. We bind our viewModel to the cycle of our activity, not fragment. So, we need to do this way:
    private lateinit var mainViewModel: MainViewModel
    private val homeViewModel by viewModels<HomeViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // VIEW BINDING -> 2. Инициализация
        binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1.2. ViewModel. We bind our viewModel to the lifecycle of our activity, not fragment. We pass our activity as an owner of the lifecycle.
        // So, we need to do this way:
        mainViewModel = ViewModelProvider(requireActivity())[MainViewModel::class.java]

        initListeners()
        subscribeOnLiveData()
    }

    private fun initListeners() {
//        homeViewModel.text.observe(viewLifecycleOwner) {
//            binding?.textHome?.text = it
//        }
    }

    private fun subscribeOnLiveData() {
        mainViewModel.nameLiveData.observe(viewLifecycleOwner) { name ->

            val textToShow = StringBuilder()

            textToShow.append(context?.resources?.getString(R.string.home_title_part1) + " ")

            when (mainViewModel.genderLiveData.value) {
                "Мальчик" -> textToShow.append(context?.resources?.getString(R.string.home_title_part2_boy))
                "Девочка" -> textToShow.append(context?.resources?.getString(R.string.home_title_part2_girl))
                else -> textToShow.append(context?.resources?.getString(R.string.home_title_part2_girl))
            }

            if (!name.isNullOrBlank()) {
                textToShow.append(" $name")
            } else {
                textToShow.append(" " + context?.resources?.getString(R.string.home_title_part3))
            }

            textToShow.append("?")

            binding?.homeTitle?.text = textToShow
        }
    }

    // VIEW BINDING -> 3. onDestroyView()
    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }
}