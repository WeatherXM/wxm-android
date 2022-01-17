package com.weatherxm.ui.devicedetail

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.whenCreated
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.weatherxm.data.Device
import com.weatherxm.data.Resource
import com.weatherxm.data.Status
import com.weatherxm.databinding.FragmentDeviceDetailsBinding
import com.weatherxm.ui.common.toast
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import timber.log.Timber

class DeviceDetailFragment : BottomSheetDialogFragment(), KoinComponent {
    /*
        Use activityViewModels because we use this model to communicate with the parent activity
        so it needs to be the same model as the parent's one.
    */
    private val deviceDetailModel: DeviceDetailViewModel by activityViewModels()
    private lateinit var binding: FragmentDeviceDetailsBinding
    private var device: Device? = null

    companion object {
        const val TAG = "DeviceDetailFragment"
        private const val ARG_DEVICE = "device"

        fun newInstance(device: Device) = DeviceDetailFragment().apply {
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

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentDeviceDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        deviceDetailModel.onDeviceDetailsUpdate().observe(this, { resource ->
            Timber.d("Data updated: ${resource.status}")
            updateUI(resource)
        })

        deviceDetailModel.fetch(device)
    }

    private fun updateUI(resource: Resource<Device>) {
        when (resource.status) {
            Status.SUCCESS -> {

                // First get the weather data
                resource.data?.currentWeather.let { weather ->
                    binding.currentWeatherCard.setWeatherData(weather)
                }

                // Then get the name/label of the device and its location
                resource.data?.let {
                    binding.name.text = it.getNameOrLabel()
                    binding.location.text = it.address
                }
            }
            Status.ERROR -> {
                Timber.d(resource.message, resource.message)
                resource.message?.let { toast(it, Toast.LENGTH_LONG) }
            }
            Status.LOADING -> {
                // TODO Do something??
            }
        }
    }
}
