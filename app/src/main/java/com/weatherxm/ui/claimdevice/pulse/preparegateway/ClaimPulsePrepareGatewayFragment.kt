package com.weatherxm.ui.claimdevice.pulse.preparegateway

import android.Manifest.permission.CAMERA
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import com.weatherxm.R
import com.weatherxm.databinding.FragmentClaimPulsePrepareClaimingBinding
import com.weatherxm.ui.claimdevice.pulse.ClaimPulseViewModel
import com.weatherxm.ui.common.setHtml
import com.weatherxm.ui.components.BaseFragment
import com.weatherxm.util.checkPermissionsAndThen
import org.koin.androidx.viewmodel.ext.android.activityViewModel

class ClaimPulsePrepareGatewayFragment : BaseFragment() {
    private val model: ClaimPulseViewModel by activityViewModel()
    private lateinit var binding: FragmentClaimPulsePrepareClaimingBinding

    // Register the launcher and result handler for QR code scanner
    private val barcodeLauncher = registerForActivityResult(ScanContract()) {
        if (!it.contents.isNullOrEmpty()) {
            println("[BARCODE SCAN RESULT]: $it")
            val scannedInfo = it.contents.removePrefix("P")
            if (model.validateSerial(scannedInfo)) {
                model.setSerialNumber(scannedInfo)
                model.next(2)
            } else {
                showSnackbarMessage(
                    binding.root,
                    getString(R.string.prepare_gateway_invalid_barcode),
                    callback = { snackbar?.dismiss() },
                    R.string.action_dismiss,
                    binding.buttonBar
                )
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentClaimPulsePrepareClaimingBinding.inflate(inflater, container, false)

        binding.firstStep.setHtml(R.string.prepare_gateway_pulse_first_step)
        binding.secondStep.setHtml(R.string.prepare_gateway_pulse_second_step)

        binding.enterManuallyBtn.setOnClickListener {
            model.next()
        }

        binding.scanBtn.setOnClickListener {
            activity?.checkPermissionsAndThen(
                permissions = arrayOf(CAMERA),
                rationaleTitle = getString(R.string.camera_permission_required_title),
                rationaleMessage = getString(R.string.camera_permission_required),
                onGranted = {
                    barcodeLauncher.launch(
                        ScanOptions()
                            .setDesiredBarcodeFormats(ScanOptions.CODE_128)
                            .setBeepEnabled(false)
                    )
                },
                onDenied = {
                    // Do nothing
                }
            )
        }

        return binding.root
    }
}
