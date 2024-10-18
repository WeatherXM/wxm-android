package com.weatherxm.ui.devicedetails.current

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.firebase.analytics.FirebaseAnalytics
import com.weatherxm.R
import com.weatherxm.analytics.AnalyticsService
import com.weatherxm.data.models.Reward
import com.weatherxm.databinding.FragmentDeviceDetailsCurrentBinding
import com.weatherxm.ui.common.AnnotationGroupCode
import com.weatherxm.ui.common.Status
import com.weatherxm.ui.common.UIDevice
import com.weatherxm.ui.common.classSimpleName
import com.weatherxm.ui.common.invisible
import com.weatherxm.ui.common.setColor
import com.weatherxm.ui.common.visible
import com.weatherxm.ui.components.BaseFragment
import com.weatherxm.ui.components.StationHealthExplanationDialogFragment
import com.weatherxm.ui.devicedetails.DeviceDetailsViewModel
import com.weatherxm.ui.explorer.UICell
import com.weatherxm.util.Rewards.getRewardScoreColor
import org.koin.androidx.viewmodel.ext.android.activityViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf

class CurrentFragment : BaseFragment() {
    private lateinit var binding: FragmentDeviceDetailsCurrentBinding
    private val parentModel: DeviceDetailsViewModel by activityViewModel()
    private val model: CurrentViewModel by viewModel {
        parametersOf(parentModel.device)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentDeviceDetailsCurrentBinding.inflate(inflater, container, false)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        parentModel.onFollowStatus().observe(viewLifecycleOwner) {
            if (it.status == Status.SUCCESS) {
                model.device = parentModel.device
                model.fetchDevice()
            }
        }

        parentModel.onDevicePolling().observe(viewLifecycleOwner) {
            model.device = it
            onDeviceUpdated(it)
        }

        model.onDevice().observe(viewLifecycleOwner) {
            parentModel.updateDevice(it)
            onDeviceUpdated(it)
        }

        model.onLoading().observe(viewLifecycleOwner) {
            if (it && binding.swiperefresh.isRefreshing) {
                binding.progress.invisible()
            } else if (it) {
                binding.progress.visible(true)
            } else {
                binding.swiperefresh.isRefreshing = false
                binding.progress.invisible()
            }
        }

        model.onError().observe(viewLifecycleOwner) {
            showSnackbarMessage(binding.root, it.errorMessage, it.retryFunction)
        }

        binding.swiperefresh.setOnRefreshListener {
            model.fetchDevice()
        }

        binding.historicalCharts.setOnClickListener {
            navigator.showHistoryActivity(requireContext(), model.device)
        }

        binding.stationHealthInfoBtn.setOnClickListener {
            StationHealthExplanationDialogFragment().show(this)
        }

        binding.followCard.visible(model.device.isUnfollowed())
        binding.historicalCharts.isEnabled = !model.device.isUnfollowed()
        binding.followPromptBtn.setOnClickListener {
            handleFollowClick()
        }
    }

    override fun onResume() {
        super.onResume()
        analytics.trackScreen(AnalyticsService.Screen.CURRENT_WEATHER, classSimpleName())
    }

    private fun handleFollowClick() {
        if (parentModel.isLoggedIn() == false) {
            navigator.showLoginDialog(
                fragmentActivity = activity,
                title = getString(R.string.add_favorites),
                htmlMessage = getString(R.string.hidden_content_login_prompt, model.device.name)
            )
            return
        }

        if (model.device.isUnfollowed() && !model.device.isOnline()) {
            navigator.showHandleFollowDialog(activity, true, model.device.name) {
                parentModel.followStation()
            }
        } else if (model.device.isUnfollowed()) {
            parentModel.followStation()
        }
    }

    private fun onDeviceUpdated(device: UIDevice) {
        binding.progress.invisible()

        binding.address.text = if (device.address.isNullOrEmpty()) {
            getString(R.string.unknown_address)
        } else {
            binding.addressCard.setOnClickListener {
                analytics.trackEventSelectContent(
                    AnalyticsService.ParamValue.REGION.paramValue,
                    customParams = arrayOf(
                        Pair(
                            AnalyticsService.CustomParam.CONTENT_NAME.paramName,
                            AnalyticsService.ParamValue.STATION_DETAILS_CHIP.paramValue
                        ),
                        Pair(
                            FirebaseAnalytics.Param.ITEM_ID,
                            AnalyticsService.ParamValue.STATION_REGION_ID.paramValue
                        )
                    )
                )
                model.device.cellCenter?.let { location ->
                    navigator.showCellInfo(context, UICell(model.device.cellIndex, location))
                }
            }
            device.address
        }

        onStationHealth(device)

        if (device.currentWeather == null || device.currentWeather.isEmpty()) {
            binding.historicalCharts.visible(false)
        }
        binding.latestWeatherCard.setData(device.currentWeather)
        binding.followCard.visible(device.isUnfollowed())
        binding.historicalCharts.isEnabled = !device.isUnfollowed()
    }

    private fun onStationHealth(device: UIDevice) {
        device.qodScore?.let {
            binding.dataQuality.text = "$it"
            binding.dataQualityIcon.setColor(getRewardScoreColor(it))
        } ?: run {
            binding.dataQuality.text = context?.getString(R.string.no_data)
            binding.dataQualityIcon.setColor(R.color.darkGrey)

        }
        binding.dataQualityPercentage.visible(device.qodScore != null)

        when (device.polReason) {
            AnnotationGroupCode.NO_LOCATION_DATA -> {
                binding.addressPoL.text = context?.getString(R.string.no_location_data)
                binding.addressIcon.setColor(R.color.error)
            }
            AnnotationGroupCode.LOCATION_NOT_VERIFIED -> {
                binding.addressPoL.text = context?.getString(R.string.not_verified)
                binding.addressIcon.setColor(R.color.warning)
            }
            else -> {
                /**
                 * Check whether everything is null which means that we are at a "pending" state or
                 * just the PoL Reason is null which means that we have no error
                 */
                if (device.qodScore == null && device.metricsTimestamp == null) {
                    binding.addressPoL.text = context?.getString(R.string.pending_verification)
                    binding.addressIcon.setColor(R.color.darkGrey)
                } else {
                    binding.addressPoL.text = context?.getString(R.string.verified)
                    binding.addressIcon.setColor(R.color.success)
                }
            }
        }

        device.metricsTimestamp?.let {
            val reward = Reward.initWithTimestamp(it)
            binding.dataQualityCard.setOnClickListener {
                navigator.showRewardDetails(requireContext(), parentModel.device, reward)
            }
        }

        binding.emptyStationHealthInfo.visible(
            device.qodScore == null && device.metricsTimestamp == null
        )
    }
}
