package com.weatherxm.ui.home.profile

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.liveData
import androidx.lifecycle.viewModelScope
import com.weatherxm.R
import com.weatherxm.data.Failure
import com.weatherxm.data.Resource
import com.weatherxm.data.User
import com.weatherxm.usecases.UserUseCase
import com.weatherxm.util.ResourcesHelper
import kotlinx.coroutines.Dispatchers
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber

class ProfileViewModel : ViewModel(), KoinComponent {

    private val userUseCase: UserUseCase by inject()
    private val resHelper: ResourcesHelper by inject()

    fun user(): LiveData<Resource<User>> =
        liveData(viewModelScope.coroutineContext + Dispatchers.IO) {
            emit(Resource.loading())
            userUseCase.getUser()
                .map {
                    Timber.d("Got profile info: $it")
                    emit(Resource.success(it))
                }
                .mapLeft {
                    emit(
                        Resource.error(
                            resHelper.getString(
                                when (it) {
                                    is Failure.NetworkError -> R.string.network_error
                                    else -> R.string.unknown_error
                                }
                            )
                        )
                    )
                }
        }

    fun wallet(): LiveData<String?> = liveData(viewModelScope.coroutineContext + Dispatchers.IO) {
        userUseCase.getWalletAddress()
            .map {
                emit(it)
            }
    }
}
