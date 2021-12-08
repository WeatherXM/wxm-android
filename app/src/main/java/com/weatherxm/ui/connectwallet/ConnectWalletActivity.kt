package com.weatherxm.ui.connectwallet

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanIntentResult
import com.journeyapps.barcodescanner.ScanOptions
import com.weatherxm.R
import com.weatherxm.data.Resource
import com.weatherxm.data.Status
import com.weatherxm.data.Wallet
import com.weatherxm.databinding.ActivityConnectWalletBinding
import com.weatherxm.ui.common.toast
import com.weatherxm.util.ResourcesHelper
import com.weatherxm.util.onTextChanged
import dev.chrisbanes.insetter.applyInsetter
import org.koin.android.ext.android.inject
import org.koin.core.component.KoinComponent
import timber.log.Timber
import java.util.*

class ConnectWalletActivity : AppCompatActivity(), KoinComponent {
    private lateinit var binding: ActivityConnectWalletBinding
    private val model: ConnectWalletViewModel by viewModels()
    private val resHelper: ResourcesHelper by inject()
    private var snackbar: Snackbar? = null

    // Register the launcher and result handler for QR code scanner
    private val barcodeLauncher =
        registerForActivityResult(ScanContract()) { result: ScanIntentResult ->
            result.contents.let { address ->
                binding.address.setText(address)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityConnectWalletBinding.inflate(layoutInflater)
        setContentView(binding.root)

        applyMapInsets()

        val wallet = intent?.extras?.getParcelable<Wallet>(ARG_WALLET)
        wallet?.address?.let {
            showCurrentAddress(it)
        }

        binding.toolbar.setNavigationOnClickListener {
            onBackPressed()
        }

        binding.addressContainer.setEndIconOnClickListener {
            barcodeLauncher.launch(ScanOptions().setBeepEnabled(false))
        }

        binding.address.onTextChanged {
            binding.addressContainer.error = null
            binding.saveBtn.isEnabled = !binding.address.text.isNullOrEmpty()
        }

        binding.saveBtn.setOnClickListener {
            val addressValidationResp = model.validateAddress(binding.address.text.toString())

            if (addressValidationResp.status == Status.ERROR) {
                binding.addressContainer.error = addressValidationResp.message
                return@setOnClickListener
            }

            val termsChecked = model.validateTermsCheckbox(binding.termsCheckbox.isChecked)
            val ownerChecked = model.validateOwnershipCheckbox(binding.ownershipCheckbox.isChecked)

            if (termsChecked.status == Status.ERROR) {
                toast(termsChecked.message!!, Toast.LENGTH_LONG)
                return@setOnClickListener
            }

            if (ownerChecked.status == Status.ERROR) {
                toast(ownerChecked.message!!, Toast.LENGTH_LONG)
                return@setOnClickListener
            }

            binding.loading.visibility = View.VISIBLE
            changeButtonState(false)
            model.saveAddress(binding.address.text.toString())
        }

        // Listen for login state change
        model.isAddressSaved().observe(this) { result ->
            onAddressSaved(result)
        }
    }

    private fun showCurrentAddress(address: String) {
        binding.toolbar.title = resHelper.getString(R.string.title_change_wallet)
        binding.currentAddressTitle.visibility = View.VISIBLE
        binding.currentAddress.visibility = View.VISIBLE
        binding.currentAddress.text = address
    }

    private fun onAddressSaved(result: Resource<String>) {
        when (result.status) {
            Status.SUCCESS -> {
                changeButtonState(true)
                changeAddressInputState(true)
                Timber.d("Address saved.")
                result.data?.let { showSnackBarMessage(it) }
                showCurrentAddress(binding.address.text.toString())
            }
            Status.ERROR -> {
                changeButtonState(true)
                changeAddressInputState(true)
                binding.loading.visibility = View.INVISIBLE
                result.message?.let { showSnackBarMessage(it) }
            }
            Status.LOADING -> {
                changeButtonState(false)
                changeAddressInputState(false)
                binding.loading.visibility = View.VISIBLE
            }
        }
    }

    private fun showSnackBarMessage(message: String) {
        if (snackbar?.isShown == true) {
            snackbar?.dismiss()
        }
        snackbar = Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG)
        snackbar?.show()
    }

    private fun changeButtonState(enabled: Boolean) {
        binding.saveBtn.isEnabled = enabled
    }

    private fun changeAddressInputState(enabled: Boolean) {
        binding.address.isEnabled = enabled
    }

    private fun applyMapInsets() {
        binding.root.applyInsetter {
            type(statusBars = true) {
                padding(left = false, top = true, right = false, bottom = false)
            }
            type(navigationBars = true) {
                padding(left = false, top = false, right = false, bottom = true)
            }
        }
    }

    companion object {
        const val ARG_WALLET = "wallet"
    }
}
