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
import com.weatherxm.analytics.AnalyticsService
import com.weatherxm.data.models.RewardsTimestampScore
import com.weatherxm.databinding.FragmentDeviceDetailsRewardsBinding
import com.weatherxm.ui.common.DeviceRelation
import com.weatherxm.ui.common.Status
import com.weatherxm.ui.common.classSimpleName
import com.weatherxm.ui.common.empty
import com.weatherxm.ui.common.invisible
import com.weatherxm.ui.common.visible
import com.weatherxm.ui.components.BaseFragment
import com.weatherxm.ui.components.compose.DailyRewardsView
import com.weatherxm.ui.devicedetails.DeviceDetailsViewModel
import com.weatherxm.util.DateTimeHelper.getFormattedDate
import com.weatherxm.util.NumberUtils.formatTokens
import com.weatherxm.util.Rewards.getRewardScoreColor
import com.weatherxm.util.getFirstLetter
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
            val totalRewards = it.totalRewards ?: 0F
            binding.emptyCard.visible(it.isEmpty())
            if (!it.isEmpty()) {
                binding.totalRewards.text =
                    getString(R.string.wxm_amount, formatTokens(totalRewards.toBigDecimal()))
                updateWeeklyStreak(it.timeline)
                binding.totalCard.visible(true)
                binding.weeklyCard.visible(true)
            }
            it.latest?.let { reward ->
                binding.dailyRewardsCard.setContent {
                    DailyRewardsView(
                        data = reward,
                        useShortAnnotationText = parentModel.device.relation != DeviceRelation.OWNED
                    ) {
                        navigator.showRewardDetails(requireContext(), parentModel.device, reward)
                    }
                }
                binding.dailyRewardsCard.visible(true)
            }
        }

        model.onLoading().observe(viewLifecycleOwner) {
            if (it && binding.swiperefresh.isRefreshing) {
                binding.progress.invisible()
            } else if (it) {
                binding.totalCard.visible(false)
                binding.dailyRewardsCard.visible(false)
                binding.weeklyCard.visible(false)
                binding.progress.visible(true)
            } else {
                binding.swiperefresh.isRefreshing = false
                binding.progress.invisible()
            }
        }

        model.onError().observe(viewLifecycleOwner) {
            binding.totalCard.visible(false)
            binding.dailyRewardsCard.visible(false)
            binding.weeklyCard.visible(false)
            showSnackbarMessage(binding.root, it.errorMessage, it.retryFunction)
        }

        parentModel.onDeviceFirstFetch().observe(viewLifecycleOwner) {
            model.device = it
            model.fetchRewardsFromNetwork()
        }

        binding.swiperefresh.setOnRefreshListener {
            model.fetchRewardsFromNetwork()
        }

        binding.viewTimeline.setOnClickListener {
            navigator.showRewardsList(requireContext(), model.device)
        }

        model.fetchRewardsFromNetwork()
    }

    private fun updateWeeklyStreak(timeline: List<RewardsTimestampScore>?) {
        val fromDate = timeline?.firstOrNull()?.timestamp.getFormattedDate()
        val toDate = timeline?.lastOrNull()?.timestamp.getFormattedDate()
        binding.weeklyStreak.text = getString(R.string.weekly_streak_desc, fromDate, toDate)

        binding.weeklyTimeline.setContent {
            WeeklyStreak(fromDate, toDate, timeline)
        }
    }

    @Suppress("FunctionNaming")
    @Composable
    internal fun WeeklyStreak(
        fromDate: String,
        toDate: String,
        timeline: List<RewardsTimestampScore>?
    ) {
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
                timeline?.forEach { item ->
                    BarAndText(item)
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
                    text = fromDate,
                    fontSize = 12.sp,
                    color = Color(requireContext().getColor(R.color.colorOnSurface)),
                )
                Text(
                    text = toDate,
                    fontSize = 12.sp,
                    color = Color(requireContext().getColor(R.color.colorOnSurface)),
                )
            }
        }
    }

    @Suppress("FunctionNaming", "MagicNumber")
    @Composable
    private fun BarAndText(entry: RewardsTimestampScore) {
        Column(
            modifier = Modifier.then(Modifier.width(20.dp)),
            verticalArrangement = Arrangement.SpaceAround,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box {
                Bar(colorId = R.color.blueTint)
                entry.baseRewardScore?.let {
                    // Fit the value (0-100) in the 20-100 range
                    val normalizedValue = (it.toFloat() / 100F * 60F + 20F).toInt()
                    Bar(height = normalizedValue, colorId = getRewardScoreColor(it))
                }
            }
            entry.timestamp?.dayOfWeek?.getFirstLetter()?.let {
                Text(
                    text = context?.getString(it) ?: String.empty(),
                    Modifier.padding(0.dp, 4.dp, 0.dp, 0.dp),
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
        analytics.trackScreen(AnalyticsService.Screen.DEVICE_REWARDS, classSimpleName())
    }
}
