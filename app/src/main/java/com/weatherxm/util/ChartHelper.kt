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
import com.weatherxm.R.string.wind_speed_beaufort
import kotlin.math.roundToInt

// Custom implementation of https://weeklycoding.com/mpandroidchart-documentation/markerview/
class CustomDefaultMarkerView(
    context: Context,
    private val times: MutableList<String>?,
    private val valueName: String,
    private val valueUnit: String,
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
            decimals > 0 -> {
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
class CustomWindMarkerView(
    context: Context,
    private val times: MutableList<String>?,
    private val windGusts: MutableList<Entry>?,
    private val windDirections: MutableList<Entry>?,
    private val windSpeedTitle: String,
    private val windGustTitle: String
) : MarkerView(context, R.layout.view_two_values_chart_marker) {
    private var timeView: TextView = findViewById(R.id.time)
    private var windSpeedView: TextView = findViewById(R.id.value)
    private var windGustView: TextView = findViewById(R.id.secondValue)

    // callbacks everytime the MarkerView is redrawn, can be used to update the
    // content (user-interface)
    @SuppressLint("SetTextI18n")
    override fun refreshContent(entryClicked: Entry, highlight: Highlight?) {
        /*
            We find the relevant timestamp, wind gust and wind direction by
            using the same index as the wind speed to get the value in the relevant list
         */
        timeView.text = times?.get(entryClicked.x.toInt()) ?: ""
        val windDirectionAsFloat = windDirections?.get(entryClicked.x.toInt())?.y
        val formattedWindDirection = if (windDirectionAsFloat != null) {
            Weather.getFormattedWindDirection(windDirectionAsFloat.toInt())
        } else {
            ""
        }

        // Get the correct wind unit
        val windUnit = Weather.getPreferredUnit(
            resources.getString(R.string.key_wind_speed_preference),
            resources.getString(R.string.wind_speed_ms)
        )

        // Get the correct decimals to show
        val beaufortUsed = windUnit == resources.getString(wind_speed_beaufort)
        val decimalsToShow = if (beaufortUsed) 0 else 1

        // Get the correct wind speed and gust formatted with the correct decimal separator
        val formattedWindSpeed = "%.${decimalsToShow}f".format(entryClicked.y)
        val formattedWindGust =
            "%.${decimalsToShow}f".format(windGusts?.get(entryClicked.x.toInt())?.y)

        // Customize the text for the marker view (if wind speed is zero, then hide direction)
        windSpeedView.text = if (entryClicked.y == 0F) {
            "$windSpeedTitle: $formattedWindSpeed$windUnit"
        } else {
            "$windSpeedTitle: $formattedWindSpeed$windUnit $formattedWindDirection"
        }
        windGustView.text = "$windGustTitle: $formattedWindGust$windUnit"

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
    private val decimals: Int = 0
) : ValueFormatter() {
    override fun getAxisLabel(value: Float, axis: AxisBase?): String {
        return if (decimals > 0) {
            "%.${decimals}f$weatherUnit".format(value)
        } else {
            "${value.roundToInt()}$weatherUnit"
        }
    }
}


