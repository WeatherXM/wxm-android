package com.weatherxm.ui.publicdevicedetail

import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.whenCreated
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.weatherxm.R
import com.weatherxm.data.Resource
import com.weatherxm.data.Status
import com.weatherxm.databinding.FragmentPublicDeviceDetailsBinding
import com.weatherxm.ui.UIDevice
import com.weatherxm.ui.explorer.ExplorerViewModel
import com.weatherxm.util.DateTimeHelper.getRelativeFormattedTime
import com.weatherxm.util.setColor
import kotlinx.coroutines.launch
import timber.log.Timber

class PublicDeviceDetailFragment : BottomSheetDialogFragment() {
    private val explorerModel: ExplorerViewModel by activityViewModels()
    private val model: PublicDeviceDetailViewModel by viewModels()
    private lateinit var binding: FragmentPublicDeviceDetailsBinding
    private var device: UIDevice? = null

    companion object {
        const val TAG = "PublicDeviceDetailFragment"
        private const val ARG_DEVICE = "device"

        fun newInstance(device: UIDevice) = PublicDeviceDetailFragment().apply {
            arguments = Bundle().apply { putParcelable(ARG_DEVICE, device) }
        }
    }

    init {
        lifecycleScope.launch {
            whenCreated {
                device = arguments?.getParcelable(ARG_DEVICE)
            }
        }
    }

    override fun getTheme(): Int {
        return R.style.ThemeOverlay_WeatherXM_BottomSheetDialog
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentPublicDeviceDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        /*
        * An alternative way to go back to the list when android's back button is clicked
        * onBackPressed or other solutions aren't available yet on BottomSheetDialogFragment so
        * this "hack" is the ugly solution here
         */
        dialog?.setOnKeyListener { dialogInterface, keyCode, keyEvent ->
            if (keyCode == KeyEvent.KEYCODE_BACK && keyEvent.action == KeyEvent.ACTION_UP) {
                explorerModel.openListOfDevicesOfHex()
                dismiss()
            }
            true
        }

        binding.toolbar.setNavigationOnClickListener {
            explorerModel.openListOfDevicesOfHex()
            dismiss()
        }

        model.onPublicDevice().observe(this) {
            updateUI(it)
        }

        binding.name.text = device?.name
        with(binding.address) {
            text = device?.address
            visibility = if (device?.address.isNullOrEmpty()) GONE else VISIBLE
        }

        model.fetchDevice(device?.cellIndex, device?.id)
    }

    private fun updateUI(resource: Resource<UIDevice>) {
        when (resource.status) {
            Status.SUCCESS -> {
                binding.empty.visibility = GONE
                updateDeviceInfo(resource.data)
                binding.currentWeatherCard.setData(
                    resource.data?.currentWeather,
                    resource.data?.timezone
                )
                binding.currentWeatherCard.show()
                resource.data?.tokenInfo?.let {
                    binding.tokenCard.setTokenInfo(it, null)
                    binding.tokenCard.show()
                }
            }
            Status.ERROR -> {
                Timber.d(resource.message, resource.message)
                binding.currentWeatherCard.hide()
                binding.tokenCard.hide()
                binding.statusIcon.visibility = GONE
                binding.empty.clear()
                binding.empty.animation(R.raw.anim_error)
                binding.empty.title(getString(R.string.error_generic_message))
                binding.empty.subtitle(resource.message)
                binding.empty.visibility = VISIBLE
            }
            Status.LOADING -> {
                binding.currentWeatherCard.hide()
                binding.tokenCard.hide()
                binding.statusIcon.visibility = GONE
                binding.empty.clear()
                binding.empty.animation(R.raw.anim_loading)
                binding.empty.visibility = VISIBLE
            }
        }
    }

    private fun updateDeviceInfo(device: UIDevice?) {
        binding.statusIcon.setColor(
            when (device?.isActive) {
                true -> R.color.device_status_online
                false -> R.color.device_status_offline
                null -> R.color.device_status_unknown
            }
        )
        binding.statusIcon.visibility = VISIBLE

        binding.lastActive.text = device?.lastWeatherStationActivity?.let {
            getString(
                R.string.last_active,
                it.getRelativeFormattedTime(getString(R.string.last_active_just_now))
            )
        }

        binding.statusLabel.text = getString(
            when (device?.isActive) {
                true -> R.string.online
                false -> R.string.offline
                null -> R.string.unknown
            }
        )
    }
}
