package com.weatherxm.ui.rewardsclaim

import android.net.Uri
import androidx.lifecycle.ViewModel
import com.weatherxm.R
import com.weatherxm.ui.common.Contracts.ARG_TOKEN_CLAIMED_AMOUNT
import com.weatherxm.ui.common.UIWalletRewards
import com.weatherxm.util.DisplayModeHelper
import com.weatherxm.util.Resources

class RewardsClaimViewModel(
    private val displayModeHelper: DisplayModeHelper,
    private val resources: Resources
) : ViewModel() {
    companion object {
        const val ARG_THEME = "theme"
        const val ARG_WALLET = "wallet"
        const val ARG_AMOUNT = "amount"
        const val ARG_REDIRECT_URL = "redirect_url"
    }

    fun isRedirectUrl(url: Uri?): Boolean {
        return url.toString()
            .startsWith(resources.getString(R.string.weatherxm_claim_redirect_url))
    }

    fun getQueryParams(data: UIWalletRewards): String {
        var queryParams = "?$ARG_AMOUNT=${data.allocated}" +
            "&$ARG_WALLET=${data.walletAddress}" +
            "&$ARG_REDIRECT_URL=${resources.getString(R.string.weatherxm_claim_redirect_url)}"

        if (!displayModeHelper.isSystem()) {
            queryParams += "&$ARG_THEME=${displayModeHelper.getDisplayMode().lowercase()}"
        }

        return queryParams
    }

    fun getAmountFromRedirectUrl(uri: Uri?): Double {
        return uri?.getQueryParameter(ARG_TOKEN_CLAIMED_AMOUNT)?.toDoubleOrNull() ?: 0.0
    }
}
