package com.weatherxm.ui.networkstats.tokenmetrics

import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import com.weatherxm.R
import com.weatherxm.analytics.AnalyticsService
import com.weatherxm.databinding.ActivityTokenMetricsBinding
import com.weatherxm.ui.common.Contracts.ARG_NETWORK_STATS
import com.weatherxm.ui.common.classSimpleName
import com.weatherxm.ui.common.parcelable
import com.weatherxm.ui.common.removeLinksUnderline
import com.weatherxm.ui.common.setHtml
import com.weatherxm.ui.common.toast
import com.weatherxm.ui.common.visible
import com.weatherxm.ui.components.BaseActivity
import com.weatherxm.ui.networkstats.NetworkStats
import com.weatherxm.util.NumberUtils.compactNumber
import me.saket.bettermovementmethod.BetterLinkMovementMethod
import timber.log.Timber

class TokenMetricsActivity : BaseActivity() {
    private lateinit var binding: ActivityTokenMetricsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTokenMetricsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val networkStats = intent.parcelable<NetworkStats>(ARG_NETWORK_STATS)
        if (networkStats == null) {
            Timber.d("Could not start TokenMetricsActivity. Network Stats are null.")
            toast(R.string.error_generic_message)
            finish()
            return
        }

        binding.toolbar.setNavigationOnClickListener {
            finish()
        }

        binding.totalSupplyBtn.setOnClickListener {
            openLearnMoreDialog(
                R.string.total_supply,
                R.string.total_supply_explanation,
                AnalyticsService.ParamValue.TOTAL_SUPPLY.paramValue
            )
        }

        binding.circSupplyBtn.setOnClickListener {
            openLearnMoreDialog(
                R.string.circulating_supply,
                R.string.circulating_supply_explanation,
                AnalyticsService.ParamValue.CIRCULATING_SUPPLY.paramValue
            )
        }

        updateUI(networkStats)
    }

    override fun onResume() {
        super.onResume()
        analytics.trackScreen(AnalyticsService.Screen.TOKEN_METRICS, classSimpleName())
    }

    private fun updateUI(data: NetworkStats) {
        binding.totalSupply.text = compactNumber(data.totalSupply)
        binding.circSupply.text = compactNumber(data.circulatingSupply)
        if (data.totalSupply != null && data.circulatingSupply != null
            && data.totalSupply >= data.circulatingSupply
        ) {
            binding.circSupplyBar.valueTo = data.totalSupply.toFloat()
            binding.circSupplyBar.values = listOf(data.circulatingSupply.toFloat())
        } else {
            binding.circSupplyBar.visible(false)
        }

        data.tokenUrl?.let {
            binding.viewTokenContractBtn.movementMethod =
                BetterLinkMovementMethod.newInstance().apply {
                    setOnLinkClickListener { _, url ->
                        analytics.trackEventSelectContent(
                            AnalyticsService.ParamValue.NETWORK_STATS.paramValue,
                            Pair(
                                FirebaseAnalytics.Param.SOURCE,
                                AnalyticsService.ParamValue.REWARD_CONTRACT.paramValue
                            )
                        )
                        navigator.openWebsite(this@TokenMetricsActivity, url)
                        return@setOnLinkClickListener true
                    }
                }
            binding.viewTokenContractBtn.setHtml(R.string.view_token_contract, it)
            binding.viewTokenContractBtn.removeLinksUnderline()
            binding.viewTokenContractBtn.visible(true)
        }

        binding.lastUpdated.text = getString(R.string.last_updated, data.lastUpdated)
    }
}

