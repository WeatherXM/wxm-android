package com.weatherxm.ui.devicedetails.current

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.snackbar.Snackbar
import com.weatherxm.R
import com.weatherxm.data.Device
import com.weatherxm.databinding.FragmentDeviceDetailsCurrentBinding
import com.weatherxm.ui.Navigator
import com.weatherxm.ui.common.UIDevice
import com.weatherxm.ui.common.setVisible
import com.weatherxm.ui.devicedetails.DeviceDetailsViewModel
import com.weatherxm.util.Analytics
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.parameter.parametersOf
import timber.log.Timber

class CurrentFragment : Fragment(), KoinComponent {
    private lateinit var binding: FragmentDeviceDetailsCurrentBinding
    private val parentModel: DeviceDetailsViewModel by activityViewModels()
    private val model: CurrentViewModel by viewModel {
        parametersOf(parentModel.device, parentModel.cellDevice)
    }
    private val navigator: Navigator by inject()
    private val analytics: Analytics by inject()
    private var snackbar: Snackbar? = null

    init {
        lifecycleScope.launch {
            // Launch the block in a new coroutine every time the lifecycle
            // is in the RESUMED state (or above) and cancel it when it's STOPPED.
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                if (parentModel.isUserDevice) {
                    Timber.d("Starting device polling")
                    // Trigger the flow for refreshing device data in the background
                    parentModel.deviceAutoRefresh().collect {
                        it.onRight { device ->
                            onDeviceUpdated(device)
                        }
                    }
                }
            }
        }
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

        parentModel.onUnitPreferenceChanged().observe(viewLifecycleOwner) {
            if (it) {
                binding.currentWeatherCard.updateCurrentWeatherUI()
            }
        }

        model.onDevice().observe(viewLifecycleOwner) {
            onDeviceUpdated(it)
        }

        model.onCellDevice().observe(viewLifecycleOwner) {
            onCellDeviceUpdated(it)
        }

        model.onLoading().observe(viewLifecycleOwner) {
            if (it && binding.swiperefresh.isRefreshing) {
                binding.progress.visibility = View.INVISIBLE
            } else if (it) {
                binding.progress.visibility = View.VISIBLE
            } else {
                binding.swiperefresh.isRefreshing = false
                binding.progress.visibility = View.INVISIBLE
            }
        }

        model.onError().observe(viewLifecycleOwner) {
            showSnackbarMessage(it.errorMessage, it.retryFunction)
        }

        binding.swiperefresh.setOnRefreshListener {
            model.fetchDevice()
        }

        binding.historicalCharts.setOnClickListener {
            navigator.showHistoryActivity(requireContext(), model.device)
        }

        if (!parentModel.isUserDevice) {
            binding.historicalCharts.setVisible(false)
            model.fetchCellDevice()
        }
    }

    override fun onResume() {
        super.onResume()
        analytics.trackScreen(
            Analytics.Screen.CURRENT_WEATHER,
            CurrentFragment::class.simpleName
        )
    }

    private fun onDeviceUpdated(device: Device) {
        binding.progress.visibility = View.INVISIBLE
        when (device.attributes?.isActive) {
            true -> {
                binding.errorCard.setVisible(false)
            }
            false -> {
                binding.errorCard.setErrorMessageWithUrl(
                    R.string.error_user_device_offline,
                    device.profile
                )
            }
            null -> {
                // Do nothing here
            }
        }
        binding.currentWeatherCard.setData(device.currentWeather)
    }

    private fun onCellDeviceUpdated(cellDevice: UIDevice) {
        binding.progress.visibility = View.INVISIBLE
        when (cellDevice.isActive) {
            true -> {
                binding.errorCard.setVisible(false)
            }
            false -> {
                binding.errorCard.setErrorMessage(getString(R.string.no_data_message_public_device))
            }
            null -> {
                // Do nothing here
            }
        }
        binding.currentWeatherCard.setData(cellDevice.currentWeather)
    }

    private fun showSnackbarMessage(message: String, callback: (() -> Unit)? = null) {
        if (snackbar?.isShown == true) {
            snackbar?.dismiss()
        }

        if (callback != null) {
            snackbar = Snackbar.make(binding.root, message, Snackbar.LENGTH_INDEFINITE)
            snackbar?.setAction(R.string.action_retry) {
                callback()
            }
        } else {
            snackbar = Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG)
        }
        snackbar?.show()
    }
}
