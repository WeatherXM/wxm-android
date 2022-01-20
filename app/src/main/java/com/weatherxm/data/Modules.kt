package com.weatherxm.data

import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences.PrefKeyEncryptionScheme
import androidx.security.crypto.EncryptedSharedPreferences.PrefValueEncryptionScheme
import androidx.security.crypto.MasterKeys
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.SettingsClient
import com.google.gson.FieldNamingPolicy
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.haroldadmin.cnradapter.NetworkResponseAdapterFactory
import com.squareup.moshi.Moshi
import com.weatherxm.BuildConfig
import com.weatherxm.data.datasource.AuthDataSource
import com.weatherxm.data.datasource.AuthDataSourceImpl
import com.weatherxm.data.datasource.AuthTokenDataSource
import com.weatherxm.data.datasource.AuthTokenDataSourceImpl
import com.weatherxm.data.datasource.CredentialsDataSource
import com.weatherxm.data.datasource.CredentialsDataSourceImpl
import com.weatherxm.data.datasource.LocationDataSource
import com.weatherxm.data.datasource.LocationDataSourceImpl
import com.weatherxm.data.datasource.UserDataSource
import com.weatherxm.data.datasource.UserDataSourceImpl
import com.weatherxm.data.network.AuthTokenJsonAdapter
import com.weatherxm.data.network.interceptor.ApiRequestInterceptor
import com.weatherxm.data.network.interceptor.AuthRequestInterceptor
import com.weatherxm.data.network.interceptor.AuthTokenAuthenticator
import com.weatherxm.data.repository.AuthRepository
import com.weatherxm.data.repository.AuthRepositoryImpl
import com.weatherxm.data.repository.DeviceRepository
import com.weatherxm.data.repository.LocationRepository
import com.weatherxm.data.repository.TokenRepository
import com.weatherxm.data.repository.UserRepository
import com.weatherxm.data.repository.WeatherRepository
import com.weatherxm.ui.Navigator
import com.weatherxm.ui.explorer.DeviceWithResolutionJsonAdapter
import com.weatherxm.usecases.ExplorerUseCase
import com.weatherxm.usecases.ExplorerUseCaseImpl
import com.weatherxm.usecases.UserDeviceUseCase
import com.weatherxm.usecases.UserDeviceUseCaseImpl
import com.weatherxm.util.Mask
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

const val RETROFIT_API = "RETROFIT_API"
const val RETROFIT_AUTH = "RETROFIT_AUTH"

private const val ENCRYPTED_PREFERENCES_KEY = "ENCRYPTED_PREFERENCES_KEY"
private const val PREFERENCES_AUTH_TOKEN = "PREFERENCES_AUTH_TOKEN"
private const val PREFERENCES_AUTH_TOKEN_FILE = "auth_token"
private const val PREFERENCES_CREDENTIALS = "PREFERENCES_CREDENTIALS"
private const val PREFERENCES_CREDENTIALS_FILE = "credentials"
private const val NETWORK_CACHE_SIZE = 50L * 1024L * 1024L // 50MB
private const val CONNECT_TIMEOUT = 30L
private const val READ_TIMEOUT = 30L
private const val WRITE_TIMEOUT = 60L

private val preferences = module {
    single<SharedPreferences> {
        PreferenceManager.getDefaultSharedPreferences(androidContext())
    }

    // Encrypted SharedPreferences key
    single<String>(named(ENCRYPTED_PREFERENCES_KEY)) {
        MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
    }

    // Encrypted SharedPreferences for storing auth token
    single<SharedPreferences>(named(PREFERENCES_AUTH_TOKEN)) {
        EncryptedSharedPreferences.create(
            PREFERENCES_AUTH_TOKEN_FILE,
            get(named(ENCRYPTED_PREFERENCES_KEY)),
            androidContext(),
            PrefKeyEncryptionScheme.AES256_SIV,
            PrefValueEncryptionScheme.AES256_GCM
        )
    }

    // Encrypted SharedPreferences for storing user credentials
    single<SharedPreferences>(named(PREFERENCES_CREDENTIALS)) {
        EncryptedSharedPreferences.create(
            PREFERENCES_CREDENTIALS_FILE,
            get(named(ENCRYPTED_PREFERENCES_KEY)),
            androidContext(),
            PrefKeyEncryptionScheme.AES256_SIV,
            PrefValueEncryptionScheme.AES256_GCM
        )
    }
}

private val datasources = module {
    single<LocationDataSource> {
        LocationDataSourceImpl()
    }

    single<UserDataSource> {
        UserDataSourceImpl(get())
    }

    single<AuthDataSource> {
        AuthDataSourceImpl(get(), get(), get())
    }

    single<AuthTokenDataSource> {
        AuthTokenDataSourceImpl(get(named(PREFERENCES_AUTH_TOKEN)))
    }

    single<CredentialsDataSource> {
        CredentialsDataSourceImpl(get(named(PREFERENCES_CREDENTIALS)))
    }
}

private val repositories = module {
    single<AuthRepository> {
        AuthRepositoryImpl(get(), get(), get())
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
    single<TokenRepository> {
        TokenRepository(get())
    }
    single<WeatherRepository> {
        WeatherRepository(get())
    }
}

private val usecases = module {
    single<ExplorerUseCase> {
        ExplorerUseCaseImpl()
    }
    single<UserDeviceUseCase> {
        UserDeviceUseCaseImpl()
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

    single<Retrofit>(named(RETROFIT_AUTH)) {
        // Create client
        val client: OkHttpClient = OkHttpClient.Builder()
            .addInterceptor(get() as HttpLoggingInterceptor)
            .addInterceptor(AuthRequestInterceptor())
            .connectTimeout(CONNECT_TIMEOUT, TimeUnit.SECONDS)
            .readTimeout(READ_TIMEOUT, TimeUnit.SECONDS)
            .writeTimeout(WRITE_TIMEOUT, TimeUnit.SECONDS)
            .build()

        // Create retrofit instance
        Retrofit.Builder()
            .baseUrl(BuildConfig.AUTH_URL)
            .addConverterFactory(get() as MoshiConverterFactory)
            .addCallAdapterFactory(NetworkResponseAdapterFactory())
            .client(client)
            .build()
    }

    single<Retrofit>(named(RETROFIT_API)) {
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

    single<Mask> {
        Mask()
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
