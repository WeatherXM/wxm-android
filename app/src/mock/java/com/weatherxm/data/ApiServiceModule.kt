package com.weatherxm.data

import co.infinum.retromock.Retromock
import com.weatherxm.data.datasource.DeviceDataSource
import com.weatherxm.data.datasource.DeviceDataSourceImpl
import com.weatherxm.data.datasource.TokenDataSource
import com.weatherxm.data.datasource.TokenDataSourceImpl
import com.weatherxm.data.datasource.WeatherDataSource
import com.weatherxm.data.datasource.WeatherDataSourceImpl
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

    single<DeviceDataSource> {
        DeviceDataSourceImpl(get())
    }

    single<TokenDataSource> {
        TokenDataSourceImpl(get())
    }

    single<WeatherDataSource> {
        WeatherDataSourceImpl(get())
    }
}
