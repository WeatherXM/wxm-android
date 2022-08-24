package com.weatherxm.data

import com.weatherxm.data.datasource.CacheWeatherForecastDataSource
import com.weatherxm.data.datasource.NetworkWeatherForecastDataSource
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

    single<NetworkWeatherForecastDataSource> {
        NetworkWeatherForecastDataSource(get())
    }

    single<CacheWeatherForecastDataSource> {
        CacheWeatherForecastDataSource()
    }
}
