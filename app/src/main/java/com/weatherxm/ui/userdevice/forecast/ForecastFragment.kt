package com.weatherxm.ui.userdevice.forecast

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.google.android.material.snackbar.Snackbar
import com.weatherxm.R
import com.weatherxm.databinding.FragmentUserDeviceForecastBinding
import com.weatherxm.ui.userdevice.UserDeviceViewModel
import com.weatherxm.util.Analytics
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.parameter.parametersOf

class ForecastFragment : Fragment(), KoinComponent {
    private lateinit var binding: FragmentUserDeviceForecastBinding
    private val analytics: Analytics by inject()
    private val parentModel: UserDeviceViewModel by activityViewModels()
    private val model: ForecastViewModel by viewModel {
        parametersOf(parentModel.device)
    }
    private var snackbar: Snackbar? = null

    private lateinit var forecastAdapter: ForecastAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentUserDeviceForecastBinding.inflate(inflater, container, false)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.swiperefresh.setOnRefreshListener {
            model.fetchForecast(true)
        }

        // Initialize the adapter with empty data
        forecastAdapter = ForecastAdapter()
        binding.forecastRecycler.adapter = forecastAdapter

        model.onForecast().observe(viewLifecycleOwner) {
            forecastAdapter.submitList(it)

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

        parentModel.onUnitPreferenceChanged().observe(viewLifecycleOwner) {
            if (it) {
                forecastAdapter.notifyDataSetChanged()
            }
        }

        // Fetch data
        model.fetchForecast()
    }

    override fun onResume() {
        super.onResume()
        analytics.trackScreen(
            Analytics.Screen.FORECAST,
            ForecastFragment::class.simpleName
        )
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
