package com.weatherxm.data

import android.content.SharedPreferences
import android.text.format.DateFormat
import androidx.preference.PreferenceManager
import androidx.room.Room
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences.PrefKeyEncryptionScheme
import androidx.security.crypto.EncryptedSharedPreferences.PrefValueEncryptionScheme
import androidx.security.crypto.MasterKeys
import com.espressif.provisioning.ESPConstants
import com.espressif.provisioning.ESPDevice
import com.espressif.provisioning.ESPProvisionManager
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.SettingsClient
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.ktx.remoteConfig
import com.google.firebase.remoteconfig.ktx.remoteConfigSettings
import com.google.gson.FieldNamingPolicy
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.haroldadmin.cnradapter.NetworkResponseAdapterFactory
import com.squareup.moshi.Moshi
import com.weatherxm.BuildConfig
import com.weatherxm.data.adapters.LocalDateJsonAdapter
import com.weatherxm.data.adapters.LocalDateTimeJsonAdapter
import com.weatherxm.data.adapters.ZonedDateTimeJsonAdapter
import com.weatherxm.data.bluetooth.BluetoothConnectionManager
import com.weatherxm.data.bluetooth.BluetoothProvisioner
import com.weatherxm.data.bluetooth.BluetoothScanner
import com.weatherxm.data.bluetooth.BluetoothUpdater
import com.weatherxm.data.database.AppDatabase
import com.weatherxm.data.database.DatabaseConverters
import com.weatherxm.data.database.dao.DeviceHistoryDao
import com.weatherxm.data.datasource.AppConfigDataSource
import com.weatherxm.data.datasource.AppConfigDataSourceImpl
import com.weatherxm.data.datasource.AuthDataSource
import com.weatherxm.data.datasource.AuthDataSourceImpl
import com.weatherxm.data.datasource.AuthTokenDataSource
import com.weatherxm.data.datasource.AuthTokenDataSourceImpl
import com.weatherxm.data.datasource.CacheUserDataSource
import com.weatherxm.data.datasource.CacheWalletDataSource
import com.weatherxm.data.datasource.CacheWeatherForecastDataSource
import com.weatherxm.data.datasource.CredentialsDataSource
import com.weatherxm.data.datasource.CredentialsDataSourceImpl
import com.weatherxm.data.datasource.DatabaseWeatherHistoryDataSource
import com.weatherxm.data.datasource.DeviceDataSource
import com.weatherxm.data.datasource.DeviceDataSourceImpl
import com.weatherxm.data.datasource.ExplorerDataSource
import com.weatherxm.data.datasource.ExplorerDataSourceImpl
import com.weatherxm.data.datasource.HttpCacheDataSource
import com.weatherxm.data.datasource.HttpCacheDataSourceImpl
import com.weatherxm.data.datasource.LocationDataSource
import com.weatherxm.data.datasource.LocationDataSourceImpl
import com.weatherxm.data.datasource.NetworkAddressDataSource
import com.weatherxm.data.datasource.NetworkUserDataSource
import com.weatherxm.data.datasource.NetworkWalletDataSource
import com.weatherxm.data.datasource.NetworkWeatherForecastDataSource
import com.weatherxm.data.datasource.NetworkWeatherHistoryDataSource
import com.weatherxm.data.datasource.SharedPreferencesDataSource
import com.weatherxm.data.datasource.SharedPreferencesDataSourceImpl
import com.weatherxm.data.datasource.StorageAddressDataSource
import com.weatherxm.data.datasource.TokenDataSource
import com.weatherxm.data.datasource.TokenDataSourceImpl
import com.weatherxm.data.datasource.UserActionDataSource
import com.weatherxm.data.datasource.UserActionDataSourceImpl
import com.weatherxm.data.datasource.bluetooth.BluetoothConnectionDataSource
import com.weatherxm.data.datasource.bluetooth.BluetoothConnectionDataSourceImpl
import com.weatherxm.data.datasource.bluetooth.BluetoothProvisionerDataSource
import com.weatherxm.data.datasource.bluetooth.BluetoothProvisionerDataSourceImpl
import com.weatherxm.data.datasource.bluetooth.BluetoothScannerDataSource
import com.weatherxm.data.datasource.bluetooth.BluetoothScannerDataSourceImpl
import com.weatherxm.data.datasource.bluetooth.BluetoothUpdaterDataSource
import com.weatherxm.data.datasource.bluetooth.BluetoothUpdaterDataSourceImpl
import com.weatherxm.data.network.AuthTokenJsonAdapter
import com.weatherxm.data.network.interceptor.ApiRequestInterceptor
import com.weatherxm.data.network.interceptor.AuthRequestInterceptor
import com.weatherxm.data.network.interceptor.AuthTokenAuthenticator
import com.weatherxm.data.network.interceptor.ClientIdentificationRequestInterceptor
import com.weatherxm.data.repository.AppConfigRepository
import com.weatherxm.data.repository.AppConfigRepositoryImpl
import com.weatherxm.data.repository.AuthRepository
import com.weatherxm.data.repository.AuthRepositoryImpl
import com.weatherxm.data.repository.DeviceRepository
import com.weatherxm.data.repository.DeviceRepositoryImpl
import com.weatherxm.data.repository.ExplorerRepository
import com.weatherxm.data.repository.ExplorerRepositoryImpl
import com.weatherxm.data.repository.LocationRepository
import com.weatherxm.data.repository.LocationRepositoryImpl
import com.weatherxm.data.repository.SharedPreferenceRepositoryImpl
import com.weatherxm.data.repository.SharedPreferencesRepository
import com.weatherxm.data.repository.TokenRepository
import com.weatherxm.data.repository.TokenRepositoryImpl
import com.weatherxm.data.repository.UserRepository
import com.weatherxm.data.repository.UserRepositoryImpl
import com.weatherxm.data.repository.WalletRepository
import com.weatherxm.data.repository.WalletRepositoryImpl
import com.weatherxm.data.repository.WeatherForecastRepository
import com.weatherxm.data.repository.WeatherForecastRepositoryImpl
import com.weatherxm.data.repository.WeatherHistoryRepository
import com.weatherxm.data.repository.WeatherHistoryRepositoryImpl
import com.weatherxm.data.repository.bluetooth.BluetoothConnectionRepository
import com.weatherxm.data.repository.bluetooth.BluetoothConnectionRepositoryImpl
import com.weatherxm.data.repository.bluetooth.BluetoothProvisionerRepository
import com.weatherxm.data.repository.bluetooth.BluetoothProvisionerRepositoryImpl
import com.weatherxm.data.repository.bluetooth.BluetoothScannerRepository
import com.weatherxm.data.repository.bluetooth.BluetoothScannerRepositoryImpl
import com.weatherxm.ui.Navigator
import com.weatherxm.ui.UIHexJsonAdapter
import com.weatherxm.ui.devicehistory.HistoryChartsViewModel
import com.weatherxm.usecases.AuthUseCase
import com.weatherxm.usecases.AuthUseCaseImpl
import com.weatherxm.usecases.BluetoothConnectionUseCase
import com.weatherxm.usecases.BluetoothConnectionUseCaseImpl
import com.weatherxm.usecases.BluetoothScannerUseCase
import com.weatherxm.usecases.BluetoothScannerUseCaseImpl
import com.weatherxm.usecases.ClaimDeviceUseCase
import com.weatherxm.usecases.ClaimDeviceUseCaseImpl
import com.weatherxm.usecases.ConnectWalletUseCase
import com.weatherxm.usecases.ConnectWalletUseCaseImpl
import com.weatherxm.usecases.ExplorerUseCase
import com.weatherxm.usecases.ExplorerUseCaseImpl
import com.weatherxm.usecases.ForecastUseCase
import com.weatherxm.usecases.ForecastUseCaseImpl
import com.weatherxm.usecases.HistoryUseCase
import com.weatherxm.usecases.HistoryUseCaseImpl
import com.weatherxm.usecases.PreferencesUseCase
import com.weatherxm.usecases.PreferencesUseCaseImpl
import com.weatherxm.usecases.SelectDeviceTypeUseCase
import com.weatherxm.usecases.SelectDeviceTypeUseCaseImpl
import com.weatherxm.usecases.SendFeedbackUseCase
import com.weatherxm.usecases.SendFeedbackUseCaseImpl
import com.weatherxm.usecases.StartupUseCase
import com.weatherxm.usecases.StartupUseCaseImpl
import com.weatherxm.usecases.TokenUseCase
import com.weatherxm.usecases.TokenUseCaseImpl
import com.weatherxm.usecases.UserDeviceUseCase
import com.weatherxm.usecases.UserDeviceUseCaseImpl
import com.weatherxm.usecases.UserUseCase
import com.weatherxm.usecases.UserUseCaseImpl
import com.weatherxm.util.DisplayModeHelper
import com.weatherxm.util.Mask
import com.weatherxm.util.ResourcesHelper
import com.weatherxm.util.Validator
import okhttp3.Cache
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import okhttp3.logging.HttpLoggingInterceptor.Level
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.qualifier.named
import org.koin.dsl.module
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import java.util.concurrent.TimeUnit

const val RETROFIT_API = "RETROFIT_API"
const val RETROFIT_AUTH = "RETROFIT_AUTH"
const val APP_DATABASE_NAME = "WEATHERXM"

const val HOUR_FORMAT_24H = "HH:mm"
const val HOUR_FORMAT_12H_FULL = "h:mm a"
const val HOUR_FORMAT_12H_HOUR_ONLY = "h a"
const val DATE_FORMAT_MONTH_DAY = "d/M"
private const val ENCRYPTED_PREFERENCES_KEY = "ENCRYPTED_PREFERENCES_KEY"
private const val PREFERENCES_AUTH_TOKEN = "PREFERENCES_AUTH_TOKEN"
private const val PREFERENCES_AUTH_TOKEN_FILE = "auth_token"
private const val PREFERENCES_CREDENTIALS = "PREFERENCES_CREDENTIALS"
private const val PREFERENCES_CREDENTIALS_FILE = "credentials"
private const val NETWORK_CACHE_SIZE = 50L * 1024L * 1024L // 50MB

private const val CONNECT_TIMEOUT = 30L
private const val READ_TIMEOUT = 30L
private const val WRITE_TIMEOUT = 60L
private const val FIREBASE_CONFIG_FETCH_INTERVAL_DEBUG = 30L
private const val FIREBASE_CONFIG_FETCH_INTERVAL_RELEASE = 3600L

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
        LocationDataSourceImpl(get())
    }

    single<NetworkWeatherHistoryDataSource> {
        NetworkWeatherHistoryDataSource(get())
    }

    single<DatabaseWeatherHistoryDataSource> {
        DatabaseWeatherHistoryDataSource(get())
    }

    single<NetworkUserDataSource> {
        NetworkUserDataSource(get())
    }

    single<CacheUserDataSource> {
        CacheUserDataSource()
    }

    single<DeviceDataSource> {
        DeviceDataSourceImpl(get())
    }

    single<NetworkWalletDataSource> {
        NetworkWalletDataSource(get())
    }

    single<CacheWalletDataSource> {
        CacheWalletDataSource()
    }

    single<TokenDataSource> {
        TokenDataSourceImpl(get())
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

    single<AppConfigDataSource> {
        AppConfigDataSourceImpl(get(), get())
    }

    single<ExplorerDataSource> {
        ExplorerDataSourceImpl(get())
    }

    single<NetworkAddressDataSource> {
        NetworkAddressDataSource(androidContext())
    }

    single<StorageAddressDataSource> {
        StorageAddressDataSource(get())
    }

    single<UserActionDataSource> {
        UserActionDataSourceImpl(get())
    }

    single<SharedPreferencesDataSource> {
        SharedPreferencesDataSourceImpl(get())
    }

    single<NetworkWeatherForecastDataSource> {
        NetworkWeatherForecastDataSource(get())
    }

    single<CacheWeatherForecastDataSource> {
        CacheWeatherForecastDataSource()
    }

    single<HttpCacheDataSource> {
        HttpCacheDataSourceImpl(get())
    }

    single<BluetoothScannerDataSource> {
        BluetoothScannerDataSourceImpl(get())
    }

    single<BluetoothConnectionDataSource> {
        BluetoothConnectionDataSourceImpl(get())
    }

    single<BluetoothUpdaterDataSource> {
        BluetoothUpdaterDataSourceImpl(get())
    }

    single<BluetoothProvisionerDataSource> {
        BluetoothProvisionerDataSourceImpl(get())
    }
}

private val repositories = module {
    single<AuthRepository> {
        AuthRepositoryImpl(get(), get(), get(), get(), get(), get(), get())
    }
    single<LocationRepository> {
        LocationRepositoryImpl(get())
    }
    single<UserRepository> {
        UserRepositoryImpl(get(), get(), get())
    }
    single<WalletRepository> {
        WalletRepositoryImpl(get(), get())
    }
    single<DeviceRepository> {
        DeviceRepositoryImpl(get(), get(), get(), get())
    }
    single<ExplorerRepository> {
        ExplorerRepositoryImpl(get(), get(), get())
    }
    single<TokenRepository> {
        TokenRepositoryImpl(get())
    }
    single<WeatherForecastRepository> {
        WeatherForecastRepositoryImpl(get(), get())
    }
    single<WeatherHistoryRepository> {
        WeatherHistoryRepositoryImpl(get(), get())
    }
    single<AppConfigRepository> {
        AppConfigRepositoryImpl(get())
    }
    single<SharedPreferencesRepository> {
        SharedPreferenceRepositoryImpl(get())
    }
    single<BluetoothScannerRepository> {
        BluetoothScannerRepositoryImpl(get())
    }
    single<BluetoothConnectionRepository> {
        BluetoothConnectionRepositoryImpl(get())
    }
    single<BluetoothProvisionerRepository> {
        BluetoothProvisionerRepositoryImpl(get())
    }
}

private val usecases = module {
    single<StartupUseCase> {
        StartupUseCaseImpl(get(), get())
    }
    single<ExplorerUseCase> {
        ExplorerUseCaseImpl(get(), get(), get(), get())
    }
    single<UserDeviceUseCase> {
        UserDeviceUseCaseImpl(get(), get(), get(), get())
    }
    single<HistoryUseCase> {
        HistoryUseCaseImpl(androidContext(), get(), get())
    }
    single<ForecastUseCase> {
        ForecastUseCaseImpl(androidContext(), get())
    }
    single<ClaimDeviceUseCase> {
        ClaimDeviceUseCaseImpl(get(), get())
    }
    single<TokenUseCase> {
        TokenUseCaseImpl(get(), get())
    }
    single<AuthUseCase> {
        AuthUseCaseImpl(get(), get())
    }
    single<UserUseCase> {
        UserUseCaseImpl(get(), get())
    }
    single<ConnectWalletUseCase> {
        ConnectWalletUseCaseImpl(get())
    }
    single<PreferencesUseCase> {
        PreferencesUseCaseImpl(get(), get(), get(), get())
    }
    single<SendFeedbackUseCase> {
        SendFeedbackUseCaseImpl(get(), get())
    }
    single<BluetoothScannerUseCase> {
        BluetoothScannerUseCaseImpl(get())
    }
    single<SelectDeviceTypeUseCase> {
        SelectDeviceTypeUseCaseImpl()
    }
    single<BluetoothConnectionUseCase> {
        BluetoothConnectionUseCaseImpl(get(), get())
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
    single<Cache> {
        // Install HTTP cache
        Cache(androidContext().cacheDir, NETWORK_CACHE_SIZE)
    }

    single<HttpLoggingInterceptor> {
        HttpLoggingInterceptor().setLevel(if (BuildConfig.DEBUG) Level.BASIC else Level.NONE)
    }

    single<ClientIdentificationRequestInterceptor> {
        ClientIdentificationRequestInterceptor(androidContext())
    }

    single<MoshiConverterFactory> {
        MoshiConverterFactory.create(get()).asLenient()
    }

    single<Retrofit>(named(RETROFIT_AUTH)) {
        // Create client
        val client: OkHttpClient = OkHttpClient.Builder()
            .addInterceptor(get() as HttpLoggingInterceptor)
            .addInterceptor(get() as ClientIdentificationRequestInterceptor)
            .addInterceptor(AuthRequestInterceptor(get(), get()))
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

        // Create client
        val client: OkHttpClient = OkHttpClient.Builder()
            .addInterceptor(get() as HttpLoggingInterceptor)
            .addInterceptor(get() as ClientIdentificationRequestInterceptor)
            .addInterceptor(ApiRequestInterceptor(get()))
            .authenticator(AuthTokenAuthenticator(get(), get(), get()))
            .connectTimeout(CONNECT_TIMEOUT, TimeUnit.SECONDS)
            .readTimeout(READ_TIMEOUT, TimeUnit.SECONDS)
            .writeTimeout(WRITE_TIMEOUT, TimeUnit.SECONDS)
            .cache(get() as Cache)
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

private val bluetooth = module {
    single<ESPProvisionManager> {
        ESPProvisionManager.getInstance(androidContext())
    }
    single<ESPDevice> {
        val espProvisionerManager = get() as ESPProvisionManager
        espProvisionerManager.createESPDevice(
            ESPConstants.TransportType.TRANSPORT_BLE,
            ESPConstants.SecurityType.SECURITY_0
        )
    }
    single<BluetoothScanner> {
        BluetoothScanner(get())
    }
    single<BluetoothConnectionManager> {
        BluetoothConnectionManager(get())
    }
    single<BluetoothUpdater> {
        BluetoothUpdater(get(), get())
    }
    single<BluetoothProvisioner> {
        BluetoothProvisioner(get())
    }
}

val validator = module {
    single {
        Validator()
    }
}

val firebase = module {
    single<FirebaseMessaging> {
        FirebaseMessaging.getInstance()
    }
    single<FirebaseRemoteConfig> {
        // Init Firebase config
        Firebase.remoteConfig.apply {
            setConfigSettingsAsync(
                remoteConfigSettings {
                    minimumFetchIntervalInSeconds = if (BuildConfig.DEBUG) {
                        FIREBASE_CONFIG_FETCH_INTERVAL_DEBUG
                    } else {
                        FIREBASE_CONFIG_FETCH_INTERVAL_RELEASE
                    }
                }
            )
            fetchAndActivate()
        }
    }
    single<FirebaseCrashlytics> {
        FirebaseCrashlytics.getInstance()
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

val database = module {
    single<AppDatabase> {
        Room.databaseBuilder(androidContext(), AppDatabase::class.java, APP_DATABASE_NAME)
            .addTypeConverter(DatabaseConverters())
            .build()
    }
    single<DeviceHistoryDao> {
        val database = get<AppDatabase>()
        database.deviceHistoryDao()
    }
}

val displayModeHelper = module {
    single {
        DisplayModeHelper(androidContext().resources, get())
    }
}

private val utilities = module {
    single<Moshi> {
        Moshi.Builder()
            .add(ZonedDateTime::class.java, ZonedDateTimeJsonAdapter())
            .add(LocalDateTime::class.java, LocalDateTimeJsonAdapter())
            .add(LocalDate::class.java, LocalDateJsonAdapter())
            .build()
    }

    single<Gson> {
        GsonBuilder()
            .setPrettyPrinting()
            .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
            .create()
    }

    single<UIHexJsonAdapter> {
        UIHexJsonAdapter(get())
    }

    single<AuthTokenJsonAdapter> {
        AuthTokenJsonAdapter(get())
    }

    single<Mask> {
        Mask()
    }
    single<DateTimeFormatter>(named(HOUR_FORMAT_24H)) {
        DateTimeFormatter.ofPattern(HOUR_FORMAT_24H)
    }
    single<DateTimeFormatter>(named(HOUR_FORMAT_12H_FULL)) {
        DateTimeFormatter.ofPattern(HOUR_FORMAT_12H_FULL)
    }
    single<DateTimeFormatter>(named(HOUR_FORMAT_12H_HOUR_ONLY)) {
        DateTimeFormatter.ofPattern(HOUR_FORMAT_12H_HOUR_ONLY)
    }
    single<DateTimeFormatter>(named(DATE_FORMAT_MONTH_DAY)) {
        val usersLocaleDateFormat =
            DateFormat.getBestDateTimePattern(Locale.getDefault(), DATE_FORMAT_MONTH_DAY)
        DateTimeFormatter.ofPattern(usersLocaleDateFormat)
    }
}

private val viewmodels = module {
    viewModel { params ->
        HistoryChartsViewModel(device = params.get())
    }
}

val modules = listOf(
    preferences,
    network,
    bluetooth,
    datasources,
    repositories,
    location,
    navigator,
    validator,
    usecases,
    resourcesHelper,
    firebase,
    apiServiceModule,
    database,
    displayModeHelper,
    utilities,
    viewmodels
)
