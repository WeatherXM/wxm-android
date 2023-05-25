package com.weatherxm.ui.devicehistory

import androidx.annotation.Keep
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineDataSet
import com.squareup.moshi.JsonClass
import java.time.LocalDate

@Keep
@JsonClass(generateAdapter = true)
data class HistoryCharts(
    var date: LocalDate,
    var temperature: LineChartData,
    var feelsLike: LineChartData,
    var precipitation: LineChartData,
    var precipitationAccumulated: LineChartData,
    var windSpeed: LineChartData,
    var windGust: LineChartData,
    var windDirection: LineChartData,
    var humidity: LineChartData,
    var pressure: LineChartData,
    var uv: LineChartData,
    var solarRadiation: LineChartData
) {
    fun isEmpty(): Boolean {
        return !temperature.isDataValid() && !feelsLike.isDataValid()
            && !precipitation.isDataValid() && !precipitationAccumulated.isDataValid()
            && !windSpeed.isDataValid() && !windGust.isDataValid() && !windDirection.isDataValid()
            && !humidity.isDataValid() && !pressure.isDataValid()
            && !uv.isDataValid() && !solarRadiation.isDataValid()
    }
}

@Keep
@JsonClass(generateAdapter = true)
data class LineChartData(
    var timestamps: MutableList<String>,
    var entries: MutableList<Entry>
) {
    fun isDataValid(): Boolean {
        return timestamps.isNotEmpty() && entries.filterNot { it.y.isNaN() }.isNotEmpty()
    }

    /**
     * In order to implement the gaps in charts we need to use multiple LineDataSets, one per gap.
     * This is the implementation here. Source:
     * https://github.com/PhilJay/MPAndroidChart/issues/1435
     */
    fun getLineDataSetsWithValues(label: String): MutableList<LineDataSet> {
        val dataSets = mutableListOf<LineDataSet>()
        var tempEntries = mutableListOf<Entry>()

        /**
         * Based on the documentation above, we need a different LineDataSet for each continuous
         * line (with no gaps) for our entries.
         */
        entries.forEach {
            /**
             * If the value is not NaN (which means that there is a value and we are NOT in a gap)
             */
            if (!it.y.isNaN()) {
                /**
                 * If the tempEntries (our "last continuous line" or last LineDataSet) is NOT empty
                 * and the last X value of its last entry is not the previous one of the one we are
                 * traversing right now, then that means that the entry we are traversing right now
                 * is the next one after a gap and we need to add the previous tempEntries as a
                 * different LineDataSet and reset the tempEntries to be able to accept the new
                 * list of continuous (with no gaps) entries
                 */
                if (tempEntries.isNotEmpty() && tempEntries.last().x != (it.x - 1)) {
                    dataSets.add(LineDataSet(tempEntries, label))
                    tempEntries = mutableListOf()
                }
                tempEntries.add(it)
            }
        }
        /**
         * If we reached the end of the entries list and we have a pending LineDataSet to add
         */
        if (tempEntries.isNotEmpty()) {
            dataSets.add(LineDataSet(tempEntries, label))
        }

        return dataSets
    }

    fun getEmptyLineDataSets(label: String): MutableList<LineDataSet> {
        val dataSets = mutableListOf<LineDataSet>()
        var tempEntries = mutableListOf<Entry>()

        /**
         * Based on the documentation above, we need a different LineDataSet for each continuous
         * gap for our entries.
         */
        entries.forEach {
            /**
             * If the value is NaN (which means we are in a gap)
             */
            if (it.y.isNaN()) {
                /**
                 * If the tempEntries (our "last continuous line" or last LineDataSet) is NOT empty
                 * and the last X value of its last entry is not the previous one of the one we are
                 * traversing right now, then that means that the entry we are traversing right now
                 * is the first one in a gap and we need to add the previous tempEntries as a
                 * different LineDataSet and reset the tempEntries to be able to accept the new
                 * list of continuous gap entries
                 */
                if (tempEntries.isNotEmpty() && tempEntries.last().x != (it.x - 1)) {
                    dataSets.add(LineDataSet(tempEntries, label))
                    tempEntries = mutableListOf()
                }
                tempEntries.add(it)
            }
        }
        /**
         * If we reached the end of the entries list and we have a pending LineDataSet to add
         */
        if (tempEntries.isNotEmpty()) {
            dataSets.add(LineDataSet(tempEntries, label))
        }

        return dataSets
    }
}
