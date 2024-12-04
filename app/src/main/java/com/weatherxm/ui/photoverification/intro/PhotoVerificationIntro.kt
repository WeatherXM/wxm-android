package com.weatherxm.ui.photoverification.intro

import android.annotation.SuppressLint
import android.os.Bundle
import com.weatherxm.R
import com.weatherxm.databinding.ActivityPhotoVerificationIntroBinding
import com.weatherxm.ui.components.ActionDialogFragment
import com.weatherxm.ui.components.BaseActivity
import com.weatherxm.ui.photoverification.PhotoVerificationInstructionsFragment

class PhotoVerificationIntro : BaseActivity() {
    private lateinit var binding: ActivityPhotoVerificationIntroBinding

    @SuppressLint("MissingPermission")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPhotoVerificationIntroBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }

    override fun onResume() {
        super.onResume()

        binding.fragmentView.getFragment<PhotoVerificationInstructionsFragment>().onIntroScreen(
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
                // TODO: Navigate to next screen
            }
        )
    }
}
