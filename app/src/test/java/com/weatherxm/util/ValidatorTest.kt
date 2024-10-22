package com.weatherxm.util

import com.weatherxm.TestUtils.createRandomString
import com.weatherxm.ui.common.DeviceType
import com.weatherxm.util.Validator.validateClaimingKey
import com.weatherxm.util.Validator.validateEthAddress
import com.weatherxm.util.Validator.validateFriendlyName
import com.weatherxm.util.Validator.validateLocation
import com.weatherxm.util.Validator.validateNetworkSearchQuery
import com.weatherxm.util.Validator.validatePassword
import com.weatherxm.util.Validator.validateSerialNumber
import com.weatherxm.util.Validator.validateUsername
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import kotlin.random.Random
import kotlin.random.nextInt

class ValidatorTest : BehaviorSpec({
    context("Validation of Username") {
        given("a username") {
            When("it is valid") {
                then("the validator should return true") {
                    validateUsername("test@weatherxm.com") shouldBe true
                }
            }
            When("it is invalid") {
                then("the validator should return false") {
                    validateUsername("test@weatherxm") shouldBe false
                    validateUsername("test@.com") shouldBe false
                    validateUsername("test.weatherxm.com") shouldBe false
                    validateUsername("test") shouldBe false
                }
            }
        }
    }
    context("Validation of Password") {
        given("a password") {
            When("it is valid") {
                then("the validator should return true") {
                    for (length: Int in 6..10) {
                        validatePassword(createRandomString(length)) shouldBe true
                    }
                }
            }
            When("it is invalid") {
                then("the validator should return false") {
                    for (length: Int in 1..5) {
                        validatePassword(createRandomString(length)) shouldBe false
                    }
                }
            }
        }
    }
    context("Validation of Network Search Query") {
        given("a query") {
            When("it is valid") {
                then("the validator should return true") {
                    for (length: Int in 2..10) {
                        validateNetworkSearchQuery(createRandomString(length)) shouldBe true
                    }
                    validateNetworkSearchQuery("t e") shouldBe true
                }
            }
            When("it is invalid") {
                then("the validator should return false") {
                    validateNetworkSearchQuery(createRandomString(length = 1)) shouldBe false
                    validateNetworkSearchQuery("  ") shouldBe false
                    validateNetworkSearchQuery("t ") shouldBe false
                }
            }
        }
    }
    context("Validation of Location") {
        given("a location") {
            When("it is valid") {
                then("the validator should return true") {
                    validateLocation(-90.0, 180.0) shouldBe true
                    validateLocation(90.0, -180.0) shouldBe true
                }
            }
            When("it is invalid") {
                then("the validator should return false") {
                    validateLocation(0.0, 0.0) shouldBe false
                    validateLocation(-180.0, 0.0) shouldBe false
                    validateLocation(91.0, -181.0) shouldBe false
                }
            }
        }
    }
    context("Validation of ETH Address") {
        given("an ETH Address") {
            When("it is valid") {
                then("the validator should return true") {
                    validateEthAddress("0x0123456789ABCDEF000000000000000000000000") shouldBe true
                }
            }
            When("it is invalid") {
                then("the validator should return false") {
                    validateEthAddress(null) shouldBe false
                    validateEthAddress("000000000000000000000000000000000000000000") shouldBe false
                    validateEthAddress("0a0000000000000000000000000000000000000000") shouldBe false
                    validateEthAddress("0x000000000000000000000000000000000000000") shouldBe false
                }
            }
        }
    }
    context("Validation of Serial Number") {
        given("a serial number of M5") {
            When("it is valid") {
                then("the validator should return true") {
                    validateSerialNumber("0123456789ABCDEF00", DeviceType.M5_WIFI) shouldBe true
                }
            }
            When("it is invalid") {
                then("the validator should return false") {
                    validateSerialNumber("0123456789ABCDEF0", DeviceType.M5_WIFI) shouldBe false
                    validateSerialNumber("0123456789ABCDEF0X", DeviceType.M5_WIFI) shouldBe false
                    validateSerialNumber("0123456789ABCDEF000", DeviceType.M5_WIFI) shouldBe false
                }
            }
        }
        given("a serial number of D1") {
            When("it is valid") {
                then("the validator should return true") {
                    validateSerialNumber("0123456789ABCDEF0000", DeviceType.D1_WIFI) shouldBe true
                }
            }
            When("it is invalid") {
                then("the validator should return false") {
                    validateSerialNumber("0123456789ABCDEF000", DeviceType.D1_WIFI) shouldBe false
                    validateSerialNumber("0123456789ABCDEF000X", DeviceType.D1_WIFI) shouldBe false
                    validateSerialNumber("0123456789ABCDEF00000", DeviceType.D1_WIFI) shouldBe false
                }
            }
        }
        given("a serial number of Pulse") {
            When("it is valid") {
                then("the validator should return true") {
                    validateSerialNumber("0123456789ABCDEF", DeviceType.PULSE_4G) shouldBe true
                }
            }
            When("it is invalid") {
                then("the validator should return false") {
                    validateSerialNumber("0123456789ABCDE", DeviceType.PULSE_4G) shouldBe false
                    validateSerialNumber("0123456789ABCDEX", DeviceType.PULSE_4G) shouldBe false
                    validateSerialNumber("0123456789ABCDEF0", DeviceType.PULSE_4G) shouldBe false
                }
            }
        }
        given("a Helium serial number (not supported through this method)") {
            then("the validator should return false") {
                validateSerialNumber("0123456789ABCDEF", DeviceType.HELIUM) shouldBe false
            }
        }
    }
    context("Validation of Claiming Key") {
        given("a claiming key") {
            When("it is valid") {
                then("the validator should return true") {
                    validateClaimingKey("012345") shouldBe true
                }
            }
            When("it is invalid") {
                then("the validator should return false") {
                    validateClaimingKey("01234") shouldBe false
                    validateClaimingKey("0123456") shouldBe false
                    validateClaimingKey("01234A") shouldBe false
                }
            }
        }
    }
    context("Validation of Friendly Name") {
        given("a claiming key") {
            When("it is valid") {
                then("the validator should return true") {
                    val randomFriendlyName = createRandomString(Random.nextInt(1..64))
                    val success = if (validateFriendlyName(randomFriendlyName)) {
                        true
                    } else {
                        println("[FAILED] Friendly Name: $randomFriendlyName")
                        false
                    }
                    success shouldBe true
                }
            }
            When("it is invalid") {
                then("the validator should return false") {
                    validateFriendlyName(null) shouldBe false
                    validateFriendlyName("") shouldBe false
                    validateFriendlyName(createRandomString(Random.nextInt(65..100))) shouldBe false
                }
            }
        }
    }
})
