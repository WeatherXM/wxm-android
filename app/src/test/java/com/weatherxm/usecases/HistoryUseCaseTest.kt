package com.weatherxm.usecases

import com.weatherxm.TestConfig.failure
import com.weatherxm.TestUtils.coMockEitherLeft
import com.weatherxm.TestUtils.coMockEitherRight
import com.weatherxm.TestUtils.isError
import com.weatherxm.TestUtils.isSuccess
import com.weatherxm.data.models.HourlyWeather
import com.weatherxm.data.repository.WeatherHistoryRepository
import com.weatherxm.ui.common.UIDevice
import io.kotest.core.spec.style.BehaviorSpec
import io.mockk.mockk
import java.time.LocalDate

class HistoryUseCaseTest : BehaviorSpec({
    val repo = mockk<WeatherHistoryRepository>()
    val usecase = HistoryUseCaseImpl(repo)

    val device = UIDevice.empty()
    val localDate = LocalDate.now()
    val forceUpdate = false
    val hourlyWeatherData = mockk<List<HourlyWeather>>()

    context("Get Weather History") {
        given("A repository providing the historical data") {
            When("it's a success") {
                coMockEitherRight({
                    repo.getHourlyWeatherHistory(device.id, localDate, forceUpdate)
                }, hourlyWeatherData)
                then("return the success") {
                    usecase.getWeatherHistory(device, localDate, forceUpdate)
                        .isSuccess(hourlyWeatherData)
                }
            }
            When("it's a failure") {
                coMockEitherLeft({
                    repo.getHourlyWeatherHistory(device.id, localDate)
                }, failure)
                then("return that failure") {
                    usecase.getWeatherHistory(device, localDate).isError()
                }
            }
        }
    }
})
