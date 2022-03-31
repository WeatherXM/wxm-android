package com.weatherxm.ui.token

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.weatherxm.R
import com.weatherxm.data.Failure
import com.weatherxm.data.Resource
import com.weatherxm.data.Transaction
import com.weatherxm.usecases.TokenUseCase
import com.weatherxm.util.ResourcesHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber

class TokenViewModel : ViewModel(), KoinComponent {
    companion object {
        const val TransactionExplorer = "https://mumbai.polygonscan.com/tx/"
    }

    private val tokenUseCase: TokenUseCase by inject()
    private val resHelper: ResourcesHelper by inject()

    private var currentPage = 0
    private var hasNextPage = false
    private var blockNewPageRequest = false
    private val currentShownTransactions = mutableListOf<Transaction>()

    private val onFirstPageTransactions = MutableLiveData<Resource<List<Transaction>>>().apply {
        value = Resource.loading()
    }

    private val onNewTransactionsPage = MutableLiveData<Resource<List<Transaction>>>()

    fun onFirstPageTransactions(): LiveData<Resource<List<Transaction>>> = onFirstPageTransactions

    fun onNewTransactionsPage(): LiveData<Resource<List<Transaction>>> = onNewTransactionsPage

    fun fetchFirstPageTransactions(deviceId: String) {
        onFirstPageTransactions.postValue(Resource.loading())
        CoroutineScope(Dispatchers.IO).launch {
            tokenUseCase.getTransactions(deviceId, currentPage)
                .map {
                    Timber.d("Got Transactions: ${it.transactions}")
                    hasNextPage = it.hasNextPage
                    currentShownTransactions.addAll(it.transactions)
                    onFirstPageTransactions.postValue(Resource.success(currentShownTransactions))
                }
                .mapLeft {
                    handleFailure(it)
                }
        }
    }

    fun fetchNewPageTransactions(deviceId: String) {
        if (hasNextPage && !blockNewPageRequest) {
            onNewTransactionsPage.postValue(Resource.loading())
            CoroutineScope(Dispatchers.IO).launch {
                currentPage++
                blockNewPageRequest = true
                tokenUseCase.getTransactions(deviceId, currentPage)
                    .map {
                        Timber.d("Got Transactions: ${it.transactions}")
                        hasNextPage = it.hasNextPage
                        currentShownTransactions.addAll(it.transactions)
                        onNewTransactionsPage.postValue(Resource.success(currentShownTransactions))
                    }
                blockNewPageRequest = false
            }
        }
    }

    private fun handleFailure(failure: Failure) {
        onFirstPageTransactions.postValue(
            Resource.error(
                resHelper.getString(
                    when (failure) {
                        is Failure.NetworkError -> R.string.error_network
                        else -> R.string.error_unknown
                    }
                )
            )
        )
    }
}
