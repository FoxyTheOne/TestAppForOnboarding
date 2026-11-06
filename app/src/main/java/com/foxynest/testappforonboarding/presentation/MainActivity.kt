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
import com.foxynest.testappforonboarding.presentation.common.toErrorMessage
import com.foxynest.testappforonboarding.presentation.dialogs.simpleDialogs.NotificationPermissionDialog
import com.foxynest.testappforonboarding.presentation.dialogs.simpleDialogs.QuitDialog
import com.foxynest.testappforonboarding.presentation.welcome.onboardingAndPaywall.OnboardingViewModel
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint

/**
 * Единственная Activity приложения. Экраны – фрагменты внутри неё (Navigation Component, граф в res/navigation).
 *
 * Что делает сама Activity:
 * - нижнее меню и его скрытие на экранах опроса/входа;
 * - заголовок и прогресс опроса;
 * - кнопка «Назад»: в опросе запрещена, на главных экранах – вопрос «выйти?»;
 * - разрешение на уведомления (по сигналу из MainViewModel) и сообщение «сессия завершена».
 *
 * `@AndroidEntryPoint` – разрешает Hilt внедрять зависимости сюда и во фрагменты этой Activity.
 */
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

    // Регистрируется при создании Activity – так требует Activity Result API
    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (!isGranted) {
                Toast.makeText(this, R.string.no_notification_permission, Toast.LENGTH_LONG).show()
            }
        }

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

        setupNavigation()
        // Уберем Action bar
        supportActionBar?.hide()

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

        // Будильник пропадает после принудительной остановки приложения, восстановления из резервной копии
        // на новом телефоне и т.п. Поэтому при каждом запуске перепланируем (операция идемпотентна).
        // Если сегодня день рождения и ещё не поздравляли – пользователь в приложении, поздравляем сразу.
        if (savedInstanceState == null) {
            mainViewModel.onAppStarted()
        }

        registerDialogResults()
        observeOnboardingState() // Подписываемся на изменения онбординга
        subscribeToObservers()
    }

    fun updateBottomNavigationSelection(destinationId: Int) {
        binding?.navView?.menu?.findItem(destinationId)?.isChecked = true
    }

    /**
     * ONBOARDING
     */
    private fun observeOnboardingState() {
        // Наблюдаем за заголовком онбординга
        onboardingViewModel.currentTitle.observe(this) { title ->
            binding?.onboardingTitle?.text = title
        }

        // Наблюдаем за прогрессом. Раньше observe(totalSteps) вызывался внутри observe(currentStep):
        // каждый шаг добавлял нового наблюдателя, и они копились до уничтожения Activity
        onboardingViewModel.currentStep.observe(this) { updateOnboardingProgress() }
        onboardingViewModel.totalSteps.observe(this) { updateOnboardingProgress() }
    }

    private fun updateOnboardingProgress() {
        val currentStep = onboardingViewModel.currentStep.value ?: 0
        val totalSteps = onboardingViewModel.totalSteps.value ?: 0
        if (totalSteps > 0) {
            binding?.onboardingProgress?.progress = currentStep * 100 / totalSteps
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
        if (supportFragmentManager.findFragmentByTag(QuitDialog.TAG) == null) {
            QuitDialog().show(supportFragmentManager, QuitDialog.TAG)
        }
    }

    private fun registerDialogResults() {
        // Fragment Result API: результат доходит и после пересоздания Activity
        QuitDialog.setResultListener(supportFragmentManager, this) { confirmed ->
            if (confirmed) finishAffinity()
        }
        NotificationPermissionDialog.setResultListener(supportFragmentManager, this) { confirmed ->
            if (confirmed) notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    /**
     * PERMISSIONS
     * Разрешение спрашиваем не на старте, а в контексте: после того как пользователь указал дату рождения
     * (или при запуске, если анкета уже заполнена), с объяснением зачем – и только один раз.
     */
    private fun showNotificationPermissionPromptIfNeeded() {
        mainViewModel.markNotificationPermissionAsked()
        val isRuntimePermissionRequired = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED
        if (isRuntimePermissionRequired && supportFragmentManager.findFragmentByTag(
                NotificationPermissionDialog.TAG
            ) == null
        ) {
            NotificationPermissionDialog().show(
                supportFragmentManager,
                NotificationPermissionDialog.TAG
            )
        }
    }

    /**
     * OTHER
     */
    private fun subscribeToObservers() {
        mainViewModel.errorMessageLiveData.observe(this) { event ->
            // Раньше показывался пустой тост, если событие уже обработано
            val toastMessage =
                event.getContentIfNotHandled()?.data?.toErrorMessage(resources) ?: return@observe
            Toast.makeText(this, toastMessage, Toast.LENGTH_LONG).show()
        }

        mainViewModel.askNotificationPermissionEvent.observe(this) { event ->
            if (event.getContentIfNotHandled() != null) showNotificationPermissionPromptIfNeeded()
        }

        // Firebase завершил сессию сам: чаще всего после подтверждения новой почты для входа
        mainViewModel.sessionEndedEvent.observe(this) { event ->
            if (event.getContentIfNotHandled() == null) return@observe
            MaterialAlertDialogBuilder(this)
                .setMessage(R.string.session_ended_message)
                .setPositiveButton(R.string.ok, null)
                .show()
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