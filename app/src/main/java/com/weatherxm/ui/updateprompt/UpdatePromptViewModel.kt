package com.weatherxm.ui.updateprompt

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import arrow.core.Either
import com.weatherxm.data.Failure
import com.weatherxm.data.repository.AppConfigRepository
import com.weatherxm.data.repository.AuthRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber

class UpdatePromptViewModel : ViewModel(), KoinComponent {

    private val appConfigRepository: AppConfigRepository by inject()

    fun isUpdateMandatory(): Boolean {
        return appConfigRepository.isUpdateMandatory()
    }

    fun getChangelog(): String {
        return appConfigRepository.getChangelog()
    }
}
