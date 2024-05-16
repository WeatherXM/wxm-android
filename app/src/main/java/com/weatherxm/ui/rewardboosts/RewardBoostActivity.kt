package com.weatherxm.ui.rewardboosts

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import coil.ImageLoader
import com.google.firebase.analytics.FirebaseAnalytics
import com.weatherxm.R
import com.weatherxm.analytics.AnalyticsService
import com.weatherxm.data.BoostCode
import com.weatherxm.data.BoostReward
import com.weatherxm.data.Status
import com.weatherxm.databinding.ActivityRewardBoostBinding
import com.weatherxm.ui.common.Contracts.ARG_BOOST_REWARD
import com.weatherxm.ui.common.Contracts.ARG_DATE
import com.weatherxm.ui.common.Contracts.ARG_DEVICE_ID
import com.weatherxm.ui.common.UIBoost
import com.weatherxm.ui.common.applyInsets
import com.weatherxm.ui.common.classSimpleName
import com.weatherxm.ui.common.empty
import com.weatherxm.ui.common.loadImage
import com.weatherxm.ui.common.parcelable
import com.weatherxm.ui.common.setBoostFallbackBackground
import com.weatherxm.ui.common.setVisible
import com.weatherxm.ui.common.toast
import com.weatherxm.ui.components.BaseActivity
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf
import timber.log.Timber

class RewardBoostActivity : BaseActivity() {
    private lateinit var binding: ActivityRewardBoostBinding
    private val model: RewardBoostViewModel by viewModel {
        parametersOf(intent?.extras?.getString(ARG_DEVICE_ID))
    }

    private val imageLoader: ImageLoader by inject()

    private var boostCode: String? = String.empty()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRewardBoostBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.root.applyInsets()

        val boostReward = intent?.extras?.parcelable<BoostReward>(ARG_BOOST_REWARD)
        val date = intent?.extras?.getString(ARG_DATE)
        if (boostReward == null || model.deviceId.isEmpty()) {
            Timber.d("Could not start RewardBoostActivity. Boost Reward or DeviceId is empty.")
            toast(R.string.error_generic_message)
            finish()
            return
        }
        boostCode = boostReward.code

        binding.toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        model.onBoostReward().observe(this) {
            when (it.status) {
                Status.SUCCESS -> {
                    it.data?.let { data ->
                        binding.amountDesc.text = getString(R.string.boost_tokens_earned, date)
                        updateUI(boostReward.code, data)
                    } ?: onFetchError(boostReward, getString(R.string.error_reach_out))
                }
                Status.ERROR -> {
                    onFetchError(boostReward, it.message)
                }
                Status.LOADING -> {
                    binding.statusView.clear().animation(R.raw.anim_loading)
                    binding.mainContainer.setVisible(false)
                    binding.statusView.setVisible(true)
                }
            }
        }

        model.fetchRewardBoost(boostReward)
    }

    @Suppress("MagicNumber")
    @SuppressLint("SetTextI18n")
    private fun updateUI(boostCode: String?, data: UIBoost) {
        binding.boostCard.setBoostFallbackBackground()
        if (data.imgUrl.isNotEmpty()) {
            binding.backgroundImage.loadImage(imageLoader, data.imgUrl)
            binding.backgroundImage.setVisible(true)
        }

        binding.title.text = data.title
        binding.amount.text = getString(R.string.reward, data.actualReward)
        if (BoostCode.beta_rewards.name == boostCode && data.boostScore != null) {
            binding.dailyBoostScore.text = "${data.boostScore}%"
            binding.rewardsDesc.text = if (data.boostScore == 100) {
                getString(R.string.got_all_beta_rewards)
            } else {
                getString(R.string.boost_tokens_lost, data.lostRewards)
            }
        } else {
            binding.rewardsDesc.setVisible(false)
            binding.divider.setVisible(false)
            binding.dailyBoostScoreTitle.setVisible(false)
            binding.dailyBoostScore.setVisible(false)
        }
        binding.boostDetailsDesc.text = data.boostDesc
        binding.boostDetailsDesc.setVisible(data.boostDesc.isNotEmpty())
        binding.boostAboutDesc.text = data.about
        binding.aboutReadMore.setOnClickListener {
            analytics.trackEventSelectContent(
                AnalyticsService.ParamValue.WEB_DOCUMENTATION.paramValue,
                Pair(FirebaseAnalytics.Param.ITEM_ID, data.docUrl)
            )
            navigator.openWebsite(this, data.docUrl)
        }
        binding.aboutReadMore.setVisible(data.docUrl.isNotEmpty())

        val detailsAdapter = RewardBoostDetailAdapter()
        binding.detailsRecycler.adapter = detailsAdapter
        detailsAdapter.submitList(data.details)

        binding.statusView.setVisible(false)
        binding.mainContainer.setVisible(true)
    }

    private fun onFetchError(boostReward: BoostReward, subtitle: String?) {
        binding.statusView.clear()
            .animation(R.raw.anim_error)
            .title(getString(R.string.error_boost_reward_title))
            .subtitle(subtitle)
            .action(getString(R.string.action_retry))
            .listener {
                model.fetchRewardBoost(boostReward)
            }
        binding.mainContainer.setVisible(false)
        binding.statusView.visibility = View.VISIBLE
    }

    override fun onResume() {
        super.onResume()
        analytics.trackScreen(
            AnalyticsService.Screen.REWARD_BOOST_DETAIL, classSimpleName(), boostCode
        )
    }
}
