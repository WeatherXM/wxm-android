package com.weatherxm.ui.devicedetails.rewards

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderColors
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.Constraints
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

    @Suppress("FunctionNaming")
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
                    val score = remember { mutableIntStateOf(item.second) }
                    Column(
                        modifier = Modifier.then(Modifier.width(20.dp)),
                        verticalArrangement = Arrangement.SpaceAround,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        VerticalSlider(value = score)
                        Text(
                            text = item.first,
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

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun VerticalSlider(value: MutableState<Int>) {
        Slider(modifier = Modifier
            .graphicsLayer {
                rotationZ = 270f
                transformOrigin = TransformOrigin(0f, 0f)
            }
            .layout { measurable, constraints ->
                val placeable = measurable.measure(
                    Constraints(
                        minWidth = constraints.minHeight,
                        maxWidth = constraints.maxHeight,
                        minHeight = constraints.minWidth,
                        maxHeight = constraints.maxWidth,
                    )
                )
                layout(placeable.height, placeable.width) {
                    placeable.place(-placeable.width, 0)
                }
            }
            .width(80.dp),
            value = value.value.toFloat(),
            valueRange = 0F..100F,
            enabled = false,
            colors = SliderDefaults.colors(
                disabledThumbColor = colorResource(R.color.transparent),
                disabledActiveTrackColor = colorResource(getRewardScoreColor(value.value)),
                disabledInactiveTrackColor = colorResource(R.color.blueTint),
            ),
            track = { sliderState ->
                SliderDefaults.Track(
                    sliderState = sliderState,
                    modifier = Modifier.scale(scaleX = 1f, scaleY = 5f),
                    colors = SliderColors(
                        disabledThumbColor = colorResource(R.color.transparent),
                        disabledActiveTrackColor = colorResource(getRewardScoreColor(value.value)),
                        disabledInactiveTrackColor = colorResource(R.color.blueTint),
                        activeTrackColor = colorResource(R.color.transparent),
                        activeTickColor = colorResource(R.color.transparent),
                        inactiveTickColor = colorResource(R.color.transparent),
                        inactiveTrackColor = colorResource(R.color.transparent),
                        disabledActiveTickColor = colorResource(R.color.transparent),
                        disabledInactiveTickColor = colorResource(R.color.transparent),
                        thumbColor = colorResource(R.color.transparent),
                    ),
                    enabled = false
                )
            },
            onValueChange = {
                value.value = it.toInt()
            })
    }

    override fun onResume() {
        super.onResume()
        analytics.trackScreen(Analytics.Screen.DEVICE_REWARDS, RewardsFragment::class.simpleName)
    }
}
