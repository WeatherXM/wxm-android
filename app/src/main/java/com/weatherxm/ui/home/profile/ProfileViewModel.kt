package com.weatherxm.ui.home.profile

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.weatherxm.R
import com.weatherxm.data.Resource
import com.weatherxm.data.User
import com.weatherxm.data.repository.UserRepository
import com.weatherxm.util.ResourcesHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber

class ProfileViewModel : ViewModel(), KoinComponent {

    private val userRepository: UserRepository by inject()
    private val resourcesHelper: ResourcesHelper by inject()

    private val user = MutableLiveData<Resource<User>>().apply {
        value = Resource.loading()
    }

    fun user(): LiveData<Resource<User>> = user

    fun fetch() {
        CoroutineScope(Dispatchers.IO).launch {
            userRepository.getUser()
                .map { user ->
                    Timber.d("Got user: $user")
                    this@ProfileViewModel.user.postValue(Resource.success(user))
                }
                .mapLeft {
                    Timber.d("Got error: $it")
                    user.postValue(
                        Resource.error(resourcesHelper.getString(R.string.user_info_error))
                    )
                }
        }
    }
}
