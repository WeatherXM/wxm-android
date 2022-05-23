package com.weatherxm.ui.publicdevicedetail

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.whenCreated
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.weatherxm.R
import com.weatherxm.data.Device
import com.weatherxm.data.Resource
import com.weatherxm.data.Status
import com.weatherxm.databinding.FragmentPublicDeviceDetailsBinding
import com.weatherxm.ui.common.toast
import com.weatherxm.ui.explorer.ExplorerViewModel
import kotlinx.coroutines.launch
import timber.log.Timber

class PublicDeviceDetailFragment : BottomSheetDialogFragment() {
    private val explorerModel: ExplorerViewModel by activityViewModels()
    private val model: PublicDeviceDetailViewModel by viewModels()
    private lateinit var binding: FragmentPublicDeviceDetailsBinding
    private var device: Device? = null

    companion object {
        const val TAG = "PublicDeviceDetailFragment"
        private const val ARG_DEVICE = "device"

        fun newInstance(device: Device) = PublicDeviceDetailFragment().apply {
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

        binding.toolbar.setNavigationOnClickListener {
            explorerModel.openListOfDevicesOfHex()
            dismiss()
        }

        model.device().observe(this) {
            updateUI(it)
        }

        model.setDevice(device)
    }

    private fun updateUI(resource: Resource<Device>) {
        when (resource.status) {
            Status.SUCCESS -> {
                binding.title.text = resource.data?.getNameOrLabel()
                with(binding.subtitle) {
                    text = resource.data?.address

                    visibility = if (resource.data?.address.isNullOrEmpty()) GONE else VISIBLE
                }
                binding.currentWeatherCard.setWeatherData(resource.data?.currentWeather, 1)
            }
            Status.ERROR -> {
                Timber.d(resource.message, resource.message)
                resource.message?.let { context.toast(it, Toast.LENGTH_LONG) }
            }
            Status.LOADING -> {}
        }
    }
}
