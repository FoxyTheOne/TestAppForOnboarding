package com.foxynest.testappforonboarding.presentation.welcome.onboardingAndPaywall

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.CountDownTimer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.activityViewModels
import com.foxynest.testappforonboarding.R
import com.foxynest.testappforonboarding.databinding.FragmentPaywallBinding
import com.foxynest.testappforonboarding.domain.entities.Gender
import com.foxynest.testappforonboarding.domain.entities.VisibilityEnum
import com.foxynest.testappforonboarding.other.constants.CommonConstants.PAY_URL
import com.foxynest.testappforonboarding.other.extensions.safeNavigate
import com.foxynest.testappforonboarding.presentation.MainViewModel
import com.foxynest.testappforonboarding.presentation.dialogs.simpleDialogs.ConfirmOnboardingRestartDialog
import com.foxynest.testappforonboarding.presentation.dialogs.simpleDialogs.RepeatSavingOnboardingStateDialog
import com.foxynest.testappforonboarding.presentation.welcome.BaseWelcomeFragmentAbstract
import dagger.hilt.android.AndroidEntryPoint


@AndroidEntryPoint
class PaywallFragment : BaseWelcomeFragmentAbstract() {
    companion object {
        private const val TAG = "PaywallFragment"
    }

    // VIEW BINDING -> 1. Объявляем переменную. This property is only valid between onCreateView and onDestroyView
    private var binding: FragmentPaywallBinding? = null
    private var countDownTimer: CountDownTimer? = null

    // ViewModel привязана к Activity
    private val mainViewModel: MainViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // VIEW BINDING -> 2. Инициализация
        binding = FragmentPaywallBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Сохранение завершённого опроса запускается на последнем вопросе (MainViewModel.finishOnboarding)
        showProgressBarAnimationBeforeShowingContent()

        initListeners()
        subscribeOnLiveData()
    }

    private fun showProgressBarAnimationBeforeShowingContent() {
        hideOrShowContent(VisibilityEnum.GONE)

        val splashTimeOut = 3000
        binding?.progressBarHorizontal?.max = splashTimeOut

        countDownTimer?.cancel()
        countDownTimer =
            object : CountDownTimer(splashTimeOut.toLong(), (splashTimeOut / 100).toLong()) {
                override fun onTick(millisUntilFinished: Long) {
                    binding?.progressBarHorizontal?.progress =
                        (splashTimeOut - millisUntilFinished).toInt()
                }

                override fun onFinish() {
                    hideOrShowContent(VisibilityEnum.VISIBLE)
                }
            }.start()
    }

    private fun hideOrShowContent(visibility: VisibilityEnum) {
        when (visibility) {
            VisibilityEnum.VISIBLE -> {
                binding?.textAnalysing?.visibility = View.GONE
                binding?.progressBarHorizontal?.visibility = View.GONE
                binding?.imageClose?.visibility = View.VISIBLE
                binding?.titlePaywall?.visibility = View.VISIBLE
                binding?.textPaywallAppeal?.visibility = View.VISIBLE
                binding?.textPaywall?.visibility = View.VISIBLE
                binding?.buttonPaywall?.visibility = View.VISIBLE
                binding?.buttonGoToProfile?.visibility = View.VISIBLE
            }

            VisibilityEnum.GONE -> {
                binding?.textAnalysing?.visibility = View.VISIBLE
                binding?.progressBarHorizontal?.visibility = View.VISIBLE
                binding?.imageClose?.visibility = View.GONE
                binding?.titlePaywall?.visibility = View.GONE
                binding?.textPaywallAppeal?.visibility = View.GONE
                binding?.textPaywall?.visibility = View.GONE
                binding?.buttonPaywall?.visibility = View.GONE
                binding?.buttonGoToProfile?.visibility = View.GONE
            }
        }
    }

    private fun initListeners() {
        binding?.imageClose?.setOnClickListener {
            safeNavigate(
                R.id.navigation_paywall,
                R.id.action_navigation_paywall_to_navigation_profile
            )
        }

        binding?.buttonGoToProfile?.setOnClickListener {
            safeNavigate(
                R.id.navigation_paywall,
                R.id.action_navigation_paywall_to_navigation_profile
            )
        }

        binding?.buttonPaywall?.setOnClickListener {
            val browserIntent =
                Intent(Intent.ACTION_VIEW, Uri.parse(PAY_URL))
            startActivity(browserIntent)
        }
    }

    private fun subscribeOnLiveData() {
        mainViewModel.onboardingAnswersLiveData.observe(viewLifecycleOwner) { answers ->
            val genderEnum = Gender.genderFromString(answers.childGender)
            when (genderEnum) {
                Gender.BOY -> binding?.textPaywallAppeal?.text =
                    getString(R.string.paywall_boy_text)

                Gender.GIRL -> binding?.textPaywallAppeal?.text =
                    getString(R.string.paywall_girl_text)
            }
        }

        // Раньше этот путь был недостижим: репозиторий сам ловил IOException, а ViewModel ждала исключение
        mainViewModel.finishOnboardingErrorLiveData.observe(viewLifecycleOwner) { event ->
            if (event.getContentIfNotHandled() != null) {
                showRepeatSavingOnboardingStateDialog()
            }
        }
    }

    private fun showRepeatSavingOnboardingStateDialog() {
        RepeatSavingOnboardingStateDialog.newInstance()
            .setListeners(
                positiveAction = {
                    // Черновик не удалён, поэтому повтор безопасен
                    mainViewModel.finishOnboarding()
                },
                negativeAction = {
                    // Пользователь отменил
                    Toast.makeText(
                        requireContext(),
                        R.string.local_onboarding_saving_error_toast,
                        Toast.LENGTH_SHORT
                    ).show()
                }
            )
            .show(parentFragmentManager, ConfirmOnboardingRestartDialog.TAG)
    }

    // VIEW BINDING -> 3. onDestroyView()
    override fun onDestroyView() {
        // Иначе таймер ещё 3 секунды держит ссылку на уничтоженный фрагмент
        countDownTimer?.cancel()
        countDownTimer = null
        super.onDestroyView()
        binding = null
    }
}