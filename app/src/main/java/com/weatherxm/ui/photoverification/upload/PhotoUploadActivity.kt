package com.weatherxm.ui.photoverification.upload

import android.graphics.BitmapFactory
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
import com.weatherxm.util.ImageFileHelper.compressImageFile
import com.weatherxm.util.ImageFileHelper.copyExifMetadata
import com.weatherxm.util.ImageFileHelper.copyInputStreamToFile
import net.gotev.uploadservice.protocols.multipart.MultipartUploadRequest
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf
import java.io.File

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
                    TODO()
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
                val file = File(cacheDir, "img$index.jpeg")
                val imageBitmap = BitmapFactory.decodeFile(stationPhoto.localPath)
                file.copyInputStreamToFile(compressImageFile(imageBitmap))
                copyExifMetadata(stationPhoto.localPath, file.path)

                val uploadRequest = MultipartUploadRequest(this, it.url)
                    .setMethod("POST")
                    .addParameter("bucket", it.fields.bucket)
                    .addParameter("X-Amz-Algorithm", it.fields.xAmzAlgo)
                    .addParameter("X-Amz-Credential", it.fields.xAmzCredentials)
                    .addParameter("X-Amz-Date", it.fields.xAmzDate)
                    //TODO: STOPSHIP .addParameter("X-Amz-Security-Token", it.fields.xAmzSecurityToken)
                    .addParameter("X-Amz-Signature", it.fields.xAmzSignature)
                    .addParameter("key", it.fields.key)
                    .addParameter("Policy", it.fields.policy)
                    .addFileToUpload(file.path, "file")
                uploadRequest.startUpload()
            }
        }
    }
}
