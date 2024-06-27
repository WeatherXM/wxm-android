package ui.util

import com.weatherxm.ui.common.DeviceType
import com.weatherxm.util.Validator.validateClaimingKey
import com.weatherxm.util.Validator.validateEthAddress
import com.weatherxm.util.Validator.validateFriendlyName
import com.weatherxm.util.Validator.validateLocation
import com.weatherxm.util.Validator.validateNetworkSearchQuery
import com.weatherxm.util.Validator.validatePassword
import com.weatherxm.util.Validator.validateSerialNumber
import com.weatherxm.util.Validator.validateUsername
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import net.bytebuddy.utility.RandomString
import kotlin.random.Random
import kotlin.random.nextInt

class ValidatorTest : ShouldSpec() {
    init {
        should("Validate Username") {
            validateUsername("test@weatherxm.com") shouldBe true
            validateUsername("test@weatherxm") shouldBe false
            validateUsername("test@.com") shouldBe false
            validateUsername("test.weatherxm.com") shouldBe false
            validateUsername("test") shouldBe false
        }

        should("Validate Password") {
            for (length: Int in 1..10) {
                val randomPassword = RandomString.make(length)
                if (length < 6) {
                    validatePassword(randomPassword) shouldBe false
                } else {
                    validatePassword(randomPassword) shouldBe true
                }
            }
        }

        should("Validate Network Search Query") {
            for (length: Int in 1..10) {
                val randomQuery = RandomString.make(length)
                if (length < 2) {
                    validateNetworkSearchQuery(randomQuery) shouldBe false
                } else {
                    validateNetworkSearchQuery(randomQuery) shouldBe true
                }
            }
            validateNetworkSearchQuery("  ") shouldBe false
            validateNetworkSearchQuery("t ") shouldBe false
            validateNetworkSearchQuery("t e") shouldBe true
        }

        should("Validate Location") {
            validateLocation(0.0, 0.0) shouldBe false
            validateLocation(-180.0, 0.0) shouldBe false
            validateLocation(91.0, -181.0) shouldBe false
            validateLocation(-90.0, 180.0) shouldBe true
            validateLocation(90.0, -180.0) shouldBe true
        }

        should("Validate ETH Address") {
            validateEthAddress(null) shouldBe false
            validateEthAddress("000000000000000000000000000000000000000000") shouldBe false
            validateEthAddress("0a0000000000000000000000000000000000000000") shouldBe false
            validateEthAddress("0x000000000000000000000000000000000000000") shouldBe false
            validateEthAddress("0x0123456789ABCDEF000000000000000000000000") shouldBe true
        }

        should("Validate Serial Number") {
            validateSerialNumber("0123456789ABCDEF0", DeviceType.M5_WIFI) shouldBe false
            validateSerialNumber("0123456789ABCDEF0X", DeviceType.M5_WIFI) shouldBe false
            validateSerialNumber("0123456789ABCDEF000", DeviceType.M5_WIFI) shouldBe false
            validateSerialNumber("0123456789ABCDEF00", DeviceType.M5_WIFI) shouldBe true
            validateSerialNumber("0123456789ABCDEF000", DeviceType.D1_WIFI) shouldBe false
            validateSerialNumber("0123456789ABCDEF000X", DeviceType.D1_WIFI) shouldBe false
            validateSerialNumber("0123456789ABCDEF00000", DeviceType.D1_WIFI) shouldBe false
            validateSerialNumber("0123456789ABCDEF0000", DeviceType.D1_WIFI) shouldBe true
        }

        should("Validate Claiming Key") {
            validateClaimingKey("01234") shouldBe false
            validateClaimingKey("0123456") shouldBe false
            validateClaimingKey("01234A") shouldBe false
            validateClaimingKey("012345") shouldBe true
        }

        should("Validate Friendly Name") {
            validateFriendlyName(null) shouldBe false
            validateFriendlyName("") shouldBe false
            validateFriendlyName(
                "This is a really big friendly name with more than 64 chars       "
            ) shouldBe false

            val randomLength = Random.nextInt(1..64)
            val randomFriendlyName = RandomString.make(randomLength)
            println("Random Friendly Name: $randomFriendlyName")
            validateFriendlyName(randomFriendlyName) shouldBe true
        }

        afterTest {
            println("[${it.b.name.uppercase()}] - ${it.a.name.testName}")
        }
    }
}
