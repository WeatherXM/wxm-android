package com.weatherxm.ui.devicedetails.rewards

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.ColorRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.weatherxm.R
import com.weatherxm.data.Status
import com.weatherxm.databinding.FragmentDeviceDetailsRewardsBinding
import com.weatherxm.ui.common.RewardsWeeklyStreak
import com.weatherxm.ui.common.empty
import com.weatherxm.ui.common.setVisible
import com.weatherxm.ui.components.BaseFragment
import com.weatherxm.ui.devicedetails.DeviceDetailsViewModel
import com.weatherxm.util.Analytics
import com.weatherxm.util.Rewards.formatTokens
import com.weatherxm.util.Rewards.getRewardScoreColor
import org.koin.androidx.viewmodel.ext.android.activityViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf

class RewardsFragment : BaseFragment() {
    private lateinit var binding: FragmentDeviceDetailsRewardsBinding
    private val parentModel: DeviceDetailsViewModel by activityViewModel()
    private val model: RewardsViewModel by viewModel {
        parametersOf(parentModel.device)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentDeviceDetailsRewardsBinding.inflate(inflater, container, false)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        parentModel.onFollowStatus().observe(viewLifecycleOwner) {
            if (it.status == Status.SUCCESS) {
                model.device = parentModel.device
                model.fetchRewardsFromNetwork()
            }
        }

        model.onRewards().observe(viewLifecycleOwner) {
            binding.dailyRewardsCard.updateUI(it.latest) {
                navigator.showRewardDetails(requireContext(), model.device, it.latest)
            }
            val totalRewards = it.allTimeRewards ?: 0F
            binding.totalRewards.text =
                getString(R.string.wxm_amount, formatTokens(totalRewards.toBigDecimal()))
            updateWeeklyStreak(it.weekly)
            binding.dailyRewardsCard.setVisible(true)
            binding.totalCard.setVisible(true)
            binding.weeklyCard.setVisible(true)
        }

        model.onLoading().observe(viewLifecycleOwner) {
            if (it && binding.swiperefresh.isRefreshing) {
                binding.progress.visibility = View.INVISIBLE
            } else if (it) {
                binding.totalCard.setVisible(false)
                binding.dailyRewardsCard.setVisible(false)
                binding.weeklyCard.setVisible(false)
                binding.progress.visibility = View.VISIBLE
            } else {
                binding.swiperefresh.isRefreshing = false
                binding.progress.visibility = View.INVISIBLE
            }
        }

        model.onError().observe(viewLifecycleOwner) {
            binding.totalCard.setVisible(false)
            binding.dailyRewardsCard.setVisible(false)
            binding.weeklyCard.setVisible(false)
            showSnackbarMessage(binding.root, it.errorMessage, it.retryFunction)
        }

        binding.swiperefresh.setOnRefreshListener {
            model.fetchRewardsFromNetwork()
        }

        binding.viewTimeline.setOnClickListener {
            navigator.showRewardsList(requireContext(), model.device)
        }

        model.fetchRewardsFromNetwork()
    }

    private fun updateWeeklyStreak(weekly: RewardsWeeklyStreak?) {
        binding.weeklyStreak.text =
            getString(R.string.weekly_streak_desc, weekly?.fromDate, weekly?.toDate)

        binding.weeklyTimeline.setContent {
            WeeklyStreak(weekly)
        }
    }

    @Suppress("FunctionNaming", "MagicNumber")
    @Composable
    internal fun WeeklyStreak(weekly: RewardsWeeklyStreak?) {
        Column(
            modifier = Modifier.then(Modifier.fillMaxWidth())
        ) {
            Row(
                modifier = Modifier.then(
                    Modifier
                        .fillMaxWidth()
                        .padding(12.dp, 0.dp, 12.dp, 0.dp)
                ),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                weekly?.timelineData?.forEach { item ->
                    Column(
                        modifier = Modifier.then(Modifier.width(20.dp)),
                        verticalArrangement = Arrangement.SpaceAround,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box {
                            val value = item.second

                            // Fit the value (0-100) in the 20-100 range
                            val normalizedValue = (value.toFloat() / 100F * 60F + 20F).toInt()

                            Bar(colorId = R.color.blueTint)
                            Bar(height = normalizedValue, colorId = getRewardScoreColor(value))
                        }
                        Text(
                            text = item.first,
                            Modifier.padding(0.dp, 4.dp, 0.dp, 0.dp),
                            fontSize = 12.sp,
                            color = Color(requireContext().getColor(R.color.colorOnSurface)),
                        )
                    }
                }
            }
            Row(
                modifier = Modifier.then(
                    Modifier
                        .fillMaxWidth()
                        .padding(0.dp, 8.dp, 0.dp, 0.dp)
                ),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = weekly?.fromDate ?: String.empty(),
                    fontSize = 12.sp,
                    color = Color(requireContext().getColor(R.color.colorOnSurface)),
                )
                Text(
                    text = weekly?.toDate ?: String.empty(),
                    fontSize = 12.sp,
                    color = Color(requireContext().getColor(R.color.colorOnSurface)),
                )
            }
        }
    }

    @Suppress("FunctionNaming")
    @Composable
    private fun BoxScope.Bar(height: Int = 80, @ColorRes colorId: Int) {
        val radius = dimensionResource(id = R.dimen.radius_medium)
        Spacer(
            modifier = Modifier
                .height(height.dp)
                .width(20.dp)
                .align(Alignment.BottomCenter)
                .background(
                    colorResource(id = colorId),
                    RoundedCornerShape(radius, radius, radius, radius)
                )
        )
    }

    override fun onResume() {
        super.onResume()
        analytics.trackScreen(Analytics.Screen.DEVICE_REWARDS, RewardsFragment::class.simpleName)
    }
}
