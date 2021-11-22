package com.weatherxm.data

import com.weatherxm.data.network.ApiService
import com.weatherxm.data.network.AuthService
import org.koin.dsl.module
import retrofit2.Retrofit
import org.koin.core.qualifier.named

val apiServiceModule = module {
    single<ApiService> {
        val retrofit = get<Retrofit>(named(RETROFIT_API))
        retrofit.create(ApiService::class.java)
    }

    single<AuthService> {
        val retrofit = get<Retrofit>(named(RETROFIT_AUTH))
        retrofit.create(AuthService::class.java)
    }
}
