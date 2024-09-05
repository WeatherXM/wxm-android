package com.weatherxm.data

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.SharedPreferences
import android.icu.text.CompactDecimalFormat
import android.icu.text.NumberFormat
import android.location.Geocoder
import android.os.Build.VERSION.SDK_INT
import android.text.format.DateFormat
import androidx.core.content.ContextCompat
import androidx.preference.PreferenceManager
import androidx.room.Room
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences.PrefKeyEncryptionScheme
import androidx.security.crypto.EncryptedSharedPreferences.PrefValueEncryptionScheme
import androidx.security.crypto.MasterKeys
import coil.ImageLoader
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.chuckerteam.chucker.api.ChuckerInterceptor
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.SettingsClient
import com.google.firebase.Firebase
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.analytics
import com.google.firebase.installations.FirebaseInstallations
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.remoteconfig.ConfigUpdate
import com.google.firebase.remoteconfig.ConfigUpdateListener
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigException
import com.google.firebase.remoteconfig.remoteConfig
import com.google.firebase.remoteconfig.remoteConfigSettings
import com.google.gson.FieldNamingPolicy
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScanner
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import com.haroldadmin.cnradapter.NetworkResponseAdapterFactory
import com.mapbox.search.SearchEngine
import com.mapbox.search.SearchEngineSettings
import com.mixpanel.android.mpmetrics.MixpanelAPI
import com.squareup.moshi.Moshi
import com.weatherxm.BuildConfig
import com.weatherxm.analytics.AnalyticsService
import com.weatherxm.analytics.AnalyticsWrapper
import com.weatherxm.analytics.FirebaseAnalyticsService
import com.weatherxm.analytics.MixpanelAnalyticsService
import com.weatherxm.data.adapters.LocalDateJsonAdapter
import com.weatherxm.data.adapters.LocalDateTimeJsonAdapter
import com.weatherxm.data.adapters.ZonedDateTimeJsonAdapter
import com.weatherxm.data.bluetooth.BluetoothConnectionManager
import com.weatherxm.data.bluetooth.BluetoothScanner
import com.weatherxm.data.bluetooth.BluetoothUpdater
import com.weatherxm.data.database.AppDatabase
import com.weatherxm.data.database.DatabaseConverters
import com.weatherxm.data.database.dao.DeviceHistoryDao
import com.weatherxm.data.database.dao.NetworkSearchRecentDao
import com.weatherxm.data.datasource.AppConfigDataSource
import com.weatherxm.data.datasource.AppConfigDataSourceImpl
import com.weatherxm.data.datasource.CacheAddressDataSource
import com.weatherxm.data.datasource.CacheAddressSearchDataSource
import com.weatherxm.data.datasource.CacheAuthDataSource
import com.weatherxm.data.datasource.CacheDeviceDataSource
import com.weatherxm.data.datasource.CacheFollowDataSource
import com.weatherxm.data.datasource.CacheUserDataSource
import com.weatherxm.data.datasource.CacheWalletDataSource
import com.weatherxm.data.datasource.CacheWeatherForecastDataSource
import com.weatherxm.data.datasource.DatabaseExplorerDataSource
import com.weatherxm.data.datasource.DatabaseWeatherHistoryDataSource
import com.weatherxm.data.datasource.DeviceOTADataSource
import com.weatherxm.data.datasource.DeviceOTADataSourceImpl
import com.weatherxm.data.datasource.LocationDataSource
import com.weatherxm.data.datasource.LocationDataSourceImpl
import com.weatherxm.data.datasource.NetworkAddressDataSource
import com.weatherxm.data.datasource.NetworkAddressSearchDataSource
import com.weatherxm.data.datasource.NetworkAuthDataSource
import com.weatherxm.data.datasource.NetworkDeviceDataSource
import com.weatherxm.data.datasource.NetworkExplorerDataSource
import com.weatherxm.data.datasource.NetworkFollowDataSource
import com.weatherxm.data.datasource.NetworkUserDataSource
import com.weatherxm.data.datasource.NetworkWalletDataSource
import com.weatherxm.data.datasource.NetworkWeatherForecastDataSource
import com.weatherxm.data.datasource.NetworkWeatherHistoryDataSource
import com.weatherxm.data.datasource.NotificationsDataSource
import com.weatherxm.data.datasource.NotificationsDataSourceImpl
import com.weatherxm.data.datasource.RemoteBannersDataSource
import com.weatherxm.data.datasource.RemoteBannersDataSourceImpl
import com.weatherxm.data.datasource.RewardsDataSource
import com.weatherxm.data.datasource.RewardsDataSourceImpl
import com.weatherxm.data.datasource.StatsDataSource
import com.weatherxm.data.datasource.StatsDataSourceImpl
import com.weatherxm.data.datasource.UserPreferenceDataSource
import com.weatherxm.data.datasource.UserPreferenceDataSourceImpl
import com.weatherxm.data.datasource.WidgetDataSource
import com.weatherxm.data.datasource.WidgetDataSourceImpl
import com.weatherxm.data.datasource.bluetooth.BluetoothConnectionDataSource
import com.weatherxm.data.datasource.bluetooth.BluetoothConnectionDataSourceImpl
import com.weatherxm.data.datasource.bluetooth.BluetoothScannerDataSource
import com.weatherxm.data.datasource.bluetooth.BluetoothScannerDataSourceImpl
import com.weatherxm.data.datasource.bluetooth.BluetoothUpdaterDataSource
import com.weatherxm.data.datasource.bluetooth.BluetoothUpdaterDataSourceImpl
import com.weatherxm.data.network.AuthTokenJsonAdapter
import com.weatherxm.data.network.interceptor.ApiRequestInterceptor
import com.weatherxm.data.network.interceptor.AuthRequestInterceptor
import com.weatherxm.data.network.interceptor.AuthTokenAuthenticator
import com.weatherxm.data.network.interceptor.ClientIdentificationRequestInterceptor
import com.weatherxm.data.repository.AddressRepository
import com.weatherxm.data.repository.AddressRepositoryImpl
import com.weatherxm.data.repository.AppConfigRepository
import com.weatherxm.data.repository.AppConfigRepositoryImpl
import com.weatherxm.data.repository.AuthRepository
import com.weatherxm.data.repository.AuthRepositoryImpl
import com.weatherxm.data.repository.DeviceOTARepository
import com.weatherxm.data.repository.DeviceOTARepositoryImpl
import com.weatherxm.data.repository.DeviceRepository
import com.weatherxm.data.repository.DeviceRepositoryImpl
import com.weatherxm.data.repository.ExplorerRepository
import com.weatherxm.data.repository.ExplorerRepositoryImpl
import com.weatherxm.data.repository.FollowRepository
import com.weatherxm.data.repository.FollowRepositoryImpl
import com.weatherxm.data.repository.LocationRepository
import com.weatherxm.data.repository.LocationRepositoryImpl
import com.weatherxm.data.repository.NotificationsRepository
import com.weatherxm.data.repository.NotificationsRepositoryImpl
import com.weatherxm.data.repository.RemoteBannersRepository
import com.weatherxm.data.repository.RemoteBannersRepositoryImpl
import com.weatherxm.data.repository.RewardsRepository
import com.weatherxm.data.repository.RewardsRepositoryImpl
import com.weatherxm.data.repository.StatsRepository
import com.weatherxm.data.repository.StatsRepositoryImpl
import com.weatherxm.data.repository.UserPreferencesRepository
import com.weatherxm.data.repository.UserPreferencesRepositoryImpl
import com.weatherxm.data.repository.UserRepository
import com.weatherxm.data.repository.UserRepositoryImpl
import com.weatherxm.data.repository.WalletRepository
import com.weatherxm.data.repository.WalletRepositoryImpl
import com.weatherxm.data.repository.WeatherForecastRepository
import com.weatherxm.data.repository.WeatherForecastRepositoryImpl
import com.weatherxm.data.repository.WeatherHistoryRepository
import com.weatherxm.data.repository.WeatherHistoryRepositoryImpl
import com.weatherxm.data.repository.WidgetRepository
import com.weatherxm.data.repository.WidgetRepositoryImpl
import com.weatherxm.data.repository.bluetooth.BluetoothConnectionRepository
import com.weatherxm.data.repository.bluetooth.BluetoothConnectionRepositoryImpl
import com.weatherxm.data.repository.bluetooth.BluetoothScannerRepository
import com.weatherxm.data.repository.bluetooth.BluetoothScannerRepositoryImpl
import com.weatherxm.data.repository.bluetooth.BluetoothUpdaterRepository
import com.weatherxm.data.repository.bluetooth.BluetoothUpdaterRepositoryImpl
import com.weatherxm.data.services.CacheService
import com.weatherxm.ui.Navigator
import com.weatherxm.ui.analytics.AnalyticsOptInViewModel
import com.weatherxm.ui.cellinfo.CellInfoViewModel
import com.weatherxm.ui.claimdevice.helium.ClaimHeliumViewModel
import com.weatherxm.ui.claimdevice.helium.frequency.ClaimHeliumFrequencyViewModel
import com.weatherxm.ui.claimdevice.helium.pair.ClaimHeliumPairViewModel
import com.weatherxm.ui.claimdevice.helium.result.ClaimHeliumResultViewModel
import com.weatherxm.ui.claimdevice.location.ClaimLocationViewModel
import com.weatherxm.ui.claimdevice.pulse.ClaimPulseViewModel
import com.weatherxm.ui.claimdevice.wifi.ClaimWifiViewModel
import com.weatherxm.ui.connectwallet.ConnectWalletViewModel
import com.weatherxm.ui.deleteaccount.DeleteAccountViewModel
import com.weatherxm.ui.deleteaccountsurvey.DeleteAccountSurveyViewModel
import com.weatherxm.ui.devicedetails.DeviceDetailsViewModel
import com.weatherxm.ui.devicedetails.current.CurrentViewModel
import com.weatherxm.ui.devicedetails.forecast.ForecastViewModel
import com.weatherxm.ui.devicedetails.rewards.RewardsViewModel
import com.weatherxm.ui.deviceeditlocation.DeviceEditLocationViewModel
import com.weatherxm.ui.deviceforecast.ForecastDetailsViewModel
import com.weatherxm.ui.deviceheliumota.DeviceHeliumOTAViewModel
import com.weatherxm.ui.devicehistory.HistoryChartsViewModel
import com.weatherxm.ui.devicesettings.helium.DeviceSettingsHeliumViewModel
import com.weatherxm.ui.devicesettings.helium.changefrequency.ChangeFrequencyViewModel
import com.weatherxm.ui.devicesettings.helium.reboot.RebootViewModel
import com.weatherxm.ui.devicesettings.wifi.DeviceSettingsWifiViewModel
import com.weatherxm.ui.devicesrewards.DevicesRewardsViewModel
import com.weatherxm.ui.explorer.ExplorerViewModel
import com.weatherxm.ui.explorer.UICellJsonAdapter
import com.weatherxm.ui.explorer.search.NetworkSearchViewModel
import com.weatherxm.ui.home.HomeViewModel
import com.weatherxm.ui.home.devices.DevicesViewModel
import com.weatherxm.ui.home.profile.ProfileViewModel
import com.weatherxm.ui.login.LoginViewModel
import com.weatherxm.ui.networkstats.NetworkStatsViewModel
import com.weatherxm.ui.passwordprompt.PasswordPromptViewModel
import com.weatherxm.ui.preferences.PreferenceViewModel
import com.weatherxm.ui.resetpassword.ResetPasswordViewModel
import com.weatherxm.ui.rewardboosts.RewardBoostViewModel
import com.weatherxm.ui.rewarddetails.RewardDetailsViewModel
import com.weatherxm.ui.rewardsclaim.RewardsClaimViewModel
import com.weatherxm.ui.rewardslist.RewardsListViewModel
import com.weatherxm.ui.signup.SignupViewModel
import com.weatherxm.ui.startup.StartupViewModel
import com.weatherxm.ui.updateprompt.UpdatePromptViewModel
import com.weatherxm.ui.deeplinkrouteractivity.DeepLinkRouterViewModel
import com.weatherxm.ui.widgets.selectstation.SelectStationViewModel
import com.weatherxm.usecases.AnalyticsOptInUseCase
import com.weatherxm.usecases.AnalyticsOptInUseCaseImpl
import com.weatherxm.usecases.AuthUseCase
import com.weatherxm.usecases.AuthUseCaseImpl
import com.weatherxm.usecases.BluetoothConnectionUseCase
import com.weatherxm.usecases.BluetoothConnectionUseCaseImpl
import com.weatherxm.usecases.BluetoothScannerUseCase
import com.weatherxm.usecases.BluetoothScannerUseCaseImpl
import com.weatherxm.usecases.BluetoothUpdaterUseCase
import com.weatherxm.usecases.BluetoothUpdaterUseCaseImpl
import com.weatherxm.usecases.ChartsUseCase
import com.weatherxm.usecases.ChartsUseCaseImpl
import com.weatherxm.usecases.ClaimDeviceUseCase
import com.weatherxm.usecases.ClaimDeviceUseCaseImpl
import com.weatherxm.usecases.ConnectWalletUseCase
import com.weatherxm.usecases.ConnectWalletUseCaseImpl
import com.weatherxm.usecases.DeleteAccountSurveyUseCase
import com.weatherxm.usecases.DeleteAccountSurveyUseCaseImpl
import com.weatherxm.usecases.DeleteAccountUseCase
import com.weatherxm.usecases.DeleteAccountUseCaseImpl
import com.weatherxm.usecases.DeviceDetailsUseCase
import com.weatherxm.usecases.DeviceDetailsUseCaseImpl
import com.weatherxm.usecases.DeviceListUseCase
import com.weatherxm.usecases.DeviceListUseCaseImpl
import com.weatherxm.usecases.EditLocationUseCase
import com.weatherxm.usecases.EditLocationUseCaseImpl
import com.weatherxm.usecases.ExplorerUseCase
import com.weatherxm.usecases.ExplorerUseCaseImpl
import com.weatherxm.usecases.FollowUseCase
import com.weatherxm.usecases.FollowUseCaseImpl
import com.weatherxm.usecases.ForecastUseCase
import com.weatherxm.usecases.ForecastUseCaseImpl
import com.weatherxm.usecases.HistoryUseCase
import com.weatherxm.usecases.HistoryUseCaseImpl
import com.weatherxm.usecases.PasswordPromptUseCase
import com.weatherxm.usecases.PasswordPromptUseCaseImpl
import com.weatherxm.usecases.PreferencesUseCase
import com.weatherxm.usecases.PreferencesUseCaseImpl
import com.weatherxm.usecases.RemoteBannersUseCase
import com.weatherxm.usecases.RemoteBannersUseCaseImpl
import com.weatherxm.usecases.RewardsUseCase
import com.weatherxm.usecases.RewardsUseCaseImpl
import com.weatherxm.usecases.StartupUseCase
import com.weatherxm.usecases.StartupUseCaseImpl
import com.weatherxm.usecases.StationSettingsUseCase
import com.weatherxm.usecases.StationSettingsUseCaseImpl
import com.weatherxm.usecases.StatsUseCase
import com.weatherxm.usecases.StatsUseCaseImpl
import com.weatherxm.usecases.UserUseCase
import com.weatherxm.usecases.UserUseCaseImpl
import com.weatherxm.usecases.WidgetCurrentWeatherUseCase
import com.weatherxm.usecases.WidgetCurrentWeatherUseCaseImpl
import com.weatherxm.usecases.WidgetSelectStationUseCase
import com.weatherxm.usecases.WidgetSelectStationUseCaseImpl
import com.weatherxm.util.DisplayModeHelper
import com.weatherxm.util.LocationHelper
import com.weatherxm.util.Resources
import com.weatherxm.util.WidgetHelper
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
import timber.log.Timber
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.concurrent.TimeUnit

const val RETROFIT_API = "RETROFIT_API"
const val RETROFIT_AUTH = "RETROFIT_AUTH"
const val APP_DATABASE_NAME = "WEATHERXM"

const val HOUR_FORMAT_24H = "HH:mm"
const val HOUR_FORMAT_12H_FULL = "h:mm a"
const val HOUR_FORMAT_12H_HOUR_ONLY = "h a"
const val DATE_FORMAT_MONTH_DAY = "d/M"
const val DATE_FORMAT_MONTH_SHORT = "MMM d"
const val DATE_FORMAT_FULL = "EEE d, MMM yy"
private const val ENCRYPTED_PREFERENCES_KEY = "ENCRYPTED_PREFERENCES_KEY"
private const val PREFERENCES_AUTH_TOKEN = "PREFERENCES_AUTH_TOKEN"
private const val PREFERENCES_AUTH_TOKEN_FILE = "auth_token"
private const val NETWORK_CACHE_SIZE = 50L * 1024L * 1024L // 50MB

private const val CONNECT_TIMEOUT = 30L
private const val READ_TIMEOUT = 30L
private const val WRITE_TIMEOUT = 60L
private const val FIREBASE_CONFIG_FETCH_INTERVAL_DEBUG = 30L
private const val FIREBASE_CONFIG_FETCH_INTERVAL_RELEASE = 1800L

private const val COIL_MEMORY_CACHE_SIZE_PERCENTAGE = 0.25
private const val COIL_DISK_CACHE_SIZE_PERCENTAGE = 0.02

private val logging = module {
    single(createdAtStart = true) {
        Timber.also {
            // Setup debug logs on DEBUG builds (before all other statements,
            // since we're using Timber for logging everywhere)
            if (BuildConfig.DEBUG) {
                Timber.plant(Timber.DebugTree())
            }
            Timber.d("Initialized Timber logging")
        }
    }
}

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
}

private val datasources = module {
    single<LocationDataSource> {
        LocationDataSourceImpl(androidContext(), get(), get())
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
        CacheUserDataSource(get())
    }
    single<NetworkDeviceDataSource> {
        NetworkDeviceDataSource(get())
    }
    single<CacheDeviceDataSource> {
        CacheDeviceDataSource(get())
    }
    single<NetworkWalletDataSource> {
        NetworkWalletDataSource(get())
    }
    single<CacheWalletDataSource> {
        CacheWalletDataSource(get())
    }
    single<RewardsDataSource> {
        RewardsDataSourceImpl(get())
    }
    single<NetworkAuthDataSource> {
        NetworkAuthDataSource(get())
    }
    single<CacheAuthDataSource> {
        CacheAuthDataSource(get())
    }
    single<AppConfigDataSource> {
        AppConfigDataSourceImpl(get(), get(), get())
    }
    single<UserPreferenceDataSource> {
        UserPreferenceDataSourceImpl(get())
    }
    single<NetworkExplorerDataSource> {
        NetworkExplorerDataSource(get())
    }
    single<DatabaseExplorerDataSource> {
        DatabaseExplorerDataSource(get())
    }
    single<NetworkAddressDataSource> {
        NetworkAddressDataSource(androidContext(), get(), get())
    }
    single<CacheAddressDataSource> {
        CacheAddressDataSource(get())
    }
    single<NetworkWeatherForecastDataSource> {
        NetworkWeatherForecastDataSource(get())
    }
    single<CacheWeatherForecastDataSource> {
        CacheWeatherForecastDataSource(get())
    }
    single<NetworkAddressSearchDataSource> {
        NetworkAddressSearchDataSource(get())
    }
    single<CacheAddressSearchDataSource> {
        CacheAddressSearchDataSource(get())
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
    single<DeviceOTADataSource> {
        DeviceOTADataSourceImpl(get(), get())
    }
    single<WidgetDataSource> {
        WidgetDataSourceImpl(get())
    }
    single<StatsDataSource> {
        StatsDataSourceImpl(get())
    }
    single<NetworkFollowDataSource> {
        NetworkFollowDataSource(get())
    }
    single<CacheFollowDataSource> {
        CacheFollowDataSource(get())
    }
    single<NotificationsDataSource> {
        NotificationsDataSourceImpl(get(), get())
    }
    single<RemoteBannersDataSource> {
        RemoteBannersDataSourceImpl(get(), get())
    }
}

private val repositories = module {
    single<AuthRepository> {
        AuthRepositoryImpl(get(), get(), get(), get(), get(), get())
    }
    single<UserRepository> {
        UserRepositoryImpl(get(), get())
    }
    single<WalletRepository> {
        WalletRepositoryImpl(get(), get())
    }
    single<DeviceRepository> {
        DeviceRepositoryImpl(get(), get(), get(), get(), get())
    }
    single<ExplorerRepository> {
        ExplorerRepositoryImpl(get(), get())
    }
    single<RewardsRepository> {
        RewardsRepositoryImpl(get())
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
    single<AddressRepository> {
        AddressRepositoryImpl(get(), get(), get(), get(), get())
    }
    single<BluetoothScannerRepository> {
        BluetoothScannerRepositoryImpl(get())
    }
    single<UserPreferencesRepository> {
        UserPreferencesRepositoryImpl(get())
    }
    single<BluetoothConnectionRepository> {
        BluetoothConnectionRepositoryImpl(get())
    }
    single<BluetoothUpdaterRepository> {
        BluetoothUpdaterRepositoryImpl(get())
    }
    single<DeviceOTARepository> {
        DeviceOTARepositoryImpl(get())
    }
    single<WidgetRepository> {
        WidgetRepositoryImpl(get())
    }
    single<StatsRepository> {
        StatsRepositoryImpl(get())
    }
    single<FollowRepository> {
        FollowRepositoryImpl(get(), get())
    }
    single<LocationRepository> {
        LocationRepositoryImpl(get(), get(), get())
    }
    single<NotificationsRepository> {
        NotificationsRepositoryImpl(get(), get())
    }
    single<RemoteBannersRepository> {
        RemoteBannersRepositoryImpl(get())
    }
}

private val usecases = module {
    single<StartupUseCase> {
        StartupUseCaseImpl(androidContext(), get(), get(), get())
    }
    single<ExplorerUseCase> {
        ExplorerUseCaseImpl(get(), get(), get(), get(), get())
    }
    single<DeviceDetailsUseCase> {
        DeviceDetailsUseCaseImpl(get(), get(), get(), get(), get())
    }
    single<ForecastUseCase> {
        ForecastUseCaseImpl(get())
    }
    single<HistoryUseCase> {
        HistoryUseCaseImpl(get())
    }
    single<ChartsUseCase> {
        ChartsUseCaseImpl(androidContext())
    }
    single<ClaimDeviceUseCase> {
        ClaimDeviceUseCaseImpl(get(), get())
    }
    single<RewardsUseCase> {
        RewardsUseCaseImpl(get(), androidContext().resources)
    }
    single<AuthUseCase> {
        AuthUseCaseImpl(get(), get(), get(), get())
    }
    single<UserUseCase> {
        UserUseCaseImpl(get(), get(), get(), get())
    }
    single<ConnectWalletUseCase> {
        ConnectWalletUseCaseImpl(get())
    }
    single<PreferencesUseCase> {
        PreferencesUseCaseImpl(get(), get(), get())
    }
    single<DeleteAccountSurveyUseCase> {
        DeleteAccountSurveyUseCaseImpl(get())
    }
    single<DeleteAccountUseCase> {
        DeleteAccountUseCaseImpl(get(), get())
    }
    single<PasswordPromptUseCase> {
        PasswordPromptUseCaseImpl(get())
    }
    single<BluetoothScannerUseCase> {
        BluetoothScannerUseCaseImpl(get())
    }
    single<BluetoothConnectionUseCase> {
        BluetoothConnectionUseCaseImpl(get())
    }
    single<BluetoothUpdaterUseCase> {
        BluetoothUpdaterUseCaseImpl(androidContext(), get(), get())
    }
    single<AnalyticsOptInUseCase> {
        AnalyticsOptInUseCaseImpl(get())
    }
    single<StationSettingsUseCase> {
        StationSettingsUseCaseImpl(get(), get(), get())
    }
    single<WidgetSelectStationUseCase> {
        WidgetSelectStationUseCaseImpl(get(), get(), get())
    }
    single<WidgetCurrentWeatherUseCase> {
        WidgetCurrentWeatherUseCaseImpl(get(), get())
    }
    single<StatsUseCase> {
        StatsUseCaseImpl(get(), get())
    }
    single<FollowUseCase> {
        FollowUseCaseImpl(get(), get())
    }
    single<DeviceListUseCase> {
        DeviceListUseCaseImpl(get(), get(), get())
    }
    single<EditLocationUseCase> {
        EditLocationUseCaseImpl(get(), get())
    }
    single<RemoteBannersUseCase> {
        RemoteBannersUseCaseImpl(get())
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

    single<ChuckerInterceptor> {
        ChuckerInterceptor(androidContext())
    }

    single<ClientIdentificationRequestInterceptor> {
        ClientIdentificationRequestInterceptor(get())
    }

    single<MoshiConverterFactory> {
        MoshiConverterFactory.create(get()).asLenient()
    }

    single<AuthTokenAuthenticator> {
        AuthTokenAuthenticator(get(), get(), get(), get())
    }

    single<ApiRequestInterceptor> {
        ApiRequestInterceptor(get())
    }

    single<AuthRequestInterceptor> {
        AuthRequestInterceptor(get(), get())
    }

    single<Retrofit>(named(RETROFIT_AUTH)) {
        // Create client
        val client: OkHttpClient = OkHttpClient.Builder()
            .addInterceptor(get() as ChuckerInterceptor)
            .addInterceptor(get() as HttpLoggingInterceptor)
            .addInterceptor(get() as ClientIdentificationRequestInterceptor)
            .addInterceptor(get() as AuthRequestInterceptor)
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
            .addInterceptor(get() as ChuckerInterceptor)
            .addInterceptor(get() as HttpLoggingInterceptor)
            .addInterceptor(get() as ClientIdentificationRequestInterceptor)
            .addInterceptor(get() as ApiRequestInterceptor)
            .authenticator(get() as AuthTokenAuthenticator)
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
    single<BluetoothAdapter?> {
        ContextCompat.getSystemService(androidContext(), BluetoothManager::class.java)?.adapter
    }
    single<BluetoothScanner> {
        BluetoothScanner()
    }
    single<BluetoothConnectionManager> {
        BluetoothConnectionManager(get(), get())
    }
    single<BluetoothUpdater> {
        BluetoothUpdater(get(), get())
    }
}

val firebase = module {
    single<FirebaseMessaging>(createdAtStart = true) {
        FirebaseMessaging.getInstance().also {
            if (BuildConfig.DEBUG) {
                // Log Firebase Cloud Messaging token for testing
                it.token
                    .addOnSuccessListener { token ->
                        Timber.d("FCM token: $token")
                    }
                    .addOnFailureListener { e ->
                        Timber.w(e, "Could not get FCM token.")
                    }
            }
        }
    }

    single<FirebaseRemoteConfig> {
        // Init Firebase config
        Firebase.remoteConfig.apply {
            setConfigSettingsAsync(remoteConfigSettings {
                minimumFetchIntervalInSeconds = if (BuildConfig.DEBUG) {
                    FIREBASE_CONFIG_FETCH_INTERVAL_DEBUG
                } else {
                    FIREBASE_CONFIG_FETCH_INTERVAL_RELEASE
                }
            })

            addOnConfigUpdateListener(object : ConfigUpdateListener {
                override fun onUpdate(configUpdate: ConfigUpdate) {
                    activate()
                }

                override fun onError(error: FirebaseRemoteConfigException) {
                    Timber.e("Config update error with code: " + error.code, error)
                }
            })

            fetchAndActivate()
        }
    }

    single<FirebaseInstallations> {
        FirebaseInstallations.getInstance()
    }

    single<GmsBarcodeScanner> {
        val options = GmsBarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS)
            .build()
        GmsBarcodeScanning.getClient(get(), options)
    }
}

val navigator = module {
    single {
        Navigator(get())
    }
}

val resources = module {
    single {
        Resources(androidContext().resources)
    }
}

val database = module {
    single<AppDatabase> {
        Room.databaseBuilder(androidContext(), AppDatabase::class.java, APP_DATABASE_NAME)
            .fallbackToDestructiveMigration()
            .addTypeConverter(DatabaseConverters())
            .build()
    }
    single<NetworkSearchRecentDao> {
        val database = get<AppDatabase>()
        database.networkSearchRecentDao()
    }
    single<DeviceHistoryDao> {
        val database = get<AppDatabase>()
        database.deviceHistoryDao()
    }
}

val displayModeHelper = module {
    single(createdAtStart = true) {
        DisplayModeHelper(androidContext().resources, get(), get()).apply {
            // Set light/dark theme at startup
            setDisplayMode()
        }
    }
}

val locationHelper = module {
    single(createdAtStart = true) {
        LocationHelper(androidContext(), get())
    }
}

val clientIdentificationHelper = module {
    single {
        ClientIdentificationHelper(androidContext(), get())
    }
}

val analytics = module {
    single<FirebaseAnalytics> {
        Firebase.analytics
    }

    single<MixpanelAPI> {
        MixpanelAPI.getInstance(androidContext(), BuildConfig.MIXPANEL_TOKEN, false).apply {
            setEnableLogging(true)
        }
    }

    factory { FirebaseAnalyticsService(get()) as AnalyticsService }
    factory { MixpanelAnalyticsService(get()) as AnalyticsService }

    single<AnalyticsWrapper> {
        AnalyticsWrapper(getAll<AnalyticsService>(), androidContext())
    }
}

val widgetHelper = module {
    single {
        WidgetHelper(get(), get())
    }
}

private val utilities = module {
    single<CacheService> {
        CacheService(get(), get<SharedPreferences>(named(PREFERENCES_AUTH_TOKEN)), get(), get())
    }
    @Suppress("MagicNumber")
    single<ImageLoader>(createdAtStart = true) {
        ImageLoader.Builder(androidContext())
            .memoryCache {
                MemoryCache.Builder(androidContext())
                    .maxSizePercent(COIL_MEMORY_CACHE_SIZE_PERCENTAGE)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(androidContext().cacheDir.resolve("image_cache"))
                    .maxSizePercent(COIL_DISK_CACHE_SIZE_PERCENTAGE)
                    .build()
            }
            .components {
                if (SDK_INT >= 28) {
                    add(ImageDecoderDecoder.Factory())
                } else {
                    add(GifDecoder.Factory())
                }
            }
            .respectCacheHeaders(false)
            .build()
    }
    single<Geocoder> {
        Geocoder(androidContext(), Locale.getDefault())
    }
    single<CompactDecimalFormat> {
        CompactDecimalFormat.getInstance(
            Locale.US,
            CompactDecimalFormat.CompactStyle.SHORT
        ).apply {
            minimumFractionDigits = 0
            maximumFractionDigits = 1
        }
    }
    single<NumberFormat> {
        NumberFormat.getInstance()
    }
    single<SearchEngine> {
        SearchEngine.createSearchEngine(SearchEngineSettings())
    }
    single<Moshi> {
        Moshi.Builder()
            .add(ZonedDateTime::class.java, ZonedDateTimeJsonAdapter())
            .add(LocalDateTime::class.java, LocalDateTimeJsonAdapter())
            .add(LocalDate::class.java, LocalDateJsonAdapter())
            .build()
    }
    single<Gson> {
        GsonBuilder().setPrettyPrinting()
            .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
            .create()
    }
    single<UICellJsonAdapter> {
        UICellJsonAdapter(get())
    }
    single<AuthTokenJsonAdapter> {
        AuthTokenJsonAdapter(get())
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
    single<DateTimeFormatter>(named(DATE_FORMAT_FULL)) {
        DateTimeFormatter.ofPattern(DATE_FORMAT_FULL, Locale.US)
    }
    single<DateTimeFormatter>(named(DATE_FORMAT_MONTH_SHORT)) {
        DateTimeFormatter.ofPattern(DATE_FORMAT_MONTH_SHORT, Locale.US)
    }
}

private val viewmodels = module {
    viewModel { params ->
        DeviceDetailsViewModel(
            device = params.get(),
            openExplorerOnBack = params.get(),
            get(),
            get(),
            get(),
            get(),
            get()
        )
    }
    viewModel { params ->
        DeviceSettingsWifiViewModel(
            device = params.get(),
            get(),
            get(),
            get(),
            get(),
            get()
        )
    }
    viewModel { params ->
        DeviceSettingsHeliumViewModel(
            device = params.get(),
            get(),
            get(),
            get(),
            get(),
            get()
        )
    }
    viewModel { params -> RebootViewModel(device = params.get(), get(), get(), get(), get()) }
    viewModel { params ->
        ChangeFrequencyViewModel(device = params.get(), get(), get(), get(), get(), get())
    }
    viewModel { params -> CurrentViewModel(device = params.get(), get(), get(), get()) }
    viewModel { params -> ForecastViewModel(device = params.get(), get(), get(), get()) }
    viewModel { params -> RewardsViewModel(device = params.get(), get(), get(), get()) }
    viewModel { params ->
        HistoryChartsViewModel(
            device = params.get(),
            get(),
            get(),
            get(),
            get()
        )
    }
    viewModel { params ->
        DeviceHeliumOTAViewModel(
            device = params.get(),
            deviceIsBleConnected = params.get(),
            get(),
            get(),
            get(),
            get(),
            get()
        )
    }
    viewModel { params ->
        CellInfoViewModel(cell = params.get(), get(), get(), get(), get(), get())
    }
    viewModel { StartupViewModel(get()) }
    viewModel { AnalyticsOptInViewModel(get(), get()) }
    viewModel { ConnectWalletViewModel(get(), get(), get()) }
    viewModel { DeleteAccountViewModel(get(), get(), get()) }
    viewModel { DeviceEditLocationViewModel(get(), get(), get(), get()) }
    viewModel { DevicesViewModel(get(), get(), get(), get()) }
    viewModel { ExplorerViewModel(get(), get(), get()) }
    viewModel { HomeViewModel(get(), get(), get()) }
    viewModel { ProfileViewModel(get(), get()) }
    viewModel { LoginViewModel(get(), get(), get()) }
    viewModel { NetworkStatsViewModel(get()) }
    viewModel { PasswordPromptViewModel(get(), get(), get()) }
    viewModel { PreferenceViewModel(get(), get()) }
    viewModel { ResetPasswordViewModel(get(), get(), get()) }
    viewModel { RewardsClaimViewModel(get(), get()) }
    viewModel { RewardsListViewModel(get(), get()) }
    viewModel { params ->
        RewardDetailsViewModel(
            device = params.get(),
            get(),
            get(),
            get(),
            get(),
            get()
        )
    }
    viewModel { params -> RewardBoostViewModel(params.get(), get(), get(), get()) }
    viewModel { DeleteAccountSurveyViewModel(get(), get()) }
    viewModel { SignupViewModel(get(), get(), get()) }
    viewModel { UpdatePromptViewModel(get()) }
    viewModel { DeepLinkRouterViewModel(get(), get(), get()) }
    viewModel { SelectStationViewModel(get()) }
    viewModel { ClaimLocationViewModel(get(), get(), get()) }
    viewModel { ClaimHeliumViewModel(get(), get(), get()) }
    viewModel { ClaimHeliumPairViewModel(get(), get(), get(), get()) }
    viewModel { ClaimHeliumResultViewModel(get(), get(), get()) }
    viewModel { ClaimHeliumFrequencyViewModel(get(), get()) }
    viewModel { NetworkSearchViewModel(get(), get()) }
    viewModel { params ->
        ForecastDetailsViewModel(
            params.get(),
            get(),
            get(),
            get(),
            get()
        )
    }
    viewModel { params ->
        ClaimWifiViewModel(deviceType = params.get(), get(), get(), get())
    }
    viewModel { ClaimPulseViewModel(get(), get(), get()) }
    viewModel { DevicesRewardsViewModel(get(), get(), get()) }
}

val modules = listOf(
    logging,
    analytics,
    preferences,
    network,
    bluetooth,
    datasources,
    repositories,
    location,
    navigator,
    usecases,
    resources,
    firebase,
    apiServiceModule,
    database,
    displayModeHelper,
    locationHelper,
    clientIdentificationHelper,
    utilities,
    widgetHelper,
    viewmodels
)
