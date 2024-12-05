package com.weatherxm.ui.photoverification.gallery

import android.Manifest.permission.CAMERA
import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
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
import java.io.File

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
                model.addPhoto(latestPhotoTakenPath)
            }
        }

    private var wentToSettingsForPermissions = false
    private var latestPhotoTakenPath: String = ""

    @SuppressLint("MissingPermission")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPhotoGalleryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.emptyPhotosText.setHtml(R.string.tap_plus_icon)

        model.onPhotoNumber().observe(this) {
            binding.addPhotoBtn.visible(it < 6)
            when (it) {
                0 -> {
                    binding.toolbar.subtitle = getString(R.string.add_2_more_to_upload)
                    binding.emptyPhotosText.visible(true)
                    binding.deletePhotoBtn.disable()
                }
                1 -> {
                    binding.toolbar.subtitle = getString(R.string.add_1_more_to_upload)
                    binding.deletePhotoBtn.enable()
                }
                else -> {
                    binding.toolbar.subtitle = null
                    binding.uploadBtn.isEnabled = true
                    binding.deletePhotoBtn.enable()
                }
            }
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
                ActionDialogFragment
                    .Builder(
                        title = getString(R.string.exit_photo_verification),
                        message = getString(R.string.exit_photo_verification_start_over),
                        negative = getString(R.string.action_back)
                    )
                    .onPositiveClick(getString(R.string.action_exit)) {
                        finish()
                    }
                    .build()
                    .show(this)
            }
        }

        binding.uploadBtn.setOnClickListener {
            ActionDialogFragment
                .Builder(
                    title = getString(R.string.upload_your_photos),
                    message = getString(R.string.upload_your_photos_dialog_message),
                    negative = getString(R.string.action_back)
                )
                .onPositiveClick(getString(R.string.action_upload)) {
                    // TODO: Will be filled with the uploading mechanism
                    finish()
                }
                .build()
                .show(this)
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

        binding.deletePhotoBtn.setOnClickListener {
            // TODO: Implement this. Also add a check if a photo is already uploaded to show the dialog
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

    private fun createPhotoFile(): File {
        val storageDir: File? = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        // TODO: Create name of image
        return File.createTempFile("JPEG_TEST_", ".jpg", storageDir).apply {
            latestPhotoTakenPath = absolutePath
        }
    }

    private fun openCamera() {
        Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { takePictureIntent ->
            val photoFile: File = createPhotoFile()
            takePictureIntent.putExtra(
                MediaStore.EXTRA_OUTPUT,
                FileProvider.getUriForFile(this, "com.weatherxm.app.fileprovider", photoFile)
            )
            cameraLauncher.launch(takePictureIntent)
        }
    }
}
