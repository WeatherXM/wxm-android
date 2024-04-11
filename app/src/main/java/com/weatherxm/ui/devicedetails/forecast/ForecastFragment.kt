package com.weatherxm.ui.devicedetails.forecast

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.weatherxm.R
import com.weatherxm.data.Status
import com.weatherxm.databinding.FragmentDeviceDetailsForecastBinding
import com.weatherxm.ui.common.DeviceRelation.UNFOLLOWED
import com.weatherxm.ui.common.setHtml
import com.weatherxm.ui.common.setVisible
import com.weatherxm.ui.components.BaseFragment
import com.weatherxm.ui.devicedetails.DeviceDetailsViewModel
import com.weatherxm.util.Analytics
import org.koin.androidx.viewmodel.ext.android.activityViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf

class ForecastFragment : BaseFragment() {
    private lateinit var binding: FragmentDeviceDetailsForecastBinding
    private val parentModel: DeviceDetailsViewModel by activityViewModel()
    private val model: ForecastViewModel by viewModel {
        parametersOf(parentModel.device)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentDeviceDetailsForecastBinding.inflate(inflater, container, false)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.swiperefresh.setOnRefreshListener {
            model.fetchForecast(true)
        }

        initHiddenContent()

        // Initialize the adapters with empty data
        val dailyForecastAdapter = DailyForecastAdapter {
            // TODO: Open forecast details
        }
        val hourlyForecastAdapter = HourlyForecastAdapter {
            // TODO: Open forecast details
        }
        binding.dailyForecastRecycler.adapter = dailyForecastAdapter
        binding.hourlyForecastRecycler.adapter = hourlyForecastAdapter

        parentModel.onFollowStatus().observe(viewLifecycleOwner) {
            if (it.status == Status.SUCCESS) {
                model.device = parentModel.device
                fetchOrHideContent()
            }
        }

        parentModel.onDeviceFirstFetch().observe(viewLifecycleOwner) {
            model.device = it
            model.fetchForecast(true)
        }

        model.onForecast().observe(viewLifecycleOwner) {
            hourlyForecastAdapter.submitList(it.next24Hours)
            dailyForecastAdapter.submitList(it.forecastDays)
            binding.dailyForecastRecycler.setVisible(true)
            binding.dailyForecastTitle.setVisible(true)
            binding.hourlyForecastRecycler.setVisible(true)
            binding.hourlyForecastTitle.setVisible(true)
        }

        model.onLoading().observe(viewLifecycleOwner) {
            if (it && binding.swiperefresh.isRefreshing) {
                binding.progress.visibility = View.INVISIBLE
            } else if (it) {
                binding.dailyForecastTitle.setVisible(false)
                binding.hourlyForecastTitle.setVisible(false)
                binding.progress.setVisible(true)
            } else {
                binding.swiperefresh.isRefreshing = false
                binding.progress.visibility = View.INVISIBLE
            }
        }

        model.onError().observe(viewLifecycleOwner) {
            showSnackbarMessage(binding.root, it.errorMessage, it.retryFunction)
        }

        fetchOrHideContent()
    }

    override fun onResume() {
        super.onResume()
        analytics.trackScreen(
            Analytics.Screen.FORECAST,
            ForecastFragment::class.simpleName
        )
    }

    private fun fetchOrHideContent() {
        if (model.device.relation != UNFOLLOWED) {
            binding.hiddenContentContainer.setVisible(false)
            model.fetchForecast()
        } else if (model.device.relation == UNFOLLOWED) {
            binding.dailyForecastRecycler.setVisible(false)
            binding.dailyForecastTitle.setVisible(false)
            binding.hourlyForecastTitle.setVisible(false)
            binding.hourlyForecastRecycler.setVisible(false)
            binding.hiddenContentContainer.setVisible(true)
        }
    }

    private fun initHiddenContent() {
        binding.hiddenContentText.setHtml(R.string.hidden_content_prompt, model.device.name)
        binding.hiddenContentBtn.setOnClickListener {
            if (parentModel.isLoggedIn() == true) {
                if (model.device.relation == UNFOLLOWED && !model.device.isOnline()) {
                    navigator.showHandleFollowDialog(activity, true, model.device.name) {
                        parentModel.followStation()
                    }
                } else {
                    parentModel.followStation()
                }
            } else {
                navigator.showLoginDialog(
                    fragmentActivity = activity,
                    title = getString(R.string.add_favorites),
                    htmlMessage = getString(R.string.hidden_content_login_prompt, model.device.name)
                )
            }
        }
    }
}
