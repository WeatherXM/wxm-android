package com.weatherxm.ui.token

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.weatherxm.data.Failure
import com.weatherxm.data.Resource
import com.weatherxm.usecases.TokenUseCase
import com.weatherxm.util.UIErrors.getDefaultMessage
import com.weatherxm.util.toISODate
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber
import java.time.ZonedDateTime

class TokenViewModel : ViewModel(), KoinComponent {
    companion object {
        const val TransactionExplorer = "https://mumbai.polygonscan.com/tx/"
        const val FETCH_INTERVAL_MONTHS = 3L
    }

    private val tokenUseCase: TokenUseCase by inject()

    private var currentPage = 0
    private var hasNextPage = false
    private var blockNewPageRequest = false
    private var reachedTotal = false
    private var currFromDate = ZonedDateTime.now().minusMonths(FETCH_INTERVAL_MONTHS)
    private var currToDate = ZonedDateTime.now()
    private val currentShownTransactions = mutableListOf<UITransaction>()

    private val onFirstPageTransactions = MutableLiveData<Resource<List<UITransaction>>>().apply {
        value = Resource.loading()
    }

    private val onNewTransactionsPage = MutableLiveData<Resource<List<UITransaction>>>()

    fun onFirstPageTransactions(): LiveData<Resource<List<UITransaction>>> = onFirstPageTransactions

    fun onNewTransactionsPage(): LiveData<Resource<List<UITransaction>>> = onNewTransactionsPage

    fun fetchFirstPageTransactions(deviceId: String) {
        onFirstPageTransactions.postValue(Resource.loading())
        viewModelScope.launch {
            tokenUseCase.getTransactions(deviceId, currentPage, currFromDate.toISODate())
                .map {
                    Timber.d("Got Transactions: ${it.uiTransactions}")
                    hasNextPage = it.hasNextPage
                    reachedTotal = it.reachedTotal
                    currentShownTransactions.addAll(it.uiTransactions)
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
            viewModelScope.launch {
                currentPage++
                blockNewPageRequest = true

                tokenUseCase.getTransactions(
                    deviceId,
                    currentPage,
                    currFromDate.toISODate(),
                    currToDate.toISODate()
                ).map {
                    Timber.d("Got Transactions: ${it.uiTransactions}")
                    hasNextPage = it.hasNextPage
                    reachedTotal = it.reachedTotal
                    currentShownTransactions.addAll(it.uiTransactions)
                    onNewTransactionsPage.postValue(Resource.success(currentShownTransactions))
                }

                blockNewPageRequest = false
            }
        } else if (!hasNextPage && !blockNewPageRequest && !reachedTotal) {
            onNewTransactionsPage.postValue(Resource.loading())
            viewModelScope.launch {
                currentPage = 0
                blockNewPageRequest = true
                currToDate = currFromDate
                currFromDate = currFromDate.minusMonths(FETCH_INTERVAL_MONTHS)

                tokenUseCase.getTransactions(
                    deviceId,
                    currentPage,
                    currFromDate.toISODate(),
                    currToDate.toISODate()
                ).map {
                    Timber.d("Got Transactions: ${it.uiTransactions}")
                    hasNextPage = it.hasNextPage
                    reachedTotal = it.reachedTotal
                    currentShownTransactions.addAll(it.uiTransactions)
                    onNewTransactionsPage.postValue(Resource.success(currentShownTransactions))
                }
                blockNewPageRequest = false
            }
        }
    }

    private fun handleFailure(failure: Failure) {
        onFirstPageTransactions.postValue(Resource.error(failure.getDefaultMessage()))
    }
}
