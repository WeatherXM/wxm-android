package com.weatherxm.ui.devicesettings

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import coil.load
import com.weatherxm.data.models.DevicePhoto
import com.weatherxm.databinding.ViewStationSettingsDevicePhotosBinding
import com.weatherxm.ui.common.visible
import org.koin.core.component.KoinComponent

open class DevicePhotosView : LinearLayout, KoinComponent {

    private lateinit var binding: ViewStationSettingsDevicePhotosBinding

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

    fun setOnClickListener(onClick: () -> Unit) {
        binding.startPhotoVerificationBtn.setOnClickListener {
            onClick()
        }
        binding.firstPhotoContainer.setOnClickListener {
            onClick()
        }
        binding.secondPhotoContainer.setOnClickListener {
            onClick()
        }
    }

    fun onEmpty() {
        binding.emptyText.visible(true)
        binding.startPhotoVerificationBtn.visible(true)
    }

    fun onPhotos(devicePhotos: List<DevicePhoto>) {
        binding.emptyText.visible(false)
        binding.startPhotoVerificationBtn.visible(false)
        binding.photosText.visible(true)

        if (devicePhotos.size >= 2) {
            binding.firstPhoto.load(devicePhotos[0].url)
            binding.secondPhoto.load(devicePhotos[1].url)
            binding.photosContainer.visible(true)
            binding.translucentViewOnSecondPhoto.visible(devicePhotos.size > 2)
        }
    }
}
