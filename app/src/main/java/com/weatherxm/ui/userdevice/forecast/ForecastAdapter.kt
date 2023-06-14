package com.weatherxm.ui.userdevice.forecast

import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.weatherxm.R
import com.weatherxm.databinding.ListItemForecastBinding
import com.weatherxm.ui.common.UIForecast
import com.weatherxm.ui.common.toggleVisibility
import com.weatherxm.util.HorizontalScrollGestureListener
import com.weatherxm.util.Weather
import com.weatherxm.util.Weather.roundToDecimals

class ForecastAdapter(private val onExpandToggle: (Int, Boolean) -> Unit) :
    ListAdapter<UIForecast, ForecastAdapter.DailyForecastViewHolder>(
        UIForecastDiffCallback()
    ) {

    private var minTemperature: Float = Float.MAX_VALUE
    private var maxTemperature: Float = Float.MIN_VALUE

    override fun submitList(list: List<UIForecast>?) {
        /*
        Consider the following case:
        Day X: minimum value: 10.01, max value 15.99
        Day Y: minimum value 10.49, max value 15.51
        In the above examples, min and max temperature are 10 and 16 on both days,
        but the slider will be drawn according to the floats, not the min/max as ints.
        Therefore we round all values to 0 decimals here before drawing the sliders.
        */
        minTemperature = list
            ?.minOfOrNull { it.minTemp ?: Float.MAX_VALUE }
            ?.let { roundToDecimals(it, 0) } ?: 0F
        maxTemperature = list
            ?.maxOfOrNull { it.maxTemp ?: Float.MIN_VALUE }
            ?.let { roundToDecimals(it, 0) } ?: 0F

        // Update data
        super.submitList(list?.map {
            it.apply {
                this.minTemp = roundToDecimals(this.minTemp ?: 0F, 0)
                this.maxTemp = roundToDecimals(this.maxTemp ?: 0F, 0)
            }
        })
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DailyForecastViewHolder {
        val binding = ListItemForecastBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return DailyForecastViewHolder(binding)
    }

    override fun onBindViewHolder(holder: DailyForecastViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class DailyForecastViewHolder(private val binding: ListItemForecastBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: UIForecast) {
            // Initialize the hourly adapter
            val hourlyAdapter = HourlyAdapter()
            binding.hourlyRecycler.adapter = hourlyAdapter
            hourlyAdapter.submitList(item.hourlyWeather)

            if (!item.hourlyWeather.isNullOrEmpty()) {
                binding.rootCard.setOnClickListener {
                    onExpandClick()
                }

                binding.toggleExpand.setOnClickListener {
                    onExpandClick()
                }
                binding.toggleExpand.visibility = View.VISIBLE
            } else {
                binding.toggleExpand.visibility = View.INVISIBLE
            }

            binding.date.text = item.nameOfDayAndDate
            binding.icon.apply {
                setAnimation(Weather.getWeatherAnimation(item.icon))
                playAnimation()
            }
            if (minTemperature == Float.MAX_VALUE || maxTemperature == Float.MIN_VALUE) {
                binding.temperature.visibility = View.INVISIBLE
            } else {
                binding.temperature.apply {
                    valueFrom = minTemperature
                    valueTo = maxTemperature
                    values = listOf(item.minTemp, item.maxTemp)
                }
            }
            binding.minTemperature.text = Weather.getFormattedTemperature(item.minTemp)
            binding.maxTemperature.text = Weather.getFormattedTemperature(item.maxTemp)

            binding.precipitationProbability.text =
                Weather.getFormattedPrecipitationProbability(item.precipProbability)

            binding.wind.text = Weather.getFormattedWind(item.windSpeed, item.windDirection)
            binding.humidity.text = Weather.getFormattedHumidity(item.humidity)

            if (absoluteAdapterPosition == 0 && !item.hourlyWeather.isNullOrEmpty()) {
                onExpandClick(ignoreEvent = true)
            }

            handleHourlyForecastSwiping()
        }

        private fun handleHourlyForecastSwiping() {
            val gestureDetector = GestureDetector(
                itemView.context,
                HorizontalScrollGestureListener(binding.hourlyRecycler)
            )

            binding.hourlyRecycler.addOnItemTouchListener(object :
                RecyclerView.OnItemTouchListener {
                override fun onTouchEvent(view: RecyclerView, event: MotionEvent) {
                    // Do nothing
                }

                override fun onInterceptTouchEvent(
                    view: RecyclerView,
                    event: MotionEvent
                ): Boolean {
                    gestureDetector.onTouchEvent(event)
                    return false
                }

                override fun onRequestDisallowInterceptTouchEvent(disallowIntercept: Boolean) {
                    // Do nothing
                }
            })
        }

        private fun onExpandClick(ignoreEvent: Boolean = false) {
            /*
             * We define a new variable here because toggleVisibility uses an animation and has a
             * delay so we cannot use `binding.hourlyRecycler.isVisible` directly`.
             */
            val willBeExpanded = !binding.hourlyRecycler.isVisible
            if (binding.hourlyRecycler.isVisible) {
                binding.toggleExpand.icon =
                    AppCompatResources.getDrawable(itemView.context, R.drawable.ic_expand_more)
            } else {
                binding.toggleExpand.icon =
                    AppCompatResources.getDrawable(itemView.context, R.drawable.ic_expand_less)
            }
            binding.hourlyRecycler.toggleVisibility()

            if (!ignoreEvent) {
                onExpandToggle.invoke(absoluteAdapterPosition, willBeExpanded)
            }
        }
    }

    class UIForecastDiffCallback : DiffUtil.ItemCallback<UIForecast>() {

        override fun areItemsTheSame(oldItem: UIForecast, newItem: UIForecast): Boolean {
            return oldItem == newItem
        }

        override fun areContentsTheSame(oldItem: UIForecast, newItem: UIForecast): Boolean {
            return oldItem.nameOfDayAndDate == newItem.nameOfDayAndDate &&
                oldItem.icon == newItem.icon &&
                oldItem.maxTemp == newItem.maxTemp &&
                oldItem.minTemp == newItem.minTemp &&
                oldItem.precipProbability == newItem.precipProbability &&
                oldItem.windSpeed == newItem.windSpeed &&
                oldItem.windDirection == newItem.windDirection &&
                oldItem.humidity == newItem.humidity &&
                oldItem.hourlyWeather == newItem.hourlyWeather
        }
    }
}
