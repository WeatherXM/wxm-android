package com.weatherxm.data

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.SettingsClient
import com.google.gson.FieldNamingPolicy
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.haroldadmin.cnradapter.NetworkResponseAdapterFactory
import com.squareup.moshi.Moshi
import com.weatherxm.BuildConfig
import com.weatherxm.data.datasource.UserDataSource
import com.weatherxm.data.datasource.UserDataSourceImpl
import com.weatherxm.data.datasource.AuthTokenDataSource
import com.weatherxm.data.datasource.AuthTokenDataSourceImpl
import com.weatherxm.data.datasource.LocationDataSource
import com.weatherxm.data.datasource.LocationDataSourceImpl
import com.weatherxm.data.datasource.DeviceDataSource
import com.weatherxm.data.datasource.DeviceDataSourceImpl
import com.weatherxm.data.datasource.CredentialsDataSource
import com.weatherxm.data.datasource.CredentialsDataSourceImpl
import com.weatherxm.data.network.AuthService
import com.weatherxm.data.network.AuthTokenJsonAdapter
import com.weatherxm.data.network.interceptor.ApiRequestInterceptor
import com.weatherxm.data.network.interceptor.AuthRequestInterceptor
import com.weatherxm.data.network.interceptor.AuthTokenAuthenticator
import com.weatherxm.data.repository.AuthRepository
import com.weatherxm.data.repository.AuthRepositoryImpl
import com.weatherxm.data.repository.DeviceRepository
import com.weatherxm.data.repository.UserRepository
import com.weatherxm.data.repository.LocationRepository
import com.weatherxm.ui.Navigator
import com.weatherxm.ui.explorer.DeviceWithResolutionJsonAdapter
import com.weatherxm.usecases.ExplorerUseCase
import com.weatherxm.usecases.ExplorerUseCaseImpl
import com.weatherxm.util.ResourcesHelper
import com.weatherxm.util.Validator
import okhttp3.Cache
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import okhttp3.logging.HttpLoggingInterceptor.Level
import org.koin.android.ext.koin.androidContext
import org.koin.core.qualifier.named
import org.koin.dsl.module
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

const val RETROFIT_CLIENT = "RETROFIT_CLIENT"
private const val PREFERENCE_AUTH_TOKEN = "PREFS_AUTH_TOKEN"
private const val PREFERENCE_CREDENTIALS = "PREFS_CREDENTIALS"
private const val NETWORK_CACHE_SIZE = 50L * 1024L * 1024L // 50MB
private const val CONNECT_TIMEOUT = 30L
private const val READ_TIMEOUT = 30L
private const val WRITE_TIMEOUT = 60L

private val preferences = module {
    single<SharedPreferences> {
        PreferenceManager.getDefaultSharedPreferences(androidContext())
    }

    single<SharedPreferences>(named(PREFERENCE_AUTH_TOKEN)) {
        androidContext().getSharedPreferences(PREFERENCE_AUTH_TOKEN, Context.MODE_PRIVATE)
    }

    single<SharedPreferences>(named(PREFERENCE_CREDENTIALS)) {
        androidContext().getSharedPreferences(PREFERENCE_CREDENTIALS, Context.MODE_PRIVATE)
    }
}

private val datasources = module {
    single<LocationDataSource> {
        LocationDataSourceImpl()
    }

    single<DeviceDataSource> {
        DeviceDataSourceImpl(get())
    }

    single<UserDataSource> {
        UserDataSourceImpl(get())
    }

    single<AuthTokenDataSource> {
        AuthTokenDataSourceImpl(get(named(PREFERENCE_AUTH_TOKEN)))
    }

    single<CredentialsDataSource> {
        CredentialsDataSourceImpl(get(named(PREFERENCE_CREDENTIALS)))
    }
}

private val repositories = module {
    single<AuthRepository> {
        AuthRepositoryImpl(get(), get())
    }
    single<LocationRepository> {
        LocationRepository()
    }
    single<UserRepository> {
        UserRepository(get())
    }
    single<DeviceRepository> {
        DeviceRepository(get())
    }
}

private val usecases = module {
    single<ExplorerUseCase> {
        ExplorerUseCaseImpl()
    }
}

private val location = module {
    single<FusedLocationProviderClient> {
        LocationServices.getFusedLocationProviderClient(androidContext())
    }

    single<SettingsClient> {
        LocationServices.getSettingsClient(androidContext())
    }
}

private val network = module {
    single<HttpLoggingInterceptor> {
        HttpLoggingInterceptor().setLevel(if (BuildConfig.DEBUG) Level.BASIC else Level.NONE)
    }

    single<MoshiConverterFactory> {
        MoshiConverterFactory.create(Moshi.Builder().build()).asLenient()
    }

    single<AuthService> {
        // Create client
        val client: OkHttpClient = OkHttpClient.Builder()
            .addInterceptor(get() as HttpLoggingInterceptor)
            .addInterceptor(AuthRequestInterceptor())
            .connectTimeout(CONNECT_TIMEOUT, TimeUnit.SECONDS)
            .readTimeout(READ_TIMEOUT, TimeUnit.SECONDS)
            .writeTimeout(WRITE_TIMEOUT, TimeUnit.SECONDS)
            .build()

        // Create retrofit instance
        val retrofit: Retrofit = Retrofit.Builder()
            .baseUrl(BuildConfig.AUTH_URL)
            .addConverterFactory(get() as MoshiConverterFactory)
            .addCallAdapterFactory(NetworkResponseAdapterFactory())
            .client(client)
            .build()

        // Create service
        retrofit.create(AuthService::class.java)
    }

    single(named(RETROFIT_CLIENT)) {
        // Install HTTP cache
        val cache = Cache(androidContext().cacheDir, NETWORK_CACHE_SIZE)

        // Create client
        val client: OkHttpClient = OkHttpClient.Builder()
            .authenticator(AuthTokenAuthenticator())
            .addInterceptor(ApiRequestInterceptor())
            .addInterceptor(get() as HttpLoggingInterceptor)
            .connectTimeout(CONNECT_TIMEOUT, TimeUnit.SECONDS)
            .readTimeout(READ_TIMEOUT, TimeUnit.SECONDS)
            .writeTimeout(WRITE_TIMEOUT, TimeUnit.SECONDS)
            .cache(cache)
            .build()

        // Create retrofit instance
        Retrofit.Builder()
            .baseUrl(BuildConfig.API_URL)
            .addConverterFactory(get() as MoshiConverterFactory)
            .addCallAdapterFactory(NetworkResponseAdapterFactory())
            .client(client)
            .build()
    }
}

val validator = module {
    single {
        Validator()
    }
}

val navigator = module {
    single {
        Navigator()
    }
}

val resourcesHelper = module {
    single {
        ResourcesHelper(androidContext().resources)
    }
}

private val utilities = module {
    single<Moshi> {
        Moshi.Builder().build()
    }

    single<Gson> {
        GsonBuilder()
            .setPrettyPrinting()
            .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
            .create()
    }

    single<DeviceWithResolutionJsonAdapter> {
        DeviceWithResolutionJsonAdapter(get())
    }

    single<AuthTokenJsonAdapter> {
        AuthTokenJsonAdapter(get())
    }
}

val modules = listOf(
    preferences,
    network,
    datasources,
    repositories,
    location,
    navigator,
    validator,
    usecases,
    resourcesHelper,
    apiServiceModule,
    utilities
)
