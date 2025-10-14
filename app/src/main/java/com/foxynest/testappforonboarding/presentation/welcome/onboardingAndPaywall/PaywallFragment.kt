package com.foxynest.testappforonboarding.presentation.welcome.onboardingAndPaywall

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.CountDownTimer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.foxynest.testappforonboarding.R
import com.foxynest.testappforonboarding.databinding.FragmentPaywallBinding
import com.foxynest.testappforonboarding.domain.entities.VisibilityEnum
import com.foxynest.testappforonboarding.other.constants.CommonConstants
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

    // 1.1. ViewModel. We bind our viewModel to the cycle of our activity, not fragment. So, we need to do this way:
    private lateinit var mainViewModel: MainViewModel
    private val paywallViewModel by viewModels<PaywallViewModel>()

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

        // 1.2. ViewModel. We bind our viewModel to the lifecycle of our activity, not fragment. We pass our activity as an owner of the lifecycle.
        // So, we need to do this way:
        mainViewModel = ViewModelProvider(requireActivity())[MainViewModel::class.java]

        paywallViewModel.saveOnboardingState(true)
        showProgressBarAnimationBeforeShowingContent()

        // Подтягиваем ответы из пройденного только что опросника. В дальнейшем метод будет вызываться в activity при запуске программы
        mainViewModel.getAndUseAnswersFromOnboardind()

        initListeners()
        subscribeOnLiveData()
    }

    private fun showProgressBarAnimationBeforeShowingContent() {
        hideOrShowContent(VisibilityEnum.GONE)

        val splashTimeOut = 3000
        binding?.progressBarHorizontal?.max = splashTimeOut

        val countDownTimer: CountDownTimer =
            object : CountDownTimer(splashTimeOut.toLong(), (splashTimeOut / 100).toLong()) {
                override fun onTick(millisUntilFinished: Long) {
                    binding?.progressBarHorizontal?.progress =
                        (splashTimeOut - millisUntilFinished).toInt()
                }

                override fun onFinish() {
                    hideOrShowContent(VisibilityEnum.VISIBLE)
                }
            }

        countDownTimer.start()
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
//        mainViewModel.text.observe(viewLifecycleOwner) {
//            binding?.textQuestion1?.text = it
//        }

        binding?.imageClose?.setOnClickListener {
            if (this.findNavController().currentDestination?.id == R.id.navigation_paywall) {
                this.findNavController()
                    .navigate(R.id.action_navigation_paywall_to_navigation_profile)
            }
        }

        binding?.buttonGoToProfile?.setOnClickListener {
            if (this.findNavController().currentDestination?.id == R.id.navigation_paywall) {
                this.findNavController()
                    .navigate(R.id.action_navigation_paywall_to_navigation_profile)
            }
        }

        binding?.buttonPaywall?.setOnClickListener {
            val browserIntent =
                Intent(Intent.ACTION_VIEW, Uri.parse("https://boosty.to/foxynest/donate"))
            startActivity(browserIntent)
        }
    }

    private fun subscribeOnLiveData() {
        mainViewModel.genderLiveData.observe(viewLifecycleOwner) { gender ->
            when (gender) {
                "Мальчик" -> binding?.textPaywallAppeal?.text = "У вас замечательный мальчик"
                "Девочка" -> binding?.textPaywallAppeal?.text = "У вас замечательная девочка"
                else -> Unit
            }
        }

        // TODO Повторить бы то же самое в профиле - вдруг этот фрагмент уже будет закрыт. Но там нет связи с этой LiveData
        // Думаю, что это не критично = ошибка этого сохранения маловероятно. К тому же, это нужно только для не зарегистрированных пользователей и там есть предупреждение о том, что данные могут быть утеряны без регистрации
        // (потому что данные из data store всё равно могут быть стёрты при очистке кэша)
        paywallViewModel.errorMessageLiveData.observe(viewLifecycleOwner) { myEvent ->
            if (myEvent.getContentIfNotHandled()?.message == CommonConstants.FAILED_TO_SAVE_ONBOARDING_STATE_LOCAL) {
                showRepeatSavingOnboardingStateDialog()
            }
        }
    }

    private fun showRepeatSavingOnboardingStateDialog() {
        RepeatSavingOnboardingStateDialog.newInstance()
            .setListeners(
                positiveAction = {
                    // Пользователь подтвердил - запускаем заново сохранение
                    paywallViewModel.saveOnboardingState(true)

                },
                negativeAction = {
                    // Пользователь отменил
                    Toast.makeText(requireContext(), R.string.local_onboarding_saving_error_toast, Toast.LENGTH_SHORT).show()
                }
            )
            .show(parentFragmentManager, ConfirmOnboardingRestartDialog.TAG)
    }

    // VIEW BINDING -> 3. onDestroyView()
    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }
}