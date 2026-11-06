package com.foxynest.testappforonboarding.presentation.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.foxynest.testappforonboarding.R
import com.foxynest.testappforonboarding.databinding.FragmentHomeBinding
import com.foxynest.testappforonboarding.domain.entities.Gender
import com.foxynest.testappforonboarding.domain.entities.user.UserPresentation
import com.foxynest.testappforonboarding.domain.entities.user.toUserPresentation
import com.foxynest.testappforonboarding.presentation.MainActivity
import com.foxynest.testappforonboarding.presentation.MainViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/**
 * Главный экран: приветствие с именем ребёнка.
 * Данные берёт из MainViewModel: для вошедшего – профиль из Firestore, иначе – анкету с устройства.
 */
@AndroidEntryPoint
class HomeFragment : Fragment() {

    // VIEW BINDING -> 1. Объявляем переменную. This property is only valid between onCreateView and onDestroyView
    private var binding: FragmentHomeBinding? = null

    // MainViewModel привязана к Activity: общая для всех экранов
    private val mainViewModel: MainViewModel by activityViewModels()

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

        // Принудительно обновляем выделение в BottomNavigationView
        (activity as? MainActivity)?.updateBottomNavigationSelection(R.id.navigation_home)

//        initListeners()
        subscribeOnLiveData()
    }

//    private fun initListeners() {
//
//    }

    private fun subscribeOnLiveData() {
        // Профиль из Firestore (snapshot-листенер) – для вошедшего пользователя
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                mainViewModel.checkedUserInfo.collect { render() }
            }
        }
        // Локальная анкета – для незарегистрированного
        mainViewModel.onboardingAnswersLiveData.observe(viewLifecycleOwner) { render() }
    }

    private fun render() {
        val user = mainViewModel.checkedUserInfo.value.data
            ?: mainViewModel.onboardingAnswersLiveData.value?.toUserPresentation()
            ?: return
        binding?.homeTitle?.text = getHomeTitleText(user)
    }

    private fun getHomeTitleText(user: UserPresentation): String {
        val childName = user.childName.ifBlank { getString(R.string.home_title_part3) }
        return when (Gender.genderFromString(user.childGender)) {
            Gender.BOY -> getString(R.string.home_title_all_boy, childName)
            Gender.GIRL -> getString(R.string.home_title_all_girl, childName)
        }
    }

    // VIEW BINDING -> 3. onDestroyView()
    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }
}