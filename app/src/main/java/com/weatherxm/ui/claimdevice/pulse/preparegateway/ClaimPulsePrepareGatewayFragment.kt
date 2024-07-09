package com.weatherxm.ui.claimdevice.pulse.preparegateway

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.mlkit.vision.codescanner.GmsBarcodeScanner
import com.weatherxm.R
import com.weatherxm.databinding.FragmentClaimPulsePrepareClaimingBinding
import com.weatherxm.ui.claimdevice.pulse.ClaimPulseViewModel
import com.weatherxm.ui.common.empty
import com.weatherxm.ui.common.setHtml
import com.weatherxm.ui.components.BaseFragment
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.activityViewModel
import timber.log.Timber

class ClaimPulsePrepareGatewayFragment : BaseFragment() {
    private val model: ClaimPulseViewModel by activityViewModel()
    private val scanner: GmsBarcodeScanner by inject()
    private lateinit var binding: FragmentClaimPulsePrepareClaimingBinding

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
            scanBarcode()
        }

        return binding.root
    }

    private fun scanBarcode() {
        scanner.startScan()
            .addOnSuccessListener { barcode ->
                val scannedInfo = barcode.rawValue?.removePrefix("P") ?: String.empty()
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
            .addOnFailureListener { e ->
                Timber.e(e, "Failure when scanning Barcode of the device")
                showSnackbarMessage(
                    binding.root,
                    getString(R.string.error_connect_wallet_scan_exception, e.message),
                    callback = { snackbar?.dismiss() },
                    R.string.action_dismiss,
                    binding.buttonBar
                )
            }
    }
}
