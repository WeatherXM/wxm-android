package com.weatherxm.service

import android.content.Context
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClient.ProductType.SUBS
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.Purchase.PurchaseState
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryPurchasesParams
import com.android.billingclient.api.queryPurchasesAsync
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import timber.log.Timber

class BillingService(
    context: Context,
    private val coroutineScope: CoroutineScope,
    private val dispatcher: CoroutineDispatcher
) {
    private var billingClient: BillingClient? = null

    private var activeSub: Purchase? = null
    private var hasFetchedPurchases: Boolean = false

    @OptIn(ExperimentalCoroutinesApi::class, DelicateCoroutinesApi::class)
    private val purchasesUpdatedListener = PurchasesUpdatedListener { billingResult, purchases ->
        Timber.d("[Purchase Update]: $billingResult --- $purchases")

        when (billingResult.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                if (purchases?.get(0)?.purchaseState == PurchaseState.PURCHASED) {
                    // TODO: STOPSHIP Handle new purchase (acknowledge it etc)
                }
            }
            else -> {
                Timber.e("[Purchase Update] Error: $billingResult")
                // TODO: STOPSHIP Got an error. Propagate the result.
            }
        }
    }

    fun hasActiveSub(): Boolean {
        return if (billingClient?.isReady == false && activeSub == null) {
            startConnection()
            false
        } else if (!hasFetchedPurchases) {
            coroutineScope.launch(dispatcher) {
                setupPurchases()
            }
            false
        } else {
            activeSub != null
        }
    }

    fun getActiveSub(): Purchase? = activeSub

    fun startConnection() {
        billingClient?.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    coroutineScope.launch(dispatcher) {
                        setupPurchases()
                    }
                    // TODO: Get the available subs
                }
            }

            override fun onBillingServiceDisconnected() {
                // Not used as we have enableAutoServiceReconnection() above.
            }
        })
    }

    suspend fun setupPurchases() {
        val purchasesResult = billingClient?.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder().setProductType(SUBS).build()
        )

        /**
         * Got an error in the process. Terminate it.
         */
        if (purchasesResult?.billingResult?.responseCode != BillingClient.BillingResponseCode.OK) {
            return
        }
        hasFetchedPurchases = true

        val latestPurchase = purchasesResult.purchasesList.firstOrNull()
        if (latestPurchase != null) {
            if (latestPurchase.isAcknowledged) {
                activeSub = latestPurchase
            } else {
                // TODO: STOPSHIP: Handle the purchase again - not acknowledged (and needs to be!!)
            }
        } else {
            activeSub = null
        }
    }

    init {
        billingClient = BillingClient.newBuilder(context)
            .setListener(purchasesUpdatedListener)
            .enableAutoServiceReconnection()
            .enablePendingPurchases(
                PendingPurchasesParams.newBuilder().enableOneTimeProducts().build()
            )
            .build()
        startConnection()
    }
}
