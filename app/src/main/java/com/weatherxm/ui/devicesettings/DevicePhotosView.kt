package com.weatherxm.ui.devicesettings

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import androidx.compose.runtime.collectAsState
import coil.load
import com.weatherxm.data.models.DevicePhoto
import com.weatherxm.databinding.ViewStationSettingsDevicePhotosBinding
import com.weatherxm.service.GlobalUploadObserverService
import com.weatherxm.ui.common.visible
import com.weatherxm.ui.components.PhotoUploadState
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

open class DevicePhotosView : LinearLayout, KoinComponent {

    private lateinit var binding: ViewStationSettingsDevicePhotosBinding
    private val uploadObserverService: GlobalUploadObserverService by inject()

    constructor(context: Context?) : super(context) {
        init(context)
    }

    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) {
        init(context)
    }

    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        init(context)
    }

    private fun init(context: Context?) {
        binding = ViewStationSettingsDevicePhotosBinding.inflate(LayoutInflater.from(context), this)
        orientation = VERTICAL
    }

    fun initProgressView(onError: () -> Unit, onSuccess: () -> Unit) {
        binding.inProgressUploadState.setContent {
            uploadObserverService.getUploadPhotosState().collectAsState(null).value?.let {
                binding.photosText.visible(false)
                binding.photosContainer.visible(false)
                binding.emptyText.visible(false)
                binding.startPhotoVerificationBtn.visible(false)
                binding.cancelUploadBtn.visible(!it.isSuccess && it.error == null)
                binding.inProgressText.visible(true)
                binding.inProgressUploadState.visible(true)

                if (it.error != null) {
                    binding.inProgressText.visible(false)
                    binding.inProgressUploadState.visible(false)
                    onError()
                    // TODO: Show error card here after onError is implemented
                } else {
                    // TODO: Hide error card
                }
                if (it.isSuccess) {
                    onSuccess()
                }
                PhotoUploadState(it, false)
            }
        }
    }

    fun setOnClickListener(onClick: () -> Unit, onCancelUpload: () -> Unit) {
        binding.startPhotoVerificationBtn.setOnClickListener {
            onClick()
        }
        binding.firstPhotoContainer.setOnClickListener {
            onClick()
        }
        binding.secondPhotoContainer.setOnClickListener {
            onClick()
        }
        binding.cancelUploadBtn.setOnClickListener {
            onCancelUpload()
        }
    }

    fun updateUI(devicePhotos: List<DevicePhoto>) {
        if (devicePhotos.isEmpty()) {
            onEmpty()
        } else {
            onPhotos(devicePhotos)
        }
    }

    private fun onEmpty() {
        binding.inProgressText.visible(false)
        binding.inProgressUploadState.visible(false)
        binding.emptyText.visible(true)
        binding.startPhotoVerificationBtn.visible(true)
    }

    private fun onPhotos(devicePhotos: List<DevicePhoto>) {
        binding.emptyText.visible(false)
        binding.startPhotoVerificationBtn.visible(false)
        binding.inProgressText.visible(false)
        binding.inProgressUploadState.visible(false)
        binding.photosText.visible(true)

        if (devicePhotos.size >= 2) {
            binding.firstPhoto.load(devicePhotos[0].url)
            binding.secondPhoto.load(devicePhotos[1].url)
            binding.photosContainer.visible(true)
            binding.translucentViewOnSecondPhoto.visible(devicePhotos.size > 2)
        }
    }
}
