package com.weatherxm.ui.rewardboosts

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import coil.ImageLoader
import coil.request.ImageRequest
import com.weatherxm.R
import com.weatherxm.data.BoostReward
import com.weatherxm.data.Status
import com.weatherxm.databinding.ActivityRewardBoostBinding
import com.weatherxm.ui.common.Contracts.ARG_BOOST_REWARD
import com.weatherxm.ui.common.Contracts.ARG_DATE
import com.weatherxm.ui.common.Contracts.ARG_DEVICE_ID
import com.weatherxm.ui.common.UIBoost
import com.weatherxm.ui.common.applyInsets
import com.weatherxm.ui.common.parcelable
import com.weatherxm.ui.common.setBoostFallbackBackground
import com.weatherxm.ui.common.setVisible
import com.weatherxm.ui.common.toast
import com.weatherxm.ui.components.BaseActivity
import com.weatherxm.util.Analytics
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

        model.onBoostReward().observe(this) {
            when (it.status) {
                Status.SUCCESS -> {
                    it.data?.let { data ->
                        binding.amountDesc.text = getString(R.string.boost_tokens_earned, date)
                        updateUI(data)
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

    @SuppressLint("SetTextI18n")
    private fun updateUI(data: UIBoost) {
        if (data.imgUrl.isEmpty()) {
            binding.boostCard.setBoostFallbackBackground()
        } else {
            imageLoader.enqueue(
                ImageRequest.Builder(this)
                    .data(data.imgUrl)
                    .target(binding.backgroundImage)
                    .build()
            )
            binding.backgroundImage.setVisible(true)
        }

        binding.title.text = data.title
        binding.amount.text = getString(R.string.wxm_amount, data.actualReward)
        binding.dailyBoostScore.text = "${data.boostScore}%"
        binding.lostRewards.text = getString(R.string.boost_tokens_lost, data.lostRewards)
        binding.boostDetailsDesc.text = data.boostDesc
        binding.boostDetailsDesc.setVisible(data.boostDesc.isNotEmpty())
        binding.boostAboutDesc.text = data.about
        binding.aboutReadMore.setOnClickListener {
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
        analytics.trackScreen(Analytics.Screen.REWARD_BOOST_DETAIL, this::class.simpleName)
    }
}
