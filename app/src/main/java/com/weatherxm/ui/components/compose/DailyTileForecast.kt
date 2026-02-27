package com.weatherxm.ui.components.compose

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieComposition
import com.weatherxm.R
import com.weatherxm.ui.common.UIForecastDay
import com.weatherxm.util.Weather
import com.weatherxm.util.getShortName
import java.time.LocalDate

@Suppress("FunctionNaming", "LongMethod")
@Composable
fun DailyTileForecast(
    forecastDays: List<UIForecastDay>,
    selectedDate: LocalDate,
    isPremiumTabSelected: Boolean,
    onDaySelected: (LocalDate) -> Unit
) {
    var currentSelectedDate by rememberSaveable { mutableStateOf(selectedDate) }
    var selectedPosition by rememberSaveable { mutableIntStateOf(0) }
    val listState = rememberLazyListState()

    // Find initial selected position
    LaunchedEffect(forecastDays, selectedDate) {
        val position = forecastDays.indexOfFirst { it.date == selectedDate }
        if (position != -1) {
            selectedPosition = position
            currentSelectedDate = selectedDate
        }
    }

    LazyRow(
        state = listState,
        contentPadding = PaddingValues(horizontal = dimensionResource(R.dimen.padding_normal)),
        horizontalArrangement = Arrangement.Start
    ) {
        itemsIndexed(
            items = forecastDays,
            key = { _, item -> item.date.toString() }
        ) { index, forecastDay ->
            val isSelected = currentSelectedDate == forecastDay.date

            DailyTileItem(
                forecastDay = forecastDay,
                isSelected = isSelected,
                isPremiumTabSelected = isPremiumTabSelected,
                onClick = {
                    if (currentSelectedDate != forecastDay.date) {
                        currentSelectedDate = forecastDay.date
                        selectedPosition = index

                        onDaySelected(forecastDay.date)
                    }
                }
            )

            // Add spacing between items (except after last item)
            if (index < forecastDays.size - 1) {
                Spacer(modifier = Modifier.width(dimensionResource(R.dimen.margin_normal)))
            }
        }
    }

    // Auto-scroll to selected item with centering
    LaunchedEffect(selectedPosition) {
        if (selectedPosition in forecastDays.indices) {
            // Calculate offset to center the item
            listState.animateScrollToItem(
                index = selectedPosition,
                scrollOffset = 0
            )
        }
    }
}

@Suppress("FunctionNaming")
@Composable
private fun DailyTileItem(
    forecastDay: UIForecastDay,
    isSelected: Boolean,
    isPremiumTabSelected: Boolean,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val shape = RoundedCornerShape(dimensionResource(R.dimen.radius_large))
    val borderModifier = if (isSelected) {
        if (isPremiumTabSelected) {
            Modifier.border(
                width = 1.dp,
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        colorResource(R.color.blue),
                        colorResource(R.color.forecast_premium)
                    )
                ),
                shape = shape
            )
        } else {
            Modifier.border(
                width = 1.dp,
                color = colorResource(R.color.colorPrimary),
                shape = shape
            )
        }
    } else {
        Modifier
    }

    Surface(
        modifier = borderModifier,
        onClick = onClick,
        shape = shape,
        color = if (isSelected) {
            colorResource(R.color.daily_selected_tile)
        } else {
            colorResource(R.color.daily_unselected_tile)
        },
        shadowElevation = dimensionResource(R.dimen.elevation_normal)
    ) {
        Column(
            modifier = Modifier
                .padding(
                    horizontal = dimensionResource(R.dimen.padding_normal),
                    vertical = dimensionResource(R.dimen.padding_small)
                ),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = context.getString(forecastDay.date.dayOfWeek.getShortName()),
                modifier = Modifier.padding(bottom = dimensionResource(R.dimen.margin_small)),
                style = MaterialTheme.typography.bodySmall,
                color = colorResource(R.color.darkestBlue),
                textAlign = TextAlign.Center
            )

            val animationComposition by rememberLottieComposition(
                LottieCompositionSpec.RawRes(Weather.getWeatherAnimation(forecastDay.icon))
            )

            LottieAnimation(
                composition = animationComposition,
                iterations = LottieConstants.IterateForever,
                modifier = Modifier.size(40.dp)
            )

            Text(
                text = Weather.getFormattedTemperature(context, forecastDay.maxTemp),
                modifier = Modifier.padding(top = dimensionResource(R.dimen.margin_small)),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = colorResource(R.color.darkestBlue),
                textAlign = TextAlign.Center
            )

            Text(
                text = Weather.getFormattedTemperature(context, forecastDay.minTemp),
                style = MaterialTheme.typography.bodySmall,
                color = colorResource(R.color.darkestBlue),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Suppress("FunctionNaming", "UnusedPrivateMember")
@Preview(showBackground = true)
@Composable
private fun PreviewDailyTileForecast() {
    val forecastDays = listOf(
        UIForecastDay(
            date = LocalDate.now(),
            icon = "clear-day",
            minTemp = 15.4f,
            maxTemp = 25.6f,
            precipProbability = 20,
            precip = 0.5f,
            windSpeed = 10.5f,
            windDirection = 180,
            humidity = 65,
            pressure = 1013.25f,
            uv = 5,
            hourlyWeather = null
        ),
        UIForecastDay(
            date = LocalDate.now().plusDays(1),
            icon = "partly-cloudy-day",
            minTemp = 14.2f,
            maxTemp = 23.8f,
            precipProbability = 30,
            precip = 1.2f,
            windSpeed = 12.0f,
            windDirection = 200,
            humidity = 70,
            pressure = 1012.5f,
            uv = 4,
            hourlyWeather = null
        ),
        UIForecastDay(
            date = LocalDate.now().plusDays(2),
            icon = "rain",
            minTemp = 12.0f,
            maxTemp = 18.5f,
            precipProbability = 80,
            precip = 5.5f,
            windSpeed = 15.5f,
            windDirection = 220,
            humidity = 85,
            pressure = 1010.0f,
            uv = 2,
            hourlyWeather = null
        )
    )

    DailyTileForecast(
        forecastDays = forecastDays,
        selectedDate = LocalDate.now(),
        isPremiumTabSelected = true,
        onDaySelected = { _ -> }
    )
}
