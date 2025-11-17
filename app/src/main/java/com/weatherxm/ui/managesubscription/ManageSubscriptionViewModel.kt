package com.weatherxm.ui.managesubscription

import androidx.lifecycle.ViewModel

class ManageSubscriptionViewModel() : ViewModel() {

    private var offerToken: String? = null
    fun getOfferToken(): String? = offerToken

    fun setOfferToken(token: String) {
        offerToken = token
    }
}
