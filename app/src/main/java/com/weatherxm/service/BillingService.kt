package com.weatherxm.service

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClient.BillingResponseCode
import com.android.billingclient.api.BillingClient.ProductType.SUBS
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.Purchase.PurchaseState
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.android.billingclient.api.acknowledgePurchase
import com.android.billingclient.api.queryProductDetails
import com.android.billingclient.api.queryPurchasesAsync
import com.weatherxm.R
import com.weatherxm.data.models.SubscriptionOffer
import com.weatherxm.data.replaceLast
import com.weatherxm.ui.common.PurchaseUpdateState
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.security.KeyFactory
import java.security.NoSuchAlgorithmException
import java.security.Signature
import java.security.spec.InvalidKeySpecException
import java.security.spec.X509EncodedKeySpec
import java.util.Base64

const val PREMIUM_FORECAST_PRODUCT_ID = "premium_forecast"
const val PLAN_MONTHLY = "monthly"
const val PLAN_YEARLY = "yearly"
const val OFFER_FREE_TRIAL = "free-trial"

class BillingService(
    private val context: Context,
    private val coroutineScope: CoroutineScope,
    private val dispatcher: CoroutineDispatcher
) {
    private var billingClient: BillingClient? = null

    private var hasFetchedPurchases: Boolean = false
    private var subs = mutableListOf<SubscriptionOffer>()
    private var acknowledgeJob: Job? = null

    private val purchaseUpdate = MutableSharedFlow<PurchaseUpdateState?>(
        replay = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    private val activeSubFlow = MutableStateFlow<Purchase?>(null)

    fun getPurchaseUpdates(): SharedFlow<PurchaseUpdateState?> = purchaseUpdate

    @OptIn(ExperimentalCoroutinesApi::class)
    fun clearPurchaseUpdates() = purchaseUpdate.resetReplayCache()

    @OptIn(ExperimentalCoroutinesApi::class, DelicateCoroutinesApi::class)
    private val purchasesUpdatedListener = PurchasesUpdatedListener { billingResult, purchases ->
        if (billingResult.responseCode == BillingResponseCode.OK) {
            if (purchases?.get(0)?.purchaseState == PurchaseState.PURCHASED) {
                Timber.d("[Purchase Update]: Purchase was successful")
                coroutineScope.launch {
                    handlePurchase(purchases[0], false)
                }
            }
        } else if (billingResult.responseCode == BillingResponseCode.USER_CANCELED) {
            Timber.w("[Purchase Update]: Purchase was canceled")
            purchaseUpdate.tryEmit(
                PurchaseUpdateState(
                    success = false,
                    isLoading = false,
                    responseCode = billingResult.responseCode,
                    debugMessage = billingResult.debugMessage
                )
            )
        } else {
            Timber.e("[Purchase Update]: Purchase failed $billingResult")
            purchaseUpdate.tryEmit(
                PurchaseUpdateState(
                    success = false,
                    isLoading = false,
                    responseCode = billingResult.responseCode,
                    debugMessage = billingResult.debugMessage
                )
            )
        }
    }

    fun hasActiveSub(): Boolean {
        val activeSub = activeSubFlow.value
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

    fun getActiveSubFlow(): StateFlow<Purchase?> = activeSubFlow

    fun getAvailableSubs(hasFreeTrialAvailable: Boolean): List<SubscriptionOffer> {
        return if (hasFreeTrialAvailable) {
            subs.filter {
                it.offerId == OFFER_FREE_TRIAL || it.offerId == null
            }.distinctBy { it.id }
        } else {
            subs.filter { it.offerId == null }.distinctBy { it.id }
        }
    }

    private fun startConnection() {
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

    suspend fun setupPurchases(inBackground: Boolean = true) {
        val purchasesResult = billingClient?.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder().setProductType(SUBS).build()
        )

        /**
         * Got an error in the process. Terminate it.
         */
        if (purchasesResult?.billingResult?.responseCode != BillingResponseCode.OK) {
            return
        }
        hasFetchedPurchases = true

        val latestPurchase = purchasesResult.purchasesList.firstOrNull()
        if (latestPurchase != null) {
            if (latestPurchase.isAcknowledged) {
                activeSubFlow.tryEmit(latestPurchase)
            } else {
                handlePurchase(latestPurchase, inBackground)
            }
        } else {
            activeSubFlow.tryEmit(null)
        }
    }

    private suspend fun getSubscriptionProduct(): ProductDetails? {
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(
                listOf(
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(PREMIUM_FORECAST_PRODUCT_ID)
                        .setProductType(SUBS)
                        .build()
                )
            )
            .build()

        val productDetailsResult = withContext(Dispatchers.IO) {
            billingClient?.queryProductDetails(params)
        }

        return productDetailsResult?.productDetailsList?.getOrNull(0)
    }

    private suspend fun setupProducts() {
        val productDetails = getSubscriptionProduct()
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

    fun startBillingFlow(activity: Activity, offerToken: String?) {
        purchaseUpdate.tryEmit(
            PurchaseUpdateState(
                success = false,
                isLoading = true,
                responseCode = null,
                debugMessage = null
            )
        )

        coroutineScope.launch(dispatcher) {
            val productDetails = getSubscriptionProduct()

            if (offerToken.isNullOrEmpty() || productDetails == null) {
                return@launch
            }

            val productDetailsParamsList = listOf(
                BillingFlowParams.ProductDetailsParams.newBuilder()
                    .setProductDetails(productDetails)
                    .setOfferToken(offerToken)
                    .build()
            )

            val billingFlowParams = BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(productDetailsParamsList)
                .build()

            val billingResult = billingClient?.launchBillingFlow(activity, billingFlowParams)
            Timber.d("[Purchase Update]: Purchase Flow Launch: $billingResult")

            when (billingResult?.responseCode) {
                BillingResponseCode.OK -> {
                    // All good, billing flow started, do nothing.
                }
                BillingResponseCode.USER_CANCELED -> {
                    purchaseUpdate.tryEmit(
                        PurchaseUpdateState(
                            success = false,
                            isLoading = false,
                            responseCode = billingResult.responseCode,
                            debugMessage = billingResult.debugMessage
                        )
                    )
                }
                else -> {
                    Timber.w("[Purchase Update]: Purchase failed $billingResult")
                    purchaseUpdate.tryEmit(
                        PurchaseUpdateState(
                            success = false,
                            isLoading = false,
                            responseCode = billingResult?.responseCode,
                            debugMessage = billingResult?.debugMessage
                        )
                    )
                }
            }
        }
    }

    private fun handlePurchase(purchase: Purchase, inBackground: Boolean) {
        if (!verifyPurchase(purchase.originalJson, purchase.signature)) {
            if (inBackground) return
            purchaseUpdate.tryEmit(
                PurchaseUpdateState(
                    success = false,
                    isLoading = false,
                    responseCode = null,
                    debugMessage = "Verification Failed"
                )
            )
            return
        }
        if (!purchase.isAcknowledged) {
            acknowledgePurchase(purchase, inBackground)
        }
    }

    private fun acknowledgePurchase(purchase: Purchase, inBackground: Boolean = false) {
        if (acknowledgeJob?.isActive == true) {
            return
        }
        acknowledgeJob = coroutineScope.launch(Dispatchers.IO) {
            val params =
                AcknowledgePurchaseParams.newBuilder().setPurchaseToken(purchase.purchaseToken)
            val result = billingClient?.acknowledgePurchase(params.build())
            Timber.d("[Acknowledge Purchase Update]: $result")

            if (inBackground) return@launch

            if (result?.responseCode == BillingResponseCode.OK) {
                activeSubFlow.tryEmit(purchase)
                purchaseUpdate.tryEmit(
                    PurchaseUpdateState(
                        success = true,
                        isLoading = false,
                        responseCode = result.responseCode,
                        debugMessage = result.debugMessage
                    )
                )
            } else {
                activeSubFlow.tryEmit(null)
                purchaseUpdate.tryEmit(
                    PurchaseUpdateState(
                        success = false,
                        isLoading = false,
                        responseCode = result?.responseCode,
                        debugMessage = result?.debugMessage
                    )
                )
            }
        }
    }

    private fun verifyPurchase(json: String, sig: String): Boolean {
        return try {
            val key =
                Base64.getDecoder().decode(context.getString(R.string.base64_encoded_pub_key))
            val pubKey = KeyFactory.getInstance("RSA").generatePublic(
                X509EncodedKeySpec(key)
            )
            val signatureBytes = Base64.getDecoder().decode(sig)
            val signature = Signature.getInstance("SHA1withRSA")
            signature.initVerify(pubKey)
            signature.update(json.toByteArray())
            signature.verify(signatureBytes)
        } catch (e: IllegalArgumentException) {
            Timber.e(e)
            false
        } catch (e: NoSuchAlgorithmException) {
            Timber.e(e)
            false
        } catch (e: InvalidKeySpecException) {
            Timber.e(e)
            false
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
