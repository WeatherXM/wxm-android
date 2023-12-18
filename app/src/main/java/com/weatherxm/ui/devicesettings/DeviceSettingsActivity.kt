package com.weatherxm.ui.devicesettings

import android.app.Activity
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import coil.ImageLoader
import coil.request.ImageRequest
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.analytics.FirebaseAnalytics
import com.weatherxm.R
import com.weatherxm.data.DeviceProfile
import com.weatherxm.databinding.ActivityDeviceSettingsBinding
import com.weatherxm.ui.Navigator
import com.weatherxm.ui.common.Contracts
import com.weatherxm.ui.common.Contracts.ARG_DEVICE
import com.weatherxm.ui.common.DeviceRelation
import com.weatherxm.ui.common.UIDevice
import com.weatherxm.ui.common.setVisible
import com.weatherxm.ui.common.toast
import com.weatherxm.util.Analytics
import com.weatherxm.util.MapboxUtils.getMinimap
import com.weatherxm.util.applyInsets
import com.weatherxm.util.applyOnGlobalLayout
import com.weatherxm.util.setHtml
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.parameter.parametersOf
import timber.log.Timber

class DeviceSettingsActivity : AppCompatActivity(), KoinComponent {
    private val model: DeviceSettingsViewModel by viewModel {
        parametersOf(intent.getParcelableExtra<UIDevice>(Contracts.ARG_DEVICE))
    }
    private lateinit var binding: ActivityDeviceSettingsBinding
    private val navigator: Navigator by inject()
    private val analytics: Analytics by inject()
    private val imageLoader: ImageLoader by inject()
    private var snackbar: Snackbar? = null
    private lateinit var adapter: DeviceInfoAdapter

    // Register the launcher for the edit location activity and wait for a possible result
    private val editLocationLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            val device = it.data?.getParcelableExtra<UIDevice>(ARG_DEVICE)
            if (it.resultCode == Activity.RESULT_OK && device != null) {
                model.device = device
                setupStationLocation(true)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDeviceSettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.root.applyInsets()

        if (model.device.isEmpty()) {
            Timber.d("Could not start DeviceSettingsActivity. Device is null.")
            toast(R.string.error_generic_message)
            finish()
            return
        }

        adapter = DeviceInfoAdapter {
            if (it == ActionType.UPDATE_FIRMWARE) {
                analytics.trackEventPrompt(
                    Analytics.ParamValue.OTA_AVAILABLE.paramValue,
                    Analytics.ParamValue.WARN.paramValue,
                    Analytics.ParamValue.ACTION.paramValue
                )
                navigator.showDeviceHeliumOTA(this, model.device, false)
                finish()
            }
        }
        binding.recyclerDeviceInfo.adapter = adapter

        binding.toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        setupInfo()

        model.onLoading().observe(this) {
            if (it) {
                binding.progress.visibility = View.VISIBLE
            } else {
                binding.progress.visibility = View.INVISIBLE
            }
        }

        model.onEditNameChange().observe(this) {
            binding.stationName.text = it
        }

        model.onError().observe(this) {
            binding.progress.visibility = View.INVISIBLE
            showSnackbarMessage(it.errorMessage, it.retryFunction)
        }

        model.onDeviceRemoved().observe(this) {
            navigator.showHome(this)
            finish()
        }

        binding.changeStationNameBtn.setOnClickListener {
            onChangeStationName()
        }

        binding.removeStationBtn.setOnClickListener {
            onRemoveStation()
        }

        binding.changeFrequencyBtn.setOnClickListener {
            navigator.showChangeFrequency(this, model.device)
        }

        binding.rebootStationBtn.setOnClickListener {
            navigator.showRebootStation(this, model.device)
        }

        binding.contactSupportBtn.setOnClickListener {
            navigator.sendSupportEmail(
                this,
                source = Analytics.ParamValue.DEVICE_INFO.paramValue
            )
        }

        if (model.device.relation == DeviceRelation.FOLLOWED) {
            binding.contactSupportBtn.text = getString(R.string.see_something_wrong_contact_support)
        }

        setupStationLocation()
    }

    private fun setupStationLocation(forceUpdateMinimap: Boolean = false) {
        binding.editLocationBtn.setOnClickListener {
            navigator.showEditLocation(editLocationLauncher, this, model.device)
        }

        if (model.device.relation == DeviceRelation.FOLLOWED) {
            binding.locationDesc.setHtml(
                R.string.station_location_favorite_desc, model.device.address ?: ""
            )
        } else {
            binding.locationDesc.setHtml(R.string.station_location_desc, model.device.address ?: "")
        }

        if(forceUpdateMinimap) {
            updateMinimap()
        } else {
            binding.locationLayout.applyOnGlobalLayout {
                updateMinimap()
            }
        }
    }

    private fun updateMinimap() {
        val deviceMapLocation = if(model.device.relation == DeviceRelation.OWNED) {
            model.device.location
        } else {
            null
        }
        getMinimap(binding.locationLayout.width, deviceMapLocation, model.device.hex7)?.let {
            imageLoader.enqueue(
                ImageRequest.Builder(this).data(it).target(binding.locationMinimap).build()
            )
        } ?: binding.locationMinimap.setVisible(false)
        getMinimap(binding.locationLayout.width, deviceMapLocation, model.device.hex7)?.let {
            imageLoader.enqueue(
                ImageRequest.Builder(this).data(it).target(binding.locationMinimap).build()
            )
        } ?: binding.locationMinimap.setVisible(false)
    }

    private fun onRemoveStation() {
        analytics.trackEventSelectContent(
            Analytics.ParamValue.REMOVE_DEVICE.paramValue,
            Pair(FirebaseAnalytics.Param.ITEM_ID, model.device.id)
        )
        navigator.showPasswordPrompt(this, R.string.remove_station_password_message) {
            if (it) {
                Timber.d("Password confirmation success!")
                model.removeDevice()
            } else {
                Timber.d("Password confirmation prompt was cancelled or failed.")
            }
        }
    }

    private fun onChangeStationName() {
        FriendlyNameDialogFragment(model.device.friendlyName, model.device.id) {
            model.setOrClearFriendlyName(it)
        }.show(this)
    }

    override fun onResume() {
        super.onResume()
        analytics.trackScreen(
            Analytics.Screen.STATION_SETTINGS,
            DeviceSettingsActivity::class.simpleName
        )
    }

    private fun setupInfo() {
        binding.stationName.text = model.device.getDefaultOrFriendlyName()

        // TODO: Remove this when we implement this
        binding.reconfigureWifiContainer.setVisible(false)

        if (model.device.relation != DeviceRelation.OWNED) {
            binding.deleteStationCard.setVisible(false)
            binding.frequencyTitle.setVisible(false)
            binding.frequencyDesc.setVisible(false)
            binding.changeFrequencyBtn.setVisible(false)
            binding.rebootStationContainer.setVisible(false)
            binding.dividerBelowFrequency.setVisible(false)
            binding.dividerBelowStationName.setVisible(false)
        }

        if (model.device.profile == DeviceProfile.Helium) {
            // binding.reconfigureWifiContainer.setVisible(false)
            with(binding.frequencyDesc) {
                movementMethod =
                    me.saket.bettermovementmethod.BetterLinkMovementMethod.newInstance().apply {
                        setOnLinkClickListener { _, url ->
                            navigator.openWebsite(context, url)
                            return@setOnLinkClickListener true
                        }
                    }
                setHtml(
                    R.string.change_station_frequency_helium,
                    getString(R.string.helium_frequencies_mapping_url)
                )
            }
        } else {
            // TODO: Remove the following lines when we implement this feature
            binding.frequencyTitle.setVisible(false)
            binding.frequencyDesc.setVisible(false)
            binding.changeFrequencyBtn.setVisible(false)
            binding.dividerBelowFrequency.setVisible(false)
            binding.dividerBelowStationName.setVisible(false)
            // binding.frequencyDesc.setHtml(R.string.change_station_frequency_m5)
            binding.rebootStationContainer.setVisible(false)
        }

        with(binding.removeStationDesc) {
            movementMethod =
                me.saket.bettermovementmethod.BetterLinkMovementMethod.newInstance().apply {
                    setOnLinkClickListener { _, url ->
                        navigator.openWebsite(context, url)
                        return@setOnLinkClickListener true
                    }
                }
            setHtml(
                R.string.remove_station_desc,
                getString(R.string.docs_url)
            )
        }

        model.onDeviceInfo().observe(this) { deviceInfo ->
            if (deviceInfo.any { it.warning != null }) {
                analytics.trackEventPrompt(
                    Analytics.ParamValue.LOW_BATTERY.paramValue,
                    Analytics.ParamValue.WARN.paramValue,
                    Analytics.ParamValue.VIEW.paramValue,
                    Pair(FirebaseAnalytics.Param.ITEM_ID, model.device.id)
                )
            }
            if (deviceInfo.any { it.action?.actionType == ActionType.UPDATE_FIRMWARE }) {
                analytics.trackEventPrompt(
                    Analytics.ParamValue.OTA_AVAILABLE.paramValue,
                    Analytics.ParamValue.WARN.paramValue,
                    Analytics.ParamValue.VIEW.paramValue
                )
            }
            adapter.submitList(deviceInfo)

            binding.shareBtn.setOnClickListener {
                navigator.openShare(this, model.parseDeviceInfoToShare(deviceInfo))

                analytics.trackEventUserAction(
                    actionName = Analytics.ParamValue.SHARE_STATION_INFO.paramValue,
                    contentType = Analytics.ParamValue.STATION_INFO.paramValue,
                    Pair(FirebaseAnalytics.Param.ITEM_ID, model.device.id)
                )
            }
        }

        model.getDeviceInformation()
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
