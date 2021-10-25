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
import com.weatherxm.R
import com.weatherxm.data.PublicDevice
import com.weatherxm.data.Resource
import com.weatherxm.data.Status
import com.weatherxm.data.Timeseries
import com.weatherxm.databinding.FragmentDeviceDetailsBinding
import com.weatherxm.ui.common.toast
import com.weatherxm.util.Weather
import com.weatherxm.util.formatDefault
import com.weatherxm.util.fromMillis
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
    private var device: PublicDevice? = null

    companion object {
        const val TAG = "DeviceDetailFragment"
        private const val ARG_DEVICE = "device"

        fun newInstance(publicDevice: PublicDevice) = DeviceDetailFragment().apply {
            arguments = Bundle().apply { putParcelable(ARG_DEVICE, publicDevice) }
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

    private fun updateUI(resource: Resource<PublicDevice>) {
        when (resource.status) {
            Status.SUCCESS -> {

                // First get the weather data
                resource.data?.timeseries.let { weather ->
                    updateWeatherUI(weather)
                }

                // Then get the name/label of the device
                resource.data?.let {
                    val nameOrLabelOfDevice = deviceDetailModel.getNameOrLabel(it.name, it.label)
                    when (nameOrLabelOfDevice.status) {
                        Status.SUCCESS -> {
                            binding.name.text = nameOrLabelOfDevice.data
                        }
                        Status.ERROR -> {
                            binding.name.visibility = View.GONE
                            toast(getString(R.string.name_not_found_error), Toast.LENGTH_LONG)
                        }
                        Status.LOADING -> {
                        }
                    }
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

    private fun updateWeatherUI(weather: Timeseries?) {
        with(FragmentDeviceDetailsBinding.bind(binding.root)) {
            icon.setAnimation(Weather.getWeatherAnimation(weather?.hourlyIcon))
            temperature.text =
                Weather.getFormattedTemperature(weather?.hourlyTemperature)
            precipitationIntensity.text =
                Weather.getFormattedPrecipitation(weather?.hourlyPrecipIntensity)
            pressure.text = Weather.getFormattedPressure(weather?.hourlyPressure)
            humidity.text = Weather.getFormattedHumidity(weather?.hourlyHumidity)
            wind.text = Weather.getFormattedWind(
                weather?.hourlyWindSpeed,
                weather?.hourlyWindDirection
            )
            cloud.text = Weather.getFormattedCloud(weather?.hourlyCloudCover)
            solar.text = Weather.getFormattedUV(weather?.hourlyUvIndex)
            updated.text = weather?.timestamp?.let {
                "Updated on ${fromMillis(it).formatDefault()}"
            } ?: ""
        }
    }
}
