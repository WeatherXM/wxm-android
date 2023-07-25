package com.weatherxm.ui.cellinfo

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.weatherxm.R
import com.weatherxm.data.Resource
import com.weatherxm.ui.common.UIDevice
import com.weatherxm.ui.explorer.UICell
import com.weatherxm.usecases.ExplorerUseCase
import com.weatherxm.util.Analytics
import com.weatherxm.util.ResourcesHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber

class CellInfoViewModel(val cell: UICell) : ViewModel(), KoinComponent {
    private val resHelper: ResourcesHelper by inject()
    private val explorerUseCase: ExplorerUseCase by inject()
    private val analytics: Analytics by inject()
    private val onCellDevices = MutableLiveData<Resource<List<UIDevice>>>(Resource.loading())
    private val address = MutableLiveData<String>()

    fun onCellDevices(): LiveData<Resource<List<UIDevice>>> = onCellDevices
    fun address(): LiveData<String> = address

    fun fetchDevices() {
        onCellDevices.postValue(Resource.loading())
        viewModelScope.launch(Dispatchers.IO) {
            if (cell.index.isEmpty()) {
                Timber.w("Getting cell devices failed: cell index = null")
                onCellDevices.postValue(
                    Resource.error(resHelper.getString(R.string.error_cell_devices_no_data))
                )
                return@launch
            }

            explorerUseCase.getCellDevices(cell)
                .map {
                    onCellDevices.postValue(Resource.success(it))
                    it.forEach { device ->
                        device.address?.let { address ->
                            this@CellInfoViewModel.address.postValue(address)
                            return@forEach
                        }
                    }

                }
                .mapLeft {
                    analytics.trackEventFailure(it.code)
                    onCellDevices.postValue(
                        Resource.error(resHelper.getString(R.string.error_cell_devices_no_data))
                    )
                }
        }
    }
}
