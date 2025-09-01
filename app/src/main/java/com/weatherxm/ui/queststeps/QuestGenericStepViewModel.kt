package com.weatherxm.ui.queststeps

import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.funkatronics.encoders.Base58
import com.solana.mobilewalletadapter.clientlib.ActivityResultSender
import com.solana.mobilewalletadapter.clientlib.ConnectionIdentity
import com.solana.mobilewalletadapter.clientlib.MobileWalletAdapter
import com.solana.mobilewalletadapter.clientlib.TransactionResult
import com.solana.mobilewalletadapter.common.signin.SignInWithSolana
import com.weatherxm.R
import com.weatherxm.data.datasource.QuestsDataSourceImpl
import com.weatherxm.data.datasource.QuestsDataSourceImpl.Companion.ONBOARDING_ID
import com.weatherxm.ui.common.QuestStep
import com.weatherxm.ui.common.SingleLiveEvent
import com.weatherxm.usecases.QuestsUseCase
import com.weatherxm.util.Resources
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import timber.log.Timber

class QuestGenericStepViewModel(
    val questStep: QuestStep,
    val userId: String,
    private val useCase: QuestsUseCase,
    private val resources: Resources,
    private val dispatcher: CoroutineDispatcher
) : ViewModel() {
    private val _onStepCompleted = SingleLiveEvent<Throwable?>()
    private val _onStepSkipped = SingleLiveEvent<Throwable?>()
    private val _onWalletAddressError = SingleLiveEvent<String?>()

    fun onStepCompleted(): SingleLiveEvent<Throwable?> = _onStepCompleted
    fun onStepSkipped(): SingleLiveEvent<Throwable?> = _onStepSkipped
    fun onWalletAddressError(): SingleLiveEvent<String?> = _onWalletAddressError

    fun markStepAsCompleted() {
        viewModelScope.launch(dispatcher) {
            useCase.markQuestStepAsCompleted(userId, ONBOARDING_ID, questStep.id)
                .onRight {
                    _onStepCompleted.postValue(null)
                }
                .onLeft {
                    Timber.e(it, "[Firestore]: Error when marking the step as completed")
                    _onStepCompleted.postValue(it)
                }
        }
    }

    fun markStepAsSkipped() {
        viewModelScope.launch(dispatcher) {
            useCase.markQuestStepAsSkipped(userId, ONBOARDING_ID, questStep.id)
                .onRight {
                    _onStepSkipped.postValue(null)
                }
                .onLeft {
                    Timber.e(it, "[Firestore]: Error when marking the step as completed")
                    _onStepSkipped.postValue(it)
                }
        }
    }

    fun connectSolanaWallet(senderForSolanaWallet: ActivityResultSender) {
        val walletAdapter = MobileWalletAdapter(
            connectionIdentity = ConnectionIdentity(
                identityUri = "https://weatherxm.network".toUri(),
                iconUri = "logo-square.png".toUri(),
                identityName = "WeatherXM App"
            )
        )
        viewModelScope.launch {
            val payload = SignInWithSolana.Payload("weatherxm.network", "Sign in to WeatherXM App")
            val result = walletAdapter.signIn(senderForSolanaWallet, payload)

            when (result) {
                is TransactionResult.Success -> {
                    result.authResult.signInResult?.publicKey?.let {
                        val addressConnected = Base58.encodeToString(it)
                        Timber.d("Address connected: $addressConnected")
                        setWalletAddress(QuestsDataSourceImpl.SOLANA_CHAIN_ID, addressConnected)
                    } ?: _onWalletAddressError.postValue(
                        resources.getString(R.string.error_address_not_found)
                    )
                }
                is TransactionResult.NoWalletFound -> {
                    Timber.e("No MWA compatible wallet app found on device.: ${result.message}")
                    _onWalletAddressError.postValue(
                        resources.getString(R.string.error_no_compatible_wallet)
                    )
                }
                is TransactionResult.Failure -> {
                    Timber.e(result.e, "Error connecting to wallet")
                    _onWalletAddressError.postValue(result.e.message)
                }
            }
        }
    }

    suspend fun setWalletAddress(chainId: String, walletAddress: String) {
        useCase.setWallet(userId, chainId, walletAddress).onRight {
            markStepAsCompleted()
        }.onLeft {
            _onWalletAddressError.postValue(it.message)
        }
    }
}
