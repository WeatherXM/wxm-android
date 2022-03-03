package com.weatherxm.util

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.widget.TextView
import com.github.mikephil.charting.components.AxisBase
import com.github.mikephil.charting.components.MarkerView
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.highlight.Highlight
import com.weatherxm.R
import kotlin.math.roundToInt

// TODO: Explore whether that suppress can be removed 
// Custom implementation of https://weeklycoding.com/mpandroidchart-documentation/markerview/
@Suppress("LongParameterList")
class CustomDefaultMarkerView(
    context: Context,
    private val times: MutableList<String>?,
    private val valueName: String,
    private val valueUnit: String,
    private val showDecimals: Boolean,
    private val isPrecipitation: Boolean = false,
    private val decimals: Int = 0
) : MarkerView(context, R.layout.view_default_chart_marker) {
    private var timeView: TextView = findViewById(R.id.time)
    private var valueView: TextView = findViewById(R.id.value)

    // callbacks everytime the MarkerView is redrawn, can be used to update the
    // content (user-interface)
    @SuppressLint("SetTextI18n")
    override fun refreshContent(entryClicked: Entry, highlight: Highlight?) {
        /*
            We find the relevant timestamp by using the same index
            as the wind speed to get the value in the relevant list
         */
        timeView.text = times?.get(entryClicked.x.toInt()) ?: ""

        // Customize the text for the marker view
        valueView.text = when {
            isPrecipitation -> {
                // Get the correct precipitation intensity formatted with the correct decimals
                val precipitationIntensity = Weather.getFormattedValueOrEmpty(
                    entryClicked.y,
                    valueUnit,
                    Weather.getDecimalsPrecipitation()
                )

                // Customize the text for the marker view
                "$valueName: $precipitationIntensity"
            }
            showDecimals -> {
                val value = "%.${decimals}f".format(entryClicked.y)
                "$valueName: $value$valueUnit"
            }
            else -> {
                "$valueName: ${entryClicked.y.roundToInt()}$valueUnit"
            }
        }
        // this will perform necessary layouting
        super.refreshContent(entryClicked, highlight)
    }

    @Suppress("MagicNumber")
    override fun draw(canvas: Canvas, posx: Float, posy: Float) {
        // translate to the correct position and draw
        var newPosX = posx
        var newPosY = posy
        // Prevent overflow to the right
        if (posx > canvas.width / 2) {
            newPosX = ((canvas.width / 3).toFloat())
        }

        // We do this as for continuous 0 values on the y Axis the marker view hides those values
        if (posy > canvas.height / 2) {
            newPosY = 0F
        }

        // Add 10 to posy so that the marker view isn't over the point selected but a bit lower
        canvas.translate(newPosX, newPosY)
        draw(canvas)
    }
}

// Custom implementation of https://weeklycoding.com/mpandroidchart-documentation/markerview/
@Suppress("LongParameterList")
class CustomWindMarkerView(
    context: Context,
    private val times: MutableList<String>?,
    private val windGusts: MutableList<Entry>?,
    private val windDirections: MutableList<Entry>?,
    private val windSpeedTitle: String,
    private val windGustTitle: String,
    private val windSpeedUnit: String
) : MarkerView(context, R.layout.view_two_values_chart_marker) {
    private var timeView: TextView = findViewById(R.id.time)
    private var valueView: TextView = findViewById(R.id.value)
    private var secondValueView: TextView = findViewById(R.id.secondValue)

    // callbacks everytime the MarkerView is redrawn, can be used to update the
    // content (user-interface)
    @SuppressLint("SetTextI18n")
    override fun refreshContent(entryClicked: Entry, highlight: Highlight?) {
        /*
            We find the relevant timestamp, wind gust and wind direction by
            using the same index as the wind speed to get the value in the relevant list
         */
        timeView.text = times?.get(entryClicked.x.toInt()) ?: ""
        val windGust = windGusts?.get(entryClicked.x.toInt())?.y
        val windDirectionAsFloat = windDirections?.get(entryClicked.x.toInt())?.y
        var formattedWindDirection: String? = ""
        if (windDirectionAsFloat != null) {
            formattedWindDirection = Weather.getFormattedWindDirection(windDirectionAsFloat.toInt())
        }

        // Get the correct wind speed and gust formatted with the correct decimals
        val formattedWindSpeed = Weather.getFormattedValueOrEmpty(
            entryClicked.y, windSpeedUnit, Weather.getDecimalsWindSpeed()
        )

        val formattedWindGust = Weather.getFormattedValueOrEmpty(
            windGust, windSpeedUnit, Weather.getDecimalsWindSpeed()
        )

        // Customize the text for the marker view (if wind speed is zero, then hide direction)
        valueView.text = if (entryClicked.y == 0F) {
            "$windSpeedTitle: $formattedWindSpeed"
        } else {
            "$windSpeedTitle: $formattedWindSpeed ($formattedWindDirection)"
        }
        secondValueView.text = "$windGustTitle: $formattedWindGust"

        // this will perform necessary layouting
        super.refreshContent(entryClicked, highlight)
    }

    @Suppress("MagicNumber")
    override fun draw(canvas: Canvas, posx: Float, posy: Float) {
        // translate to the correct position and draw
        var newPosX = posx
        var newPosY = posy
        // Prevent overflow to the right
        if (posx > canvas.width / 2) {
            newPosX = ((canvas.width / 3).toFloat())
        }

        // We do this as for continuous 0 values on intensity the marker view hides those values
        if (posy > canvas.height / 2) {
            newPosY = 0F
        }

        canvas.translate(newPosX, newPosY)
        draw(canvas)
    }
}

class CustomXAxisFormatter(private val times: MutableList<String>?) : ValueFormatter() {
    override fun getAxisLabel(value: Float, axis: AxisBase?): String {
        return times?.getOrNull(value.toInt()) ?: value.toString()
    }
}

class CustomYAxisFormatter(
    private val weatherUnit: String,
    private val showDecimals: Boolean,
    private val decimals: Int?
) : ValueFormatter() {
    override fun getAxisLabel(value: Float, axis: AxisBase?): String {
        return if (showDecimals) {
            if (decimals == null) {
                "$value$weatherUnit"
            } else {
                "%.${decimals}f$weatherUnit".format(value)
            }
        } else {
            "${value.roundToInt()}$weatherUnit"
        }
    }
}


