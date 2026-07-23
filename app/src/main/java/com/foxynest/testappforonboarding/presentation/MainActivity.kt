package com.foxynest.testappforonboarding.presentation

import android.Manifest
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.ViewTreeObserver
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
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
import com.foxynest.testappforonboarding.presentation.welcome.onboardingAndPaywall.OnboardingViewModel
import com.google.android.material.bottomnavigation.BottomNavigationView
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "MainActivity"

        /**
         * Список фрагментов онбординга, где кнопка назад должна быть заблокирована
         */
        private val ONBOARDING_FRAGMENTS = setOf(
            R.id.navigation_welcome,
            R.id.navigation_login,
            R.id.navigation_register,
            R.id.navigation_question1,
            R.id.navigation_question2,
            R.id.navigation_question3,
            R.id.navigation_paywall
        )
    }

    /**
     * Список основных фрагментов приложения, где кнопка назад должна показывать диалог выхода
     */
    private val mainAppFragments = setOf(
        R.id.navigation_home,
        R.id.navigation_profile,
        R.id.navigation_useful_information
    )

    private var quitDialog: BaseDialogFragment? = null

    // VIEW BINDING -> 1. Объявляем переменную. This property is only valid between onCreateView and onDestroyView
    private var binding: ActivityMainBinding? = null

    private lateinit var navController: NavController

    private val mainViewModel by viewModels<MainViewModel>()
    private val onboardingViewModel by viewModels<OnboardingViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (resources.configuration.smallestScreenWidthDp <= 600) {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        } // Но учтите: на планшетах ориентация всё равно будет меняться – система проигнорирует ваш вызов. Это особенность Android 16, и её нельзя обойти (без использования недокументированных и запрещенных методов)

        // VIEW BINDING -> 2. Инициализация
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding!!.root)

        // Инициализируем navController
        navController = findNavController(R.id.nav_host_fragment_activity_main)

        // СНАЧАЛА настраиваем навигацию:
        setupNavigation()
        // Уберем Action bar
        supportActionBar?.hide()

//        // ПОТОМ запрашиваем данные: подтягиваем ответы из пройденного ранее онбординга. Загружаем данные БЕЗ принудительного обновления Firebase
//        mainViewModel.getAndUseAnswersFromOnboarding()
        mainViewModel.checkLoggedInStateGetUser() // -> проверяем в каждом фрагменте отдельно (home, profile, info)

        /**
         * Регистрируем обработчик кнопки "Назад"
         */
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val currentDestination = navController.currentDestination?.id

                when (currentDestination) {
                    in ONBOARDING_FRAGMENTS -> {
                        // Запрещаем возврат в онбординге – просто ничего не делаем
                        // Можно также показать тост "Действие запрещено"
                        Toast.makeText(
                            this@MainActivity,
                            R.string.forbidden_action,
                            Toast.LENGTH_SHORT
                        ).show()
                        return
                    }

                    in mainAppFragments -> {
                        // Показываем диалог подтверждения выхода
                        showQuitDialog()
                        return
                    }

                    else -> {
                        // Для остальных случаев - стандартное поведение (выход из Activity или навигация назад)
                        if (supportFragmentManager.backStackEntryCount == 0 && navController.graph.startDestinationId == currentDestination) {
                            // Если фрагментов нет или это корневой экран - закрываем Activity
                            finish()
                        } else {
                            navController.navigateUp()
                        }
                    }
                }
            }
        })

        setupNotificationPermission()
        observeOnboardingState() // Подписываемся на изменения онбординга
        subscribeToObservers()
    }

    fun updateBottomNavigationSelection(destinationId: Int) {
        binding?.navView?.menu?.findItem(destinationId)?.isChecked = true
    }

    /**
     * PERMISSIONS
     */
    private fun setupNotificationPermission() {
        // Запрос на разрешение notification
        val requestPermissionLauncherNotification =
            registerForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { isGranted: Boolean ->
                if (!isGranted) {
                    Toast.makeText(
                        this,
                        getString(R.string.no_notification_permission),
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
    }

    /**
     * ONBOARDING
     */
    private fun observeOnboardingState() {
        // Наблюдаем за заголовком онбординга
        onboardingViewModel.currentTitle.observe(this) { title ->
            binding?.onboardingTitle?.text = title
        }

        // Наблюдаем за прогрессом
        onboardingViewModel.currentStep.observe(this) { currentStep ->
            onboardingViewModel.totalSteps.observe(this) { totalSteps ->
                if (totalSteps > 0) {
                    val progress = (currentStep.toFloat() / totalSteps.toFloat()) * 100
                    binding?.onboardingProgress?.progress = progress.toInt()

                    // Можно добавить отображение текста прогресса
                    // binding?.progressText?.text = onboardingViewModel.getProgressText()
                }
            }
        }
    }

    private fun updateOnboardingTitle(destinationId: Int) {
        val title = when (destinationId) {
            R.id.navigation_question1 -> getString(R.string.onboarding_container_title)
            R.id.navigation_question2 -> getString(R.string.onboarding_container_title)
            R.id.navigation_question3 -> getString(R.string.onboarding_container_title)
            else -> getString(R.string.onboarding_tittle)
        }
        onboardingViewModel.updateTitle(title)
    }

    /**
     * NAVIGATION
     */
    private fun setupNavigation() {
        // Bottom navigation setup
        val navView: BottomNavigationView = binding!!.navView

        // Passing each menu ID as a set of Ids because each menu should be considered as top level destinations.
        val appBarConfiguration = AppBarConfiguration(mainAppFragments)
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

        // Fix для правильного выделения пунктов меню
        navView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_home -> {
                    if (navController.currentDestination?.id != R.id.navigation_home) {
                        navController.navigate(R.id.navigation_home)
                    }
                    true
                }

                R.id.navigation_profile -> {
                    if (navController.currentDestination?.id != R.id.navigation_profile) {
                        navController.navigate(R.id.navigation_profile)
                    }
                    true
                }

                R.id.navigation_useful_information -> {
                    if (navController.currentDestination?.id != R.id.navigation_useful_information) {
                        navController.navigate(R.id.navigation_useful_information)
                    }
                    true
                }

                else -> false
            }
        }

        // Let's add a listener to our NavController to hide BottomBar when we are on the onboarding and paywall pages
        // Слушатель для скрытия/показа BottomNavigationView и управления онбордингом
        navController.addOnDestinationChangedListener { _, destination, _ ->
            val destId = destination.id

            // Управление видимостью элементов онбординга
            if (destId in ONBOARDING_FRAGMENTS) {
                binding?.navView?.visibility = View.GONE

                // Показываем заголовок и прогресс-бар для фрагментов онбординга (кроме приветствия)
                if (destId != R.id.navigation_welcome
                    && destId != R.id.navigation_login
                    && destId != R.id.navigation_register
                    && destId != R.id.navigation_paywall
                ) {
                    binding?.onboardingTitle?.visibility = View.VISIBLE
                    binding?.onboardingProgress?.visibility = View.VISIBLE

                    // Обновляем состояние онбординга
                    onboardingViewModel.updateProgress(destId)

                    // Устанавливаем заголовок для текущего фрагмента
                    updateOnboardingTitle(destId)
                } else {
                    binding?.onboardingTitle?.visibility = View.GONE
                    binding?.onboardingProgress?.visibility = View.GONE
                }
            } else {
                binding?.navView?.visibility = View.VISIBLE
                binding?.onboardingTitle?.visibility = View.GONE
                binding?.onboardingProgress?.visibility = View.GONE
                updateBottomNavigationSelection(destId)
            }
        }

        // Отступ у контента в случае появления bottomNavigationView
        setupContentPadding()
    }

    /**
     * DIALOGS
     */
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

    /**
     * OTHER
     */
    private fun subscribeToObservers() {
        mainViewModel.errorMessageLiveData.observe(this) { event ->
            val toastMessage = event.getContentIfNotHandled()?.data?.toErrorMessage(resources)
            Toast.makeText(this, toastMessage, Toast.LENGTH_LONG).show()
        }
    }

    private fun setupContentPadding() {
        val bottomNavigationView = binding?.navView
        val mainContent = binding?.flFragmentContainer
        bottomNavigationView?.viewTreeObserver?.addOnGlobalLayoutListener(object :
            ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                if (mainContent != null) {
                    val bottomBarHeight = bottomNavigationView.height
                    val params = mainContent.layoutParams as ConstraintLayout.LayoutParams
                    params.bottomMargin = bottomBarHeight
                    mainContent.layoutParams = params
                    bottomNavigationView.viewTreeObserver.removeOnGlobalLayoutListener(this)
                }
            }
        })
    }

    // VIEW BINDING -> 3. onDestroyView()
    override fun onDestroy() {
        binding = null
        super.onDestroy()
    }
}