package com.weatherxm.ui.userdevice.current

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
import com.weatherxm.databinding.FragmentUserDeviceCurrentBinding
import com.weatherxm.ui.Navigator
import com.weatherxm.ui.userdevice.UserDeviceViewModel
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.parameter.parametersOf
import timber.log.Timber

class CurrentFragment : Fragment(), KoinComponent {
    private lateinit var binding: FragmentUserDeviceCurrentBinding
    private val parentModel: UserDeviceViewModel by activityViewModels()
    private val model: CurrentViewModel by viewModel {
        parametersOf(parentModel.device)
    }
    private val navigator: Navigator by inject()
    private var snackbar: Snackbar? = null

    init {
        lifecycleScope.launch {
            // Launch the block in a new coroutine every time the lifecycle
            // is in the RESUMED state (or above) and cancel it when it's STOPPED.
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
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

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentUserDeviceCurrentBinding.inflate(inflater, container, false)

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
            model.fetchUserDevice()
        }

        binding.historicalCharts.setOnClickListener {
            navigator.showHistoryActivity(requireContext(), model.device)
        }
    }

    private fun onDeviceUpdated(device: Device) {
        binding.progress.visibility = View.INVISIBLE
        when (device.attributes?.isActive) {
            true -> {
                binding.errorCard.hide()
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
