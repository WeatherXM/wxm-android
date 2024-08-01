package com.weatherxm.ui.devicedetails.current

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.weatherxm.R
import com.weatherxm.analytics.AnalyticsService
import com.weatherxm.data.Status
import com.weatherxm.databinding.FragmentDeviceDetailsCurrentBinding
import com.weatherxm.ui.common.BundleName
import com.weatherxm.ui.common.DeviceAlertType
import com.weatherxm.ui.common.UIDevice
import com.weatherxm.ui.common.classSimpleName
import com.weatherxm.ui.common.invisible
import com.weatherxm.ui.common.setCardStroke
import com.weatherxm.ui.common.visible
import com.weatherxm.ui.components.BaseFragment
import com.weatherxm.ui.devicedetails.DeviceDetailsViewModel
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
        setAlerts(device)
        if (device.currentWeather == null || device.currentWeather.isEmpty()) {
            binding.historicalCharts.visible(false)
        }
        binding.currentWeatherCard.setData(device.currentWeather)
        binding.followCard.visible(device.isUnfollowed())
        binding.historicalCharts.isEnabled = !device.isUnfollowed()
    }

    private fun setAlerts(device: UIDevice) {
        if (device.alerts.firstOrNull { it.alert == DeviceAlertType.OFFLINE } != null) {
            onDeviceOfflineAlert(device)
        } else {
            binding.currentWeatherCardWithErrorContainer.setCardStroke(R.color.transparent, 0)
        }
    }

    private fun onDeviceOfflineAlert(device: UIDevice) {
        if (!device.isOwned()) {
            binding.alert.message(getString(R.string.no_data_message_public_device))
        } else {
            when (device.bundleName) {
                BundleName.m5 -> showOfflineAlert(getString(R.string.troubleshooting_m5_url))
                BundleName.d1 -> showOfflineAlert(getString(R.string.troubleshooting_d1_url))
                BundleName.h1, BundleName.h2 -> {
                    showOfflineAlert(getString(R.string.troubleshooting_helium_url))
                }
                BundleName.pulse -> showOfflineAlert(getString(R.string.troubleshooting_pulse_url))
                null -> {
                    // Do nothing
                }
            }
        }
        binding.currentWeatherCardWithErrorContainer.setCardStroke(R.color.error, 2)
        binding.alert.visible(true)
    }

    private fun showOfflineAlert(url: String) {
        binding.alert.htmlMessage(getString(R.string.error_user_device_offline, url)) {
            navigator.openWebsite(context, url)
        }
    }
}
