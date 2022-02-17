package com.weatherxm.ui.signup

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.weatherxm.R
import com.weatherxm.data.Resource
import com.weatherxm.usecases.AuthUseCase
import com.weatherxm.util.ResourcesHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber

class SignupViewModel : ViewModel(), KoinComponent {

    private val authUseCase: AuthUseCase by inject()
    private val resHelper: ResourcesHelper by inject()

    private val isSignedUp = MutableLiveData<Resource<String>>()
    fun isSignedUp() = isSignedUp

    fun signup(username: String, firstName: String?, lastName: String?) {
        isSignedUp.postValue(Resource.loading())
        CoroutineScope(Dispatchers.IO).launch {
            val first = if (firstName.isNullOrEmpty()) null else firstName
            val last = if (lastName.isNullOrEmpty()) null else lastName
            authUseCase.signup(username, first, last)
                .mapLeft {
                    Timber.d(it, "Signup Error")
                    isSignedUp.postValue(Resource.error("Signup Error. ${it.message}"))
                }.map {
                    isSignedUp.postValue(
                        Resource.success(resHelper.getString(R.string.success_signup_text, it))
                    )
                }
        }
    }
}
