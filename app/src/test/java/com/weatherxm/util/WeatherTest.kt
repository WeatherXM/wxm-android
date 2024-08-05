package com.weatherxm.util

import android.content.SharedPreferences
import com.weatherxm.R
import com.weatherxm.util.Weather.getWeatherAnimation
import com.weatherxm.util.Weather.getWeatherStaticIcon
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.core.spec.style.scopes.BehaviorSpecGivenContainerScope
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module

class WeatherTest : BehaviorSpec({
    val resources = mockk<Resources>()
    val sharedPreferences = mockk<SharedPreferences>()

    beforeSpec {
        startKoin {
            modules(
                module {
                    single { resources }
                    single { sharedPreferences }
                }
            )
        }
    }

    suspend fun BehaviorSpecGivenContainerScope.testWeatherIcon(
        icon: String?,
        expectedAnimation: Int,
        expectedStaticIcon: Int?
    ) {
        When("$icon") {
            then("Get the animation") {
                getWeatherAnimation(icon) shouldBe expectedAnimation
            }
            then("Get the static icon") {
                getWeatherStaticIcon(icon) shouldBe expectedStaticIcon
            }
        }
    }

    given("A Weather icon") {
        testWeatherIcon("not-available", R.raw.anim_not_available, null)
        testWeatherIcon("clear-day", R.raw.anim_weather_clear_day, R.drawable.ic_weather_clear_day)
        testWeatherIcon("clear-night", R.raw.anim_weather_clear_night, R.drawable.ic_weather_clear_night)
        testWeatherIcon("partly-cloudy-day", R.raw.anim_weather_partly_cloudy_day, R.drawable.ic_weather_partly_cloudy_day)
        testWeatherIcon("partly-cloudy-night", R.raw.anim_weather_partly_cloudy_night, R.drawable.ic_weather_partly_cloudy_night)
        testWeatherIcon("overcast-day", R.raw.anim_weather_overcast_day, R.drawable.ic_weather_overcast_day)
        testWeatherIcon("overcast-night", R.raw.anim_weather_overcast_night, R.drawable.ic_weather_overcast_night)
        testWeatherIcon("drizzle", R.raw.anim_weather_drizzle, R.drawable.ic_weather_drizzle)
        testWeatherIcon("rain", R.raw.anim_weather_rain, R.drawable.ic_weather_rain)
        testWeatherIcon("thunderstorms-rain", R.raw.anim_weather_thunderstorms_rain, R.drawable.ic_weather_thunderstorms_rain)
        testWeatherIcon("snow", R.raw.anim_weather_snow, R.drawable.ic_weather_snow)
        testWeatherIcon("sleet", R.raw.anim_weather_sleet, R.drawable.ic_weather_sleet)
        testWeatherIcon("wind", R.raw.anim_weather_wind, R.drawable.ic_weather_windy)
        testWeatherIcon("fog", R.raw.anim_weather_fog, R.drawable.ic_weather_fog)
        testWeatherIcon("cloudy", R.raw.anim_weather_cloudy, R.drawable.ic_weather_cloudy)
        testWeatherIcon(null, R.raw.anim_not_available, null)
    }

    afterSpec {
        stopKoin()
    }

})
