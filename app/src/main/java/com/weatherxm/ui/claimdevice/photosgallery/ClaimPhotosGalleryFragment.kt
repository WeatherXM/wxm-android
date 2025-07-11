package com.weatherxm.ui.claimdevice.photosgallery

import android.Manifest.permission.CAMERA
import android.annotation.SuppressLint
import android.app.Activity.RESULT_OK
import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.dimensionResource
import coil3.load
import com.google.firebase.analytics.FirebaseAnalytics
import com.weatherxm.R
import com.weatherxm.analytics.AnalyticsService
import com.weatherxm.databinding.FragmentClaimPhotosGalleryBinding
import com.weatherxm.ui.claimdevice.helium.ClaimHeliumViewModel
import com.weatherxm.ui.claimdevice.pulse.ClaimPulseViewModel
import com.weatherxm.ui.claimdevice.wifi.ClaimWifiViewModel
import com.weatherxm.ui.common.Contracts.ARG_DEVICE_TYPE
import com.weatherxm.ui.common.DeviceType
import com.weatherxm.ui.common.PhotoSource
import com.weatherxm.ui.common.StationPhoto
import com.weatherxm.ui.common.UIDevice
import com.weatherxm.ui.common.disable
import com.weatherxm.ui.common.empty
import com.weatherxm.ui.common.enable
import com.weatherxm.ui.common.parcelable
import com.weatherxm.ui.common.setHtml
import com.weatherxm.ui.common.visible
import com.weatherxm.ui.components.BaseFragment
import com.weatherxm.ui.photoverification.gallery.Thumbnail
import com.weatherxm.util.hasPermission
import kotlinx.io.files.FileNotFoundException
import org.koin.androidx.viewmodel.ext.android.activityViewModel
import timber.log.Timber
import java.io.File

class ClaimPhotosGalleryFragment : BaseFragment() {
    companion object {
        const val MIN_PHOTOS = 2
        const val MAX_PHOTOS = 6

        fun newInstance(deviceType: DeviceType) = ClaimPhotosGalleryFragment().apply {
            arguments = Bundle().apply { putParcelable(ARG_DEVICE_TYPE, deviceType) }
        }
    }

    private val model: ClaimPhotosGalleryViewModel by activityViewModel()
    private val heliumParentModel: ClaimHeliumViewModel by activityViewModel()
    private val wifiParentModel: ClaimWifiViewModel by activityViewModel()
    private val pulseParentModel: ClaimPulseViewModel by activityViewModel()
    private lateinit var binding: FragmentClaimPhotosGalleryBinding

    private val cameraLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == RESULT_OK) {
                analytics.trackEventUserAction(
                    AnalyticsService.ParamValue.ADD_STATION_PHOTO.paramValue,
                    null,
                    customParams = arrayOf(
                        Pair(
                            AnalyticsService.CustomParam.ACTION.paramName,
                            AnalyticsService.ParamValue.COMPLETED.paramValue
                        ),
                        Pair(
                            FirebaseAnalytics.Param.SOURCE,
                            AnalyticsService.ParamValue.CAMERA.paramValue
                        )
                    )
                )
                model.addPhoto(latestPhotoTakenPath, PhotoSource.CAMERA)
            }
        }

    private val photoPickerLauncher =
        registerForActivityResult(
            ActivityResultContracts.PickVisualMedia()
        ) { uri ->
            uri?.let {
                analytics.trackEventUserAction(
                    AnalyticsService.ParamValue.ADD_STATION_PHOTO.paramValue,
                    null,
                    customParams = arrayOf(
                        Pair(
                            AnalyticsService.CustomParam.ACTION.paramName,
                            AnalyticsService.ParamValue.COMPLETED.paramValue
                        ),
                        Pair(
                            FirebaseAnalytics.Param.SOURCE,
                            AnalyticsService.ParamValue.GALLERY.paramValue
                        )
                    )
                )

                val file = createPhotoFile()
                try {
                    context?.contentResolver?.openInputStream(it)?.use { inputStream ->
                        file.outputStream().use { outputStream ->
                            inputStream.copyTo(outputStream)
                        }
                        // Save the file path to the model
                        model.addPhoto(file.absolutePath, PhotoSource.GALLERY)
                    }
                } catch (e: FileNotFoundException) {
                    Timber.d(e, "Could not copy file")
                }
            }
        }

    private var deviceType: DeviceType? = null
    private var wentToSettingsForPermissions = false
    private var latestPhotoTakenPath: String = String.empty()
    private var selectedPhoto: MutableState<StationPhoto?> = mutableStateOf(null)

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentClaimPhotosGalleryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        deviceType = arguments?.parcelable<DeviceType>(ARG_DEVICE_TYPE)
        if (context == null || deviceType == null) {
            // No point executing if in the meanwhile the activity is dead
            return
        }

        binding.emptyPhotosText.setHtml(R.string.tap_plus_icon)

        @Suppress("UseCheckOrError")
        binding.uploadAndClaimBtn.setOnClickListener {
            when (deviceType) {
                DeviceType.M5_WIFI, DeviceType.D1_WIFI -> wifiParentModel.next()
                DeviceType.PULSE_4G -> pulseParentModel.next()
                DeviceType.HELIUM -> heliumParentModel.next()
                null -> throw IllegalStateException("Device type is null")
            }
        }

        binding.instructionsBtn.setOnClickListener {
            navigator.showPhotoVerificationIntro(context, UIDevice.empty(), instructionsOnly = true)
        }

        binding.openSettingsBtn.setOnClickListener {
            navigator.openAppSettings(context)
            wentToSettingsForPermissions = true
        }

        binding.deletePhotoBtn.setOnClickListener {
            onDeletePhoto()
        }

        binding.thumbnails.setContent {
            Thumbnails()
        }

        model.onRequestCameraPermission().observe(viewLifecycleOwner) {
            if (it && model.onPhotos.isEmpty()) {
                getCameraPermissions()
            }
        }
    }

    override fun onResume() {
        if (wentToSettingsForPermissions && context?.hasPermission(CAMERA) == true) {
            onPermissionsGivenFromSettings()
        }
        super.onResume()
    }

    private fun onPhotosNumber(photosNumber: Int) {
        binding.uploadAndClaimBtn.isEnabled = photosNumber >= MIN_PHOTOS
        if (photosNumber > 0) {
            binding.deletePhotoBtn.enable()
        } else {
            binding.deletePhotoBtn.disable()
        }
        if (photosNumber == MAX_PHOTOS) {
            binding.addPhotoBtn.setOnClickListener {
                showSnackbarMessage(binding.root, getString(R.string.max_photos_reached_message))
            }

            binding.galleryBtn.setOnClickListener {
                showSnackbarMessage(binding.root, getString(R.string.max_photos_reached_message))
            }
        } else {
            binding.addPhotoBtn.setOnClickListener {
                analytics.trackEventUserAction(
                    AnalyticsService.ParamValue.ADD_STATION_PHOTO.paramValue,
                    null,
                    customParams = arrayOf(
                        Pair(
                            AnalyticsService.CustomParam.ACTION.paramName,
                            AnalyticsService.ParamValue.STARTED.paramValue
                        ),
                        Pair(
                            FirebaseAnalytics.Param.SOURCE,
                            AnalyticsService.ParamValue.CAMERA.paramValue
                        )
                    )
                )
                getCameraPermissions()
            }

            binding.galleryBtn.setOnClickListener {
                analytics.trackEventUserAction(
                    AnalyticsService.ParamValue.ADD_STATION_PHOTO.paramValue,
                    null,
                    customParams = arrayOf(
                        Pair(
                            AnalyticsService.CustomParam.ACTION.paramName,
                            AnalyticsService.ParamValue.STARTED.paramValue
                        ),
                        Pair(
                            FirebaseAnalytics.Param.SOURCE,
                            AnalyticsService.ParamValue.GALLERY.paramValue
                        )
                    )
                )
                navigator.openPhotoPicker(photoPickerLauncher)
            }
        }
    }

    private fun onDeletePhoto() {
        selectedPhoto.value?.let {
            analytics.trackEventUserAction(
                AnalyticsService.ParamValue.REMOVE_STATION_PHOTO.paramValue,
                contentType = null,
                Pair(
                    FirebaseAnalytics.Param.ITEM_ID,
                    AnalyticsService.ParamValue.LOCAL.paramValue
                )
            )
            model.deletePhoto(it)
        }
    }

    @Suppress("MagicNumber")
    private fun onCameraDenied() {
        if (model.onPhotos.isEmpty()) {
            binding.deletePhotoBtn.disable()
            binding.instructionsBtn.alpha = 0.4F
            binding.instructionsBtn.isEnabled = false
            binding.addPhotoBtn.disable()
            binding.emptyPhotosText.visible(false)
            binding.permissionsContainer.visible(true)
        }
    }

    private fun onPermissionsGivenFromSettings() {
        binding.instructionsBtn.alpha = 1.0F
        binding.instructionsBtn.isEnabled = true
        binding.addPhotoBtn.enable()
        binding.permissionsContainer.visible(false)
        if (model.onPhotos.isEmpty()) {
            binding.emptyPhotosText.visible(true)
            binding.deletePhotoBtn.disable()
        }
        wentToSettingsForPermissions = false
    }

    /**
     * `fromOnResume` helps us identify along with the `wentToSettingsForPermissions` if the user
     * came back from the settings screen or not, in order not to open the camera on every
     * `onResume` invocation but only on the `onCreate` one.
     */
    @SuppressLint("InlinedApi")
    private fun getCameraPermissions() {
        /**
         * Location-related permissions are also needed to be granted beforehand in order
         * to have them in EXIF Metadata.
         */
        requestCameraPermissions(
            activity,
            { navigator.openCamera(cameraLauncher, requireActivity(), createPhotoFile()) },
            { onCameraDenied() }
        )
    }

    fun createPhotoFile(): File {
        val storageDir: File? = activity?.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(getPhotoPrefix(), ".jpg", storageDir).apply {
            latestPhotoTakenPath = absolutePath
        }
    }

    @Suppress("UseCheckOrError")
    private fun getPhotoPrefix(): String {
        return when (deviceType) {
            DeviceType.M5_WIFI, DeviceType.D1_WIFI -> wifiParentModel.getSerialNumber()
            DeviceType.PULSE_4G -> pulseParentModel.getSerialNumber()
            DeviceType.HELIUM -> DeviceType.HELIUM.name
            null -> throw IllegalStateException("Device type is null")
        }
    }

    @Suppress("FunctionNaming")
    @Composable
    fun Thumbnails() {
        val photos = remember { model.onPhotos }

        onPhotosNumber(photos.size)
        binding.emptyPhotosText.visible(photos.isEmpty())
        binding.selectedPhoto.visible(photos.isNotEmpty())
        selectedPhoto.value = photos.lastOrNull()?.apply {
            binding.selectedPhoto.load(remotePath ?: localPath)
        }

        LazyRow(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.margin_small))
        ) {
            items(photos) { photo ->
                Thumbnail(
                    item = photo,
                    isSelected = photo == selectedPhoto.value,
                    onClick = {
                        binding.selectedPhoto.load(photo.remotePath ?: photo.localPath)
                        selectedPhoto.value = photo
                    }
                )
            }
        }
    }
}
