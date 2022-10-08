package com.weatherxm.ui.claimdevice.helium.verify

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanIntentResult
import com.journeyapps.barcodescanner.ScanOptions
import com.weatherxm.R
import com.weatherxm.databinding.FragmentClaimDeviceHeliumVerifyBinding
import com.weatherxm.ui.claimdevice.helium.ClaimHeliumDeviceViewModel
import com.weatherxm.util.onTextChanged

class ClaimHeliumDeviceVerifyFragment : Fragment() {
    private val parentModel: ClaimHeliumDeviceViewModel by activityViewModels()
    private val model: ClaimHeliumDeviceVerifyViewModel by viewModels()
    private lateinit var binding: FragmentClaimDeviceHeliumVerifyBinding

    // Register the launcher and result handler for QR code scanner
    private val barcodeLauncher =
        registerForActivityResult(ScanContract()) { result: ScanIntentResult ->
            result.contents.let {
                val scannedEUI = model.getEUIFromScanner(it)
                val scannedKey = model.getKeyFromScanner(it)
                binding.devKey.setText(scannedKey)
                binding.devEUI.setText(scannedEUI)
                parentModel.setDeviceKey(scannedKey)
                parentModel.setDeviceEUI(scannedEUI)
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentClaimDeviceHeliumVerifyBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.devEUI.onTextChanged {
            binding.devEUIContainer.error = null
        }

        binding.devKey.onTextChanged {
            binding.devKeyContainer.error = null
        }

        binding.scan.setOnClickListener {
            barcodeLauncher.launch(ScanOptions().setBeepEnabled(false))
        }

        binding.cancel.setOnClickListener {
            parentModel.cancel()
        }

        binding.verify.setOnClickListener {
            model.checkAndVerify(
                binding.devEUI.text.toString().trim(),
                binding.devKey.text.toString().trim()
            )
            // TODO: Remove this
            parentModel.next()
        }

        model.onDevEUIError().observe(viewLifecycleOwner) {
            if (it) {
                binding.devEUIContainer.error = getString(R.string.invalid_dev_eui)
            }
        }

        model.onDevKeyError().observe(viewLifecycleOwner) {
            if (it) {
                binding.devKeyContainer.error = getString(R.string.invalid_dev_key)
            }
        }

        model.onVerifyError().observe(viewLifecycleOwner) {
            if (it) {
                binding.errorCard.htmlMessage(R.string.wrong_combination_message)
                binding.errorCard.show()
            }
        }
    }
}
