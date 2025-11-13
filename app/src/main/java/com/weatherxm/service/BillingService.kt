package com.weatherxm.service

import android.content.Context
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClient.ProductType.SUBS
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.Purchase.PurchaseState
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.android.billingclient.api.queryProductDetails
import com.android.billingclient.api.queryPurchasesAsync
import com.weatherxm.data.models.SubscriptionOffer
import com.weatherxm.data.replaceLast
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

private const val PRODUCT_ID = "premium_forecast"
const val PLAN_MONTHLY = "monthly"
const val PLAN_YEARLY = "yearly"
const val OFFER_FREE_TRIAL = "free-trial"

class BillingService(
    context: Context,
    private val coroutineScope: CoroutineScope,
    private val dispatcher: CoroutineDispatcher
) {
    private var billingClient: BillingClient? = null

    private var activeSub: Purchase? = null
    private var hasFetchedPurchases: Boolean = false
    private var productDetails: ProductDetails? = null
    private var subs = mutableListOf<SubscriptionOffer>()

    @OptIn(ExperimentalCoroutinesApi::class, DelicateCoroutinesApi::class)
    private val purchasesUpdatedListener = PurchasesUpdatedListener { billingResult, purchases ->
        Timber.d("[Purchase Update]: $billingResult --- $purchases")

        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
            if (purchases?.get(0)?.purchaseState == PurchaseState.PURCHASED) {
                // TODO: STOPSHIP Handle new purchase (acknowledge it etc)
            }
        } else {
            Timber.e("[Purchase Update] Error: $billingResult")
            // TODO: STOPSHIP Got an error. Propagate the result.
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

    fun getAvailableSubs(hasFreeTrialAvailable: Boolean): List<SubscriptionOffer> {
        return if (hasFreeTrialAvailable) {
            subs.filter {
                it.offerId == OFFER_FREE_TRIAL || it.offerId == null
            }.distinctBy { it.id }
        } else {
            subs.filter { it.offerId == null }.distinctBy { it.id }
        }
    }

    fun startConnection() {
        billingClient?.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    coroutineScope.launch(dispatcher) {
                        setupPurchases()
                    }
                    coroutineScope.launch(dispatcher) {
                        setupProducts()
                    }
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

    suspend fun setupProducts() {
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(
                listOf(
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(PRODUCT_ID)
                        .setProductType(SUBS)
                        .build()
                )
            )
            .build()

        val productDetailsResult = withContext(Dispatchers.IO) {
            billingClient?.queryProductDetails(params)
        }

        if (productDetailsResult?.productDetailsList.isNullOrEmpty()) {
            return
        }

        productDetails = productDetailsResult.productDetailsList?.get(0)
        subs = mutableListOf()

        productDetails?.subscriptionOfferDetails?.forEach { details ->
            details.pricingPhases.pricingPhaseList.forEach {
                /**
                 * The below might produce duplicates (e.g. a plan with and without the free trial),
                 * the UI will be responsible to show each one by calling the getAvailableSubs()
                 * function.
                 *
                 * Also due to the Billing Service returning the formatted price as "3.99 $"
                 * we format it so that it becomes "3.99$".
                 */
                val offerSupported = details.offerId == OFFER_FREE_TRIAL || details.offerId == null
                if (it.priceAmountMicros > 0 && offerSupported) {
                    subs.add(
                        SubscriptionOffer(
                            details.basePlanId,
                            it.formattedPrice.replaceLast(" ", ""),
                            details.offerToken,
                            details.offerId
                        )
                    )
                }
            }
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
