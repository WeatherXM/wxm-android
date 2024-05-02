package com.weatherxm.analytics

import com.google.firebase.analytics.FirebaseAnalytics
import com.mixpanel.android.mpmetrics.MixpanelAPI
import com.weatherxm.BuildConfig
import org.json.JSONObject
import timber.log.Timber

class MixpanelAnalyticsService(private val mixpanelAPI: MixpanelAPI): AnalyticsService {

    override fun setUserProperties(userId: String, params: List<Pair<String, String>>) {
        mixpanelAPI.identify(userId)
        mixpanelAPI.people.set(paramsToJSON(params))
    }

    override fun setAnalyticsEnabled(enabled: Boolean) {
        if (BuildConfig.DEBUG) {
            Timber.d("Skipping analytics tracking in DEBUG mode [enabled=$enabled].")
            mixpanelAPI.optOutTracking()
        } else {
            if (enabled) {
                mixpanelAPI.optInTracking()
            } else {
                mixpanelAPI.optOutTracking()
            }
        }
    }

    override fun trackScreen(screen: AnalyticsService.Screen, screenClass: String, itemId: String?) {
        TODO("Not yet implemented")
    }

    override fun trackScreen(screenName: String, screenClass: String) {
        TODO("Not yet implemented")
    }

    override fun trackEventUserAction(
        actionName: String,
        contentType: String?,
        vararg customParams: Pair<String, String>
    ) {
        TODO("Not yet implemented")
    }

    fun trackScreen(screenClass: String, itemId: String? = null) {
        val params = mutableListOf(Pair(FirebaseAnalytics.Param.SCREEN_CLASS, screenClass))
        itemId?.let {
            params.add(Pair(FirebaseAnalytics.Param.ITEM_ID, it))
        }
        mixpanelAPI.track(FirebaseAnalytics.Param.SCREEN_NAME, paramsToJSON(params))
    }

    @Suppress("SpreadOperator")
    override fun trackEventUserAction(
        actionName: String,
        contentType: String,
        vararg customParams: Pair<String, String>
    ) {
        val mixpanelParams: MutableList<Pair<String, Any>> = mutableListOf(
            Pair(AnalyticsService.CustomParam.ACTION_NAME.paramName, actionName),
            *customParams.map {
                it
            }.toTypedArray()
        )

        contentType?.let {
            mixpanelParams.add(Pair(FirebaseAnalytics.Param.CONTENT_TYPE, contentType))
        }
        mixpanelAPI.track(
            AnalyticsService.CustomEvent.USER_ACTION.eventName, paramsToJSON(mixpanelParams)
        )
    }

    @Suppress("SpreadOperator")
    override fun trackEventViewContent(
        contentName: String,
        contentId: String,
        vararg customParams: Pair<String, String>,
        success: Long?
    ) {
        val mixpanelParams: MutableList<Pair<String, Any>> = mutableListOf(
            Pair(AnalyticsService.CustomParam.CONTENT_NAME.paramName, contentName),
            Pair(AnalyticsService.CustomParam.CONTENT_ID.paramName, contentId),
            *customParams.map {
                it
            }.toTypedArray()
        )
        success?.let {
            mixpanelParams.add(Pair(FirebaseAnalytics.Param.SUCCESS, it))
        }
        mixpanelAPI.track(
            AnalyticsService.CustomEvent.VIEW_CONTENT.eventName, paramsToJSON(mixpanelParams)
        )
    }

    override fun trackEventFailure(failureId: String?) {
        TODO("Not yet implemented")
    }

    @Suppress("SpreadOperator")
    override fun trackEventPrompt(
        promptName: String,
        promptType: String,
        action: String,
        vararg customParams: Pair<String, String>
    ) {
        val mixpanelParams = mutableListOf(
            Pair(AnalyticsService.CustomParam.PROMPT_NAME.paramName, promptName),
            Pair(AnalyticsService.CustomParam.PROMPT_TYPE.paramName, promptType),
            Pair(AnalyticsService.CustomParam.ACTION.paramName, action),
            *customParams.map {
                it
            }.toTypedArray()
        )
        mixpanelAPI.track(AnalyticsService.CustomEvent.PROMPT.eventName, paramsToJSON(mixpanelParams))
    }

    @Suppress("SpreadOperator")
    override fun trackEventSelectContent(
        contentType: String,
        vararg customParams: Pair<String, String>,
        index: Long?
    ) {
        val mixpanelParams: MutableList<Pair<String, Any>> = mutableListOf(
            Pair(FirebaseAnalytics.Param.CONTENT_TYPE, contentType),
            *customParams.map {
                it
            }.toTypedArray()
        )

        index?.let {
            mixpanelParams.add(Pair(FirebaseAnalytics.Param.INDEX, it))
        }
        mixpanelAPI.track(FirebaseAnalytics.Event.SELECT_CONTENT, paramsToJSON(mixpanelParams))
    }

    private fun paramsToJSON(params: List<Pair<String, Any>>): JSONObject {
        val entries = JSONObject()

        params.forEach {
            entries.put(it.first, it.second)
        }
        return entries
    }
}
