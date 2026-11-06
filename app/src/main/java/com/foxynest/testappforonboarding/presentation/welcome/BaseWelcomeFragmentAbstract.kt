package com.foxynest.testappforonboarding.presentation.welcome

import androidx.fragment.app.Fragment
import dagger.hilt.android.AndroidEntryPoint

/**
 * Базовый фрагмент для расширения фрагментами с онбордингом.
 * Содержит логику перехода сразу на content, если в предыдущий раз пользователь полностью прошёл онбординг - перенесла логику в HOME
 *
 * Сейчас класс пустой: решение «куда идти при запуске» принимает SplashViewModel.
 * Оставлен как общий родитель для Welcome/Login/Registration/Paywall – можно убрать, если общей логики так и не появится.
 */
@AndroidEntryPoint
abstract class BaseWelcomeFragmentAbstract : Fragment() {
    companion object {
        private const val TAG = "BaseOnboardingFragmentAbstract"
    }

//    // Воспользуемся global navigation actions, чотбы открывать сразу content, если выполнено условие
//    @Inject
//    lateinit var appPreferences: IAppDatastorePreferences
//
//    @Inject
//    lateinit var getCurrentLogInStateUC: GetCurrentUserUC
//
////    override fun onCreate(savedInstanceState: Bundle?) {
////        super.onCreate(savedInstanceState)
////
////        lifecycleScope.launch {
////            // Проверка авторизации при запуске. Регистрация возможна только после прохождения опроса, значит если человек залогинен - опрос пройден
////            if (getCurrentLogInStateUC.execute().data != null) {
////                if (findNavController().currentDestination?.id == R.id.navigation_welcome) {
////                    findNavController().navigate(R.id.action_global_navigation_home)
////                }
////            } else {
////                // Если user = null, значит not logged in
////                appPreferences.isOnboardingCompleted()
////                    .collect { isCompleted ->
////                        Log.d(TAG, "Проверка, пройден ли онбординг = $isCompleted")
////                        if (isCompleted) {
//////                        findNavController().navigate(R.id.action_global_navigation_home) // Срабатывает моментально, а нам это нужно только для последующих запусков, не в первый запуск. Не универсально, не подходит
////                            if (findNavController().currentDestination?.id == R.id.navigation_welcome) {
////                                findNavController().navigate(R.id.action_global_navigation_home)
////                            }
////                        }
////                    }
////            }
////        }
////
////    }
//
//    // Ждем пока View будет полностью создано
//    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
//        super.onViewCreated(view, savedInstanceState)
//
//        viewLifecycleOwner.lifecycleScope.launch {
//            // Проверка авторизации при запуске. Регистрация возможна только после прохождения опроса, значит если человек залогинен - опрос пройден
//            if (getCurrentLogInStateUC.execute().data != null) {
//                // Проверяем текущую destination перед навигацией
//                if (findNavController().currentDestination?.id == R.id.navigation_welcome) {
//                    findNavController().navigate(R.id.action_global_navigation_home)
//                }
//            } else {
//                // Если user = null, значит not logged in
//                appPreferences.isOnboardingCompleted()
//                    .collect { isCompleted ->
//                        Log.d(TAG, "Проверка, пройден ли онбординг = $isCompleted")
//                        if (isCompleted && findNavController().currentDestination?.id == R.id.navigation_welcome) {
//                            findNavController().navigate(R.id.action_global_navigation_home)
//                        }
//                    }
//            }
//        }
//
//    }
}