package com.weatherxm.ui.photoverification.upload

import android.os.Bundle
import com.weatherxm.R
import com.weatherxm.data.models.PhotoPresignedMetadata
import com.weatherxm.databinding.ActivityPhotoUploadBinding
import com.weatherxm.service.GlobalUploadObserverService
import com.weatherxm.ui.common.Contracts
import com.weatherxm.ui.common.Contracts.ARG_DEVICE
import com.weatherxm.ui.common.StationPhoto
import com.weatherxm.ui.common.Status
import com.weatherxm.ui.common.UIDevice
import com.weatherxm.ui.common.parcelable
import com.weatherxm.ui.common.visible
import com.weatherxm.ui.components.BaseActivity
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf

class PhotoUploadActivity : BaseActivity() {
    private val uploadObserverService: GlobalUploadObserverService by inject()

    private lateinit var binding: ActivityPhotoUploadBinding

    private val model: PhotoUploadViewModel by viewModel {
        val photoLocalPaths = intent.getStringArrayListExtra(Contracts.ARG_PHOTOS) ?: arrayListOf()
        parametersOf(
            intent.parcelable<UIDevice>(ARG_DEVICE) ?: UIDevice.empty(),
            photoLocalPaths.map { StationPhoto(null, it) }
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPhotoUploadBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.shareBtn.setOnClickListener {
            navigator.openShareImages(this, model.getUrisOfLocalPhotos(this))
        }

        binding.continueBtn.setOnClickListener {
            finish()
        }

        model.onPhotosPresignedMetadata().observe(this) { resource ->
            when (resource.status) {
                Status.SUCCESS -> {
                    resource.data?.let {
                        upload(it)
                    }
                    binding.status
                        .clear()
                        .animation(R.raw.anim_upload_success)
                        .title(R.string.upload_started)
                        .subtitle(R.string.upload_started_subtitle)
                    binding.shareBtn.visible(true)
                    binding.continueBtn.visible(true)
                }
                Status.ERROR -> {
                    binding.status
                        .clear()
                        .animation(R.raw.anim_error)
                        .title(R.string.upload_failed_to_start)
                        .subtitle(resource.message)
                        .action(getString(R.string.action_retry_upload))
                        .setOnClickListener { model.prepareUpload() }
                }
                Status.LOADING -> {
                    // Do nothing. Already in loading state.
                }
            }
        }

        model.prepareUpload()
    }

    fun upload(photosPresignedMetadata: List<PhotoPresignedMetadata>) {
        uploadObserverService.setDevice(model.device)
        model.photos.forEachIndexed { index, stationPhoto ->
            photosPresignedMetadata.getOrNull(index)?.let {
                prepareAndStartUpload(this, stationPhoto.localPath, model.device.id, index, it)
            }
        }
    }
}
