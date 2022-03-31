package com.weatherxm.data

import com.weatherxm.data.datasource.DeviceDataSource
import com.weatherxm.data.datasource.DeviceDataSourceImpl
import com.weatherxm.data.datasource.WeatherDataSource
import com.weatherxm.data.datasource.WeatherDataSourceImpl
import com.weatherxm.data.network.ApiService
import com.weatherxm.data.network.AuthService
import org.koin.core.qualifier.named
import org.koin.dsl.module
import retrofit2.Retrofit

val apiServiceModule = module {
    single<ApiService> {
        val retrofit = get<Retrofit>(named(RETROFIT_API))
        retrofit.create(ApiService::class.java)
    }

    single<AuthService> {
        val retrofit = get<Retrofit>(named(RETROFIT_AUTH))
        retrofit.create(AuthService::class.java)
    }

    single<DeviceDataSource> {
        DeviceDataSourceImpl(get())
    }

    single<WeatherDataSource> {
        WeatherDataSourceImpl(get())
    }
}
