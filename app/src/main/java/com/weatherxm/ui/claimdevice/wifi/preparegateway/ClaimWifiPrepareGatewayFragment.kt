package com.weatherxm.ui.claimdevice.wifi.preparegateway

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import coil.ImageLoader
import com.google.mlkit.vision.codescanner.GmsBarcodeScanner
import com.weatherxm.R
import com.weatherxm.databinding.FragmentClaimWifiPrepareClaimingBinding
import com.weatherxm.ui.claimdevice.wifi.ClaimWifiViewModel
import com.weatherxm.ui.common.DeviceType
import com.weatherxm.ui.common.empty
import com.weatherxm.ui.common.loadImage
import com.weatherxm.ui.common.setHtml
import com.weatherxm.ui.common.visible
import com.weatherxm.ui.common.toast
import com.weatherxm.ui.components.BaseFragment
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.activityViewModel
import timber.log.Timber

class ClaimWifiPrepareGatewayFragment : BaseFragment() {
    private val model: ClaimWifiViewModel by activityViewModel()
    private val imageLoader: ImageLoader by inject()
    private val scanner: GmsBarcodeScanner by inject()
    private lateinit var binding: FragmentClaimWifiPrepareClaimingBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentClaimWifiPrepareClaimingBinding.inflate(inflater, container, false)

        if (model.deviceType == DeviceType.M5_WIFI) {
            binding.firstStep.setHtml(R.string.prepare_gateway_m1_first_step)
            binding.m5Notice.setHtml(R.string.prepare_gateway_m5_notice)
            binding.guideGif.loadImage(imageLoader, R.raw.m5_claim)
        } else {
            binding.firstStep.setHtml(R.string.prepare_gateway_d1_first_step)
            binding.guideGif.loadImage(imageLoader, R.raw.d1_claim)
        }
        binding.secondStep.setHtml(R.string.prepare_gateway_wifi_second_step)
        binding.m5Notice.visible(model.deviceType == DeviceType.M5_WIFI)

        binding.enterManuallyBtn.setOnClickListener {
            model.next()
        }

        binding.scanBtn.setOnClickListener {
            scanQR()
        }

        return binding.root
    }

    private fun scanQR() {
        scanner.startScan()
            .addOnSuccessListener { barcode ->
                val scannedInfo = barcode.rawValue ?: String.empty()
                if (model.deviceType == DeviceType.M5_WIFI) {
                    handleM5QR(scannedInfo)
                } else {
                    handleD1QR(scannedInfo)
                }
            }
            .addOnFailureListener { e ->
                Timber.e(e, "Failure when scanning QR of the device")
                context?.toast(
                    R.string.error_connect_wallet_scan_exception,
                    e.message ?: String.empty(),
                    Toast.LENGTH_LONG
                )
            }
    }

    private fun handleM5QR(qrCode: String) {
        if (model.validateSerial(qrCode)) {
            model.setSerialNumber(qrCode)
            model.next(2)
        } else {
            showIncorrectQRMessage()
        }
    }

    private fun handleD1QR(qrCode: String) {
        if (!qrCode.contains(",")) {
            showIncorrectQRMessage()
            return
        }
        val (serialNumber, claimingKey) = qrCode.split(",")

        if (model.validateSerial(serialNumber) && model.validateClaimingKey(claimingKey)) {
            model.setSerialNumber(serialNumber)
            model.setClaimingKey(claimingKey)
            model.next(2)
        } else {
            showIncorrectQRMessage()
        }
    }

    private fun showIncorrectQRMessage() {
        showSnackbarMessage(binding.root, getString(R.string.prepare_gateway_invalid_qr_code))
    }
}
