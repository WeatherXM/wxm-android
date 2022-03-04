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
        const val TransactionExplorer = "https://polygonscan.com/tx/"
    }

    private val tokenUseCase: TokenUseCase by inject()
    private val resHelper: ResourcesHelper by inject()

    // All charts currently visible
    private val onTransactions = MutableLiveData<Resource<List<Transaction>>>().apply {
        value = Resource.loading()
    }

    fun onTransactions(): LiveData<Resource<List<Transaction>>> = onTransactions

    fun fetchTransactions(deviceId: String) {
        onTransactions.postValue(Resource.loading())
        CoroutineScope(Dispatchers.IO).launch {
            tokenUseCase.getTransactions(deviceId)
                .map { transactions ->
                    Timber.d("Got Transactions: $transactions")
                    onTransactions.postValue(Resource.success(transactions))
                }
                .mapLeft {
                    handleFailure(it)
                }
        }
    }

    private fun handleFailure(failure: Failure) {
        onTransactions.postValue(
            Resource.error(
                resHelper.getString(
                    when (failure) {
                        is Failure.NetworkError -> R.string.network_error
                        else -> R.string.unknown_error
                    }
                )
            )
        )
    }
}
