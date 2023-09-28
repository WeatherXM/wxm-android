package com.weatherxm.ui.devicedetails.current

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.google.android.material.snackbar.Snackbar
import com.weatherxm.R
import com.weatherxm.data.DeviceProfile
import com.weatherxm.data.Status
import com.weatherxm.databinding.FragmentDeviceDetailsCurrentBinding
import com.weatherxm.ui.Navigator
import com.weatherxm.ui.common.DeviceRelation
import com.weatherxm.ui.common.UIDevice
import com.weatherxm.ui.common.setVisible
import com.weatherxm.ui.devicedetails.DeviceDetailsViewModel
import com.weatherxm.util.Analytics
import com.weatherxm.util.setCardStroke
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

        parentModel.onFollowStatus().observe(viewLifecycleOwner) {
            if (it.status == Status.SUCCESS) {
                model.device = parentModel.device
                model.fetchDevice()
            }
        }

        parentModel.onDevicePolling().observe(viewLifecycleOwner) {
            onDeviceUpdated(it)
        }

        model.onDevice().observe(viewLifecycleOwner) {
            parentModel.updateDevice(it)
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

        binding.followCard.setVisible(
            model.device.relation == DeviceRelation.UNFOLLOWED
        )
        binding.historicalCharts.isEnabled =
            model.device.relation != DeviceRelation.UNFOLLOWED

        binding.followPromptBtn.setOnClickListener {
            handleFollowClick()
        }
    }

    override fun onResume() {
        super.onResume()
        analytics.trackScreen(
            Analytics.Screen.CURRENT_WEATHER,
            CurrentFragment::class.simpleName
        )
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

        if (model.device.relation == DeviceRelation.UNFOLLOWED && !model.device.isOnline()) {
            navigator.showHandleFollowDialog(activity, true, model.device.name) {
                parentModel.followStation()
            }
        } else if (model.device.relation == DeviceRelation.UNFOLLOWED) {
            parentModel.followStation()
        }
    }

    private fun onDeviceUpdated(device: UIDevice) {
        binding.progress.visibility = View.INVISIBLE
        binding.error.setVisible(device.isActive == false)
        when (device.isActive) {
            true -> {
                binding.currentWeatherCardWithErrorContainer.setCardStroke(R.color.transparent, 0)
            }
            false -> {
                onDeviceOffline(device.relation, device.profile)
            }
            else -> {
                // Do nothing here
            }
        }
        binding.currentWeatherCard.setData(device.currentWeather)

        binding.followCard.setVisible(
            device.relation == DeviceRelation.UNFOLLOWED
        )
        binding.historicalCharts.isEnabled =
            device.relation != DeviceRelation.UNFOLLOWED
    }

    private fun onDeviceOffline(relation: DeviceRelation?, profile: DeviceProfile?) {
        if (relation == DeviceRelation.OWNED && profile == DeviceProfile.M5) {
            val m5TroubleshootingUrl = getString(R.string.troubleshooting_m5_url)
            binding.error.htmlMessage(
                getString(R.string.error_user_device_offline, m5TroubleshootingUrl)
            ) {
                navigator.openWebsite(context, getString(R.string.troubleshooting_m5_url))
            }
        } else if (relation == DeviceRelation.OWNED && profile == DeviceProfile.Helium) {
            val heliumTroubleshootingUrl = getString(R.string.troubleshooting_helium_url)
            binding.error.htmlMessage(
                getString(R.string.error_user_device_offline, heliumTroubleshootingUrl)
            ) {
                navigator.openWebsite(context, heliumTroubleshootingUrl)
            }
        } else {
            binding.error.message(getString(R.string.no_data_message_public_device))
        }
        binding.currentWeatherCardWithErrorContainer.setCardStroke(R.color.error, 2)
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
