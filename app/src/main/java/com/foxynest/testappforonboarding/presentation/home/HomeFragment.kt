package com.foxynest.testappforonboarding.presentation.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.foxynest.testappforonboarding.R
import com.foxynest.testappforonboarding.databinding.FragmentHomeBinding
import com.foxynest.testappforonboarding.domain.entities.Gender
import com.foxynest.testappforonboarding.domain.entities.UserPresentation
import com.foxynest.testappforonboarding.presentation.MainViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.lang.StringBuilder

@AndroidEntryPoint
class HomeFragment : Fragment() {
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

        // 1.2. ViewModel. We bind our viewModel to the lifecycle of our activity, not fragment. We pass our activity as an owner of the lifecycle.
        // So, we need to do this way:
        mainViewModel = ViewModelProvider(requireActivity())[MainViewModel::class.java]

//        // Firebase: При инициализации Activity проверьте, вошел ли пользователь в систему
//        mainViewModel.checkLoggedInStateGetUser()

        initListeners()
        subscribeOnLiveData()
    }

    // Firebase будет держать user зарегистрированным даже после того, как он закроет программу и откроет заново. Поэтому нам нужно переписать метод onStart(), чтобы проверять, is user logged in in firebase or not
    override fun onStart() {
        super.onStart()
        mainViewModel.checkLoggedInStateGetUser()
    }

    private fun initListeners() {
//        homeViewModel.text.observe(viewLifecycleOwner) {
//            binding?.textHome?.text = it
//        }
    }

    private fun subscribeOnLiveData() {
//        mainViewModel.childNameLiveData.observe(viewLifecycleOwner) { childName ->
//            showHelloInHome(
//                UserPresentation(
//                    "",
//                    mainViewModel.emailLiveData.value ?: "",
//                    false,
//                    mainViewModel.userNameLiveData.value ?: "",
//                    mainViewModel.genderLiveData.value ?: "",
//                    mainViewModel.isItABoyLiveData.value ?: "",
//                    childName,
//                    mainViewModel.hobbyLiveData.value ?: "",
//                    mainViewModel.childAgeLiveData.value ?: 0,
//                    mainViewModel.childBirthDateLiveData.value ?: ""
//                )
//            )
//        }

//        // Ответ на разовый запрос, получаем User
//        viewLifecycleOwner.lifecycleScope.launch {
//            mainViewModel.checkedUserInfo.collect { resourceUser ->
//                resourceUser.data?.let { nonNullUser ->
//                    // Заполняем поля исходя из данных на сервере
//                    showHelloInHome(nonNullUser)
//                }
//            }
//        }

        viewLifecycleOwner.lifecycleScope.launch {
            mainViewModel.checkedUserInfo.collect { resourceUser ->
                val user = resourceUser.data
                if (user != null) {
                    // Удаляем наблюдатель локальных полей
                    localFieldsObserver?.let { observer ->
                        mainViewModel.childNameLiveData.removeObserver(observer)
                        // Удалить остальные наблюдатели, если есть
                    }
                    localFieldsObserver = null

                    // Заполняем поля исходя из данных на сервере
                    showHelloInHome(user)
                } else {
                    // Создаем наблюдатель только если его еще нет
                    if (localFieldsObserver == null) {
                        localFieldsObserver = Observer<Any> {
                            showHelloInHome(
                                UserPresentation(
                                    "",
                                    mainViewModel.emailLiveData.value ?: "",
                                    false,
                                    mainViewModel.userNameLiveData.value ?: "",
                                    mainViewModel.genderLiveData.value ?: "",
                                    mainViewModel.isItABoyLiveData.value ?: "",
                                    mainViewModel.childNameLiveData.value ?: "",
                                    mainViewModel.hobbyLiveData.value ?: "",
//                                    mainViewModel.childAgeLiveData.value ?: 0,
                                    mainViewModel.childBirthDateLiveData.value ?: ""
                                )
                            )
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
            }
        }

        // Подписка на состояние AuthState
        mainViewModel.authStateFlow.onEach { user ->
            if (user != null) {
                // Пользователь авторизован:
                mainViewModel.checkLoggedInStateGetUser() // Получаем user и обновляем данные в checkedUserInfo LiveData
            } else {
                // Пользователь не авторизован:
                showHelloInHome(
                    UserPresentation(
                        "",
                        mainViewModel.emailLiveData.value ?: "",
                        false,
                        mainViewModel.userNameLiveData.value ?: "",
                        mainViewModel.genderLiveData.value ?: "",
                        mainViewModel.isItABoyLiveData.value ?: "",
                        mainViewModel.childNameLiveData.value ?: "",
                        mainViewModel.hobbyLiveData.value ?: "",
//                        mainViewModel.childAgeLiveData.value ?: 0,
                        mainViewModel.childBirthDateLiveData.value ?: ""
                    )
                )
            }
        }.launchIn(lifecycleScope)
    }

    private fun showHelloInHome(user: UserPresentation) {
        val textToShow = StringBuilder()

        textToShow.append(context?.resources?.getString(R.string.home_title_part1) + " ")

        when (mainViewModel.genderLiveData.value) {
            Gender.BOY.toString() -> textToShow.append(context?.resources?.getString(R.string.home_title_part2_boy))
            Gender.GIRL.toString() -> textToShow.append(context?.resources?.getString(R.string.home_title_part2_girl))
            else -> textToShow.append(context?.resources?.getString(R.string.home_title_part2_boy))
        }

        if (!user.childName.isNullOrBlank()) {
            textToShow.append(" ${user.childName}")
        } else {
            textToShow.append(" " + context?.resources?.getString(R.string.home_title_part3))
        }

        textToShow.append("?")

        binding?.homeTitle?.text = textToShow
    }

    // VIEW BINDING -> 3. onDestroyView()
    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }
}