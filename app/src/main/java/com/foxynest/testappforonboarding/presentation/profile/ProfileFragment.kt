package com.foxynest.testappforonboarding.presentation.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import com.foxynest.testappforonboarding.databinding.FragmentProfileBinding
import com.foxynest.testappforonboarding.presentation.MainViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ProfileFragment : Fragment() {
    companion object {
        private const val TAG = "ProfileFragment"
    }

    // VIEW BINDING -> 1. Объявляем переменную. This property is only valid between onCreateView and onDestroyView
    private var binding: FragmentProfileBinding? = null

    // 1.1. ViewModel. We bind our viewModel to the cycle of our activity, not fragment. So, we need to do this way:
    private lateinit var mainViewModel: MainViewModel
    private val profileViewModel by viewModels<ProfileViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // VIEW BINDING -> 2. Инициализация
        binding = FragmentProfileBinding.inflate(inflater, container, false)
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
        binding?.textGenderAnswerIsItABoy?.text = "Нет"

//        binding?.buttonGoToProfile?.setOnClickListener {
//            if (this.findNavController().currentDestination?.id == R.id.navigation_paywall) {
//                this.findNavController()
//                    .navigate(R.id.action_navigation_paywall_to_navigation_profile)
//            }
//        }
    }

    private fun subscribeOnLiveData() {
        mainViewModel.genderLiveData.observe(viewLifecycleOwner) { gender ->
            binding?.textGenderAnswer?.text = gender
        }
        mainViewModel.isItABoyLiveData.observe(viewLifecycleOwner) { isItABoy ->
            binding?.textGenderAnswerIsItABoy?.text = isItABoy.toString()
        }
        mainViewModel.nameLiveData.observe(viewLifecycleOwner) { name ->
            binding?.textNameAnswer?.text = name
        }
        mainViewModel.hobbyLiveData.observe(viewLifecycleOwner) { hobby ->
            binding?.textHobbyAnswer?.text = hobby
        }
    }

    // VIEW BINDING -> 3. onDestroyView()
    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }
}