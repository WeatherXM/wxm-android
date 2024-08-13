package com.weatherxm.util

import android.content.res.Resources
import com.weatherxm.R
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify

class ResourcesTest : BehaviorSpec({
    val androidResources = mockk<Resources>()
    val resourcesHelper = Resources(androidResources)
    val stringResponse = "OK"
    val arg = "test"
    val stringResponseWithArg = "OK test"

    beforeSpec {
        every { androidResources.getColor(R.color.colorPrimary, null) } returns 0
        every { androidResources.getString(R.string.action_ok) } returns stringResponse
        every {
            androidResources.getString(R.string.action_ok, any())
        } returns stringResponseWithArg
    }

    given("a Color resources ID") {
        then("Return that color from resources") {
            resourcesHelper.getColor(R.color.colorPrimary) shouldBe 0
            verify(exactly = 1) { androidResources.getColor(R.color.colorPrimary, null) }
        }
    }

    given("a String resources ID") {
        When("that string doesn't have any arguments") {
            then("Return that string from resources") {
                resourcesHelper.getString(R.string.action_ok) shouldBe stringResponse
                verify(exactly = 1) { androidResources.getString(R.string.action_ok) }
            }
        }
        When("that string has arguments") {
            then("Return that string from resources") {
                resourcesHelper.getString(R.string.action_ok, arg) shouldBe stringResponseWithArg
                verify(exactly = 1) { androidResources.getString(R.string.action_ok, arg) }
            }
        }
    }
})
