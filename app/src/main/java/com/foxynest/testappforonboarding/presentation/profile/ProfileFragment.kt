package com.foxynest.testappforonboarding.presentation.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.foxynest.testappforonboarding.R
import com.foxynest.testappforonboarding.databinding.FragmentProfileBinding
import com.foxynest.testappforonboarding.domain.entities.Gender
import com.foxynest.testappforonboarding.domain.entities.language.LanguageEnum
import com.foxynest.testappforonboarding.domain.entities.user.UserPresentation
import com.foxynest.testappforonboarding.domain.entities.user.isValidData
import com.foxynest.testappforonboarding.domain.entities.user.toUserPresentation
import com.foxynest.testappforonboarding.other.Status
import com.foxynest.testappforonboarding.other.constants.CommonConstants.HOBBIES_DELIMITER
import com.foxynest.testappforonboarding.other.constants.CommonConstants.PROFILE_DESTINATION
import com.foxynest.testappforonboarding.other.extensions.calculateAge
import com.foxynest.testappforonboarding.other.extensions.parseDateToLocalDate
import com.foxynest.testappforonboarding.other.extensions.safeNavigate
import com.foxynest.testappforonboarding.presentation.MainActivity
import com.foxynest.testappforonboarding.presentation.MainViewModel
import com.foxynest.testappforonboarding.presentation.common.HobbyDisplay
import com.foxynest.testappforonboarding.presentation.common.displayName
import com.foxynest.testappforonboarding.presentation.common.toErrorMessage
import com.foxynest.testappforonboarding.presentation.common.yesNoDisplayName
import com.foxynest.testappforonboarding.presentation.dialogs.ChangeAuthEmailDialog
import com.foxynest.testappforonboarding.presentation.dialogs.DataSaveErrorDialog
import com.foxynest.testappforonboarding.presentation.dialogs.DeleteAccountDialog
import com.foxynest.testappforonboarding.presentation.dialogs.simpleDialogs.ConfirmOnboardingRestartDialog
import com.foxynest.testappforonboarding.presentation.welcome.onboardingAndPaywall.BaseOnboardingFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/**
 * Профиль: ответы анкеты, язык, почта и всё про аккаунт (вход, регистрация, выход, удаление, смена почты, подтверждение почты).
 *
 * Две ViewModel:
 * - MainViewModel (`activityViewModels`) – общая для всех экранов: профиль, выход, удаление аккаунта;
 * - ProfileViewModel (`viewModels`) – только этого экрана: язык и подтверждение почты.
 *
 * Подписки на StateFlow – через `repeatOnLifecycle(STARTED)` ([collectWhileStarted]): пока экран не виден,
 * сбор останавливается, и snapshot-листенер Firestore в MainViewModel может отключиться.
 */
@AndroidEntryPoint
class ProfileFragment : Fragment() {

    // VIEW BINDING -> 1. Объявляем переменную. This property is only valid between onCreateView and onDestroyView
    private var binding: FragmentProfileBinding? = null

    // MainViewModel привязана к Activity: общая для всех экранов
    private val mainViewModel: MainViewModel by activityViewModels()
    private val profileViewModel by viewModels<ProfileViewModel>()

    // Переменная для метода setupLanguageSwitcher() при выборе radiobutton для избежания вечного цикла
    private var currentLang: LanguageEnum? = null

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

        setupLanguageSwitcher()

        // Принудительно обновляем выделение в BottomNavigationView
        (activity as? MainActivity)?.updateBottomNavigationSelection(R.id.navigation_profile)

        // СНАЧАЛА подписываемся на observers
        subscribeOnObservers()

        // ПОТОМ инициализируем listeners
        initListeners()

        renderCurrentUser()
    }

    override fun onResume() {
        super.onResume()

        // Разовая проверка: пользователь мог подтвердить почту, пока приложение было в фоне
        profileViewModel.checkEmailVerificationStatus()
    }

    private fun initListeners() {
        binding?.buttonVerifyEmail?.setOnClickListener {
            profileViewModel.sendVerificationEmail()
        }

        binding?.buttonLogIn?.setOnClickListener {
            safeNavigate(
                R.id.navigation_profile,
                ProfileFragmentDirections.actionNavigationProfileToLoginFragment(PROFILE_DESTINATION)
            )
        }

        binding?.buttonRegisterAndLogin?.setOnClickListener {
            // Анкету экран регистрации возьмёт из MainViewModel
            safeNavigate(
                R.id.navigation_profile,
                R.id.action_navigation_profile_to_registerFragment
            )
        }

        binding?.buttonLogOut?.setOnClickListener {
            mainViewModel.logout()
        }

        binding?.buttonDeleteAccount?.setOnClickListener {
            if (childFragmentManager.findFragmentByTag(DeleteAccountDialog.TAG) == null) {
                DeleteAccountDialog().show(childFragmentManager, DeleteAccountDialog.TAG)
            }
        }

        binding?.buttonRefillFields?.setOnClickListener {
            ConfirmOnboardingRestartDialog().show(
                childFragmentManager,
                ConfirmOnboardingRestartDialog.TAG
            )
        }

        binding?.buttonChangeAuthEmail?.setOnClickListener {
            if (childFragmentManager.findFragmentByTag(ChangeAuthEmailDialog.TAG) == null) {
                val currentEmail = binding?.textAuthEmailAnswer?.text?.toString().orEmpty()
                ChangeAuthEmailDialog.newInstance(currentEmail)
                    .show(childFragmentManager, ChangeAuthEmailDialog.TAG)
            }
        }

        // Результат диалога приходит и после пересоздания экрана (Fragment Result API)
        ConfirmOnboardingRestartDialog.setResultListener(
            childFragmentManager,
            viewLifecycleOwner
        ) { confirmed ->
            if (confirmed) {
                val args = Bundle().apply {
                    putString(
                        BaseOnboardingFragment.SOURCE_FRAGMENT_KEY,
                        "profile"
                    )
                }
                safeNavigate(R.id.navigation_profile, R.id.navigation_question1, args)
            }
        }
    }

    private fun subscribeOnObservers() {
        // Профиль из Firestore (snapshot-листенер в MainViewModel)
        collectWhileStarted {
            mainViewModel.checkedUserInfo.collect { renderCurrentUser() }
        }

        // Локальная анкета – для незарегистрированного пользователя
        mainViewModel.onboardingAnswersLiveData.observe(viewLifecycleOwner) { renderCurrentUser() }

        collectWhileStarted {
            profileViewModel.isEmailVerified.collect { updateEmailVerificationUI(it) }
        }

        profileViewModel.emailVerifiedEvent.observe(viewLifecycleOwner) { event ->
            if (event.getContentIfNotHandled() == true) {
                Toast.makeText(
                    requireContext(),
                    R.string.profile_email_verified_successfully,
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        collectWhileStarted {
            profileViewModel.isVerificationButtonEnabled.collect { updateVerificationButton(it) }
        }

        profileViewModel.verificationEmailSentEvent.observe(viewLifecycleOwner) { event ->
            if (event.getContentIfNotHandled() != null) {
                Toast.makeText(
                    requireContext(),
                    R.string.profile_email_verification_send,
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        mainViewModel.logoutEvent.observe(viewLifecycleOwner) { event ->
            val result = event.getContentIfNotHandled() ?: return@observe
            if (result.status == Status.SUCCESS) {
                navigateToWelcome()
            } else {
                Toast.makeText(
                    requireContext(),
                    result.data.toErrorMessage(resources),
                    Toast.LENGTH_LONG
                ).show()
            }
        }

        mainViewModel.deleteAccountState.observe(viewLifecycleOwner) { state ->
            if (state.status == Status.SUCCESS) {
                mainViewModel.resetDeleteAccountState()
                Toast.makeText(requireContext(), R.string.account_deleted, Toast.LENGTH_LONG).show()
                navigateToWelcome()
            }
        }

        mainViewModel.profileUpdatedEvent.observe(viewLifecycleOwner) { event ->
            val result = event.getContentIfNotHandled() ?: return@observe
            if (result.status == Status.ERROR &&
                childFragmentManager.findFragmentByTag(DataSaveErrorDialog.TAG) == null
            ) {
                DataSaveErrorDialog().show(childFragmentManager, DataSaveErrorDialog.TAG)
            }
        }
    }

    private fun collectWhileStarted(block: suspend () -> Unit) {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) { block() }
        }
    }

    private fun renderCurrentUser() {
        val remoteUser = mainViewModel.checkedUserInfo.value.data
        if (remoteUser != null) {
            fillFieldsInProfile(remoteUser)
            showLogOutButtons(true)
        } else {
            mainViewModel.onboardingAnswersLiveData.value?.toUserPresentation()
                ?.let(::fillFieldsInProfile)
            showLogOutButtons(false)
        }
    }

    private fun navigateToWelcome() {
        safeNavigate(R.id.navigation_profile, R.id.action_navigation_profile_to_navigation_welcome2)
    }

    private fun updateEmailVerificationUI(isVerified: Boolean?) {
        val hasEmail = mainViewModel.checkedUserInfo.value.data?.authEmail?.isNotBlank() == true
        // Мягкая верификация: ненавязчивая плашка с кнопкой, пользоваться приложением можно и без подтверждения
        val showBanner = isVerified == false && hasEmail
        binding?.buttonVerifyEmail?.visibility = if (showBanner) View.VISIBLE else View.GONE
        binding?.textEmailNotVerified?.visibility = if (showBanner) View.VISIBLE else View.GONE
    }

    private fun updateVerificationButton(isEnabled: Boolean) {
        val context = context ?: return
        binding?.buttonVerifyEmail?.isEnabled = isEnabled
        binding?.buttonVerifyEmail?.setTextColor(
            ContextCompat.getColor(context, if (isEnabled) R.color.white else R.color.gray)
        )
        binding?.textEmailNotVerified?.text = getString(
            if (isEnabled) R.string.profile_email_not_verified else R.string.profile_email_verification_send
        )
    }

    private fun fillFieldsInProfile(user: UserPresentation) {
        if (!user.isValidData()) return
        val context = requireContext()

        binding?.textUserNameAnswer?.text = user.userName
        binding?.textGenderAnswer?.text =
            Gender.genderFromString(user.childGender).displayName(context)
        binding?.textGenderAnswerIsItABoy?.text = yesNoDisplayName(context, user.isItABoy)
        binding?.textChildNameAnswer?.text = user.childName
        binding?.textHobbyAnswer?.text = user.childHobbies
            .split(HOBBIES_DELIMITER)
            .joinToString("\n") { HobbyDisplay.displayName(context, it) }

        val age = user.childBirthDate.parseDateToLocalDate()?.calculateAge()?.data
        binding?.textChildAgeAnswer?.text = age?.toString().orEmpty()
        binding?.textBirthDateAnswer?.text =
            profileViewModel.formatBirthDateForDisplay(user.childBirthDate, context)
        binding?.textAuthEmailAnswer?.text = user.authEmail
    }

    private fun showLogOutButtons(isUserLoggedIn: Boolean) {
        val loggedIn = if (isUserLoggedIn) View.VISIBLE else View.GONE
        val loggedOut = if (isUserLoggedIn) View.GONE else View.VISIBLE
        binding?.apply {
            buttonLogIn.visibility = loggedOut
            buttonRegisterAndLogin.visibility = loggedOut
            textWarning.visibility = loggedOut
            textWarningData.visibility = loggedOut

            buttonLogOut.visibility = loggedIn
            buttonDeleteAccount.visibility = loggedIn
            textAuthEmailTitle.visibility = loggedIn
            linearAuthEmailAnswer.visibility = loggedIn
            linearEmailVerification.visibility = loggedIn
        }
        if (isUserLoggedIn) updateEmailVerificationUI(profileViewModel.isEmailVerified.value)
    }

    private fun setupLanguageSwitcher() {
        var isUpdatingProgrammatically = false

        // viewLifecycleOwner, а не lifecycleScope фрагмента: фрагмент в back stack переживает свою View,
        // и каждый onViewCreated добавлял ещё одного вечного подписчика (утечка корутин)
        collectWhileStarted {
            profileViewModel.currentLanguageFlow.collect { lang ->
                isUpdatingProgrammatically = true
                binding?.apply {
                    when (lang) {
                        LanguageEnum.RU -> rbRussian.isChecked = true
                        LanguageEnum.EN -> rbEnglish.isChecked = true
                        else -> rbSystem.isChecked = true
                    }
                }
                currentLang = lang
                isUpdatingProgrammatically = false
            }
        }
        binding?.rgLanguage?.setOnCheckedChangeListener { _, checkedId ->
            if (isUpdatingProgrammatically) return@setOnCheckedChangeListener
            val newLang = when (checkedId) {
                R.id.rbSystem -> LanguageEnum.SYSTEM
                R.id.rbRussian -> LanguageEnum.RU
                R.id.rbEnglish -> LanguageEnum.EN
                else -> return@setOnCheckedChangeListener
            }
            if (newLang != currentLang) {
                profileViewModel.changeLanguage(newLang)
                currentLang = newLang
            }
        }
    }

    // VIEW BINDING -> 3. onDestroyView()
    override fun onDestroyView() {
        binding = null
        super.onDestroyView()
    }
}