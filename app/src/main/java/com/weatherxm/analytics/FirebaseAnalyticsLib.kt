package com.weatherxm.analytics

import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ParametersBuilder
import com.google.firebase.analytics.logEvent
import com.weatherxm.BuildConfig
import com.weatherxm.ui.common.empty
import timber.log.Timber

class FirebaseAnalyticsLib(private val firebaseAnalytics: FirebaseAnalytics) {

    fun setUserProperties(userId: String, params: List<Pair<String, String>>) {
        firebaseAnalytics.setUserId(userId)
        params.forEach {
            firebaseAnalytics.setUserProperty(it.first, it.second)
        }
    }

    fun setAnalyticsEnabled(enabled: Boolean) {
        if (BuildConfig.DEBUG) {
            Timber.d("Skipping analytics tracking in DEBUG mode [enabled=$enabled].")
            firebaseAnalytics.setAnalyticsCollectionEnabled(false)
        } else {
            Timber.d("Resetting analytics tracking [enabled=$enabled]")
            firebaseAnalytics.setAnalyticsCollectionEnabled(enabled)
        }
    }

    fun trackScreen(screenName: String, screenClass: String?, itemId: String? = null) {
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW) {
            param(FirebaseAnalytics.Param.SCREEN_NAME, screenName)
            param(FirebaseAnalytics.Param.SCREEN_CLASS, screenClass ?: String.empty())
            itemId?.let {
                param(FirebaseAnalytics.Param.ITEM_ID, itemId)
            }
        }
    }

    fun trackEventUserAction(
        actionName: String,
        contentType: String? = null,
        vararg customParams: Pair<String, String>
    ) {
        val params = ParametersBuilder().apply {
            param(Analytics.CustomParam.ACTION_NAME.paramName, actionName)

            contentType?.let {
                param(FirebaseAnalytics.Param.CONTENT_TYPE, contentType)
            }

            customParams.forEach {
                param(it.first, it.second)
            }
        }
        firebaseAnalytics.logEvent(Analytics.CustomEvent.USER_ACTION.eventName, params.bundle)
    }

    fun trackEventViewContent(
        contentName: String,
        contentId: String,
        vararg customParams: Pair<String, String>,
        success: Long? = null
    ) {
        val params = ParametersBuilder().apply {
            param(Analytics.CustomParam.CONTENT_NAME.paramName, contentName)
            param(Analytics.CustomParam.CONTENT_ID.paramName, contentId)

            customParams.forEach {
                param(it.first, it.second)
            }
            success?.let {
                param(FirebaseAnalytics.Param.SUCCESS, it)
            }
        }
        firebaseAnalytics.logEvent(Analytics.CustomEvent.VIEW_CONTENT.eventName, params.bundle)
    }

    fun trackEventPrompt(
        promptName: String,
        promptType: String,
        action: String,
        vararg customParams: Pair<String, String>
    ) {
        val params = ParametersBuilder().apply {
            param(Analytics.CustomParam.PROMPT_NAME.paramName, promptName)
            param(Analytics.CustomParam.PROMPT_TYPE.paramName, promptType)
            param(Analytics.CustomParam.ACTION.paramName, action)

            customParams.forEach {
                param(it.first, it.second)
            }
        }
        firebaseAnalytics.logEvent(Analytics.CustomEvent.PROMPT.eventName, params.bundle)
    }

    fun trackEventSelectContent(
        contentType: String,
        vararg customParams: Pair<String, String>,
        index: Long? = null
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
