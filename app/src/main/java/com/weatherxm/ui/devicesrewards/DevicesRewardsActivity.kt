package com.weatherxm.ui.devicesrewards

import android.os.Bundle
import androidx.compose.foundation.layout.Arrangement.spacedBy
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.weatherxm.R
import com.weatherxm.analytics.AnalyticsService
import com.weatherxm.databinding.ActivityDevicesRewardsBinding
import com.weatherxm.ui.common.Contracts
import com.weatherxm.ui.common.DevicesRewards
import com.weatherxm.ui.common.Status
import com.weatherxm.ui.common.classSimpleName
import com.weatherxm.ui.common.invisible
import com.weatherxm.ui.common.parcelable
import com.weatherxm.ui.common.visible
import com.weatherxm.ui.components.BaseActivity
import com.weatherxm.ui.components.compose.LargeText
import com.weatherxm.util.NumberUtils.formatTokens
import com.weatherxm.util.initTotalEarnedChart
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

        binding.totalEarnedRangeSelector.listener {
            model.getDevicesRewardsByRangeTotals(it)
        }

        model.onRewardsByRange().observe(this) {
            when (it.status) {
                Status.SUCCESS -> {
                    binding.totalEarnedStatus.visible(false)
                    binding.retryCard.visible(false)
                    binding.totalEarnedRangeSelector.enable()
                    binding.totalEarned.text =
                        getString(R.string.wxm_amount, formatTokens(it.data?.total))
                    it.data?.let { data ->
                        binding.totalEarnedChart.initTotalEarnedChart(
                            data.lineChartData,
                            data.datesChartTooltip
                        )
                    }
                    binding.totalEarned.visible(true)
                    binding.totalEarnedChart.visible(true)
                }
                Status.ERROR -> {
                    onError()
                }
                Status.LOADING -> {
                    binding.totalEarnedRangeSelector.disable()
                    binding.totalEarnedChart.invisible()
                    binding.totalEarned.invisible()
                    binding.retryCard.visible(false)
                    binding.totalEarnedStatus.clear().animation(R.raw.anim_loading).visible(true)
                }
            }
        }

        model.onDeviceRewardDetails().observe(this) {
            adapter.replaceItem(it.first, it.second)
        }

        adapter = DeviceRewardsAdapter(
            onFetchNewData = { deviceId, position, checkedRangeChipId ->
                model.getDeviceRewardsByRange(deviceId, position, checkedRangeChipId)
            },
            onCancelFetching = { position ->
                model.cancelFetching(position)
            }
        )
        binding.devicesRecycler.adapter = adapter

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

            adapter.submitList(model.rewards.devices)

            binding.totalEarnedRangeSelector.checkWeek()
            model.getDevicesRewardsByRangeTotals()
            model.getDeviceRewardsByRange(model.rewards.devices[0].id, 0)
        } else {
            binding.noStationsContainer.visible(true)
        }
    }

    override fun onResume() {
        super.onResume()
        analytics.trackScreen(AnalyticsService.Screen.REWARD_ANALYTICS, classSimpleName())
    }

    private fun onError() {
        binding.totalEarnedRangeSelector.enable()
        binding.totalEarnedChart.invisible()
        binding.totalEarned.invisible()
        binding.totalEarnedStatus.visible(false)
        binding.retryCard.setContent {
            RetryCard {
                model.getDevicesRewardsByRangeTotals(
                    binding.totalEarnedRangeSelector.checkedChipId()
                )
            }
        }
        binding.retryCard.visible(true)
    }
}

@Suppress("FunctionNaming")
@Composable
fun RetryCard(onClickListener: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = colorResource(R.color.colorSurface)
        ),
        onClick = { onClickListener() },
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(
            Modifier
                .padding(0.dp, dimensionResource(R.dimen.padding_normal_to_large))
                .fillMaxWidth(),
            verticalArrangement = spacedBy(dimensionResource(R.dimen.padding_small)),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                modifier = Modifier.padding(
                    0.dp,
                    0.dp,
                    0.dp,
                    dimensionResource(R.dimen.padding_small_to_normal)
                ),
                painter = painterResource(R.drawable.ic_retry),
                contentDescription = null
            )
            LargeText(
                text = stringResource(R.string.error_generic_message),
                fontWeight = FontWeight.Bold
            )
            LargeText(text = stringResource(R.string.tap_to_retry))
        }
    }
}
