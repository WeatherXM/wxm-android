package com.weatherxm.ui.devicesrewards

import android.os.Bundle
import com.mapbox.maps.extension.style.expressions.dsl.generated.has
import com.weatherxm.R
import com.weatherxm.databinding.ActivityDevicesRewardsBinding
import com.weatherxm.ui.common.Contracts
import com.weatherxm.ui.common.DevicesRewards
import com.weatherxm.ui.common.parcelable
import com.weatherxm.ui.common.visible
import com.weatherxm.ui.components.BaseActivity
import com.weatherxm.util.Rewards.formatTokens
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf

class DevicesRewardsActivity : BaseActivity() {
    private val model: DevicesRewardsViewModel by viewModel {
        parametersOf(intent.parcelable<DevicesRewards>(Contracts.ARG_DEVICES_REWARDS))
    }
    private lateinit var binding: ActivityDevicesRewardsBinding

    lateinit var adapter: DeviceRewardsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDevicesRewardsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        binding.buyStationBtn.setOnClickListener {
            navigator.openWebsite(this, getString(R.string.shop_url))
        }

        val hasDevices = model.rewards.devices.isNotEmpty()

        if (hasDevices) {
            binding.totalEarnedStationsTitle.text = resources.getQuantityString(
                R.plurals.total_earned_stations,
                model.rewards.devices.size,
                model.rewards.devices.size
            )
            binding.totalEarnedStations.text =
                getString(R.string.wxm_amount, formatTokens(model.rewards.total))
            binding.lastRun.text = getString(R.string.reward, formatTokens(model.rewards.latest))
            binding.mainContainer.visible(true)
            binding.emptyRewardsCard.visible(model.rewards.total == 0F)

            adapter = DeviceRewardsAdapter { _, _ ->
                // TODO: Expanded/collapsed use a map holding the info and populate it
            }
            binding.devicesRecycler.adapter = adapter
            adapter.submitList(model.rewards.devices)
        } else {
            binding.noStationsContainer.visible(true)
        }
    }
}
