package com.weatherxm.analytics

import com.mixpanel.android.mpmetrics.MixpanelAPI
import com.weatherxm.BuildConfig
import org.json.JSONObject
import timber.log.Timber

class MixpanelAnalyticsService(private val mixpanelAPI: MixpanelAPI) : AnalyticsService {

    override fun setUserId(userId: String) {
        mixpanelAPI.identify(userId)
    }

    override fun setUserProperties(params: List<Pair<String, String>>) {
        mixpanelAPI.people.set(paramsToJSON(params))
    }

    override fun setAnalyticsEnabled(enabled: Boolean) {
        if (enabled && mixpanelAPI.hasOptedOutTracking()) {
            mixpanelAPI.optInTracking()
        } else if(!enabled) {
            mixpanelAPI.optOutTracking()
        }
    }

    override fun onLogout() {
        Timber.d("Logged out, resetting Mixpanel")
        mixpanelAPI.reset()
    }

    override fun trackScreen(
        screen: AnalyticsService.Screen,
        screenClass: String,
        itemId: String?
    ) {
        val params =
            mutableListOf(Pair(AnalyticsService.EventKey.SCREEN_NAME.key, screen.screenName))
        itemId?.let {
            params.add(Pair(AnalyticsService.EventKey.ITEM_ID.key, it))
        }
        mixpanelAPI.track(AnalyticsService.EventKey.SCREEN_VIEW.key, paramsToJSON(params))
    }

    @Suppress("SpreadOperator")
    override fun trackEventUserAction(
        actionName: String,
        contentType: String?,
        vararg customParams: Pair<String, String>
    ) {
        val mixpanelParams: MutableList<Pair<String, Any>> = mutableListOf(
            Pair(AnalyticsService.CustomParam.ACTION_NAME.paramName, actionName),
            *customParams.map {
                it
            }.toTypedArray()
        )

        contentType?.let {
            mixpanelParams.add(Pair(AnalyticsService.EventKey.CONTENT_TYPE.key, contentType))
        }
        mixpanelAPI.track(
            AnalyticsService.CustomEvent.USER_ACTION.eventName, paramsToJSON(mixpanelParams)
        )
    }

    @Suppress("SpreadOperator")
    override fun trackEventViewContent(
        contentName: String,
        vararg customParams: Pair<String, String>,
        success: Long?
    ) {
        val mixpanelParams: MutableList<Pair<String, Any>> = mutableListOf(
            Pair(AnalyticsService.CustomParam.CONTENT_NAME.paramName, contentName),
            *customParams.map {
                it
            }.toTypedArray()
        )
        success?.let {
            mixpanelParams.add(Pair(AnalyticsService.EventKey.SUCCESS.key, it))
        }
        mixpanelAPI.track(
            AnalyticsService.CustomEvent.VIEW_CONTENT.eventName, paramsToJSON(mixpanelParams)
        )
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
        mixpanelAPI.track(
            AnalyticsService.CustomEvent.PROMPT.eventName,
            paramsToJSON(mixpanelParams)
        )
    }

    @Suppress("SpreadOperator")
    override fun trackEventSelectContent(
        contentType: String,
        vararg customParams: Pair<String, String>,
        index: Long?
    ) {
        val mixpanelParams: MutableList<Pair<String, Any>> = mutableListOf(
            Pair(AnalyticsService.EventKey.CONTENT_TYPE.key, contentType),
            *customParams.map {
                it
            }.toTypedArray()
        )

        index?.let {
            mixpanelParams.add(Pair(AnalyticsService.EventKey.INDEX.key, it))
        }
        mixpanelAPI.track(
            AnalyticsService.EventKey.SELECT_CONTENT.key, paramsToJSON(mixpanelParams)
        )
    }

    fun paramsToJSON(params: List<Pair<String, Any>>): JSONObject {
        val entries = JSONObject()

        // We want on every event to have the APP ID to identify the source of the event
        entries.put(AnalyticsService.CustomParam.APP_ID.paramName, BuildConfig.APPLICATION_ID)

        params.forEach {
            entries.put(it.first, it.second)
        }
        return entries
    }
}
