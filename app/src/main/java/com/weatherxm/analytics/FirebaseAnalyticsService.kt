package com.weatherxm.analytics

import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ParametersBuilder
import com.google.firebase.analytics.logEvent
import com.weatherxm.BuildConfig
import timber.log.Timber

class FirebaseAnalyticsService(private val firebaseAnalytics: FirebaseAnalytics) :
    AnalyticsService {

    override fun setUserProperties(userId: String, params: List<Pair<String, String>>) {
        firebaseAnalytics.setUserId(userId)
        params.forEach {
            firebaseAnalytics.setUserProperty(it.first, it.second)
        }
    }

    override fun trackScreen(
        screen: AnalyticsService.Screen,
        screenClass: String,
        itemId: String?
    ) {
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW) {
            param(FirebaseAnalytics.Param.SCREEN_NAME, screen.screenName)
            param(FirebaseAnalytics.Param.SCREEN_CLASS, screenClass)
            itemId?.let {
                param(FirebaseAnalytics.Param.ITEM_ID, itemId)
            }
        }
    }

    override fun setAnalyticsEnabled(enabled: Boolean) {
        if (BuildConfig.DEBUG) {
            Timber.d("Skipping analytics tracking in DEBUG mode [enabled=$enabled].")
            firebaseAnalytics.setAnalyticsCollectionEnabled(enabled)
        } else {
            Timber.d("Resetting analytics tracking [enabled=$enabled]")
            firebaseAnalytics.setAnalyticsCollectionEnabled(enabled)
        }
    }

    override fun trackEventUserAction(
        actionName: String,
        contentType: String?,
        vararg customParams: Pair<String, String>
    ) {
        val params = ParametersBuilder().apply {
            param(AnalyticsService.CustomParam.ACTION_NAME.paramName, actionName)

            contentType?.let {
                param(FirebaseAnalytics.Param.CONTENT_TYPE, contentType)
            }

            customParams.forEach {
                param(it.first, it.second)
            }
        }
        firebaseAnalytics.logEvent(
            AnalyticsService.CustomEvent.USER_ACTION.eventName,
            params.bundle
        )
    }

    override fun trackEventViewContent(
        contentName: String,
        contentId: String?,
        vararg customParams: Pair<String, String>,
        success: Long?
    ) {
        val params = ParametersBuilder().apply {
            param(AnalyticsService.CustomParam.CONTENT_NAME.paramName, contentName)

            contentId?.let {
                param(AnalyticsService.CustomParam.CONTENT_ID.paramName, it)
            }

            customParams.forEach {
                param(it.first, it.second)
            }
            success?.let {
                param(FirebaseAnalytics.Param.SUCCESS, it)
            }
        }
        firebaseAnalytics.logEvent(
            AnalyticsService.CustomEvent.VIEW_CONTENT.eventName,
            params.bundle
        )
    }

    override fun trackEventPrompt(
        promptName: String,
        promptType: String,
        action: String,
        vararg customParams: Pair<String, String>
    ) {
        val params = ParametersBuilder().apply {
            param(AnalyticsService.CustomParam.PROMPT_NAME.paramName, promptName)
            param(AnalyticsService.CustomParam.PROMPT_TYPE.paramName, promptType)
            param(AnalyticsService.CustomParam.ACTION.paramName, action)

            customParams.forEach {
                param(it.first, it.second)
            }
        }
        firebaseAnalytics.logEvent(AnalyticsService.CustomEvent.PROMPT.eventName, params.bundle)
    }

    override fun trackEventSelectContent(
        contentType: String,
        vararg customParams: Pair<String, String>,
        index: Long?
    ) {
        val params = ParametersBuilder().apply {
            param(FirebaseAnalytics.Param.CONTENT_TYPE, contentType)

            customParams.forEach {
                param(it.first, it.second)
            }

            index?.let {
                param(FirebaseAnalytics.Param.INDEX, it)
            }
        }
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, params.bundle)
    }
}
