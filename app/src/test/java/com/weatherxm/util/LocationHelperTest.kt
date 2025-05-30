package com.weatherxm.util

import android.Manifest.permission.ACCESS_COARSE_LOCATION
import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.content.pm.PackageManager
import com.google.android.gms.location.FusedLocationProviderClient
import com.weatherxm.TestConfig.context
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk

class LocationHelperTest : BehaviorSpec({
    val fusedLocationProvider = mockk<FusedLocationProviderClient>()
    val locationHelper = LocationHelper(context, fusedLocationProvider)

    fun mockHasPermission(fineLocationPerm: Int, coarseLocationPerm: Int) {
        every {
            context.checkSelfPermission(ACCESS_FINE_LOCATION)
        } returns fineLocationPerm
        every {
            context.checkSelfPermission(ACCESS_COARSE_LOCATION)
        } returns coarseLocationPerm
    }

    context("Get if the app has location permissions") {
        given("ACCESS_FINE_LOCATION or ACCESS_COARSE_LOCATION") {
            When("ACCESS_FINE_LOCATION = granted") {
                mockHasPermission(
                    fineLocationPerm = PackageManager.PERMISSION_GRANTED,
                    coarseLocationPerm = PackageManager.PERMISSION_DENIED
                )
                then("hasLocationPermissions should return true") {
                    locationHelper.hasLocationPermissions() shouldBe true
                }
            }
            When("ACCESS_COARSE_LOCATION = granted") {
                mockHasPermission(
                    fineLocationPerm = PackageManager.PERMISSION_DENIED,
                    coarseLocationPerm = PackageManager.PERMISSION_GRANTED
                )
                then("hasLocationPermissions should return true") {
                    locationHelper.hasLocationPermissions() shouldBe true
                }
            }
            When("Both are NOT granted") {
                mockHasPermission(
                    fineLocationPerm = PackageManager.PERMISSION_DENIED,
                    coarseLocationPerm = PackageManager.PERMISSION_DENIED
                )
                then("hasLocationPermissions should return false") {
                    locationHelper.hasLocationPermissions() shouldBe false
                }
            }
        }
    }
})
