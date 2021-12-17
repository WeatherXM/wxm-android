package com.weatherxm.ui.userdevice

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.weatherxm.data.Device
import com.weatherxm.data.Timeseries
import com.weatherxm.databinding.ActivityUserDeviceBinding
import com.weatherxm.util.Weather
import com.weatherxm.util.formatOnlyTime
import com.weatherxm.util.fromMillis
import dev.chrisbanes.insetter.applyInsetter
import org.koin.core.component.KoinComponent

class UserDeviceActivity : AppCompatActivity(), KoinComponent {

    private lateinit var binding: ActivityUserDeviceBinding
    private var device: Device? = null

    companion object {
        const val ARG_DEVICE = "device"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUserDeviceBinding.inflate(layoutInflater)
        setContentView(binding.root)
        applyMapInsets()

        binding.toolbar.setNavigationOnClickListener {
            onBackPressed()
        }

        device = intent?.extras?.getParcelable(ARG_DEVICE)
        device?.let {
            updateUI(it)
        }
    }

    private fun updateUI(device: Device?) {
        updateCurrentWeatherUI(device?.timeseries)
        updateHourlyUI()
        binding.toolbar.title = device?.name
    }

    private fun updateHourlyUI() {
        // val adapter = HourlyAdapter(hourlyCardsData)
        // binding.recycler.adapter = adapter
    }

    private fun updateCurrentWeatherUI(weather: Timeseries?) {
        with(ActivityUserDeviceBinding.bind(binding.root)) {
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
            updatedOn.text = weather?.timestamp?.let { fromMillis(it).formatOnlyTime() } ?: ""
        }
    }

    private fun applyMapInsets() {
        binding.appBar.applyInsetter {
            type(statusBars = true) {
                margin(left = false, top = true, right = false, bottom = false)
            }
        }
    }
}
