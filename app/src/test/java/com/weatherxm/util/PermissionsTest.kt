package com.weatherxm.util

import android.content.pm.PackageManager
import com.weatherxm.TestConfig.context
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every

class PermissionsTest : BehaviorSpec({
    val grantedPermission = "grantedPermission"
    val deniedPermission = "deniedPermission"

    beforeSpec {
        every {
            context.checkSelfPermission(grantedPermission)
        } returns PackageManager.PERMISSION_GRANTED
        every {
            context.checkSelfPermission(deniedPermission)
        } returns PackageManager.PERMISSION_DENIED
    }

    given("A Permission") {
        When("The permission is granted") {
            then("hasPermission should return true") {
                context.hasPermission(grantedPermission) shouldBe true
            }
        }
        When("The permission is denied") {
            then("hasPermission should return false") {
                context.hasPermission(deniedPermission) shouldBe false
            }
        }
    }
})
