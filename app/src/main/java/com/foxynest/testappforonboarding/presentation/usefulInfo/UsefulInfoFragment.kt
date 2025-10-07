package com.foxynest.testappforonboarding.presentation.usefulInfo

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.foxynest.testappforonboarding.databinding.FragmentUsefulInfoBinding
import com.foxynest.testappforonboarding.domain.entities.UserPresentation
import com.foxynest.testappforonboarding.presentation.MainViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

@AndroidEntryPoint
class UsefulInfoFragment : Fragment() {
    private var localFieldsObserver: Observer<Any>? = null

    // VIEW BINDING -> 1. Объявляем переменную. This property is only valid between onCreateView and onDestroyView
    private var binding: FragmentUsefulInfoBinding? = null

    // 1.1. ViewModel. We bind our viewModel to the cycle of our activity, not fragment. So, we need to do this way:
    private lateinit var mainViewModel: MainViewModel
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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1.2. ViewModel. We bind our viewModel to the lifecycle of our activity, not fragment. We pass our activity as an owner of the lifecycle.
        // So, we need to do this way:
        mainViewModel = ViewModelProvider(requireActivity())[MainViewModel::class.java]

        // TODO определить, что надо делать на этой странице и затем реализовать:
//        // Firebase: При инициализации Activity проверьте, вошел ли пользователь в систему
//        mainViewModel.checkLoggedInStateGetUser() -> переделать в onStart

//        subscribeOnLiveData()
    }

//    private fun subscribeOnLiveData() {
//        viewLifecycleOwner.lifecycleScope.launch {
//            mainViewModel.checkedUserInfo.collect { resourceUser ->
//                val user = resourceUser.data
//                if (user != null) {
//                    // Удаляем наблюдатель локальных полей
//                    localFieldsObserver?.let { observer ->
//                        mainViewModel.userNameLiveData.removeObserver(observer)
//                        // !!! Удалить остальные наблюдатели, если есть
//                    }
//                    localFieldsObserver = null
//
//                    // Заполняем поля исходя из данных на сервере
//                    fillFieldsInUsefulInfo(user)
//                } else {
//                    // Создаем наблюдатель только если его еще нет
//                    if (localFieldsObserver == null) {
//                        localFieldsObserver = Observer<Any> {
//                            fillFieldsInUsefulInfo(
//                                UserPresentation(
//                                    "",
//                                    mainViewModel.emailLiveData.value ?: "",
//                                    false,
//                                    mainViewModel.userNameLiveData.value ?: "",
//                                    mainViewModel.genderLiveData.value ?: "",
//                                    mainViewModel.isItABoyLiveData.value ?: "",
//                                    mainViewModel.childNameLiveData.value ?: "",
//                                    mainViewModel.hobbyLiveData.value ?: "",
//                                    mainViewModel.childAgeLiveData.value ?: 0,
//                                    mainViewModel.childBirthDateLiveData.value ?: ""
//                                )
//                            )
//                        }
//
//                        // Подписываемся на все необходимые LiveData
//                        listOf(
//                            mainViewModel.userNameLiveData
//                            // !!! Остальные LiveData, если нужны
//                        ).forEach { liveData ->
//                            liveData.observe(viewLifecycleOwner, localFieldsObserver!!)
//                        }
//                    }
//                }
//            }
//        }
//
//        // Подписка на состояние AuthState
//        mainViewModel.authStateFlow.onEach { user ->
//            if (user != null) {
//                // Пользователь авторизован:
//                mainViewModel.checkLoggedInStateGetUser() // Получаем user и обновляем данные в checkedUserInfo LiveData
//            } else {
//                // Пользователь не авторизован:
//                fillFieldsInUsefulInfo(
//                    UserPresentation(
//                        "",
//                        mainViewModel.emailLiveData.value ?: "",
//                        false,
//                        mainViewModel.userNameLiveData.value ?: "",
//                        mainViewModel.genderLiveData.value ?: "",
//                        mainViewModel.isItABoyLiveData.value ?: "",
//                        mainViewModel.childNameLiveData.value ?: "",
//                        mainViewModel.hobbyLiveData.value ?: "",
//                        mainViewModel.childAgeLiveData.value ?: 0,
//                        mainViewModel.childBirthDateLiveData.value ?: ""
//                    )
//                )
//            }
//        }.launchIn(lifecycleScope)
//    }

//    private fun fillFieldsInUsefulInfo(user: UserPresentation) {
//        TODO("Not yet implemented")
//    }

    // VIEW BINDING -> 3. onDestroyView()
    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }
}