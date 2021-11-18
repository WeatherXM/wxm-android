package com.weatherxm.data

import co.infinum.retromock.Retromock
import com.weatherxm.data.network.ApiService
import org.koin.android.ext.koin.androidContext
import org.koin.core.qualifier.named
import org.koin.dsl.module

val apiServiceModule = module {
    single<ApiService> {
        Retromock.Builder()
            .retrofit(get(named(RETROFIT_CLIENT)))
            .defaultBodyFactory(androidContext().assets::open)
            .build()
            .create(ApiService::class.java)
    }
}