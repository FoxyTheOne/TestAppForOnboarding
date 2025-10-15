package com.foxynest.testappforonboarding.presentation

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.ViewTreeObserver
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.foxynest.testappforonboarding.R
import com.foxynest.testappforonboarding.databinding.ActivityMainBinding
import com.foxynest.testappforonboarding.other.extensions.toErrorMessage
import com.foxynest.testappforonboarding.presentation.dialogs.simpleDialogs.BaseDialogFragment
import com.foxynest.testappforonboarding.presentation.dialogs.simpleDialogs.QuitDialog
import com.google.android.material.bottomnavigation.BottomNavigationView
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "MainActivity"
    }

    private var quitDialog: BaseDialogFragment? = null

    // VIEW BINDING -> 1. Объявляем переменную. This property is only valid between onCreateView and onDestroyView
    private var binding: ActivityMainBinding? = null

    private val navController: NavController by lazy {
        findNavController(R.id.nav_host_fragment_activity_main)
    }
    private val mainViewModel by viewModels<MainViewModel>() // Такую же view model мы зарегистрировали в онбординге и в профиле, куда должны подтягиваться результаты онбординга. Основная ViewModel.

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // VIEW BINDING -> 2. Инициализация
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view: View = binding!!.root
        setContentView(view)

        // Подтягиваем ответы из пройденного ранее онбординга
        mainViewModel.getAndUseAnswersFromOnboardind()
//        // Firebase: При инициализации Activity проверьте, вошел ли пользователь в систему
//        mainViewModel.checkLoggedInState() -> проверяем в BaseWelcomeFragmentAbstract при запуске и в каждом фрагменте отдельно

        // Уберем Action bar
        supportActionBar?.hide()

        // Bottom navigation setup
        val navView: BottomNavigationView = binding!!.navView
//        val navController = findNavController(R.id.nav_host_fragment_activity_main)

        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        val appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.navigation_home, R.id.navigation_profile, R.id.navigation_useful_information
            )
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

        // Let's add a listener to our NavController to hide BottomBar when we are on the onboarding and paywall pages
        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.navigation_welcome -> navView.visibility = View.GONE
                R.id.navigation_login -> navView.visibility = View.GONE
                R.id.navigation_register -> navView.visibility = View.GONE
                R.id.navigation_question1 -> navView.visibility = View.GONE
                R.id.navigation_question2 -> navView.visibility = View.GONE
                R.id.navigation_question3 -> navView.visibility = View.GONE
                R.id.navigation_paywall -> navView.visibility = View.GONE
                R.id.navigation_profile -> navView.visibility = View.VISIBLE
                R.id.navigation_home -> navView.visibility = View.VISIBLE
                else -> navView.visibility = View.VISIBLE
            }
        }

        // Отступ у контента в случае появления bottomNavigationView
        val bottomNavigationView = binding?.navView
        val mainContent = binding?.flFragmentContainer
        bottomNavigationView?.viewTreeObserver?.addOnGlobalLayoutListener(object :
            ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                if (mainContent != null) {
                    val bottomBarHeight = bottomNavigationView.height
                    // Применить отступ к mainContent
                    val params = mainContent.layoutParams as ConstraintLayout.LayoutParams
                    params.bottomMargin = bottomBarHeight
                    mainContent.layoutParams = params

                    bottomNavigationView.viewTreeObserver.removeOnGlobalLayoutListener(this)
                }
            }
        })

        // Запрос на разрешение notification
        val requestPermissionLauncherNotification =
            registerForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { isGranted: Boolean ->
                if (!isGranted) {
                    Toast.makeText(
                        this,
                        "We don't have permission to show notifications on your Android",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // Если нет разрешения - вызываем requestPermissionLauncher
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                requestPermissionLauncherNotification.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

//        initListeners()
        subscribeToObservers()
    }

//    private fun initListeners() {
//
//    }

    private fun subscribeToObservers() {
        mainViewModel.errorMessageLiveData.observe(this) { event ->
            val toastMessage = event.getContentIfNotHandled()?.data?.toErrorMessage(resources)
            Toast.makeText(this, toastMessage, Toast.LENGTH_LONG).show()
        }
    }

    override fun onBackPressed() {
//        // !!! Всплывающее окно "вы действительно хотите выйти?" перед закрытием приложения вместо запрета слушателя кнопки назад
//        if (navController.currentDestination?.id == R.id.navigation_welcome ||
//            navController.currentDestination?.id == R.id.navigation_login ||
//            navController.currentDestination?.id == R.id.navigation_register ||
//            navController.currentDestination?.id == R.id.navigation_question1 ||
//            navController.currentDestination?.id == R.id.navigation_question2 ||
//            navController.currentDestination?.id == R.id.navigation_question3 ||
//            navController.currentDestination?.id == R.id.navigation_paywall
//        ) {
//            // Запрет возврата с paywall экрана
//            return
//        }
//        super.onBackPressed()

        val currentDestination = navController.currentDestination?.id

        // Список фрагментов онбординга, где кнопка назад должна быть заблокирована
        val onboardingFragments = setOf(
            R.id.navigation_welcome,
            R.id.navigation_login,
            R.id.navigation_register,
            R.id.navigation_question1,
            R.id.navigation_question2,
            R.id.navigation_question3,
            R.id.navigation_paywall
        )

        // Список основных фрагментов приложения, где кнопка назад должна показывать диалог выхода
        val mainAppFragments = setOf(
            R.id.navigation_home,
            R.id.navigation_profile,
            R.id.navigation_useful_information
        )

        when (currentDestination) {
            in onboardingFragments -> {
                // Запрещаем возврат в онбординге
                return
            }

            in mainAppFragments -> {
                // Показываем диалог подтверждения выхода в основном приложении
                showQuitDialog()
                return
            }

            else -> {
                // Для остальных случаев - стандартное поведение
                super.onBackPressed()
            }
        }

//        В онбординге:
//        Welcome, Login, Register, Question1-3, Paywall - кнопка "Назад" полностью заблокирована
//        Пользователь не может случайно выйти во время заполнения опроса
//
//        В основном приложении:
//        Home, Profile, Useful Information - кнопка "Назад" показывает диалог подтверждения
//        Пользователь воспринимает нижнее меню как вкладки, поэтому выход всегда через диалог
//        Если пользователь нажимает "Да" - приложение закрывается
//        Если "Нет" - диалог закрывается, пользователь остается в приложении
//
//        Особенности реализации:
//        finishAffinity() - полностью закрывает приложение, а не только активность
//        Проверка isAdded - предотвращает показ нескольких диалогов одновременно
//        Обнуление ссылки - после закрытия диалога ссылка обнуляется для предотвращения утечек памяти
    }

    private fun showQuitDialog() {
        if (quitDialog != null && quitDialog!!.isAdded) {
            return
        }

        quitDialog = QuitDialog.newInstance()
            .setListeners(
                positiveAction = {
                    finishAffinity()
                },
                negativeAction = {
                    quitDialog = null
                }
            )

        quitDialog?.show(supportFragmentManager, QuitDialog.TAG)
    }

    override fun onDestroy() {
        binding = null // VIEW BINDING -> 3. onDestroyView()
        super.onDestroy()
    }
}