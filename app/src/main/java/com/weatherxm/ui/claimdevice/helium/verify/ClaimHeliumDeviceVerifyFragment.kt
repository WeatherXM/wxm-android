package com.weatherxm.ui.claimdevice.helium.verify

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanIntentResult
import com.journeyapps.barcodescanner.ScanOptions
import com.weatherxm.R
import com.weatherxm.databinding.FragmentClaimDeviceHeliumVerifyBinding
import com.weatherxm.ui.UIError
import com.weatherxm.ui.claimdevice.helium.ClaimHeliumDeviceViewModel
import com.weatherxm.ui.common.ErrorDialogFragment

class ClaimHeliumDeviceVerifyFragment : Fragment() {
    private val parentModel: ClaimHeliumDeviceViewModel by activityViewModels()
    private val model: ClaimHeliumDeviceVerifyViewModel by viewModels()
    private lateinit var binding: FragmentClaimDeviceHeliumVerifyBinding

    // TODO: This will be used in the Update activity where the flow is TBD.
    private val findZipFileLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
            result.data?.data?.let {
                model.update(it)
            }
        }

    // Register the launcher and result handler for QR code scanner
    private val barcodeLauncher =
        registerForActivityResult(ScanContract()) { result: ScanIntentResult ->
            result.contents.let {
                model.parseScanResult(it)
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

        binding.scan.setOnClickListener {
            barcodeLauncher.launch(
                ScanOptions().setBeepEnabled(false)
            )
        }

        binding.cancel.setOnClickListener {
            parentModel.cancel()
        }

        binding.next.setOnClickListener {
            // TODO: Handle verification status and go next
        }

        if (!parentModel.isManualClaiming()) {
            binding.devKeyTitle.visibility = View.GONE
            binding.devKeyContainer.visibility = View.GONE

            connectToDevice()
        }

        model.onError().observe(viewLifecycleOwner) {
            showErrorDialog(it)
        }
    }

    private fun showErrorDialog(uiError: UIError) {
        ErrorDialogFragment
            .Builder(
                title = getString(R.string.pairing_failed_title),
                message = uiError.errorMessage
            )
            .onNegativeClick(getString(R.string.action_quit_claiming)) {
                parentModel.cancel()
            }
            .onPositiveClick(getString(R.string.action_try_again)) {
                uiError.retryFunction
            }
            .build()
            .show(this)
    }

    private fun connectToDevice() {
        model.setPeripheral(parentModel.getDeviceAddress())
    }

//        TODO: For testing purposes. Remove on PR.
//        model.onBondedDevice().observe(this) {
//            val intent = Intent(Intent.ACTION_GET_CONTENT).addCategory(Intent.CATEGORY_OPENABLE)
//                .setType("application/zip")
//
//            findZipFileLauncher.launch(intent)
//        }
}
