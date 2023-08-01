package com.weatherxm.ui.devicedetails.current

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.google.android.material.snackbar.Snackbar
import com.weatherxm.R
import com.weatherxm.databinding.FragmentDeviceDetailsCurrentBinding
import com.weatherxm.ui.Navigator
import com.weatherxm.ui.common.DeviceOwnershipStatus
import com.weatherxm.ui.common.UIDevice
import com.weatherxm.ui.common.setVisible
import com.weatherxm.ui.devicedetails.DeviceDetailsViewModel
import com.weatherxm.util.Analytics
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.parameter.parametersOf

class CurrentFragment : Fragment(), KoinComponent {
    private lateinit var binding: FragmentDeviceDetailsCurrentBinding
    private val parentModel: DeviceDetailsViewModel by activityViewModels()
    private val model: CurrentViewModel by viewModel {
        parametersOf(parentModel.device)
    }
    private val navigator: Navigator by inject()
    private val analytics: Analytics by inject()
    private var snackbar: Snackbar? = null

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

        parentModel.onDevicePolling().observe(viewLifecycleOwner) {
            onDeviceUpdated(it)
        }

        model.onDevice().observe(viewLifecycleOwner) {
            onDeviceUpdated(it)
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
    }

    override fun onResume() {
        super.onResume()
        analytics.trackScreen(
            Analytics.Screen.CURRENT_WEATHER,
            CurrentFragment::class.simpleName
        )
    }

    private fun onDeviceUpdated(device: UIDevice) {
        binding.progress.visibility = View.INVISIBLE
        when (device.isActive) {
            true -> {
                binding.errorCard.setVisible(false)
            }
            false -> {
                if (model.device.ownershipStatus == DeviceOwnershipStatus.OWNED) {
                    binding.errorCard.setErrorMessageWithUrl(
                        R.string.error_user_device_offline,
                        device.profile
                    )
                } else {
                    binding.errorCard.setErrorMessage(
                        getString(R.string.no_data_message_public_device)
                    )
                }
            }
            else -> {
                // Do nothing here
            }
        }
        binding.currentWeatherCard.setData(device.currentWeather)
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
