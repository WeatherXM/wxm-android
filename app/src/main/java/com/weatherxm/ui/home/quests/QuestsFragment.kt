package com.weatherxm.ui.home.quests

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import com.google.firebase.auth.FirebaseAuth
import com.weatherxm.R
import com.weatherxm.analytics.AnalyticsService
import com.weatherxm.databinding.FragmentQuestsBinding
import com.weatherxm.ui.common.DevicesRewards
import com.weatherxm.ui.common.visible
import com.weatherxm.ui.components.BaseFragment
import com.weatherxm.ui.components.compose.DailyQuestCard
import com.weatherxm.ui.home.devices.DevicesViewModel
import com.weatherxm.util.NumberUtils.formatTokens
import dev.chrisbanes.insetter.applyInsetter
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.activityViewModel
import timber.log.Timber

class QuestsFragment : BaseFragment() {

    private val devicesModel: DevicesViewModel by activityViewModel()
    private val model: QuestsViewModel by viewModels()
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

        binding.swiperefresh.setOnRefreshListener {
            getFirebaseUserAndFetchData()
        }

        binding.dailyQuestCard.setContent { DailyQuestCard() }

        getFirebaseUserAndFetchData()

        return binding.root
    }

    private fun getFirebaseUserAndFetchData() {
        if (model.user != null) {
            Timber.d("We already have the user data: ${model.user?.uid}")
            // TODO: Fetch user's progress as we have the user already
        } else if (firebaseAuth.currentUser != null) {
            Timber.d("User data exists on firebase: ${firebaseAuth.currentUser?.uid}")
            model.user = firebaseAuth.currentUser
            // TODO: Fetch user's progress as we have the user already
        } else {
            Timber.d("Starting Firebase Anonymous Authentication...")
            firebaseAuth.signInAnonymously().addOnCompleteListener { task ->
                if (task.isSuccessful && task.result.user != null) {
                    Timber.d("Success on anonymous login via Firebase: ${task.result.user?.uid}")
                    model.user = task.result.user
                    // TODO: Fetch user's progress now that we have the user
                } else {
                    Timber.w(task.exception, "Error on anonymous login via Firebase")
                    // TODO: Show a snackbar with the retry function.
                }
            }
        }
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
}
