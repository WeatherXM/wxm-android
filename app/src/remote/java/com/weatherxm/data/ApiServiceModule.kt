package com.weatherxm.data

import com.weatherxm.data.datasource.CacheWeatherDataSource
import com.weatherxm.data.datasource.NetworkWeatherDataSource
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

    single<NetworkWeatherDataSource> {
        NetworkWeatherDataSource(get())
    }

    single<CacheWeatherDataSource> {
        CacheWeatherDataSource()
    }
}
