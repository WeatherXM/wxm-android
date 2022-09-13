package com.weatherxm.data

import co.infinum.retromock.Retromock
import com.weatherxm.data.datasource.CacheWeatherForecastDataSource
import com.weatherxm.data.datasource.NetworkWeatherForecastDataSource
import com.weatherxm.data.network.ApiService
import com.weatherxm.data.network.AuthService
import org.koin.android.ext.koin.androidContext
import org.koin.core.qualifier.named
import org.koin.dsl.module

val apiServiceModule = module {
    single<ApiService> {
        Retromock.Builder()
            .retrofit(get(named(RETROFIT_API)))
            .defaultBodyFactory(androidContext().assets::open)
            .build()
            .create(ApiService::class.java)
    }

    single<AuthService> {
        Retromock.Builder()
            .retrofit(get(named(RETROFIT_AUTH)))
            .defaultBodyFactory(androidContext().assets::open)
            .build()
            .create(AuthService::class.java)
    }

    single<NetworkWeatherForecastDataSource> {
        NetworkWeatherForecastDataSource(get())
    }

    single<CacheWeatherForecastDataSource> {
        CacheWeatherForecastDataSource()
    }
}
