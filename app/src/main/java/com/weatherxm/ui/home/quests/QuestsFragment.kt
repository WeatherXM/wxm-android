package com.weatherxm.ui.home.quests

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.firebase.auth.FirebaseAuth
import com.weatherxm.R
import com.weatherxm.analytics.AnalyticsService
import com.weatherxm.databinding.FragmentQuestsBinding
import com.weatherxm.ui.common.DevicesRewards
import com.weatherxm.ui.common.visible
import com.weatherxm.ui.components.BaseFragment
import com.weatherxm.ui.components.compose.QuestDailyCard
import com.weatherxm.ui.components.compose.QuestOnboardingCard
import com.weatherxm.ui.components.compose.QuestsPageSelector
import com.weatherxm.ui.home.devices.DevicesViewModel
import com.weatherxm.util.NumberUtils.formatTokens
import dev.chrisbanes.insetter.applyInsetter
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.activityViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel
import timber.log.Timber

class QuestsFragment : BaseFragment() {
    private val devicesModel: DevicesViewModel by activityViewModel()
    private val model: QuestsViewModel by viewModel()
    private val firebaseAuth: FirebaseAuth by inject()
    private lateinit var binding: FragmentQuestsBinding

    @SuppressLint("NotifyDataSetChanged")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentQuestsBinding.inflate(inflater, container, false)

        binding.root.applyInsetter {
            type(statusBars = true) {
                padding(left = false, top = true, right = false, bottom = false)
            }
        }

        devicesModel.onDevicesRewards().observe(viewLifecycleOwner) {
            onDevicesRewards(it)
        }

        model.onError().observe(viewLifecycleOwner) {
            if (it != null) {
                binding.loading.visible(false)
                binding.swiperefresh.isRefreshing = false
                showSnackbarMessage(
                    binding.root,
                    it.message ?: getString(R.string.error_generic_message),
                    { model.getData() },
                    R.string.action_retry,
                    null
                )
            }
        }

        model.onDataLoaded().observe(viewLifecycleOwner) {
            binding.loading.visible(false)
            binding.swiperefresh.isRefreshing = false
            toggleOnboardingVisibility(model.onQuestToggleOption.value)
            model.onboardingQuestData?.let {
                binding.onboardingQuest.setContent {
                    QuestOnboardingCard(it) {
                        // TODO: Handle this click
                    }
                }
            }
        }

        binding.swiperefresh.setOnRefreshListener {
            getFirebaseUserAndFetchData()
        }

        binding.dailyQuestCard.setContent { QuestDailyCard() }

        binding.questsPageSelector.setContent {
            QuestsPageSelector(model.onQuestToggleOption) {
                model.onQuestToggleOption.value = it
                binding.dailyQuestCard.visible(it == 0)
                toggleOnboardingVisibility(it)
                binding.questsTitle.text = if (it == 0) {
                    getString(R.string.quests)
                } else {
                    getString(R.string.completed_quests)
                }
            }
        }

        return binding.root
    }

    private fun getFirebaseUserAndFetchData() {
        if(!binding.swiperefresh.isRefreshing) {
            binding.loading.visible(true)
        }
        if (model.user != null) {
            Timber.d("We already have the user data: ${model.user?.uid}")
            model.getData()
        } else if (firebaseAuth.currentUser != null) {
            Timber.d("User data exists on firebase: ${firebaseAuth.currentUser?.uid}")
            model.user = firebaseAuth.currentUser
            model.getData()
        } else {
            Timber.d("Starting Firebase Anonymous Authentication...")
            firebaseAuth.signInAnonymously().addOnCompleteListener { task ->
                if (task.isSuccessful && task.result.user != null) {
                    Timber.d("Success on anonymous login via Firebase: ${task.result.user?.uid}")
                    model.user = task.result.user
                    model.getData()
                } else {
                    Timber.w(task.exception, "Error on anonymous login via Firebase")
                    showSnackbarMessage(
                        binding.root,
                        task.exception?.message ?: getString(R.string.error_generic_message),
                        { getFirebaseUserAndFetchData() },
                        R.string.action_retry,
                        null
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        getFirebaseUserAndFetchData()
    }

    private fun onDevicesRewards(rewards: DevicesRewards) {
        binding.totalEarnedCard.visible(true)
        binding.totalEarnedCard.setOnClickListener {
            analytics.trackEventUserAction(
                AnalyticsService.ParamValue.TOKENS_EARNED_PRESSED.paramValue
            )
            navigator.showDevicesRewards(this, rewards)
        }
        binding.stationRewards.text = getString(R.string.wxm_amount, formatTokens(rewards.total))
        binding.totalEarnedContainer.visible(rewards.total > 0F)
        binding.noRewardsYet.visible(rewards.devices.isNotEmpty() && rewards.total == 0F)
        binding.ownDeployEarn.visible(rewards.devices.isEmpty() && rewards.total == 0F)
    }

    private fun toggleOnboardingVisibility(currentQuestPage: Int) {
        binding.onboardingQuest.visible(
            (currentQuestPage == 0 && model.onboardingQuestData?.isCompleted == false) ||
                    (currentQuestPage == 1 && model.onboardingQuestData?.isCompleted == true)
        )
    }
}
