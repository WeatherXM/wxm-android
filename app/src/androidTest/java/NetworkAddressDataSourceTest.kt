
import androidx.test.platform.app.InstrumentationRegistry
import com.weatherxm.data.Hex
import com.weatherxm.data.Location
import com.weatherxm.data.datasource.NetworkAddressDataSource
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import java.util.*

class NetworkAddressDataSourceTest {
    private lateinit var networkAddressDataSource: NetworkAddressDataSource

    @Before
    fun setup() {
        val instrumentationContext = InstrumentationRegistry.getInstrumentation().targetContext
        networkAddressDataSource = NetworkAddressDataSource(instrumentationContext)
    }

    @Test
    fun english_locale_test_1(): Unit = runBlocking {
        // Expected address = "Chania, GR"
        val hex = Hex("", arrayOf(), Location(35.51742583, 24.02897029))

        networkAddressDataSource.getLocationAddress(hex, Locale.ENGLISH).map {
            assert(it == "Chania, GR")
        }
    }

    @Test
    fun english_locale_test_2(): Unit = runBlocking {
        // Expected address = "Ottawa, CA"
        val hex = Hex("", arrayOf(), Location(45.34419422, -75.69030833))

        networkAddressDataSource.getLocationAddress(hex, Locale.ENGLISH).map {
            assert(it == "Ottawa, CA")
        }
    }

    @Test
    fun english_locale_test_3(): Unit = runBlocking {
        // Expected address = "Athina, GR"
        val hex = Hex("", arrayOf(), Location(37.98101496, 23.71952882))

        networkAddressDataSource.getLocationAddress(hex, Locale.ENGLISH).map {
            assert(it == "Athina, GR")
        }
    }

    @Test
    fun english_locale_test_4(): Unit = runBlocking {
        // Expected address = "Cristian, RO"
        val hex = Hex("", arrayOf(), Location(45.78895315, 24.02408933))

        networkAddressDataSource.getLocationAddress(hex, Locale.ENGLISH).map {
            assert(it == "Cristian, RO")
        }
    }

    @Test
    fun english_locale_test_5(): Unit = runBlocking {
        // Expected address = "Plano, US"
        val hex = Hex("", arrayOf(), Location(33.02277422, -96.67423257))

        networkAddressDataSource.getLocationAddress(hex, Locale.ENGLISH).map {
            assert(it == "Plano, US")
        }
    }

    @Test
    fun english_locale_test_6(): Unit = runBlocking {
        // Expected address = "Grafschaft, DE"
        val hex = Hex("", arrayOf(), Location(50.5545733, 7.111956497))

        networkAddressDataSource.getLocationAddress(hex, Locale.ENGLISH).map {
            assert(it == "Grafschaft, DE")
        }
    }

    @Test
    fun english_locale_test_7(): Unit = runBlocking {
        // Expected address = "Kilsby, GB"
        val hex = Hex("", arrayOf(), Location(52.34868912, -1.1893716))

        networkAddressDataSource.getLocationAddress(hex, Locale.ENGLISH).map {
            assert(it == "Kilsby, GB")
        }
    }

    @Test
    fun english_locale_test_8(): Unit = runBlocking {
        // Expected address = "San Jose, US"
        val hex = Hex("", arrayOf(), Location(37.28546527, -121.8749344))

        networkAddressDataSource.getLocationAddress(hex, Locale.ENGLISH).map {
            assert(it == "San Jose, US")
        }
    }

    @Test
    fun english_locale_test_9(): Unit = runBlocking {
        // Expected address = "Boca Raton, US"
        val hex = Hex("", arrayOf(), Location(26.32961528, -80.17513142))

        networkAddressDataSource.getLocationAddress(hex, Locale.ENGLISH).map {
            assert(it == "Boca Raton, US")
        }
    }

    @Test
    fun english_locale_test_10(): Unit = runBlocking {
        // Expected address = "Surrey, CA"
        val hex = Hex("", arrayOf(), Location(49.02680944, -122.7706035))

        networkAddressDataSource.getLocationAddress(hex, Locale.ENGLISH).map {
            assert(it == "Surrey, CA")
        }
    }

    @Test
    fun english_locale_test_11(): Unit = runBlocking {
        // Expected address = "Plympton, AU"
        val hex = Hex("", arrayOf(), Location(-34.95640734, 138.5613848))

        networkAddressDataSource.getLocationAddress(hex, Locale.ENGLISH).map {
            assert(it == "Plympton, AU")
        }
    }

    @Test
    fun english_locale_test_12(): Unit = runBlocking {
        // Expected address = "Stratford, CA"
        val hex = Hex("", arrayOf(), Location(46.22908473, -63.0902769))

        networkAddressDataSource.getLocationAddress(hex, Locale.ENGLISH).map {
            assert(it == "Stratford, CA")
        }
    }

    @Test
    fun english_locale_test_13(): Unit = runBlocking {
        // Expected address = "Minganie Regional County Municipality, CA"
        val hex = Hex("", arrayOf(), Location(50.22410606, -60.07464779))

        networkAddressDataSource.getLocationAddress(hex, Locale.ENGLISH).map {
            assert(it == "Minganie Regional County Municipality, CA")
        }
    }

    @Test
    fun english_locale_test_14(): Unit = runBlocking {
        // Expected address = "Lejweleputswa, ZA"
        val hex = Hex("", arrayOf(), Location(-28.960434, 26.385414))

        networkAddressDataSource.getLocationAddress(hex, Locale.ENGLISH).map {
            assert(it == "Lejweleputswa, ZA")
        }
    }

    @Test
    fun english_locale_test_15(): Unit = runBlocking {
        // Expected address = "Norilsk, RU"
        val hex = Hex("", arrayOf(), Location(69.351927, 88.192773))

        networkAddressDataSource.getLocationAddress(hex, Locale.ENGLISH).map {
            assert(it == "Norilsk, RU")
        }
    }

    @Test
    fun english_locale_test_16(): Unit = runBlocking {
        // Expected address = "Uonuma, JP"
        val hex = Hex("", arrayOf(), Location(37.288546, 138.999332))

        networkAddressDataSource.getLocationAddress(hex, Locale.ENGLISH).map {
            assert(it == "Uonuma, JP")
        }
    }

    @Test
    fun english_locale_test_17(): Unit = runBlocking {
        // Expected address = "Shalateen, EG"
        val hex = Hex("", arrayOf(), Location(23.956289, 35.486345))

        networkAddressDataSource.getLocationAddress(hex, Locale.ENGLISH).map {
            assert(it == "Shalateen, EG")
        }
    }

    @Test
    fun english_locale_test_18(): Unit = runBlocking {
        // Expected address = "Marsa Alam, EG"
        val hex = Hex("", arrayOf(), Location(25.051628, 34.900716))

        networkAddressDataSource.getLocationAddress(hex, Locale.ENGLISH).map {
            assert(it == "Marsa Alam, EG")
        }
    }

    @Test
    fun greek_locale_test_1(): Unit = runBlocking {
        // Expected address = "Χανιά, GR"
        val hex = Hex("", arrayOf(), Location(35.51742583, 24.02897029))

        networkAddressDataSource.getLocationAddress(hex, Locale("el_GR")).map {
            assert(it == "Χανιά, GR")
        }
    }

    @Test
    fun greek_locale_test_2(): Unit = runBlocking {
        // Expected address = "Αθήνα, GR"
        val hex = Hex("", arrayOf(), Location(37.98101496, 23.71952882))

        networkAddressDataSource.getLocationAddress(hex, Locale("el_GR")).map {
            assert(it == "Αθήνα, GR")
        }
    }
}
