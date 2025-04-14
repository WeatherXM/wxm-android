package com.weatherxm.util

import android.content.res.Configuration
import android.content.res.Configuration.UI_MODE_NIGHT_NO
import android.content.res.Configuration.UI_MODE_NIGHT_YES
import android.content.res.Resources
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
import androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO
import androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES
import com.weatherxm.R
import com.weatherxm.TestConfig.sharedPref
import com.weatherxm.analytics.AnalyticsService
import com.weatherxm.analytics.AnalyticsWrapper
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.core.spec.style.scopes.BehaviorSpecWhenContainerScope
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.spyk
import io.mockk.verify

class DisplayModeHelperTest : BehaviorSpec({
    val androidResources = mockk<Resources>()
    val configuration = mockk<Configuration>()
    val analyticsWrapper = mockk<AnalyticsWrapper>()
    val displayModeHelper = spyk(DisplayModeHelper(androidResources, sharedPref, analyticsWrapper))

    val theme = "theme"
    val system = "system"
    val dark = "dark"
    val light = "light"

    beforeSpec {
        every { androidResources.configuration } returns configuration
        every { androidResources.getString(R.string.key_theme) } returns theme
        every { androidResources.getString(R.string.dark_value) } returns dark
        every { androidResources.getString(R.string.light_value) } returns light
        every { androidResources.getString(R.string.system_value) } returns system
        justRun { analyticsWrapper.setDisplayMode(any()) }
        justRun { displayModeHelper.setDefaultNightMode(MODE_NIGHT_YES) }
        justRun { displayModeHelper.setDefaultNightMode(MODE_NIGHT_NO) }
        justRun { displayModeHelper.setDefaultNightMode(MODE_NIGHT_FOLLOW_SYSTEM) }
    }

    suspend fun BehaviorSpecWhenContainerScope.testDisplayMode(
        mode: String,
        setDefaultNightMode: Int
    ) {
        // Mock response from shared preferences which acts as "current theme" value
        every { sharedPref.getString(theme, system) } returns mode

        then("The setter should work properly") {
            displayModeHelper.setDisplayMode(mode)
            verify(exactly = 1) {
                displayModeHelper.setDefaultNightMode(setDefaultNightMode)
            }
        }
        and("The getter should work properly") {
            displayModeHelper.getDisplayMode() shouldBe mode
        }
        and("The isSystem() should work properly") {
            displayModeHelper.isSystem() shouldBe (mode == system)
        }
        and("The isDarkModeEnabled() should work properly") {
            if (mode == system) {
                When("The system is dark") {
                    configuration.uiMode = UI_MODE_NIGHT_YES
                    displayModeHelper.isDarkModeEnabled() shouldBe true
                }
                When("The system is light") {
                    configuration.uiMode = UI_MODE_NIGHT_NO
                    displayModeHelper.isDarkModeEnabled() shouldBe false
                }
            } else {
                displayModeHelper.isDarkModeEnabled() shouldBe (mode == dark)
            }
        }
        and("The updateDisplayModeInAnalytics() should work properly") {
            val analyticsProperty = when (mode) {
                dark -> AnalyticsService.UserProperty.DARK.propertyName
                light -> AnalyticsService.UserProperty.LIGHT.propertyName
                else -> AnalyticsService.UserProperty.SYSTEM.propertyName
            }
            verify(exactly = 1) { analyticsWrapper.setDisplayMode(analyticsProperty) }
        }
    }

    context("Perform display mode related tests (set/get etc)") {
        given("a display mode") {
            var displayMode: String
            When("SYSTEM") {
                displayMode = system
                testDisplayMode(displayMode, MODE_NIGHT_FOLLOW_SYSTEM)
            }
            When("DARK") {
                displayMode = dark
                testDisplayMode(displayMode, MODE_NIGHT_YES)
            }
            When("LIGHT") {
                displayMode = light
                testDisplayMode(displayMode, MODE_NIGHT_NO)
            }
        }
        given("no selected display mode") {
            every { sharedPref.getString(theme, system) } returns system
            displayModeHelper.setDisplayMode()
            then("System should be set") {
                verify(exactly = 2) {
                    displayModeHelper.setDefaultNightMode(MODE_NIGHT_FOLLOW_SYSTEM)
                }
            }

        }
    }
})
