package com.weatherxm.ui.photoverification.gallery

import android.Manifest.permission.CAMERA
import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.provider.MediaStore
import androidx.activity.result.contract.ActivityResultContracts
import com.weatherxm.R
import com.weatherxm.databinding.ActivityPhotoGalleryBinding
import com.weatherxm.ui.common.Contracts
import com.weatherxm.ui.common.Contracts.ARG_FROM_CLAIMING
import com.weatherxm.ui.common.disable
import com.weatherxm.ui.common.enable
import com.weatherxm.ui.common.setHtml
import com.weatherxm.ui.common.visible
import com.weatherxm.ui.components.ActionDialogFragment
import com.weatherxm.ui.components.BaseActivity
import com.weatherxm.util.checkPermissionsAndThen
import com.weatherxm.util.hasPermission
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf

class PhotoGalleryActivity : BaseActivity() {
    private lateinit var binding: ActivityPhotoGalleryBinding

    private val model: PhotoGalleryViewModel by viewModel {
        parametersOf(
            intent.getStringArrayListExtra(Contracts.ARG_PHOTOS),
            intent.getBooleanExtra(ARG_FROM_CLAIMING, false)
        )
    }

    private val cameraLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == RESULT_OK) {
                // TODO: Will be filled with the uploading mechanism
            }
        }

    private var wentToSettingsForPermissions = false

    @SuppressLint("MissingPermission")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPhotoGalleryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.emptyPhotosText.setHtml(R.string.tap_plus_icon)

        if (model.currentPhotosList.isEmpty()) {
            binding.toolbar.subtitle = getString(R.string.add_2_more_to_upload)
            binding.emptyPhotosText.visible(true)
            binding.deletePhotoBtn.disable()
        } else if (model.currentPhotosList.size == 1) {
            binding.toolbar.subtitle = getString(R.string.add_1_more_to_upload)
        } else {
            binding.uploadBtn.isEnabled = true
        }

        if (model.fromClaiming) {
            binding.toolbar.setNavigationIcon(R.drawable.ic_close)
            binding.toolbar.setNavigationOnClickListener {
                ActionDialogFragment
                    .Builder(
                        title = getString(R.string.exit_photo_verification),
                        message = getString(R.string.exit_photo_verification_message),
                        negative = getString(R.string.action_back)
                    )
                    .onPositiveClick(getString(R.string.action_exit)) {
                        finish()
                    }
                    .build()
                    .show(this)
            }
        } else {
            binding.toolbar.setNavigationIcon(R.drawable.ic_back)
            binding.toolbar.setNavigationOnClickListener {
                // TODO: Add a check if less than 2 photos are here
                finish()
            }
        }

        binding.instructionsBtn.setOnClickListener {
            navigator.showPhotoVerificationIntro(this, true)
        }

        binding.openSettingsBtn.setOnClickListener {
            navigator.openAppSettings(this)
            wentToSettingsForPermissions = true
        }

        binding.addPhotoBtn.setOnClickListener {
            getCameraPermissions()
        }

        getCameraPermissions()
    }

    override fun onResume() {
        if (wentToSettingsForPermissions && hasPermission(CAMERA)) {
            onPermissionsGivenFromSettings()
        }
        super.onResume()
    }

    private fun onCameraDenied() {
        binding.deletePhotoBtn.disable()
        binding.instructionsBtn.alpha = 0.4F
        binding.instructionsBtn.isEnabled = false
        binding.addPhotoBtn.disable()
        binding.emptyPhotosText.visible(false)
        binding.permissionsContainer.visible(true)
    }

    private fun onPermissionsGivenFromSettings() {
        binding.instructionsBtn.alpha = 1.0F
        binding.instructionsBtn.isEnabled = true
        binding.addPhotoBtn.enable()
        binding.permissionsContainer.visible(false)
        if (model.currentPhotosList.isEmpty()) {
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
        checkPermissionsAndThen(
            permissions = arrayOf(CAMERA),
            rationaleTitle = getString(R.string.camera_permission_required_title),
            rationaleMessage = getString(R.string.camera_permission_required),
            onGranted = { openCamera() },
            onDenied = { onCameraDenied() },
            showOnPermanentlyDenied = false
        )
    }

    private fun openCamera() {
        Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { takePictureIntent ->
            // TODO: Create the image file in order to save it
            cameraLauncher.launch(takePictureIntent)
        }
    }
}
