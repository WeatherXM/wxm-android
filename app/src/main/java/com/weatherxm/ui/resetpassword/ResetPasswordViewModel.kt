package com.weatherxm.ui.resetpassword

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.weatherxm.R
import com.weatherxm.data.Failure
import com.weatherxm.data.Resource
import com.weatherxm.data.ServerError
import com.weatherxm.data.repository.AuthRepository
import com.weatherxm.util.ResourcesHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber

class ResetPasswordViewModel : ViewModel(), KoinComponent {

    private val repository: AuthRepository by inject()
    private val resHelper: ResourcesHelper by inject()

    private val isEmailSent = MutableLiveData<Resource<Unit>>()
    fun isEmailSent() = isEmailSent

    fun resetPassword(email: String) {
        isEmailSent.postValue(Resource.loading())
        CoroutineScope(Dispatchers.IO).launch {
            repository.resetPassword(email)
                .mapLeft {
                    Timber.d("Got error: $it")
                    when (it) {
                        is Failure.NetworkError -> isEmailSent.postValue(
                            Resource.error(resHelper.getString(R.string.network_error))
                        )
                        is ServerError -> isEmailSent.postValue(
                            Resource.error(resHelper.getString(R.string.server_error))
                        )
                        is Failure.UnknownError -> isEmailSent.postValue(
                            Resource.error(resHelper.getString(R.string.unknown_error))
                        )
                        else -> isEmailSent.postValue(
                            Resource.error(resHelper.getString(R.string.unknown_error))
                        )
                    }
                }
                .map {
                    isEmailSent.postValue(Resource.success(Unit))
                }
        }
    }
}
