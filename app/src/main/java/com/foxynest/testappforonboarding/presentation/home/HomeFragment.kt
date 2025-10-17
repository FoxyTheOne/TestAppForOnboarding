package com.foxynest.testappforonboarding.presentation.home

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.foxynest.testappforonboarding.R
import com.foxynest.testappforonboarding.databinding.FragmentHomeBinding
import com.foxynest.testappforonboarding.domain.entities.Gender
import com.foxynest.testappforonboarding.domain.entities.UserPresentation
import com.foxynest.testappforonboarding.other.constants.CommonConstants
import com.foxynest.testappforonboarding.presentation.MainViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

@AndroidEntryPoint
class HomeFragment : Fragment() {
    companion object {
        private const val TAG = "HomeFragment"
    }

    private var localFieldsObserver: Observer<Any>? = null

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

        homeViewModel.showWelcomeFragmentIfOnboardingIsNotComplete()

        // 1.2. ViewModel. We bind our viewModel to the lifecycle of our activity, not fragment. We pass our activity as an owner of the lifecycle.
        // So, we need to do this way:
        mainViewModel = ViewModelProvider(requireActivity())[MainViewModel::class.java]

//        initListeners()
        subscribeOnLiveData()
    }

    // Firebase будет держать user зарегистрированным даже после того, как он закроет программу и откроет заново. Поэтому нам нужно переписать метод onStart(), чтобы проверять, is user logged in in firebase or not
    override fun onStart() {
        super.onStart()
        mainViewModel.checkLoggedInStateGetUser()
    }

//    private fun initListeners() {
//
//    }

    private fun subscribeOnLiveData() {
        // 1. Подписка на пользовательские данные
        viewLifecycleOwner.lifecycleScope.launch {
            mainViewModel.checkedUserInfo
                .flowWithLifecycle(viewLifecycleOwner.lifecycle, Lifecycle.State.STARTED)
                .collect { resourceUser ->
                    if (resourceUser.data != null) {
                        removeLocalObservers()
                        // Заполняем поля исходя из данных на сервере
                        showHelloInHome(resourceUser.data)
                    } else {
                        // Локальные данные
                        setupLocalObservers()
                    }
                }
        }

        // 2. Подписка на auth state
        viewLifecycleOwner.lifecycleScope.launch {
            mainViewModel.authStateFlow
                .flowWithLifecycle(viewLifecycleOwner.lifecycle, Lifecycle.State.STARTED)
                .collect { user ->

                    if (user != null) {
                        // Пользователь авторизован. Получаем user и обновляем данные в checkedUserInfo LiveData:
                        mainViewModel.checkLoggedInStateGetUser()
                    } else {
                        // Пользователь не авторизован:
                        showHelloInHome(createUserFromLocalData())
                    }

                }
        }

        homeViewModel.openWelcomeFragmentLiveData.observe(viewLifecycleOwner) { onboardingIsNotComplete ->
            if (onboardingIsNotComplete && findNavController().currentDestination?.id == R.id.navigation_home) {
                findNavController().navigate(R.id.action_global_navigation_welcome)
            }
        }
    }

    private fun removeLocalObservers() {
        // Удаляем наблюдатель локальных полей
        localFieldsObserver?.let { observer ->
            mainViewModel.childNameLiveData.removeObserver(observer)
            // Удалить остальные наблюдатели, если есть
        }
        localFieldsObserver = null
    }

    private fun setupLocalObservers() {
        if (localFieldsObserver == null) {
            localFieldsObserver = Observer<Any> {
                showHelloInHome(createUserFromLocalData())
            }

            // Подписываемся на все необходимые LiveData
            listOf(
                mainViewModel.childNameLiveData
                // остальные LiveData, если нужны
            ).forEach { liveData ->
                liveData.observe(viewLifecycleOwner, localFieldsObserver!!)
            }
        }
    }

    private fun createUserFromLocalData(): UserPresentation {
        return UserPresentation(
            "",
            mainViewModel.emailLiveData.value ?: "",
            false,
            mainViewModel.userNameLiveData.value ?: "",
            mainViewModel.genderLiveData.value ?: "",
            mainViewModel.isItABoyLiveData.value ?: getString(R.string.no),
            mainViewModel.childNameLiveData.value ?: "",
            mainViewModel.hobbyLiveData.value ?: "",
            mainViewModel.childBirthDateLiveData.value ?: ""
        )
    }

    // Делаем метод тестируемым
    private fun showHelloInHome(user: UserPresentation) {
        binding?.homeTitle?.text = getHomeTitleText(user)
    }

    fun getHomeTitleText(user: UserPresentation): String {
        val textToShow = StringBuilder()
        textToShow.append(getString(R.string.home_title_part1) + " ")

//        when (mainViewModel.genderLiveData.value) {
        val gender = user.childGender
        Log.d(TAG, "Пол ребенка = $gender")
        when (gender) {
            Gender.BOY.genderName -> {
                textToShow.append(context?.resources?.getString(R.string.home_title_part2_boy))
                Log.d(TAG, "gender = $gender, мы в ветке ${Gender.BOY.genderName}")
            }

            Gender.GIRL.genderName -> {
                textToShow.append(context?.resources?.getString(R.string.home_title_part2_girl))
                Log.d(TAG, "gender = $gender, мы в ветке ${Gender.GIRL.genderName}")
            }

            else -> {
                textToShow.append(context?.resources?.getString(R.string.home_title_part2_boy))
                Log.d(TAG, "Пол ребенка = $gender , мы в ветке else")
            }
        }

        if (user.childName.isNotBlank() && user.childName != "null") {
            textToShow.append(" ${user.childName}")
        } else {
            textToShow.append(" " + getString(R.string.home_title_part3))
        }

        textToShow.append("?")

        return textToShow.toString()
    }

    // VIEW BINDING -> 3. onDestroyView()
    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }
}