package com.weatherxm.ui.photoverification.intro

import android.annotation.SuppressLint
import android.os.Bundle
import com.weatherxm.R
import com.weatherxm.databinding.ActivityPhotoVerificationIntroBinding
import com.weatherxm.ui.common.Contracts.ARG_DEVICE
import com.weatherxm.ui.common.Contracts.ARG_INSTRUCTIONS_ONLY
import com.weatherxm.ui.common.UIDevice
import com.weatherxm.ui.common.parcelable
import com.weatherxm.ui.components.ActionDialogFragment
import com.weatherxm.ui.components.BaseActivity
import com.weatherxm.ui.photoverification.PhotoVerificationInstructionsFragment

class PhotoVerificationIntroActivity : BaseActivity() {
    private lateinit var binding: ActivityPhotoVerificationIntroBinding

    private var instructionsOnly = false
    private var device = UIDevice.empty()

    @SuppressLint("MissingPermission")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPhotoVerificationIntroBinding.inflate(layoutInflater)
        setContentView(binding.root)

        instructionsOnly = intent.getBooleanExtra(ARG_INSTRUCTIONS_ONLY, false)
        device = intent.parcelable<UIDevice>(ARG_DEVICE) ?: UIDevice.empty()
    }

    override fun onResume() {
        super.onResume()

        with(binding.fragmentView.getFragment<PhotoVerificationInstructionsFragment>()) {
            if (instructionsOnly) {
                onInstructionScreen {
                    finish()
                }
            } else {
                onIntroScreen(
                    onClose = {
                        ActionDialogFragment
                            .Builder(
                                title = getString(R.string.exit_photo_verification),
                                message = getString(R.string.exit_photo_verification_message),
                                negative = getString(R.string.action_back)
                            )
                            .onPositiveClick(getString(R.string.action_exit)) { finish() }
                            .build()
                            .show(this)
                    },
                    onTakePhoto = {
                        navigator.showPhotoGallery(
                            this@PhotoVerificationIntroActivity,
                            device,
                            arrayListOf(),
                            true
                        )
                        finish()
                    }
                )
            }
        }
    }
}
