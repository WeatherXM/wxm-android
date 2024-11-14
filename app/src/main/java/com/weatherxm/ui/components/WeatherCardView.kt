package com.weatherxm.ui.components

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.widget.LinearLayout
import com.weatherxm.R
import com.weatherxm.data.models.HourlyWeather
import com.weatherxm.databinding.ViewWeatherCardBinding
import com.weatherxm.ui.common.UIDevice
import com.weatherxm.ui.common.empty
import com.weatherxm.ui.common.setNoDataMessage
import com.weatherxm.ui.common.setWeatherAnimation
import com.weatherxm.ui.common.visible
import com.weatherxm.util.DateTimeHelper.getFormattedDateAndTime
import com.weatherxm.util.UnitSelector
import com.weatherxm.util.Weather
import com.weatherxm.util.Weather.getFormattedHumidity
import com.weatherxm.util.Weather.getUVClassification
import com.weatherxm.util.Weather.getWindDirectionDrawable

class WeatherCardView : LinearLayout {

    private lateinit var binding: ViewWeatherCardBinding
    private var weatherData: HourlyWeather? = null

    constructor(context: Context?) : super(context) {
        init(context)
    }

    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) {
        init(context)
    }

    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        init(context)
    }

    private fun init(context: Context?) {
        binding = ViewWeatherCardBinding.inflate(LayoutInflater.from(context), this)
        orientation = VERTICAL
        gravity = Gravity.CENTER
    }

    private fun updateCurrentWeatherUI() {
        with(binding) {
            val temperatureUnitSymbol = UnitSelector.getTemperatureUnit(context).unit

            icon.setWeatherAnimation(weatherData?.icon)
            temperature.text = Weather.getFormattedTemperature(
                context, weatherData?.temperature, 1, includeUnit = false
            )
            temperatureUnit.text = temperatureUnitSymbol
            feelsLike.text = Weather.getFormattedTemperature(
                context, weatherData?.feelsLike, 1, includeUnit = false
            )
            feelsLikeUnit.text = temperatureUnitSymbol
            dewPoint.setData(
                Weather.getFormattedTemperature(
                    context,
                    weatherData?.dewPoint,
                    1,
                    includeUnit = false
                ),
                temperatureUnitSymbol
            )

            humidity.setData(getFormattedHumidity(weatherData?.humidity, includeUnit = false), "%")

            val windValue = Weather.getFormattedWind(
                context = context,
                windSpeed = weatherData?.windSpeed,
                windDirection = weatherData?.windDirection,
                includeUnits = false
            )
            val windUnit = UnitSelector.getWindUnit(context).unit
            val windDirectionUnit =
                Weather.getFormattedWindDirection(context, weatherData?.windDirection)
            val windGustValue = Weather.getFormattedWind(
                context = context,
                windSpeed = weatherData?.windGust,
                windDirection = weatherData?.windDirection,
                includeUnits = false
            )
            val windDirDrawable = getWindDirectionDrawable(context, weatherData?.windDirection)
            wind.setData(windValue, "$windUnit $windDirectionUnit", windDirDrawable)
            windGust.setData(windGustValue, "$windUnit $windDirectionUnit", windDirDrawable)

            rainRate.setData(
                Weather.getFormattedPrecipitation(
                    context = context,
                    value = weatherData?.precipitation,
                    includeUnit = false
                ),
                UnitSelector.getPrecipitationUnit(context, true).unit
            )
            precipitationAccumulated.setData(
                Weather.getFormattedPrecipitation(
                    context = context,
                    value = weatherData?.precipAccumulated,
                    isRainRate = false,
                    includeUnit = false
                ),
                UnitSelector.getPrecipitationUnit(context, false).unit
            )

            val pressureUnit = UnitSelector.getPressureUnit(context).unit
            pressure.setData(
                Weather.getFormattedPressure(
                    context = context,
                    value = weatherData?.pressure,
                    includeUnit = false
                ),
                pressureUnit
            )
            val uvLabel = weatherData?.uvIndex?.let {
                getUVClassification(context, it)
            } ?: String.empty()
            uv.setData(Weather.getFormattedUV(context, weatherData?.uvIndex, false), uvLabel)
            solarRadiation.setData(
                Weather.getFormattedSolarRadiation(context, weatherData?.solarIrradiance, false),
                context.getString(R.string.solar_radiation_unit)
            )
        }
    }

    fun setData(device: UIDevice) {
        val data = device.currentWeather
        if (data == null || data.isEmpty()) {
            binding.weatherDataLayout.visible(false)
            binding.secondaryCard.visible(false)
            binding.noDataLayout.visible(true)
            device.setNoDataMessage(context, binding.noDataMessage)
        } else {
            weatherData = data
            updateCurrentWeatherUI()
            binding.lastUpdatedOn.text = context.getString(
                R.string.last_updated,
                data.timestamp.getFormattedDateAndTime(context)
            )
        }

    }
}
