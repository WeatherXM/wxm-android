package com.weatherxm.ui.startup

import android.content.Intent
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.weatherxm.data.models.RemoteMessageType
import com.weatherxm.data.models.WXMRemoteMessage
import com.weatherxm.ui.common.Contracts
import com.weatherxm.ui.common.Contracts.ARG_DEVICE_ID
import com.weatherxm.ui.common.Contracts.ARG_URL
import com.weatherxm.ui.common.empty
import com.weatherxm.usecases.StartupUseCase
import kotlinx.coroutines.launch

class StartupViewModel(private val startupUseCase: StartupUseCase) : ViewModel() {

    private val onStartupState = MutableLiveData<StartupState>()

    fun startup(): LiveData<StartupState> = onStartupState

    fun handleStartup(intent: Intent) {
        val type = if (intent.hasExtra(Contracts.ARG_TYPE)) {
            RemoteMessageType.parse(intent.getStringExtra(Contracts.ARG_TYPE) ?: String.empty())
        } else {
            defaultStartup()
            return
        }

        when (type) {
            RemoteMessageType.ANNOUNCEMENT -> {
                onStartupState.postValue(
                    StartupState.ShowDeepLinkRouter(
                        WXMRemoteMessage(type, url = intent.getStringExtra(ARG_URL))
                    )
                )
            }
            RemoteMessageType.STATION -> {
                onStartupState.postValue(
                    StartupState.ShowDeepLinkRouter(
                        WXMRemoteMessage(type, deviceId = intent.getStringExtra(ARG_DEVICE_ID))
                    )
                )
            }
            else -> defaultStartup()
        }
    }

    private fun defaultStartup() {
        viewModelScope.launch {
            startupUseCase.getStartupState().collect {
                onStartupState.postValue(it)
            }
        }
    }
}
