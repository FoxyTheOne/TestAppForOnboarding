package com.foxynest.testappforonboarding.presentation.welcome.onboardingAndPaywall

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.foxynest.testappforonboarding.R
import com.foxynest.testappforonboarding.databinding.FragmentQuestion4Binding
import com.foxynest.testappforonboarding.other.constants.CommonConstants
import com.foxynest.testappforonboarding.other.extensions.safeNavigate
import com.foxynest.testappforonboarding.presentation.common.HobbyDisplay
import com.google.android.material.chip.Chip
import dagger.hilt.android.AndroidEntryPoint

/**
 * Вопрос 4: хобби (чипы). Список ключей – res/values/arrays.xml → hobbies_array, текст – HobbyDisplay.
 * «Готово» сохраняет хобби и завершает опрос (finishOnboarding).
 */
@AndroidEntryPoint
class Question4Fragment : BaseOnboardingFragment() {

    private var binding: FragmentQuestion4Binding? = null
    override val currentFragmentId: Int = R.id.navigation_question4

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentQuestion4Binding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initChips()
        initListeners()
        restoreSelectedHobbies()
    }

    private fun initChips() {
        val hobbyKeys = resources.getStringArray(R.array.hobbies_array)
        hobbyKeys.forEach { key ->
            val chip =
                layoutInflater.inflate(R.layout.item_chip, binding?.chipGroupHobbies, false) as Chip
            chip.text = HobbyDisplay.displayName(requireContext(), key)
            chip.tag = key
            chip.isCheckable = true
            binding?.chipGroupHobbies?.addView(chip)
        }
    }

    private fun restoreSelectedHobbies() {
        val savedHobbies = mainViewModel.onboardingDraftLiveData.value?.childHobbies ?: ""
        if (savedHobbies.isNotBlank()) {
            val selectedKeys = savedHobbies.split(CommonConstants.HOBBIES_DELIMITER)
            for (i in 0 until (binding?.chipGroupHobbies?.childCount ?: 0)) {
                val chip = binding?.chipGroupHobbies?.getChildAt(i) as? Chip
                chip?.let {
                    val key = it.tag as? String
                    if (key != null && key in selectedKeys) {
                        it.isChecked = true
                    }
                }
            }
        }
    }

    private fun initListeners() {
        binding?.buttonCancel?.setOnClickListener {
            showCancelConfirmationDialog()
        }

        binding?.buttonFinish?.setOnClickListener {
            val selectedHobbies = getSelectedHobbies()

            if (selectedHobbies.isEmpty()) {
                showError(true)
            } else {
                showError(false)
                val hobbiesString = selectedHobbies.joinToString(CommonConstants.HOBBIES_DELIMITER)
                mainViewModel.updateChildHobbies(hobbiesString)

                // Сохранение ответов и отправка в Firebase – в finishOnboarding() строго после этой записи
                mainViewModel.finishOnboarding()

                safeNavigate(
                    R.id.navigation_question4,
                    R.id.action_navigation_question4_to_paywallFragment,
                    sourceArgs()
                )
            }
        }

        binding?.chipGroupHobbies?.setOnCheckedStateChangeListener { _, _ ->
            showError(false)
        }
    }

    private fun getSelectedHobbies(): List<String> {
        val selectedKeys = mutableListOf<String>()
        for (i in 0 until (binding?.chipGroupHobbies?.childCount ?: 0)) {
            val chip = binding?.chipGroupHobbies?.getChildAt(i) as? Chip
            if (chip?.isChecked == true) {
                val key = chip.tag as? String
                key?.let { selectedKeys.add(it) }
            }
        }
        return selectedKeys
    }

    private fun showError(show: Boolean) {
        binding?.textErrorHobbies?.visibility = if (show) View.VISIBLE else View.GONE
    }

    override fun onDestroyView() {
        binding = null
        super.onDestroyView()
    }

}