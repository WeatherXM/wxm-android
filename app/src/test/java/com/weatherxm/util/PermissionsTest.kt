package com.weatherxm.util

import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic

class PermissionsTest : BehaviorSpec({
    val context = mockk<Context>()
    val grantedPermission = "grantedPermission"
    val deniedPermission = "deniedPermission"

    beforeSpec {
        mockkStatic(ContextCompat::class)
        every {
            ContextCompat.checkSelfPermission(context, grantedPermission)
        } returns PackageManager.PERMISSION_GRANTED
        every {
            ContextCompat.checkSelfPermission(context, deniedPermission)
        } returns PackageManager.PERMISSION_DENIED
    }

    given("A Permission") {
        When("The permission is granted") {
            Then("hasPermission should return true") {
                context.hasPermission(grantedPermission) shouldBe true
            }
        }
        When("The permission is denied") {
            Then("hasPermission should return false") {
                context.hasPermission(deniedPermission) shouldBe false
            }
        }
    }
})
