package com.weatherxm.data

import com.weatherxm.data.network.ApiService
import org.koin.core.qualifier.named
import org.koin.dsl.module
import retrofit2.Retrofit

val apiServiceModule = module {
    single<ApiService> {
        val retrofit = get<Retrofit>(named(RETROFIT_CLIENT))
        retrofit.create(ApiService::class.java)
    }
}
